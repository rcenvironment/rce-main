/*
 * Copyright 2006-2023 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.database.gui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetWidgetFactory;

import de.rcenvironment.components.database.common.DatabaseComponentConstants;
import de.rcenvironment.components.database.common.jdbc.JDBCDriverInformation;
import de.rcenvironment.components.database.common.jdbc.JDBCDriverService;
import de.rcenvironment.core.gui.resources.api.ImageManager;
import de.rcenvironment.core.gui.resources.api.StandardImages;
import de.rcenvironment.core.gui.workflow.editor.properties.ValidatingWorkflowNodePropertySection;
import de.rcenvironment.core.utils.incubator.ServiceRegistry;
import de.rcenvironment.core.utils.incubator.ServiceRegistryAccess;

/**
 * Database connection sections.
 *
 * @author Oliver Seebach
 * @author Kathrin Schaffert
 * @author Jan Flink 
 */
public class DatabaseConnectionSection extends ValidatingWorkflowNodePropertySection {

    private static final Integer MINIMUM_TEXTFIELD_WIDTH = 150;

    private JDBCDriverService jdbcDriverService;

    public DatabaseConnectionSection() {
        ServiceRegistryAccess serviceRegistryAccess = ServiceRegistry.createAccessFor(this);
        jdbcDriverService = serviceRegistryAccess.getService(JDBCDriverService.class);
    }

