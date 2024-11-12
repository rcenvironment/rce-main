/*
 * Copyright 2006-2023 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.execution.internal;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import de.rcenvironment.core.communication.api.CommunicationService;
import de.rcenvironment.core.communication.api.PlatformService;
import de.rcenvironment.core.communication.common.CommunicationException;
import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.communication.common.ResolvableNodeId;
import de.rcenvironment.core.communication.management.WorkflowHostService;
import de.rcenvironment.core.component.execution.api.ExecutionControllerException;
import de.rcenvironment.core.component.workflow.api.WorkflowConstants;
import de.rcenvironment.core.component.workflow.execution.api.ExecutionAuthorizationTokenService;
import de.rcenvironment.core.component.workflow.execution.api.FinalWorkflowState;
import de.rcenvironment.core.component.workflow.execution.api.RemotableWorkflowExecutionControllerService;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionContext;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionException;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionHandle;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionInformation;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionService;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowState;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.datamanagement.MetaDataService;
import de.rcenvironment.core.eventlog.api.EventLog;
import de.rcenvironment.core.eventlog.api.EventLogConstants;
import de.rcenvironment.core.eventlog.api.EventLogEntry;
import de.rcenvironment.core.eventlog.api.EventType;
import de.rcenvironment.core.notification.DistributedNotificationService;
import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;

/**
 * Implementation of {@link WorkflowExecutionService}.
 * 
 * @author Doreen Seider
 * @author Robert Mischke
 * @author Kathrin Schaffert (little refactoring)
 */
@Component
public class WorkflowExecutionServiceImpl implements WorkflowExecutionService {

    static final String ERROR_MESSAGE_COMPONENT_VISIBILITY_FAILURE =
        "Failed to query the selected workflow controller about component visibility: ";

    /** Exception Message. */
    static final String ERROR_MESSAGE_FAILED_TO_DETERMINE_STORAGE_ID = "Failed to determine the storage id of workflow run ";

    /** Exception Message. */
    static final String ERROR_MESSAGE_FAILED_TO_GET_WF_EXEC_INFO = "Failed to get local workflow execution information; cause: ";

    /** Exception Message. */
    static final String ERROR_MESSAGE_COULD_NOT_DELETE_WORKFLOW_RUN = "Could not delete workflow run ";

    /** Exception Message. */
    static final String WORKFLOW_INTERRUPTED_EXCEPTION_MESSAGE =
        "Received interruption signal while waiting for workflow to terminate";

    /** Exception Message. */
    static final String FINAL_STATE_UNKNOWN_EXCEPTION_MESSAGE =
        "Most likely because the connection to the workflow host node was interupted. See logs for more details.";

    /** Exception Message. */
    static final String WF_EXECUTION_FAILURE_EXCEPTION_MESSAGE = "Failed to execute workflow";

    /**
     * The interval (in msec) between the "heartbeat" notifications sent for active workflows. Workflows are considered active when they are
     * running or paused, or in the transitional states in-between.
     */
    private static final int ACTIVE_WORKFLOW_HEARTBEAT_NOTIFICATION_INTERVAL_MSEC = 6 * 1000;

    // !Removed final modifier for testing purposes only!
    // The modifier of this variable was changed to add the setLog() method to ensure good test coverage for a major overhaul of the
    // Workflow Engine. After this work is completed, the variable should be changed back to private final and the setLog method must be
    // removed.
    // Dec. 2023, Kathrin Schaffert
    private WorkflowExecutionServiceLog log = new WorkflowExecutionServiceLog();

    private PlatformService platformService;

    private DistributedNotificationService notificationService;

    private CommunicationService communicationService;

    private WorkflowHostService workflowHostService;

    private RemotableWorkflowExecutionControllerService wfExeCtrlService;

    private MetaDataService metaDataService;

    private ExecutionAuthorizationTokenService authorizationTokenService;

    private WorkflowExecutionInformationCache workflowExecutionInformationCache = new WorkflowExecutionInformationCache(
        () -> workflowHostService.getWorkflowHostNodesAndSelf(),
        node -> communicationService.getRemotableService(RemotableWorkflowExecutionControllerService.class, node),
        log);

    private ScheduledFuture<?> heartbeatSendFuture;

    @Activate
    protected void activate(BundleContext context) {

        heartbeatSendFuture =
            ConcurrencyUtils.getAsyncTaskService().scheduleAtFixedInterval("Send heartbeat for active workflows",
                this::sendHeartbeatForActiveWorkflows, ACTIVE_WORKFLOW_HEARTBEAT_NOTIFICATION_INTERVAL_MSEC);
    }

