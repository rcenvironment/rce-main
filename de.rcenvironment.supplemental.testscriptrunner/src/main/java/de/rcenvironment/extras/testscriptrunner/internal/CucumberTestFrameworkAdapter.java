/*
 * Copyright 2006-2023 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.extras.testscriptrunner.internal;

import static io.cucumber.core.runtime.SynchronizedEventBus.synchronize;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

import de.rcenvironment.core.configuration.bootstrap.BootstrapConfiguration;
import de.rcenvironment.core.utils.common.OSFamily;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.textstream.TextOutputReceiver;
import de.rcenvironment.extras.testscriptrunner.definitions.common.TestScenarioExecutionContext;
import io.cucumber.core.eventbus.EventBus;
import io.cucumber.core.feature.FeatureParser;
import io.cucumber.core.feature.FeatureWithLines;
import io.cucumber.core.feature.GluePath;
import io.cucumber.core.filter.Filters;
import io.cucumber.core.gherkin.Feature;
import io.cucumber.core.gherkin.Pickle;
import io.cucumber.core.options.RuntimeOptions;
import io.cucumber.core.options.RuntimeOptionsBuilder;
import io.cucumber.core.order.PickleOrder;
import io.cucumber.core.order.StandardPickleOrders;
import io.cucumber.core.plugin.PluginFactory;
import io.cucumber.core.plugin.Plugins;
import io.cucumber.core.runtime.BackendServiceLoader;
import io.cucumber.core.runtime.CucumberExecutionContext;
import io.cucumber.core.runtime.ExitStatus;
import io.cucumber.core.runtime.FeaturePathFeatureSupplier;
import io.cucumber.core.runtime.ObjectFactoryServiceLoader;
import io.cucumber.core.runtime.ObjectFactorySupplier;
import io.cucumber.core.runtime.RunnerSupplier;
import io.cucumber.core.runtime.SingletonObjectFactorySupplier;
import io.cucumber.core.runtime.SingletonRunnerSupplier;
import io.cucumber.core.runtime.TimeServiceEventBus;
import io.cucumber.core.snippets.SnippetType;
import io.cucumber.picocontainer.PicoFactory;
import io.cucumber.plugin.event.EventHandler;
import io.cucumber.plugin.event.PickleStepTestStep;
import io.cucumber.plugin.event.Result;
import io.cucumber.plugin.event.Status;
import io.cucumber.plugin.event.TestCaseFinished;
import io.cucumber.plugin.event.TestRunFinished;
import io.cucumber.plugin.event.TestStepFinished;
import io.cucumber.tagexpressions.TagExpressionParser;

/**
 * A wrapper around the Cucumber BDD test framework to encapsulate various setup, classloader and file location issues. It is intended to
 * make test scripts runnable from plain (non-OSGi) unit tests, a RCE command plugin embedded in an RCE launch from Eclipse, and the same
 * command plugin when installed into a standalone RCE product.
 *
 * @author Robert Mischke
 * @author Alexander Weinert (Sonar cleanup, support for multiple output formats)
 * @author Devika Jalgaonkar (#17806)
 */
public class CucumberTestFrameworkAdapter {

    private final Log log = LogFactory.getLog(getClass());

    private final List<URL> classFileUrlsForRedirectingGlueCodeSearch;

    public enum ReportOutputFormat {

        /** Report formatted by cucumber "pretty" plugin. */
        PRETTY("pretty", ".txt"),
        /** Report formatted by cucumber "json" plugin. */
        JSON("json", ".json"),
        /** Report formatted by cucumber "html" plugin. */
        HTML("html", ".html");

        private final String formatSpecifier;

        private final String reportFileSuffix;

        ReportOutputFormat(String formatSpecifierParam, String reportFileSuffixParam) {
            this.formatSpecifier = formatSpecifierParam;
            this.reportFileSuffix = reportFileSuffixParam;
        }

        public String getFormatSpecifier() {
            return this.formatSpecifier;
        }

        public String getReportFileSuffix() {
            return this.reportFileSuffix;
        }
    }

    /**
     * Test Execution result status.
     * 
     * @author Devika Jalgaonkar
     */
    public enum ExecutionResultStatus {

