/*
 * Copyright 2006-2024 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.api;

import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;

public class MockWorkflowDescriptionBuilder {
    
    public static WorkflowDescription workflowDescription(String identifier) {
        return new WorkflowDescription(identifier);
    }

}