    @Deactivate
    protected void deactivate() {
        if (heartbeatSendFuture != null) {
            heartbeatSendFuture.cancel(true);
        }
    }

    @Override
    public Map<String, String> validateRemoteWorkflowControllerVisibilityOfComponents(WorkflowDescription wfDescription) {
        List<String> componentRefs = new ArrayList<>();
        for (WorkflowNode wfDescNode : wfDescription.getWorkflowNodes()) {
            LogicalNodeId compLocation = wfDescNode.getComponentDescription().getNode();
            if (platformService.matchesLocalInstance(compLocation)) {
                // components on the initiating instance do not need to be checked as they grant the controller special access -- misc_ro
                continue;
            }
            componentRefs.add(StringUtils.escapeAndConcat(
                // 0: component id (for display grouping)
                wfDescNode.getIdentifierAsObject().toString(),
                // 1: component id and version
                wfDescNode.getComponentIdentifierWithVersion(),
                // 2: location (node id)
                compLocation.getLogicalNodeIdString()));
        }
        RemotableWorkflowExecutionControllerService remoteWFExecControllerService =
            communicationService.getRemotableService(RemotableWorkflowExecutionControllerService.class, wfDescription.getControllerNode());

        return remoteWFExecControllerService.verifyComponentVisibility(componentRefs);
    }

    private WorkflowExecutionInformation startWorkflowExecutionInternal(WorkflowExecutionContext wfExeCtx)
        throws RemoteOperationException, WorkflowExecutionException {
        WorkflowExecutionInformation workflowExecutionInformation = createExecutionController(wfExeCtx);
        try {
            performStartOnExecutionController(workflowExecutionInformation.getWorkflowExecutionHandle());
        } catch (ExecutionControllerException | RemoteOperationException e) {
            throw new WorkflowExecutionException(WF_EXECUTION_FAILURE_EXCEPTION_MESSAGE, e);
        }
        return workflowExecutionInformation;
    }

    private WorkflowExecutionInformation createExecutionController(WorkflowExecutionContext wfExeCtx) throws RemoteOperationException,
        WorkflowExecutionException {

        Map<String, String> authTokens = authorizationTokenService
            .acquireExecutionAuthorizationTokensForComponents(wfExeCtx.getWorkflowDescription().getWorkflowNodes());

        boolean controllerLocationIsLocalNode = platformService.matchesLocalInstance(wfExeCtx.getNodeId());

        // TODO workflow file; workflow metadata? workflow title?
        EventLogEntry eventLogEntry = EventLog.newEntry(EventType.WORKFLOW_REQUEST_INITIATED)
            .set(EventType.Attributes.WORKFLOW_RUN_ID, wfExeCtx.getExecutionIdentifier())
            .set(EventType.Attributes.WORKFLOW_CONTROLLER_NODE, wfExeCtx.getNodeId().getLogicalNodeIdString())
            .set(EventType.Attributes.WORKFLOW_CONTROLLER_IS_LOCAL_NODE,
                EventLogConstants.trueFalseValueFromBoolean(controllerLocationIsLocalNode));

        final WorkflowExecutionInformation result;
        try {
            result = getExecutionControllerService(wfExeCtx.getNodeId()).createExecutionController(wfExeCtx, authTokens,
                !controllerLocationIsLocalNode);
            // at this point, the workflow was at least successfully initiated; it may still fail on starting, though
            eventLogEntry.set(EventType.Attributes.SUCCESS, EventLogConstants.TRUE_VALUE);
            EventLog.append(eventLogEntry);
        } catch (WorkflowExecutionException | RemoteOperationException e) {
            eventLogEntry.set(EventType.Attributes.SUCCESS, EventLogConstants.FALSE_VALUE);
            EventLog.append(eventLogEntry);
            throw e; // re-throw
        }

        return result;

    }

    private void performStartOnExecutionController(WorkflowExecutionHandle handle) throws ExecutionControllerException,
        RemoteOperationException {
        getExecutionControllerService(handle.getLocation()).performStart(handle.getIdentifier());
    }

    @Override
    public void cancel(WorkflowExecutionHandle handle) throws ExecutionControllerException, RemoteOperationException {
        getExecutionControllerService(handle.getLocation()).performCancel(handle.getIdentifier());
    }

