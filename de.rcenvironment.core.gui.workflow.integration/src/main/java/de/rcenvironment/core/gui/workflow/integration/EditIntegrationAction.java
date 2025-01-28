/*
 * Copyright 2006-2025 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.workflow.integration;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import de.rcenvironment.core.component.api.DistributedComponentKnowledgeService;
import de.rcenvironment.core.component.management.api.DistributedComponentEntry;
import de.rcenvironment.core.gui.workflow.editor.WorkflowEditorAction;
import de.rcenvironment.core.utils.incubator.ServiceRegistry;

/**
 * Abstract {@link WorkflowEditorAction} to edit integrated components.
 * 
 * @author Kathrin Schaffert
 */
public abstract class EditIntegrationAction extends WorkflowEditorAction {

    @Override
    public boolean isEnabled() {
        if (!super.isEnabled()) {
            return false;
        }

        DistributedComponentKnowledgeService distributedComponentKnowledgeService = ServiceRegistry.createPublisherAccessFor(this)
            .getService(DistributedComponentKnowledgeService.class);

        Collection<DistributedComponentEntry> currentToolInstallations =
            distributedComponentKnowledgeService.getCurrentSnapshot().getAllLocalInstallations();

        String componentName = workflowNode.getComponentDescription().getName();

        List<DistributedComponentEntry> list =
            currentToolInstallations.stream().filter(entry -> !entry.getComponentInstallation().isMappedComponent())
                .filter(entry -> entry.getDisplayName().equals(componentName))
                .collect(Collectors.toList());

        return !list.isEmpty();
    }

}
