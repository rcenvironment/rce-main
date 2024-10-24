/*
 * Copyright 2006-2023 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.utils.common.textstream.receivers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.utils.common.textstream.TextOutputReceiver;

/**
 * A simple implementation of {@link TextOutputReceiver} that logs all received events to an Apache Commons logger, with an optional prefix
 * set through the constructor.
 * 
 * This class is mainly intended for unit/integration test output. The log level is currently hard-coded to "debug".
 * 
 * @author Robert Mischke
 */
public class LoggingTextOutReceiver implements TextOutputReceiver {

    private final Log log;

    private final String logPrefix;

    /**
     * Creates an instance that appends to a log with its own, internal class name (when the log category is irrelevant, or when no better
     * log instance is available on the calling side).
     * 
     * @param logPrefix the prefix for all log messages
     */
    public LoggingTextOutReceiver(String logPrefix) {
        this.log = LogFactory.getLog(getClass());
        this.logPrefix = logPrefix;
    }

    /**
     * Creates an instance that appends to the given log instance.
     * 
     * @param log the externally created log instance to append to
     * @param logPrefix the prefix for all log messages
     */
    public LoggingTextOutReceiver(Log log, String logPrefix) {
        this.log = log;
        this.logPrefix = logPrefix;
    }

    @Override
    public void onStart() {
        log.debug(logPrefix + " -> Started reading");
    }

    @Override
    public void onFinished() {
        log.debug(logPrefix + " -> End of Stream");
    }

    @Override
    public void onFatalError(Exception e) {
        log.debug(logPrefix + " -> Exception", e);
    }

    @Override
    public void addOutput(String line) {
        log.debug(logPrefix + ": " + line);
    }

}
