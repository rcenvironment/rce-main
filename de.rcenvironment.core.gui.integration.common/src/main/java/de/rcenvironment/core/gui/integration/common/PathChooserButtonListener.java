/*
 * Copyright 2006-2023 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.integration.common;

import de.rcenvironment.core.gui.utils.common.components.PropertyTabGuiHelper;
import java.io.File;

import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;



/**
 * Listener for all Buttons that should open a filessystem file dialog and put the selected path into the given text field. If the
 * isDirectoryDialog boolean is true, a DirectoryDialog will open instead.
 * 
 * @author Sascha Zur
 * @author Kathrin Schaffert (added filterNames)
 */
public class PathChooserButtonListener implements SelectionListener {

    private static final String OPEN_PATH = "Open path...";

    private Text linkedTextfield;

    private Shell shell;

    private boolean directoryDialog;

    private File openPath = null;

    private String[] filterNames;

    public PathChooserButtonListener(Text linkedTextfield, boolean isDirectoryDialog, Shell shell) {
        this(linkedTextfield, isDirectoryDialog, new String[] { "*.*" }, shell);
    }

    public PathChooserButtonListener(Text linkedTextfield, boolean isDirectoryDialog, String[] filterNames, Shell shell) {
        this.linkedTextfield = linkedTextfield;
        this.shell = shell;
        this.directoryDialog = isDirectoryDialog;
        this.filterNames = filterNames;
    }

    @Override
    public void widgetSelected(SelectionEvent arg0) {
        String selectedPath;
        if (!directoryDialog) {
            if (openPath != null) {
                selectedPath = PropertyTabGuiHelper.selectFileFromFileSystem(shell,
                    filterNames, OPEN_PATH, openPath.getAbsolutePath());
            } else {
                selectedPath = PropertyTabGuiHelper.selectFileFromFileSystem(shell,
                    filterNames, OPEN_PATH);
            }
        } else {
            selectedPath = PropertyTabGuiHelper.selectDirectoryFromFileSystem(shell, OPEN_PATH);
        }
        if (selectedPath != null) {
            linkedTextfield.setText(selectedPath);
        }
    }

    @Override
    public void widgetDefaultSelected(SelectionEvent arg0) {
        widgetSelected(arg0);

    }

    public void setOpenPath(File path) {
        openPath = path;
    }
}
