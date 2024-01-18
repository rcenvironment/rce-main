/*
 * Copyright 2023 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.execution.internal;

import static de.rcenvironment.core.utils.testing.CollectionMatchers.isEmpty;
import static org.junit.Assert.assertEquals;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.hamcrest.CoreMatchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.component.execution.api.ExecutionControllerException;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionContext;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionException;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionInformation;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowState;
import de.rcenvironment.core.component.workflow.execution.impl.WorkflowExecutionContextImpl;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.component.workflow.model.api.testutils.WorkflowNodeMockBuilder;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;

/**
 * Integration Tests for {@link WorkflowExecutionServiceImpl}.
 * 
 * @author Kathrin Schaffert
 */
public class WorkflowExecutionServiceImplIntegrationTest extends WorkflowExecutionServiceImplTestHelper {

    /** Rule for expecting an Exception during test run. */
    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    @Test
    public void whenControllingWorkflowThenLocalWorkflowControllerIsCalled()
        throws WorkflowExecutionException, ExecutionControllerException, RemoteOperationException {
        final LogicalNodeId localNodeId = localNodeId();

        final Map<String, String> authTokens = new HashMap<>();

        final WorkflowDescription description = new WorkflowDescription(workflowIdentifier());
        description.setControllerNode(localNodeId);

        final WorkflowExecutionContext context = new WorkflowExecutionContextImpl(executionIdentifier(), description);
        context.setNodeIdentifierStartedExecution(localNodeId);

        final WorkflowExecutionServiceImplIntegrationTestBuilder builder =
            new WorkflowExecutionServiceImplIntegrationTestBuilder();
        final WorkflowExecutionServiceImpl service = ((WorkflowExecutionServiceImplIntegrationTestBuilder) builder
            .withLocalNodeId(localNodeId)
            .expectAuthorizationTokenAcquisition(isEmpty(), authTokens))
                .bindWorkflowExecutionControllerServiceMockExpectingAllWorklfowStates(localNodeId)
                .expectControllerServiceCreation(localNodeId, executionIdentifier())
                .build();

        final WorkflowExecutionInformation info = service.start(context);
        service.pause(info.getWorkflowExecutionHandle());
        service.resume(info.getWorkflowExecutionHandle());
        service.cancel(info.getWorkflowExecutionHandle());
        service.dispose(info.getWorkflowExecutionHandle());

        builder.verifyAllDependencies();
    }

    @Test
    public void whenWorkflowHostIsRemoteThenWorkflowControllerIsRemote()
        throws ExecutionControllerException, RemoteOperationException, WorkflowExecutionException {
        final LogicalNodeId localNodeId = localNodeId();
        final LogicalNodeId remoteNodeId = remoteNodeId();

        final Map<String, String> authTokens = new HashMap<>();

        final WorkflowDescription description = new WorkflowDescription(workflowIdentifier());
        description.setControllerNode(remoteNodeId);

        final WorkflowExecutionContext context = new WorkflowExecutionContextImpl(executionIdentifier(), description);
        context.setNodeIdentifierStartedExecution(localNodeId);

        final WorkflowExecutionServiceImplIntegrationTestBuilder builder = new WorkflowExecutionServiceImplIntegrationTestBuilder();
        final WorkflowExecutionServiceImpl service =
            ((WorkflowExecutionServiceImplIntegrationTestBuilder) builder
                .withLocalNodeId(localNodeId)
                .expectAuthorizationTokenAcquisition(CoreMatchers.any(Collection.class), authTokens))
                    .bindWorkflowExecutionControllerService(remoteNodeId)
                    .expectControllerServiceCreation(remoteNodeId, executionIdentifier())
                    .build();

        service.start(context);

        builder.verifyAllDependencies();
    }

    @Test
    public void whenStartWorkflowExecutionThrowsExecutionControllerException()
        throws WorkflowExecutionException, ExecutionControllerException, RemoteOperationException {
        final LogicalNodeId localNodeId = localNodeId();

        final Map<String, String> authTokens = new HashMap<>();

        final WorkflowDescription description = new WorkflowDescription(workflowIdentifier());
        description.setControllerNode(localNodeId);

        final WorkflowExecutionContext context = new WorkflowExecutionContextImpl(executionIdentifier(), description);
        context.setNodeIdentifierStartedExecution(localNodeId);

        final WorkflowExecutionServiceImplIntegrationTestBuilder builder = new WorkflowExecutionServiceImplIntegrationTestBuilder();
        final WorkflowExecutionServiceImpl service =
            ((WorkflowExecutionServiceImplIntegrationTestBuilder) builder
                .withLocalNodeId(localNodeId)
                .expectAuthorizationTokenAcquisition(isEmpty(), authTokens))
                    .bindWorkflowExecutionControllerServiceThrowingExceptionWhenStartOnController(localNodeId, executionIdentifier())
                    .expectControllerServiceCreation(localNodeId, executionIdentifier())
                    .build();

        exceptionRule.expect(WorkflowExecutionException.class);
        exceptionRule.expectMessage(WorkflowExecutionServiceImpl.WF_EXECUTION_FAILURE_EXCEPTION_MESSAGE);
        service.start(context);

    }

