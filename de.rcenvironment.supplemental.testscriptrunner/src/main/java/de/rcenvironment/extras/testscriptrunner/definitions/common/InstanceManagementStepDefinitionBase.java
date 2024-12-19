/*
 * Copyright 2006-2023 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.extras.testscriptrunner.definitions.common;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import com.jcraft.jsch.JSchException;

import de.rcenvironment.core.instancemanagement.InstanceManagementService;
import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.exception.OperationFailureException;
import de.rcenvironment.core.utils.common.textstream.receivers.CapturingTextOutReceiver;
import de.rcenvironment.core.utils.ssh.jsch.SshParameterException;
import de.rcenvironment.extras.testscriptrunner.definitions.helper.StepDefinitionConstants;
import de.rcenvironment.toolkit.modules.concurrency.api.RunnablesGroup;

/**
 * common superclass for test step definitions, providing common interfaces, subclasses, methods and teststeps for instance management.
 * 
 * @author Marlon Schroeter
 * @author Robert Mischke
 */
public abstract class InstanceManagementStepDefinitionBase extends AbstractStepDefinitionBase {

    // TODO randomize and check for collisions before starting instance
    protected static final AtomicInteger PORT_NUMBER_GENERATOR = new AtomicInteger(52100);

    protected static final InstanceManagementService INSTANCE_MANAGEMENT_SERVICE = ExternalServiceHolder.getInstanceManagementService();

    private static final int CONCURRENT_INSTANCE_STARTING_STAGGERED_DELAY_MSEC = 500;

    public InstanceManagementStepDefinitionBase(TestScenarioExecutionContext executionContext) {
        super(executionContext);
    }

    /**
     * Interface passing method to be performed on instances.
     * 
     * @author Marlon Schroeter
     * @author Robert Mischke
     */
    protected interface InstanceAction {

        void performActionOnInstance(ManagedInstance instance, long timeout) throws OperationFailureException, IOException;
    }

    /**
     * Interface passing method to be performed on a subset of all instances.
     * 
     * @author Marlon Schroeter
     */
    protected interface InstanceIterator {

        void iterateActionOverInstance(ManagedInstance instance) throws OperationFailureException, IOException;
    }

    /**
     * Enum listing all possible execution modes, when performing actions on instances.
     */
    protected enum InstanceActionExecutionType {
        ORDERED,
        CONCURRENT,
        RANDOM
    }

    /**
     * @param isMainAction true if executing this command is the actual test action; false if this it is incidental, e.g. for testing state
     *        or performing cleanup
     * @throws OperationFailureException on failure to execute the command
     */
    protected final String executeCommandOnInstance(final ManagedInstance instance, String commandString, boolean isMainAction)
        throws OperationFailureException {
        final String instanceId = instance.getId();
        final String startInfoText = StringUtils.format("Executing command \"%s\" on instance \"%s\"", commandString, instanceId);
        if (isMainAction) {
            printToCommandConsole(startInfoText);
        }
        log.debug(startInfoText);
        CapturingTextOutReceiver commandOutputReceiver = new CapturingTextOutReceiver();
        try {
            final int maxAttempts = 3;
            int numAttempts = 0;
            while (numAttempts < maxAttempts) {
                try {
                    INSTANCE_MANAGEMENT_SERVICE.executeCommandOnInstance(instanceId, commandString, commandOutputReceiver);
                    break; // exit retry loop on success
                } catch (JSchException e) {
                    if (!e.toString().contains("Connection refused: connect")) {
                        // rethrow and fail on other errors
                        throw testExecutionError(StringUtils.format("Failed to execute command \"%s\" on instance \"%s\": %s",
                            commandString, instanceId, e.toString()));
                    }
                }
                numAttempts++;
            }
            if (numAttempts >= maxAttempts) { // ">" should never happen; just for safety
                throw testExecutionError(
                    StringUtils.format("Failed to execute command \"%s\" on instance \"%s\": Maximum number of attempts exceeded",
                        commandString, instanceId));
            }
            if (numAttempts > 1) {
                String retrySuffix = " after retrying the SSH connection for " + (numAttempts - 1) + " times";
                printToCommandConsole(
                    StringUtils.format("  (Executed command \"%s\" on instance \"%s\"%s)", commandString, instanceId, retrySuffix));
            }
            String commandOutput = commandOutputReceiver.getBufferedOutput();
            instance.setLastCommandOutput(commandOutput);
            log.debug(StringUtils.format("Finished execution of command \"%s\" on instance \"%s\"", commandString, instanceId));
            return commandOutput;
        } catch (SshParameterException | IOException | InterruptedException | TimeoutException e) {
            throw testExecutionError(
                StringUtils.format("Failed to execute command \"%s\" on instance \"%s\": %s", commandString, instanceId, e.toString()));
        }

    }

    protected void iterateInstances(InstanceIterator instanceIterator, String allFlag, String instanceIds)
        throws OperationFailureException, IOException {
        for (final ManagedInstance instance : resolveInstanceList(allFlag != null, instanceIds)) {
            instanceIterator.iterateActionOverInstance(instance);
        }
    }

