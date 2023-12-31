/*
 * Copyright 2020-2023 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.embedded.ssh.internal;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import de.rcenvironment.core.command.spi.CommandContext;
import de.rcenvironment.core.command.spi.CommandPlugin;
import de.rcenvironment.core.command.spi.MainCommandDescription;
import de.rcenvironment.core.command.spi.SubCommandDescription;
import de.rcenvironment.core.embedded.ssh.api.SshAccountConfigurationService;
import de.rcenvironment.toolkit.utils.common.IdGenerator;
import de.rcenvironment.toolkit.utils.common.IdGeneratorType;

/**
 * Execution handler for "keytool" commands. Currently located in the SSH bundle for simplicity; could also be moved elsewhere or split up.
 *
 * @author Robert Mischke
 */
@Component
public class KeyToolCommandPlugin implements CommandPlugin {

    private static final String SUBCMD_SSH_PW = "ssh-pw";

    private static final String SUBCMD_UPLINK_PW = "uplink-pw";

    private static final String MAIN_CMD = "keytool";

    // 14 characters of Base64 minus two characters (~6 bits/char) gives ~80 bits of entropy, which is much more than
    // a typical human-generated password, while still being reasonable to type if necessary. -- misc_ro
    private static final int GENERATED_PASSWORD_LENGTH = 14;

    @Reference
    private SshAccountConfigurationService sshAccountService;

    @Override
    public MainCommandDescription[] getCommands() {
        final MainCommandDescription keytoolCommands = new MainCommandDescription(MAIN_CMD, "generate ssh passwords",
            "commands for ssh passwords",
            new SubCommandDescription(SUBCMD_SSH_PW, "generates a password for an SSH or Uplink connection, and the corresponding "
                + "server entry", this::performGenerateSshOrUplinkPw),
            new SubCommandDescription(SUBCMD_UPLINK_PW, "generates a password for an SSH or Uplink connection, and the corresponding "
                    + "server entry", this::performGenerateSshOrUplinkPw)
        );
        
        return new MainCommandDescription[] { keytoolCommands };
    }

    private void performGenerateSshOrUplinkPw(CommandContext context) {
        // To ensure an unbiased password without the special Base64url characters ("_" and "-"), over-generate at twice the length...
        String password = IdGenerator.createRandomBase64UrlString(GENERATED_PASSWORD_LENGTH * 2, IdGeneratorType.SECURE);
        // ...and then remove the offending characters and trim to the actual length. For this to fail, more than half of the
        // generated characters would have to be the 2 out of 64 ones removed. This is extremely unlikely, and even in that
        // case, the result would not be broken, but only a slightly shorter password. -- misc_ro
        password = password.replaceAll("[-_]", "").substring(0, GENERATED_PASSWORD_LENGTH);

        context.println("The generated password (keep this confidential):");
        context.println(password);
        context.println("The password hash (send this to the server's administrator):");
        context.println(sshAccountService.generatePasswordHash(password));
    }
    
}
