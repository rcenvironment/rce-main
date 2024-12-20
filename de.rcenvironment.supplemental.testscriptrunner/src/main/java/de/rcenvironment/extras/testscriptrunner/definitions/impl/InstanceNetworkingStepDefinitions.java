/*
 * Copyright 2006-2024 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.extras.testscriptrunner.definitions.impl;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.rcenvironment.core.command.spi.AbstractParsedCommandParameter;
import de.rcenvironment.core.command.spi.ParsedBooleanParameter;
import de.rcenvironment.core.command.spi.ParsedIntegerParameter;
import de.rcenvironment.core.command.spi.ParsedMultiParameter;
import de.rcenvironment.core.command.spi.ParsedStringParameter;
import de.rcenvironment.core.instancemanagement.internal.InstanceConfigurationException;
import de.rcenvironment.core.instancemanagement.internal.SSHAccountParameters;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.exception.OperationFailureException;
import de.rcenvironment.extras.testscriptrunner.definitions.common.InstanceManagementStepDefinitionBase;
import de.rcenvironment.extras.testscriptrunner.definitions.common.ManagedInstance;
import de.rcenvironment.extras.testscriptrunner.definitions.common.TestScenarioExecutionContext;
import de.rcenvironment.extras.testscriptrunner.definitions.helper.ConnectionOptionConstants;
import de.rcenvironment.extras.testscriptrunner.definitions.helper.RegularConnectionOptions;
import de.rcenvironment.extras.testscriptrunner.definitions.helper.RegularConnectionParameters;
import de.rcenvironment.extras.testscriptrunner.definitions.helper.SSHConnectionOptions;
import de.rcenvironment.extras.testscriptrunner.definitions.helper.StepDefinitionConstants;
import de.rcenvironment.extras.testscriptrunner.definitions.helper.UplinkConnectionOptions;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/**
 * Steps for setting up, changing or asserting the network connection(s) between instances.
 * 
 * @author Marlon Schroeter
 * @author Robert Mischke
 * @author Matthias Y. Wagner
 */
public class InstanceNetworkingStepDefinitions extends InstanceManagementStepDefinitionBase {

    private static final String DEFAULT_UPLINK_CLIENT_ID = "default";

    private static final ManagedInstance[] EMPTY_INSTANCE_ARRAY = new ManagedInstance[0];

    public InstanceNetworkingStepDefinitions(TestScenarioExecutionContext executionContext) {
        super(executionContext);
    }

    /**
     * Configures the network connections defined by the given description string.
     * 
     * @param connectionsSetup the comma-separated list of configurations to configure. Has to follow the specific pattern: A-[type]->B
     *        [opt1 opt2], where type specifies which kind of connection shall be configured (reg[ular],ssh, upl[ink]) and a whitespace
     *        separated list consisting of these possible options: autoStart, serverNumber_<number>, role_<role> TODO which options to
     *        support?
     * @param cloneFlag for multiple clients to configure, indicate if they shall have the same "cloned" ID
     */
    @Given("^configured( cloned)? network connection[s]? \"([^\"]*)\"$")
    public void givenConfiguredNetworkConnections(String cloneFlag, String connectionsSetup) throws Exception {
        Boolean cloned = cloneFlag != null;
        if (cloned) {
            throw internalError("The 'cloned' flag is deprecated; specify a clientId=... option instead");
        }

        printToCommandConsole(StringUtils.format("Configuring network connections \"%s\"", connectionsSetup));
        // parse the string defining the intended network connections
        Pattern p = Pattern.compile("\\s*(\\w+)-(?:\\[(reg|ssh|upl)\\]-)?>(\\w+)\\s*(?:\\[([\\w\\s=]*)\\])?\\s*");
        for (String connectionSetupPart : connectionsSetup.split(",")) {
            Matcher m = p.matcher(connectionSetupPart);
            if (!m.matches()) {
                fail("Syntax error in connection setup part: " + connectionSetupPart);
            }
            final ManagedInstance clientInstance = resolveInstance(m.group(1));

            final String connectionType;
            if (m.group(2) == null) {
                connectionType = StepDefinitionConstants.CONNECTION_TYPE_REGULAR; // supporting previous -> connections representing regular
                                                                                  // connections
            } else {
                connectionType = m.group(2);
            }

            final ManagedInstance serverInstance = resolveInstance(m.group(3));
            final String options = m.group(4);
            switch (connectionType) {
            case (StepDefinitionConstants.CONNECTION_TYPE_REGULAR):
                setUpCompleteRegularConnection(clientInstance, serverInstance, options);
                break;
            case (StepDefinitionConstants.CONNECTION_TYPE_SSH):
                setUpCompleteSSHConnection(clientInstance, serverInstance, options);
                break;
            case (StepDefinitionConstants.CONNECTION_TYPE_UPLINK):
                setUpBothEndsOfUplinkConnection(clientInstance, serverInstance, options);
                break;
            default:
                fail(StringUtils.format(StepDefinitionConstants.ERROR_MESSAGE_UNSUPPORTED_TYPE, connectionType));
            }
        }
    }

