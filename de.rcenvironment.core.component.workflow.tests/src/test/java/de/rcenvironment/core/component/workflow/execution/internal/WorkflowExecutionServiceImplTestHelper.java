/*
 * Copyright 2023 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.component.workflow.execution.internal;

import org.easymock.EasyMock;

import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionHandle;

/**
 * Parent class for {@link WorkflowExecutionServiceImpl} tests.
 * 
 * @author Kathrin Schaffert
 */
class WorkflowExecutionServiceImplTestHelper {

    static final String LOGICAL_NODE_ID = "logicalNodeId";

    static final String REMOTE_NODE_ID = "remoteNodeId";

    static final String WORKFLOW_NODE_IDENTIFIER = "workflowNodeIdentifier";

    static final String WF_EXEC_INFO_IDENTIFIER = "wfExecInfoIdentifier";

    static final long DATA_MANAGEMENT_ID = 2345;

    protected WorkflowExecutionServiceImplTestHelper() {};

    static String workflowIdentifier() {
        return "workflowIdentifier";
    }

    static String executionIdentifier() {
        return "executionIdentifier";
    }

    static LogicalNodeId localNodeId() {
        final LogicalNodeId localNodeId = EasyMock.createStrictMock(LogicalNodeId.class);
        EasyMock.expect(localNodeId.getLogicalNodeIdString()).andStubReturn(LOGICAL_NODE_ID);
        EasyMock.expect(localNodeId.convertToDefaultLogicalNodeId()).andStubReturn(localNodeId);
        EasyMock.replay(localNodeId);
        return localNodeId;
    }

    static LogicalNodeId remoteNodeId() {
        final LogicalNodeId localNodeId = EasyMock.createStrictMock(LogicalNodeId.class);
        EasyMock.expect(localNodeId.getLogicalNodeIdString()).andStubReturn(REMOTE_NODE_ID);
        EasyMock.replay(localNodeId);
        return localNodeId;
    }

    static WorkflowExecutionHandle handle(LogicalNodeId localNodeId) {
        WorkflowExecutionHandle handle = EasyMock.createStrictMock(WorkflowExecutionHandle.class);

        EasyMock.expect(handle.getLocation()).andStubReturn(localNodeId);
        EasyMock.expect(handle.getIdentifier()).andStubReturn(executionIdentifier());
        EasyMock.replay(handle);
        return handle;
    }
}
