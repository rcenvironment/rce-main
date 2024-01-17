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

/**
 * Parent class for {@link WorkflowExecutionServiceImpl} tests.
 * 
 * @author Kathrin Schaffert
 */
class WorkflowExecutionServiceImplTestHelper {

    static final String LOGICAL_NODE_ID = "logicalNodeId";

    static final String REMOTE_NODE_ID = "remoteNodeId";

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
}
