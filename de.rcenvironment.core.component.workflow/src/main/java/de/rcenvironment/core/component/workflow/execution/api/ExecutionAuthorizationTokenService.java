/*
 * Copyright 2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.execution.api;

import java.util.Collection;
import java.util.Map;

import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;

public interface ExecutionAuthorizationTokenService {

    Map<String, String> acquireExecutionAuthorizationTokensForComponents(Collection<WorkflowNode> workflowNodes)
        throws WorkflowExecutionException;

}
