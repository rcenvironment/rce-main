/*
 * Copyright 2023 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.execution.headless.internal;

import java.io.File;

import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionService;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowState;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;
import de.rcenvironment.core.notification.DistributedNotificationService;

/**
 * Mock implementation for {@link HeadlessWorkflowExecutionContextImpl}.
 * 
 * @author Kathrin Schaffert
 */
public class HeadlessWorkflowExecutionContextMock extends HeadlessWorkflowExecutionContextImpl {

    private static final long serialVersionUID = 1L;

    private WorkflowState finalState;

    public HeadlessWorkflowExecutionContextMock(String executionIdentifier, WorkflowDescription description) {
        super(executionIdentifier, description);
        // TODO Auto-generated constructor stub
    }

    @Override
    public WorkflowState waitForTermination() throws InterruptedException {
        if (finalState != null) {
            return finalState;
        } else {
            throw new InterruptedException("thrown for unit testing");
        }
    }

    @Override
    public File[] getLogFiles() {
        return new File[0];
    }

    @Override
    public void afterExecutionTerminated(DistributedNotificationService notificationService,
        WorkflowExecutionService workflowExecutionService, boolean behavedAsExpected) {
        // Intentionally left empty for unit testing
    }

    public void setFinalState(WorkflowState finalState) {
        this.finalState = finalState;
    }

}