    /**
     * TODO consider transforming to given step.
     * 
     * @param serverInstanceId server instance connected to
     * @param connectionType non-null represents a connection type other than regular
     * @param options list of whitespace separated options
     */
    @When("^configuring(?: instance)? \"([^\"]+)\" as a(?: (reg|ssh|upl))? server(?: given(?: the)? option[s]? \\[([^\\]]*)\\])?$")
    public void whenConfiguringInstanceAsServer(String serverInstanceId, String connectionType, String options)
        throws Exception {
        if (connectionType == null) {
            connectionType = StepDefinitionConstants.CONNECTION_TYPE_REGULAR;
        }
        final ManagedInstance serverInstance = resolveInstance(serverInstanceId);
        // currently only one option (servernumber) supported for configuring server this way. If this changes, creating a class
        // ServerOptions similar to ConnectionOptions should be considered and handled in a similar way.
        final int serverNumber;
        if (options == null) {
            serverNumber = 0;
        } else {
            serverNumber = Integer.parseInt(options.split(StepDefinitionConstants.OPTION_KV_SEPARATOR)[1]);
        }
        switch (connectionType) {
        case (StepDefinitionConstants.CONNECTION_TYPE_REGULAR):
            configureServer(serverInstance, serverNumber);
            break;
        case (StepDefinitionConstants.CONNECTION_TYPE_SSH):
        case (StepDefinitionConstants.CONNECTION_TYPE_UPLINK):
            configureSSHServer(serverInstance, serverNumber);
            break;
        default:
            fail(StringUtils.format(StepDefinitionConstants.ERROR_MESSAGE_UNSUPPORTED_TYPE, connectionType));
        }
    }

    /**
     * 
     * @param disabledFlag non-null value if ssh account is to be initialized as disabled
     * @param userRole user role of added ssh account
     * @param userName user name of added ssh account
     * @param password password of added ssh account
     * @param serverInstanceId instance where ssh account is added to
     */
    @When("^adding( disabled)? ssh account with user role \"([^\"]+)\", user name \"([^\"]+)\" and password \"([^\"]+)\""
        + " to(?: (?:instance|server))? \"([^\"]+)\"$")
    public void whenAddingSSHAccount(String disabledFlag, String userRole, String userName, String password, String serverInstanceId)
        throws Exception {
        boolean enabled = disabledFlag == null;
        final SSHAccountParameters accountParameters = SSHAccountParameters.builder()
            .userRole(userRole)
            .userName(userName)
            .password(password)
            .isEnabled(enabled)
            .build();
        addSSHAccount(resolveInstance(serverInstanceId), accountParameters);
    }

    /**
     * @param clientInstanceId client instance
     * @param serverInstanceId server instance, needs to be configured beforehand
     * @param connectionType type of connection used for connecting. Viable is reg[ular] ssh and upl[ink]. Omitting this leads to a regular
     *        connection
     * @param options list of whitespace separated options
     */
    // TODO clarify: the step text suggests performing the connection, while it actually only configures it
    @When("^connecting(?: instance)? \"([^\"]+)\" to(?: (?:instance|server))? \"([^\"]+)\"(?: via(reg|ssh|upl))?"
        + "(?: given(?: the)? option[s]? \\[([^\\]]*)\\])?$")
    public void whenConnectingInstances(String clientInstanceId, String serverInstanceId, String connectionType,
        String options) throws Exception {
        if (connectionType == null) {
            connectionType = StepDefinitionConstants.CONNECTION_TYPE_REGULAR;
        }
        ManagedInstance clientInstance = resolveInstance(clientInstanceId);
        ManagedInstance serverInstance = resolveInstance(serverInstanceId);
        switch (connectionType) {
        case (StepDefinitionConstants.CONNECTION_TYPE_REGULAR):
            setupPartialRegularConnection(clientInstance, serverInstance, options);
            break;
        case (StepDefinitionConstants.CONNECTION_TYPE_SSH):
            setUpClientSideOfSSHConnection(clientInstance, serverInstance, options);
            break;
        case (StepDefinitionConstants.CONNECTION_TYPE_UPLINK):
            setUpPartialUplinkConnection(clientInstance, serverInstance, options);
            break;
        default:
            fail(StringUtils.format(StepDefinitionConstants.ERROR_MESSAGE_UNSUPPORTED_TYPE, connectionType));
        }
    }

    /**
     * Checks each of the connections configured using {@link #configuredNetworkConnections()} until it is in the CONNECTED state, or the
     * given timeout is reached.
     * 
     * @param maxWaitTimeSeconds the maximum time to wait for connections to be established
     */
    @Then("^all auto-start network connections should be ready within (\\d+) seconds$")
    public void thenVerifyAutoStartConnectionsConnected(int maxWaitTimeSeconds) throws Exception {
        printToCommandConsole("Waiting for all auto-start network connections to complete");

        final Set<ManagedInstance> pendingInstances = new HashSet<>();
        for (ManagedInstance instance : executionContext.getEnabledInstances()) {
            if (!instance.accessConfiguredAutostartConnectionIds().isEmpty()) {
                pendingInstances.add(instance);
            }
        }

        final long maximumTimestampForStandardAttempts = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(maxWaitTimeSeconds);

        // printToCommandConsole("Waiting for " + maxWaitTimeSeconds + " seconds(s)...");
        while (!pendingInstances.isEmpty() && System.currentTimeMillis() <= maximumTimestampForStandardAttempts) {
            for (ManagedInstance instance : detachedIterableCopy(pendingInstances)) {
                boolean success = testIfConfiguredOutgoingConnectionsAreConnected(instance, false);
                if (success) {
                    pendingInstances.remove(instance);
                }
            }
            if (!pendingInstances.isEmpty()) {
                Thread.sleep(StepDefinitionConstants.SLEEP_DEFAULT_IN_MILLISECS);
            }
        }

        if (!pendingInstances.isEmpty()) {
            // at least one instance was not successfully validated yet; make one final attempt and log any failures
            for (ManagedInstance instance : detachedIterableCopy(pendingInstances)) {
                boolean success = testIfConfiguredOutgoingConnectionsAreConnected(instance, true); // true = print failure info
                if (success) {
                    pendingInstances.remove(instance);
                }
            }
        }

        if (!pendingInstances.isEmpty()) {
            fail("On " + pendingInstances.size() + " instance(s), the configured outgoing connections "
                + "were not established after waiting/retrying for " + maxWaitTimeSeconds + " second(s)");
        }
    }

