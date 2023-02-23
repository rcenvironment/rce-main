/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.optimizer.gui.properties;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Widget;

import de.rcenvironment.components.optimizer.common.MethodDescription;
import de.rcenvironment.components.optimizer.common.OptimizerComponentConstants;
import de.rcenvironment.core.gui.resources.api.ImageManager;
import de.rcenvironment.core.gui.resources.api.StandardImages;
import de.rcenvironment.core.gui.utils.incubator.NumericalTextConstraintListener;
import de.rcenvironment.core.gui.utils.incubator.WidgetGroupFactory;

/**
 * This class is for generating a properties {@link Dialog} for a given optimization method based on a json file.
 * 
 * @author Sascha Zur
 * @author Kathrin Schaffert (#17981, refactoring)
 */
public class MethodPropertiesDialogGenerator extends Dialog {

    private static final String TRUE = "true";

    private final MethodDescription methodDescription;

    private Map<Widget, String> widgetToKeyMap;

    protected MethodPropertiesDialogGenerator(Shell parentShell, MethodDescription methodDescription) {
        super(parentShell);
        this.methodDescription = methodDescription;
    }

    @Override
    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        shell.setText(Messages.algorithmProperties + " - " + methodDescription.getMethodName());
        InputStream path = getClass().getResourceAsStream("/resources/optimizer16.png");
        Image icon = new Image(null, new ImageData(path));
        shell.setImage(icon);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        widgetToKeyMap = new HashMap<>();

