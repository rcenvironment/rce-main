/*
 * Copyright 2006-2025 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.command.internal;

import java.util.Set;

import de.rcenvironment.core.command.api.ICommandAuthorizer;
import de.rcenvironment.core.command.spi.ExecutableCommand;


/**
 * Dummy implementation of {@link ICommandAuthorizer} for testing purposes.
 * 
 * @author David Wichter
 */
public class DummyCommandAuthorizer implements ICommandAuthorizer {

    @Override
    public boolean canExecuteCommand(ExecutableCommand command) {
        return true;
    }

    @Override
    public Set<ExecutableCommand> getAuthorizedCommands() {
        return null;
    }

}
