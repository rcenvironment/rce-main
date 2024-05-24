/*
 * Copyright 2006-2023 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.extras.testscriptrunner.definitions.common;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.textstream.TextOutputReceiver;
import de.rcenvironment.core.utils.common.textstream.receivers.PrefixingTextOutForwarder;

/**
 * Common superclass for test step definitions, providing common infrastructure and utility methods.
 *
 * @author Robert Mischke
 */
public abstract class AbstractStepDefinitionBase {

    /**
     * The default retry wait time in msec used by {@link #executeWithRetry(ExecutionAttempt, int)}.
     */
    protected static final int DEFAULT_RETRY_DELAY = 1000;

    protected final TestScenarioExecutionContext executionContext;

    protected final TextOutputReceiver outputReceiver;

    protected final Log log = LogFactory.getLog(getClass());

    /**
     * A lambda interface for operations in a test step that should be executed in a retry loop.
     * 
     * The expected behavior of lambda blocks is:
     * <ul>
     * <li>Success - return true
     * <li>Temporary failure (retry) - return false
     * <li>Fatal or unhandled error (abort) - throw an AssertionError or an Exception
     * </ul>
     */
    @FunctionalInterface
    protected interface ExecutionAttempt {

        /**
         * Lambda definition for the execution of a single attempt within a retry loop.
         * 
         * @param attemptCount The index of the current attempt (1..maxAttempts)
         * @param isLastAttempt Whether the current attempt is the last one that will be performed
         * @return true on success, false for initiating a retry if possible
         * @throws AssertionError on non-retryable failure or unexpected error; aborts the retry loop
         * @throws Exception on non-retryable failure or unexpected error; aborts the retry loop
         */
        boolean attempt(int attemptCount, boolean isLastAttempt) throws AssertionError, Exception;
    }

    public AbstractStepDefinitionBase(TestScenarioExecutionContext executionContext) {
        this.executionContext = executionContext;
        this.outputReceiver = executionContext.getOutputReceiver();
    }

    /**
     * Executes the given lambda within a retry loop. Returns normally if an attempt was successful (returned true) and throws an exception
     * on unexpected or unhandled errors or exceeding the maximum retry count.
     * 
     * This method variant uses the default delay of {@link #DEFAULT_RETRY_DELAY} msec.
     * 
     * @param operation the operation to attempt
     * @param operationTitle the title to display for the attempted operation
     * @param maxAttempts the maximum number of times to try the operation, including the first one
     * 
     * @return the attempt on which the operation was successful (range 1..maxAttempts)
     * @throws AssertionError if the operation threw an unhandled exception or the maximum retry count was exceeded
     */
    protected final int executeWithRetry(ExecutionAttempt operation, String operationTitle, int maxAttempts) throws AssertionError {
        return executeWithRetry(operation, operationTitle, maxAttempts, DEFAULT_RETRY_DELAY);
    }

    /**
     * Executes the given lambda within a retry loop. Returns normally if an attempt was successful (returned true) and throws an exception
     * on unexpected or unhandled errors or exceeding the maximum retry count.
     * 
     * @param operation the operation to attempt
     * @param operationTitle the title to display for the attempted operation
     * @param maxAttempts the maximum number of times to try the operation, including the first one
     * @param delayMsec the custom delay (in msec) to use
     * 
     * @return the attempt on which the operation was successful (range 1..maxAttempts)
     * @throws AssertionError if the operation threw an unhandled exception or the maximum retry count was exceeded
     */
    protected final int executeWithRetry(ExecutionAttempt operation, String operationTitle, int maxAttempts, int delayMsec)
        throws AssertionError {
        for (int attemptCount = 1; attemptCount <= maxAttempts; attemptCount++) {
            try {
                if (operation.attempt(attemptCount, attemptCount == maxAttempts)) {
                    printToCommandConsole(StringUtils.format("Operation '%s' succeeded after %d attempt(s)",
                        operationTitle, attemptCount));
                    return attemptCount; // operation attempt returned "true" -> success
                }
                // fall-through: the operation attempt returned "false" -> continue with retry loop
            } catch (Exception e) { // TODO Checkstyle violation, but currently necessary as some operations declare "throws Exception"
                throw new AssertionError(StringUtils.format("Exception during attempt %d for operation '%s'",
                    attemptCount, operationTitle), e);
            }
        }
        throw new AssertionError(StringUtils.format("Exceeded the maximum retry count of %d for operation '%s', aborting",
            maxAttempts, operationTitle));
    }