        Composite dialogContainer = (Composite) super.createDialogArea(parent);
        CTabFolder settingsTabFolder = new CTabFolder(dialogContainer, SWT.BORDER);
        if (methodDescription != null) {
            if (methodDescription.getCommonSettings() != null
                && checkIfSettingsAreGUIRelevant(methodDescription.getCommonSettings())) {
                CTabItem commonSettingsTab = new CTabItem(settingsTabFolder, SWT.NONE);
                commonSettingsTab.setText("Common Settings");
                Composite commonSettingsContainer = new Composite(settingsTabFolder, SWT.NONE);
                commonSettingsContainer.setLayout(new GridLayout(2, true));
                createSettingsTab(methodDescription.getCommonSettings(), commonSettingsContainer);
                commonSettingsTab.setControl(commonSettingsContainer);
            }

            if (methodDescription.getSpecificSettings() != null
                && checkIfSettingsAreGUIRelevant(methodDescription.getSpecificSettings())) {
                CTabItem specificSettingsTab = new CTabItem(settingsTabFolder, SWT.NONE);
                specificSettingsTab.setText("Algorithm Specific Settings");
                Composite specificSettingsContainer = new Composite(settingsTabFolder, SWT.NONE);
                specificSettingsContainer.setLayout(new GridLayout(2, true));
                createSettingsTab(methodDescription.getSpecificSettings(), specificSettingsContainer);
                specificSettingsTab.setControl(specificSettingsContainer);
            }
            if (methodDescription.getResponsesSettings() != null
                && checkIfSettingsAreGUIRelevant(methodDescription.getResponsesSettings())) {

                CTabItem responsesSettingsTab = new CTabItem(settingsTabFolder, SWT.NONE);
                responsesSettingsTab.setText("Responses Settings");
                Composite responsesSettingsContainer = new Composite(settingsTabFolder, SWT.NONE);
                responsesSettingsContainer.setLayout(new GridLayout(2, true));
                createSettingsTab(methodDescription.getResponsesSettings(), responsesSettingsContainer);
                responsesSettingsTab.setControl(responsesSettingsContainer);
            }
        }
        return dialogContainer;
    }

    private boolean checkIfSettingsAreGUIRelevant(Map<String, Map<String, String>> settings) {
        boolean returnValue = true;
        if (settings.isEmpty()) {
            returnValue = false;
        }

        for (String key : settings.keySet()) {
            if (settings.get(key).get(OptimizerComponentConstants.DONT_SHOW_KEY) != null) {
                returnValue = false;
            }
        }

        return returnValue;
    }

    private void createSettingsTab(Map<String, Map<String, String>> settings, Composite container) {
        if (settings == null) {
            return;
        }
        createWidgets(settings, container);
        createRestoreDefaultsButton(settings, container);
    }

    private void createWidgets(Map<String, Map<String, String>> settings, Composite container) {
        String[] sortedSettings = sortSettings(settings);
        for (String key : sortedSettings) {
            Map<String, String> currentSetting = settings.get(key);
            if (settings.get(key).get(OptimizerComponentConstants.DONT_SHOW_KEY) != null
                && settings.get(key).get(OptimizerComponentConstants.DONT_SHOW_KEY).equalsIgnoreCase(TRUE)) {
                continue;
            }
            String swtWidget = settings.get(key).get(OptimizerComponentConstants.SWTWIDGET_KEY);
            if (swtWidget.equals(OptimizerComponentConstants.WIDGET_TEXT)) {
                createTextField(container, key, currentSetting);
            } else if (swtWidget.equals(OptimizerComponentConstants.WIDGET_COMBO)) {
                createComboBox(container, key, currentSetting);
            } else if (swtWidget.equals(OptimizerComponentConstants.WIDGET_CHECK)) {
                createCheckBox(container, key, currentSetting);
            }
        }
    }

    private String[] sortSettings(Map<String, Map<String, String>> settings) {
        String[] sortedSettings = new String[settings.keySet().size()];
        int position = 0 - 1;
        for (Entry<String, Map<String, String>> entry : settings.entrySet()) {
            String orderNumber = entry.getValue().get(OptimizerComponentConstants.GUI_ORDER_KEY);
            if (orderNumber != null) {
                position = Integer.parseInt(orderNumber) - 1;
                if (position >= sortedSettings.length) {
                    while (position >= sortedSettings.length || sortedSettings[position] != null) {
                        position--;
                    }
                } else {
                    while (sortedSettings[position] != null) {
                        position++;
                    }
                }
            } else {
                position = sortedSettings.length - 1;
                while (sortedSettings[position] != null) {
                    position--;
                }
            }
            sortedSettings[position] = entry.getKey();
        }
        return sortedSettings;
    }

    private void createRestoreDefaultsButton(Map<String, Map<String, String>> settings, Composite container) {
        new Label(container, SWT.NONE).setText("");
        Label horizontalLine = new Label(container, SWT.SEPARATOR | SWT.HORIZONTAL);
        GridData lineGridData = new GridData(GridData.FILL_HORIZONTAL | SWT.END);
        horizontalLine.setLayoutData(lineGridData);
        new Label(container, SWT.NONE).setText("");
        Button loadDefaults = new Button(container, SWT.PUSH);
        loadDefaults.setImage(ImageManager.getInstance().getImageDescriptor(StandardImages.RESTORE_DEFAULT).createImage());
        GridData gridData = new GridData();
        gridData.horizontalAlignment = SWT.RIGHT;
        loadDefaults.setLayoutData(gridData);
        loadDefaults.setText(Messages.restoreDefaultAlgorithmProperties);
        loadDefaults.addSelectionListener(new DefaultSelectionListener(container, settings));
    }

    private String getValueOrDefault(Map<String, String> currentSetting) {
        String value = currentSetting.get(OptimizerComponentConstants.VALUE_KEY);
        if (value == null || value.equals("")) {
            value = currentSetting.get(OptimizerComponentConstants.DEFAULT_VALUE_KEY);
        }
        return value;
    }

    private void createCheckBox(Composite container, String key, Map<String, String> currentSetting) {
        Button newCheckbox = createLabelAndCheckbox(container, currentSetting.get(OptimizerComponentConstants.GUINAME_KEY),
            getValueOrDefault(currentSetting));
        widgetToKeyMap.put(newCheckbox, key);
        newCheckbox.setData(key);
        newCheckbox.addSelectionListener(new SelectionChangedListener());
    }

    private void createComboBox(Composite container, String key, Map<String, String> currentSetting) {
        Combo newCombo = createLabelAndCombo(container, currentSetting.get(OptimizerComponentConstants.GUINAME_KEY),
            currentSetting.get(OptimizerComponentConstants.CHOICES_KEY), getValueOrDefault(currentSetting));
        widgetToKeyMap.put(newCombo, key);
        newCombo.setData(key);
        newCombo.addModifyListener(new MethodPropertiesModifyListener());
    }

    private void createTextField(Composite container, String key, Map<String, String> currentSetting) {
        Text newTextfield =
            createLabelAndTextfield(container, currentSetting.get(OptimizerComponentConstants.GUINAME_KEY),
                currentSetting.get(OptimizerComponentConstants.DATA_TYPE_KEY), getValueOrDefault(currentSetting));
        newTextfield.setData(key);
        widgetToKeyMap.put(newTextfield, key);
        newTextfield.addModifyListener(new MethodPropertiesModifyListener());
    }

    /**
     * 
     * Implements the selection listener for the "load default" values button.
     *
     * @author Jascha Riedel
     */
    private class DefaultSelectionListener implements SelectionListener {

        private Composite container;

        private Map<String, Map<String, String>> settings;

        DefaultSelectionListener(Composite container, Map<String, Map<String, String>> settings) {
            this.container = container;
            this.settings = settings;
        }

        @Override
        public void widgetDefaultSelected(SelectionEvent arg0) {}

        @Override
        public void widgetSelected(SelectionEvent arg0) {
            for (Control field : container.getChildren()) {
                String key = getFieldSettingsKey(field);
                if (key == null) {
                    continue;
                }
                String value = settings.get(key).get(OptimizerComponentConstants.DEFAULT_VALUE_KEY);
                if (value == null) {
                    continue;
                }
                if (field instanceof Text) {
                    ((Text) field).setText(value);
                } else if (field instanceof Combo) {
                    ((Combo) field).setText(value);
                } else if (field instanceof Button && (value.equals(TRUE) || value.equals("false"))) {
                    ((Button) field).setSelection(Boolean.parseBoolean(value));
                    updatePropertiesSettings(field, value);
                }
            }
        }

        private String getFieldSettingsKey(Control field) {
            String key = null;
            if (field instanceof Text) {
                key = (String) ((Text) field).getData();
            } else if (field instanceof Combo) {
                key = (String) ((Combo) field).getData();
            } else if (field instanceof Button) {
                key = (String) ((Button) field).getData();
            }
            return key;
        }
    }

    private Button createLabelAndCheckbox(Composite container, String text, String value) {
        new Label(container, SWT.NONE).setText(text);
        Button result = new Button(container, SWT.CHECK);
        result.setSelection(value.equals(TRUE));
        return result;
    }

    private Combo createLabelAndCombo(Composite container, String text, String entries, String value) {
        new Label(container, SWT.NONE).setText(text);
        Combo result = new Combo(container, SWT.READ_ONLY);
        String[] entryData = entries.split(OptimizerComponentConstants.SEPARATOR);
        for (String entry : entryData) {
            result.add(entry);
        }
        result.select(result.indexOf(value));
        return result;
    }

    private Text createLabelAndTextfield(Composite container, String text, String dataType, String value) {
        new Label(container, SWT.NONE).setText(text);
        Text result = new Text(container, SWT.SINGLE | SWT.BORDER);
        result.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        result.setText(value);
        if (dataType.equals("Int")) {
            result.addVerifyListener(new NumericalTextConstraintListener(WidgetGroupFactory.ONLY_INTEGER));
        } else if (dataType.equals("Real")) {
            result.addVerifyListener(new NumericalTextConstraintListener(WidgetGroupFactory.ONLY_FLOAT));
        }
        return result;
    }

    /**
     * Validated all current inputs in the dialog.
     */
    public void validateInputs() {
        for (Entry<Widget, String> entry : widgetToKeyMap.entrySet()) {
            Map<String, String> settings = null;
            if (methodDescription.getCommonSettings() != null
                && methodDescription.getCommonSettings().containsKey(entry.getValue())) {
                settings = methodDescription.getCommonSettings().get(entry.getValue());
            } else if (methodDescription.getSpecificSettings() != null
                && methodDescription.getSpecificSettings().containsKey(entry.getValue())) {
                settings = methodDescription.getSpecificSettings().get(entry.getValue());
            } else if (methodDescription.getResponsesSettings() != null
                && methodDescription.getResponsesSettings().containsKey(entry.getValue())) {
                settings = methodDescription.getResponsesSettings().get(entry.getValue());
            }
            if (settings == null) {
                continue;
            }
            String swtWidget = settings.get(OptimizerComponentConstants.SWTWIDGET_KEY);
            if (!swtWidget.equals(OptimizerComponentConstants.WIDGET_TEXT)) {
                continue;
            }
            if (!validateTextField((Text) entry.getKey(), settings)) {
                getButton(IDialogConstants.OK_ID).setEnabled(false);
                return;
            }
        }
        getButton(IDialogConstants.OK_ID).setEnabled(true);
    }

    private boolean validateTextField(Text textField, Map<String, String> settings) {
        String dataType = settings.get(OptimizerComponentConstants.DATA_TYPE_KEY);
        String validation = settings.get(OptimizerComponentConstants.VALIDATION_KEY);
        if (validation == null || validation.equals("")) {
            return true;
        }
        if (textField.getText().equals("") && (validation.contains("required"))) {
            return false;
        } else if (!textField.getText().equals("")) {
            if (dataType.equalsIgnoreCase(OptimizerComponentConstants.TYPE_INT)) {
                try {
                    int value = Integer.parseInt(textField.getText());
                    return checkValidation(value, validation);
                } catch (NumberFormatException e) {
                    return false;
                }
            } else if (dataType.equalsIgnoreCase(OptimizerComponentConstants.TYPE_REAL)) {
                try {
                    double value = Double.parseDouble(textField.getText());
                    return checkValidation(value, validation);
                } catch (NumberFormatException e) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean checkValidation(double value, String validation) {
        String[] splitValidations = validation.split(OptimizerComponentConstants.SEPARATOR);
        for (String argument : splitValidations) {
            if (argument.contains("<=")) {
                double restriction = Double.parseDouble(argument.substring(2));
                if (value > restriction) {
                    return false;
                }
            } else if (argument.contains(">=")) {
                double restriction = Double.parseDouble(argument.substring(2));
                if (value < restriction) {
                    return false;
                }
            } else if (argument.contains("<")) {
                double restriction = Double.parseDouble(argument.substring(1));
                if (value >= restriction) {
                    return false;
                }
            } else if (argument.contains(">")) {
                double restriction = Double.parseDouble(argument.substring(1));
                if (value <= restriction) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean checkValidation(int value, String validation) {
        return checkValidation((double) value, validation);
    }

    /**
     * ModifyListener for changing the new values in the given MethodDescription.
     * 
     * @author Sascha Zur
     */
    private class MethodPropertiesModifyListener implements ModifyListener {

        @Override
        public void modifyText(ModifyEvent arg0) {
            Widget source = (Widget) arg0.getSource();
            if (source instanceof Text) {
                updatePropertiesSettings(source, ((Text) source).getText());
                validateInputs();
            } else if (source instanceof Combo) {
                updatePropertiesSettings(source, ((Combo) source).getText());
            }
        }

    }

    /**
     * Listener for changing checkbox values.
     * 
     * @author Sascha Zur
     */
    private class SelectionChangedListener extends SelectionAdapter {

        @Override
        public void widgetDefaultSelected(SelectionEvent e) {
            Button source = (Button) e.getSource();
            updatePropertiesSettings(source, "" + source.getSelection());
        }

        @Override
        public void widgetSelected(SelectionEvent e) {
            widgetDefaultSelected(e);
        }
    }

    private void updatePropertiesSettings(Widget source, String value) {
        if (methodDescription.getCommonSettings() != null
            && methodDescription.getCommonSettings().containsKey(widgetToKeyMap.get(source))) {
            methodDescription.getCommonSettings().get(widgetToKeyMap.get(source)).put(OptimizerComponentConstants.VALUE_KEY,
                value);
        } else if (methodDescription.getSpecificSettings() != null
            && methodDescription.getSpecificSettings().containsKey(widgetToKeyMap.get(source))) {
            methodDescription.getSpecificSettings().get(widgetToKeyMap.get(source)).put(OptimizerComponentConstants.VALUE_KEY,
                value);
        } else if (methodDescription.getResponsesSettings() != null
            && methodDescription.getResponsesSettings().containsKey(widgetToKeyMap.get(source))) {
            methodDescription.getResponsesSettings().get(widgetToKeyMap.get(source)).put(OptimizerComponentConstants.VALUE_KEY,
                value);
        }
    }

}
