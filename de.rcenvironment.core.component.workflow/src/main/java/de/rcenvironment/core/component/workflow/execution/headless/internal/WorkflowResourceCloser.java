package de.rcenvironment.core.component.workflow.execution.headless.internal;

import java.io.Closeable;
import java.util.function.Consumer;

import de.rcenvironment.core.component.workflow.execution.api.WorkflowState;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowStateNotificationSubscriber;

/**
 * Interface for classes that hold resources and notification subscriptions that shall be cleaned up after the workflow has terminated and
 * after it has been disposed of.
 * 
 * @author Alexander Weinert
 */
public interface WorkflowResourceCloser {

    void registerResourceToCloseOnFinish(Closeable consoleRowSubscriber);

    void registerNotificationSubscriptionsToUnsubscribeOnFinish(
        WorkflowStateNotificationSubscriber.NotificationSubscription subscriberContext);

    public WorkflowStateNotificationSubscriber createWorkflowStateChangeListener(String wfExecutionId, Consumer<WorkflowState> logger);
}
