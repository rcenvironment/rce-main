/*
 * Copyright 2006-2023 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.execution.impl;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import de.rcenvironment.core.communication.api.ServiceCallContext;
import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.communication.common.NetworkDestination;
import de.rcenvironment.core.component.execution.api.ComponentExecutionIdentifier;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionContext;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionException;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionHandle;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionService;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowState;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNodeIdentifier;
import de.rcenvironment.core.notification.DistributedNotificationService;
import de.rcenvironment.core.utils.common.exception.NotImplementedException;

/**
 * Implementation of {@link WorkflowExecutionContext}.
 * 
 * @author Doreen Seider
 * @author Robert Mischke
 */
public class WorkflowExecutionContextImpl implements WorkflowExecutionContext {

    protected class DisplayName {

        private final Optional<String> shortForm;

        private final Optional<String> longForm;

        public DisplayName() {
            this.shortForm = Optional.empty();
            this.longForm = Optional.empty();
        }

        public DisplayName(String shortForm, String longForm) {
            this.shortForm = Optional.of(shortForm);
            this.longForm = Optional.of(longForm);
        }

        public String getShortForm() {
            return shortForm.orElse("<unknown>");
        }

        public String getLongForm() {
            return longForm.orElse("<unknown>");
        }
    }

    private static final long serialVersionUID = 238066231055021678L;

    private DisplayName originDisplayName = new DisplayName();

    private String executionIdentifier;

    private String instanceName;

    private WorkflowDescription workflowDescription;

    private WorkflowExecutionHandle workflowHandle;

    private Map<WorkflowNodeIdentifier, ComponentExecutionIdentifier> componentExecutionIdentifiers;

    private LogicalNodeId nodeIdentifierStartedExecution;

    private String additionalInformation;

    public WorkflowExecutionContextImpl(String executionIdentifier, WorkflowDescription workflowDescription) {
        this.executionIdentifier = executionIdentifier;
        this.workflowDescription = workflowDescription;

        if (workflowDescription == null) {
            return;
        }

        this.workflowHandle = new ExecutionHandleImpl(executionIdentifier, workflowDescription.getControllerNode());
        componentExecutionIdentifiers = new HashMap<>();
        for (WorkflowNode wfNode : workflowDescription.getWorkflowNodes()) {
            ComponentExecutionIdentifier compExeId = new ComponentExecutionIdentifier(UUID.randomUUID().toString());
            componentExecutionIdentifiers.put(wfNode.getIdentifierAsObject(), compExeId);
        }
    }

    @Override
    public String getExecutionIdentifier() {
        return executionIdentifier;
    }

    @Override
    public String getInstanceName() {
        return instanceName;
    }

    @Override
    public LogicalNodeId getNodeId() {
        return workflowDescription.getControllerNode();
    }

    @Override
    public WorkflowExecutionHandle getWorkflowExecutionHandle() {
        return workflowHandle;
    }

    @Override
    public LogicalNodeId getStorageNodeId() {
        return workflowDescription.getControllerNode();
    }

    @Override
    public NetworkDestination getStorageNetworkDestination() {
        // important: this is only correct as long as this is used on the workflow node itself and it is also the storage node!
        return workflowDescription.getControllerNode();
    }

    @Override
    public WorkflowDescription getWorkflowDescription() {
        return workflowDescription;
    }

    @Override
    public ComponentExecutionIdentifier getCompExeIdByWfNode(WorkflowNode wfNode) {
        return componentExecutionIdentifiers.get(wfNode.getIdentifierAsObject());
    }

    @Override
    public LogicalNodeId getNodeIdStartedExecution() {
        return nodeIdentifierStartedExecution;
    }

    @Override
    public String getAdditionalInformationProvidedAtStart() {
        return additionalInformation;
    }

    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }

    @Override
    public void setNodeIdentifierStartedExecution(LogicalNodeId nodeIdentifier) {
        this.nodeIdentifierStartedExecution = nodeIdentifier;
    }

    public void setAdditionalInformationProvidedAtStart(String additionalInformationProvidedAtStart) {
        this.additionalInformation = additionalInformationProvidedAtStart;
    }

    @Override
    public ServiceCallContext getServiceCallContext() {
        return null; // implement this once needed
    }

    @Override
    public String getWorkflowOriginDisplayName() {
        return this.workflowDescription.getFileName();
    }

    @Override
    public WorkflowState waitForTermination() throws InterruptedException {
        throw new NotImplementedException();
    }

    @Override
    public void reportFinalWorkflowState(WorkflowState finalState) {
        // Do nothing
    }

    @Override
    public void closeResourcesQuietly() {
        // Do nothing, since there are no resources to be closed
    }

    @Override
    public File getWorkflowFile() {
        throw new NotImplementedException();
    }

    @Override
    public File[] getLogFiles() {
        throw new NotImplementedException();
    }

    @Override
    public synchronized long getExecutionDuration() {
        throw new NotImplementedException();
    }

    @Override
    public File getLogDirectory() {
        throw new NotImplementedException();
    }

    @Override
    public void beforeExecutionStarted(DistributedNotificationService notificationServiceParam, LogicalNodeId localNodeId)
        throws WorkflowExecutionException {
        // Do nothing
    }

    @Override
    public void afterExecutionStarted() {
        // Do nothing
    }

    @Override
    public void afterExecutionTerminated(DistributedNotificationService notificationService,
        WorkflowExecutionService workflowExecutionService, boolean behavedAsExpected) {
        // Do nothing
    }

    protected String getLongOriginDisplayName() {
        return originDisplayName.getLongForm();
    }

    protected String getShortOriginDisplayName() {
        return originDisplayName.getShortForm();
    }

    public void setOriginDisplayName(String shortName, String longName) {
        this.originDisplayName = new DisplayName(shortName, longName);
    }
}
