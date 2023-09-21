/*
 * Copyright 2022-2023 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.execution.internal;

import static de.rcenvironment.core.utils.testing.CollectionMatchers.contains;
import static de.rcenvironment.core.utils.testing.CollectionMatchers.isEmpty;
import static de.rcenvironment.core.utils.testing.CollectionMatchers.size;
import static org.hamcrest.CoreMatchers.allOf;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.easymock.EasyMock;
import org.hamcrest.CoreMatchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.component.api.ComponentUtils;
import de.rcenvironment.core.component.execution.api.ExecutionControllerException;
import de.rcenvironment.core.component.model.api.ComponentDescription;
import de.rcenvironment.core.component.model.api.ComponentInstallation;
import de.rcenvironment.core.component.workflow.execution.api.FinalWorkflowState;
import de.rcenvironment.core.component.workflow.execution.api.RemotableWorkflowExecutionControllerService;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionContext;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionException;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionInformation;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowState;
import de.rcenvironment.core.component.workflow.execution.headless.internal.HeadlessWorkflowExecutionContextMock;
import de.rcenvironment.core.component.workflow.execution.impl.WorkflowExecutionContextImpl;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNodeIdentifier;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;

/**
 * Tests for {@link WorkflowExecutionServiceImpl}.
 * 
 * @author Alexander Weinert
 * @author Kathrin Schaffert
 */
public class WorkflowExecutionServiceImplTest {

    /** Rule for expecting an Exception during test run. */
    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    // TODO ggf wieder umbenennen ohne Mock
    @Test
    public void whenControllingWorkflow_thenWorkflowControllerMockIsCalled()
        throws WorkflowExecutionException, ExecutionControllerException, RemoteOperationException {
        final LogicalNodeId localNodeId = localNodeId();

        final Map<String, String> authTokens = new HashMap<>();

        final WorkflowDescription description = new WorkflowDescription(workflowIdentifier());
        description.setControllerNode(localNodeId);

        final WorkflowExecutionContext context = new WorkflowExecutionContextImpl(executionIdentifier(), description);
        context.setNodeIdentifierStartedExecution(localNodeId);

        final WorkflowExecutionServiceImplTestBuilder builder = new WorkflowExecutionServiceImplTestBuilder();
        final WorkflowExecutionServiceImpl service = builder
            .withLocalNodeId(localNodeId)
            .expectAuthorizationTokenAcquisition(isEmpty(), authTokens)
            .expectControllerServiceCreation(localNodeId)
            .expectLocalControllerCreation(localNodeId, context, authTokens)
            .expectStartOnController(localNodeId, executionIdentifier())
            .expectPauseOnController(localNodeId, executionIdentifier())
            .expectResumeOnController(localNodeId, executionIdentifier())
            .expectCancelOnController(localNodeId, executionIdentifier())
            .expectDisposeOnController(localNodeId, executionIdentifier())
            .build();

        final WorkflowExecutionInformation info = service.start(context);
        service.pause(info.getWorkflowExecutionHandle());
        service.resume(info.getWorkflowExecutionHandle());
        service.cancel(info.getWorkflowExecutionHandle());
        service.dispose(info.getWorkflowExecutionHandle());

        builder.verifyAllDependencies();
    }
    // TODO dicuss with Alex, if needed
//    @Test
//    public void whenControllingWorkflow_thenWorkflowControllerIsCalled()
//        throws WorkflowExecutionException, ExecutionControllerException, RemoteOperationException {
//        final LogicalNodeId localNodeId = localNodeId();
//
//        final Map<String, String> authTokens = new HashMap<>();
//
//        final WorkflowDescription description = new WorkflowDescription(workflowIdentifier());
//        description.setControllerNode(localNodeId);
//
//        final WorkflowExecutionContext context = new WorkflowExecutionContextImpl(executionIdentifier(), description);
//        context.setNodeIdentifierStartedExecution(localNodeId);
//
//        final WorkflowExecutionServiceImplTestBuilder builder = new WorkflowExecutionServiceImplTestBuilder();
//        final WorkflowExecutionServiceImpl service = builder
//            .withLocalNodeId(localNodeId)
//            .expectAuthorizationTokenAcquisition(isEmpty(), authTokens)
//            .expectControllerServiceCreation(localNodeId)
//            .expectLocalControllerCreation(localNodeId, context, authTokens)
////            .expectStartOnController(localNodeId, executionIdentifier())
//            .build();
//
//        final WorkflowExecutionInformation info = service.start(context);
//
////        service.pause(info.getWorkflowExecutionHandle());
////        service.resume(info.getWorkflowExecutionHandle());
////        service.cancel(info.getWorkflowExecutionHandle());
////        service.dispose(info.getWorkflowExecutionHandle());
//
////        builder.verifyAllDependencies();
//    }

