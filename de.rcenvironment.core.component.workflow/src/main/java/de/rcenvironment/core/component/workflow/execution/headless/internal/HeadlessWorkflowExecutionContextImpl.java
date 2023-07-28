/*
 * Copyright 2006-2023 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.execution.headless.internal;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.component.execution.api.ConsoleRow;
import de.rcenvironment.core.component.execution.api.ConsoleRowUtils;
import de.rcenvironment.core.component.execution.api.ExecutionControllerException;
import de.rcenvironment.core.component.execution.api.SingleConsoleRowsProcessor;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionContext;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionException;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionService;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowState;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowStateNotificationSubscriber;
import de.rcenvironment.core.component.workflow.execution.headless.api.LoggingConsoleRowSubscriber;
import de.rcenvironment.core.component.workflow.execution.impl.WorkflowExecutionContextImpl;
import de.rcenvironment.core.component.workflow.execution.spi.SingleWorkflowStateChangeListener;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;
import de.rcenvironment.core.notification.DistributedNotificationService;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;
import de.rcenvironment.core.utils.common.textstream.TextOutputReceiver;

/**
 * Implementation of {@link WorkflowExecutionContext} for headless workflow executions.
 * 
 * @author Doreen Seider
 * @author Robert Mischke (minor change)
 */
