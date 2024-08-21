/*
 * Copyright 2006-2023 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.validation.impl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import org.easymock.EasyMock;
import org.junit.Test;

import de.rcenvironment.core.communication.api.PlatformService;
import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.communication.management.WorkflowHostService;
import de.rcenvironment.core.component.api.DistributedComponentKnowledgeService;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowDescriptionValidationResult;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;

public class WorkflowDescriptionValidationServiceImplTest {
    
    @Test
    public void validationOfEmptyWorkflowShouldSucceed() {
        WorkflowDescriptionValidationServiceImpl serviceUnderTest = new WorkflowDescriptionValidationServiceImpl();
        
        serviceUnderTest.bindPlatformService(EasyMock.createNiceMock(PlatformService.class));
        serviceUnderTest.bindComponentKnowledgeService(EasyMock.createNiceMock(DistributedComponentKnowledgeService.class));
        final WorkflowHostService workflowHostService = EasyMock.createNiceMock(WorkflowHostService.class);
        EasyMock.expect(workflowHostService.getLogicalWorkflowHostNodesAndSelf()).andStubReturn(new HashSet<>());
        EasyMock.replay(workflowHostService);
        serviceUnderTest.bindWorkflowHostService(workflowHostService);
        
        final WorkflowDescription description = new WorkflowDescription("identifier");
        
        final WorkflowDescriptionValidationResult result = serviceUnderTest.validateAvailabilityOfNodesAndComponentsFromLocalKnowledge(description);
        
        assertTrue(result.isSucceeded());
    }
    
    @Test
    public void validationWithMissingControllerNodeShouldFail() {
        WorkflowDescriptionValidationServiceImpl serviceUnderTest = new WorkflowDescriptionValidationServiceImpl();
        
        serviceUnderTest.bindPlatformService(EasyMock.createNiceMock(PlatformService.class));
        serviceUnderTest.bindComponentKnowledgeService(EasyMock.createNiceMock(DistributedComponentKnowledgeService.class));
        final WorkflowHostService workflowHostService = EasyMock.createNiceMock(WorkflowHostService.class);
        EasyMock.expect(workflowHostService.getLogicalWorkflowHostNodesAndSelf()).andStubReturn(new HashSet<>());
        EasyMock.replay(workflowHostService);
        serviceUnderTest.bindWorkflowHostService(workflowHostService);
        
        final WorkflowDescription description = new WorkflowDescription("identifier");
        description.setControllerNode(createNodeId());
        
        final WorkflowDescriptionValidationResult result = serviceUnderTest.validateAvailabilityOfNodesAndComponentsFromLocalKnowledge(description);
        
        assertFalse(result.isSucceeded());
    }
    
    @Test
    public void validationWithControllerNodePresentShouldSucceed() {
        WorkflowDescriptionValidationServiceImpl serviceUnderTest = new WorkflowDescriptionValidationServiceImpl();
        final LogicalNodeId controllerNode = createNodeId();
        
        serviceUnderTest.bindPlatformService(EasyMock.createNiceMock(PlatformService.class));
        serviceUnderTest.bindComponentKnowledgeService(EasyMock.createNiceMock(DistributedComponentKnowledgeService.class));
        final WorkflowHostService workflowHostService = EasyMock.createNiceMock(WorkflowHostService.class);
        EasyMock.expect(workflowHostService.getLogicalWorkflowHostNodesAndSelf())
            .andStubReturn(Collections.unmodifiableSet(new HashSet<>(Arrays.asList(controllerNode))));
        EasyMock.replay(workflowHostService);
        serviceUnderTest.bindWorkflowHostService(workflowHostService);
        
        final WorkflowDescription description = new WorkflowDescription("identifier");
        description.setControllerNode(controllerNode);
        
        final WorkflowDescriptionValidationResult result = serviceUnderTest.validateAvailabilityOfNodesAndComponentsFromLocalKnowledge(description);
        
        assertTrue(result.isSucceeded());
    }
    
    private static LogicalNodeId createNodeId() {
        final LogicalNodeId returnValue = EasyMock.createNiceMock(LogicalNodeId.class);
        EasyMock.replay(returnValue);
        return returnValue;
    }

}