    /**
     * Verifies that a given set of instances is present in a given instance's visible network.
     * 
     * @param instanceId the node that should have its visible network inspected
     * @param testType whether to test for "should contain" or "should consist of" the given list of instances
     * @param listOfExpectedVisibleInstances the comma-separated list of expected instances
     * @throws OperationFailureException on failure to execute the step (as opposed to an assertion failure)
     */
    @Then("^the visible network of \"([^\"]*)\" should (consist of|contain) \"([^\"]*)\"$")
    public void thenVerifyVisibleNetworkConsistsOfOrContains(String instanceId, String testType, String listOfExpectedVisibleInstances)
        throws OperationFailureException {

        String commandOutput = executeCommandOnInstance(resolveInstance(instanceId), "net info", false);

        List<String> expectedVisibleInstances = parseCommaSeparatedList(listOfExpectedVisibleInstances);
        switch (testType) {
        case "contain":
            for (String expectedInstanceId : expectedVisibleInstances) {
                if (!commandOutput.contains(expectedInstanceId)) {
                    fail("The visible network of instance " + instanceId + " does not contain the expected instance " + expectedInstanceId);
                }
            }
            break;
        case "consist of":
            // check if all instances in output are expected
            for (String line : commandOutput.split(StepDefinitionConstants.LINEBREAK_REGEX)) {
                if (line.contains("Reachable network nodes")) {
                    continue; // line indicating list of reachable nodes follows. Must not be checked.
                }
                if (line.contains("Message channels")) {
                    break; // line indicating list of reachable nodes is finished. It and following lines must not be checked.
                }
                if (!isExpectedVisibleInstance(line, expectedVisibleInstances)) {
                    fail(StringUtils.format("Instance %s could see unexpected instances: \n %s", instanceId, line));
                }
            }
            // check if all instances expected are present in output
            thenVerifyVisibleNetworkConsistsOfOrContains(instanceId, "contain", listOfExpectedVisibleInstances);
            break;
        default:
            fail(StringUtils.format("Test type %s is not supported.", testType));
        }
        printToCommandConsole("Verified the visible network of instance \"" + instanceId + "\"");
    }

    /**
     * Verifies the state of an Uplink connection, more precisely the state of a potential Uplink connection setup -- a configured
     * configuration which may or may not exist.
     * 
     * "present" vs "absent" tests whether a given connection setup exists at all; "absent" means that such a setup was never configured or
     * was deleted. "connected" vs "disconnected" represents whether the network connection attached to the given setup is in the state
     * "connected" or not; intermediate states like "connecting" or "disconnecting" are considered "disconnected".
     * 
     * @param sourceInstanceId the id of the instance with the potentially configured connection
     * @param targetInstanceId the id of the potential connection's target instance
     * @param criterion the test criterion string
     * @param maxiumWaitSeconds the maximum retry time until success
     */
    @Then("the Uplink connection from \"([^\"]+)\" to \"([^\"]+)\" (?:with userName=\"([^\"]+)\" and clientId=\"([^\"]+)\" )?"
        + "should be (connected|disconnected|present|absent)(?: within (\\d+) seconds?)?$")
    public void thenVerifyStateOfUplinkConnection(String sourceInstanceId, String targetInstanceId, String userNameOverride,
        String clientIdOverride, String criterion, Integer maxiumWaitSeconds) {

        String loginName = Optional.ofNullable(userNameOverride).orElse(sourceInstanceId);
        String clientId = Optional.ofNullable(clientIdOverride).orElse(DEFAULT_UPLINK_CLIENT_ID);

        maxiumWaitSeconds = applyFallbackMaximumRetryTime(maxiumWaitSeconds);

        String description =
            StringUtils.format("Expecting the Uplink connection from \"%s\" to \"%s\" to be \"%s\"", sourceInstanceId,
                targetInstanceId, criterion);
        executeWithRetry((ExecutionAttempt) (attemptCount, isLastAttempt) -> {
            return executeOnceVerifyStateOfUplinkConnections(sourceInstanceId, targetInstanceId, loginName, clientId, criterion,
                isLastAttempt);
        }, description, maxiumWaitSeconds);
    }

    private boolean executeOnceVerifyStateOfUplinkConnections(String sourceInstanceId, String targetInstanceId, String loginName,
        String clientId, String criterion, boolean isLastAttempt) throws OperationFailureException {

        String commandOutput;
        try {
            commandOutput = executeCommandOnInstance(resolveInstance(sourceInstanceId), "uplink list", false);
        } catch (OperationFailureException e) {
            // in a retry loop, a failure to execute the query command on the instance should not generally cause a fatal error but a retry
            if (isLastAttempt) {
                throw e; // rethrow
            } else {
                log.debug(StringUtils.format("Failed to query the state of uplink connections on %s; retrying", targetInstanceId));
                return false; // retry
            }
        } catch (AssertionError e) {
            // remote command execution failing is possible in regular operation if an instance is queried right after its basic startup
            if (!isLastAttempt) {
                return false; // retry
            }
            fail(StringUtils.format("Failed to execute \"uplink list\" on instance %s: %s", sourceInstanceId, e.toString()));
            return false; // only to prevent compiler errors; never reached due to fail() above
        }

        // TODO review this comment
        /**
         * NOTE THAT: From the test case we get only the "simple" name of an instance (e. g. "Uplink"). To be able to check the availability
         * of instances, we need the "Id" of instance, as it is constructed in the test steps which are "using the default build" which are
         * used for the "configures network connections" (e. g. "Uplink_userName"). So we build here a new List where the Ids are
         * constructed as in the described test steps.
         * 
         * !If in the initial steps we do not use the defaults as described, this will not work! mwag: This could be a major TODO to provide
         * easy access to these default values from everywhere
         * 
         * @see InstanceInstantiationStepDefinitions.givenInstancesUsingBuild()
         * @see givenConfiguredNetworkConnections()
         */

        String[] outputLines = commandOutput.split(StepDefinitionConstants.LINEBREAK_REGEX);
        Map<String, Boolean> uplinkInstances = getUplinkConnectionsWithConnectedState(outputLines);

        String connectionId = constructUplinkConnectionSetupId(targetInstanceId, loginName, clientId);

        switch (criterion) {
        case "connected":
            if (!uplinkInstances.getOrDefault(connectionId, false)) {
                if (!isLastAttempt) {
                    return false;
                }
                fail(StringUtils.format("Connection %s at instance %s is not connected", connectionId, sourceInstanceId));
            }

            break;
        case "disconnected":
            if (uplinkInstances.getOrDefault(connectionId, false)) {
                if (!isLastAttempt) {
                    return false;
                }
                fail(StringUtils.format("Connection %s at instance %s is connected when it should be disconnected", connectionId,
                    sourceInstanceId));
            }

            break;
        case "present":
            if (!uplinkInstances.containsKey(connectionId)) {
                if (!isLastAttempt) {
                    return false;
                }
                fail(StringUtils.format("There is no Uplink connection %s configured at %s", connectionId,
                    sourceInstanceId));
            }
            break;
        case "absent":
            if (uplinkInstances.containsKey(connectionId)) {
                if (!isLastAttempt) {
                    return false;
                }
                fail(StringUtils.format("There should be no Uplink connection %s configured at %s", connectionId,
                    sourceInstanceId));
            }

            break;
        default:
            fail(StringUtils.format("Unsopported test criterion %s", criterion));
        }

        return true; // attempt successful
    }

