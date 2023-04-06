/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.integration.toolintegration;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.help.IWorkbenchHelpSystem;

import de.rcenvironment.core.component.integration.IntegrationConstants;
import de.rcenvironment.core.component.integration.IntegrationContextType;
import de.rcenvironment.core.component.integration.ToolIntegrationConstants;
import de.rcenvironment.core.datamodel.api.DataType;
import de.rcenvironment.core.gui.integration.toolintegration.api.ToolIntegrationWizardPage;
import de.rcenvironment.core.gui.utils.common.widgets.LineNumberStyledText;

/**
 * @author Sascha Zur
 * @author Kathrin Schaffert (#16533 changed Combos into CCombo and added COMBO_WIDTH, INSERT_BUTTON_WIDTH, refactoring)
 */
public class ScriptConfigurationPage extends ToolIntegrationWizardPage {

    enum ComboType {
        INPUT_COMBO,
        OUTPUT_COMBO,
        PROPERTY_COMBO,
        ADD_PROPERTY_COMBO,
        DIRECTORY_COMBO
    }

    private static final int COMBO_WIDTH = 125;

    private static final int INSERT_BUTTON_WIDTH = 50;

    private static final int MOCK_GROUP_MINIMUM_HEIGHT = 67;

    private static final String HELP_CONTEXT_ID = "de.rcenvironment.core.gui.wizard.toolintegration.integration_execution";

    private static final int TEXTFIELD_HEIGHT = 270;

    private static final int TEXTFIELD_WIDTH = 300;

    private static final int NUMBER_OF_TABS = 4;

    // To avoid dependency cycles this key exists also in the file CpacsToolIntegrationConstants.java,
    // see KEY_MOCK_TOOL_OUTPUT_FILENAME
    private static final String CPACS_MOCK_TOOL_OUTPUT_FILENAME = "imitationToolOutputFilename";

    protected Map<String, Object> configurationMap;

    private final CCombo[] inputCombos;

    private final CCombo[] outputCombos;

    private final CCombo[] propertiesCombos;

    private final CCombo[] directoryCombos;

    private final LineNumberStyledText[] textFields;

    private Button winEnabledButton = null;

    private Button linuxEnabledButton = null;

    private boolean winScriptHasFocus = false;

    private Button noErrorOnOtherExitCodeButton;

    private Button setWorkingDirAsCwdButton;

    private Button setToolDirAsCwdButton;

    private Label executionPathLabel;

    private Button mockModeCheckBox;

    private CTabFolder tabFolder;

    private Composite mockScriptTabComposite;

    private Composite mockScriptTabButtonComposite;

    private Text mockToolOutputFilenameText;

    private Label mockToolOutputFilenameLabel;

    protected ScriptConfigurationPage(String pageName, Map<String, Object> configurationMap) {
        super(pageName);
        setTitle(pageName);
        setDescription(Messages.scriptPageDescription);
        this.configurationMap = configurationMap;
        inputCombos = new CCombo[NUMBER_OF_TABS];
        outputCombos = new CCombo[NUMBER_OF_TABS];
        propertiesCombos = new CCombo[NUMBER_OF_TABS];
        directoryCombos = new CCombo[NUMBER_OF_TABS];
        textFields = new LineNumberStyledText[NUMBER_OF_TABS + 1];

    }

    @Override
    public void createControl(Composite parent) {
        Composite container = new Composite(parent, SWT.NONE);
        container.setLayout(new GridLayout(1, false));
        tabFolder = new CTabFolder(container, SWT.BORDER);
        GridData layoutData = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL
            | GridData.FILL_VERTICAL | GridData.GRAB_VERTICAL);
        layoutData.grabExcessVerticalSpace = true;
        tabFolder.setLayoutData(layoutData);
        createScriptExecutionTabItem(ToolIntegrationConstants.KEY_COMMAND_SCRIPT_LINUX, Messages.commandScriptMessage);
        createScriptTabItem(ToolIntegrationConstants.KEY_PRE_SCRIPT, Messages.preScript, 1);
        createScriptTabItem(ToolIntegrationConstants.KEY_POST_SCRIPT, Messages.postScript, 2);
        createScriptTabItem(ToolIntegrationConstants.KEY_MOCK_SCRIPT, "Tool run imitation script", 3);

        tabFolder.setSelection(0);

        createMockModeGroup(container);

        setControl(container);

        PlatformUI.getWorkbench().getHelpSystem().setHelp(this.getControl(),
            HELP_CONTEXT_ID);

