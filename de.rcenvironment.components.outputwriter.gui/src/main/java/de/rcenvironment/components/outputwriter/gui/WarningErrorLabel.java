/*
 * Copyright 2006-2025 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.outputwriter.gui;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import de.rcenvironment.core.gui.resources.api.ImageManager;
import de.rcenvironment.core.gui.resources.api.StandardImages;

/**
 * A composite that is able to show both errors and warnings. To this end, it renders two CLabels, which are labelled with symbols for error
 * and warning, respectively. If no error or warning is present, the respective label is not shown.
 * 
 * @author Alexander Weinert
 */
public class WarningErrorLabel extends Composite {

    private static final int COMPOSITE_MIN_HEIGHT = 45;

    private static final int LABEL_WIDTH_HINT = 80;

    private Text upperText;

    private Text lowerText;

    private CLabel upperLabel;

    private CLabel lowerLabel;

    private final List<String> errors = new LinkedList<>();

    private final List<String> warnings = new LinkedList<>();

    private GridData labelGridData;

    private GridData textGridData;

    public WarningErrorLabel(Composite parent, int style) {
        super(parent, style);

        this.setVisible(true);

        final GridLayout gd = new GridLayout(2, false);
        gd.marginWidth = 0;
        final GridData thisGridData =
            new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL);
        this.setLayout(gd);
        this.setLayoutData(thisGridData);

        labelGridData =
            new GridData(GridData.FILL_VERTICAL | GridData.GRAB_VERTICAL);
        labelGridData.verticalAlignment = SWT.TOP;
        labelGridData.horizontalAlignment = SWT.RIGHT;
        labelGridData.widthHint = LABEL_WIDTH_HINT;
        labelGridData.grabExcessVerticalSpace = true;

        textGridData =
            new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL);
        textGridData.minimumHeight = COMPOSITE_MIN_HEIGHT;
        textGridData.verticalIndent = 4;

        upperLabel = new CLabel(this, SWT.TOP | SWT.RIGHT);
        upperLabel.setLayoutData(labelGridData);
        upperText = new Text(this, SWT.READ_ONLY | SWT.V_SCROLL);
        upperText.setLayoutData(textGridData);

        lowerLabel = new CLabel(this, SWT.TOP | SWT.RIGHT);
        lowerLabel.setLayoutData(labelGridData);
        lowerText = new Text(this, SWT.READ_ONLY | SWT.V_SCROLL);
        lowerText.setLayoutData(textGridData);
    }

    /**
     * Appends the given warning to the currently displayed ones.
     * 
     * @param warning The warning to be displayed.
     */
    public void addWarning(String warning) {
        this.warnings.add(warning);
        refresh();
    }

    /**
     * Appends the given set of warnings to the currently displayed ones.
     * 
     * @param warningsParam The warnings do be displayed.
     */
    public void addWarnings(Collection<String> warningsParam) {
        this.warnings.addAll(warningsParam);
        refresh();
    }

    /**
     * After calling this method, no more warnings are shown.
     */
    public void clearWarnings() {
        this.warnings.clear();
        refresh();
    }

    /**
     * Appends the given error to the currently displayed ones.
     * 
     * @param error The error to be displayed.
     */
    public void addError(String error) {
        this.errors.add(error);
        refresh();
    }

    /**
     * Appends the given set of errors to the currently displayed ones.
     * 
     * @param errorsParam The errors do be displayed.
     */
    public void addErrors(Collection<String> errorsParam) {
        this.errors.addAll(errorsParam);
        refresh();
    }

    /**
     * After calling this method, no more errors are shown.
     */
    public void clearErrors() {
        this.errors.clear();
        refresh();
    }

    /**
     * Refresh the widgets to correctly display the warnings and errors stored in this object.
     */
    private void refresh() {

        upperText.dispose();
        lowerText.dispose();
        upperLabel.dispose();
        lowerLabel.dispose();

        if (!errors.isEmpty()) {
            upperLabel = new CLabel(this, SWT.TOP | SWT.RIGHT);
            upperLabel.setLayoutData(labelGridData);
            upperLabel.setImage(ImageManager.getInstance().getSharedImage(StandardImages.ERROR_16));
            upperLabel.setText("Errors:");
            upperText = new Text(this, SWT.READ_ONLY | SWT.V_SCROLL);
            upperText.setLayoutData(textGridData);
            upperText.setText(String.join("\n", errors));
        }

        if (!warnings.isEmpty()) {
            lowerLabel = new CLabel(this, SWT.TOP | SWT.RIGHT);
            lowerLabel.setLayoutData(labelGridData);
            lowerLabel.setImage(ImageManager.getInstance().getSharedImage(StandardImages.WARNING_16));
            lowerLabel.setText("Warnings:");
            lowerText = new Text(this, SWT.READ_ONLY | SWT.V_SCROLL);
            lowerText.setLayoutData(textGridData);
            lowerText.setText(String.join("\n", warnings));
        }
        this.layout();
    }
}
