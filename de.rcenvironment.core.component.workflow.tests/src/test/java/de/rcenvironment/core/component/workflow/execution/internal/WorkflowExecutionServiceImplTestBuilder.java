/*
 * Copyright 2022-2023 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.execution.internal;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.hamcrest.Matcher;

import de.rcenvironment.core.communication.api.CommunicationService;
import de.rcenvironment.core.communication.api.PlatformService;
import de.rcenvironment.core.communication.common.CommunicationException;
import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.communication.common.ResolvableNodeId;
import de.rcenvironment.core.component.execution.api.ExecutionControllerException;
import de.rcenvironment.core.component.workflow.api.WorkflowConstants;
import de.rcenvironment.core.component.workflow.execution.api.ExecutionAuthorizationTokenService;
import de.rcenvironment.core.component.workflow.execution.api.RemotableWorkflowExecutionControllerService;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionContext;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionException;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionInformation;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowState;
import de.rcenvironment.core.component.workflow.execution.impl.WorkflowExecutionInformationImpl;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.datamanagement.MetaDataService;
import de.rcenvironment.core.notification.DistributedNotificationService;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;
import junit.framework.AssertionFailedError;

class WorkflowExecutionServiceImplTestBuilder {

    static final String ERROR_MESSAGE = "any message";

    private final WorkflowExecutionServiceImpl service = new WorkflowExecutionServiceImpl();

    private final DistributedNotificationService notificationService = EasyMock.createMock(DistributedNotificationService.class);

    private final ExecutionAuthorizationTokenService authorizationTokenService =
        EasyMock.createMock(ExecutionAuthorizationTokenService.class);

    private final PlatformService platformService = EasyMock.createNiceMock(PlatformService.class);

    private final CommunicationService communicationService = EasyMock.createMock(CommunicationService.class);

    private final Map<ResolvableNodeId, RemotableWorkflowExecutionControllerService> controllerServices = new HashMap<>();

    private final MetaDataService metaDataService = EasyMock.createMock(MetaDataService.class);

    private interface ControllerMethod {

        void accept(String executionId) throws ExecutionControllerException, RemoteOperationException;
    }

    public WorkflowExecutionServiceImpl build() {
        service.bindNotificationService(notificationService);
        service.bindExecutionAuthorizationTokenService(authorizationTokenService);
        service.bindPlatformService(platformService);
        service.bindCommunicationService(communicationService);
        service.bindMetaDataService(metaDataService);

        replayAllServices();

        return service;
    }

    private void replayAllServices() {
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

    public WorkflowExecutionServiceImplTestBuilder expectAuthorizationTokenAcquisition(Matcher<?> expectedNodes,
        Map<String, String> authTokens) {
        final Capture<Collection<WorkflowNode>> parameterMatcher = Capture.newInstance(CaptureType.LAST);
        try {
            EasyMock.expect(authorizationTokenService.acquireExecutionAuthorizationTokensForComponents(EasyMock.capture(parameterMatcher)))
                .andStubAnswer(() -> {
                    if (!expectedNodes.matches(parameterMatcher.getValue())) {
                        throw new AssertionFailedError();
                    }
                    return authTokens;
                });
        } catch (WorkflowExecutionException e) {
            // Will never happen, since we call this method only on a mock
        }

        return this;
    }

    WorkflowExecutionServiceImplTestBuilder withLocalNodeId(LogicalNodeId localNodeId) {
        EasyMock.expect(platformService.getLocalDefaultLogicalNodeId()).andStubReturn(localNodeId);

        final Capture<ResolvableNodeId> matchesLocalInstanceArgument = Capture.newInstance(CaptureType.LAST);
        EasyMock
            .expect(platformService.matchesLocalInstance(EasyMock.capture(matchesLocalInstanceArgument)))
            .andStubAnswer(() -> matchesLocalInstanceArgument.getValue().equals(localNodeId));

        return this;
    }

    public WorkflowExecutionServiceImplTestBuilder expectControllerServiceCreation(LogicalNodeId targetNode) {
        final RemotableWorkflowExecutionControllerService controllerService = getOrComputeControllerServiceMock(targetNode);

        EasyMock
            .expect(communicationService.getRemotableService(RemotableWorkflowExecutionControllerService.class, targetNode))
            .andStubReturn(controllerService);

        return this;
    }

    public WorkflowExecutionServiceImplTestBuilder expectLocalControllerCreation(LogicalNodeId targetNodeId,
        WorkflowExecutionContext context, Map<String, String> authTokens) {
        return expectControllerCreation(targetNodeId, context, authTokens, false);
    }

    public WorkflowExecutionServiceImplTestBuilder expectLocalControllerCreationThrowsRemoteOperationException(LogicalNodeId targetNodeId,
        WorkflowExecutionContext context, Map<String, String> authTokens) throws WorkflowExecutionException, RemoteOperationException {
        return expectControllerCreationThrowsRemoteOperationException(targetNodeId, context, authTokens, false);
    }

    public WorkflowExecutionServiceImplTestBuilder expectRemoteControllerCreation(LogicalNodeId targetNodeId,
        WorkflowExecutionContext context, Map<String, String> authTokens) {
        return expectControllerCreation(targetNodeId, context, authTokens, true);
    }

    private WorkflowExecutionServiceImplTestBuilder expectControllerCreation(LogicalNodeId targetNodeId, WorkflowExecutionContext context,
        Map<String, String> authTokens, boolean isRemote) {
        final RemotableWorkflowExecutionControllerService controllerService = getOrComputeControllerServiceMock(targetNodeId);

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

    private WorkflowExecutionServiceImplTestBuilder expectControllerCreationThrowsRemoteOperationException(LogicalNodeId targetNodeId,
        WorkflowExecutionContext context,
        Map<String, String> authTokens, boolean isRemote) throws WorkflowExecutionException, RemoteOperationException {
        final RemotableWorkflowExecutionControllerService controllerService = getOrComputeControllerServiceMock(targetNodeId);

        EasyMock
            .expect(controllerService.createExecutionController(context, authTokens, isRemote))
            .andThrow(new RemoteOperationException(ERROR_MESSAGE));

        return this;
    }

    public WorkflowExecutionServiceImplTestBuilder expectStartOnController(LogicalNodeId localNodeId, String executionId) {
        final RemotableWorkflowExecutionControllerService controllerService = getOrComputeControllerServiceMock(localNodeId);
        expectCallOnController(controllerService::performStart, executionId);
        return this;
    }

    public WorkflowExecutionServiceImplTestBuilder expectThrowingExecutionControllerExceptionWhenStartOnController(
        LogicalNodeId localNodeId,
        String executionId) throws ExecutionControllerException, RemoteOperationException {
        final RemotableWorkflowExecutionControllerService controllerService = getOrComputeControllerServiceMock(localNodeId);
        expectThrowsExecutionControllerExceptionWhenCallOnController(controllerService::performStart, executionId);
        return this;
    }

    public WorkflowExecutionServiceImplTestBuilder expectThrowingRemoteOperationExceptionWhenStartOnController(LogicalNodeId localNodeId,
        String executionId) throws ExecutionControllerException, RemoteOperationException {
        final RemotableWorkflowExecutionControllerService controllerService = getOrComputeControllerServiceMock(localNodeId);
        expectThrowsRemoteOperationExceptionWhenCallOnController(controllerService::performStart, executionId);
        return this;
    }

    public WorkflowExecutionServiceImplTestBuilder expectPauseOnController(LogicalNodeId localNodeId, String executionId) {
        final RemotableWorkflowExecutionControllerService controllerService = getOrComputeControllerServiceMock(localNodeId);
        expectCallOnController(controllerService::performPause, executionId);
        return this;
    }

    public WorkflowExecutionServiceImplTestBuilder expectResumeOnController(LogicalNodeId localNodeId, String executionId) {
        final RemotableWorkflowExecutionControllerService controllerService = getOrComputeControllerServiceMock(localNodeId);
        expectCallOnController(controllerService::performResume, executionId);
        return this;
    }

    public WorkflowExecutionServiceImplTestBuilder expectCancelOnController(LogicalNodeId localNodeId, String executionId) {
        final RemotableWorkflowExecutionControllerService controllerService = getOrComputeControllerServiceMock(localNodeId);
        expectCallOnController(controllerService::performCancel, executionId);
        return this;
    }

    public WorkflowExecutionServiceImplTestBuilder expectDisposeOnController(LogicalNodeId localNodeId, String executionId) {
        final RemotableWorkflowExecutionControllerService controllerService = getOrComputeControllerServiceMock(localNodeId);
        expectCallOnController(controllerService::performDispose, executionId);
        return this;
    }

    private void expectCallOnController(ControllerMethod method, String executionId) {
        try {
            method.accept(executionId);
            EasyMock.expectLastCall();
        } catch (ExecutionControllerException | RemoteOperationException e) {
            // Will not be thrown, as we only call this method on a mock
        }

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

    public WorkflowExecutionServiceImplTestBuilder expectControllerServiceCreationAndComponentVisibilityVerification(
        LogicalNodeId localNodeId, List<String> componentRefs) {
        EasyMock
            .expect(communicationService.getRemotableService(RemotableWorkflowExecutionControllerService.class, localNodeId))
            .andStubAnswer(() -> {
                final RemotableWorkflowExecutionControllerService controllerService =
                    getOrComputeControllerServiceMock(localNodeId);
                EasyMock.expect(controllerService.verifyComponentVisibility(componentRefs)).andStubReturn(new HashMap<>());
                EasyMock.replay(controllerService);
                return controllerService;
            });

        return this;
    }

    public WorkflowExecutionServiceImplTestBuilder expectControllerServiceVisibilityVerificationThrowsException(
        LogicalNodeId localNodeId, List<String> componentRefs) {
        EasyMock
            .expect(communicationService.getRemotableService(RemotableWorkflowExecutionControllerService.class, localNodeId))
            .andStubAnswer(() -> {
                final RemotableWorkflowExecutionControllerService controllerService =
                    getOrComputeControllerServiceMock(localNodeId);
                EasyMock.expect(controllerService.verifyComponentVisibility(componentRefs))
                    .andThrow(new RemoteOperationException(ERROR_MESSAGE));
                EasyMock.replay(controllerService);
                return controllerService;
            });

        return this;
    }

    public WorkflowExecutionServiceImplTestBuilder expectNotificationServiceSendNotification(String identifier) {
        notificationService.send(WorkflowConstants.STATE_NOTIFICATION_ID + identifier, WorkflowState.IS_ALIVE.name());
        EasyMock.expectLastCall();
        return this;
    }

    public WorkflowExecutionServiceImplTestBuilder expectControllerServiceReturnsWorkflowDataManagementId(LogicalNodeId targetNode,
        String identifier, long id)
        throws ExecutionControllerException, RemoteOperationException {
        final RemotableWorkflowExecutionControllerService controllerService = getOrComputeControllerServiceMock(targetNode);
        EasyMock.expect(controllerService.getWorkflowDataManagementId(identifier)).andStubReturn(id);
        return this;
    }

    public WorkflowExecutionServiceImplTestBuilder expectControllerServiceThrowsException(LogicalNodeId targetNode, String identifier)
        throws ExecutionControllerException, RemoteOperationException {
        final RemotableWorkflowExecutionControllerService controllerService = getOrComputeControllerServiceMock(targetNode);
        EasyMock.expect(controllerService.getWorkflowDataManagementId(identifier))
            .andThrow(new ExecutionControllerException(ERROR_MESSAGE));
        return this;
    }

    public WorkflowExecutionServiceImplTestBuilder expectMetaDataServiceDeletesWorkflowRun(LogicalNodeId localNodeId)
        throws CommunicationException {
        EasyMock.expect(metaDataService.deleteWorkflowRun(EasyMock.anyLong(), EasyMock.anyObject())).andStubReturn(true);
        return this;
    }

    public WorkflowExecutionServiceImplTestBuilder expectMetaDataServiceThrowsException(LogicalNodeId localNodeId)
        throws CommunicationException {
        EasyMock.expect(metaDataService.deleteWorkflowRun(EasyMock.anyLong(), EasyMock.anyObject()))
            .andThrow(new CommunicationException(ERROR_MESSAGE));
        return this;
    }

    public WorkflowExecutionServiceImplTestBuilder bindWorkflowExecutionControllerService(
        RemotableWorkflowExecutionControllerService controllerService) {
        service.bindWorkflowExecutionControllerService(controllerService);
        return this;
    }

    public WorkflowExecutionServiceImplTestBuilder setMockedLog(WorkflowExecutionServiceLog log) {
        service.setLog(log);
        return this;
    }

    private RemotableWorkflowExecutionControllerService getOrComputeControllerServiceMock(LogicalNodeId localNodeId) {
        return this.controllerServices.computeIfAbsent(localNodeId,
            ignored -> EasyMock.createMock(RemotableWorkflowExecutionControllerService.class));
    }

}
