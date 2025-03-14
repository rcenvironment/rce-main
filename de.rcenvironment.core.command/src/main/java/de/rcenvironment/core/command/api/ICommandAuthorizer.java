/*
 * Copyright 2006-2025 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.command.api;

import java.util.Set;

/**
 * Interface for checking whether a command is authorized to be executed.
 * @author David Wichter
 */
public interface ICommandAuthorizer {

    /**
     * Determines whether a given command, identified by its fully qualified command name (group + sub command, i.e. "sysmon api")
     * is authorized to be executed.
     * @param fullyQualifiedCommandName The fully qualified name of the command that is to be checked.
     * @return true if the command is authorized to be executed.
     */
    boolean isAuthorized(String fullyQualifiedCommandName);
}
