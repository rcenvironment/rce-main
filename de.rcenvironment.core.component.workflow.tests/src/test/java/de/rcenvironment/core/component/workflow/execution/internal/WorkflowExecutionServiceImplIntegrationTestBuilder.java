/*
 * Copyright 2023 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.execution.internal;

import java.util.HashMap;
import java.util.Map;

import org.easymock.EasyMock;

import de.rcenvironment.core.communication.common.CommunicationException;
import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.component.workflow.api.WorkflowConstants;
import de.rcenvironment.core.component.workflow.execution.api.RemotableWorkflowExecutionControllerService;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionContext;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionInformation;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowState;
import de.rcenvironment.core.component.workflow.execution.impl.WorkflowExecutionInformationImpl;
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
        EasyMock.replay(notificationService, authorizationTokenService, platformService, communicationService, metaDataService);
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

    public WorkflowExecutionServiceImplIntegrationTestBuilder bindWorkflowExecutionControllerServiceToVailidateControllerVisibility(
        LogicalNodeId targetNodeId, boolean isComponentVisible) {
        this.controllerService = getControllerServiceWithComponentVisibility(targetNodeId, isComponentVisible);
        service.bindWorkflowExecutionControllerService(this.controllerService);
        return this;
    }

    public WorkflowExecutionServiceImplIntegrationTestBuilder bindWorkflowExecutionControllerServiceWithWfExecutionInformations(
        LogicalNodeId targetNodeId, WorkflowExecutionContext context) {
        this.controllerService = getControllerServiceGetWorkflowExecutionInformationsIsCalled(targetNodeId, context);
        service.bindWorkflowExecutionControllerService(this.controllerService);
        return this;
    }

    public WorkflowExecutionServiceImplIntegrationTestBuilder bindWorkflowExecutionControllerServiceSendHeartbeat(
        LogicalNodeId localNodeId, WorkflowExecutionContext context) {

        this.controllerService = getControllerServiceGetWorkflowExecutionInformationsIsCalled(localNodeId, context);

        service.bindWorkflowExecutionControllerService(this.controllerService);
        return this;
    }

    public WorkflowExecutionServiceImplIntegrationTestBuilder bindWorkflowExecutionControllerServiceGetWorkflowDataManagementIdIsCalled(
        LogicalNodeId targetNodeId) {
        this.controllerService = getControllerServiceGetWorkflowDataManagementIdIsCalled(targetNodeId);
        service.bindWorkflowExecutionControllerService(this.controllerService);
        return this;
    }

    public WorkflowExecutionServiceImplIntegrationTestBuilder expectControllerServiceCreation(LogicalNodeId targetNode, String identifier) {
        EasyMock
            .expect(communicationService.getRemotableService(RemotableWorkflowExecutionControllerService.class, targetNode))
            .andStubReturn(controllerService);

        return this;
    }

    public WorkflowExecutionServiceImplIntegrationTestBuilder expectNotificationServiceSendNotification(String wfExecInfoIdentifier) {
        notificationService.send(WorkflowConstants.STATE_NOTIFICATION_ID + wfExecInfoIdentifier, WorkflowState.IS_ALIVE.name());
        EasyMock.expectLastCall();
        return this;
    }

    public WorkflowExecutionServiceImplIntegrationTestBuilder expectMetaDataServiceDeleteWorkflowRun(Long id, LogicalNodeId nodeId) {
        try {
            EasyMock.expect(metaDataService.deleteWorkflowRun(id, nodeId)).andStubReturn(true);
        } catch (CommunicationException e) {
            // should never happen since this is called in a mock
        }
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

    protected RemotableWorkflowExecutionControllerService getControllerServiceWithComponentVisibility(
        LogicalNodeId targetNodeId, boolean isComponentVisible) {
        WorkflowExecutionControllerServiceImplTestBuilder builder = new WorkflowExecutionControllerServiceImplTestBuilder();
        WorkflowExecutionControllerServiceImpl workflowExecutionControllerService =
            builder
                .expectComponentKnowledgeServiceGetCurrentSnapshotIsCalled()
                .expectCommunicationServiceGetReachableLogicalNodesIsCalled(targetNodeId)
                .build(targetNodeId, isComponentVisible);
        return this.controllerServices.computeIfAbsent(targetNodeId, ignored -> workflowExecutionControllerService);
    }

    protected RemotableWorkflowExecutionControllerService getControllerServiceGetWorkflowExecutionInformationsIsCalled(
        LogicalNodeId targetNodeId, WorkflowExecutionContext context) {
        WorkflowExecutionControllerServiceImplTestBuilder builder = new WorkflowExecutionControllerServiceImplTestBuilder();
        WorkflowExecutionControllerServiceImpl workflowExecutionControllerService = builder
            .expectExecCtrlUtilsServiceGetExecutionControllerIsCalled(WorkflowExecutionServiceImplTestHelper.executionIdentifier())
            .expectExecutionControllerGetStateIsCalled()
            .expectExecutionControllerGetDataManagmentIdIsCalled()
            .build();

        Map<String, WorkflowExecutionInformation> map = new HashMap<String, WorkflowExecutionInformation>();
        WorkflowExecutionInformationImpl info = new WorkflowExecutionInformationImpl(context);
        info.setWorkflowExecutionIdentifier(WorkflowExecutionServiceImplTestHelper.WF_EXEC_INFO_IDENTIFIER);
        info.setIdentifier(WorkflowExecutionServiceImplTestHelper.executionIdentifier());
        map.put(WorkflowExecutionServiceImplTestHelper.executionIdentifier(), info);
        workflowExecutionControllerService.setWorkflowExecutionInformations(map);

        return this.controllerServices.computeIfAbsent(targetNodeId, ignored -> workflowExecutionControllerService);
    }

    protected RemotableWorkflowExecutionControllerService getControllerServiceGetWorkflowDataManagementIdIsCalled(
        LogicalNodeId targetNodeId) {
        WorkflowExecutionControllerServiceImplTestBuilder builder = new WorkflowExecutionControllerServiceImplTestBuilder();
        WorkflowExecutionControllerServiceImpl workflowExecutionControllerService = builder
            .expectExecCtrlUtilsServiceGetExecutionControllerIsCalled(WorkflowExecutionServiceImplTestHelper.executionIdentifier())
            .expectExecutionControllerGetDataManagmentIdIsCalled()
            .build();

        return this.controllerServices.computeIfAbsent(targetNodeId, ignored -> workflowExecutionControllerService);
    }

}
