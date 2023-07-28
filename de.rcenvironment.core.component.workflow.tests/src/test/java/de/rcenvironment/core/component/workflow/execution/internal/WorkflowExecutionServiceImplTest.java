/*
 * Copyright 2006-2023 DLR, Germany
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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.easymock.EasyMock;
import org.hamcrest.CoreMatchers;
import org.junit.Test;

import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.component.api.ComponentUtils;
import de.rcenvironment.core.component.execution.api.ExecutionControllerException;
import de.rcenvironment.core.component.model.api.ComponentDescription;
import de.rcenvironment.core.component.model.api.ComponentInstallation;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionContext;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionException;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionInformation;
import de.rcenvironment.core.component.workflow.execution.impl.WorkflowExecutionContextImpl;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;

public class WorkflowExecutionServiceImplTest {

    @Test
    public void whenControllingWorkflow_thenWorkflowControllerIsCalled()
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

        final ComponentInstallation localInstallation = ComponentUtils.createPlaceholderComponentInstallation("localComponent", "v1", "localComponent", localNodeId);
        final ComponentDescription localDescription = new ComponentDescription(localInstallation);
        final WorkflowNode localNode = new WorkflowNode(localDescription);
        description.addWorkflowNode(localNode);

        final ComponentInstallation remoteInstallation = ComponentUtils.createPlaceholderComponentInstallation("remoteComponent", "v1", "remoteComponent", remoteNodeId);
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

}
