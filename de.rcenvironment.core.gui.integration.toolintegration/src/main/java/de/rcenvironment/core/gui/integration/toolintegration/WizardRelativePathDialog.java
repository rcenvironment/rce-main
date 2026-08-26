/*
 * Copyright 2006-2026 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.integration.toolintegration;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import de.rcenvironment.core.component.integration.ToolIntegrationConstants;
import de.rcenvironment.core.component.integration.ToolIntegrationConstants.DirectoryPlaceholder;
import de.rcenvironment.core.utils.common.PathUtils.PathUtilsTargetOS;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * A dialog for for extracting file names from file inputs in the command script.
 * 
 * @author Jan Flink
 */
class WizardRelativePathDialog extends Dialog {


    private static final int MIN_WIDTH = 340;

    private static final int MIN_HEIGHT = 140;

    private final List<String> inputs;

    private Combo inputCombo;

    private Combo directoryCombo;

    private Combo targetOSCombo;

    private String inputSelection;

    private String directorySelection;

    private PathUtilsTargetOS targetOSSelection;

    private EnumSet<DirectoryPlaceholder> directories;

    protected WizardRelativePathDialog(Shell parentShell, List<String> endpointNames, EnumSet<DirectoryPlaceholder> directories) {
        super(parentShell);
        setShellStyle(SWT.CLOSE | SWT.MAX | SWT.TITLE | SWT.BORDER | SWT.RESIZE
            | getDefaultOrientation());

        this.inputs = endpointNames;
        this.directories = directories;
    }

    @Override
    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        shell.setText("Select File/Directory Input");
        shell.setMinimumSize(MIN_WIDTH, MIN_HEIGHT);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite container = (Composite) super.createDialogArea(parent);
        container.setLayout(new GridLayout(2, false));
        GridData containerData = new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL);
        container.setLayoutData(containerData);

        GridData inputLabelData = new GridData(GridData.HORIZONTAL_ALIGN_END);
        Label inputLabel = new Label(container, SWT.None);
        inputLabel.setText("Input:");
        inputLabel.setLayoutData(inputLabelData);

        GridData comboData = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
        inputCombo = new Combo(container, SWT.READ_ONLY);
        inputCombo.setLayoutData(comboData);

        Label directoryLabel = new Label(container, SWT.None);
        directoryLabel.setText("Relative from:");

        directoryCombo = new Combo(container, SWT.READ_ONLY);
        directoryCombo.setLayoutData(comboData);

        Label targetOSLabel = new Label(container, SWT.None);
        targetOSLabel.setText("Target OS:");

        targetOSCombo = new Combo(container, SWT.READ_ONLY);
        targetOSCombo.setLayoutData(comboData);

        fillCombos();
        ComboSelectionListener selectionListener = new ComboSelectionListener();
        inputCombo.addSelectionListener(selectionListener);
        directoryCombo.addSelectionListener(selectionListener);

        return container;
    }


    @Override
    public void create() {
        super.create();
        getButton(Dialog.OK).setEnabled(false);
    }

    private void fillCombos() {
        for (String input : inputs) {
            inputCombo.add(input);
            inputCombo.setData(input, StringUtils.format(ToolIntegrationConstants.INPUT_PLACEHOLDER_TEMPLATE, input));
        }
        for (ToolIntegrationConstants.DirectoryPlaceholder directory : directories) {
            directoryCombo.add(directory.displayName);
            directoryCombo.setData(directory.displayName,
                StringUtils.format(ToolIntegrationConstants.DIRECTORY_PLACEHOLDER_TEMPLATE, directory.placeholderString));
        }
        directoryCombo.select(directoryCombo.indexOf(ToolIntegrationConstants.DirectoryPlaceholder.WORKING_DIR.displayName));
        for (PathUtilsTargetOS targetOS : Arrays.stream(PathUtilsTargetOS.values()).sorted().collect(Collectors.toList())) {
            targetOSCombo.add(targetOS.displayName);
            targetOSCombo.setData(targetOS.displayName, targetOS);
        }
        targetOSCombo.select(targetOSCombo.indexOf(PathUtilsTargetOS.DEFAULT.displayName));
    }

    public String getPlaceholder() {
        if (inputSelection != null && !inputSelection.isEmpty()) {
            if (targetOSSelection == PathUtilsTargetOS.DEFAULT) {
                return StringUtils.format(ToolIntegrationConstants.REL_PATH_PLACEHOLDER_TEMPLATE, directorySelection, inputSelection);
            } else {
                return StringUtils.format(ToolIntegrationConstants.OS_SPECIFIC_REL_PATH_PLACEHOLDER_TEMPLATE, targetOSSelection.shortName,
                    directorySelection, inputSelection);
            }
        }
        return "";
    }

    public class ComboSelectionListener implements SelectionListener {

        private static final int INDEX_NO_SELECTION = -1;

        @Override
        public void widgetDefaultSelected(SelectionEvent arg0) {
            // Intentionally left empty
        }

        @Override
        public void widgetSelected(SelectionEvent arg0) {
            if (inputCombo.getSelectionIndex() != INDEX_NO_SELECTION) {
                inputSelection = inputCombo.getData(inputCombo.getText()).toString();
            }
            if (directoryCombo.getSelectionIndex() != INDEX_NO_SELECTION) {
                directorySelection = directoryCombo.getData(directoryCombo.getText()).toString();
            }
            if (targetOSCombo.getSelectionIndex() != INDEX_NO_SELECTION) {
                targetOSSelection = (PathUtilsTargetOS) targetOSCombo.getData(targetOSCombo.getText());
            }
            getButton(Dialog.OK).setEnabled(
                inputCombo.getSelectionIndex() != INDEX_NO_SELECTION);
        }
    }

}