    /**
     * Triggers a hard shutdown of an instance. Note that this action does NOT wait for the instance's termination.
     * 
     * @param instanceId the node that is to be shut down.
     * @throws OperationFailureException on failure to execute the step (as opposed to an assertion failure)
     */
    @When("^instance \"([^\"]*)\" crashes$")
    public void whenTriggeringACrashOfInstance(String instanceId) throws OperationFailureException {
        triggerHardShutdownOfInstance(instanceId);
        printToCommandConsole("Triggered a hard shutdown (crash) of instance \"" + instanceId
            + "\"; note that this action does NOT wait for the instance's termination");
    }

    /**
     * Triggers a hard shutdown of an instance and waits for its termination.
     * 
     * @param instanceId the node that is to be shut down.
     * @param maxWaitSeconds the maximum time to allow until actual shutdown
     * @throws OperationFailureException on failure to execute the step (as opposed to an assertion failure)
     */
    @When("^triggering a crash of instance \"([^\"]*)\" and it terminated within (\\d+) seconds?$")
    public void whenTriggeringACrashOfInstanceAndWaitingForTerminination(String instanceId, int maxWaitSeconds)
        throws OperationFailureException {
        triggerHardShutdownOfInstance(instanceId);
        final String operationTitle = StringUtils.format("Trigger a crash of '%s' and wait for its termination", instanceId);
        // wait for shutdown
        executeWithRetry((ExecutionAttempt) (attempt, isLastAttempt) -> {
            return !INSTANCE_MANAGEMENT_SERVICE.isInstanceRunning(instanceId); // not running -> return success, otherwise retry
        }, operationTitle, maxWaitSeconds);
    }

    private void triggerHardShutdownOfInstance(String instanceId) throws OperationFailureException {
        String commandOutput = executeCommandOnInstance(resolveInstance(instanceId), "force-crash 0", false);
        printToCommandConsole(commandOutput);
    }

    /**
     * Parses the output of "uplink list" and returns a map of connection setup ids with their "connected" state.
     * 
     * @param outputLines the command output split in lines
     * @return a Map with the uplink instances and their respective state of connected (true/false).
     */
    private Map<String, Boolean> getUplinkConnectionsWithConnectedState(String[] outputLines) {
        Map<String, Boolean> uplinkMap = new HashMap<String, Boolean>();
        String foundInstances = "";
        for (String line : outputLines) {
            if (line.startsWith("Finished executing command") || line.length() == 0) {
                // empty line or synthetic final line; not part of the actual output
                continue;
            }
            int positionOfId = line.indexOf("(id:");
            final int minusOne = -1;
            if (positionOfId == minusOne) {
                fail(StringUtils.format("Failed to parse output line of command \"uplink list\":\n%s", line));
            } else {
                String foundInstanceId = line.substring(positionOfId + 5, line.indexOf(")", positionOfId));
                // Alternatively we may want to use the format as when it is originally created:
                // foundInstanceId = StringUtils.format(StepDefinitionConstants.CONNECTION_ID_FORMAT, foundInstanceId,
                // ConnectionOptionConstants.USER_NAME_DEFAULT);
                if (!foundInstances.equals("")) {
                    foundInstances += ", ";
                }
                foundInstances += foundInstanceId;

                if (line.contains("CONNECTED: true")) {
                    uplinkMap.put(foundInstanceId, true);
                } else if (line.contains("CONNECTED: false")) {
                    uplinkMap.put(foundInstanceId, false);
                } else {
                    fail(StringUtils.format("Line of output from command \\\"uplink list: \\n %s has no connected status", line));
                }
            }
        }
        return uplinkMap;
    }

    private boolean isExpectedVisibleInstance(String line, List<String> expectedVisibleInstances) {
        for (String expectedInstanceId : expectedVisibleInstances) {
            if (line.contains(expectedInstanceId)) {
                return true;
            }
        }
        return false;
    }

    private Optional<String> extractValueFromOption(String option) {
        String[] keyValuePair = option.split(StepDefinitionConstants.OPTION_KV_SEPARATOR);
        switch (keyValuePair.length) {
        case (1):
            return Optional.empty();
        case (2):
            return Optional.of(keyValuePair[1]);
        default:
            fail(StringUtils.format("Option %s contains multiple %s", option, StepDefinitionConstants.OPTION_KV_SEPARATOR));
            return null; // never reaches, since fail breaks
        }
    }

