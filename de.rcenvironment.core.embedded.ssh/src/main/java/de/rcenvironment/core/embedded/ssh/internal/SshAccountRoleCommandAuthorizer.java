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
import de.rcenvironment.core.command.spi.AbstractCommandDescription;
import de.rcenvironment.core.command.spi.MainCommandDescription;

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
    public boolean canExecuteCommand(AbstractCommandDescription commandDescription) {
        if (role.getRoleName().equals(SshConstants.ROLE_NAME_DEVELOPER)) {
            return true;
        } else {
            return role.getAllowedCommands().contains(commandDescription.getFullyQualifiedCommand());
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public Set<MainCommandDescription> getAuthorizedCommands() {
        return new HashSet<>(); //TODO: Implement
    }
}