    protected final void assertPropertyOfTextOutput(ManagedInstance instance, String negationFlag, String useRegexpMarker,
        String substring, final String output, final String outputType) {
        final boolean expectedPresence = (negationFlag == null); // true = step did NOT contain phrase "... should not contain ..."
        final boolean useRegexp = (useRegexpMarker != null); // step contained phrase "... the pattern ..."
        if (!useRegexp) {
            useRegexpMarker = ""; // prevent "null" in output below
        }
        final boolean found;
        if (useRegexp) {
            found = Pattern.compile(substring, Pattern.MULTILINE).matcher(output).find();
        } else {
            found = output.contains(substring);
        }
        if (expectedPresence && !found) {
            // on failure, write the examined output to a temp file and log its location as dumping a large file is slow in terminals
            fail(
                StringUtils.format("The %s of instance \"%s\" did not contain %s\"%s\";\n  saving the examined output as %s for inspection",
                    outputType, instance, useRegexpMarker, substring, writeOutputToTempFile(output)));
        }
        if (!expectedPresence && found) {
            // on failure, write the examined output to a temp file and log its location as dumping a large file is slow in terminals
            fail(StringUtils.format(
                "The %s of instance \"%s\" contained %s\"%s\" although it should not;\n  saving the examined output as %s for inspection",
                outputType, instance, useRegexpMarker, substring, writeOutputToTempFile(output)));
        }
        if (expectedPresence) {
            printToCommandConsole(
                StringUtils.format("  The %s of instance \"%s\" contained the expected text \"%s\"", outputType, instance, substring));
        } else {
            printToCommandConsole(
                StringUtils.format("  The %s of instance \"%s\" did not contain text \"%s\" (as expected)",
                    outputType, instance, substring));
        }
    }

    protected List<String> listOfSingleStringElement(String element) {
        List<String> singleInstanceList = new ArrayList<>();
        singleInstanceList.add(element);
        return singleInstanceList;
    }

    /**
     * Parses comma separated list to List of Strings removing whitespaces.
     */
    protected final List<String> parseCommaSeparatedList(String commaSeparatedList) {
        if (commaSeparatedList == null) {
            return new LinkedList<String>();
        }
        return Arrays.asList(commaSeparatedList.trim().split("\\s*,\\s*"));
    }

    protected final void printToCommandConsole(String text) {
        outputReceiver.addOutput(text);
    }

    protected final boolean stringContainsOrContainsNot(String string, String substring, boolean shouldContain, boolean useRegex) {
        final boolean found;
        if (useRegex) {
            found = Pattern.compile(substring, Pattern.MULTILINE).matcher(string).find();
        } else {
            found = string.contains(substring);
        }
        return shouldContain == found;
    }

    /**
     * @return the location of the generated temp file
     * @throws IOException
     */
    protected final String writeOutputToTempFile(final String output) {
        try {
            final File tempFile = File.createTempFile("bdd_test_failure_data", ".txt");
            FileUtils.write(tempFile, output);
            return tempFile.getAbsolutePath();
        } catch (IOException e) {
            fail("Unexpected error writing temp file: " + e.toString());
            return null; // never reached
        }
    }

    protected PrefixingTextOutForwarder getTextoutReceiverForIMOperations() {
        return new PrefixingTextOutForwarder("  (IM output) ", outputReceiver);
    }

}