        /** SUCCESSFUL only if all scenarios (test) have Cucumber status PASSED. */
        SUCCESSFUL,
        /** UNSUCCESSFUL if any scenario has cucumber status UNDEFINED, FAILED, SKIPPED, AMBIGUOUS, PENDING. */
        UNSUCCESSFUL;
    }

    /**
     * Simple container for execution result data.
     *
     * @author Robert Mischke
     */
    public static final class ExecutionResult {

        private List<String> reportFileLines;

        private List<String> capturedStdOutLines;

        private ExecutionResultStatus status;

        public ExecutionResult(List<String> reportLines, List<String> capturedStdOutLines, ExecutionResultStatus status) {
            this.reportFileLines = reportLines;
            this.capturedStdOutLines = capturedStdOutLines;
            this.status = status;

        }

        public List<String> getReportFileLines() {
            return reportFileLines;
        }

        public List<String> getCapturedStdOutLines() {
            return capturedStdOutLines;
        }

        public ExecutionResultStatus getExecutionResultStatus() {
            return status;
        }

    }

    /**
     * Container for execution result summary.
     *
     * @author Devika Jalgaonkar
     */
    public class TestStatistics {

        /** Total test-cases with Cucumber PASSED status. */
        public int passedCount = 0;

        /** Total test-cases with Cucumber FAILED status. */
        public int failedCount = 0;

        /** Total test-cases with Cucumber AMBIGUOUS status. */
        public int ambiguousCount = 0;

        /** Total test-cases with Cucumber SKIPPED status. */
        public int skippedCount = 0;

        /** Total test-cases with Cucumber PENDING status. */
        public int pendingCount = 0;

        /** Total test-cases with Cucumber UNDEFINED status. */
        public int undefinedCount = 0;

        /** Total no. of test-cases. */
        public int totalCount = 0;

        void printSummary(TextOutputReceiver outputReceiver, String prefix) {
            final int unsuccessfulCount = totalCount - passedCount;
            if (unsuccessfulCount == 0) {
                outputReceiver.addOutput(
                    StringUtils.format("%s%d passed, 0 unsuccessful or skipped", prefix, passedCount));
            } else {
                outputReceiver.addOutput(
                    StringUtils.format(
                        "%s%d passed, %d unsuccessful or skipped (%d failed, %d skipped, %d pending, %d undefined, %d ambiguous)",
                        prefix, passedCount, unsuccessfulCount,
                        passedCount, failedCount, skippedCount, pendingCount, undefinedCount, ambiguousCount));
            }
        }

    }

    /**
     * A holder for all data that is related to a specific invocation, and data-related execution methods. This class makes
     * {@link CucumberTestFrameworkAdapter} thread safe again after the Cucumber 7.x upgrade.
     * 
     * @author Robert Mischke
     */
    private final class ExecutionContext {

        private final EventHandler<TestStepFinished> cucumberTestStepFinishedHandler = this::cucumberTestStepFinished;

        private final EventHandler<TestCaseFinished> cucumberTestCaseFinishedHandler = this::cucumberTestCaseResult;

        private final EventHandler<TestRunFinished> cucumberTestRunFinishedHandler = this::cucumberTestRunFinished;

        private Map<String, String> unsuccessfulScenarios = new HashMap<String, String>();

        private final class ClassLoaderWrapper extends ClassLoader {

            /**
             * A wrapper to intercept and rewrite certain Cucumber classloader calls that fail in an OSGi environment.
             * 
             * @author Robert Mischke
             */
            private ClassLoaderWrapper(ClassLoader parent) {
                super(parent);

            }