    /**
     * Performs an action on the provided instances concurrently, sequentially or in a given order.
     * 
     * @param action object of type InstanceAction containing the method to be performed on the instances.
     * @param instances List of instances on which the same action is to be performed.
     * @param executionMode indicating in which kind of execution the actions are to be performed.
     */
    protected void performActionOnInstances(InstanceAction action, List<ManagedInstance> instances,
        InstanceActionExecutionType executionMode) {
        switch (executionMode) {
        case CONCURRENT:
            final RunnablesGroup runnablesGroup = ConcurrencyUtils.getFactory().createRunnablesGroup();
            int staggeredStartDelayCounter = 0;
            for (final ManagedInstance instance : instances) {
                // make a final copy of the loop value
                final int staggeredStartDelayForInstance = staggeredStartDelayCounter;
                runnablesGroup.add(new Runnable() {

                    @Override
                    public void run() {
                        if (action.getClass().getSimpleName().endsWith("StartInstanceAction")) {
                            // a temporary workaround for an apparent Equinox framework issue that breaks concurrent starts
                            // (#18228) and which we can not fix directly; this should be replaced as soon as feasible
                            // as it makes the concurrent startup tests weaker / less aggressive (by design)
                            try {
                                Thread.sleep(staggeredStartDelayForInstance);
                            } catch (InterruptedException e) {
                                log.warn("Interrupted while waiting to initiate staggered start of instance " + instance.getId());
                                return;
                            }
                            try {
                                // TODO does not work in parallel execution context. Make accessible to multiple threads at the same time.
                                action.performActionOnInstance(instance,
                                    StepDefinitionConstants.IM_ACTION_TIMEOUT_IN_SECS * instances.size());
                            } catch (IOException | OperationFailureException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                });
                staggeredStartDelayCounter += CONCURRENT_INSTANCE_STARTING_STAGGERED_DELAY_MSEC;
            }
            executeRunnablesGroupAndHandlePotentialErrors(runnablesGroup, "performing action on instance");
            break;
        case ORDERED:
            for (ManagedInstance instance : instances) {
                try {
                    action.performActionOnInstance(instance, StepDefinitionConstants.IM_ACTION_TIMEOUT_IN_SECS * instances.size());
                } catch (OperationFailureException | IOException e) {
                    throw new RuntimeException(e);
                }
            }
            break;
        case RANDOM:
            Collections.shuffle(instances);
            for (ManagedInstance instance : instances) {
                try {
                    action.performActionOnInstance(instance, StepDefinitionConstants.IM_ACTION_TIMEOUT_IN_SECS * instances.size());
                } catch (OperationFailureException | IOException e) {
                    throw new RuntimeException(e);
                }
            }
            break;
        default:
            throw internalError("unknown execution mode");
        }
    }

    protected InstanceActionExecutionType resolveExecutionMode(String executionDesc, InstanceActionExecutionType fallback) {
        if (executionDesc == null) {
            if (fallback != null) {
                return fallback;
            }
            throw internalError(
                "executionDesc was set to null, which is not supported. Provide a fallback execution mode to circumvent this.");
        }

        switch (executionDesc) {
        case ("in the given order"):
            return InstanceActionExecutionType.ORDERED;
        case ("concurrently"):
            return InstanceActionExecutionType.CONCURRENT;
        case ("in a random order"):
            return InstanceActionExecutionType.RANDOM;
        default:
            if (fallback != null) {
                return fallback;
            }
            throw internalError(
                "executionDesc was set to an unsupported value. Provide a fallback execution mode to circumvent this.");
        }
    }

    protected InstanceActionExecutionType resolveExecutionMode(String executionDesc) {
        return resolveExecutionMode(executionDesc, null); // null = no fallback
    }

    protected final ManagedInstance resolveInstance(String instanceId) throws OperationFailureException {
        final ManagedInstance instanceIfPresent = executionContext.getInstanceFromId(instanceId);
        if (instanceIfPresent == null) {
            throw new OperationFailureException(StringUtils.format("Unregistered instance id '%s'", instanceId));
        }
        return instanceIfPresent;
    }

    /**
     * Returns a list of ManagedInstances depending on the parameter inputs. All | List 0 0 : returns all enabled instances 0 1 : returns
     * given list of instances 1 0 : returns all enabled instances 1 1 : fails
     * 
     * @param allFlag value regarding if all instances should be effected
     * @param instanceIds list of instances that should be effected
     * 
     * @return List of ManagedInstances
     * @throws OperationFailureException on execution failure (e.g. an invalid instance id)
     */
    protected List<ManagedInstance> resolveInstanceList(boolean allFlag, String instanceIds) throws OperationFailureException {
        List<ManagedInstance> instances;
        if (instanceIds == null) {
            instances = new ArrayList<ManagedInstance>(executionContext.getEnabledInstances());
        } else if (!allFlag) {
            instances = new ArrayList<ManagedInstance>();
            for (String instanceId : parseCommaSeparatedList(instanceIds)) {
                instances.add(resolveInstance(instanceId));
            }
        } else {
            // allFlag being set and a list of instance being provided is ambiguous and therefore not supported
            throw internalError("calling this operation for all instances and providing a list is not supported. Choose either one.");
        }
        return instances;
    }

    private void executeRunnablesGroupAndHandlePotentialErrors(final RunnablesGroup runnablesGroup, String singleTaskDescription) {
        final List<RuntimeException> exceptions = runnablesGroup.executeParallel();
        boolean hasFailure = false;
        for (RuntimeException e : exceptions) {
            if (e != null) {
                log.warn("Exception while asynchronously " + singleTaskDescription, e);
                hasFailure = true;
            }
        }
        if (hasFailure) {
            // rethrow an arbitrary one
            for (RuntimeException e : exceptions) {
                if (e != null) {
                    throw e;
                }
            }
        }
    }
}
