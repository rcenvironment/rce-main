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
import java.time.Clock;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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

    private final ClassLoader classLoader = getClass().getClassLoader();

    private Predicate<Pickle> tagFilters;

    private CucumberExecutionContext cucumberExecutionContext;

    private List<Feature> features;

    private ExecutorService executor;

    private PickleOrder pickleOrder;

    private Supplier<ClassLoader> classloaderSupplier = () -> classLoader;

    public enum ReportOutputFormat {

        /** Report formatted by cucumber "pretty" plugin. */
        PRETTY("pretty", ".txt"),
        /** Report formatted by cucumber "json" plugin. Deprecated in Cucumber. */
        JSON("json", ".json");

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
     * Simple container for execution result data.
     *
     * @author Robert Mischke
     */
    public static final class ExecutionResult {

        private List<String> reportFileLines;

        private List<String> capturedStdOutLines;

        public ExecutionResult(List<String> reportLines, List<String> capturedStdOutLines) {
            this.reportFileLines = reportLines;
            this.capturedStdOutLines = capturedStdOutLines;
        }

        public List<String> getReportFileLines() {
            return reportFileLines;
        }

        public List<String> getCapturedStdOutLines() {
            return capturedStdOutLines;
        }

    }

    public CucumberTestFrameworkAdapter(final Class<?>... stepDefinitions) {
        // TODO document rationale behind this
    }

    private void initialiseRuntime(ReportOutputFormat reportFormat, String reportDirUriString, String reportFileName,
        String tagNameFilter) {
        EventBus eventBus = synchronize(new TimeServiceEventBus(Clock.systemUTC(), UUID::randomUUID));
        File scriptLocationRoot =
//            new File("C:/RCEMain/rce-main/de.rcenvironment.supplemental.testscriptrunner.scripts/resources/scripts"); // remove

            new File("C:/RCEMain/rce-main/de.rcenvironment.supplemental.testscriptrunner.tests/src/test/resources/scripts"); // remove
                                                                                                                             // hard-coded
        String reportType = "html";
        String htmlReportPath = "target/report/";
        String htmlReportName = "Report";
        // path
        RuntimeOptionsBuilder cucumberRuntimeOptionsBuilder =
            new RuntimeOptionsBuilder().addGlue(GluePath.parse("de.rcenvironment.extras.testscriptrunner"))
                .setMonochrome(true)
                .setSnippetType(SnippetType.CAMELCASE).setPickleOrder(StandardPickleOrders.lexicalUriOrder())
                .addTagFilter(TagExpressionParser.parse("not @disabled or not @Disabled"))
                .addPluginName(StringUtils.format("%s:%s/%s", reportFormat.getFormatSpecifier(), reportDirUriString, reportFileName))
                .setObjectFactoryClass(PicoFactory.class).addPluginName("pretty")
                .addPluginName(reportType + ":" + htmlReportPath + htmlReportName + "." + reportType)
                .addFeature(FeatureWithLines.parse(scriptLocationRoot.getAbsolutePath()));
        if (!StringUtils.isNullorEmpty(tagNameFilter)) {
            // normalize filter parts and prepend "@" character
            final StringBuilder buffer = new StringBuilder();
            for (String filterPart : tagNameFilter.split(",")) {
                if (buffer.length() != 0) {
                    buffer.append(",");
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

        this.tagFilters = new Filters(cucumberRuntimeOptions);
        this.pickleOrder = cucumberRuntimeOptions.getPickleOrder();
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
        // Start test execution now.
        cucumberPlugins.setEventBusOnEventListenerPlugins(eventBus);
        executor = new SingleThreadedExecutionService();
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
        initialiseRuntime(reportFormat, reportDirUriString, reportFileName, tagNameFilter);
        ByteArrayOutputStream outputBuffer = new ByteArrayOutputStream();

        TestScenarioExecutionContext.setThreadLocalParameters(outputReceiver, buildUnderTestId, scriptLocationRoot);
        try {
            runFeatureFiles();
        } finally {
            TestScenarioExecutionContext.discardThreadLocalParameters();
        }
        if (reportFile.isFile()) {
            final List<String> reportLines = FileUtils.readLines(reportFile, Charsets.UTF_8); // TODO charset correct?
            final List<String> capturedStdOutLines =
                IOUtils.readLines(new ByteArrayInputStream(outputBuffer.toByteArray()), Charsets.UTF_8);
            return new ExecutionResult(reportLines, capturedStdOutLines);
        } else {
            return null;
        }
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

    private static final class SingleThreadedExecutionService extends AbstractExecutorService {

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
            System.out.println("Thread shutting down");

        }

    }

}