            @Override
            public Enumeration<URL> getResources(String name) throws IOException {
                // intercept the resource-to-URL resolution which normally returns a bundle URL, which cannot be used by Cucumber

                Enumeration<URL> originalResult = super.getResources(name);
                List<URL> originalResultAsList = new ArrayList<>();
                // IMPORTANT: this consumes the original enumeration, so it cannot be returned anymore
                while (originalResult.hasMoreElements()) {
                    originalResultAsList.add(originalResult.nextElement());
                }
                originalResult = null; // avoid accidental usage

                List<URL> resultList = new ArrayList<>();
                log.debug("Original classloader result of getResources(\"" + name + "\"):");
                AtomicBoolean modified = new AtomicBoolean();
                originalResultAsList.forEach((url) -> {
                    log.debug("  " + url);
                    if ("bundleresource".equals(url.getProtocol()) && !url.getPath().contains("META-INF")) {
                        // cucumber cannot handle this kind of URL for searching for glue code classes, so we need to rewrite it
                        if (classFileUrlsForRedirectingGlueCodeSearch.isEmpty()) {
                            log.error("No URL registered to redirect the glue code class file search to");
                        }
                        // only add the redirect URLs once
                        if (!modified.get()) {
                            resultList.addAll(classFileUrlsForRedirectingGlueCodeSearch);
                            modified.set(true);
                        }
                    } else {
                        // keep original result
                        resultList.add(url);
                    }
                });

                if (modified.get()) {
                    log.debug("Result substituted with:");
                    resultList.forEach((u) -> log.debug("  " + u));
                }
                return Collections.enumeration(resultList);
            }
        }

        private final ReportOutputFormat reportFormat;

        private final String reportDirUriString;

        private final String reportFileName;

        private final String tagNameFilter;

        private final File scriptLocationRoot;

        private final Supplier<ClassLoader> classloaderSupplier;

        // set by initializeCucumberRuntime()
        private Filters tagFilters;

        // set by initializeCucumberRuntime()
        private PickleOrder pickleOrder;

        // set by initializeCucumberRuntime()
        private CucumberExecutionContext cucumberExecutionContext;

        // set by initializeCucumberRuntime()
        private List<Feature> features;

        // set by initializeCucumberRuntime()
        private ExecutorService executor;

        private TextOutputReceiver outputReceiver;

        private TestStatistics scenarioStatistics = new TestStatistics();

        private TestStatistics stepStatistics = new TestStatistics();

        // set by cucumberFinishTestRun
        private ExecutionResultStatus executionResultStatus;

        private ExecutionContext(ReportOutputFormat reportFormat, String reportDirUriString, String reportFileName, String tagNameFilter,
            File scriptLocationRoot, TextOutputReceiver outputReceiver) {
            this.reportFormat = reportFormat;
            this.reportDirUriString = reportDirUriString;
            this.reportFileName = reportFileName;
            this.tagNameFilter = tagNameFilter;
            this.scriptLocationRoot = scriptLocationRoot;
            this.outputReceiver = outputReceiver;

            // patch the OSGi classloader by wrapping it
            ClassLoader upstreamClassLoader = getClass().getClassLoader();
            ClassLoader classLoader = new ClassLoaderWrapper(upstreamClassLoader);
            classloaderSupplier = () -> classLoader;

            initializeCucumberRuntime();
        }

