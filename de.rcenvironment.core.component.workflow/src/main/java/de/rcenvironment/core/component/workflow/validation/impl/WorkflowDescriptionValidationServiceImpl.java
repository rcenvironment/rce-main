/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.component.workflow.validation.impl;

import java.util.HashMap;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import de.rcenvironment.core.communication.api.PlatformService;
import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.communication.management.WorkflowHostService;
import de.rcenvironment.core.component.api.ComponentUtils;
import de.rcenvironment.core.component.api.DistributedComponentKnowledge;
import de.rcenvironment.core.component.api.DistributedComponentKnowledgeService;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowDescriptionValidationResult;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.component.workflow.validation.api.WorkflowDescriptionValidationService;

/**
 * @author Alexander Weinert
 */
@Component
public class WorkflowDescriptionValidationServiceImpl implements WorkflowDescriptionValidationService {

    private PlatformService platformService;

    private WorkflowHostService workflowHostService;

    private DistributedComponentKnowledgeService componentKnowledgeService;

    @Override
    public WorkflowDescriptionValidationResult validateAvailabilityOfNodesAndComponentsFromLocalKnowledge(
        WorkflowDescription workflowDescription) {
        LogicalNodeId missingControllerNodeId = null;
        Map<String, LogicalNodeId> missingComponentsNodeIds = new HashMap<>();

        LogicalNodeId controllerNode = workflowDescription.getControllerNode();
        if (controllerNode == null) {
            controllerNode = platformService.getLocalDefaultLogicalNodeId();
        }
        if (!workflowHostService.getLogicalWorkflowHostNodesAndSelf().contains(controllerNode)) {
            missingControllerNodeId = controllerNode;
        }

        DistributedComponentKnowledge compKnowledge = componentKnowledgeService.getCurrentSnapshot();

        for (WorkflowNode node : workflowDescription.getWorkflowNodes()) {
            LogicalNodeId componentNode = node.getComponentDescription().getNode();
            if (componentNode == null) {
                componentNode = platformService.getLocalDefaultLogicalNodeId();
            }
            if (!ComponentUtils.hasComponent(compKnowledge.getAllInstallations(), node.getComponentDescription().getIdentifier(),
                componentNode)) {
                missingComponentsNodeIds.put(node.getName(), componentNode);
            }
        }
        if (missingControllerNodeId == null && missingComponentsNodeIds.isEmpty()) {
            return WorkflowDescriptionValidationResult.createResultForSuccess();
        } else {
            return WorkflowDescriptionValidationResult.createResultForFailure(missingControllerNodeId, missingComponentsNodeIds);
        }
    }

    @Reference
    protected void bindPlatformService(PlatformService newService) {
        this.platformService = newService;
    }

    @Reference
    protected void bindWorkflowHostService(WorkflowHostService newService) {
        this.workflowHostService = newService;
    }

    @Reference
    protected void bindComponentKnowledgeService(DistributedComponentKnowledgeService newService) {
        this.componentKnowledgeService = newService;
    }

}
