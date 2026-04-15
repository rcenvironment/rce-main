/*
 * Copyright 2006-2025 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.embedded.ssh.internal;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Class to describe the different roles and their privileges.
 * 
 * @author Sebastian Holtappels
 * @author Brigitte Boden
 * @author David Wichter
 */
public final class SshAccountRole {

    private String roleName;

    private boolean allowedToOpenShell;

    private boolean allowedToUseUplink;
    
    private final Set<String> allowedCommands = new HashSet<>();

    public SshAccountRole(String roleName, boolean allowedToOpenShell, boolean allowedToUseUplink, Set<String> allowedCommands) {
        this.roleName = roleName;
        
        this.allowedCommands.addAll(allowedCommands);
        this.allowedToOpenShell = allowedToOpenShell;
        this.allowedToUseUplink = allowedToUseUplink;
    }

    public String getRoleName() {
        return roleName;
    }
    
    public boolean isAllowedToUseUplink() {
        return allowedToUseUplink;
    }

    public boolean isAllowedToOpenShell() {
        return allowedToOpenShell;
    }
    
    /**
     * Returns the set of allowed commands for this role. The returned set is unmodifiable.
     * @return An unmodifiable set of allowed commands for the account role instance.
     */
    public Set<String> getAllowedCommands() {
        return Collections.unmodifiableSet(allowedCommands);
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
