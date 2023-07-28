/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.workflow.execution.internal;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import de.rcenvironment.core.authorization.api.AuthorizationAccessGroup;
import de.rcenvironment.core.authorization.api.AuthorizationAccessGroupKeyData;
import de.rcenvironment.core.authorization.api.AuthorizationPermissionSet;
import de.rcenvironment.core.authorization.api.AuthorizationService;
import de.rcenvironment.core.authorization.cryptography.api.CryptographyOperationsProvider;
import de.rcenvironment.core.communication.api.CommunicationService;
import de.rcenvironment.core.communication.api.PlatformService;
import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.component.api.DistributedComponentKnowledge;
import de.rcenvironment.core.component.api.DistributedComponentKnowledgeService;
import de.rcenvironment.core.component.authorization.api.ComponentExecutionAuthorizationService;
import de.rcenvironment.core.component.authorization.api.RemotableComponentExecutionAuthorizationService;
import de.rcenvironment.core.component.management.api.DistributedComponentEntry;
import de.rcenvironment.core.component.model.api.ComponentDescription;
import de.rcenvironment.core.component.workflow.execution.api.ExecutionAuthorizationTokenService;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionException;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.toolkitbridge.transitional.ConcurrencyUtils;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.exception.OperationFailureException;
import de.rcenvironment.core.utils.common.rpc.RemoteOperationException;
import de.rcenvironment.toolkit.modules.concurrency.api.CallablesGroup;
import de.rcenvironment.toolkit.modules.concurrency.api.TaskDescription;

/**
 * 
 * Default implementation of ExecutionAuthorizationTokenService.
 *
 * @author Alexander Weinert (extraction from WorkflowExecutionServiceImpl)
 */
@Component
public class ExecutionAuthorizationTokenServiceImpl implements ExecutionAuthorizationTokenService {

    private DistributedComponentKnowledgeService componentKnowledgeService;

    private PlatformService platformService;

    private ComponentExecutionAuthorizationService componentExecutionAuthorizationService;

    private CommunicationService communicationService;

    private Log log = LogFactory.getLog(getClass());

    private AuthorizationService authorizationService;

    private CryptographyOperationsProvider cryptographyOperationsProvider;

    @Override
    public Map<String, String> acquireExecutionAuthorizationTokensForComponents(Collection<WorkflowNode> workflowNodes)
        throws WorkflowExecutionException {

        final Map<String, String> resultMap = new HashMap<>(); // access must be synchronized by caller
        DistributedComponentKnowledge distrCompKnowledge = componentKnowledgeService.getCurrentSnapshot();

        final CallablesGroup<WorkflowExecutionException> callablesGroup =
            ConcurrencyUtils.getFactory().createCallablesGroup(WorkflowExecutionException.class);
        for (WorkflowNode wfDescriptionNode : workflowNodes) {

            // do not acquire authorization tokens for disabled components
            // note that when redesigning the workflow engine, this sort of exclusion should happen centrally -- misc_ro
            if (!wfDescriptionNode.isEnabled()) {
                continue;
            }

            // TODO convert to lambda after API migration is complete
            // TODO this would really benefit from an "execute with standard exception handling" API
            callablesGroup.add(new Callable<WorkflowExecutionException>() {

                @Override
                @TaskDescription("Acquire access token for component")
                public WorkflowExecutionException call() throws Exception {
                    try {
                        final String accessToken =
                            acquireOrRegisterExecutionAuthorizationToken(wfDescriptionNode, distrCompKnowledge);
                        synchronized (resultMap) {
                            // note: map key kept unchanged from previous code; could be improved/clarified
                            resultMap.put(wfDescriptionNode.getIdentifierAsObject().toString(), accessToken);
                        }
                        return null;
                    } catch (RemoteOperationException | OperationFailureException e) {
                        final String message = "Failed to acquire permission to execute component \"" + wfDescriptionNode.getName()
                            + "\" on " + wfDescriptionNode.getComponentDescription().getNode();
                        // log here immediately (without a stacktrace), as only the first exception will be rethrown
                        log.error(message + ": " + e.toString());
                        return new WorkflowExecutionException(message, e);
                    }
                }
            });

        }
        final List<WorkflowExecutionException> exceptions = callablesGroup.executeParallel(null);
        // rethrow first exception (if any) to abort
        for (WorkflowExecutionException e : exceptions) {
            if (e != null) {
                throw e;
            }
        }
        // synchronized to ensure thread visibility; may be redundant
        synchronized (resultMap) {
            return resultMap;
        }
    }

