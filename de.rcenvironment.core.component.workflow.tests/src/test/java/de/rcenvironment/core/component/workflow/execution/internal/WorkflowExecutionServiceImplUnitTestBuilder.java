/*
 * Copyright 2022-2023 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.execution.internal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.easymock.EasyMock;

import de.rcenvironment.core.communication.common.CommunicationException;
import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.component.execution.api.ExecutionControllerException;
import de.rcenvironment.core.component.workflow.api.WorkflowConstants;
import de.rcenvironment.core.component.workflow.execution.api.RemotableWorkflowExecutionControllerService;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionContext;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionException;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionInformation;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowState;
import de.rcenvironment.core.component.workflow.execution.impl.WorkflowExecutionInformationImpl;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;

/**
 * Unit Test Builder for {@link WorkflowExecutionServiceImpl}.
 * 
 * @author Alexander Weinert
 * @author Kathrin Schaffert
 */
class WorkflowExecutionServiceImplUnitTestBuilder extends WorkflowExecutionServiceImplTestBuilder {

    static final String ERROR_MESSAGE = "any message";

    private interface ControllerMethod {
        void accept(String executionId) throws ExecutionControllerException, RemoteOperationException;
    }

    protected void replayAllServices() {
        EasyMock.replay(notificationService, authorizationTokenService, platformService, communicationService, metaDataService);
        for (RemotableWorkflowExecutionControllerService service : controllerServices.values()) {
            EasyMock.replay(service);
        }
    }

    public void verifyAllDependencies() {
        EasyMock.verify(notificationService, authorizationTokenService, platformService, communicationService, metaDataService);
        for (RemotableWorkflowExecutionControllerService service : controllerServices.values()) {
            EasyMock.verify(service);
        }
    }

    public WorkflowExecutionServiceImplUnitTestBuilder expectControllerServiceCreation(LogicalNodeId targetNode) {
        final RemotableWorkflowExecutionControllerService controllerService = getControllerService(targetNode);

        EasyMock
            .expect(communicationService.getRemotableService(RemotableWorkflowExecutionControllerService.class, targetNode))
            .andStubReturn(controllerService);

        return this;
    }

    public WorkflowExecutionServiceImplUnitTestBuilder expectLocalControllerCreationThrowsRemoteOperationException(
        LogicalNodeId targetNodeId,
        WorkflowExecutionContext context, Map<String, String> authTokens) throws WorkflowExecutionException, RemoteOperationException {
        return expectControllerCreationThrowsRemoteOperationException(targetNodeId, context, authTokens, false);
    }

    private WorkflowExecutionServiceImplUnitTestBuilder expectControllerCreationThrowsRemoteOperationException(LogicalNodeId targetNodeId,
        WorkflowExecutionContext context,
        Map<String, String> authTokens, boolean isRemote) throws WorkflowExecutionException, RemoteOperationException {
        final RemotableWorkflowExecutionControllerService controllerService = getControllerService(targetNodeId);

        EasyMock
            .expect(controllerService.createExecutionController(context, authTokens, isRemote))
            .andThrow(new RemoteOperationException(ERROR_MESSAGE));

        return this;
    }

    public WorkflowExecutionServiceImplUnitTestBuilder expectRemoteControllerCreation(LogicalNodeId targetNodeId,
        WorkflowExecutionContext context, Map<String, String> authTokens) {
        return expectControllerCreation(targetNodeId, context, authTokens, true);
    }

    protected WorkflowExecutionServiceImplUnitTestBuilder expectControllerCreation(LogicalNodeId targetNodeId,
        WorkflowExecutionContext context,
        Map<String, String> authTokens, boolean isRemote) {
        final RemotableWorkflowExecutionControllerService controllerService = getControllerService(targetNodeId);

        final WorkflowExecutionInformation info = new WorkflowExecutionInformationImpl(context);

        try {
            EasyMock
                .expect(controllerService.createExecutionController(context, authTokens, isRemote))
                .andStubReturn(info);
        } catch (WorkflowExecutionException | RemoteOperationException e) {
            // Will not be thrown, as we only call this method on a mock
        }

        return this;
    }

