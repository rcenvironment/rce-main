/*
 * Copyright 2006-2025 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.command.api;

import java.util.Set;

import de.rcenvironment.core.command.spi.ExecutableCommand;

/**
 * Interface for checking whether a command is authorized to be executed.
 * @author David Wichter
 */
public interface ICommandAuthorizer {

    /**
     * Determines whether a given command is authorized to be executed.
     * @param command The command that is to be checked.
     * @return true if the command is authorized to be executed.
     */
    boolean canExecuteCommand(ExecutableCommand command);

    /**
     * Returns the set of commands that are authorized to be executed.
     * @return A set of commands, each of which is authorized to be executed.
     */
    Set<ExecutableCommand> getAuthorizedCommands();

}
