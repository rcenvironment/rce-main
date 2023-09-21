/*
 * Copyright 2022 DLR, Germany
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
import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.communication.common.ResolvableNodeId;
import de.rcenvironment.core.component.execution.api.ExecutionControllerException;
import de.rcenvironment.core.component.workflow.execution.api.ExecutionAuthorizationTokenService;
import de.rcenvironment.core.component.workflow.execution.api.RemotableWorkflowExecutionControllerService;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionContext;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionException;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionInformation;
import de.rcenvironment.core.component.workflow.execution.impl.WorkflowExecutionInformationImpl;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.notification.DistributedNotificationService;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;
import junit.framework.AssertionFailedError;

class WorkflowExecutionServiceImplTestBuilder {

    private final WorkflowExecutionServiceImpl service = new WorkflowExecutionServiceImpl();

    private final DistributedNotificationService notificationService = EasyMock.createMock(DistributedNotificationService.class);

    private final ExecutionAuthorizationTokenService authorizationTokenService =
        EasyMock.createMock(ExecutionAuthorizationTokenService.class);

    private final PlatformService platformService = EasyMock.createNiceMock(PlatformService.class);

    private final CommunicationService communicationService = EasyMock.createMock(CommunicationService.class);

    private final Map<ResolvableNodeId, RemotableWorkflowExecutionControllerService> controllerServices = new HashMap<>();

    private interface ControllerMethod {

        void accept(String executionId) throws ExecutionControllerException, RemoteOperationException;
    }

    public WorkflowExecutionServiceImpl build() {
        service.bindNotificationService(notificationService);
        service.bindExecutionAuthorizationTokenService(authorizationTokenService);
        service.bindPlatformService(platformService);
        service.bindCommunicationService(communicationService);

        replayAllServices();

        return service;
    }

    private void replayAllServices() {
        EasyMock.replay(notificationService, authorizationTokenService, platformService, communicationService);
        for (RemotableWorkflowExecutionControllerService service : controllerServices.values()) {
            EasyMock.replay(service);
        }
    }

    public void verifyAllDependencies() {
        EasyMock.verify(notificationService, authorizationTokenService, platformService, communicationService);
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

    public WorkflowExecutionServiceImplTestBuilder expectStartOnController(LogicalNodeId localNodeId, String executionId) {
        final RemotableWorkflowExecutionControllerService controllerService = getOrComputeControllerServiceMock(localNodeId);
        expectCallOnController(controllerService::performStart, executionId);
        return this;
    }

    // TODO dicuss with Alex, if needed
//    public WorkflowExecutionServiceImplTestBuilder expectStartOnController(LogicalNodeId localNodeId, String executionId) {
//        final RemotableWorkflowExecutionControllerService controllerService = getOrComputeControllerService(localNodeId);
//        try {
//            controllerService.performStart(executionId);
//        } catch (ExecutionControllerException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        } catch (RemoteOperationException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
////        expectCallOnController(controllerService::performStart, executionId);
//        return this;
//    }

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

    public WorkflowExecutionServiceImplTestBuilder expectControllerServiceCreationAndComponentVisibilityVerification(
        LogicalNodeId targetNode, List<String> componentRefs) {
        EasyMock
            .expect(communicationService.getRemotableService(RemotableWorkflowExecutionControllerService.class, targetNode))
            .andStubAnswer(() -> {
                final RemotableWorkflowExecutionControllerService controllerService =
                    EasyMock.createNiceMock(RemotableWorkflowExecutionControllerService.class);
                EasyMock.expect(controllerService.verifyComponentVisibility(componentRefs)).andStubReturn(new HashMap<>());
                EasyMock.replay(controllerService);
                return controllerService;
            });

        return this;
    }

    public WorkflowExecutionServiceImplTestBuilder bindWorkflowExecutionControllerService(
        RemotableWorkflowExecutionControllerService controllerService) {
        service.bindWorkflowExecutionControllerService(controllerService);
        return this;
    }

    // TODO: ggf wieder umbenennen ohne Mock
    private RemotableWorkflowExecutionControllerService getOrComputeControllerServiceMock(LogicalNodeId localNodeId) {
        return this.controllerServices.computeIfAbsent(localNodeId,
            ignored -> EasyMock.createMock(RemotableWorkflowExecutionControllerService.class));
    }

    // TODO dicuss with Alex, if needed
//    private RemotableWorkflowExecutionControllerService getOrComputeControllerService(LogicalNodeId localNodeId) {
//
//        WorkflowExecutionControllerServiceImplTestBuilder builder = new WorkflowExecutionControllerServiceImplTestBuilder();
//
//        return this.controllerServices.computeIfAbsent(localNodeId,
//            ignored -> builder.build());
//    }
}
