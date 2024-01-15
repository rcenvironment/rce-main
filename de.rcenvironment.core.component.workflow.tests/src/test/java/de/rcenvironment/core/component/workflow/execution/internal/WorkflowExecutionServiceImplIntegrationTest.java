/*
 * Copyright 2023 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.execution.internal;

import static de.rcenvironment.core.utils.testing.CollectionMatchers.isEmpty;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.hamcrest.CoreMatchers;
import org.junit.Test;

import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.component.execution.api.ExecutionControllerException;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionContext;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionException;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionInformation;
import de.rcenvironment.core.component.workflow.execution.impl.WorkflowExecutionContextImpl;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;

/**
 * Integration Tests for {@link WorkflowExecutionServiceImpl}.
 * 
 * @author Kathrin Schaffert
 */
public class WorkflowExecutionServiceImplIntegrationTest extends WorkflowExecutionServiceImplTestHelper {

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

}