public class HeadlessWorkflowExecutionContextImpl
    extends WorkflowExecutionContextImpl
    implements LoggingConsoleRowSubscriber.Observer, WorkflowResourceCloser {
    
    private File logDirectory;

    private TextOutputReceiver textOutputReceiver;

    private SingleConsoleRowsProcessor singleConsoleRowsProcessor;

    private WorkflowExecutionContext.DisposalBehavior disposeBehavior = WorkflowExecutionContext.DisposalBehavior.OnExpected;

    private WorkflowExecutionContext.DeletionBehavior deletionBehavior = WorkflowExecutionContext.DeletionBehavior.OnExpected;

    private boolean isCompactOutput = false;

    private final Log log = LogFactory.getLog(getClass());

    private final CountDownLatch workflowFinishedLatch;

    private final CountDownLatch consoleOutputFinishedLatch;

    private final CountDownLatch workflowDisposedLatch;

    private final List<Closeable> resourcesToCloseOnFinish = new ArrayList<>();

    private final List<WorkflowStateNotificationSubscriber.NotificationSubscription> notificationSubscriptionsToUnsubscribeOnFinish = new ArrayList<>();

    private final long startTimestampMillis;

    private long executionDurationMillis;

    private WorkflowState finalState;

    public HeadlessWorkflowExecutionContextImpl(String executionIdentifier, WorkflowDescription description) {
        super(executionIdentifier, description);

        setInstanceName(description.getName());
        if (!StringUtils.isNullorEmpty(description.getAdditionalInformation())) {
            setAdditionalInformationProvidedAtStart(description.getAdditionalInformation());
        }

        // wait for two events: the "end of console output marker", and the workflow reaching a finished state; using two separate latches
        // to ensure a duplicate event cannot count for the other type -misc_ro
        workflowFinishedLatch = new CountDownLatch(1);
        consoleOutputFinishedLatch = new CountDownLatch(1);

        workflowDisposedLatch = new CountDownLatch(1);

        // to keep is simple the actual time the workflow is started is expected to be very close to the instantiation of this class, if a
        // more precise workflow execution time is needed, the time should be set from the workflow execution code right before the actual
        // start -seid_do
        startTimestampMillis = System.currentTimeMillis();
    }

    /**
     * @param consoleRow called if new console row was received, e.g. by the {@link LoggingConsoleRowSubscriber}
     */
    @Override
    public final void reportConsoleRowReceived(ConsoleRow consoleRow) {
        if (singleConsoleRowsProcessor != null) {
            singleConsoleRowsProcessor.onConsoleRow(consoleRow);
        }
    }

    @Override
    public synchronized long getExecutionDuration() {
        return executionDurationMillis;
    }

    /**
     * Reports that all console output was received.
     */
    @Override
    public void reportConsoleOutputTerminated() {
        consoleOutputFinishedLatch.countDown();
    }

    /**
     * Awaits termination.
     * 
     * @return {@link WorkflowState} after termination
     * @throws InterruptedException on error
     */
    @Override
    public WorkflowState waitForTermination() throws InterruptedException {
        // TODO add timeout and workflow heartbeat checking
        workflowFinishedLatch.await();
        consoleOutputFinishedLatch.await();
        synchronized (this) {
            return finalState;
        }
    }

    /**
     * Registers a resource which must be closed if workflow is terminated.
     * 
     * @param resource {@link Closeable} subcriber to register
     */
    @Override
    public synchronized void registerResourceToCloseOnFinish(Closeable resource) {
        resourcesToCloseOnFinish.add(resource);
    }

    /**
     * Registers a NS which must be closed if workflow is terminated.
     * 
     * @param subscriber {@link Closeable} subcriber to register
     */
    @Override
    public synchronized void registerNotificationSubscriptionsToUnsubscribeOnFinish(WorkflowStateNotificationSubscriber.NotificationSubscription subscriber) {
        notificationSubscriptionsToUnsubscribeOnFinish.add(subscriber);
    }

    /**
     * Closes resources added via {@link #registerResourceToCloseOnFinish(Closeable)}.
     */
    @Override
    public void closeResourcesQuietly() {
        for (Closeable resource : getResourcesToCloseOnFinish()) {
            try {
                resource.close();
            } catch (IOException e) {
                log.warn(StringUtils.format("Error closing resource after end of workflow '%s' (%s) ",
                    this.getInstanceName(), this.getExecutionIdentifier()), e);
            }
        }
    }

    private synchronized List<Closeable> getResourcesToCloseOnFinish() {
        return new ArrayList<>(resourcesToCloseOnFinish);
    }

    private void unsubscribeNotificationSubscribersQuietly(DistributedNotificationService notificationService) {
        for (WorkflowStateNotificationSubscriber.NotificationSubscription subscription : getNotificationSubscribersToUnsubscribeOnFinish()) {
            try {
                notificationService.unsubscribe(subscription.notificationId, subscription.subscriber, subscription.nodeId);
            } catch (RemoteOperationException e) {
                log.warn(StringUtils.format("Failed to unsubscribe %s from notification service (workflow '%s' (%s)",
                    subscription.subscriber.getClass(), this.getInstanceName(), this.getExecutionIdentifier()), e);
            }
        }
    }

    private synchronized List<WorkflowStateNotificationSubscriber.NotificationSubscription> getNotificationSubscribersToUnsubscribeOnFinish() {
        return new ArrayList<>(notificationSubscriptionsToUnsubscribeOnFinish);
    }

    /**
     * To be called after the execution of the workflow has started.
     */
    @Override
    public void afterExecutionStarted() {
        super.afterExecutionStarted();
        addOutput(this.getExecutionIdentifier(),
            StringUtils.format("Executing: '%s' (%s); id: %s",
                getShortOriginDisplayName(),
                getLongOriginDisplayName(),
                this.getExecutionIdentifier()));
    }

    @Override
    public void reportFinalWorkflowState(WorkflowState finalState) {
        addOutput(StringUtils.format("%s: %s", this
            .getExecutionIdentifier(), finalState.getDisplayName()), StringUtils.format("%s: '%s' (full path: %s)",
                finalState.getDisplayName(), getShortOriginDisplayName(),
                getLongOriginDisplayName()));
    }

    private void addOutput(String verboseOutput) {
        addOutput(null, verboseOutput);
    }

    private void addOutput(String compactOutput, String verboseOutput) {
        if (this.textOutputReceiver == null) {
            return;
        }
        if (isCompactOutput) {
            if (compactOutput != null && !compactOutput.isEmpty()) {
                this.textOutputReceiver.addOutput(compactOutput);
            }
        } else {
            if (verboseOutput != null && !verboseOutput.isEmpty()) {
                this.textOutputReceiver.addOutput(verboseOutput);
            }
        }
    }

    @Override
    public File getLogDirectory() {
        return logDirectory;
    }

    @Override
    public File[] getLogFiles() {
        return getLogDirectory().listFiles((dir, name) -> name.startsWith("workflow.log"));
    }

    public void setLogDirectory(File logDirectory) {
        this.logDirectory = logDirectory;
    }

    public void setTextOutputReceiver(TextOutputReceiver textOutputReceiver) {
        this.textOutputReceiver = textOutputReceiver;
    }

    public void setSingleConsoleRowsProcessor(SingleConsoleRowsProcessor singleConsoleRowsProcessor) {
        this.singleConsoleRowsProcessor = singleConsoleRowsProcessor;
    }

    public void setDisposeBehavior(WorkflowExecutionContext.DisposalBehavior disposeBehavior) {
        this.disposeBehavior = disposeBehavior;
    }

    public void setDeletionBehavior(WorkflowExecutionContext.DeletionBehavior delete) {
        this.deletionBehavior = delete;
    }

    public void setIsCompactOutput(boolean isCompactOutput) {
        this.isCompactOutput = isCompactOutput;
    }

    @Override
    public WorkflowStateNotificationSubscriber createWorkflowStateChangeListener(String wfExecutionId, Consumer<WorkflowState> logger) {
        return new WorkflowStateNotificationSubscriber(new SingleWorkflowStateChangeListener() {

            @Override
            public void onWorkflowStateChanged(WorkflowState newState) {
                logger.accept(newState);
                switch (newState) {
                case CANCELLED:
                case FAILED:
                case RESULTS_REJECTED:
                case FINISHED:
                    reportWorkflowTerminated(newState);
                    break;
                case DISPOSED:
                    reportWorkflowDisposed(newState);
                    break;
                default:
                    // ignore
                    break; // workaround for CheckStyle bug
                           // (http://sourceforge.net/p/checkstyle/bugs/454/) - misc_ro
                }
            }

            @Override
            public void onWorkflowNotAliveAnymore(String errorMessage) {
                reportWorkflowNotAliveAnymore(errorMessage);
            }

        }, wfExecutionId);
    }

    private synchronized void reportWorkflowTerminated(WorkflowState newState) {
        if (this.finalState != null) {
            log.warn(StringUtils.format("Workflow '%s' (%s) was already marked as %s, new final state: %s (%s)",
                this.getInstanceName(), this.getExecutionIdentifier(),
                finalState.getDisplayName(), newState.getDisplayName(), getLongOriginDisplayName()));
        }
        this.finalState = newState;
        if (finalState != WorkflowState.FINISHED) {
            addOutput(StringUtils.format("'%s' terminated abnormally: %s; check log and console output for details",
                getShortOriginDisplayName(), finalState.getDisplayName()));

        }
        log.debug(StringUtils.format("Workflow '%s' (%s) has terminated, final state: %s (%s)",
            this.getInstanceName(), this.getExecutionIdentifier(),
            finalState.getDisplayName(), getShortOriginDisplayName()));

        executionDurationMillis = System.currentTimeMillis() - startTimestampMillis;
        workflowFinishedLatch.countDown();
    }

    private synchronized void reportWorkflowNotAliveAnymore(String errorMessage) {
        log.error(StringUtils.format("Final state of workflow '%s' (%s) is %s - %s",
            this.getInstanceName(), this.getExecutionIdentifier(),
            WorkflowState.UNKNOWN.getDisplayName(),
            errorMessage));
        finalState = WorkflowState.UNKNOWN;
        workflowFinishedLatch.countDown();
        consoleOutputFinishedLatch.countDown();
    }

    private synchronized void reportWorkflowDisposed(WorkflowState newState) {
        log.debug(StringUtils.format("Workflow '%s' (%s) is done, disposed: %s (%s)",
            this.getInstanceName(), this.getExecutionIdentifier(),
            newState == WorkflowState.DISPOSED, getShortOriginDisplayName()));
        workflowDisposedLatch.countDown();
    }

    private WorkflowStateNotificationSubscriber.NotificationSubscription createSubscriberContext(
        String executionIdentifier, LogicalNodeId nodeId, final Consumer<WorkflowState> logger) {
        final WorkflowStateNotificationSubscriber wfStateChangeListener = createWorkflowStateChangeListener(executionIdentifier, logger);
        return wfStateChangeListener.createSubscriberContext(executionIdentifier, nodeId);
    }

    public LoggingConsoleRowSubscriber createConsoleRowLogger(File logDirectory, final WorkflowExecutionContext workflowExecutionContext) {
        final LoggingConsoleRowSubscriber consoleRowLogger = new LoggingConsoleRowSubscriber(this, logDirectory);
        consoleRowLogger.insertLogFileMetaInformation(workflowExecutionContext);
        return consoleRowLogger;
    }

    @Override
    public String getWorkflowOriginDisplayName() {
        return getLongOriginDisplayName();
    }

    @Override
    public void beforeExecutionStarted(DistributedNotificationService notificationService, LogicalNodeId localNodeId)
        throws WorkflowExecutionException {
        super.beforeExecutionStarted(notificationService, localNodeId);
        setNodeIdentifierStartedExecution(localNodeId);

        try {
            setupLogDirectory();
            subscribeToWorkflowStateChanges(notificationService);
        } catch (WorkflowExecutionException e) {
            onWorkflowExecutionFailed(e, notificationService);
            throw e;
        }

        try {
            subscribeToConsoleRowsOnController(notificationService);
        } catch (RemoteOperationException e) {
            log.error("Failed to subscribe for console row output: " + getWorkflowOriginDisplayName(), e);
        }
    }

    private void setupLogDirectory() throws LogDirectoryCreationFailureException {
        logDirectory.mkdirs();
        if (logDirectory.isDirectory()) {
            log.debug(
                StringUtils.format("Created log file directory '%s' for execution of '%s'",
                    logDirectory.getAbsolutePath(), getLongOriginDisplayName()));
        } else {
            throw new LogDirectoryCreationFailureException(logDirectory.getAbsolutePath(), getLongOriginDisplayName());
        }
    }

    private void subscribeToWorkflowStateChanges(DistributedNotificationService notificationService) throws WorkflowExecutionException {
        final Consumer<WorkflowState> logger =
            newState -> log.debug(StringUtils.format("Received state change event for workflow '%s' (%s): %s",
                this.getInstanceName(), getExecutionIdentifier(), newState));

        try {
            final WorkflowStateNotificationSubscriber.NotificationSubscription subscriberContext =
                createSubscriberContext(getExecutionIdentifier(), this.getNodeId(), logger);
            notificationService.subscribe(subscriberContext.notificationId, subscriberContext.subscriber, subscriberContext.nodeId);
            registerNotificationSubscriptionsToUnsubscribeOnFinish(subscriberContext);
        } catch (RemoteOperationException e) {
            final String errorMessage = "Failed to execute workflow (error while subscribing for state changes)";
            log.error(StringUtils.format("%s: %s", errorMessage, getLongOriginDisplayName()), e);
            throw new WorkflowExecutionException(errorMessage, e);
        }
    }

    private void onWorkflowExecutionFailed(WorkflowExecutionException e, DistributedNotificationService notificationService) {
        final String message = StringUtils.format("Failed: '%s' (%s); %s",
            getShortOriginDisplayName(), getLongOriginDisplayName(), e.getMessage());
        addOutput(message, message);

        closeResourcesQuietly();
        unsubscribeNotificationSubscribersQuietly(notificationService);
    }

    private void subscribeToConsoleRowsOnController(DistributedNotificationService notificationService) throws RemoteOperationException {
        final WorkflowExecutionContext workflowExecutionContext = this;
        final LoggingConsoleRowSubscriber consoleRowLogger = createConsoleRowLogger(logDirectory, workflowExecutionContext);

        // subscribe to a console row notification on workflow controller node
        final LogicalNodeId nodeId = workflowExecutionContext.getNodeId();
        final String workflowExecutionIdentifier = workflowExecutionContext.getExecutionIdentifier();

        final String notificationId = ConsoleRowUtils.composeConsoleNotificationId(nodeId, workflowExecutionIdentifier);
        notificationService.subscribe(notificationId, consoleRowLogger, nodeId);

        registerResourceToCloseOnFinish(consoleRowLogger);
    }

    @Override
    public void afterExecutionTerminated(DistributedNotificationService notificationService,
        WorkflowExecutionService workflowExecutionService, boolean behavedAsExpected) {
        super.afterExecutionTerminated(notificationService, workflowExecutionService, behavedAsExpected);

        disposeOrDeleteWorkflowIfIntended(workflowExecutionService, behavedAsExpected);
        unsubscribeNotificationSubscribersQuietly(notificationService);
    }

    private void disposeOrDeleteWorkflowIfIntended(WorkflowExecutionService executionService, boolean behavedAsExpected) {
        boolean delete = shouldBeDeleted(behavedAsExpected);
        boolean dispose = delete || shouldBeDisposed(behavedAsExpected);
        try {
            if (delete) {
                executionService.deleteFromDataManagement(getWorkflowExecutionHandle());
            }

            if (dispose) {
                executionService.dispose(getWorkflowExecutionHandle());
                waitForDisposal();
            }

            if (delete) {
                deleteLogDirectory();
            }
        } catch (ExecutionControllerException | RemoteOperationException | RuntimeException e) {
            log.error(StringUtils.format("Failed to delete workflow '%s' (%s) ", getInstanceName(), getExecutionIdentifier()), e);
            reportWorkflowDisposed(WorkflowState.FAILED);
        } catch (InterruptedException e) {
            log.error(StringUtils.format("Received interruption signal while waiting for disposeal of workflow '%s' (%s) ",
                getInstanceName(), getExecutionIdentifier()), e);
        }
    }

    private boolean shouldBeDisposed(boolean behavedAsExpected) {
        return disposeBehavior == WorkflowExecutionContext.DisposalBehavior.Always
            || (behavedAsExpected
                && disposeBehavior == WorkflowExecutionContext.DisposalBehavior.OnExpected);
    }

    private boolean shouldBeDeleted(boolean behavedAsExpected) {
        return deletionBehavior == WorkflowExecutionContext.DeletionBehavior.Always
            || (behavedAsExpected
                && deletionBehavior == WorkflowExecutionContext.DeletionBehavior.OnExpected);
    }

    /**
     * Awaits disposal.
     * 
     * @throws InterruptedException on error
     */
    private void waitForDisposal() throws InterruptedException {
        // TODO add timeout and workflow heartbeat checking
        workflowDisposedLatch.await();
    }

    private void deleteLogDirectory() {
        try {
            FileUtils.deleteDirectory(getLogDirectory());
        } catch (IOException e) {
            log.error("Failed to delete log directory: " + getLogDirectory(), e);
        }
    }
}
