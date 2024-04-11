/*
 * Copyright 2023 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.execution.internal;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.hamcrest.Matcher;

import de.rcenvironment.core.communication.api.CommunicationService;
import de.rcenvironment.core.communication.api.PlatformService;
import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.communication.common.ResolvableNodeId;
import de.rcenvironment.core.component.workflow.execution.api.ExecutionAuthorizationTokenService;
import de.rcenvironment.core.component.workflow.execution.api.RemotableWorkflowExecutionControllerService;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionException;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.datamanagement.MetaDataService;
import de.rcenvironment.core.notification.DistributedNotificationService;
import junit.framework.AssertionFailedError;

/**
 * Abstract Test Builder for {@link WorkflowExecutionServiceImpl}.
 * 
 * @author Kathrin Schaffert
 */
abstract class WorkflowExecutionServiceImplTestBuilder {

    protected final WorkflowExecutionServiceImpl service = new WorkflowExecutionServiceImpl();

    protected final DistributedNotificationService notificationService = EasyMock.createMock(DistributedNotificationService.class);

    protected final PlatformService platformService = EasyMock.createNiceMock(PlatformService.class);

    protected final CommunicationService communicationService = EasyMock.createMock(CommunicationService.class);

    protected final ExecutionAuthorizationTokenService authorizationTokenService =
        EasyMock.createMock(ExecutionAuthorizationTokenService.class);

    protected final MetaDataService metaDataService = EasyMock.createMock(MetaDataService.class);

    protected final Map<ResolvableNodeId, RemotableWorkflowExecutionControllerService> controllerServices = new HashMap<>();

    public WorkflowExecutionServiceImpl build() {
        service.bindNotificationService(notificationService);
        service.bindExecutionAuthorizationTokenService(authorizationTokenService);
        service.bindPlatformService(platformService);
        service.bindCommunicationService(communicationService);
        service.bindMetaDataService(metaDataService);

        replayAllServices();

        return service;
    }

    protected abstract void replayAllServices();

    WorkflowExecutionServiceImplTestBuilder withLocalNodeId(LogicalNodeId localNodeId) {
        EasyMock.expect(platformService.getLocalDefaultLogicalNodeId()).andStubReturn(localNodeId);

        final Capture<ResolvableNodeId> matchesLocalInstanceArgument = Capture.newInstance(CaptureType.LAST);
        EasyMock
            .expect(platformService.matchesLocalInstance(EasyMock.capture(matchesLocalInstanceArgument)))
            .andStubAnswer(() -> matchesLocalInstanceArgument.getValue().equals(localNodeId));

        return this;
    }

    public WorkflowExecutionServiceImplTestBuilder expectAuthorizationTokenAcquisition(Matcher<?> expectedNodes,
        Map<String, String> authTokens) {
        final Capture<Collection<WorkflowNode>> parameterMatcher = Capture.newInstance(CaptureType.LAST);
        try {
            EasyMock.expect(authorizationTokenService.acquireExecutionAuthorizationTokensForComponents(EasyMock.capture(parameterMatcher)))
                .andStubAnswer(() -> {
                    if (!expectedNodes.matches(parameterMatcher.getValue())) {
                        throw new AssertionFailedError();
                    }
                    return authTokens;
                });
        } catch (WorkflowExecutionException e) {
            // Will never happen, since we call this method only on a mock
        }

        return this;
    }

}
