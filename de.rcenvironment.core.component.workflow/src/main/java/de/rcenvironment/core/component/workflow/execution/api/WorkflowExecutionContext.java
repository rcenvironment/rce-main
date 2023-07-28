/*
 * Copyright 2006-2023 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.execution.api;

import java.io.File;

import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.component.execution.api.ComponentExecutionIdentifier;
import de.rcenvironment.core.component.execution.api.ExecutionContext;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.notification.DistributedNotificationService;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * Workflow-specific {@link ExecutionContext}.
 * 
 * @author Doreen Seider
 */
public interface WorkflowExecutionContext extends ExecutionContext {

    class LogDirectoryCreationFailureException extends WorkflowExecutionException {

        private static final long serialVersionUID = -2237004371001713067L;

        public LogDirectoryCreationFailureException(String logDirectoryPath, String workflowFilePath) {
            super(StringUtils.format("Failed to create log directory '%s' for execution of '%s'", logDirectoryPath, workflowFilePath));
        }
    }

    /**
     * Dispose behavior after workflow execution.
     * 
     * @author Doreen Seider
     */
    enum DisposalBehavior {
        Always, Never, OnExpected;
    }

    /**
     * Delete behavior after workflow execution.
     * 
     * @author Sascha Zur
     */
    enum DeletionBehavior {
        Always, Never, OnExpected;
    }

    /**
     * @return {@link WorkflowDescription} of the workflow executed
     */
    WorkflowDescription getWorkflowDescription();

    /**
     * @param wfNode workflow node of the component within the {@link WorkflowDescription}
     * @return execution identifier of the component with the given workflow node within the {@link WorkflowDescription}
     */
    ComponentExecutionIdentifier getCompExeIdByWfNode(WorkflowNode wfNode);

    /**
     * @return {@link InstanceNodeSessionId} of the instance the execution was started from
     */
    LogicalNodeId getNodeIdStartedExecution();

    /**
     * @return additional information optionally provided at workflow start
     */
    String getAdditionalInformationProvidedAtStart();

    /**
     * @return the {@link WorkflowExecutionHandle} object that references this workflow uniquely
     */
    WorkflowExecutionHandle getWorkflowExecutionHandle();

    /**
     * @return location for workflow log files. Is optional and can be <code>null</code>
     */
    File getLogDirectory();

    String getWorkflowOriginDisplayName();

    WorkflowState waitForTermination() throws InterruptedException;

    void reportFinalWorkflowState(WorkflowState finalState);

    void closeResourcesQuietly();

    File getWorkflowFile();

    File[] getLogFiles();

    long getExecutionDuration();

    void setNodeIdentifierStartedExecution(LogicalNodeId nodeIdentifier);
    
    void beforeExecutionStarted(DistributedNotificationService notificationServiceParam, LogicalNodeId localNodeId)
        throws WorkflowExecutionException;

    void afterExecutionStarted();

    void afterExecutionTerminated(DistributedNotificationService notificationService,
        WorkflowExecutionService workflowExecutionService, boolean behavedAsExpected);
}