    private RegularConnectionOptions parseRegularConnectionOptions(String optionsString) {
        if (optionsString == null) {
            return RegularConnectionOptions.builder().build();
        }
        RegularConnectionOptions.Builder builder = RegularConnectionOptions.builder();
        for (String option : optionsString.split(StepDefinitionConstants.WHITESPACE_SEPARATOR)) {
            final String key = extractKeyFromOption(option);
            final Optional<String> value = extractValueFromOption(option);

            try {
                switch (key) {
                case (ConnectionOptionConstants.AUTO_START_FLAG):
                    builder.autoStart(true);
                    break;
                case (ConnectionOptionConstants.AUTO_RETRY_FLAG):
                    builder.autoRetry(true);
                    break;
                case (ConnectionOptionConstants.AUTO_RETRY_INITIAL_DELAY):
                    builder.autoRetryInitialDelay(Integer.parseInt(value.get()));
                    break;
                case (ConnectionOptionConstants.AUTO_RETRY_MAX_DELAY):
                    builder.autoRetryMaxDelay(Integer.parseInt(value.get()));
                    break;
                case (ConnectionOptionConstants.AUTO_RETRY_DELAY_MULTIPLIER):
                    builder.autoRetryDelayMultiplier(Float.parseFloat(value.get()));
                    break;
                case (ConnectionOptionConstants.CONNECTION_NAME):
                    builder.connectionName(value.get());
                    break;
                case (ConnectionOptionConstants.HOST):
                    builder.host(value.get());
                    break;
                case (ConnectionOptionConstants.PORT):
                    builder.port(Integer.parseInt(value.get()));
                    break;
                case (ConnectionOptionConstants.RELAY):
                    builder.relay(true);
                    break;
                case (ConnectionOptionConstants.SERVER_NUMBER):
                    builder.serverNumber(Integer.parseInt(value.get()));
                    break;
                default:
                    fail(StringUtils.format("Option %s is not valid for a regular connection", key));
                }
            } catch (NumberFormatException e) {
                fail(StringUtils.format(StepDefinitionConstants.ERROR_MESSAGE_WRONG_TYPE, value, key));
            }
        }
        return builder.build();
    }

    private SSHConnectionOptions parseSSHConnectionOptions(String optionsString) {
        if (optionsString == null) {
            return SSHConnectionOptions.builder().build();
        }
        SSHConnectionOptions.Builder builder = SSHConnectionOptions.builder();
        for (String option : optionsString.split(StepDefinitionConstants.WHITESPACE_SEPARATOR)) {
            final String key = extractKeyFromOption(option);
            final Optional<String> value = extractValueFromOption(option);

            try {
                switch (key) {
                case (ConnectionOptionConstants.CONNECTION_NAME):
                    builder.connectionName(value.get());
                    break;
                case (ConnectionOptionConstants.DISPLAY_NAME):
                    builder.displayName(value.get());
                    break;
                case (ConnectionOptionConstants.HOST):
                    builder.host(value.get());
                    break;
                case (ConnectionOptionConstants.PORT):
                    builder.port(Integer.parseInt(value.get()));
                    break;
                case (ConnectionOptionConstants.SERVER_NUMBER):
                    builder.serverNumber(Integer.parseInt(value.get()));
                    break;
                case (ConnectionOptionConstants.USER_NAME):
                    builder.userName(value.get());
                    break;
                case (ConnectionOptionConstants.USER_ROLE):
                    builder.userRole(value.get());
                    break;
                default:
                    fail(StringUtils.format("Option %s is not valid for a SSH connection", key));
                }
            } catch (NumberFormatException e) {
                fail(StringUtils.format(StepDefinitionConstants.ERROR_MESSAGE_WRONG_TYPE, value, key));
            }
        }
        return builder.build();
    }

    private UplinkConnectionOptions parseUplinkConnectionOptions(String optionsString) {
        if (optionsString == null) {
            return UplinkConnectionOptions.builder().build();
        }
        UplinkConnectionOptions.Builder builder = UplinkConnectionOptions.builder();
        for (String option : optionsString.split(StepDefinitionConstants.WHITESPACE_SEPARATOR)) {
            final String key = extractKeyFromOption(option);
            final Optional<String> value = extractValueFromOption(option);

            try {
                switch (key) {
                case (ConnectionOptionConstants.AUTO_RETRY_FLAG):
                    builder.autoRetry(true);
                    break;
                case (ConnectionOptionConstants.AUTO_START_FLAG):
                    builder.autoStart(true);
                    break;
                case (ConnectionOptionConstants.CLIENT_ID):
                    builder.clientId(value.get());
                    break;
                case (ConnectionOptionConstants.CONNECTION_NAME):
                    builder.connectionName(value.get());
                    break;
                case (ConnectionOptionConstants.GATEWAY_FLAG):
                    builder.gateway(true);
                    break;
                case (ConnectionOptionConstants.HOST):
                    builder.host(value.get());
                    break;
                case (ConnectionOptionConstants.PASSWORD):
                    builder.password(value.get());
                    break;
                case (ConnectionOptionConstants.PORT):
                    builder.port(Integer.parseInt(value.get()));
                    break;
                case (ConnectionOptionConstants.SERVER_NUMBER):
                    builder.serverNumber(Integer.parseInt(value.get()));
                    break;
                case (ConnectionOptionConstants.USER_NAME):
                    builder.userName(value.get());
                    break;
                case (ConnectionOptionConstants.USER_ROLE):
                    builder.userRole(value.get());
                    break;
                default:
                    fail(StringUtils.format("Option %s is not valid for a uplink connection", key));
                }
            } catch (NumberFormatException e) {
                fail(StringUtils.format(StepDefinitionConstants.ERROR_MESSAGE_WRONG_TYPE, value, key));
            }
        }
        return builder.build();
    }