    @Override
    protected void createCompositeContent(final Composite parent, TabbedPropertySheetPage aTabbedPropertySheetPage) {

        super.createCompositeContent(parent, aTabbedPropertySheetPage);
        TabbedPropertySheetWidgetFactory factory = aTabbedPropertySheetPage.getWidgetFactory();

        final Section sectionDatabase = factory.createSection(parent, Section.TITLE_BAR | Section.EXPANDED);
        sectionDatabase.setText("Database Connection");
        sectionDatabase.marginWidth = 5;
        sectionDatabase.marginHeight = 5;

        Composite mainComposite = new Composite(sectionDatabase, SWT.NONE);
        mainComposite.setLayout(new GridLayout(1, false));
        mainComposite.setBackground(Display.getCurrent().getSystemColor(1));
        GridData mainData = new GridData(GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL | GridData.FILL_BOTH);
        mainComposite.setLayoutData(mainData);
        sectionDatabase.setClient(mainComposite);

        // 1
        Composite informationLabelComposite = new Composite(mainComposite, SWT.NONE);
        informationLabelComposite.setLayout(new GridLayout(2, false));
        GridData informationLabelData = new GridData(GridData.GRAB_HORIZONTAL | GridData.FILL_BOTH);
        informationLabelData.horizontalSpan = 2;
        informationLabelComposite.setLayoutData(informationLabelData);

        CLabel informationLabel = new CLabel(informationLabelComposite, SWT.LEFT | SWT.SHADOW_NONE);

        if (jdbcDriverService.getRegisteredJDBCDrivers().isEmpty()) {
            informationLabel.setText("WARNING: No database connectors registered!\n"
                + "For information about registering database connectors, see the component's help.");
            informationLabel.setImage(ImageManager.getInstance().getSharedImage(StandardImages.WARNING_16));
        } else {
            informationLabel.setText("Please configure the database for this component to use:");
        }

        // 2
        Composite databaseSelectionComposite = new Composite(mainComposite, SWT.NONE);
        databaseSelectionComposite.setLayout(new GridLayout(1, false));
        GridData databaseSelectionData = new GridData(GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL | GridData.FILL_VERTICAL);
        databaseSelectionComposite.setLayoutData(databaseSelectionData);

        Group databaseSelectionGroup = new Group(databaseSelectionComposite, SWT.NONE);
        databaseSelectionGroup.setText("Database");
        databaseSelectionGroup.setLayout(new GridLayout(2, false));
        GridData databaseSelectionGroupData = new GridData(GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL | GridData.FILL_VERTICAL);
        databaseSelectionGroup.setLayoutData(databaseSelectionGroupData);

        // DB NAME
        Label databaseNameLabel = new Label(databaseSelectionGroup, SWT.NONE);
        databaseNameLabel.setText("Database Name: ");
        GridData databaseNameLabelData = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
        databaseNameLabel.setLayoutData(databaseNameLabelData);

        Text databaseNameText = new Text(databaseSelectionGroup, SWT.BORDER);
        GridData databaseNameTextData = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
        databaseNameTextData.minimumWidth = MINIMUM_TEXTFIELD_WIDTH;
        databaseNameTextData.widthHint = MINIMUM_TEXTFIELD_WIDTH;
        databaseNameText.setLayoutData(databaseNameTextData);
        databaseNameText.setData(CONTROL_PROPERTY_KEY, DatabaseComponentConstants.DATABASE_NAME);

        // DB CONNECTOR
        Label databaseConnectorLabel = new Label(databaseSelectionGroup, SWT.NONE);
        databaseConnectorLabel.setText("Database Type: ");
        GridData databaseConnectorLabelData = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
        databaseConnectorLabel.setLayoutData(databaseConnectorLabelData);

        CCombo databaseConnectorCombo = new CCombo(databaseSelectionGroup, SWT.READ_ONLY | SWT.BORDER);
        GridData databaseConnectorData = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
        databaseConnectorData.minimumWidth = MINIMUM_TEXTFIELD_WIDTH;
        databaseConnectorData.widthHint = MINIMUM_TEXTFIELD_WIDTH;
        databaseConnectorCombo.setLayoutData(databaseConnectorData);
        databaseConnectorCombo.setData(CONTROL_PROPERTY_KEY, DatabaseComponentConstants.DATABASE_CONNECTOR);
        databaseConnectorCombo
            .setItems(
                jdbcDriverService.getRegisteredJDBCDrivers().stream().map(JDBCDriverInformation::getDisplayName).toArray(String[]::new));

        // DB HOST
        Label databaseHostLabel = new Label(databaseSelectionGroup, SWT.NONE);
        databaseHostLabel.setText("Database Host: ");
        GridData databaseHostLabelData = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
        databaseHostLabel.setLayoutData(databaseHostLabelData);

        Text databaseHostText = new Text(databaseSelectionGroup, SWT.BORDER);
        GridData databaseHostTextData = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
        databaseHostTextData.minimumWidth = MINIMUM_TEXTFIELD_WIDTH;
        databaseHostTextData.widthHint = MINIMUM_TEXTFIELD_WIDTH;
        databaseHostText.setLayoutData(databaseHostTextData);
        databaseHostText.setData(CONTROL_PROPERTY_KEY, DatabaseComponentConstants.DATABASE_HOST);

        // DB PORT
        Label databasePortLabel = new Label(databaseSelectionGroup, SWT.NONE);
        databasePortLabel.setText("Database Port: ");
        GridData databasePortLabelData = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
        databasePortLabel.setLayoutData(databasePortLabelData);

        Text databasePortText = new Text(databaseSelectionGroup, SWT.BORDER);
        GridData databasePortTextData = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
        databasePortTextData.minimumWidth = MINIMUM_TEXTFIELD_WIDTH;
        databasePortTextData.widthHint = MINIMUM_TEXTFIELD_WIDTH;
        databasePortText.setLayoutData(databasePortTextData);
        databasePortText.setData(CONTROL_PROPERTY_KEY, DatabaseComponentConstants.DATABASE_PORT);

        // DB SCHEME
        Label databaseSchemeLabel = new Label(databaseSelectionGroup, SWT.NONE);
        databaseSchemeLabel.setText("Default Scheme: ");
        GridData databaseSchemeLabelData = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
        databaseSchemeLabel.setLayoutData(databaseSchemeLabelData);

        Text databaseSchemeText = new Text(databaseSelectionGroup, SWT.BORDER);
        GridData databaseSchemeTextData = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
        databaseSchemeTextData.minimumWidth = MINIMUM_TEXTFIELD_WIDTH;
        databaseSchemeTextData.widthHint = MINIMUM_TEXTFIELD_WIDTH;
        databaseSchemeText.setLayoutData(databaseSchemeTextData);
        databaseSchemeText.setData(CONTROL_PROPERTY_KEY, DatabaseComponentConstants.DATABASE_SCHEME);

        // INFORMATION LABEL
        CLabel databaseSchemeInformationLabel = new CLabel(mainComposite, SWT.NONE);
        databaseSchemeInformationLabel
            .setText("Please note: 'Default Scheme' is the scheme you would define using the 'USE <scheme_name>' command.");
        databaseSchemeInformationLabel.setImage(ImageManager.getInstance().getSharedImage(StandardImages.INFORMATION_16));
        GridData databaseSchemeInformationLabelData = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
        databaseSchemeInformationLabel.setLayoutData(databaseSchemeInformationLabelData);
    }

    @Override
    public void refreshSection() {
        super.refreshSection();
        String configString =
            getConfiguration().getConfigurationDescription().getConfigurationValue(DatabaseComponentConstants.DATABASE_HOST);
        // when db component is instantiated for the first time the configuration values have to be set with a default string ""
        // this is necessary for the correct functionality of undo mechanism
        // Kathrin Schaffert, Feb 2019
        if (configString == null) {
            getConfiguration().getConfigurationDescription().setConfigurationValue(DatabaseComponentConstants.DATABASE_NAME, "");
            getConfiguration().getConfigurationDescription().setConfigurationValue(DatabaseComponentConstants.DATABASE_HOST, "");
            getConfiguration().getConfigurationDescription().setConfigurationValue(DatabaseComponentConstants.DATABASE_PORT, "");
            getConfiguration().getConfigurationDescription().setConfigurationValue(DatabaseComponentConstants.DATABASE_SCHEME, "");
        }
    }

}
