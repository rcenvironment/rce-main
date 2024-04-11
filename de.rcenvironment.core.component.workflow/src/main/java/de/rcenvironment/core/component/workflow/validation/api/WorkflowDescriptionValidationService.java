/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.component.workflow.validation.api;

import de.rcenvironment.core.component.workflow.execution.api.WorkflowDescriptionValidationResult;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;

/**
 * @author Alexander Weinert
 */
public interface WorkflowDescriptionValidationService {

    WorkflowDescriptionValidationResult validateAvailabilityOfNodesAndComponentsFromLocalKnowledge(
        WorkflowDescription workflowDescription);

}
