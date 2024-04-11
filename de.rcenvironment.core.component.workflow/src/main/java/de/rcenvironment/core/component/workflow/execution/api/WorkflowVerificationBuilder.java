/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.component.workflow.execution.api;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import de.rcenvironment.core.utils.common.textstream.TextOutputReceiver;

public interface WorkflowVerificationBuilder {
    
    public interface LogFolderFactory {
        File constructLogFolderForWorkflowFile(File workflowFile) throws IOException;
    }


    WorkflowVerificationBuilder workflowRootFile(File workflowRootFile);

    WorkflowVerificationBuilder addWorkflowExpectedToSucceed(File workflowExpectedToSucceed);

    WorkflowVerificationBuilder addWorkflowsExpectedToSucceed(Collection<File> workflowsExpectedToSucceed);

    WorkflowVerificationBuilder addWorkflowExpectedToFail(File workflowExpectedToFail);

    WorkflowVerificationBuilder addWorkflowsExpectedToFail(Collection<File> workflowsExpectedToFail);

    WorkflowVerificationBuilder placeholdersFile(File placeholdersFileParam);

    WorkflowVerificationBuilder logFileFactory(LogFolderFactory logFileFactory);

    WorkflowVerificationBuilder numberOfParallelRuns(int parallelRunsParam);

    WorkflowVerificationBuilder numberOfSequentialRuns(int sequentialRunsParam);

    WorkflowVerificationBuilder disposalBehavior(WorkflowExecutionContext.DisposalBehavior disposeParam);

    WorkflowVerificationBuilder deletionBehavior(WorkflowExecutionContext.DeletionBehavior deleteParam);

    WorkflowVerificationBuilder workflowExecutionService(WorkflowExecutionService service);

    WorkflowVerificationResults verify() throws IOException;

    WorkflowVerificationBuilder outputReceiver(TextOutputReceiver receiver);

    WorkflowVerificationBuilder workflowFileLoaderFacade(WorkflowFileLoaderFacade service);
}