    @Override
    public void pause(WorkflowExecutionHandle handle) throws ExecutionControllerException, RemoteOperationException {
        getExecutionControllerService(handle.getLocation()).performPause(handle.getIdentifier());
    }

    @Override
    public void resume(WorkflowExecutionHandle handle) throws ExecutionControllerException, RemoteOperationException {
        getExecutionControllerService(handle.getLocation()).performResume(handle.getIdentifier());
    }

    @Override
    public void dispose(WorkflowExecutionHandle handle) throws ExecutionControllerException, RemoteOperationException {
        getExecutionControllerService(handle.getLocation()).performDispose(handle.getIdentifier());
    }

    @Override
    public void deleteFromDataManagement(WorkflowExecutionHandle handle) throws ExecutionControllerException {
        Long wfDataManagementId;
        try {
            wfDataManagementId = getWorkflowDataManagementId(handle);
        } catch (ExecutionControllerException | RemoteOperationException e) {
            throw new ExecutionControllerException(ERROR_MESSAGE_FAILED_TO_DETERMINE_STORAGE_ID + handle.getIdentifier(), e);
        }
        try {
            // note: this relies on the current convention that the storage location is always the wf controller's location
            metaDataService.deleteWorkflowRun(wfDataManagementId, handle.getLocation());
        } catch (CommunicationException e) {
            throw new ExecutionControllerException(ERROR_MESSAGE_COULD_NOT_DELETE_WORKFLOW_RUN + wfDataManagementId, e);
        }
    }

    @Override
    public WorkflowState getWorkflowState(WorkflowExecutionHandle handle) throws ExecutionControllerException,
        RemoteOperationException {
        return getExecutionControllerService(handle.getLocation()).getWorkflowState(handle.getIdentifier());
    }

    @Override
    public Long getWorkflowDataManagementId(WorkflowExecutionHandle handle) throws ExecutionControllerException,
        RemoteOperationException {
        return getExecutionControllerService(handle.getLocation()).getWorkflowDataManagementId(handle.getIdentifier());
    }

    @Override
    public Set<WorkflowExecutionInformation> getLocalWorkflowExecutionInformations() {
        try {
            return new HashSet<>(wfExeCtrlService.getWorkflowExecutionInformations());
        } catch (RemoteOperationException e) {
            // should not happen as it is finally a local call and the ExecutionController are directly fetched before
            throw new IllegalStateException(ERROR_MESSAGE_FAILED_TO_GET_WF_EXEC_INFO + e.getMessage());
        }
    }

    @Override
    public Set<WorkflowExecutionInformation> getWorkflowExecutionInformations(boolean forceRefresh) {
        if (forceRefresh) {
            workflowExecutionInformationCache.refreshCache();
        }

        return workflowExecutionInformationCache.getCachedWorkflowExecutionInformations();
    }

    private RemotableWorkflowExecutionControllerService getExecutionControllerService(ResolvableNodeId node) {
        // fetching the service proxy on each call, assuming that it will be cached centrally if necessary
        return communicationService.getRemotableService(RemotableWorkflowExecutionControllerService.class, node);
    }

    // !Visibility "package" for testing purposes only!
    // The visibility of this method has been changed to package to ensure a good test coverage for a major overhaul of the Workflow Engine.
    // After this work is completed, the method should be changed back to private.
    // Dec. 2023, Kathrin Schaffert
    void sendHeartbeatForActiveWorkflows() {
        Set<WorkflowExecutionInformation> wfExeInfoSnapshot = getWorkflowExecutionInformation();
        for (WorkflowExecutionInformation wfExeInfo : wfExeInfoSnapshot) {
            String wfExeId = wfExeInfo.getExecutionIdentifier();
            switch (wfExeInfo.getWorkflowState()) {
            case INIT:
            case PREPARING:
            case STARTING:
            case RUNNING:
            case PAUSING:
            case PAUSED:
            case RESUMING:
            case CANCELING:
            case CANCELING_AFTER_FAILED:
                notificationService.send(WorkflowConstants.STATE_NOTIFICATION_ID + wfExeId, WorkflowState.IS_ALIVE.name());
                log.heartbeatNotificationSent(wfExeInfo, wfExeId);
                break;
            default:
                // do nothing
                break;
            }
        }
    }

    private Set<WorkflowExecutionInformation> getWorkflowExecutionInformation() {
        Set<WorkflowExecutionInformation> wfExeInfoSnapshot = new HashSet<>();
        try {
            wfExeInfoSnapshot.addAll(wfExeCtrlService.getWorkflowExecutionInformations());
        } catch (RemoteOperationException e) {
            log.fetchingLocalWorkflowExecutionInformationFailed(e);
        }
        return wfExeInfoSnapshot;
    }

