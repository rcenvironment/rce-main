/*
 * Copyright 2023 DLR, Germany
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

import de.rcenvironment.core.communication.api.PlatformService;
import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.component.api.ComponentUtils;
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

    private Set<DistributedComponentEntry> currentToolInstallations;

    protected EditIntegrationAction() {

        DistributedComponentKnowledgeService distributedComponentKnowledgeService = ServiceRegistry.createPublisherAccessFor(this)
            .getService(DistributedComponentKnowledgeService.class);

        LogicalNodeId localNode = ServiceRegistry.createAccessFor(this).getService(PlatformService.class).getLocalDefaultLogicalNodeId();

        this.currentToolInstallations = getCurrentToolInstallations(distributedComponentKnowledgeService, localNode);

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

    protected Set<DistributedComponentEntry> getCurrentToolInstallations(
        DistributedComponentKnowledgeService distributedComponentKnowledgeService, LogicalNodeId localNode) {
        return new HashSet<>(ComponentUtils.eliminateComponentInterfaceDuplicates(
            distributedComponentKnowledgeService.getCurrentSnapshot().getLocalAccessInstallations(), localNode));
    }

}