    // TODO rework naming scheme with regular and partial setup
    private void setUpCompleteRegularConnection(ManagedInstance clientInstance, ManagedInstance serverInstance, String options)
        throws Exception {
        RegularConnectionOptions connectionOptions = parseRegularConnectionOptions(options);
        final Integer serverPort = configureServer(serverInstance, connectionOptions.getServerNumber());
        if (connectionOptions.isRelay()) {
            configureRelay(serverInstance, true);
        }
        final RegularConnectionParameters connectionParameters = RegularConnectionParameters.builder()
            .connectionId(StringUtils.format(StepDefinitionConstants.CONNECTION_ID_FORMAT, serverInstance.getId(), serverPort))
            .host(ConnectionOptionConstants.HOST_DEFAULT)
            .port(serverPort)
            .autoStartFlag(connectionOptions.getAutoStartFlag())
            .autoRetryFlag(connectionOptions.getAutoRetryFlag())
            .autoRetryInitDelay(connectionOptions.getAutoRetryInitialDelay())
            .autoRetryMaxDelay(connectionOptions.getAutoRetryMaxDelay())
            .autoRetryDelayMultiplier(connectionOptions.getAutoRetryDelayMultiplier())
            .build();
        configureRegularConnection(clientInstance, connectionParameters);
    }

    private void setupPartialRegularConnection(ManagedInstance clientInstance, ManagedInstance serverInstance, String options)
        throws Exception {
        RegularConnectionOptions connectionOptions = parseRegularConnectionOptions(options);
        final RegularConnectionParameters connectionParameters = RegularConnectionParameters.builder()
            .connectionId(connectionOptions.getConnectionName()
                .orElse(
                    StringUtils.format(StepDefinitionConstants.CONNECTION_ID_FORMAT, serverInstance.getId(), connectionOptions.getPort())))
            .host(connectionOptions.getHost())
            .port(connectionOptions.getPort()
                .orElse(
                    getServerPort(serverInstance, connectionOptions.getServerNumber(), StepDefinitionConstants.CONNECTION_TYPE_REGULAR)))
            .autoStartFlag(connectionOptions.getAutoStartFlag())
            .autoRetryFlag(connectionOptions.getAutoRetryFlag())
            .autoRetryInitDelay(connectionOptions.getAutoRetryInitialDelay())
            .autoRetryMaxDelay(connectionOptions.getAutoRetryMaxDelay())
            .autoRetryDelayMultiplier(connectionOptions.getAutoRetryDelayMultiplier())
            .build();
        configureRegularConnection(clientInstance, connectionParameters);
    }

    private void setUpCompleteSSHConnection(ManagedInstance clientInstance, ManagedInstance serverInstance, String options)
        throws Exception {

        SSHConnectionOptions connectionOptions = parseSSHConnectionOptions(options);
        final String userName = connectionOptions.getUserName().orElse(clientInstance.getId());
        // intended for testing setups (e.g. BDD) only: use the login name as default password
        final String password = userName; // TODO clarify: why is there no getPassword() method?

        final Integer serverPort = configureSSHServer(serverInstance, connectionOptions.getServerNumber());
        final SSHAccountParameters accountParametersSSH = SSHAccountParameters.builder()
            .userRole(connectionOptions.getUserRole().orElse("ra_demo")) // FIXME this is not a role; pick a better default
            .userName(userName)
            .password(password)
            .isEnabled(true)
            .build();
        addSSHAccount(serverInstance, accountParametersSSH);
        configureSSHConnection(clientInstance,
            StringUtils.format(StepDefinitionConstants.CONNECTION_ID_FORMAT, serverInstance.getId(), connectionOptions.getPort()),
            clientInstance.getId(), ConnectionOptionConstants.HOST_DEFAULT, serverPort, userName);
    }

    // TODO refactor this to eliminate duplication with setUpCompleteSSHConnection()?
    // TODO clarify password configuration
    private void setUpClientSideOfSSHConnection(ManagedInstance clientInstance, ManagedInstance serverInstance, String options)
        throws Exception {
        SSHConnectionOptions connectionOptions = parseSSHConnectionOptions(options);
        final String userName = connectionOptions.getUserName().orElse(clientInstance.getId());

        final Integer serverPort = connectionOptions.getPort().orElse(
            getServerPort(serverInstance, connectionOptions.getServerNumber(), StepDefinitionConstants.CONNECTION_TYPE_REGULAR));
        configureSSHConnection(clientInstance,
            connectionOptions.getConnectionName()
                .orElse(StringUtils.format(StepDefinitionConstants.CONNECTION_ID_FORMAT, serverInstance.getId(),
                    userName)),
            connectionOptions.getDisplayName()
                .orElse(StringUtils.format(StepDefinitionConstants.CONNECTION_ID_FORMAT, serverInstance.getId(),
                    userName)),
            connectionOptions.getHost(), serverPort, userName);
    }