    @Override
    public FinalWorkflowState waitForWorkflowTermination(WorkflowExecutionContext wfExeContext,
        final WorkflowExecutionCallback callback) throws WorkflowExecutionException {
        FinalWorkflowState finalState = waitForWorkflowExecutionTermination(wfExeContext);
        boolean behavedAsExpected = callback.onExecutionTerminated(
            wfExeContext.getLogFiles(),
            finalState,
            wfExeContext.getExecutionDuration());

        wfExeContext.afterExecutionTerminated(notificationService, this, behavedAsExpected);
        return finalState;
    }

    @Override
    public WorkflowExecutionInformation start(WorkflowExecutionContext wfExeCtx) throws WorkflowExecutionException {

        wfExeCtx.beforeExecutionStarted(notificationService, platformService.getLocalDefaultLogicalNodeId());

        final WorkflowExecutionInformation returnValue;
        try {
            returnValue = startWorkflowExecutionInternal(wfExeCtx);
        } catch (RemoteOperationException e) {
            throw new WorkflowExecutionException(WF_EXECUTION_FAILURE_EXCEPTION_MESSAGE, e);
        }
        log.workflowStarted(wfExeCtx.getWorkflowOriginDisplayName(), returnValue);

        wfExeCtx.afterExecutionStarted();
        return returnValue;
    }

    private FinalWorkflowState waitForWorkflowExecutionTermination(WorkflowExecutionContext executionContext)
        throws WorkflowExecutionException {
        WorkflowState finalState = null; // = unknown
        try {
            finalState = executionContext.waitForTermination();
        } catch (InterruptedException e) {
            throw new WorkflowExecutionException(WORKFLOW_INTERRUPTED_EXCEPTION_MESSAGE);
        }
        executionContext.reportFinalWorkflowState(finalState);
        executionContext.closeResourcesQuietly();
        // map to reduced set of final workflow states (to avoid downstream checking for invalid values)
        switch (finalState) {
        case FINISHED:
            return FinalWorkflowState.FINISHED;
        case CANCELLED:
            return FinalWorkflowState.CANCELLED;
        case FAILED:
            return FinalWorkflowState.FAILED;
        case RESULTS_REJECTED:
            return FinalWorkflowState.RESULTS_REJECTED;
        case UNKNOWN:
            throw new WorkflowExecutionException(StringUtils.format("Final state of '%s' is %s. " + FINAL_STATE_UNKNOWN_EXCEPTION_MESSAGE,
                executionContext.getWorkflowOriginDisplayName(), finalState.getDisplayName()));
        default:
            throw new WorkflowExecutionException(StringUtils.format("Unexpected value '%s' for final state for '%s'",
                finalState.getDisplayName(), executionContext.getWorkflowOriginDisplayName()));
        }
    }

    @Reference
    protected void bindCommunicationService(CommunicationService newService) {
        communicationService = newService;
    }

    @Reference
    protected void bindNotificationService(DistributedNotificationService newService) {
        notificationService = newService;
    }

    @Reference
    protected void bindPlatformService(PlatformService newService) {
        platformService = newService;
    }

    @Reference
    protected void bindWorkflowHostService(WorkflowHostService newService) {
        workflowHostService = newService;
    }

    @Reference
    protected void bindWorkflowExecutionControllerService(RemotableWorkflowExecutionControllerService newService) {
        wfExeCtrlService = newService;
    }

    @Reference
    protected void bindMetaDataService(MetaDataService newService) {
        metaDataService = newService;
    }

    @Reference
    protected void bindExecutionAuthorizationTokenService(ExecutionAuthorizationTokenService newService) {
        authorizationTokenService = newService;
    }

    // !Method was added for testing purposes only!
    // Method was added to ensure good test coverage for a major overhaul of the Workflow Engine.
    // After this work is completed, the method should again be removed.
    // Dec. 2023, Kathrin Schaffert
    void setLog(WorkflowExecutionServiceLog log) {
        this.log = log;
    }

    // !Method was added for testing purposes only!
    // Method was added to ensure good test coverage for a major overhaul of the Workflow Engine.
    // After this work is completed, the method should again be removed.
    // Dec. 2023, Kathrin Schaffert
    void setCache(WorkflowExecutionInformationCache cache) {
        this.workflowExecutionInformationCache = cache;
    }

}
