/*
 * Copyright 2023 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.execution.internal;

import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;

class WorkflowDescriptionMock extends WorkflowDescription {

    /** Generated only for testing. */
    private static final long serialVersionUID = 1L;

    WorkflowDescriptionMock(String identifier) {
        super(identifier);
    }

    public void addNode(WorkflowNode node) {
        getNodes().add(node);
    }

}