        private void initializeCucumberRuntime() {

            EventBus eventBus = synchronize(new TimeServiceEventBus(Clock.systemUTC(), UUID::randomUUID));

            String subdirForBDDReportFiles = "rce-bdd-reports";
            if (OSFamily.isLinux()) {
                subdirForBDDReportFiles += "-" + System.getProperty("user.name");
            }
            final File reportFolder = new File(System.getProperty("java.io.tmpdir"), subdirForBDDReportFiles);

            // set report format - pretty by default

            // CURRENT BEHAVIOUR - generates both HTML and PRETTY/JSON(depending on command)
            String cucumberReportFormatter = "";
            String cucumberReportFileSuffix = "";

            if (reportFormat.equals(ReportOutputFormat.PRETTY)) {
                cucumberReportFormatter = ReportOutputFormat.PRETTY.getFormatSpecifier();
                cucumberReportFileSuffix = ReportOutputFormat.PRETTY.getReportFileSuffix();
            } else if (reportFormat.equals(ReportOutputFormat.JSON)) {
                cucumberReportFormatter = ReportOutputFormat.JSON.getFormatSpecifier();
                cucumberReportFileSuffix = ReportOutputFormat.JSON.getReportFileSuffix();
            }
            long unixTime = Instant.now().getEpochSecond();
            final String cucumberReportBaseName = "bdd-report-" + unixTime;

            final File cucumberHtmlReportFile = new File(reportFolder, cucumberReportBaseName + ".html").getAbsoluteFile();
            final File cucumberRequestedFormatReportFile =
                new File(reportFolder, cucumberReportBaseName + cucumberReportFileSuffix).getAbsoluteFile();

            // store the path of the most recent BDD report file in a hardcoded file to allow automated, non-parallel processes
            // (especially CI) to access it easily; not using symlinks to avoid permission issues on Windows
            try {
                FileUtils.writeStringToFile(new File(reportFolder, "bdd-report-latest-path-html"), cucumberHtmlReportFile.toString(),
                    StandardCharsets.UTF_8);
            } catch (IOException e) {
                log.error("Failed to store report file location", e);
            }
            outputReceiver.addOutput("Location of HTML report file: " + cucumberHtmlReportFile.toString());
            outputReceiver.addOutput("Location of " + reportFormat + " report file: " + cucumberRequestedFormatReportFile.toString());

            eventBus.registerHandlerFor(TestStepFinished.class, cucumberTestStepFinishedHandler);
            eventBus.registerHandlerFor(TestCaseFinished.class, cucumberTestCaseFinishedHandler);
            eventBus.registerHandlerFor(TestRunFinished.class, cucumberTestRunFinishedHandler);
            RuntimeOptionsBuilder cucumberRuntimeOptionsBuilder =
                new RuntimeOptionsBuilder().addGlue(GluePath.parse("de.rcenvironment.extras.testscriptrunner"))
                    .setMonochrome(true)
                    .setSnippetType(SnippetType.CAMELCASE).setPickleOrder(StandardPickleOrders.lexicalUriOrder())
                    .addTagFilter(TagExpressionParser.parse("not @disabled or not @Disabled"))
                    .addPluginName(StringUtils.format("%s:%s/%s", reportFormat.getFormatSpecifier(),
                        reportDirUriString, reportFileName))
                    .setObjectFactoryClass(PicoFactory.class).addPluginName("pretty").addDefaultSummaryPrinterIfNotDisabled()
                    .addPluginName(ReportOutputFormat.HTML.getFormatSpecifier() + ":" + cucumberHtmlReportFile.toString())
                    .addPluginName(cucumberReportFormatter + ":" + cucumberRequestedFormatReportFile.toString())
                    .addFeature(FeatureWithLines.parse(scriptLocationRoot.getAbsolutePath()));
            if ((!StringUtils.isNullorEmpty(tagNameFilter)) && !tagNameFilter.equals("--all")) {
                // normalize filter parts and prepend "@" character
                final StringBuilder buffer = new StringBuilder();
                for (String filterPart : tagNameFilter.split(",")) {
                    if (buffer.length() != 0) {
                        buffer.append(" or ");
                    }
                    final String trimmedPart = filterPart.trim();
                    if (!trimmedPart.startsWith("@")) {
                        buffer.append("@");
                    }
                    buffer.append(trimmedPart);
                }
                if (buffer.length() != 0) {
                    cucumberRuntimeOptionsBuilder.addTagFilter(TagExpressionParser.parse(buffer.toString()));
                }
            }
            RuntimeOptions cucumberRuntimeOptions = cucumberRuntimeOptionsBuilder.build();
            Supplier<UUID> uuidSupplier = () -> eventBus.generateId();

            FeatureParser featureParser = new FeatureParser(uuidSupplier);
            FeaturePathFeatureSupplier featureSupplier = new FeaturePathFeatureSupplier(classloaderSupplier,
                cucumberRuntimeOptions, featureParser);
            features = featureSupplier.get();

            tagFilters = new Filters(cucumberRuntimeOptions);
            pickleOrder = cucumberRuntimeOptions.getPickleOrder();
            ExitStatus exitStatus = new ExitStatus(cucumberRuntimeOptions);
            Plugins cucumberPlugins = new Plugins(new PluginFactory(), cucumberRuntimeOptions);

            cucumberPlugins.addPlugin(exitStatus);
            ObjectFactoryServiceLoader cucumberObjectFactoryServiceLoader = new ObjectFactoryServiceLoader(
                classloaderSupplier, cucumberRuntimeOptions);
            ObjectFactorySupplier cucumberObjectFactorySupplier = new SingletonObjectFactorySupplier(cucumberObjectFactoryServiceLoader);
            BackendServiceLoader cucumberBackendSupplier = new BackendServiceLoader(classloaderSupplier,
                cucumberObjectFactorySupplier);

            RunnerSupplier cucumberRunnerSupplier = new SingletonRunnerSupplier(cucumberRuntimeOptions, eventBus, cucumberBackendSupplier,
                cucumberObjectFactorySupplier);
            cucumberExecutionContext = new CucumberExecutionContext(eventBus, exitStatus, cucumberRunnerSupplier);
            cucumberPlugins.setEventBusOnEventListenerPlugins(eventBus);
            executor = new SingleThreadedExecutionService();
        }

