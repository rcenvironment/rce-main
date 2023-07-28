/*
 * Copyright 2006-2023 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.execution.api;

import java.io.File;
import java.util.UUID;

import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.component.execution.api.SingleConsoleRowsProcessor;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionContext.DeletionBehavior;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionContext.DisposalBehavior;
import de.rcenvironment.core.component.workflow.execution.headless.internal.HeadlessWorkflowExecutionContextImpl;
import de.rcenvironment.core.component.workflow.execution.impl.WorkflowExecutionContextImpl;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;
import de.rcenvironment.core.utils.common.CrossPlatformFilenameUtils;
import de.rcenvironment.core.utils.common.InvalidFilenameException;
import de.rcenvironment.core.utils.common.textstream.TextOutputReceiver;

/**
 * Creates {@link WorkflowExecutionContext} objects.
 * 
 * @author Doreen Seider
 */
public class WorkflowExecutionContextBuilder {

    private WorkflowExecutionContextImpl wfExeCtx;

    private HeadlessWorkflowExecutionContextImpl headlessWfExeCtx;

    /**
     * @param wfFile workflow file
     * @param logDirectory set the location for workflow log files
     * @throws InvalidFilenameException
     */
    protected WorkflowExecutionContextBuilder(WorkflowDescription description) {
        wfExeCtx = new WorkflowExecutionContextImpl(UUID.randomUUID().toString(), description);
        headlessWfExeCtx = new HeadlessWorkflowExecutionContextImpl(UUID.randomUUID().toString(), description);
    }

    public static WorkflowExecutionContextBuilder createContextBuilder(WorkflowDescription workflowDescription) {
        return new WorkflowExecutionContextBuilder(workflowDescription);
    }

    public WorkflowExecutionContextBuilder setOriginDisplayName(String shortName, String longName) {
        headlessWfExeCtx.setOriginDisplayName(shortName, longName);
        return this;
    }

    /**
     * @param instanceName name of the workflow executed
     * @return {@link WorkflowExecutionContextBuilder} instance for method chaining
     */
    public WorkflowExecutionContextBuilder setInstanceName(String instanceName) {
        wfExeCtx.setInstanceName(instanceName);
        return this;
    }

    /**
     * @param nodeIdentifier {@link InstanceNodeSessionId} of node the workflow was started
     * @return {@link WorkflowExecutionContextBuilder} instance for method chaining
     */
    public WorkflowExecutionContextBuilder setNodeIdentifierStartedExecution(LogicalNodeId nodeIdentifier) {
        wfExeCtx.setNodeIdentifierStartedExecution(nodeIdentifier);
        return this;
    }

    /**
     * @param additionalInformation additional information provided at start
     * @return {@link WorkflowExecutionContextBuilder} instance for method chaining
     */
    public WorkflowExecutionContextBuilder setAdditionalInformationProvidedAtStart(String additionalInformation) {
        wfExeCtx.setAdditionalInformationProvidedAtStart(additionalInformation);
        return this;
    }

    public WorkflowExecutionContextBuilder setLogDirectory(File logDirectory) {
        headlessWfExeCtx.setLogDirectory(logDirectory);
        return this;
    }

    /**
     * @param outputReceiver an optional {@link TextOutputReceiver} to write status messages to
     * @return {@link HeadlessWorkflowExecutionContextBuilder} to support method chaining
     */
    public WorkflowExecutionContextBuilder setTextOutputReceiver(TextOutputReceiver outputReceiver) {
        headlessWfExeCtx.setTextOutputReceiver(outputReceiver);
        return this;
    }

    /**
     * @param outputReceiver an optional {@link TextOutputReceiver} to write status messages to
     * @param isCompactOutput <code>true</code> if output should be compact. Default is <code>false</code>
     * @return {@link HeadlessWorkflowExecutionContextBuilder} to support method chaining
     */
    public WorkflowExecutionContextBuilder setTextOutputReceiver(TextOutputReceiver outputReceiver, boolean isCompactOutput) {
        headlessWfExeCtx.setTextOutputReceiver(outputReceiver);
        headlessWfExeCtx.setIsCompactOutput(isCompactOutput);
        return this;
    }

    /**
     * @param consoleRowsProcessor an optional listener for all received ConsoleRows
     * @return {@link HeadlessWorkflowExecutionContextBuilder} to support method chaining
     */
    public WorkflowExecutionContextBuilder setSingleConsoleRowsProcessor(SingleConsoleRowsProcessor consoleRowsProcessor) {
        headlessWfExeCtx.setSingleConsoleRowsProcessor(consoleRowsProcessor);
        return this;
    }

    /**
     * Only used for headless contexts.
     * @param disposalBehavior the {@link WorkflowExecutionContext.DisposalBehavior}. Default is {@link DisposalBehavior#OnFinished}
     * @return {@link HeadlessWorkflowExecutionContextBuilder} to support method chaining
     */
    public WorkflowExecutionContextBuilder setDisposalBehavior(WorkflowExecutionContext.DisposalBehavior disposalBehavior) {
        headlessWfExeCtx.setDisposeBehavior(disposalBehavior);
        return this;
    }

    /**
     * @param delete the {@link WorkflowExecutionContext.DeletionBehavior}. Default is {@link DeletionBehavior#OnFinished}
     * @return {@link HeadlessWorkflowExecutionContextBuilder} to support method chaining
     */
    public WorkflowExecutionContextBuilder setDeletionBehavior(WorkflowExecutionContext.DeletionBehavior delete) {
        headlessWfExeCtx.setDeletionBehavior(delete);
        return this;
    }

    /**
     * @return {@link WorkflowExecutionContext} instance
     */
    public WorkflowExecutionContext build() {
        return wfExeCtx;
    }

    /**
     * @return An {@link ExtendedHeadlessWorkflowExecutionContext} as configured by preceding method calls
     */
    public WorkflowExecutionContext buildHeadless() {
        return headlessWfExeCtx;
    }

}
