/*
 * Copyright 2006-2024 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.instancemanagement.internal;

/**
 * 
 * Encapsules a connection in the configuration.
 *
 * @author David Scholz
 * @author Jan Flink
 */
public class ConfigurationConnection {

    private final String connectionName;

    private final String host;

    private final int port;

    private final boolean connectOnStartup;

    private final boolean autoRetry;

    private final long autoRetryInitialDelay;

    private final long autoRetryMaximumDelay;

    private final float autoRetryDelayMultiplier;

    public ConfigurationConnection(String connectionName, String host, int port, boolean connectOnStartup, boolean autoRetry,
        long autoRetryInitialDelay,
        long autoRetryMaximumDelay, float autoRetryDelayMultiplier) {
        this.connectionName = connectionName;
        this.host = host;
        this.port = port;
        this.connectOnStartup = connectOnStartup;
        this.autoRetry = autoRetry;
        this.autoRetryInitialDelay = autoRetryInitialDelay;
        this.autoRetryMaximumDelay = autoRetryMaximumDelay;
        this.autoRetryDelayMultiplier = autoRetryDelayMultiplier;
    }

    public String getConnectionName() {
        return connectionName;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public boolean getConnectOnStartup() {
        return connectOnStartup;
    }

    public boolean getAutoRetry() {
        return autoRetry;
    }
    public long getAutoRetryInitialDelay() {
        return autoRetryInitialDelay;
    }

    public long getAutoRetryMaximumDelay() {
        return autoRetryMaximumDelay;
    }

    public float getAutoRetryDelayMultiplier() {
        return autoRetryDelayMultiplier;
    }

}