    @Test
    public void whenWorkflowHostIsRemote_thenWorkflowControllerIsRemote()
        throws ExecutionControllerException, RemoteOperationException, WorkflowExecutionException {
        final LogicalNodeId localNodeId = localNodeId();
        final LogicalNodeId remoteNodeId = remoteNodeId();

        final Map<String, String> authTokens = new HashMap<>();

        final WorkflowDescription description = new WorkflowDescription(workflowIdentifier());
        description.setControllerNode(remoteNodeId);

        final WorkflowExecutionContext context = new WorkflowExecutionContextImpl(executionIdentifier(), description);
        context.setNodeIdentifierStartedExecution(localNodeId);

        final WorkflowExecutionServiceImplTestBuilder builder = new WorkflowExecutionServiceImplTestBuilder();
        final WorkflowExecutionServiceImpl service = builder
            .withLocalNodeId(localNodeId)
            .expectAuthorizationTokenAcquisition(CoreMatchers.any(Collection.class), authTokens)
            .expectControllerServiceCreation(remoteNodeId)
            .expectRemoteControllerCreation(remoteNodeId, context, authTokens)
            .expectStartOnController(remoteNodeId, executionIdentifier())
            .build();

        service.start(context);

        builder.verifyAllDependencies();
    }

    @Test
    public void whenWorkflowContainsNodes_thenAuthorizationTokensAreAcquiredForEachNode() throws WorkflowExecutionException {
        final LogicalNodeId localNodeId = localNodeId();
        final LogicalNodeId remoteNodeId = remoteNodeId();

        final Map<String, String> authTokens = new HashMap<>();
        authTokens.put(localNodeId().toString(), "localAuthToken");
        authTokens.put(remoteNodeId().toString(), "remoteAuthToken");

        final WorkflowDescription description = new WorkflowDescription(workflowIdentifier());
        description.setControllerNode(remoteNodeId);

        final ComponentInstallation localInstallation =
            ComponentUtils.createPlaceholderComponentInstallation("localComponent", "v1", "localComponent", localNodeId);
        final ComponentDescription localDescription = new ComponentDescription(localInstallation);
        final WorkflowNode localNode = new WorkflowNode(localDescription);
        description.addWorkflowNode(localNode);

        final ComponentInstallation remoteInstallation =
            ComponentUtils.createPlaceholderComponentInstallation("remoteComponent", "v1", "remoteComponent", remoteNodeId);
        final ComponentDescription remoteDescription = new ComponentDescription(remoteInstallation);
        final WorkflowNode remoteNode = new WorkflowNode(remoteDescription);
        description.addWorkflowNode(remoteNode);

        final WorkflowExecutionContext context = new WorkflowExecutionContextImpl(executionIdentifier(), description);
        context.setNodeIdentifierStartedExecution(localNodeId);

        final WorkflowExecutionServiceImplTestBuilder builder = new WorkflowExecutionServiceImplTestBuilder();
        final WorkflowExecutionServiceImpl service = builder
            .withLocalNodeId(localNodeId)
            .expectAuthorizationTokenAcquisition(allOf(size(2), contains(localNode), contains(remoteNode)), authTokens)
            .expectControllerServiceCreation(remoteNodeId)
            .expectRemoteControllerCreation(remoteNodeId, context, authTokens)
            .expectStartOnController(remoteNodeId, executionIdentifier())
            .build();

        service.start(context);

        builder.verifyAllDependencies();
    }

