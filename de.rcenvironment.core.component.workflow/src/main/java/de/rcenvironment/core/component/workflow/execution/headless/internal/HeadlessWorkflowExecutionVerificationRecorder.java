/*
 * Copyright 2006-2023 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.execution.headless.internal;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import de.rcenvironment.core.component.workflow.execution.api.FinalWorkflowState;

/**
 * Used to record results if workflows are executed and verified.
 * 
 * @author Doreen Seider
 * @author Hendrik Abbenhaus
 */
public interface HeadlessWorkflowExecutionVerificationRecorder {

    public static class WorkflowExecutionResult {

        final File wfFile;

        final File[] wfLogFiles;

        final FinalWorkflowState finalState;

        final long executionDuration;

        public WorkflowExecutionResult(File wfFile, File[] wfLogFiles, FinalWorkflowState finalState, long executionDuration) {
            this.wfFile = wfFile;
            this.wfLogFiles = wfLogFiles;
            this.finalState = finalState;
            this.executionDuration = executionDuration;
        }
    }

    /**
     * Announces an error for the given workflow.
     * 
     * @param wfFile workflow file that causes the error
     * @param errorMessage message related to the error occurred
     */
    void addWorkflowError(File wfFile, String errorMessage);

    /**
     * Announces a {@link FinalWorkflowState} for the given workflow file.
     * 
     * @param wfFile workflow file with given {@link FinalWorkflowState}
     * @param wfLogFiles workflow log of the execution
     * @param finalState {@link FinalWorkflowState} of the given workflow file
     * @param executionDuration duration of the workflow execution
     * @throws IOException thrown if accessing the workflow log file failed
     */
    boolean addWorkflowExecutionResult(File wfFile, File[] wfLogFiles, FinalWorkflowState finalState, long executionDuration)
        throws IOException;

    /**
     * Sets start- and end-time of the current job to calculate duration and total time in verification report.
     * 
     * @param start starttime of this job
     * @param end endtime of this job
     */
    void setStartAndEndTime(Date start, Date end);

}