        private void cucumberTestRunFinished(TestRunFinished event) {

            final String textSeparatorLine =
                "-----------------------------------------------------------------------------------------------";
            outputReceiver.addOutput(textSeparatorLine);
            outputReceiver.addOutput("Test Run Summary:");
            outputReceiver.addOutput(textSeparatorLine);
            scenarioStatistics.printSummary(outputReceiver, "Scenario counts: ");
            stepStatistics.printSummary(outputReceiver, "Step counts: ");
            if (scenarioStatistics.totalCount > 0 && (scenarioStatistics.totalCount == scenarioStatistics.passedCount)) {
                executionResultStatus = ExecutionResultStatus.SUCCESSFUL;
                outputReceiver.addOutput("All scenarios passed successfully");
            } else {
                executionResultStatus = ExecutionResultStatus.UNSUCCESSFUL;
                outputReceiver.addOutput("Unsuccessful scenarios:");

                for (Entry<String, String> unsuccessfulScenario : unsuccessfulScenarios.entrySet()) {
                    outputReceiver.addOutput("- " + unsuccessfulScenario.getKey() + ": " + unsuccessfulScenario.getValue());
                }
            }
            outputReceiver.addOutput(textSeparatorLine);
        }

        private void cucumberTestStepFinished(TestStepFinished event) {
            if (event.getTestStep() instanceof PickleStepTestStep) {
                stepStatistics.totalCount++;
                PickleStepTestStep testStep = (PickleStepTestStep) event.getTestStep();
                cucumberStepResult(testStep, event.getResult());
            }
        }

        private void cucumberStepResult(PickleStepTestStep testStep, Result result) {
            Status testStepResult = result.getStatus();
            if (testStepResult.equals(Status.SKIPPED)) {
                stepStatistics.skippedCount++;
            } else if (testStepResult.equals(Status.UNDEFINED)) {
                stepStatistics.undefinedCount++;
            } else if (testStepResult.equals(Status.FAILED)) {
                stepStatistics.failedCount++;
            } else if (testStepResult.equals(Status.AMBIGUOUS)) {
                stepStatistics.ambiguousCount++;
            } else if (testStepResult.equals(Status.PENDING)) {
                stepStatistics.pendingCount++;
            } else if (testStepResult.equals(Status.PASSED)) {
                stepStatistics.passedCount++;
            } else {
                log.warn("Ignoring unhandled test step outcome " + testStepResult);
            }

        }

        private void cucumberTestCaseResult(TestCaseFinished scenario) {
            scenarioStatistics.totalCount++;
            Status testCaseStatus = scenario.getResult().getStatus();
            if (testCaseStatus.equals(Status.PASSED)) {
                scenarioStatistics.passedCount++;
            } else {
                unsuccessfulScenarios.put(scenario.getTestCase().getName(), scenario.getResult().getStatus().toString());
                if (testCaseStatus.equals(Status.SKIPPED)) {
                    scenarioStatistics.skippedCount++;
                    log.info("Skipped");
                } else if (testCaseStatus.equals(Status.UNDEFINED)) {
                    scenarioStatistics.undefinedCount++;
                    log.error("Undefined Scenario");
                } else if (testCaseStatus.equals(Status.FAILED)) {
                    scenarioStatistics.failedCount++;
                    log.error("Failed Scenario");
                } else if (testCaseStatus.equals(Status.AMBIGUOUS)) {
                    scenarioStatistics.ambiguousCount++;
                    log.error("Ambiguous Scenario");
                } else if (testCaseStatus.equals(Status.PENDING)) {
                    scenarioStatistics.pendingCount++;
                    log.error("Pending Scenario");
                }
            }
        }