    @Test
    public void whenWorkflowExecutionRequestIsRefused()
        throws WorkflowExecutionException, ExecutionControllerException, RemoteOperationException {
        final LogicalNodeId localNodeId = localNodeId();
        final LogicalNodeId remoteNodeId = remoteNodeId();

        final Map<String, String> authTokens = new HashMap<>();

        final WorkflowDescription description = new WorkflowDescription(workflowIdentifier());
        description.setControllerNode(remoteNodeId);

        final WorkflowExecutionContext context = new WorkflowExecutionContextImpl(executionIdentifier(), description);
        context.setNodeIdentifierStartedExecution(remoteNodeId);

        final WorkflowExecutionServiceImplIntegrationTestBuilder builder = new WorkflowExecutionServiceImplIntegrationTestBuilder();
        final WorkflowExecutionServiceImpl service =
            ((WorkflowExecutionServiceImplIntegrationTestBuilder) builder
                .withLocalNodeId(localNodeId)
                .expectAuthorizationTokenAcquisition(isEmpty(), authTokens))
                    .bindWorkflowExecutionControllerServiceRefusingRequest(remoteNodeId)
                    .expectControllerServiceCreation(remoteNodeId, executionIdentifier())
                    .build();

        exceptionRule.expect(WorkflowExecutionException.class);
        exceptionRule.expectMessage(WorkflowExecutionControllerServiceImpl.ERROR_MESSAGE_WF_EXECUTION_REFUSED + " " + context.getNodeId());
        service.start(context);
    }

    @Test
    public void whenVailidatingRemoteWorkflowControllerVisibilityAndComponentIsVisible() throws WorkflowExecutionException {

        final LogicalNodeId localNodeId = localNodeId();
        final WorkflowNode node = new WorkflowNodeMockBuilder()
            .identifier(WORKFLOW_NODE_IDENTIFIER)
            .build();

        final WorkflowDescriptionMock description = new WorkflowDescriptionMock(workflowIdentifier());
        description.setControllerNode(localNodeId);
        description.addNode(node);

        final WorkflowExecutionServiceImplIntegrationTestBuilder builder = new WorkflowExecutionServiceImplIntegrationTestBuilder();
        final WorkflowExecutionServiceImpl service = builder
            .bindWorkflowExecutionControllerServiceToVailidateControllerVisibility(localNodeId, true)
            .expectControllerServiceCreation(localNodeId, executionIdentifier())
            .build();

        Map<String, String> result = service.validateRemoteWorkflowControllerVisibilityOfComponents(description);
        assertEquals(0, result.size());

        builder.verifyAllDependencies();
    }

    @Test
    public void whenVailidatingRemoteWorkflowControllerVisibilityAndComponentIsNotVisible() throws WorkflowExecutionException {

        final LogicalNodeId localNodeId = localNodeId();
        final WorkflowNode node = new WorkflowNodeMockBuilder()
            .identifier(WORKFLOW_NODE_IDENTIFIER)
            .build();

        final WorkflowDescriptionMock description = new WorkflowDescriptionMock(workflowIdentifier());
        description.setControllerNode(localNodeId);
        description.addNode(node);

        final WorkflowExecutionServiceImplIntegrationTestBuilder builder = new WorkflowExecutionServiceImplIntegrationTestBuilder();
        final WorkflowExecutionServiceImpl service = builder
            .bindWorkflowExecutionControllerServiceToVailidateControllerVisibility(localNodeId, false)
            .expectControllerServiceCreation(localNodeId, executionIdentifier())
            .build();

        Map<String, String> result = service.validateRemoteWorkflowControllerVisibilityOfComponents(description);
        builder.verifyAllDependencies();
        assertEquals(WorkflowExecutionControllerServiceImpl.ERROR_MESSAGE_CANNOT_ACCESS_COMPONENT,
            result.get(WORKFLOW_NODE_IDENTIFIER));
    }

    @Test
    public void whenGetLocalWorkflowExecutionInformations() throws ExecutionControllerException, RemoteOperationException {

        final LogicalNodeId localNodeId = localNodeId();
        final WorkflowDescription description = new WorkflowDescription(workflowIdentifier());
        final WorkflowExecutionContext context = new WorkflowExecutionContextImpl(executionIdentifier(), description);

        final WorkflowExecutionServiceImplIntegrationTestBuilder builder = new WorkflowExecutionServiceImplIntegrationTestBuilder();
        final WorkflowExecutionServiceImpl service = builder
            .bindWorkflowExecutionControllerServiceWithWfExecutionInformations(localNodeId, context)
            .build();

        Set<WorkflowExecutionInformation> actualSet = service.getLocalWorkflowExecutionInformations();
        Optional<WorkflowExecutionInformation> entry = actualSet.stream().findFirst();
        assertEquals(true, entry.isPresent());
        WorkflowExecutionInformation info = entry.get();
        assertEquals(WorkflowState.RUNNING, info.getWorkflowState());
        final long expected = 2345;
        Long current = info.getWorkflowDataManagementId();
        assertEquals(expected, current.longValue());
    }

    @Test
    public void whenSendHeartBeatForActiveWorkflowsForStateRunning()
        throws ExecutionControllerException, RemoteOperationException {

        final LogicalNodeId localNodeId = localNodeId();
        final WorkflowDescription description = new WorkflowDescription(workflowIdentifier());
        final WorkflowExecutionContext context = new WorkflowExecutionContextImpl(executionIdentifier(), description);

        final WorkflowExecutionServiceImplIntegrationTestBuilder builder = new WorkflowExecutionServiceImplIntegrationTestBuilder();
        final WorkflowExecutionServiceImpl service = builder
            .bindWorkflowExecutionControllerServiceSendHeartbeat(localNodeId, context)
            .expectNotificationServiceSendNotification(executionIdentifier())
            .build();

        service.sendHeartbeatForActiveWorkflows();
        builder.verifyAllDependencies();
    }

}
