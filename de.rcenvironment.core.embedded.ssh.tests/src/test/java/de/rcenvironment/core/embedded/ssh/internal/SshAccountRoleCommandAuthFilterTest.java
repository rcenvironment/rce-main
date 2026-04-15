/*
 * Copyright 2006-2025 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.embedded.ssh.internal;

import org.junit.Test;

import de.rcenvironment.core.command.api.CommandAuthFilter;
import junit.framework.TestCase;


/**
 * Test class for the SSH {@link CommandAuthFilter}.
 * 
 * @author David Wichter
 */
public class SshAccountRoleCommandAuthFilterTest extends TestCase {

    private SshConfiguration configuration = null;
    private SshAuthenticationManager authenticationManager = null;

    public SshAccountRoleCommandAuthFilterTest() {
        configuration = SshTestUtils.getValidConfig();
        authenticationManager = new SshAuthenticationManager(configuration, null, 3);
    }

    /**
     * 
     * Test role management with authorized commands.
     * 
     */
    @Test
    public void testAuthorizedCommands() {
        SshAccountRole adminRole = authenticationManager.getRoleForUser("admin");
        assertNotNull("Error during test setup: No admin role found.", adminRole);
        SshAccountRole userRole = authenticationManager.getRoleForUser("user");
        assertNotNull("Error during test setup: No user role found.", userRole);

        testRoleManagement(adminRole, "osgi", true);
        testRoleManagement(adminRole, "tasks", true);
        testRoleManagement(adminRole, "net", true);
        testRoleManagement(userRole, "stats", true);
        testRoleManagement(userRole, "tasks", true);
        testRoleManagement(userRole, "net", true);
    }

    /**
     * 
     * Test role management with unauthorized commands.
     * 
     */
    @Test
    public void testUnauthorizedCommands() {
        SshAccountRole userRole = authenticationManager.getRoleForUser("user");
        assertNotNull("Error during test setup: No user role found.", userRole);

        testRoleManagement(userRole, "wf", false);
        testRoleManagement(userRole, "wf run test.wf", false);
        testRoleManagement(userRole, "osgi", false);
        testRoleManagement(userRole, "falseCommand", false);
    }

    /**
     * 
     * Test role management with non existing user.
     * 
     */
    @Test
    public void testWrongUser() {
        SshAccountRole userRole = authenticationManager.getRoleForUser("sdhjdfsh5412546"); // this user should not exist.
        SshAccountRoleCommandAuthFilter sut = new SshAccountRoleCommandAuthFilter(userRole);
        boolean actual = sut.isAuthorized("stats");
        assertFalse("unknown user is allowed to execute command (that's bad; Do not define a test user with the name sdhjdfsh5412546)",
            actual);
    }

    private void testRoleManagement(SshAccountRole userRole, String command, boolean expected) {
        SshAccountRoleCommandAuthFilter sut = new SshAccountRoleCommandAuthFilter(userRole);
        boolean actual = sut.isAuthorized(command);

        assertEquals(
            String.format(
                "Authorization for role '%s' and command '%s' was expected to be %s, but was %s instead.",
                userRole.getRoleName(),
                command,
                expected,
                actual),
            expected,
            actual
        );
    }
}
