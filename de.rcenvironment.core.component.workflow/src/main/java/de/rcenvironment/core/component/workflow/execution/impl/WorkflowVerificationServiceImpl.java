/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.component.workflow.execution.impl;

import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import de.rcenvironment.core.component.workflow.execution.api.PersistentWorkflowDescriptionLoaderService;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowDescriptionValidationResult;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionService;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionUtils;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowFileException;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowFileLoaderFacade;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowVerificationBuilder;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowVerificationService;
import de.rcenvironment.core.component.workflow.execution.headless.api.HeadlessWorkflowDescriptionLoaderCallback;
import de.rcenvironment.core.component.workflow.execution.internal.WorkflowVerification;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;
import de.rcenvironment.core.component.workflow.validation.api.WorkflowDescriptionValidationService;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.textstream.TextOutputReceiver;
import de.rcenvironment.core.utils.incubator.ServiceRegistry;


@Component
public class WorkflowVerificationServiceImpl implements WorkflowVerificationService {

    private static final int MAXIMUM_WORKFLOW_PARSE_RETRIES = 5;

    private static final int PARSING_WORKFLOW_FILE_RETRY_INTERVAL = 2000;

    private WorkflowExecutionService workflowExecutionService;

    private final Log log = LogFactory.getLog(getClass());

    private WorkflowFileLoaderFacade workflowFileLoaderFacade;

	private WorkflowDescriptionValidationService workflowValidationService;

    /**
     * Note: retry mechanism applied to ensure that all available components and their updaters are registered (both remote and local); it
     * slows down the execution as parsing the workflow file is done at least twice: when pre-validating workflow and for actual execution
     * -> should be improved (https://mantis.sc.dlr.de/view.php?id=14716).
     * 
     * @param outputReceiver Sink for text output
     * @param wfFile workflow to validate
     * @param printNonErrorProgressMessages
     * @return <code>true</code> if workflow is valid, otherwise <code>false</code>
     */
    @Override
    public boolean preValidateWorkflow(TextOutputReceiver outputReceiver, File wfFile, boolean printNonErrorProgressMessages) {
        WorkflowDescription workflowDescription;
        int retries = 0;
        while (true) {
            try {
                workflowDescription = ServiceRegistry.createAccessFor(this)
                    .getService(PersistentWorkflowDescriptionLoaderService.class)
                    .loadWorkflowDescriptionFromFileConsideringUpdates(wfFile,
                        new HeadlessWorkflowDescriptionLoaderCallback(outputReceiver));
            } catch (WorkflowFileException e) {
                log.error("Exception while parsing the workflow file " + wfFile.getAbsolutePath(), e);
                outputReceiver.addOutput(StringUtils.format("Error when parsing '%s': %s (full path: %s)", wfFile.getName(), e.getMessage(),
                    wfFile.getAbsolutePath()));
                return false;
            }
            if (WorkflowExecutionUtils.hasMissingWorkflowNode(workflowDescription.getWorkflowNodes())) {
                if (retries >= MAXIMUM_WORKFLOW_PARSE_RETRIES) {
                    log.debug(StringUtils.format("Maximum number of retries (%d) reached while validating the workflow file '%s'",
                        MAXIMUM_WORKFLOW_PARSE_RETRIES, wfFile.getAbsolutePath()));
                    outputReceiver.addOutput(StringUtils.format("Workflow component(s) of '%s' unknown (full path: %s)",
                        wfFile.getName(), wfFile.getAbsolutePath()));
                    return false;
                }
                retries = waitForWorkflowValidationRetry(retries);
                continue;
            } else {
                if (printNonErrorProgressMessages) {
                    outputReceiver.addOutput(
                        StringUtils.format("Validating target instances of '%s' (full path: %s)", wfFile.getName(),
                            wfFile.getAbsolutePath()));
                }
                WorkflowDescriptionValidationResult validationResult =
                		workflowValidationService.validateAvailabilityOfNodesAndComponentsFromLocalKnowledge(workflowDescription);
                if (validationResult.isSucceeded()) {
                    if (printNonErrorProgressMessages) {
                        outputReceiver.addOutput(
                            StringUtils.format("Target instance(s) of '%s' validated successfully (full path: %s)", wfFile.getName(),
                                wfFile.getAbsolutePath()));
                    }
                    break;
                } else {
                    if (retries >= MAXIMUM_WORKFLOW_PARSE_RETRIES) {
                        log.debug(StringUtils.format("Maximum number of retries (%d) reached while validating the "
                            + "target instances of workflow file '%s'", MAXIMUM_WORKFLOW_PARSE_RETRIES, wfFile.getAbsolutePath()));
                        outputReceiver.addOutput(StringUtils.format("Some target instance(s) of '%s' unknown: %s (full path: %s)",
                            wfFile.getName(), validationResult.toString(), wfFile.getAbsolutePath()));
                        return false;
                    }
                    retries = waitForWorkflowValidationRetry(retries);
                    continue;
                }
            }
        }
        return true;
    }

    private int waitForWorkflowValidationRetry(int retries) {
        log.debug("Retrying workflow validation in a few seconds");
        try {
            Thread.sleep(PARSING_WORKFLOW_FILE_RETRY_INTERVAL);
        } catch (InterruptedException e) {
            log.error("Interrupted while waiting for parsing retry", e);
        }
        return ++retries;
    }

    @Override
    public WorkflowVerificationBuilder getVerificationBuilder() {
        return new WorkflowVerification.Builder()
            .workflowVerificationService(this)
            .workflowExecutionService(this.workflowExecutionService)
            .workflowFileLoaderFacade(this.workflowFileLoaderFacade);
    }

    @Reference
    public void bindWorkflowExecutionService(final WorkflowExecutionService service) {
        this.workflowExecutionService = service;
    }
    
    @Reference
    public void bindWorkflowValidationService(final WorkflowDescriptionValidationService service) {
    	this.workflowValidationService = service;
    }
    
    @Reference
    public void bindWorkflowFileLoaderFacade(final WorkflowFileLoaderFacade service) {
        this.workflowFileLoaderFacade = service;
    }
}