    @Test
    public void whenWaitingForWorkflowExecutionTermination() throws WorkflowExecutionException {
        final LogicalNodeId localNodeId = localNodeId();

        final WorkflowDescription description = new WorkflowDescription(workflowIdentifier());
        description.setControllerNode(localNodeId);

        final HeadlessWorkflowExecutionContextMock context = new HeadlessWorkflowExecutionContextMock(executionIdentifier(), description);
        context.setNodeIdentifierStartedExecution(localNodeId);

        final WorkflowExecutionServiceImplTestBuilder builder = new WorkflowExecutionServiceImplTestBuilder();
        final WorkflowExecutionServiceImpl service = builder.build();

        context.setFinalState(WorkflowState.FINISHED);
        FinalWorkflowState finished = service.waitForWorkflowTermination(context);
        assertEquals(FinalWorkflowState.FINISHED, finished);
        context.setFinalState(WorkflowState.CANCELLED);
        FinalWorkflowState cancelled = service.waitForWorkflowTermination(context);
        assertEquals(FinalWorkflowState.CANCELLED, cancelled);
        context.setFinalState(WorkflowState.FAILED);
        FinalWorkflowState failed = service.waitForWorkflowTermination(context);
        assertEquals(FinalWorkflowState.FAILED, failed);
        context.setFinalState(WorkflowState.RESULTS_REJECTED);
        FinalWorkflowState rejected = service.waitForWorkflowTermination(context);
        assertEquals(FinalWorkflowState.RESULTS_REJECTED, rejected);
    }

    @Test
    public void whenWaitingForWorkflowExecutionTerminationAndFinalStateUnknown() throws WorkflowExecutionException {
        final LogicalNodeId localNodeId = localNodeId();

        final WorkflowDescription description = new WorkflowDescription(workflowIdentifier());
        description.setControllerNode(localNodeId);

        final HeadlessWorkflowExecutionContextMock context = new HeadlessWorkflowExecutionContextMock(executionIdentifier(), description);
        context.setNodeIdentifierStartedExecution(localNodeId);

        final WorkflowExecutionServiceImplTestBuilder builder = new WorkflowExecutionServiceImplTestBuilder();
        final WorkflowExecutionServiceImpl service = builder.build();

        context.setFinalState(WorkflowState.UNKNOWN);

        exceptionRule.expect(WorkflowExecutionException.class);
        exceptionRule.expectMessage(WorkflowExecutionServiceImpl.FINAL_STATE_UNKNOWN_EXCEPTION_MESSAGE);
        service.waitForWorkflowTermination(context);
    }

    @Test
    public void whenWaitingForWorkflowExecutionTerminationAndStateIsNotFinal() throws WorkflowExecutionException {
        final LogicalNodeId localNodeId = localNodeId();

        final WorkflowDescription description = new WorkflowDescription(workflowIdentifier());
        description.setControllerNode(localNodeId);

        final HeadlessWorkflowExecutionContextMock context = new HeadlessWorkflowExecutionContextMock(executionIdentifier(), description);
        context.setNodeIdentifierStartedExecution(localNodeId);

        final WorkflowExecutionServiceImplTestBuilder builder = new WorkflowExecutionServiceImplTestBuilder();
        final WorkflowExecutionServiceImpl service = builder.build();

        context.setFinalState(WorkflowState.PAUSED);

        exceptionRule.expect(WorkflowExecutionException.class);
        exceptionRule.expectMessage("Unexpected value");
        service.waitForWorkflowTermination(context);
    }

    @Test
    public void whenWaitingForWorkflowExecutionTerminationAndInterruptedExceptionIsThrown() throws WorkflowExecutionException {
        final LogicalNodeId localNodeId = localNodeId();

        final WorkflowDescription description = new WorkflowDescription(workflowIdentifier());
        description.setControllerNode(localNodeId);

        final HeadlessWorkflowExecutionContextMock context = new HeadlessWorkflowExecutionContextMock(executionIdentifier(), description);
        context.setNodeIdentifierStartedExecution(localNodeId);

        final WorkflowExecutionServiceImplTestBuilder builder = new WorkflowExecutionServiceImplTestBuilder();
        final WorkflowExecutionServiceImpl service = builder.build();

        exceptionRule.expect(WorkflowExecutionException.class);
        exceptionRule.expectMessage(WorkflowExecutionServiceImpl.WORKFLOW_INTERRUPTED_EXCEPTION_MESSAGE);
        service.waitForWorkflowTermination(context);
    }

    // TODO add test that passes through catch clause
    @Test
    public void whenVailidatingRemoteWorkflowControllerVisibility() throws WorkflowExecutionException {

        final LogicalNodeId localNodeId = localNodeId();
        final WorkflowNode node = workflowNode();

        final WorkflowDescriptionMock description = new WorkflowDescriptionMock(workflowIdentifier());
        description.setControllerNode(localNodeId);
        description.addNode(node);

        List<String> componentRefs = new ArrayList<>();
        componentRefs.add(StringUtils.escapeAndConcat("workflowNodeIdentifier", "identifierWithVersion", "localNodeId"));

        final WorkflowExecutionServiceImplTestBuilder builder = new WorkflowExecutionServiceImplTestBuilder();
        final WorkflowExecutionServiceImpl service = builder
            .expectControllerServiceCreationAndComponentVisibilityVerification(localNodeId, componentRefs)
            .build();

        service.validateRemoteWorkflowControllerVisibilityOfComponents(description);
    }

