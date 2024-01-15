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

    private RemotableWorkflowExecutionControllerService controllerService;

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

    public WorkflowExecutionServiceImplIntegrationTestBuilder bindWorkflowExecutionControllerService(LogicalNodeId targetNodeId) {
        this.controllerService = getControllerService(targetNodeId);
        service.bindWorkflowExecutionControllerService(this.controllerService);
        return this;
    }

    public WorkflowExecutionServiceImplIntegrationTestBuilder bindWorkflowExecutionControllerServiceMockExpectingAllWorklfowStates(
        LogicalNodeId targetNodeId) {
        this.controllerService = getControllerServiceExpectingAllWorklfowStates(targetNodeId);
        service.bindWorkflowExecutionControllerService(this.controllerService);
        return this;
    }

    public WorkflowExecutionServiceImplIntegrationTestBuilder bindWorkflowExecutionControllerServiceThrowingExceptionWhenStartOnController(
        LogicalNodeId targetNodeId, String identifier) {
        this.controllerService = getControllerServiceThrowsException(targetNodeId);
        service.bindWorkflowExecutionControllerService(this.controllerService);
        return this;
    }
    
    public WorkflowExecutionServiceImplIntegrationTestBuilder bindWorkflowExecutionControllerServiceRefusingRequest(
        LogicalNodeId targetNodeId) {
        this.controllerService = getControllerServiceRefusingRequest(targetNodeId);
        service.bindWorkflowExecutionControllerService(this.controllerService);
        return this;
    }

    public WorkflowExecutionServiceImplIntegrationTestBuilder expectControllerServiceCreation(LogicalNodeId targetNode, String identifier) {
        EasyMock
            .expect(communicationService.getRemotableService(RemotableWorkflowExecutionControllerService.class, targetNode))
            .andStubReturn(controllerService);

        return this;
    }

    protected RemotableWorkflowExecutionControllerService getControllerService(LogicalNodeId targetNodeId) {

        WorkflowExecutionControllerServiceImplTestBuilder builder = new WorkflowExecutionControllerServiceImplTestBuilder();
        WorkflowExecutionControllerServiceImpl workflowExecutionControllerService =
            builder
                .expectWorkflowHostServiceGetLogicalWorkflowHostNodesIsCalled(targetNodeId)
                .expectExecCtrlUtilsServiceGetExecutionControllerIsCalled(WorkflowExecutionServiceImplTestHelper.executionIdentifier())
                .expectNotificationServiceSubscription()
                .expectStartOnController()
                .build();
        return this.controllerServices.computeIfAbsent(targetNodeId, ignored -> workflowExecutionControllerService);
    }

    protected RemotableWorkflowExecutionControllerService getControllerServiceExpectingAllWorklfowStates(LogicalNodeId targetNodeId) {

        WorkflowExecutionControllerServiceImplTestBuilder builder = new WorkflowExecutionControllerServiceImplTestBuilder();
        WorkflowExecutionControllerServiceImpl workflowExecutionControllerService =
            builder
                .expectWorkflowHostServiceGetLogicalWorkflowHostNodesIsCalled(targetNodeId)
                .expectExecCtrlUtilsServiceGetExecutionControllerIsCalled(WorkflowExecutionServiceImplTestHelper.executionIdentifier())
                .expectNotificationServiceSubscription()
                .expectStartOnController()
                .expectPauseOnController()
                .expectResumeOnController()
                .expectCancelOnController()
                .expectDisposeOnController()
                .build();
        return this.controllerServices.computeIfAbsent(targetNodeId, ignored -> workflowExecutionControllerService);
    }

    protected RemotableWorkflowExecutionControllerService getControllerServiceThrowsException(LogicalNodeId targetNodeId) {
        WorkflowExecutionControllerServiceImplTestBuilder builder = new WorkflowExecutionControllerServiceImplTestBuilder();
        WorkflowExecutionControllerServiceImpl workflowExecutionControllerService =
            builder
                .expectWorkflowHostServiceGetLogicalWorkflowHostNodesIsCalled(targetNodeId)
                .expectExecCtrlUtilsServiceGetExecutionControllerThrowsException(
                    WorkflowExecutionServiceImplTestHelper.executionIdentifier())
                .build();
        return this.controllerServices.computeIfAbsent(targetNodeId, ignored -> workflowExecutionControllerService);
    }

    protected RemotableWorkflowExecutionControllerService getControllerServiceRefusingRequest(LogicalNodeId targetNodeId) {
        WorkflowExecutionControllerServiceImplTestBuilder builder = new WorkflowExecutionControllerServiceImplTestBuilder();
        WorkflowExecutionControllerServiceImpl workflowExecutionControllerService =
            builder
                .expectWorkflowHostUnknown()
                .build();
        return this.controllerServices.computeIfAbsent(targetNodeId, ignored -> workflowExecutionControllerService);
    }

}
