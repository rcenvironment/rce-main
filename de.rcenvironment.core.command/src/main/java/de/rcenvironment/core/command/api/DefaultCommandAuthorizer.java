/*
 * Copyright 2006-2025 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.command.api;

/**
 * A default command authorizer implementation. It does not implement any authorization logic, 
 * i.e. every command is considered to be authorized.
 * 
 * @author David Wichter
 */
public class DefaultCommandAuthorizer implements ICommandAuthorizer {

    @Override
    public boolean isAuthorized(String command) {
        return true;
    }
}
