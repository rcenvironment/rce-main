/*
 * Copyright 2023 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.execution.internal;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.easymock.EasyMock;

import de.rcenvironment.core.communication.api.CommunicationService;
import de.rcenvironment.core.communication.common.IdentifierException;
import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.communication.management.WorkflowHostService;
import de.rcenvironment.core.component.api.DistributedComponentKnowledge;
import de.rcenvironment.core.component.api.DistributedComponentKnowledgeService;
import de.rcenvironment.core.component.execution.api.ExecutionControllerException;
import de.rcenvironment.core.component.execution.api.LocalExecutionControllerUtilsService;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionContext;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionController;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowState;
import de.rcenvironment.core.notification.NotificationService;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;

/**
 * Test Service Builder for {@link WorkflowExecutionControllerServiceImpl}.
 * 
 * @author Kathrin Schaffert
 */
class WorkflowExecutionControllerServiceImplTestBuilder {

    class WorkflowExecutionControllerServiceImplMock extends WorkflowExecutionControllerServiceImpl {

        private LogicalNodeId targetNodeId;

        private boolean isComponentVisible;

        @Override
        WorkflowExecutionControllerImpl createWorkflowExecutionController(WorkflowExecutionContext wfExeCtx) {
            return EasyMock.createMock(WorkflowExecutionControllerImpl.class);
        }

        @Override
        void registerExecutionController(WorkflowExecutionController workflowController, final String executionId) {
            // no implementation for testing purposes
        }

        @Override
        LogicalNodeId parseLogicalNodeIdString(String[] refParts) throws IdentifierException {
            return targetNodeId;
        }

        @Override
        boolean isComponentVisible(DistributedComponentKnowledge compKnowledge, String componentIdAndVersion,
            LogicalNodeId logicalNodeId) {
            return isComponentVisible;
        }

        public void verifyAllDependencies() {
            EasyMock.verify(workflowHostService, exeCtrlUtilsService, notificationService, communicationService,
                distributedComponentKnowledgeService, executionController);
        }

        public void setTargetNodeId(LogicalNodeId targetNodeId) {
            this.targetNodeId = targetNodeId;
        }

        public void setComponentVisible(boolean isComponentVisible) {
            this.isComponentVisible = isComponentVisible;
        }

    }

    private WorkflowExecutionControllerServiceImplMock service = new WorkflowExecutionControllerServiceImplMock();

    private final WorkflowHostService workflowHostService = EasyMock.createMock(WorkflowHostService.class);

    private final LocalExecutionControllerUtilsService exeCtrlUtilsService =
        EasyMock.createMock(LocalExecutionControllerUtilsService.class);

    private final NotificationService notificationService = EasyMock.createMock(NotificationService.class);

    private final CommunicationService communicationService = EasyMock.createMock(CommunicationService.class);

    private final DistributedComponentKnowledgeService distributedComponentKnowledgeService =
        EasyMock.createMock(DistributedComponentKnowledgeService.class);

    private WorkflowExecutionControllerImpl executionController = EasyMock.createMock(WorkflowExecutionControllerImpl.class);

    public WorkflowExecutionControllerServiceImpl build() {
        service.bindWorkflowHostService(workflowHostService);
        service.bindLocalExecutionControllerUtilsService(exeCtrlUtilsService);
        service.bindNotificationService(notificationService);
        service.bindDistributedComponentKnowledgeService(distributedComponentKnowledgeService);
        service.setCommunicationService(communicationService);

        replayAllServices();

        return service;
    }

    public WorkflowExecutionControllerServiceImpl build(LogicalNodeId targetNodeId, boolean isComponentVisible) {
        service.setTargetNodeId(targetNodeId);
        service.setComponentVisible(isComponentVisible);
        return build();
    }

    private void replayAllServices() {
        EasyMock.replay(workflowHostService, exeCtrlUtilsService, notificationService, communicationService,
            distributedComponentKnowledgeService, executionController);
    }

