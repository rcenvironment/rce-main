/*
 * Copyright 2006-2023 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.gui.workflow.integration;

import org.apache.commons.logging.LogFactory;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import de.rcenvironment.core.gui.integration.toolintegration.ShowIntegrationEditWizardHandler;

/**
 * Action that allows the user to edit the integration of a tool directly from its context menu in the workflow editor.
 * 
 * @author Alexander Weinert
 */
public class EditToolIntegrationAction extends EditIntegrationAction {

    @Override
    public void run() {
        try {
            new ShowIntegrationEditWizardHandler(workflowNode.getComponentDescription().getName()).execute(new ExecutionEvent());
        } catch (ExecutionException e) {
            LogFactory.getLog(getClass()).error("Opening Tool Edit wizard failed", e);
        }
    }

}
