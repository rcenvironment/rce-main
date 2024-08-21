/*
 * Copyright 2006-2023 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.execution.internal;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.function.Supplier;

import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.component.workflow.execution.api.RemotableWorkflowExecutionControllerService;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionInformation;
import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;
import de.rcenvironment.toolkit.modules.concurrency.api.CallablesGroup;
import de.rcenvironment.toolkit.modules.concurrency.api.TaskDescription;

class WorkflowExecutionInformationCache {
    
    private Set<WorkflowExecutionInformation> workflowExecutionInformations;
    
    private final Supplier<Iterable<InstanceNodeSessionId>> workflowHostNodesAndSelf;
    
    private final Function<InstanceNodeSessionId, RemotableWorkflowExecutionControllerService> executionControllerServiceSupplier;
    
    private final WorkflowExecutionServiceLog log;

    public WorkflowExecutionInformationCache(
        Supplier<Iterable<InstanceNodeSessionId>> workflowHostNodesAndSelf,
        Function<InstanceNodeSessionId, RemotableWorkflowExecutionControllerService> executionControllerService,
        WorkflowExecutionServiceLog log) {
        this.workflowHostNodesAndSelf = workflowHostNodesAndSelf;
        this.executionControllerServiceSupplier = executionControllerService;
        this.log = log;
    }

    public synchronized void refreshCache() {
        Set<WorkflowExecutionInformation> tempWfExeInfos = new HashSet<>();

        CallablesGroup<Collection> callablesGroup =
            ConcurrencyUtils.getFactory().createCallablesGroup(Collection.class);

        for (InstanceNodeSessionId node : workflowHostNodesAndSelf.get()) {
            final InstanceNodeSessionId finalNode = node;
            callablesGroup.add(new Callable<Collection>() {

                @Override
                @TaskDescription("Distributed query: getWorkflowInformations()")
                public Collection<WorkflowExecutionInformation> call() throws Exception {
                    RemotableWorkflowExecutionControllerService executionControllerService = executionControllerServiceSupplier.apply(node);
                    try {
                        return executionControllerService.getWorkflowExecutionInformations();
                    } catch (RemoteOperationException e) {
                        log.workflowInformationQueryFailed(finalNode, e);
                    }
                    return null;
                }
            });
        }
        List<Collection> results = callablesGroup.executeParallel(log::asynchronousExecutionFailed);

        // merge results
        for (Collection<WorkflowExecutionInformation> singleResult : results) {
            if (singleResult != null) {
                tempWfExeInfos.addAll(singleResult);
            }
        }
        workflowExecutionInformations = tempWfExeInfos;
    }

    public synchronized Set<WorkflowExecutionInformation> getCachedWorkflowExecutionInformations() {
        if (this.workflowExecutionInformations == null) {
            this.refreshCache();
        }

        return new HashSet<>(workflowExecutionInformations);
    }
}