    /**
     * Sets up an Uplink connection for test scenarios (e.g. BDD testing) by configuring both the client and the server side. Previous
     * versions of this method offered a special "clone" parameter; this is superseded by specifying "clientId=..." as part of the options
     * string.
     * 
     * @param clientInstance the client instance where a connection is to be registered
     * @param serverInstance the Uplink server where an account is to be registered
     * @param options a string of options, separated by {@link StepDefinitionConstants#WHITESPACE_SEPARATOR}, with each option either being
     *        a flag (key only) or a key-value pair separated by {@link StepDefinitionConstants#OPTION_KV_SEPARATOR}
     */
    private void setUpBothEndsOfUplinkConnection(ManagedInstance clientInstance, ManagedInstance serverInstance, String options)
        throws Exception {

        UplinkConnectionOptions connectionOptions = parseUplinkConnectionOptions(options);
        final Integer serverPort = configureSSHServer(serverInstance, connectionOptions.getServerNumber());
        final String userName = connectionOptions.getUserName().orElse(clientInstance.getId());
        // the default value should (but doesn't have to) match UplinkProtocolConstants.SESSION_QUALIFIER_DEFAULT
        final String clientId = connectionOptions.getClientId().orElse(DEFAULT_UPLINK_CLIENT_ID);
        // intended for testing setups (e.g. BDD) only: use the login name as default password
        final String password = connectionOptions.getPassword().orElse(userName);

        final SSHAccountParameters accountParametersUpl = SSHAccountParameters.builder()
            .userRole(connectionOptions.getUserRole().orElse("uplink_client"))
            .userName(userName)
            .password(password)
            .isEnabled(true)
            .build();
        addSSHAccount(serverInstance, accountParametersUpl);

        final String connectionSetupId = constructUplinkConnectionSetupId(serverInstance.getId(), userName, clientId);
        // StringUtils.format(StepDefinitionConstants.CONNECTION_ID_FORMAT, clientInstance.getId(), serverInstance.getId());

        final ParsedMultiParameter uplinkParameters = new ParsedMultiParameter(
            new AbstractParsedCommandParameter[] {
                new ParsedStringParameter(connectionSetupId),
                new ParsedStringParameter(ConnectionOptionConstants.HOST_DEFAULT),
                new ParsedIntegerParameter(serverPort),
                new ParsedStringParameter(clientId),
                new ParsedBooleanParameter(connectionOptions.getGateway()),
                new ParsedBooleanParameter(connectionOptions.getAutoStart()),
                new ParsedBooleanParameter(connectionOptions.getAutoRetry()),
                new ParsedStringParameter(userName),
                new ParsedStringParameter("password"),
                new ParsedStringParameter(password)
            });

        configureUplinkConnection(clientInstance, uplinkParameters);
    }

    // TODO refactor this to eliminate duplication with setUpUplinkConnectionForTesting()?
    private void setUpPartialUplinkConnection(ManagedInstance clientInstance, ManagedInstance serverInstance, String options)
        throws Exception {
        UplinkConnectionOptions connectionOptions = parseUplinkConnectionOptions(options);
        final String userName = connectionOptions.getUserName().orElse(clientInstance.getId());
        // the default value should (but doesn't have to) match UplinkProtocolConstants.SESSION_QUALIFIER_DEFAULT
        final String clientId = connectionOptions.getClientId().orElse(DEFAULT_UPLINK_CLIENT_ID);
        // intended for testing setups (e.g. BDD) only: use the login name as default password
        final String password = connectionOptions.getPassword().orElse(userName);

        final String connectionSetupId = constructUplinkConnectionSetupId(clientInstance.getId(), userName, clientId);

        final ParsedMultiParameter uplinkParameters = new ParsedMultiParameter(
            new AbstractParsedCommandParameter[] {
                new ParsedStringParameter(connectionSetupId),
                new ParsedStringParameter(ConnectionOptionConstants.HOST_DEFAULT),
                new ParsedIntegerParameter(connectionOptions.getPort()
                    .orElse(
                        getServerPort(serverInstance, connectionOptions.getServerNumber(),
                            StepDefinitionConstants.CONNECTION_TYPE_REGULAR))),
                new ParsedStringParameter(clientId),
                new ParsedBooleanParameter(connectionOptions.getGateway()),
                new ParsedBooleanParameter(connectionOptions.getAutoStart()),
                new ParsedBooleanParameter(connectionOptions.getAutoRetry()),
                new ParsedStringParameter(userName),
                new ParsedStringParameter("password"),
                new ParsedStringParameter(password)
            });

        configureUplinkConnection(clientInstance, uplinkParameters);
    }

    private void configureRegularConnection(final ManagedInstance clientInstance, final RegularConnectionParameters parameters)
        throws Exception {
        INSTANCE_MANAGEMENT_SERVICE.applyInstanceConfigurationOperations(
            clientInstance.getId(),
            INSTANCE_MANAGEMENT_SERVICE.newConfigurationOperationSequence().addNetworkConnection(
                parameters.getConnectionId(), parameters.getHost(), parameters.getPort(), parameters.isAutoStart(),
                parameters.isAutoRetry(), parameters.getAutoRetryInitDelay(), parameters.getAutoRetryMaxDelay(),
                parameters.getAutoRetryDelayMultiplier()),
            getTextoutReceiverForIMOperations());
        if (parameters.isAutoStart()) {
            // note: as of release 8.1.0 and before, "cn list" does not output the connection id provided via IM configuration, but
            // "ip:port" for each connection; so this is the string needed to detect the connection's state from the output --
            // misc_ro
            clientInstance.accessConfiguredAutostartConnectionIds()
                .add(StringUtils.format("%s:%d", parameters.getHost(), parameters.getPort()));
        }
    }

    private void configureRelay(ManagedInstance serverInstance, boolean isRelay) throws Exception {
        INSTANCE_MANAGEMENT_SERVICE.applyInstanceConfigurationOperations(
            serverInstance.getId(),
            INSTANCE_MANAGEMENT_SERVICE.newConfigurationOperationSequence().setRelayFlag(isRelay),
            getTextoutReceiverForIMOperations());
    }

    private void configureSSHConnection(final ManagedInstance clientInstance, final String connectionId, final String displayName,
        final String host, final int port, final String loginName) throws Exception {
        INSTANCE_MANAGEMENT_SERVICE.applyInstanceConfigurationOperations(clientInstance.getId(),
            INSTANCE_MANAGEMENT_SERVICE.newConfigurationOperationSequence().addSshConnection(connectionId, displayName, host, port,
                loginName),
            getTextoutReceiverForIMOperations());

    }

