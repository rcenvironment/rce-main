/*
 * Copyright 2023 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.execution.internal;

import org.easymock.EasyMock;

import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.component.workflow.execution.api.RemotableWorkflowExecutionControllerService;
import de.rcenvironment.core.component.workflow.execution.internal.WorkflowExecutionControllerServiceImplTestBuilder.WorkflowExecutionControllerServiceImplMock;

/**
 * Integration Test Builder for {@link WorkflowExecutionServiceImpl}.
 * 
 * @author Kathrin Schaffert
 */
class WorkflowExecutionServiceImplIntegrationTestBuilder extends WorkflowExecutionServiceImplTestBuilder {

    public WorkflowExecutionServiceImpl build() {

        service.bindWorkflowExecutionControllerService(getControllerService());
        super.build();

        return service;
    }

    @Override
    protected void replayAllServices() {
        EasyMock.replay(notificationService, authorizationTokenService, platformService, communicationService);
    }

    public void verifyAllDependencies() {
        EasyMock.verify(notificationService, authorizationTokenService, platformService, communicationService);
        for (RemotableWorkflowExecutionControllerService service : controllerServices.values()) {
            ((WorkflowExecutionControllerServiceImplMock) service).verifyAllDependencies();
        }
    }

    public WorkflowExecutionServiceImplTestBuilder expectControllerServiceCreation(LogicalNodeId targetNode, String identifier) {
        final RemotableWorkflowExecutionControllerService controllerService = getControllerService(targetNode, identifier);

        EasyMock
            .expect(communicationService.getRemotableService(RemotableWorkflowExecutionControllerService.class, targetNode))
            .andStubReturn(controllerService);

        return this;
    }

    private RemotableWorkflowExecutionControllerService getControllerService() {
        WorkflowExecutionControllerServiceImplTestBuilder builder = new WorkflowExecutionControllerServiceImplTestBuilder();
        return builder.build();
    }

    protected RemotableWorkflowExecutionControllerService getControllerService(LogicalNodeId localNodeId, String identifier) {
        
        WorkflowExecutionControllerServiceImplTestBuilder builder = new WorkflowExecutionControllerServiceImplTestBuilder();
        WorkflowExecutionControllerServiceImpl workflowExecutionControllerService =
            builder
                .expectWorkflowHostServiceGetLogicalWorkflowHostNodesIsCalled(localNodeId)
                .expectExecCtrlUtilsServiceGetExecutionControllerIsCalled(identifier)
                .expectNotificationServiceSubscription()
                .build();
        return this.controllerServices.computeIfAbsent(localNodeId, ignored -> workflowExecutionControllerService);
    }
}
