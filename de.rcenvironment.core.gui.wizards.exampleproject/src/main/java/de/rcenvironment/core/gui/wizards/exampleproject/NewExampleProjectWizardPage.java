/*
 * Copyright 2006-2025 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.gui.wizards.exampleproject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.IDialogPage;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.osgi.framework.Bundle;

import de.rcenvironment.core.component.integration.ToolIntegrationService;
import de.rcenvironment.core.configuration.ConfigurationService;
import de.rcenvironment.core.configuration.ConfigurationService.ConfigurablePathId;
import de.rcenvironment.core.gui.resources.api.ImageManager;
import de.rcenvironment.core.gui.resources.api.StandardImages;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.incubator.ServiceRegistry;
import de.rcenvironment.core.utils.incubator.ServiceRegistryAccess;

/**
 * The wizard GUI page. The only input element is a field for choosing a project name.
 * 
 * @author Robert Mischke
 * @author Sascha Zur
 */
public class NewExampleProjectWizardPage extends WizardPage {

    /**
     * constant.
     */
    public static final int COPY_EXAMPLE_TOOL = 1;

    /**
     * constant.
     */
    public static final int DONT_COPY_EXAMPLE_TOOL = 0;

    /**
     * constant.
     */
    public static final int RED_X_CANCELD = -1;

    private static final int MINIMUM_HEIGHT = 250;

    private static final int MINIMUM_WIDTH = 500;

    private static final String ASTERISK = "*";

    private static final int MINUS_ONE = -1;

    private static final String EXAMPLE_TOOL_NAME = "Example";

    private Text textFieldProjectName;

    private NewExampleProjectWizard newExampleProjectWizard;

    private Button createExampleIntegratedToolButton;

    private final Log log = LogFactory.getLog(getClass());

    private ServiceRegistryAccess serviceRegistryAccess;

    private Group toolIntegrationGroup;

    /**
     * Constructor for SampleNewWizardPage.
     * 
     * @param newExampleProjectWizard
     * 
     * @param pageName
     */
    public NewExampleProjectWizardPage(ISelection selection, NewExampleProjectWizard newExampleProjectWizard) {
        super("wizardPage");
        setTitle("Create Workflow Examples Project");
        setDescription("This wizard generates an example project containing example workflows.");
        this.newExampleProjectWizard = newExampleProjectWizard;
        serviceRegistryAccess = ServiceRegistry.createAccessFor(this);
    }

    /**
     * @see IDialogPage#createControl(Composite)
     * @param parent :
     */
    @Override
    public void createControl(Composite parent) {
        Composite container = new Composite(parent, SWT.NULL);
        GridLayout layout = new GridLayout();
        container.setLayout(layout);
        layout.numColumns = 2;
        layout.verticalSpacing = 9;
        Label label = new Label(container, SWT.NULL);
        label.setText("Project &Name:");

        textFieldProjectName = new Text(container, SWT.BORDER | SWT.SINGLE);
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        textFieldProjectName.setLayoutData(gd);
        textFieldProjectName.addModifyListener(new ModifyListener() {

            @Override
            public void modifyText(ModifyEvent e) {
                dialogChanged();
            }
        });

        toolIntegrationGroup = new Group(container, SWT.NULL);
        GridData gridDataHorizontalSpan2 = new GridData(GridData.FILL_HORIZONTAL);
        gridDataHorizontalSpan2.horizontalSpan = 2;
        toolIntegrationGroup.setLayoutData(gridDataHorizontalSpan2);
        toolIntegrationGroup.setLayout(new GridLayout(2, false));
        toolIntegrationGroup.setText("Tool Integration Example");

        createExampleIntegratedToolButton = new Button(toolIntegrationGroup, SWT.CHECK);
        createExampleIntegratedToolButton.setText("Integrate example tool");
        createExampleIntegratedToolButton.setLayoutData(gridDataHorizontalSpan2);

        if (integratedComponentIdExists(".+(?<!common)\\." + EXAMPLE_TOOL_NAME)) {
            GridData gridDataVerticalTop = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
            Label warningIcon = new Label(toolIntegrationGroup, SWT.NULL);
            warningIcon.setImage(ImageManager.getInstance().getSharedImage(StandardImages.WARNING_16));
            warningIcon.setLayoutData(gridDataVerticalTop);
            GridData gridDataText = new GridData(GridData.FILL_HORIZONTAL);
            gridDataText.widthHint = MINIMUM_WIDTH;
            Text componentExistsInformationText = new Text(toolIntegrationGroup, SWT.MULTI | SWT.WRAP | SWT.TRANSPARENT);
            componentExistsInformationText
                .setText(StringUtils.format("A workflow component with the name '%1$s' already exists in your profile.\n"
                + "Since dublicate names are not allowed and this component is not integrated as a common tool, "
                + "the example tool cannot be integrated.\n"
                + "Please rename the existing '%1$s' component and try again.", EXAMPLE_TOOL_NAME));
            componentExistsInformationText.setLayoutData(gridDataText);
            componentExistsInformationText.setEditable(false);
            createExampleIntegratedToolButton.setEnabled(false);
        }

        initialize();
        dialogChanged();
        setControl(container);
        getShell().setMinimumSize(MINIMUM_WIDTH, MINIMUM_HEIGHT);
    }

