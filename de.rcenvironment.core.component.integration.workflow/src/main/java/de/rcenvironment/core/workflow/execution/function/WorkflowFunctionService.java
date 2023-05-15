/*
 * Copyright 2020-2023 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.workflow.execution.function;


public interface WorkflowFunctionService {
    WorkflowFunction.Builder createBuilder();
}