    public WorkflowExecutionServiceImplUnitTestBuilder expectStartOnController(LogicalNodeId localNodeId, String executionId) {
        final RemotableWorkflowExecutionControllerService controllerService = getControllerService(localNodeId);
        expectCallOnController(controllerService::performStart, executionId);
        return this;
    }

    public WorkflowExecutionServiceImplUnitTestBuilder expectPauseOnController(LogicalNodeId localNodeId, String executionId) {
        final RemotableWorkflowExecutionControllerService controllerService = getControllerService(localNodeId);
        expectCallOnController(controllerService::performPause, executionId);
        return this;
    }

    public WorkflowExecutionServiceImplUnitTestBuilder expectResumeOnController(LogicalNodeId localNodeId, String executionId) {
        final RemotableWorkflowExecutionControllerService controllerService = getControllerService(localNodeId);
        expectCallOnController(controllerService::performResume, executionId);
        return this;
    }

    public WorkflowExecutionServiceImplUnitTestBuilder expectCancelOnController(LogicalNodeId localNodeId, String executionId) {
        final RemotableWorkflowExecutionControllerService controllerService = getControllerService(localNodeId);
        expectCallOnController(controllerService::performCancel, executionId);
        return this;
    }

    public WorkflowExecutionServiceImplUnitTestBuilder expectDisposeOnController(LogicalNodeId localNodeId, String executionId) {
        final RemotableWorkflowExecutionControllerService controllerService = getControllerService(localNodeId);
        expectCallOnController(controllerService::performDispose, executionId);
        return this;
    }

    public WorkflowExecutionServiceImplUnitTestBuilder expectLocalControllerCreation(LogicalNodeId targetNodeId,
        WorkflowExecutionContext context, Map<String, String> authTokens) {
        return expectControllerCreation(targetNodeId, context, authTokens, false);
    }

    private void expectCallOnController(ControllerMethod method, String executionId) {
        try {
            method.accept(executionId);
            EasyMock.expectLastCall();
        } catch (ExecutionControllerException | RemoteOperationException e) {
            // Will not be thrown, as we only call this method on a mock
        }

    }

    public WorkflowExecutionServiceImplUnitTestBuilder expectThrowingExecutionControllerExceptionWhenStartOnController(
        LogicalNodeId localNodeId,
        String executionId) throws ExecutionControllerException, RemoteOperationException {
        final RemotableWorkflowExecutionControllerService controllerService = getControllerService(localNodeId);
        expectThrowsExecutionControllerExceptionWhenCallOnController(controllerService::performStart, executionId);
        return this;
    }

    public WorkflowExecutionServiceImplUnitTestBuilder expectThrowingRemoteOperationExceptionWhenStartOnController(
        LogicalNodeId localNodeId,
        String executionId) throws ExecutionControllerException, RemoteOperationException {
        final RemotableWorkflowExecutionControllerService controllerService = getControllerService(localNodeId);
        expectThrowsRemoteOperationExceptionWhenCallOnController(controllerService::performStart, executionId);
        return this;
    }

    private void expectThrowsExecutionControllerExceptionWhenCallOnController(ControllerMethod method, String executionId)
        throws ExecutionControllerException, RemoteOperationException {
        method.accept(executionId);
        EasyMock.expectLastCall().andThrow(new ExecutionControllerException(ERROR_MESSAGE));
    }

    private void expectThrowsRemoteOperationExceptionWhenCallOnController(ControllerMethod method, String executionId)
        throws ExecutionControllerException, RemoteOperationException {
        method.accept(executionId);
        EasyMock.expectLastCall().andThrow(new RemoteOperationException(ERROR_MESSAGE));
    }

    public WorkflowExecutionServiceImplUnitTestBuilder expectControllerServiceCreationAndComponentVisibilityVerification(
        LogicalNodeId localNodeId, List<String> componentRefs) {
        EasyMock
            .expect(communicationService.getRemotableService(RemotableWorkflowExecutionControllerService.class, localNodeId))
            .andStubAnswer(() -> {
                final RemotableWorkflowExecutionControllerService controllerService =
                    getControllerService(localNodeId);
                EasyMock.expect(controllerService.verifyComponentVisibility(componentRefs)).andStubReturn(new HashMap<>());
                EasyMock.replay(controllerService);
                return controllerService;
            });

        return this;
    }

