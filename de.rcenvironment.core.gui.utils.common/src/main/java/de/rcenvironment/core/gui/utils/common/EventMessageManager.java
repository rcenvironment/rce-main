/*
 * Copyright 2024 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.utils.common;

import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

/**
 * Manages informational GUI representations (e.g. dialogs) that are used to inform the user about events that have occurred.
 * 
 * The main purpose of this manager is to centrally handle certain GUI-wide issues. One of these is handling fallback situations, like no
 * GUI being available at all, or it being shut down. The other main issue is multiple events "stacking" on top of each other, forcing the
 * user to dismiss several dialogs before the GUI becomes usable again. This manager makes sure that at most one dialog window is opened at
 * a time, and any subsequent events are handled differently. How such a fallback is done exactly may evolve over time, which is another
 * reason for handling this through a centralized interface. For now, fallback events are simply logged at the INFO level.
 * 
 * @author Robert Mischke
 */
public final class EventMessageManager {

    private static final EventMessageManager INSTANCE = new EventMessageManager();

    private final AtomicBoolean guiAvailable = new AtomicBoolean(false);

    private final AtomicBoolean dialogOpen = new AtomicBoolean(false);

    private final Log log = LogFactory.getLog(getClass());

    private EventMessageManager() {}

    public static EventMessageManager getInstance() {
        return INSTANCE;
    }

    /**
     * This method attempts to open a dialog and returns without waiting for the dialog to be closed. Note that the (modal) dialog may still
     * block the GUI from being interacted with until it is closed.
     * 
     * @param message the dialog message to show
     * @param swtIconCode one of the SWT codes ICON_ERROR, ICON_INFORMATION, ICON_WARNING for the dialog's icon
     * @param parentShell the parent shell for the dialog
     */
    public void submitEventMessageAsync(String message, int swtIconCode, Shell parentShell) {
        if (swtIconCode != SWT.ICON_ERROR && swtIconCode != SWT.ICON_WARNING && swtIconCode != SWT.ICON_INFORMATION) {
            throw new IllegalArgumentException("Invalid icon code: " + swtIconCode);
        }

        if (!guiAvailable.get()) {
            performDialogAlternative(message, "because no GUI is available");
            return;
        }

        if (parentShell.isDisposed()) {
            log.warn("Unexpected state: The GUI is marked as still available, but the parent shell provided for a dialog is disposed");
            performDialogAlternative(message, "because the parent Shell is disposed");
        }

        boolean previousDialogOpen = dialogOpen.getAndSet(true);

        // the setting was already "true" before
        if (previousDialogOpen) {
            performDialogAlternative(message, "because another dialog is already open");
            return;
        }

        // no dialog was open before (false), but the flag has now been "reserved" for the new dialog
        showDialogAndReleaseFlagWhenClosed(message, swtIconCode, parentShell);
    }

    private void showDialogAndReleaseFlagWhenClosed(String message, int swtIconCode, Shell parentShell) {
        parentShell.getDisplay().asyncExec(() -> {
            try {
                MessageBox dialog = new MessageBox(parentShell, swtIconCode | SWT.OK);
                dialog.setMessage(message);
                dialog.open(); // this blocks any other user input, but background GUI updates can still occur
            } finally {
                dialogOpen.set(false);
            }
        });
    }

    private void performDialogAlternative(String message, String noShowReason) {
        // TODO replace newlines that may have been added to the message for presenting it in a dialog?
        log.info(message + " (This message was logged instead of being shown as a dialog " + noShowReason + ".)");
    }

    public void registerGuiAvailable() {
        // duplicate calls are allowed, but have no effect
        guiAvailable.set(true);
    }

    public void registerGuiStopping() {
        // duplicate calls are allowed, but have no effect
        guiAvailable.set(false);
    }
}
