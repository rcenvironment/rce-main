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

import org.easymock.EasyMock;

import de.rcenvironment.core.communication.api.CommunicationService;
import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.communication.management.WorkflowHostService;
import de.rcenvironment.core.component.api.DistributedComponentKnowledgeService;
import de.rcenvironment.core.component.execution.api.ExecutionControllerException;
import de.rcenvironment.core.component.execution.api.LocalExecutionControllerUtilsService;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionContext;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionController;
import de.rcenvironment.core.notification.NotificationService;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;

/**
 * Test Service Builder for {@link WorkflowExecutionControllerServiceImpl}.
 * 
 * @author Kathrin Schaffert
 */
class WorkflowExecutionControllerServiceImplTestBuilder {

    class WorkflowExecutionControllerServiceImplMock extends WorkflowExecutionControllerServiceImpl {

        @Override
        WorkflowExecutionControllerImpl createWorkflowExecutionController(WorkflowExecutionContext wfExeCtx) {
            return EasyMock.createMock(WorkflowExecutionControllerImpl.class);
        }

        @Override
        void registerExecutionController(WorkflowExecutionController workflowController, final String executionId) {
            // no implementation for testing purposes
        }

        public void verifyAllDependencies() {
            EasyMock.verify(workflowHostService, exeCtrlUtilsService, notificationService, communicationService,
                distributedComponentKnowledgeService, executionController);
        }

    }

    private WorkflowExecutionControllerServiceImpl service = new WorkflowExecutionControllerServiceImplMock();

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

    private void replayAllServices() {
        EasyMock.replay(workflowHostService, exeCtrlUtilsService, notificationService, communicationService,
            distributedComponentKnowledgeService, executionController);
    }

    public WorkflowExecutionControllerServiceImplTestBuilder expectWorkflowHostServiceGetLogicalWorkflowHostNodesIsCalled(
        LogicalNodeId localNodeId) {
        EasyMock.expect(workflowHostService.getLogicalWorkflowHostNodes()).andStubReturn(new HashSet<>(Arrays.asList(localNodeId)));
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

}
