/*
 * Copyright 2006-2025 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.embedded.ssh.internal;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.LogFactory;

/**
 * Class to describe the different roles and their privileges.
 * 
 * @author Sebastian Holtappels
 * @author Brigitte Boden
 * @author David Wichter
 */
public final class SshAccountRole {

    private String roleName;

    private boolean allowedToOpenShell = true;

    private boolean allowedToUseUplink = false;

    public SshAccountRole(String roleName) {
        this.roleName = roleName;

        // Set allowed command patterns for role name
        switch (roleName) {
        case SshConstants.ROLE_NAME_REMOTE_ACCESS_USER:
        case SshConstants.ROLE_NAME_REMOTE_ACCESS_USER_ALIAS:
            allowedToUseUplink = true;
            break;
        case SshConstants.ROLE_NAME_UPLINK_CLIENT:
            allowedToOpenShell = false;
            allowedToUseUplink = true;
            break;
        case SshConstants.ROLE_NAME_REMOTE_ACCESS_ADMIN:
        case SshConstants.ROLE_NAME_WORKFLOW_OBSERVER:
        case SshConstants.ROLE_NAME_WORKFLOW_ADMIN:
        case SshConstants.ROLE_NAME_LOCAL_ADMIN:
        case SshConstants.ROLE_NAME_IM_ADMIN:
        case SshConstants.ROLE_NAME_IM_DELEGATE:
        case SshConstants.ROLE_NAME_DEVELOPER:
        case SshConstants.ROLE_NAME_HEALTH_CHECK: // same permissions as the default role; uses the "dummy" command for testing
        case SshConstants.ROLE_NAME_DEFAULT:
            break;
        default:
            // as long as no user-defined roles are supported, this is always an internal error
            throw new IllegalArgumentException("Tried to create a role with an unhandled id: " + roleName);
        }
    }
    public boolean isAllowedToUseUplink() {
        return allowedToUseUplink;
    }

    public String getRoleName() {
        return roleName;
    }

    public boolean isAllowedToOpenShell() {
        return allowedToOpenShell;
    }

    @Override
    public int hashCode() {
        return roleName.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof SshAccountRole) {
            SshAccountRole other = (SshAccountRole) obj;
            return roleName.equals(other.getRoleName());
        } else {
            return false;
        }
    }
}