        private ExecutionResultStatus getExecutionResultStatus() {
            return executionResultStatus;
        }

        private void runFeatureFiles() {
            cucumberExecutionContext.runFeatures(() -> runFeatureFiles(features));

        }

        private Runnable runPickle(Pickle pickleToRun) {
            return () -> cucumberExecutionContext.runTestCase(cucumberRunner -> cucumberRunner.runPickle(pickleToRun));
        }

        private void runFeatureFiles(List<Feature> featuresList) {
            featuresList.forEach(cucumberExecutionContext::beforeFeature);
            List<Future<?>> pickleList = featuresList.stream().flatMap(feature -> feature.getPickles().stream()).filter(tagFilters)
                .collect(collectingAndThen(
                    toList(), orderedPickleList -> pickleOrder.orderPickles(orderedPickleList).stream()))
                .map(pickle -> executor.submit(runPickle(pickle))).collect(toList());

            executor.shutdown();

            for (Future<?> pickle : pickleList) {
                try {
                    pickle.get();
                } catch (ExecutionException e) {
                    log.error(e);
                } catch (InterruptedException e) {
                    log.error(e);
                    executor.shutdownNow();
                }
            }
        }
    }

    public CucumberTestFrameworkAdapter() {
        // generate replacement URLs to support the way Cucumber 7.x loads glue code class files
        this.classFileUrlsForRedirectingGlueCodeSearch = new ArrayList<>();
        Bundle containingBundle = FrameworkUtil.getBundle(getClass());
        if (containingBundle != null) {
            String locationInfo = containingBundle.getLocation();
            log.debug("Original OSGi bundle location value: " + locationInfo);
            // strip the "initial@" part of "initial@reference:file:" locations that occur during Tycho Surefire tests
            if (locationInfo.startsWith("initial@")) {
                locationInfo = locationInfo.substring("initial@".length());
            }
            if (!locationInfo.startsWith("reference:file:")) {
                throw new RuntimeException("Unexpected bundle location format (expected a 'reference:file:' prefix): " + locationInfo);
            }
            if (locationInfo.endsWith("/")) {
                // when run from Eclipse, locationInfo has the format
                // "reference:file:<...>/de.rcenvironment.supplemental.testscriptrunner/"
                String rewrittenUrlString =
                    locationInfo.replace("reference:", "") + "target/classes/de/rcenvironment/extras/testscriptrunner";
                log.debug("Apparently running from an IDE; using rewritten class files location " + rewrittenUrlString);
                // in this case, add both the main and self-test glue code locations
                try {
                    this.classFileUrlsForRedirectingGlueCodeSearch.add(new URL(rewrittenUrlString));
                    this.classFileUrlsForRedirectingGlueCodeSearch
                        .add(new URL(rewrittenUrlString.replace("testscriptrunner/", "testscriptrunner.tests/")));
                } catch (MalformedURLException e) {
                    throw new RuntimeException("Failed to recreate URL from string " + rewrittenUrlString);
                }
            } else if (locationInfo.endsWith(".jar")) {
                // when run from a standalone build, locationInfo has the format
                // "reference:file:plugins/de.rcenvironment.supplemental.testscriptrunner_<...>.jar";
                // unfortunately, the path is relative, so for robustness, we need to resolve it against the installation dir
                String relativePathPart = locationInfo.replace("reference:file:", "");
                File installationDir = BootstrapConfiguration.getInstallationDir();
                File absolutePath = new File(installationDir, relativePathPart);
                if (!absolutePath.isFile()) {
                    throw new RuntimeException(
                        "Derived the absolute bundle path " + absolutePath + ", but it does not point at a JAR file");
                }
                final URI jarFileUri = absolutePath.toURI();
                final String assembledJarUrlStringWithInternalPath =
                    "jar:" + jarFileUri.toString() + "!/de/rcenvironment/extras/testscriptrunner";
                log.debug("Apparently running from a standalone build, rewriting class file search URL to "
                    + assembledJarUrlStringWithInternalPath);
                try {
                    this.classFileUrlsForRedirectingGlueCodeSearch.add(new URL(assembledJarUrlStringWithInternalPath));
                    // unlike the Eclipse case, do not add a reference to the self-test bundle as it is absent in the standalone build
                } catch (MalformedURLException e) {
                    throw new RuntimeException("Failed to recreate URL from URI " + assembledJarUrlStringWithInternalPath);
                }
            } else {
                throw new RuntimeException("Unrecognized bundle location format: " + locationInfo);
            }
        }
    }