    private String acquireOrRegisterExecutionAuthorizationToken(WorkflowNode wfDescriptionNode,
        DistributedComponentKnowledge distrCompKnowledge)
        throws RemoteOperationException, OperationFailureException {

        // extract data
        ComponentDescription componentDescription = wfDescriptionNode.getComponentDescription();
        LogicalNodeId compLocation = componentDescription.getNode();
        String compIdWithoutVersion = componentDescription.getComponentInterface().getIdentifier();
        String compVersion = componentDescription.getComponentInterface().getVersion();

        final String accessToken;
        if (compLocation == null || platformService.matchesLocalInstance(compLocation)) {
            // for components on the same instance as the workflow initiator, special access
            // is granted to the (potentially remote) workflow
            // controller; the id parameter is only provided here for usable log output
            accessToken = componentExecutionAuthorizationService.createAndRegisterExecutionTokenForLocalComponent(compIdWithoutVersion);
        } else {
            Optional<DistributedComponentEntry> distrComponentEntryResult =
                resolveComponentIdToDistributedComponentEntry(distrCompKnowledge, compLocation, compIdWithoutVersion);
            if (!distrComponentEntryResult.isPresent()) {
                throw new OperationFailureException("Could not resolve component id " + compIdWithoutVersion
                    + " to an accessible component on instance " + compLocation.getAssociatedDisplayName());
            }

            DistributedComponentEntry distrComponentEntry = distrComponentEntryResult.get();

            AuthorizationPermissionSet matchingPermissionSet = distrComponentEntry.getMatchingPermissionSet();
            log.debug(StringUtils.format("Determined [%s] as the list of available authorization group(s) "
                + "for component '%s' on %s", matchingPermissionSet, compIdWithoutVersion, compLocation));

            RemotableComponentExecutionAuthorizationService remoteService =
                communicationService.getRemotableService(RemotableComponentExecutionAuthorizationService.class, compLocation);
            if (matchingPermissionSet.isPublic()) {
                accessToken = remoteService.requestExecutionTokenForPublicComponent(compIdWithoutVersion, compVersion);
            } else if (!matchingPermissionSet.isLocalOnly()) {
                // arbitrarily choose the first group shared by the local instance and the
                // component host; no obvious criterion to choose by
                AuthorizationAccessGroup sharedAccessGroup = matchingPermissionSet.getAccessGroups().iterator().next();
                // request a token, which is returned encrypted with the group key (as a simple
                // authorization check)
                String encryptedAccessToken = remoteService.requestEncryptedExecutionTokenViaGroupMembership(
                    compIdWithoutVersion, compVersion, sharedAccessGroup.getFullId());
                // attempt to decrypt it
                AuthorizationAccessGroupKeyData groupKeyData = authorizationService.getKeyDataForGroup(sharedAccessGroup);
                accessToken = cryptographyOperationsProvider.decodeAndDecryptString(groupKeyData.getSymmetricKey(), encryptedAccessToken);
                // simple verification: check for a known substring; must match the string
                // composed within the token-generating method
                if (!accessToken.contains(":group:")) {
                    throw new OperationFailureException("Failed to decrypt the component execution token for component "
                        + componentDescription.getName());
                }
            } else {
                throw new OperationFailureException(
                    "Failed to acquire permission to execute component \"" + componentDescription.getName()
                        + "\": There are no shared authorization groups between the local instance "
                        + "and the instance providing the component");
            }
        }
        return accessToken;
    }

    // TODO convert this into a central API method?
    private Optional<DistributedComponentEntry> resolveComponentIdToDistributedComponentEntry(
        DistributedComponentKnowledge distrCompKnowledge, LogicalNodeId compLocation, String compIdWithoutVersion) {
        for (DistributedComponentEntry componentEntry : distrCompKnowledge.getKnownSharedInstallationsOnNode(compLocation, false)) {
            if (componentEntry.getComponentInterface().getIdentifier().equals(compIdWithoutVersion)) {
                return Optional.of(componentEntry);
            }
        }
        return Optional.empty();
    }

    @Reference
    protected void bindComponentKnowledgeService(DistributedComponentKnowledgeService service) {
        this.componentKnowledgeService = service;
    }

    @Reference
    protected void bindPlatformService(PlatformService service) {
        this.platformService = service;
    }

    @Reference
    protected void bindComponentExecutionAuthorizationService(ComponentExecutionAuthorizationService service) {
        this.componentExecutionAuthorizationService = service;
    }

    @Reference
    protected void bindCommunicationService(CommunicationService service) {
        this.communicationService = service;
    }

    @Reference
    protected void bindAuthorizationService(AuthorizationService service) {
        this.authorizationService = service;
    }

    @Reference
    protected void bindCryptographyOperationsProvider(CryptographyOperationsProvider provider) {
        this.cryptographyOperationsProvider = provider;
    }

}