    public WorkflowExecutionServiceImplUnitTestBuilder expectControllerServiceVisibilityVerificationThrowsException(
        LogicalNodeId localNodeId, List<String> componentRefs) {
        EasyMock
            .expect(communicationService.getRemotableService(RemotableWorkflowExecutionControllerService.class, localNodeId))
            .andStubAnswer(() -> {
                final RemotableWorkflowExecutionControllerService controllerService =
                    getControllerService(localNodeId);
                EasyMock.expect(controllerService.verifyComponentVisibility(componentRefs))
                    .andThrow(new RemoteOperationException(ERROR_MESSAGE));
                EasyMock.replay(controllerService);
                return controllerService;
            });

        return this;
    }

    public WorkflowExecutionServiceImplUnitTestBuilder expectNotificationServiceSendNotification(String identifier) {
        notificationService.send(WorkflowConstants.STATE_NOTIFICATION_ID + identifier, WorkflowState.IS_ALIVE.name());
        EasyMock.expectLastCall();
        return this;
    }

    public WorkflowExecutionServiceImplUnitTestBuilder expectControllerServiceReturnsWorkflowDataManagementId(LogicalNodeId targetNode,
        String identifier, long id)
        throws ExecutionControllerException, RemoteOperationException {
        final RemotableWorkflowExecutionControllerService controllerService = getControllerService(targetNode);
        EasyMock.expect(controllerService.getWorkflowDataManagementId(identifier)).andStubReturn(id);
        return this;
    }

    public WorkflowExecutionServiceImplUnitTestBuilder expectControllerServiceThrowsException(LogicalNodeId targetNode, String identifier)
        throws ExecutionControllerException, RemoteOperationException {
        final RemotableWorkflowExecutionControllerService controllerService = getControllerService(targetNode);
        EasyMock.expect(controllerService.getWorkflowDataManagementId(identifier))
            .andThrow(new ExecutionControllerException(ERROR_MESSAGE));
        return this;
    }

    public WorkflowExecutionServiceImplUnitTestBuilder expectMetaDataServiceDeletesWorkflowRun(LogicalNodeId localNodeId)
        throws CommunicationException {
        EasyMock.expect(metaDataService.deleteWorkflowRun(EasyMock.anyLong(), EasyMock.anyObject())).andStubReturn(true);
        return this;
    }

    public WorkflowExecutionServiceImplUnitTestBuilder expectMetaDataServiceThrowsException(LogicalNodeId localNodeId)
        throws CommunicationException {
        EasyMock.expect(metaDataService.deleteWorkflowRun(EasyMock.anyLong(), EasyMock.anyObject()))
            .andThrow(new CommunicationException(ERROR_MESSAGE));
        return this;
    }

    public WorkflowExecutionServiceImplUnitTestBuilder expectControllerServiceReturnsWorkflowState(LogicalNodeId targetNode,
        String identifier)
        throws ExecutionControllerException, RemoteOperationException {
        final RemotableWorkflowExecutionControllerService controllerService = getControllerService(targetNode);
        EasyMock.expect(controllerService.getWorkflowState(identifier)).andStubReturn(WorkflowState.CANCELLED);
        return this;
    }

    public WorkflowExecutionServiceImplUnitTestBuilder bindWorkflowExecutionControllerService(
        RemotableWorkflowExecutionControllerService controllerService) {
        service.bindWorkflowExecutionControllerService(controllerService);
        return this;
    }

    public WorkflowExecutionServiceImplUnitTestBuilder setMockedLog(WorkflowExecutionServiceLog log) {
        service.setLog(log);
        return this;
    }

    public WorkflowExecutionServiceImplUnitTestBuilder setMockedCache(WorkflowExecutionInformationCache cache) {
        service.setCache(cache);
        return this;
    }

    protected RemotableWorkflowExecutionControllerService getControllerService(LogicalNodeId localNodeId) {
        return this.controllerServices.computeIfAbsent(localNodeId,
            ignored -> EasyMock.createMock(RemotableWorkflowExecutionControllerService.class));
    }

}
