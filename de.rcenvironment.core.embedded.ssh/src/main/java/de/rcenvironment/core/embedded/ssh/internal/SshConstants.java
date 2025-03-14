/*
 * Copyright 2006-2025 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.embedded.ssh.internal;

import java.util.Arrays;
import java.util.List;

/**
 * Used to have a single point to add/ read constants.
 * 
 * @author Sebastian Holtappels
 * @author Robert Mischke
 */
public final class SshConstants {

    // TODO move at least some of these to the API package; for example, the names of the predefined SSH roles should be accessible from
    // Instance Management code, instead of having disconnected constants there -- misc_ro

    // CONSOLE STRINGS AND CONFIGURATION VALUES - START

    /**
     * Template for the interactive shell prompt.
     */
    public static final String CONSOLE_PROMPT_TEMPLATE = "\r\n%s@RCE: ";

    /**
     * Constant for exit command.
     */
    public static final String EXIT_COMMAND = "exit";

    /**
     * Constant for exit command.
     */
    public static final String DEFAULT_COMMANDS = "help|exit|(version.*)";

    /**
     * Constant for help command.
     */
    public static final String HELP_COMMAND = "help";

    /**
     * sysmon command group.
     */
    public static final List<String> SYSMON_COMMAND_GROUP = Arrays.asList("sysmon", "sysmon api", "sysmon remote");

    /**
     * auth command group.
     */
    public static final List<String> AUTH_COMMAND_GROUP = Arrays.asList(
        "auth",
        "auth list",
        "auth create",
        "auth delete",
        "auth export",
        "auth import"
    );

    /**
     * Constant for auth command.
     */
    public static final String AUTH_COMMAND = "auth";

    /**
     * components command group.
     */
    public static final List<String> COMPONENTS_COMMAND_GROUP = Arrays.asList(
        "components",
        "components list", 
        "components list-auth",
        "components set-auth",
        "components show"
    );

    /**
     * Reduced net command group.
     */
    public static final List<String> REDUCED_NET_COMMAND_GROUP = Arrays.asList("net", "net info");

    /**
     * net command group.
     */
    public static final List<String> NET_COMMAND_GROUP = Arrays.asList(
        "net",
        "net info",
        "net filter",
        "net reload-filter"
    );

    /**
     * Constant for scp command.
     */
    public static final String SCP_COMMAND = "scp";

    /**
     * Constant for ra command.
     */
    public static final String RA_COMMAND = "ra";

    /**
     * ra-admin command group.
     */
    public static final List<String> RA_ADMIN_COMMAND_GROUP = Arrays.asList(
        "ra-admin list-wfs", 
        "ra-admin publish-wfs",
        "ra-admin unpublish-wfs"
    );

    /**
     * Reduced wf command group.
     */
    public static final List<String> REDUCED_WF_COMMAND_GROUP = Arrays.asList("wf", "wf list", "wf details");

    /**
     * wf command group.
     */
    public static final List<String> WF_COMMAND_GROUP = Arrays.asList(
        "wf",
        "wf list",
        "wf cancel",
        "wf delete",
        "wf details",
        "wf dispose",
        "wf open",
        "wf pause",
        "wf resume",
        "wf run",
        "wf start",
        "wf verify",
        "wf-integrate"
    );

    /**
     * Constant for stop command.
     */
    public static final String STOP_COMMAND = "stop";

    /**
     * Constant for shutdown command.
     */
    public static final String SHUTDOWN_COMMAND = "shutdown";

    /**
     * Constant for restart command.
     */
    public static final String RESTART_COMMAND = "restart";

    /**
     * Constant for mail command.
     */
    public static final String MAIL_COMMAND = "mail";

    /**
     * Constant for stats command.
     */
    public static final String STATS_COMMAND = "stats";

    /**
     * Constant for task command.
     */
    public static final String TASKS_COMMAND = "tasks";

    /**
     * cn command group.
     */
    public static final List<String> CN_COMMAND_GROUP = Arrays.asList(
        "cn",
        "cn list",
        "cn add",
        "cn start",
        "cn stop"
    );

    /**
     * im command group.
     */
    public static final List<String> IM_COMMAND_GROUP = Arrays.asList(
        "im install",
        "im reinstall",
        "im configure",
        "im start",
        "im stop",
        "im start-all",
        "im stop-all",
        "im restart",
        "im dispose",
        "im list",
        "im info"
    );

    /**
     * tc command group.
     */
    public static final List<String> TC_COMMAND_GROUP = Arrays.asList(
        "tc open_view",
        "tc close_view",
        "tc close_welcome",
        "tc export_wf_run",
        "tc compare_wf_runs",
        "tc export_all_wf_runs"
    );

    /**
     * uplink command group.
     */
    public static final List<String> UPLINK_COMMAND_GROUP = Arrays.asList(
        "uplink",
        "uplink list",
        "uplink add",
        "uplink start",
        "uplink stop"
    );

    /**
     * Constant for force-crash command.
     */
    public static final String FORCE_CRASH_COMMAND = "force-crash";

    /**
     * Constant for the name of the configuration file.
     */
    public static final String CONFIGURATION_NAME = "de.rcenvironment.core.embedded.ssh";