    private void initialize() {
        textFieldProjectName.setText(newExampleProjectWizard.getProjectDefaultName());
    }

    /**
     * Ensures that both text fields are set.
     */

    private void dialogChanged() {
        final String newProjectName = textFieldProjectName.getText();
        if (newProjectName.length() == 0) {
            updateStatus("Please chose a name for the new project");
            return;
        }

        IProject existingProject = ResourcesPlugin.getWorkspace().getRoot()
            .getProject(newProjectName);
        if (existingProject != null && existingProject.exists()) {
            updateStatus("This project name is already in use");
            return;
        }

        // FIXME validate name structure

        updateStatus(null);
    }

    private void updateStatus(String message) {
        setErrorMessage(message);
        setPageComplete(message == null);
    }

    public String getNewProjectName() {
        return textFieldProjectName.getText();
    }

    private boolean integratedComponentIdExists(String regex) {
        ToolIntegrationService integrationService = serviceRegistryAccess.getService(ToolIntegrationService.class);
        return integrationService.getIntegratedComponentIds().stream().anyMatch(toolID -> toolID.matches(regex));
    }

    /**
     * 
     * Checks if the example tool should be integrated and if it already exists.
     * 
     * @return Integrate Example tool
     * @throws IOException
     */
    public int getCreateTIExample() {
        boolean isSelected = createExampleIntegratedToolButton.getSelection();

        try {
            if (isSelected
                && (integratedComponentIdExists(".+(common)\\." + EXAMPLE_TOOL_NAME)
                    || checkOrDeleteExampleToolConfigOnDisk(false))) {
                // returns 1 for keep and 0 for replace
                MessageDialog dialog = new MessageDialog(this.getShell(), "Overwrite Example Tool Confirmation", null,
                    "An integrated tool named 'Example' already exists.\n"
                        + "Replace or keep it?",
                    MessageDialog.WARNING, new String[] { "Replace", "Keep" }, 1);
                int overwrite = dialog.open();
                if (overwrite == MessageDialog.OK) {
                    checkOrDeleteExampleToolConfigOnDisk(true);
                    return COPY_EXAMPLE_TOOL;
                } else if (overwrite == MessageDialog.CANCEL) {
                    return DONT_COPY_EXAMPLE_TOOL;
                } else {
                    return RED_X_CANCELD;
                }
            }
        } catch (IOException e) {
            log.error("Could not verify existence of example tool (I/O Error)", e);
            return DONT_COPY_EXAMPLE_TOOL;
        }
        if (isSelected) {
            return COPY_EXAMPLE_TOOL;
        } else {
            return DONT_COPY_EXAMPLE_TOOL;
        }
    }

    private boolean checkOrDeleteExampleToolConfigOnDisk(boolean delete) throws IOException {

        Bundle bundle = Platform.getBundle(newExampleProjectWizard.getPluginID());

        ConfigurationService configurationService = serviceRegistryAccess.getService(ConfigurationService.class);
        File integrationFolder = configurationService.getConfigurablePath(ConfigurablePathId.PROFILE_INTEGRATION_DATA);
        File commonsFolder = new File(new File(integrationFolder, "tools"), "common");

        boolean exampleToolExists = false;

        if (!commonsFolder.exists()) {
            return false;
        }
        @SuppressWarnings("rawtypes") final Enumeration toolFiles =
            bundle.findEntries("templates/" + "integration_example", ASTERISK, true);
        while (toolFiles.hasMoreElements()) {
            final URL elementURL = (URL) toolFiles.nextElement();
            final String rawPath = elementURL.getPath();
            final String targetPath = rawPath.replaceFirst("^/templates/\\w+/", "");
            final File target = new File(commonsFolder, targetPath);

            if (!delete && target.exists() && (!target.isDirectory() || !rawPath.endsWith("/"))) {
                InputStream downloadStream = elementURL.openStream();
                InputStream oldFileStream = FileUtils.openInputStream(target);
                BufferedInputStream bDownloadStream = new BufferedInputStream(downloadStream);
                BufferedInputStream bOldFileStream = new BufferedInputStream(oldFileStream);
                int data;
                while ((data = bDownloadStream.read()) != MINUS_ONE) {
                    if (data != bOldFileStream.read()) {
                        exampleToolExists = true;
                    }
                }
                // different file size
                if (bOldFileStream.read() != MINUS_ONE) {
                    exampleToolExists = true;
                }
            } else if (delete && target.exists() && target.isDirectory()) {
                try {
                    FileUtils.deleteDirectory(target);
                } catch (IOException e) {
                    log.info(StringUtils.format("Could not delete directory %s, maybe already deleted.", target));
                }
            }
        }
        return exampleToolExists;
    }

}
