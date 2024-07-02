/*
 * Copyright 2006-2023 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.xpathchooser;

import java.util.Map;

import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Widget;

import de.rcenvironment.core.component.model.endpoint.api.EndpointMetaDataDefinition;
import de.rcenvironment.core.component.workflow.model.spi.ComponentInstanceProperties;
import de.rcenvironment.core.datamodel.api.EndpointActionType;
import de.rcenvironment.core.datamodel.api.EndpointType;
import de.rcenvironment.core.gui.workflow.editor.properties.EndpointEditDialog;

/**
 * A dialog for defining and editing xpath as additional endpoints.
 * 
 * @author Markus Kunde
 * @author Adrian Stock
 * @author Jan Flink
 * @author Tim Rosenbach
 */
public class XPathEditDialog extends EndpointEditDialog {

    private static final int TEXT_HEIGHT = 60;

    private static final String CHANNEL_XPATH = "variable.xpath";

    private static final int DIALOG_WIDTH = 345;

    private static final int DIALOG_HEIGHT = 435;

    public XPathEditDialog(Shell parentShell, EndpointActionType actionType, ComponentInstanceProperties configuration,
        EndpointType direction, String id, boolean isStatic, Image icon,
        EndpointMetaDataDefinition metaData, Map<String, String> metadataValues) {
        super(parentShell, actionType, configuration, direction, id, isStatic, metaData, metadataValues);
    }

    @Override
    protected Point getInitialSize() {
        return new Point(DIALOG_WIDTH, DIALOG_HEIGHT);
    }

    @Override
    protected Control createConfigurationArea(Composite parent) {
        Control superControl = super.createConfigurationArea(parent);
        createXPathChoosingButton(parent);
        return superControl;
    }

    private void createXPathChoosingButton(Composite parent) {
        Composite container = new Composite(parent, SWT.NONE);
        GridData gd = new GridData(GridData.FILL, GridData.FILL, true, false);
        container.setLayout(new GridLayout(1, false));
        container.setLayoutData(gd);
        Button selectButton = new Button(container, SWT.NONE);
        selectButton.setText(Messages.selectButton);
        GridData g = new GridData();
        g.horizontalAlignment = GridData.END;
        g.grabExcessHorizontalSpace = true;
        selectButton.setLayoutData(g);
        selectButton.addListener(SWT.Selection, event -> createXPathDialog());
    }

    private void createXPathDialog() {
        XPathChooserDialog dialog = new XPathChooserDialog(super.getShell(), null);
        if (dialog.getChooser() != null && dialog.open() == Window.OK) {
            final VariableEntry newVar = dialog.getSelectedVariable();
            String xpath = newVar.getXpath();
            if (xpath != null && !xpath.isEmpty()) {
                Widget address = super.getWidget(CHANNEL_XPATH);
                if (address instanceof Text) {
                    ((Text) address).setText(xpath);
                }
            }
        }
    }

    @Override
    protected Text createLabelAndTextfield(Composite container, String text, String dataType, String value) {
        new Label(container, SWT.NONE).setText(text);
        Text result = new Text(container, SWT.MULTI | SWT.WRAP | SWT.BORDER | SWT.V_SCROLL);
        GridData layoutData = new GridData(SWT.FILL, SWT.FILL, true, false);
        layoutData.heightHint = TEXT_HEIGHT;
        result.setLayoutData(layoutData);
        result.setText(value);
        addVerifyListener(dataType, value, result);
        return result;
    }

}
