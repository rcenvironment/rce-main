/*
 * Copyright 2006-2023 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.scripting.python;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Writer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;
import de.rcenvironment.toolkit.modules.concurrency.api.AsyncCallbackExceptionPolicy;
import de.rcenvironment.toolkit.modules.concurrency.api.AsyncOrderedExecutionQueue;

/**
 * A specialized {@link Writer} for capturing the output of Jython, which does not seem to call {@link Writer#close()} properly. It relies
 * on the fact that Jython calls {@link Writer#flush()} at the end of each line. Each line is forwarded to the appropriate notification
 * call. If {@link Writer#close()} is called from own code, {@link #CONSOLE_END} must be written at the end of the console output to forward
 * as {@link PythonOutputWriter#close()} waits until this line is captured. This line won't be forwarded.
 * 
 * @author Robert Mischke
 * @author Sascha Zur
 * @author Doreen Seider
 */
public abstract class PythonOutputWriter extends Writer {

    /**
     * Indicates the last console line sent. Must be sent by each consumer after the last productive line.
     */
    public static final String CONSOLE_END = "c02abd1c-67bc-4974-902b-439cd2b14efc";

    /**
     * Timeout used when waiting for {@link #CONSOLE_END}.
     */
    public static final int WAIT_FOR_CONSOLE_END_TIMEOUT_IN_MILI_SEC = 5000;

    protected final AsyncOrderedExecutionQueue executionQueue;

    private final Log log = LogFactory.getLog(getClass());

    private final StringBuilder buffer = new StringBuilder();

    private FileOutputStream logFileStream;

    private final Object logFileLock = new Object();

    private boolean isClosed;

    private final CountDownLatch finishLatch = new CountDownLatch(1);

    public PythonOutputWriter(Object lock, File logFile) {
        super(lock);
        isClosed = false; // redundant, but added to make it explicit
        executionQueue = ConcurrencyUtils.getFactory().createAsyncOrderedExecutionQueue(AsyncCallbackExceptionPolicy.LOG_AND_PROCEED);

        if (logFile != null) {
            try {
                logFileStream = new FileOutputStream(logFile);
            } catch (FileNotFoundException e) {
                LogFactory.getLog(getClass()).error("Creating stream for given log file failed", e);
            }
        }
    }

    protected abstract void onNewLineToForward(final String line);

    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
        synchronized (lock) {
            buffer.append(cbuf, off, len);
        }
    }

    @Override
    public void flush() throws IOException {
        synchronized (lock) {
            String line = buffer.toString();
            buffer.setLength(0);
            if (isClosed) {
                if (!line.isEmpty()) {
                    log.warn("Unexpected caller behavior: flush() called after close(); captured output=" + line);
                }
            } else {
                if (line.contains(CONSOLE_END)) {
                    // not forwarded as an empty line would be forwarded as well, which is generated by sys.stderr/stdout.flush() for some
                    // reason. we call both of the commands at the end of each Jython script
                    // forwardLineAsConsoleRow(compInfo, line.replace(CONSOLE_END, ""));
                    isClosed = true;
                    forwardLine(null);
                    finishLatch.countDown();
                } else {
                    forwardLine(line);
                }

            }

        }
    }

    protected void forwardLine(final String line) {

        executionQueue.enqueue(new Runnable() {

            @Override
            public void run() {
                synchronized (logFileLock) {
                    if (logFileStream != null && line != null) {
                        try {
                            logFileStream.write(line.getBytes());
                        } catch (IOException e) {
                            log.error("Writing output to file failed", e);
                        }
                    }
                }
                onNewLineToForward(line);
            }
        });
    }

    @Override
    public void close() throws IOException {
        try {
            finishLatch.await(WAIT_FOR_CONSOLE_END_TIMEOUT_IN_MILI_SEC, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            log.error(
                "Waiting for last console line was interrupted. Writer instance will be closed immediately. Console lines might be lost");
        }
        synchronized (logFileLock) {
            if (logFileStream != null) {
                logFileStream.close();
                logFileStream = null;
            }
        }
        synchronized (lock) {
            isClosed = true;
        }
    }

}
