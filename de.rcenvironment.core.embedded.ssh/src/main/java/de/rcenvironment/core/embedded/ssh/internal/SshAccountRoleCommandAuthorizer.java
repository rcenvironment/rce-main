/*
 * Copyright 2006-2025 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.embedded.ssh.internal;

import java.util.HashSet;
import java.util.Set;

import de.rcenvironment.core.command.api.ICommandAuthorizer;
import de.rcenvironment.core.command.spi.ExecutableCommand;

/**
 * Authorizes a command based on the currently set SSH account role.
 * @author David Wichter
 */
public class SshAccountRoleCommandAuthorizer implements ICommandAuthorizer {

    private final SshAccountRole role;
    
    public SshAccountRoleCommandAuthorizer(SshAccountRole role) {
        super();
        this.role = role;
    }

    /**
     * @inheritDoc
     */
    @Override
    public boolean canExecuteCommand(ExecutableCommand command) {
        return false; //TODO: Implement
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public Set<ExecutableCommand> getAuthorizedCommands() {
        return new HashSet<>(); //TODO: Implement
    }
}