    /**
     * Executes a number of test scenarios loaded from "feature" files (see Cucumber docs for terminology).
     * 
     * @param scriptLocationRoot the directory that is scanned for .feature files
     * @param tagNameSelection a comma-separated list of tags to include in the test run (joined by logical OR); an empty or null list
     *        executes all tests
     * @param outputReceiver the {@link TextOutputReceiver} for test script output
     * @param buildUnderTestId the id of the build to test; injected into the {@link TestScenarioExecutionContext} for consumption by step
     *        definitions
     * @param reportDir the directory to write the generated reports to
     * @return an {@link ExecutionResult} containing: 1. the generated report file's lines, or null if no report was generated; 2. the
     *         captured StdOut lines (never null, but could in theory be empty)
     * @throws IOException on I/O errors
     */
    public ExecutionResult executeTestScripts(File scriptLocationRoot, String tagNameSelection, TextOutputReceiver outputReceiver,
        String buildUnderTestId, File reportDir) throws IOException {
        return executeTestScripts(scriptLocationRoot, tagNameSelection, outputReceiver, buildUnderTestId, reportDir,
            ReportOutputFormat.PRETTY);
    }

    public ExecutionResult executeTestScripts(File scriptLocationRoot, String tagNameFilter, TextOutputReceiver outputReceiver,
        String buildUnderTestId, File reportDir, ReportOutputFormat reportFormat) throws IOException {

        // TODO (p2) check whether this can be reworked to use an individual file per run; this would enable parallel runs
        final String reportDirUriString = reportDir.toURI().toASCIIString();
        final String reportFileName = "plain" + reportFormat.getReportFileSuffix();
        File reportFile = new File(reportDir, reportFileName);
        if (reportFile.isFile()) {
            reportFile.delete(); // NOSONAR ignore return value; result is checked below, along with potential existence as directory
        }
        if (reportFile.isFile()) {
            throw new IOException("Failed to delete pre-existing report file " + reportFile.getAbsolutePath());
        }
        ExecutionContext executionContext =
            new ExecutionContext(reportFormat, reportDirUriString, reportFileName, tagNameFilter, scriptLocationRoot, outputReceiver);

        // TODO check: it looks like this is never attached to any data source, only consumed
        PrintStream oldStdOut = System.out;
        ByteArrayOutputStream outputBuffer = new ByteArrayOutputStream();
        final PrintStream outputWriter = new PrintStream(outputBuffer, false, "UTF-8");
        System.setOut(outputWriter);
        TestScenarioExecutionContext.setThreadLocalParameters(outputReceiver, buildUnderTestId, scriptLocationRoot);

        try {
            executionContext.runFeatureFiles();
        } finally {
            TestScenarioExecutionContext.discardThreadLocalParameters();
        }
        System.setOut(oldStdOut);
        outputWriter.close();

        if (reportFile.isFile()) {
            final List<String> reportLines = FileUtils.readLines(reportFile, Charsets.UTF_8); // TODO charset correct?
            final List<String> capturedStdOutLines =
                IOUtils.readLines(new ByteArrayInputStream(outputBuffer.toByteArray()), Charsets.UTF_8);
            final ExecutionResultStatus executionResultStatus = executionContext.getExecutionResultStatus();
            return new ExecutionResult(reportLines, capturedStdOutLines, executionResultStatus);
        } else {
            return null;
        }
    }

    // TODO consider moving this into ExecutionContext, potentially as its constructor

    private final class SingleThreadedExecutionService extends AbstractExecutorService {

        @Override
        public void execute(Runnable commandd) {
            commandd.run();
        }

        @Override
        public List<Runnable> shutdownNow() {
            return Collections.emptyList();
        }

        @Override
        public boolean isShutdown() {
            return true;
        }

        @Override
        public boolean isTerminated() {
            return true;
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) {
            return true;
        }

        @Override
        public void shutdown() {
            log.debug("Cucumber execution thread shutting down");
        }

    }

}
