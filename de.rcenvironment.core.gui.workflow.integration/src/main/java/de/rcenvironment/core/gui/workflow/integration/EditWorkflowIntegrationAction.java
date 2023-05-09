/*
 * Copyright 2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.gui.workflow.integration;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.logging.LogFactory;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import de.rcenvironment.core.communication.api.PlatformService;
import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.component.api.ComponentUtils;
import de.rcenvironment.core.component.api.DistributedComponentKnowledgeService;
import de.rcenvironment.core.component.management.api.DistributedComponentEntry;
import de.rcenvironment.core.gui.integration.workflowintegration.handlers.EditWorkflowIntegrationHandler;
import de.rcenvironment.core.gui.workflow.editor.WorkflowEditorAction;
import de.rcenvironment.core.utils.incubator.ServiceRegistry;

/**
 * Action that allows the user to edit the integration of a workflow directly from its context menu in the workflow editor.
 * 
 * @author Kathrin Schaffert
 */
public class EditWorkflowIntegrationAction extends WorkflowEditorAction {

    private LogicalNodeId localNode;

    private Set<DistributedComponentEntry> currentToolInstallations;

    public EditWorkflowIntegrationAction() {

        DistributedComponentKnowledgeService distributedComponentKnowledgeService = ServiceRegistry.createPublisherAccessFor(this)
            .getService(DistributedComponentKnowledgeService.class);

        localNode = ServiceRegistry.createAccessFor(this).getService(PlatformService.class).getLocalDefaultLogicalNodeId();

        this.currentToolInstallations = getCurrentToolInstallations(distributedComponentKnowledgeService);

    }

    @Override
    public void run() {
        try {
            new EditWorkflowIntegrationHandler(workflowNode.getComponentDescription().getName()).execute(new ExecutionEvent());
        } catch (ExecutionException e) {
            LogFactory.getLog(getClass()).error("Opening Workflow Integration Editor failed", e);
        }
    }

    @Override
    public boolean isEnabled() {
        if (!super.isEnabled()) {
            return false;
        }

        String id = workflowNode.getComponentDescription().getComponentInstallation().getInstallationId();

        List<DistributedComponentEntry> list =
            currentToolInstallations.stream().filter(entry -> entry.getComponentInstallation().getInstallationId().equals(id))
                .collect(Collectors.toList());

        if (!list.isEmpty()) {
            return list.get(0).getType().isLocal() && !list.get(0).getComponentInstallation().isMappedComponent();
        }

        return false;
    }

    // copied from PaletteViewContentProvider >> TODO reuse existing code
    protected Set<DistributedComponentEntry> getCurrentToolInstallations(
        DistributedComponentKnowledgeService distributedComponentKnowledgeService) {
        return new HashSet<>(ComponentUtils.eliminateComponentInterfaceDuplicates(
            distributedComponentKnowledgeService.getCurrentSnapshot().getLocalAccessInstallations(), localNode));
    }

}