    private void configureUplinkConnection(final ManagedInstance clientInstance, final ParsedMultiParameter uplinkParameters)
        throws Exception {
        INSTANCE_MANAGEMENT_SERVICE.applyInstanceConfigurationOperations(clientInstance.getId(),
            INSTANCE_MANAGEMENT_SERVICE.newConfigurationOperationSequence().addUplinkConnection(uplinkParameters),
            getTextoutReceiverForIMOperations());
    }

    private Integer configureServer(final ManagedInstance serverInstance, final int serverNumber)
        throws InstanceConfigurationException, IOException {
        Integer serverPort = getServerPort(serverInstance, serverNumber, StepDefinitionConstants.CONNECTION_TYPE_REGULAR);
        INSTANCE_MANAGEMENT_SERVICE.applyInstanceConfigurationOperations(serverInstance.getId(),
            INSTANCE_MANAGEMENT_SERVICE.newConfigurationOperationSequence()
                .addServerPort(
                    StringUtils.format(StepDefinitionConstants.HOST_PORT_FORMAT, ConnectionOptionConstants.HOST_DEFAULT, serverPort),
                    ConnectionOptionConstants.HOST_DEFAULT, serverPort),
            getTextoutReceiverForIMOperations());
        return serverPort;
    }

    private Integer configureSSHServer(final ManagedInstance serverInstance, final int serverNumber) throws Exception {
        Integer serverPort = getServerPort(serverInstance, serverNumber, StepDefinitionConstants.CONNECTION_TYPE_SSH);
        INSTANCE_MANAGEMENT_SERVICE.applyInstanceConfigurationOperations(serverInstance.getId(),
            INSTANCE_MANAGEMENT_SERVICE.newConfigurationOperationSequence().enableSshServer(ConnectionOptionConstants.HOST_DEFAULT,
                serverPort),
            getTextoutReceiverForIMOperations());
        return serverPort;
    }

    private void addSSHAccount(final ManagedInstance serverInstance, final SSHAccountParameters parameters)
        throws Exception {
        INSTANCE_MANAGEMENT_SERVICE.applyInstanceConfigurationOperations(serverInstance.getId(),
            INSTANCE_MANAGEMENT_SERVICE.newConfigurationOperationSequence().addSshAccount(parameters),
            getTextoutReceiverForIMOperations());
    }

    private Integer getServerPort(final ManagedInstance instance, final int serverNumber, String connectionType) {
        if (connectionType.equals(StepDefinitionConstants.CONNECTION_TYPE_UPLINK)) {
            connectionType = StepDefinitionConstants.CONNECTION_TYPE_SSH;
        }
        Integer serverPort = instance.getServerPort(connectionType, serverNumber);
        if (serverPort == null) {
            serverPort = PORT_NUMBER_GENERATOR.incrementAndGet();
            while (!isPortAvailable(serverPort)) {
                serverPort = PORT_NUMBER_GENERATOR.incrementAndGet();
            }
            instance.setServerPort(connectionType, serverNumber, serverPort);
        }
        return serverPort;
    }

    private boolean isPortAvailable(int portNumber) {
        ServerSocket socket = null;
        // Solution for checking port availability inspired by https://stackoverflow.com/a/435579
        try {
            socket = new ServerSocket(portNumber);
            socket.setReuseAddress(true);
            return true;
        } catch (IOException e) {
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException innerException) {
                    /* should not be thrown */
                }
            }
        }
        return false;
    }

    private String constructUplinkConnectionSetupId(String targetInstanceId, String loginName, String clientId) {
        return String.join("_", targetInstanceId, loginName, clientId);
    }

    private boolean testIfConfiguredOutgoingConnectionsAreConnected(final ManagedInstance instance, boolean isFinalAttempt)
        throws OperationFailureException {
        final List<String> connectionIds = instance.accessConfiguredAutostartConnectionIds();

        String commandOutput = executeCommandOnInstance(instance, "cn list", false);
        int matches = 0;
        for (String connectionId : connectionIds) {
            Matcher matcher = Pattern.compile("'" + connectionId + "'.*?- (\\w+)").matcher(commandOutput);
            if (!matcher.find()) {
                if (isFinalAttempt) {
                    fail(StringUtils.format(
                        "Unexpected state: Attempted to verify the state of connection \"%s\" on \"%s\", "
                            + "but it did not appear in the output of \"cn list\" at all; full command output:\n%s",
                        connectionId, instance, commandOutput));
                } else {
                    continue; // most likely, the tested instance has simply not registered the connection(s) yet, so just retry
                }
            }
            String state = matcher.group(1);
            if (matcher.find()) {
                fail(StringUtils.format(
                    "Unexpected state: Found more than one entry for connection \"%s\" on \"%s\" "
                        + "in the output of \"cn list\"; full command output:\n%s",
                    connectionId, instance, commandOutput));
            }
            if ("CONNECTED".equals(state)) {
                matches++;
            } else {
                if (isFinalAttempt) {
                    printToCommandConsole(StringUtils.format(
                        "Failed expectation: the connection \"%s\" on \"%s\" should be in state \"CONNECTED\", but is \"%s\"",
                        connectionId, instance, state));
                }
            }
        }
        boolean success = matches == connectionIds.size();
        return success;
    }

    private ManagedInstance[] detachedIterableCopy(final Collection<ManagedInstance> pendingInstances) {
        return pendingInstances.toArray(EMPTY_INSTANCE_ARRAY);
    }

    private String extractKeyFromOption(String option) {
        return option.split(StepDefinitionConstants.OPTION_KV_SEPARATOR)[0];
    }

}
