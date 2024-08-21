/*
 * Copyright 2006-2023 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.execution.internal;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.authorization.api.AuthorizationPermissionSet;
import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.component.model.configuration.api.PlaceholdersMetaDataDefinition;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionContext;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionException;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionInformation;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowState;
import de.rcenvironment.core.component.workflow.execution.headless.internal.HeadlessWorkflowExecutionContextImpl;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;
import de.rcenvironment.core.utils.incubator.DebugSettings;

public class WorkflowExecutionServiceLog {

    private final Log log = LogFactory.getLog(WorkflowExecutionServiceImpl.class); // NOSONAR, since this method encapsulates logging
                                                                                   // for WorkflowExecutionServiceImpl

    private final boolean verboseLogging = DebugSettings.getVerboseLoggingEnabled("WorkflowExecution");

    public void authorizationGroupsForWorkflowNodeDetermined(LogicalNodeId compLocation, String compIdWithoutVersion,
        AuthorizationPermissionSet matchingPermissionSet) {
        log.debug(StringUtils.format("Determined [%s] as the list of available authorization group(s) "
            + "for component '%s' on %s", matchingPermissionSet, compIdWithoutVersion, compLocation));
    }

    public void placeholderFileParsed(final File placeholdersFile, final Map<String, Map<String, String>> placeholderValues) {
        log.debug(StringUtils.format("Loaded placeholder values from %s: %s", placeholdersFile.getAbsolutePath(), placeholderValues));
    }

    public void workflowInformationQueryFailed(final InstanceNodeSessionId finalNode, RemoteOperationException e) {
        log.error(StringUtils.format("Failed to query remote workflows on node %s; cause: %s",
            finalNode, e.toString()));
    }

    public String executionPermissionAcquisitionFailed(WorkflowNode wfDescriptionNode, Exception e) {
        final String message = "Failed to acquire permission to execute component \"" + wfDescriptionNode.getName()
            + "\" on " + wfDescriptionNode.getComponentDescription().getNode();
        // log here immediately (without a stacktrace), as only the first exception will be rethrown
        log.error(message + ": " + e.toString());
        return message;
    }

    public void workflowDisposalFailed(WorkflowExecutionContext executionContext, final String wfExecutionId,
        Exception e) {
        log.error(StringUtils.format("Failed to dispose workflow '%s' (%s) ",
            executionContext.getInstanceName(), wfExecutionId), e);
    }

    public void waitingForDisposalWasInterrupted(WorkflowExecutionContext executionContext, final String wfExecutionId,
        InterruptedException e) {
        log.error(StringUtils.format("Received interruption signal while waiting for disposeal of workflow '%s' (%s) ",
            executionContext.getInstanceName(), wfExecutionId), e);
    }

    public void workflowDeletionFailed(WorkflowExecutionContext executionContext, final String wfExecutionId,
        Exception e) {
        log.error(StringUtils.format("Failed to delete workflow '%s' (%s) ",
            executionContext.getInstanceName(), wfExecutionId), e);
    }

    public void logDirectoryDeletionFailed(WorkflowExecutionContext executionContext, IOException e) {
        log.error("Failed to delete log directory: " + executionContext.getLogDirectory(), e);
    }

    public void error(Exception e) {
        log.error(e.getMessage(), e);
    }

    public void fetchingLocalWorkflowExecutionInformationFailed(Exception e) {
        log.error("Failed to fetch local workflow execution informations: " + e.getMessage());
    }

    public void appliedPlaceholdersToWorkflowNode(WorkflowNode wn, Map<String, String> cPlaceholderValues) {
        PlaceholdersMetaDataDefinition placeholderMetaDataDefinition = wn.getComponentDescription().getConfigurationDescription()
            .getComponentConfigurationDefinition().getPlaceholderMetaDataDefinition();

        Map<String, String> cPlaceholderValuesToLog = new HashMap<>();
        for (Map.Entry<String, String> cPlaceholderEntry : cPlaceholderValues.entrySet()) {
            final String cPlaceholderKey = cPlaceholderEntry.getKey();
            if (placeholderMetaDataDefinition.decode(cPlaceholderKey)) {
                cPlaceholderValuesToLog.put(cPlaceholderKey, "*****");
            } else {
                cPlaceholderValuesToLog.put(cPlaceholderKey, cPlaceholderEntry.getValue());
            }
        }
        log.debug(StringUtils.format("Applying %d placeholder value(s) to workflow node %s: %s", cPlaceholderValues.size(), wn,
            cPlaceholderValuesToLog));
    }

    public void asynchronousExecutionFailed(Exception e) {
        log.warn("Exception during asynchrous execution", e);
    }

    public void heartbeatNotificationSent(WorkflowExecutionInformation wfExeInfo, String wfExeId) {
        if (verboseLogging) {
            log.debug(StringUtils.format("Sending heartbeat notification for active workflow '%s' (%s)",
                wfExeInfo.getInstanceName(), wfExeId));
        }
    }

    public void logFileDirectoryCreated(String logDirectoryPath, String workflowFilePath) {
        log.debug(StringUtils.format("Created log file directory '%s' for execution of '%s'", logDirectoryPath, workflowFilePath));
    }

    public void workflowStarted(String workflowFileDisplayName,
        WorkflowExecutionInformation wfExeInfo) {
        log.debug(StringUtils.format("Created workflow from file '%s' with name '%s', with id %s on node %s",
            workflowFileDisplayName, wfExeInfo.getInstanceName(), wfExeInfo.getExecutionIdentifier(),
            wfExeInfo.getNodeId()));
    }

    public String workflowStatechangeSubscriptionFailed(String workflowFileAbsolutePath, RemoteOperationException e) {
        String errorMessage = "Failed to execute workflow (error while subscribing for state changes)";
        log.error(StringUtils.format("%s: %s", errorMessage, workflowFileAbsolutePath), e);
        return errorMessage;
    }

    public void workflowStatechangeReceived(String workflowDisplayName, final String wfExecutionId, WorkflowState newState) {
        log.debug(StringUtils.format("Received state change event for workflow '%s' (%s): %s",
            workflowDisplayName, wfExecutionId, newState.getDisplayName()));
    }

    public void debug(String message) {
        log.debug(message);
    }

}