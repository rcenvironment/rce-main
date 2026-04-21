/*
 * Copyright 2006-2026 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.embedded.ssh.internal;

import de.rcenvironment.core.command.api.CommandAuthFilter;

/**
 * Authorizes a command based on the currently set SSH account role.
 * @author David Wichter
 */
public class SshAccountRoleCommandAuthFilter implements CommandAuthFilter {

    private final SshAccountRole role;
    
    public SshAccountRoleCommandAuthFilter(SshAccountRole role) {
        this.role = role;
    }

    /**
     * @inheritDoc
     */
    @Override
    public boolean isAuthorized(String qualifiedCommandName) {
        if (role == null) {
            return false;
        } else if (role.getRoleName().equals(SshConstants.ROLE_NAME_DEVELOPER)) {
            return true;
        } else {
            return role.getAllowedCommands().contains(qualifiedCommandName);
        }
    }
}