    /**
     * Lowest possible port. used to check if configured port is valid.
     */
    public static final int MIN_PORT_NUMBER = 0;

    /**
     * Highest possible port. used to check if configured port is valid.
     */
    public static final int MAX_PORT_NUMBER = 65535;

    /**
     * The shift for the write method in ConsoleAuthenticator.
     */
    public static final int AUTH_SHIFT = 24;

    /**
     * The int for the write method in ConsoleAuthenticator.
     */
    public static final int AUTH_INT = 0xFF;

    /**
     * The int for the write method in ConsoleAuthenticator.
     */
    public static final int COMMAND_HISTORY_SIZE = 20;

    // CONSOLE STRINGS AND CONFIGURATION VALUES - END
    // ASCII CODES - START

    /**
     * Code of the return key.
     */
    public static final int RETURN_KEY_CODE = 13;

    /**
     * Code of the del key.
     */
    public static final int DEL_KEY_CODE = 127;

    /**
     * Code of the tab key.
     */
    public static final int TAB_KEY_CODE = 9;

    /**
     * Lowest useful char for console.
     */
    public static final int LOWEST_USEFUL_KEY_CODE = 32;

    /**
     * Code for telling CommandBuilder to not add or printout the given sign.
     */
    public static final int NO_ADDING_INT_CODE = -1;

    /**
     * Code for the ESC key. Important because it is used as escape character for special keys
     */
    public static final int ESC_KEY_CODE = 27;
    // ASCII CODES - END
    // REGULAR EXPRESSIONS FOR SPECIAL KEY HANDLING - START

    /**
     * Regular expression to validate, if it could be a special key string or not.
     */
    public static final String SPECIAL_KEY_REGEX = "\\[([A-D]?|([1-6]([~0-9]~?)?)?)";

    /**
     * Code for special key.
     */
    public static final String SPECIAL_KEY_LEFT = "\\[D";

    /**
     * Code for special key.
     */
    public static final String SPECIAL_KEY_RIGHT = "\\[C";

    /**
     * Code for special key.
     */
    public static final String SPECIAL_KEY_UP = "\\[A";

    /**
     * Code for special key.
     */
    public static final String SPECIAL_KEY_DOWN = "\\[B";

    /**
     * Code for special key.
     */
    public static final String SPECIAL_KEY_ENTF = "\\[3~";

    /**
     * Regular expression for F1, F2,..., F12, einfg, arrow down, arrow up, pos1, end, bup, bdown.
     */
    public static final String SPECIAL_KEYS_IGNORE_LIST = "\\[[12][0-9]~|\\[2~|\\[1~|\\[4~|\\[5~|\\[6~";

    // REGULAR EXPRESSIONS FOR SPECIAL KEY HANDLING - END

    /**
     * Length of the random parts of the username for temporary accounts.
     */
    public static final int TEMP_USER_NAME_RANDOM_LENGTH = 6;

    /**
     * Length of the random passwords for temporary accounts.
     */
    public static final int TEMP_USER_PASSWORD_RANDOM_LENGTH = 6;

    /**
     * Prefix for temporary user accounts.
     */
    public static final String TEMP_USER_PREFIX = "t_";

    // TODO (p3) this list of predefined roles should probably be an Enum -- misc_ro

    /**
     * Names of predefined roles.
     */
    public static final String ROLE_NAME_REMOTE_ACCESS_USER = "remote_access_user";

    /**
     * Names of predefined roles.
     */
    public static final String ROLE_NAME_REMOTE_ACCESS_USER_ALIAS = "remote access";

    /**
     * Names of predefined roles.
     */
    public static final String ROLE_NAME_REMOTE_ACCESS_ADMIN = "remote_access_admin";

    /**
     * Names of predefined roles.
     */
    public static final String ROLE_NAME_UPLINK_CLIENT = "uplink_client";

    /**
     * Names of predefined roles.
     */
    public static final String ROLE_NAME_WORKFLOW_OBSERVER = "workflow_observer";

    /**
     * Names of predefined roles.
     */
    public static final String ROLE_NAME_WORKFLOW_ADMIN = "workflow_admin";

    /**
     * Names of predefined roles.
     */
    public static final String ROLE_NAME_LOCAL_ADMIN = "local_admin";

    /**
     * Names of predefined roles.
     */
    public static final String ROLE_NAME_IM_ADMIN = "instance_management_admin";

    /**
     * Names of predefined roles.
     */
    public static final String ROLE_NAME_IM_DELEGATE = "instance_management_delegate_user";

    /**
     * Names of predefined roles.
     */
    public static final String ROLE_NAME_DEVELOPER = "developer";

    /**
     * Names of predefined roles.
     */
    public static final String ROLE_NAME_HEALTH_CHECK = "health_check";

    /**
     * Names of predefined roles.
     */
    public static final String ROLE_NAME_DEFAULT = "default";

    /**
     * The pseudo command that is "executed" from the viewpoint of the SSH uplink client to attach input and output streams to.
     */
    public static final String SSH_UPLINK_VIRTUAL_CONSOLE_COMMAND = "ra uplink";

    private SshConstants() {}
}