        updatePageComplete();
    }

    private void createMockModeGroup(Composite container) {
        GridData layoutData;
        Group mockGroup = new Group(container, SWT.NONE);
        mockGroup.setText("Tool run imitation mode");
        mockGroup.setLayout(new GridLayout(2, false));
        layoutData = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
        layoutData.minimumHeight = MOCK_GROUP_MINIMUM_HEIGHT;
        mockGroup.setLayoutData(layoutData);
        mockModeCheckBox = new Button(mockGroup, SWT.CHECK);
        layoutData = new GridData();
        layoutData.horizontalSpan = 2;
        mockModeCheckBox.setLayoutData(layoutData);
        mockModeCheckBox.setText("Support tool run imitation");
        mockModeCheckBox.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent event) {
                setMockScriptTabEnabled(mockModeCheckBox.getSelection());
                if (mockToolOutputFilenameText != null) {
                    mockToolOutputFilenameText.setEnabled(mockModeCheckBox.getSelection());
                }
                configurationMap.put(ToolIntegrationConstants.KEY_MOCK_MODE_SUPPORTED, mockModeCheckBox.getSelection());
                updatePageComplete();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent event) {
                widgetSelected(event);
            }
        });

        // FIXME: CPACS-specific stuff must not be handled here; temporary workaround as wizard will
        // be replaced by an editor soon
        mockToolOutputFilenameLabel = new Label(mockGroup, SWT.NONE);
        mockToolOutputFilenameLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
        mockToolOutputFilenameLabel.setText("Dummy tool output filename:");

        mockToolOutputFilenameText = new Text(mockGroup, SWT.BORDER);
        mockToolOutputFilenameText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        mockToolOutputFilenameText.addModifyListener(new TextAreaModifyListener(CPACS_MOCK_TOOL_OUTPUT_FILENAME));

        mockToolOutputFilenameLabel.setVisible(false);
        mockToolOutputFilenameText.setVisible(false);
    }

    private void setChildrenEnabled(Composite parent, boolean enabled) {
        for (Control control : parent.getChildren()) {
            control.setEnabled(enabled);
        }
    }

    private void setMockScriptTabEnabled(boolean enabled) {
        setChildrenEnabled(mockScriptTabComposite, enabled);
        setChildrenEnabled(mockScriptTabButtonComposite, enabled);
    }

    /**
     * Updates all Combos when page is shown.
     */
    @Override
    @SuppressWarnings("unchecked")
    public void updatePage() {
        for (int i = 0; i < NUMBER_OF_TABS; i++) {
            addAllEndpoints(inputCombos[i], IntegrationConstants.KEY_ENDPOINT_INPUTS, i);
            setComboEnabled(inputCombos[i]);
            if (i > 0) {
                addAllEndpoints(outputCombos[i], IntegrationConstants.KEY_ENDPOINT_OUTPUTS, i);
            }
            setComboEnabled(outputCombos[i]);
            propertiesCombos[i].removeAll();
            if (configurationMap.containsKey(IntegrationConstants.KEY_PROPERTIES)) {
                Map<String, Object> properties = (Map<String, Object>) configurationMap.get(IntegrationConstants.KEY_PROPERTIES);
                for (Object proptabObject : properties.values()) {
                    Map<String, Object> proptab = (Map<String, Object>) proptabObject;
                    for (Object propertyObject : proptab.values()) {
                        if (propertyObject instanceof Map<?, ?>) {
                            Map<String, String> property = (Map<String, String>) propertyObject;
                            propertiesCombos[i].add((property.get(IntegrationConstants.KEY_PROPERTY_DISPLAYNAME)));
                        }
                    }
                }
            }
            if (propertiesCombos[i].getItemCount() > 0) {
                propertiesCombos[i].select(0);
            }
            setComboEnabled(propertiesCombos[i]);
            directoryCombos[i].select(0);
            boolean mockModeSupported = configurationMap.containsKey(ToolIntegrationConstants.KEY_MOCK_MODE_SUPPORTED)
                && (boolean) configurationMap.get(ToolIntegrationConstants.KEY_MOCK_MODE_SUPPORTED);
            setMockScriptTabEnabled(mockModeSupported);
            mockModeCheckBox.setSelection(mockModeSupported);
            if (mockToolOutputFilenameText != null) {
                mockToolOutputFilenameText.setEnabled(mockModeSupported);
            }
        }

        boolean isCPACSType = ((ToolIntegrationWizard) getWizard()).getCurrentContext().getContextTypeString().equalsIgnoreCase("CPACS");

        mockToolOutputFilenameLabel.setVisible(isCPACSType);
        mockToolOutputFilenameText.setVisible(isCPACSType);
        updateButtons();
        updatePageComplete();
    }

    private void setComboEnabled(CCombo combo) {
        if (combo != null) {
            combo.setEnabled(combo.getItemCount() != 0);
        }
    }

    private void updateButtons() {

        boolean windowsEnabled = false;
        boolean linuxEnabled = false;
        if (configurationMap.get(ToolIntegrationConstants.KEY_COMMAND_SCRIPT_WINDOWS_ENABLED) != null) {
            windowsEnabled = (Boolean) configurationMap.get(ToolIntegrationConstants.KEY_COMMAND_SCRIPT_WINDOWS_ENABLED);
        }
        if (configurationMap.get(ToolIntegrationConstants.KEY_COMMAND_SCRIPT_LINUX_ENABLED) != null) {
            linuxEnabled = (Boolean) configurationMap.get(ToolIntegrationConstants.KEY_COMMAND_SCRIPT_LINUX_ENABLED);
        }
        winEnabledButton.setSelection(windowsEnabled);
        linuxEnabledButton.setSelection(linuxEnabled);

        textFields[0].setEditable(linuxEnabled);
        textFields[0].setEnabled(linuxEnabled);
        textFields[textFields.length - 1].setEditable(windowsEnabled);
        textFields[textFields.length - 1].setEnabled(windowsEnabled);

        if ((String) configurationMap.get(ToolIntegrationConstants.KEY_COMMAND_SCRIPT_LINUX) != null) {
            textFields[0].setText((String) configurationMap.get(ToolIntegrationConstants.KEY_COMMAND_SCRIPT_LINUX));
        } else {
            textFields[0].setText("");
        }
        if ((String) configurationMap.get(ToolIntegrationConstants.KEY_COMMAND_SCRIPT_WINDOWS) != null) {
            textFields[textFields.length - 1].setText((String) configurationMap
                .get(ToolIntegrationConstants.KEY_COMMAND_SCRIPT_WINDOWS));
        } else {
            textFields[textFields.length - 1].setText("");
        }
        if ((String) configurationMap.get(ToolIntegrationConstants.KEY_PRE_SCRIPT) != null) {
            textFields[1].setText((String) configurationMap.get(ToolIntegrationConstants.KEY_PRE_SCRIPT));
        } else {
            textFields[1].setText("");
        }
        if ((String) configurationMap.get(ToolIntegrationConstants.KEY_POST_SCRIPT) != null) {
            textFields[2].setText((String) configurationMap.get(ToolIntegrationConstants.KEY_POST_SCRIPT));
        } else {
            textFields[2].setText("");
        }
        if ((String) configurationMap.get(ToolIntegrationConstants.KEY_MOCK_SCRIPT) != null) {
            textFields[3].setText((String) configurationMap.get(ToolIntegrationConstants.KEY_MOCK_SCRIPT));
        } else {
            textFields[3].setText("");
        }

        if (configurationMap.get(ToolIntegrationConstants.DONT_CRASH_ON_NON_ZERO_EXIT_CODES) != null) {
            noErrorOnOtherExitCodeButton.setSelection((Boolean) configurationMap
                .get(ToolIntegrationConstants.DONT_CRASH_ON_NON_ZERO_EXIT_CODES));
        }
        if (configurationMap.get(ToolIntegrationConstants.KEY_SET_TOOL_DIR_AS_WORKING_DIR) != null) {
            boolean toolDirIsCwd = (Boolean) configurationMap.get(ToolIntegrationConstants.KEY_SET_TOOL_DIR_AS_WORKING_DIR);
            setWorkingDirAsCwdButton.setSelection(!toolDirIsCwd);
            setToolDirAsCwdButton.setSelection(toolDirIsCwd);
        } else {
            setWorkingDirAsCwdButton.setSelection(true);
            setToolDirAsCwdButton.setSelection(false);
        }

        setWorkingDirAsCwdButton.setEnabled(true);
        setToolDirAsCwdButton.setEnabled(true);
        executionPathLabel.setEnabled(true);
        for (String key : ((ToolIntegrationWizard) this.getWizard()).getCurrentContext().getDisabledIntegrationKeys()) {
            if (ToolIntegrationConstants.KEY_SET_TOOL_DIR_AS_WORKING_DIR.equals(key)) {
                setToolDirAsCwdButton.setEnabled(false);
                setWorkingDirAsCwdButton.setEnabled(false);
                executionPathLabel.setEnabled(false);
            }
        }

        if (mockToolOutputFilenameText != null) {
            if ((String) configurationMap.get(CPACS_MOCK_TOOL_OUTPUT_FILENAME) != null) {
                mockToolOutputFilenameText.setText((String) configurationMap.get(CPACS_MOCK_TOOL_OUTPUT_FILENAME));
            } else {
                mockToolOutputFilenameText.setText("");
            }
        }
    }

    private void updatePageComplete() {
        boolean winEnabled = (configurationMap.get(ToolIntegrationConstants.KEY_COMMAND_SCRIPT_WINDOWS_ENABLED) != null
            && (Boolean) configurationMap.get(ToolIntegrationConstants.KEY_COMMAND_SCRIPT_WINDOWS_ENABLED));
        boolean linuxEnabled =
            (configurationMap.get(ToolIntegrationConstants.KEY_COMMAND_SCRIPT_LINUX_ENABLED) != null && (Boolean) configurationMap
                .get(ToolIntegrationConstants.KEY_COMMAND_SCRIPT_LINUX_ENABLED));
        if (!(winEnabled || linuxEnabled)) {
            setPageComplete(false);
        } else {
            boolean winScriptNotEmpty = ((String) configurationMap.get(ToolIntegrationConstants.KEY_COMMAND_SCRIPT_WINDOWS)) != null
                && !((String) configurationMap.get(ToolIntegrationConstants.KEY_COMMAND_SCRIPT_WINDOWS)).trim().isEmpty();
            boolean linuxScriptNotEmpty = ((String) configurationMap.get(ToolIntegrationConstants.KEY_COMMAND_SCRIPT_LINUX)) != null
                && !((String) configurationMap.get(ToolIntegrationConstants.KEY_COMMAND_SCRIPT_LINUX)).trim().isEmpty();
            if ((winEnabled && winScriptNotEmpty) || (linuxEnabled && linuxScriptNotEmpty)) {
                validateIsMockScriptConfiguration();
            } else {
                setMessage(Messages.toolExecutionCommandNeeded, DialogPage.ERROR);
                setPageComplete(false);
            }
        }
    }

    private LineNumberStyledText createScriptExecutionTabItem(String propertyKey, String name) {
        CTabItem item = new CTabItem(tabFolder, SWT.NONE);
        item.setText(name);
        Composite client = new Composite(tabFolder, SWT.NONE);
        winEnabledButton = new Button(client, SWT.CHECK);
        winEnabledButton.setText(Messages.winCommandUse);
        linuxEnabledButton = new Button(client, SWT.CHECK);
        linuxEnabledButton.setText(Messages.linuxCommandUse);
        new Label(client, SWT.NONE);

        final LineNumberStyledText scriptAreaWin;
        client.setLayout(new GridLayout(3, false));
        scriptAreaWin = new LineNumberStyledText(client, SWT.MULTI | SWT.BORDER | SWT.WRAP | SWT.V_SCROLL);
        GridData scriptAreaWinData = new GridData(GridData.FILL_BOTH);
        scriptAreaWinData.widthHint = TEXTFIELD_WIDTH / 2;
        scriptAreaWinData.heightHint = TEXTFIELD_HEIGHT;
        scriptAreaWin.setLayoutData(scriptAreaWinData);
        scriptAreaWin.addModifyListener(new TextAreaModifyListener(ToolIntegrationConstants.KEY_COMMAND_SCRIPT_WINDOWS));
        scriptAreaWin.addFocusListener(new FocusListener() {

            @Override
            public void focusLost(FocusEvent arg0) {}

            @Override
            public void focusGained(FocusEvent arg0) {
                winScriptHasFocus = true;
            }
        });
        scriptAreaWin.setEditable(false);
        textFields[textFields.length - 1] = scriptAreaWin;

        item.setControl(client);
        final LineNumberStyledText scriptArea = new LineNumberStyledText(client, SWT.MULTI | SWT.BORDER | SWT.WRAP | SWT.V_SCROLL);
        GridData scriptAreaData = new GridData(GridData.FILL_BOTH);
        scriptArea.setEnabled(false);
        scriptArea.setEditable(false);
        scriptArea.addFocusListener(new FocusListener() {

            @Override
            public void focusLost(FocusEvent arg0) {}

            @Override
            public void focusGained(FocusEvent arg0) {
                winScriptHasFocus = false;
            }
        });
        scriptAreaData.widthHint = TEXTFIELD_WIDTH / 2;
        scriptAreaData.heightHint = TEXTFIELD_HEIGHT;
        scriptArea.setLayoutData(scriptAreaData);
        scriptArea.addModifyListener(new TextAreaModifyListener(propertyKey));

        textFields[0] = scriptArea;
        addScriptSelectButtonListener();

        createInsertFieldsOnExecutionTab(client, scriptAreaWin, scriptArea);

        new Label(client, SWT.NONE).setText(Messages.scriptLanguageHintBatch);
        new Label(client, SWT.NONE).setText(Messages.scriptLanguageHintBash);
        new Label(client, SWT.NONE).setText("");

        Group executionPropertiesGroup = new Group(client, SWT.NONE);
        executionPropertiesGroup.setText("Execution Options");
        executionPropertiesGroup.setLayout(new GridLayout(1, false));
        GridData executionPropertiesData = new GridData(GridData.GRAB_HORIZONTAL | GridData.FILL_HORIZONTAL);
        executionPropertiesData.horizontalSpan = 3;
        executionPropertiesGroup.setLayoutData(executionPropertiesData);

        noErrorOnOtherExitCodeButton = new Button(executionPropertiesGroup, SWT.CHECK);
        noErrorOnOtherExitCodeButton.setText(Messages.dontCrashOtherThanZeroLabel);

        noErrorOnOtherExitCodeButton.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent arg0) {
                configurationMap.put(ToolIntegrationConstants.DONT_CRASH_ON_NON_ZERO_EXIT_CODES,
                    noErrorOnOtherExitCodeButton.getSelection());
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent arg0) {
                widgetSelected(arg0);
            }
        });
        executionPathLabel = new Label(executionPropertiesGroup, SWT.NONE);
        executionPathLabel.setText("Execute (command(s), pre execution/post execution/tool run imitation script) from");
        setWorkingDirAsCwdButton = new Button(executionPropertiesGroup, SWT.RADIO);
        setWorkingDirAsCwdButton.setSelection(true);
        setWorkingDirAsCwdButton.setText("Working directory");
        setWorkingDirAsCwdButton.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent arg0) {
                configurationMap.put(ToolIntegrationConstants.KEY_SET_TOOL_DIR_AS_WORKING_DIR,
                    !setWorkingDirAsCwdButton.getSelection());
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent arg0) {
                widgetSelected(arg0);
            }
        });
        setToolDirAsCwdButton = new Button(executionPropertiesGroup, SWT.RADIO);
        setToolDirAsCwdButton.setText("Tool directory");
        setToolDirAsCwdButton.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent arg0) {
                configurationMap.put(ToolIntegrationConstants.KEY_SET_TOOL_DIR_AS_WORKING_DIR,
                    setToolDirAsCwdButton.getSelection());
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent arg0) {
                widgetSelected(arg0);
            }
        });

        return scriptArea;
    }

    private LineNumberStyledText createScriptTabItem(String propertyKey, String name, int tabIndex) {
        CTabItem item = new CTabItem(tabFolder, SWT.NONE);
        item.setText(name);
        Composite client = new Composite(tabFolder, SWT.NONE);
        client.setLayout(new GridLayout(2, false));

        item.setControl(client);
        final LineNumberStyledText scriptArea = new LineNumberStyledText(client, SWT.MULTI | SWT.BORDER | SWT.WRAP | SWT.V_SCROLL);
        GridData scriptAreaData = new GridData(GridData.FILL_BOTH);

        scriptAreaData.widthHint = TEXTFIELD_WIDTH;
        scriptAreaData.heightHint = TEXTFIELD_HEIGHT;
        scriptArea.setLayoutData(scriptAreaData);
        scriptArea.addModifyListener(new TextAreaModifyListener(propertyKey));

        textFields[tabIndex] = scriptArea;

        createInsertFields(tabIndex, client, scriptArea);

        Label jythonLabel = new Label(client, SWT.NONE);
        jythonLabel.setText(Messages.scriptLanguageHint);

        if (tabIndex == 3) {
            mockScriptTabComposite = client;
        }

        return scriptArea;
    }

    private void validateIsMockScriptConfiguration() {
        if (IntegrationContextType.COMMON.toString()
            .equals(configurationMap.get(IntegrationConstants.INTEGRATION_TYPE))
            || configurationMap.get(IntegrationConstants.INTEGRATION_TYPE) == null) {
            if (mockModeCheckBox.getSelection() && textFields[3].getText().isEmpty() && !mockToolOutputFilenameText.isVisible()) {
                setMessage("Tool run imitation mode is supported but no tool run imitation script is configured.", IMessageProvider.ERROR);
                setPageComplete(false);
            } else if (mockModeCheckBox.getSelection() && mockToolOutputFilenameText.isVisible()
                && mockToolOutputFilenameText.getText().isEmpty()) {
                setMessage("Tool run imitation mode is supported but no dummy tool output filename is configured.", IMessageProvider.ERROR);
                setPageComplete(false);
            } else {
                setMessage(null, IMessageProvider.NONE);
                setPageComplete(true);
            }
        } else {
            setMessage(null, IMessageProvider.NONE);
            setPageComplete(true);
        }
    }

    private void createInsertFields(int tabIndex, Composite client, final LineNumberStyledText scriptArea) {
        Composite buttonComposite = createButtonComposite(client);

        CCombo inputCombo = createCombo(buttonComposite, Messages.inputs);
        Button inputInsertButton = createInsertButton(buttonComposite);
        addInsertButtonSelectionListener(inputInsertButton, inputCombo, ComboType.INPUT_COMBO, scriptArea);
        inputCombos[tabIndex] = inputCombo;

        CCombo outputCombo = createCombo(buttonComposite, Messages.outputs);
        Button outputInsertButton = createInsertButton(buttonComposite);
        addInsertButtonSelectionListener(outputInsertButton, outputCombo, ComboType.OUTPUT_COMBO, scriptArea);
        outputCombos[tabIndex] = outputCombo;

        CCombo propertiesCombo = createCombo(buttonComposite, Messages.properties);
        Button propertyInsertButton = createInsertButton(buttonComposite);
        addInsertButtonSelectionListener(propertyInsertButton, propertiesCombo, ComboType.PROPERTY_COMBO, scriptArea);
        propertiesCombos[tabIndex] = propertiesCombo;

        CCombo directoriesCombo = createCombo(buttonComposite, Messages.directory);
        directoriesCombo.setItems(ToolIntegrationConstants.DIRECTORIES_PLACEHOLDERS_DISPLAYNAMES);
        Button directoryInsertButton = createInsertButton(buttonComposite);
        addInsertButtonSelectionListener(directoryInsertButton, directoriesCombo, ComboType.DIRECTORY_COMBO, scriptArea);
        directoryCombos[tabIndex] = directoriesCombo;

        if (tabIndex == 2) {
            CCombo addPropCombo = createCombo(buttonComposite, "Additional Properties");
            addPropCombo.add(Messages.exitCodeLabel);
            addPropCombo.select(0);
            Button addPropInsertButton = createInsertButton(buttonComposite);
            addInsertButtonSelectionListener(addPropInsertButton, addPropCombo, ComboType.ADD_PROPERTY_COMBO, scriptArea);
        }
        createInsertCopyFileDirArea(scriptArea, buttonComposite);
        if (tabIndex == 3) {
            mockScriptTabButtonComposite = buttonComposite;
        }

    }

    private void createInsertFieldsOnExecutionTab(Composite client, final LineNumberStyledText scriptAreaWin,
        final LineNumberStyledText scriptArea) {
        Composite buttonComposite = createButtonComposite(client);

        CCombo inputCombo = createCombo(buttonComposite, Messages.inputs);
        Button inputInsertButton = createInsertButton(buttonComposite);
        addInsertButtonSelectionListener(inputInsertButton, inputCombo, ComboType.INPUT_COMBO, scriptAreaWin, scriptArea);
        inputCombos[0] = inputCombo;

        CCombo propertiesCombo = createCombo(buttonComposite, Messages.properties);
        Button propertyInsertButton = createInsertButton(buttonComposite);
        addInsertButtonSelectionListener(propertyInsertButton, propertiesCombo, ComboType.PROPERTY_COMBO, scriptAreaWin, scriptArea);
        propertiesCombos[0] = propertiesCombo;

        CCombo directoriesCombo = createCombo(buttonComposite, Messages.directory);
        directoriesCombo.setItems(ToolIntegrationConstants.DIRECTORIES_PLACEHOLDERS_DISPLAYNAMES);
        Button directoryInsertButton = createInsertButton(buttonComposite);
        addInsertButtonSelectionListener(directoryInsertButton, directoriesCombo, ComboType.DIRECTORY_COMBO, scriptAreaWin, scriptArea);
        directoryCombos[0] = directoriesCombo;
    }

    private Composite createButtonComposite(Composite client) {
        Composite buttonComposite = new Composite(client, SWT.NONE);
        buttonComposite.setLayout(new GridLayout(2, false));
        GridData buttonCompositeData = new GridData();
        buttonCompositeData.verticalAlignment = GridData.BEGINNING;
        buttonCompositeData.horizontalSpan = 1;
        buttonComposite.setLayoutData(buttonCompositeData);
        return buttonComposite;
    }

    private void createInsertCopyFileDirArea(final LineNumberStyledText scriptArea, Composite buttonComposite) {
        new Label(buttonComposite, SWT.NONE).setText("");
        new Label(buttonComposite, SWT.NONE).setText("");
        Button insertCopyCommand = new Button(buttonComposite, SWT.PUSH);
        GridData copyData = new GridData();
        insertCopyCommand.setLayoutData(copyData);
        insertCopyCommand.setText("Insert copy of file/dir...");
        insertCopyCommand.addSelectionListener(new CopyInputListener(scriptArea));
    }

    private CCombo createCombo(Composite buttonComposite, String messageLabel) {
        GridData labelData = new GridData();
        labelData.horizontalSpan = 2;
        Label label = new Label(buttonComposite, SWT.NONE);
        label.setText(messageLabel);
        label.setLayoutData(labelData);
        CCombo combo = new CCombo(buttonComposite, SWT.READ_ONLY | SWT.BORDER);
        GridData comboData = new GridData(GridData.FILL_HORIZONTAL);
        comboData.widthHint = COMBO_WIDTH;
        combo.setLayoutData(comboData);

        return combo;
    }

    private Button createInsertButton(Composite buttonComposite) {
        GridData insertButtonData = new GridData();
        insertButtonData.horizontalSpan = 1;
        insertButtonData.widthHint = INSERT_BUTTON_WIDTH;
        Button inputInsertButton = new Button(buttonComposite, SWT.PUSH);
        inputInsertButton.setLayoutData(insertButtonData);
        inputInsertButton.setText(Messages.insertButtonLabel);
        return inputInsertButton;
    }

    private void addInsertButtonSelectionListener(Button button, CCombo combo,
        ComboType comboType, final LineNumberStyledText scriptAreaWin, final LineNumberStyledText scriptArea) {
        button.addSelectionListener(
            new InsertButtonListener(combo, scriptArea, scriptAreaWin, comboType));

    }

    private void addInsertButtonSelectionListener(Button button, CCombo combo, ComboType comboType, final LineNumberStyledText scriptArea) {
        button.addSelectionListener(new InsertButtonListener(combo, scriptArea, comboType));
    }

    private void addScriptSelectButtonListener() {
        winEnabledButton.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent arg0) {
                textFields[NUMBER_OF_TABS].setEnabled(winEnabledButton.getSelection());
                textFields[NUMBER_OF_TABS].setEditable(winEnabledButton.getSelection());
                configurationMap.put(ToolIntegrationConstants.KEY_COMMAND_SCRIPT_WINDOWS_ENABLED, winEnabledButton.getSelection());
                updatePageComplete();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent arg0) {
                widgetSelected(arg0);
            }
        });
        linuxEnabledButton.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent arg0) {
                textFields[0].setEnabled(linuxEnabledButton.getSelection());
                textFields[0].setEditable(linuxEnabledButton.getSelection());
                configurationMap.put(ToolIntegrationConstants.KEY_COMMAND_SCRIPT_LINUX_ENABLED, linuxEnabledButton.getSelection());
                updatePageComplete();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent arg0) {
                widgetSelected(arg0);
            }
        });
    }

    private void addAllEndpoints(CCombo endpointCombo, String key, int tabNumber) {
        endpointCombo.removeAll();
        @SuppressWarnings("unchecked") List<Map<String, String>> endpointList = (List<Map<String, String>>) configurationMap.get(key);
        if (endpointList != null) {
            for (Map<String, String> endpoint : endpointList) {
                if (!(tabNumber == 0 && (endpoint.get(IntegrationConstants.KEY_ENDPOINT_DATA_TYPE).equals(DataType.Vector.name())
                    || endpoint.get(IntegrationConstants.KEY_ENDPOINT_DATA_TYPE).equals(DataType.Matrix.name())))) {
                    endpointCombo.add(endpoint.get(IntegrationConstants.KEY_ENDPOINT_NAME));
                }
            }
            if (endpointCombo.getItemCount() > 0) {
                endpointCombo.select(0);
            }
        }
    }

    /**
     * Listener for the script text areas for saving the content to the correct key.
     * 
     * @author Sascha Zur
     */
    private class TextAreaModifyListener implements ModifyListener {

        private final String key;

        TextAreaModifyListener(String key) {
            this.key = key;
        }

        @Override
        public void modifyText(ModifyEvent arg0) {
            Object obj = arg0.getSource();
            if (obj instanceof Text) {
                configurationMap.put(key, ((Text) obj).getText());
            } else if (obj instanceof LineNumberStyledText) {
                configurationMap.put(key, ((LineNumberStyledText) obj).getText());
            }

            updatePageComplete();
        }
    }

    /**
     * Listener to insert the current selected text from the combo box to the given text.
     * 
     * @author Sascha Zur
     */
    private class InsertButtonListener implements SelectionListener {

        private static final String QUOTE = "\"";

        private final CCombo combo;

        private final LineNumberStyledText text;

        private final ComboType comboType;

        private LineNumberStyledText text2;

        InsertButtonListener(CCombo inputCombo, LineNumberStyledText scriptArea, ComboType comboType) {
            combo = inputCombo;
            text = scriptArea;
            this.comboType = comboType;
        }

        InsertButtonListener(CCombo inputCombo, LineNumberStyledText scriptArea, LineNumberStyledText scriptArea2, ComboType comboType) {
            combo = inputCombo;
            text = scriptArea;
            text2 = scriptArea2;
            this.comboType = comboType;
        }

        @Override
        public void widgetDefaultSelected(SelectionEvent arg0) {
            widgetSelected(arg0);

        }

        @SuppressWarnings("unchecked")
        @Override
        public void widgetSelected(SelectionEvent arg0) {
            String insertText = combo.getText();
            LineNumberStyledText currentText = text;
            if (winScriptHasFocus && text2 != null) {
                currentText = text2;
            }
            if (currentText.isEnabled()) {
                if (comboType.equals(ComboType.INPUT_COMBO) && insertText != null && !insertText.isEmpty()) {
                    int distanceCaretPositionToTextLength = currentText.getText().length() - currentText.getSelection().x;
                    String possibleQuotes = "";
                    List<Map<String, Object>> endpointList =
                        (List<Map<String, Object>>) configurationMap.get(IntegrationConstants.KEY_ENDPOINT_INPUTS);
                    Map<String, Object> endpoint = null;
                    for (Map<String, Object> ep : endpointList) {
                        if (ep.get(IntegrationConstants.KEY_ENDPOINT_NAME).equals(insertText)) {
                            endpoint = ep;
                        }
                    }
                    if (endpoint != null) {
                        if (endpoint.get(IntegrationConstants.KEY_ENDPOINT_DATA_TYPE).equals(DataType.ShortText.name())
                            || endpoint.get(IntegrationConstants.KEY_ENDPOINT_DATA_TYPE).equals(DataType.FileReference.name())
                            || endpoint.get(IntegrationConstants.KEY_ENDPOINT_DATA_TYPE).equals(DataType.DirectoryReference.name())) {
                            possibleQuotes = QUOTE;
                        }
                    }
                    currentText.insert(possibleQuotes + ToolIntegrationConstants.PLACEHOLDER_PREFIX
                        + ToolIntegrationConstants.PLACEHOLDER_INPUT_PREFIX
                        + ToolIntegrationConstants.PLACEHOLDER_SEPARATOR + insertText + ToolIntegrationConstants.PLACEHOLDER_SUFFIX
                        + possibleQuotes);
                    currentText.setSelection(currentText.getText().length() - distanceCaretPositionToTextLength);
                }
                if (comboType.equals(ComboType.OUTPUT_COMBO) && insertText != null && !insertText.isEmpty()) {
                    int distanceCaretPositionToTextLength = currentText.getText().length() - currentText.getSelection().x;
                    currentText.insert(ToolIntegrationConstants.PLACEHOLDER_PREFIX + ToolIntegrationConstants.PLACEHOLDER_OUTPUT_PREFIX
                        + ToolIntegrationConstants.PLACEHOLDER_SEPARATOR + insertText + ToolIntegrationConstants.PLACEHOLDER_SUFFIX);
                    currentText.setSelection(currentText.getText().length() - distanceCaretPositionToTextLength);
                }
                if (comboType.equals(ComboType.PROPERTY_COMBO) && insertText != null && !insertText.isEmpty()
                    && configurationMap.containsKey(IntegrationConstants.KEY_PROPERTIES)) {
                    int distanceCaretPositionToTextLength = currentText.getText().length() - currentText.getSelection().x;
                    Map<String, Object> properties = (Map<String, Object>) configurationMap.get(IntegrationConstants.KEY_PROPERTIES);
                    for (String propTabName : properties.keySet()) {
                        Map<String, Object> proptab = (Map<String, Object>) properties.get(propTabName);
                        for (Map.Entry<String, Object> proptabEntry : proptab.entrySet()) {
                            final String propkey = proptabEntry.getKey();
                            final Object propertyObject = proptabEntry.getValue();
                            if (propertyObject instanceof Map<?, ?>) {
                                Map<String, String> property = (Map<String, String>) propertyObject;
                                if (property.get(IntegrationConstants.KEY_PROPERTY_DISPLAYNAME).equals(insertText)) {
                                    currentText.insert(QUOTE + ToolIntegrationConstants.PLACEHOLDER_PREFIX
                                        + ToolIntegrationConstants.PLACEHOLDER_PROPERTY_PREFIX
                                        + ToolIntegrationConstants.PLACEHOLDER_SEPARATOR
                                        + propkey + ToolIntegrationConstants.PLACEHOLDER_SUFFIX + QUOTE);
                                }
                            }
                        }
                    }
                    currentText.setSelection(currentText.getText().length() - distanceCaretPositionToTextLength);
                }
                if (comboType.equals(ComboType.DIRECTORY_COMBO) && insertText != null && !insertText.isEmpty()) {
                    int distanceCaretPositionToTextLength = currentText.getText().length() - currentText.getSelection().x;
                    currentText.insert(QUOTE + ToolIntegrationConstants.PLACEHOLDER_PREFIX
                        + ToolIntegrationConstants.PLACEHOLDER_DIRECTORY_PREFIX
                        + ToolIntegrationConstants.PLACEHOLDER_SEPARATOR
                        + ToolIntegrationConstants.DIRECTORIES_PLACEHOLDER[combo.getSelectionIndex()]
                        + ToolIntegrationConstants.PLACEHOLDER_SUFFIX + QUOTE);
                    currentText.setSelection(currentText.getText().length() - distanceCaretPositionToTextLength);
                }

                if (comboType.equals(ComboType.ADD_PROPERTY_COMBO) && insertText != null && !insertText.isEmpty()) {
                    currentText.insert(createAddPropertyPlaceHolder(ToolIntegrationConstants.PLACEHOLDER_EXIT_CODE));
                }
                currentText.setFocus();
            }
        }

        private String createAddPropertyPlaceHolder(String addPropPlaceholder) {
            return ToolIntegrationConstants.PLACEHOLDER_PREFIX
                + ToolIntegrationConstants.PLACEHOLDER_ADDITIONAL_PROPERTIES_PREFIX
                + ToolIntegrationConstants.PLACEHOLDER_SEPARATOR
                + addPropPlaceholder
                + ToolIntegrationConstants.PLACEHOLDER_SUFFIX;
        }
    }


    /**
     * Listener for the insert copy command button.
     * 
     * @author Sascha Zur
     */
    private class CopyInputListener implements SelectionListener {

        private final LineNumberStyledText text;

        CopyInputListener(LineNumberStyledText text) {
            this.text = text;
        }

        @Override
        public void widgetDefaultSelected(SelectionEvent arg0) {
            widgetSelected(arg0);

        }

        @Override
        public void widgetSelected(SelectionEvent arg0) {
            @SuppressWarnings("unchecked") List<Map<String, String>> endpointList =
                (List<Map<String, String>>) configurationMap.get(IntegrationConstants.KEY_ENDPOINT_INPUTS);
            List<String> endpointNames = new LinkedList<>();
            if (endpointList != null) {
                for (Map<String, String> endpoint : endpointList) {
                    if (endpoint.get(IntegrationConstants.KEY_ENDPOINT_DATA_TYPE).equals(DataType.FileReference.name())) {
                        endpointNames.add("File : " + endpoint.get(IntegrationConstants.KEY_ENDPOINT_NAME));
                    }
                    if (endpoint.get(IntegrationConstants.KEY_ENDPOINT_DATA_TYPE).equals(DataType.DirectoryReference.name())) {
                        endpointNames.add("Directory : " + endpoint.get(IntegrationConstants.KEY_ENDPOINT_NAME));
                    }
                }
            }
            WizardInsertCopyCommandDialog dialog =
                new WizardInsertCopyCommandDialog(getShell(), endpointNames, directoryCombos[1].getItems());
            if (dialog.open() == Dialog.OK) {
                String command = dialog.getCopyCommand();
                if (!text.isDisposed()) {
                    text.insert(command + "\n");
                    text.setFocus();
                }
            }
        }
    }

    /**
     * Sets a new configurationMap and updates all fields.
     * 
     * @param newConfigurationMap new map
     */
    @Override
    public void setConfigMap(Map<String, Object> newConfigurationMap) {
        configurationMap = newConfigurationMap;
        updatePageValues();
    }

    private void updatePageValues() {
        updatePage();
        updatePageComplete();
    }

    @Override
    public void performHelp() {
        super.performHelp();
        IWorkbenchHelpSystem helpSystem = PlatformUI.getWorkbench().getHelpSystem();
        helpSystem.displayHelp(HELP_CONTEXT_ID);
    }

}
