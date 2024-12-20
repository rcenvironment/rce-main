/*
 * Copyright 2006-2024 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.start.validators.internal;

import de.rcenvironment.core.start.common.validation.api.InstanceValidationResult;
import de.rcenvironment.core.start.common.validation.api.InstanceValidationResultFactory;
import de.rcenvironment.core.start.common.validation.spi.DefaultInstanceValidator;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Checks if the appropriate launcher is used, or if an older one is used.
 *
 * @author Tobias Brieden
 * @author Robert Mischke
 */
public class LauncherVersionValidator extends DefaultInstanceValidator {

    private static final String VALIDATION_DISPLAY_NAME = "Launcher Version";

    /**
     * System property name for passing the actual launcher version to the main application. This must be kept in sync with
     * {@link de.rcenvironment.bootstrap.launcher.api.RCELauncherConstants}, but is expected to remain constant over time.
     */
    private static final String SYSTEM_PROPERTY_KEY_RCE_LAUNCHER_VERSION = "de.rcenvironment.launcher.version";

    /**
     * The expected semantic version of the RCE launcher. This number should only be updated if the code within the launcher changes.
     * 
     * Within a released product version, this value should always match the corresponding one in
     * {@link de.rcenvironment.bootstrap.launcher.api.RCELauncherConstants}. At runtime, however, these numbers can differ during an update,
     * which is the reason for this mechanism in the first place.
     * 
     * Background: After an update, RCE needs to be restarted. During the restart process, however, the rce.ini is not reevaluated and
     * therefore, the new launcher is not executed. This can lead to problems if the application code relies on new or modified
     * aspects/properties of the launcher. To prevent this, startup is prevented and the user is prompted to perform a fresh restart.
     */
    private static final int EXPECTED_RCE_LAUNCHER_VERSION = 810;

    @Override
    public InstanceValidationResult validate() {

        String actualLauncherVersionStr = System.getProperty(SYSTEM_PROPERTY_KEY_RCE_LAUNCHER_VERSION);
        if (actualLauncherVersionStr == null) {
            // RCE is currently running with a launcher which does not write its version number (RCE <= 8.0.2)

            // If the running launcher is from RCE 8.0.0 - RCE 8.0.2, the early logging might not be available. But there is already a
            // warning written by the InsertOldLogAppender in this case.

            // If the running launcher is from RCE < 8.0.0, certain bugs might not be fixed.

            final String message = "RCE was started with an unexpected application launcher (no version information provided)";
            return InstanceValidationResultFactory.createResultForFailureWhichRequiresInstanceShutdown(VALIDATION_DISPLAY_NAME, message,
                message);
        }

        try {
            int runningLauncherVersion = Integer.parseInt(actualLauncherVersionStr);

            if (runningLauncherVersion != EXPECTED_RCE_LAUNCHER_VERSION) {
                // TODO this behavior has not been tested with recent Eclipse RCP versions yet
                final String message = StringUtils.format(
                    "RCE was apparently restarted after an update. Due to internal changes, a full shutdown "
                        + "and fresh start of the application is required instead. (Running launcher version: %d, expected version: %d)",
                    runningLauncherVersion, EXPECTED_RCE_LAUNCHER_VERSION);
                return InstanceValidationResultFactory.createResultForFailureWhichRequiresInstanceShutdown(VALIDATION_DISPLAY_NAME, message,
                    message);
            }

            return InstanceValidationResultFactory.createResultForPassed(VALIDATION_DISPLAY_NAME);
        } catch (NumberFormatException e) {
            final String message = "RCE was started with an unknown application launcher. This can result in undefined behaviour.";
            return InstanceValidationResultFactory.createResultForFailureWhichAllowsToProceed(VALIDATION_DISPLAY_NAME,
                message, message);
        }

    }

}