    public WorkflowExecutionControllerServiceImplTestBuilder expectWorkflowHostServiceGetLogicalWorkflowHostNodesIsCalled(
        LogicalNodeId localNodeId) {
        EasyMock.expect(workflowHostService.getLogicalWorkflowHostNodes()).andStubReturn(new HashSet<>(Arrays.asList(localNodeId)));
        return this;
    }

    public WorkflowExecutionControllerServiceImplTestBuilder expectWorkflowHostUnknown() {
        EasyMock.expect(workflowHostService.getLogicalWorkflowHostNodes()).andStubReturn(new HashSet<>());
        return this;
    }

    public WorkflowExecutionControllerServiceImplTestBuilder expectExecCtrlUtilsServiceGetExecutionControllerIsCalled(String identifier) {

        try {
            EasyMock
                .expect(exeCtrlUtilsService.getExecutionController(WorkflowExecutionController.class, identifier,
                    null))
                .andStubReturn(executionController);
        } catch (ExecutionControllerException e) {
            // Will never happen, since we call this method only on a mock
        }
        return this;
    }

    public WorkflowExecutionControllerServiceImplTestBuilder expectNotificationServiceSubscription() {

        try {
            EasyMock.expect(notificationService.subscribe(EasyMock.anyString(), EasyMock.anyObject()))
                .andStubReturn(new HashMap<String, Long>());
        } catch (RemoteOperationException e) {
            // Will never happen, since we call this method only on a mock
        }
        return this;
    }

    public WorkflowExecutionControllerServiceImplTestBuilder expectStartOnController() {
        executionController.start();
        EasyMock.expectLastCall();
        return this;
    }

    public WorkflowExecutionControllerServiceImplTestBuilder expectPauseOnController() {
        executionController.pause();
        EasyMock.expectLastCall();
        return this;
    }

    public WorkflowExecutionControllerServiceImplTestBuilder expectResumeOnController() {
        executionController.resume();
        EasyMock.expectLastCall();
        return this;
    }

    public WorkflowExecutionControllerServiceImplTestBuilder expectCancelOnController() {
        executionController.cancel();
        EasyMock.expectLastCall();
        return this;
    }

    public WorkflowExecutionControllerServiceImplTestBuilder expectDisposeOnController() {
        executionController.dispose();
        EasyMock.expectLastCall();
        return this;
    }

    public WorkflowExecutionControllerServiceImplTestBuilder expectExecutionControllerGetStateIsCalled() {
        EasyMock.expect(executionController.getState()).andStubReturn(WorkflowState.RUNNING);
        return this;
    }

    public WorkflowExecutionControllerServiceImplTestBuilder expectExecutionControllerGetDataManagmentIdIsCalled() {
        final long id = 2345;
        EasyMock.expect(executionController.getDataManagementId()).andStubReturn(id);
        return this;
    }

    public WorkflowExecutionControllerServiceImplTestBuilder expectExecCtrlUtilsServiceGetExecutionControllerThrowsException(
        String identifier) {

        try {
            EasyMock
                .expect(exeCtrlUtilsService.getExecutionController(WorkflowExecutionController.class, identifier,
                    null))
                .andThrow(new ExecutionControllerException("Test Error"));
        } catch (ExecutionControllerException e) {
            // Will never happen, since we call this method only on a mock
        }
        return this;
    }

    public WorkflowExecutionControllerServiceImplTestBuilder expectComponentKnowledgeServiceGetCurrentSnapshotIsCalled() {
        EasyMock.expect(distributedComponentKnowledgeService.getCurrentSnapshot())
            .andStubReturn(null);
        return this;
    }

    public WorkflowExecutionControllerServiceImplTestBuilder expectCommunicationServiceGetReachableLogicalNodesIsCalled(
        LogicalNodeId targetNodeId) {

        Set<LogicalNodeId> nodesSet = new HashSet<LogicalNodeId>();
        nodesSet.add(targetNodeId);

        EasyMock.expect(communicationService.getReachableLogicalNodes())
            .andStubReturn(nodesSet);
        return this;
    }

}