    @Test
    public void whenGetLocalWorkflowExecutionInformations() throws ExecutionControllerException, RemoteOperationException {

        final RemotableWorkflowExecutionControllerService controllerService = controllerService();

        final WorkflowExecutionServiceImplTestBuilder builder = new WorkflowExecutionServiceImplTestBuilder();
        final WorkflowExecutionServiceImpl service = builder
            .bindWorkflowExecutionControllerService(controllerService)
            .build();

        service.getLocalWorkflowExecutionInformations();
    }

    @Test
    public void whenGetLocalWorkflowExecutionInformationsThrowsException() throws ExecutionControllerException, RemoteOperationException {

        final RemotableWorkflowExecutionControllerService controllerService = controllerServiceThrowsException();

        final WorkflowExecutionServiceImplTestBuilder builder = new WorkflowExecutionServiceImplTestBuilder();
        final WorkflowExecutionServiceImpl service = builder
            .bindWorkflowExecutionControllerService(controllerService)
            .build();

        exceptionRule.expect(IllegalStateException.class);
        exceptionRule.expectMessage("Failed to get local workflow execution information");

        service.getLocalWorkflowExecutionInformations();
    }

    private static String workflowIdentifier() {
        return "workflowIdentifier";
    }

    private static String executionIdentifier() {
        return "executionIdentifier";
    }

    private static LogicalNodeId localNodeId() {
        final LogicalNodeId localNodeId = EasyMock.createStrictMock(LogicalNodeId.class);
        EasyMock.expect(localNodeId.getLogicalNodeIdString()).andStubReturn("localNodeId");
        EasyMock.replay(localNodeId);
        return localNodeId;
    }

    private static LogicalNodeId remoteNodeId() {
        final LogicalNodeId localNodeId = EasyMock.createStrictMock(LogicalNodeId.class);
        EasyMock.expect(localNodeId.getLogicalNodeIdString()).andStubReturn("remoteNodeId");
        EasyMock.replay(localNodeId);
        return localNodeId;
    }

    private static WorkflowNode workflowNode() {
        final WorkflowNode node = EasyMock.createMock(WorkflowNode.class);

        EasyMock.expect(node.getComponentDescription()).andStubAnswer(() -> {
            final ComponentDescription description = EasyMock.createMock(ComponentDescription.class);
            EasyMock.expect(description.getNode()).andStubAnswer(() -> {
                final LogicalNodeId logicalNodeId = EasyMock.createStrictMock(LogicalNodeId.class);
                EasyMock.expect(logicalNodeId.getLogicalNodeIdString()).andStubReturn("logicalNodeId");
                EasyMock.replay(logicalNodeId);
                return logicalNodeId;
            });
            EasyMock.replay(description);
            return description;
        });

        EasyMock.expect(node.getIdentifierAsObject()).andStubAnswer(() -> {
            return new WorkflowNodeIdentifier("workflowNodeIdentifier");
        });

        EasyMock.expect(node.getComponentIdentifierWithVersion()).andStubAnswer(() -> {
            return "identifierWithVersion";
        });

        EasyMock.replay(node);
        return node;
    }

    private static RemotableWorkflowExecutionControllerService controllerService()
        throws ExecutionControllerException, RemoteOperationException {
        final RemotableWorkflowExecutionControllerService controllerService =
            EasyMock.createMock(RemotableWorkflowExecutionControllerService.class);

        EasyMock.expect(controllerService.getWorkflowExecutionInformations()).andStubReturn(new HashSet<>());
        EasyMock.replay(controllerService);
        return controllerService;
    }

    private static RemotableWorkflowExecutionControllerService controllerServiceThrowsException()
        throws ExecutionControllerException, RemoteOperationException {
        final RemotableWorkflowExecutionControllerService controllerService =
            EasyMock.createMock(RemotableWorkflowExecutionControllerService.class);

        EasyMock.expect(controllerService.getWorkflowExecutionInformations()).andThrow(new ExecutionControllerException("Test Exception"));
        EasyMock.replay(controllerService);
        return controllerService;
    }

}
