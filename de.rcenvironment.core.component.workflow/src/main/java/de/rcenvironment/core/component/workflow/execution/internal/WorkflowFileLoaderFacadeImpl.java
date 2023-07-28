/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.component.workflow.execution.internal;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.rcenvironment.core.communication.api.PlatformService;
import de.rcenvironment.core.component.api.DistributedComponentKnowledgeService;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowFileLoaderFacade;
import de.rcenvironment.core.component.workflow.execution.api.PersistentWorkflowDescriptionLoaderService;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionException;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowExecutionUtils;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowFileException;
import de.rcenvironment.core.component.workflow.execution.api.WorkflowPlaceholderHandler;
import de.rcenvironment.core.component.workflow.execution.headless.api.HeadlessWorkflowDescriptionLoaderCallback;
import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNode;
import de.rcenvironment.core.component.workflow.model.api.WorkflowNodeIdentifier;
import de.rcenvironment.core.component.workflow.validation.api.WorkflowDescriptionValidationService;
import de.rcenvironment.core.utils.common.CrossPlatformFilenameUtils;
import de.rcenvironment.core.utils.common.InvalidFilenameException;
import de.rcenvironment.core.utils.common.JsonUtils;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.textstream.TextOutputReceiver;

/**
 * @author Doreen Seider
 * @author Robert Mischke
 * @author Alexander Weinert (extracted from WorkflowExecutionServiceImpl)
 */
@Component
public class WorkflowFileLoaderFacadeImpl implements WorkflowFileLoaderFacade {

    private WorkflowExecutionServiceLog log = new WorkflowExecutionServiceLog();

    private PlatformService platformService;

    private DistributedComponentKnowledgeService componentKnowledgeService;

    private PersistentWorkflowDescriptionLoaderService workflowDescriptionLoaderService;

    private WorkflowDescriptionValidationService validator;

    @Override
    public WorkflowDescription loadAndValidateWorkflowDescription(File workflowFile, File placeholdersFile, boolean allowUpdates,
        TextOutputReceiver output) throws PlaceholderFileException, WorkflowFileException, WorkflowExecutionException, InvalidFilenameException {
        CrossPlatformFilenameUtils.throwExceptionIfFilenameNotValid(workflowFile.getName());
        output.addOutput(StringUtils.format("Loading: '%s'; (full path: %s)", 
            workflowFile.getName(), workflowFile.getAbsolutePath()));
        final WorkflowDescription wfDescription = loadWorkflowDescriptionAndPlaceholders(
            workflowFile, allowUpdates, placeholdersFile, output);

        WorkflowExecutionUtils.replaceNullNodeIdentifiersWithActualNodeIdentifier(wfDescription,
            platformService.getLocalDefaultLogicalNodeId(),
            componentKnowledgeService.getCurrentSnapshot());
        WorkflowExecutionUtils
            .setNodeIdentifiersToTransientInCaseOfLocalOnes(wfDescription, platformService.getLocalDefaultLogicalNodeId());

        wfDescription
            .setName(WorkflowExecutionUtils.generateDefaultNameforExecutingWorkflow(workflowFile.getName(),
                wfDescription));
        wfDescription.setFileName(workflowFile.getName());

        if (!validator.validateAvailabilityOfNodesAndComponentsFromLocalKnowledge(wfDescription).isSucceeded()) {
            throw new WorkflowExecutionException(
                "Workflow description invalid: " + workflowFile.getAbsolutePath());
        }
        return wfDescription;
    }

    private WorkflowDescription loadWorkflowDescriptionAndPlaceholders(File workflowFile,
        boolean abortLoadIfUpdateRequired, File placeholdersFile, TextOutputReceiver outputReceiver)
        throws TypePlaceholdersPresentException, PlaceholderValuesMissingException,
        PlaceholderFileParsingFailedException, WorkflowFileException {

        verifyFileExistsAndIsReadable(workflowFile);
        boolean abortIfWorkflowUpdateRequired = abortLoadIfUpdateRequired;
        WorkflowDescription workflowDescription = workflowDescriptionLoaderService.loadWorkflowDescriptionFromFileConsideringUpdates(
            workflowFile, new HeadlessWorkflowDescriptionLoaderCallback(outputReceiver), abortIfWorkflowUpdateRequired);
        if (placeholdersFile != null) {
            verifyFileExistsAndIsReadable(placeholdersFile);
        }
        applyPlaceholdersAndVerify(workflowDescription, placeholdersFile);
        return workflowDescription;
    }

    private void applyPlaceholdersAndVerify(final WorkflowDescription workflowDescription, final File placeholdersFile)
        throws TypePlaceholdersPresentException, PlaceholderValuesMissingException, PlaceholderFileParsingFailedException {

        final Map<String, Map<String, String>> placeholderValues;
        if (placeholdersFile != null) {
            placeholderValues = parsePlaceholdersFile(placeholdersFile);
            log.placeholderFileParsed(placeholdersFile, placeholderValues);
        } else {
            placeholderValues = new HashMap<>();
        }

        WorkflowPlaceholderHandler placeholderDescription =
            WorkflowPlaceholderHandler.createPlaceholderDescriptionFromWorkflowDescription(workflowDescription, "");

        // check for unsupported placeholders
        Map<String, Map<String, String>> componentTypePlaceholders = placeholderDescription.getComponentTypePlaceholders();
        if (!componentTypePlaceholders.isEmpty()) {
            throw new TypePlaceholdersPresentException();
        }

        // get copyInstanceId -> (key->value) map of placeholders
        Map<String, Map<String, String>> componentInstancePlaceholders = placeholderDescription.getComponentInstancePlaceholders();

        Set<String> missingPlaceholderValues = new TreeSet<>();

        // collect required placeholder values
        for (WorkflowNode wn : workflowDescription.getWorkflowNodes()) {
            if (wn.isEnabled()) {
                // extract information
                WorkflowNodeIdentifier compInstanceId = wn.getIdentifierAsObject();
                String componentId = wn.getComponentDescription().getIdentifier();
                // first, try component instance specific placeholders definition
                Map<String, String> ciPlaceholderValues = placeholderValues.get(createComponentInstancePlaceholderKey(wn));
                // then, try component type specific placeholders definition
                if (ciPlaceholderValues == null) {
                    ciPlaceholderValues = placeholderValues.get(componentId);
                }
                Map<String, String> ciPlaceholders = componentInstancePlaceholders.get(compInstanceId.toString());
                // subtract available keys and collect remaining (missing) ones
                if (ciPlaceholders != null) {
                    Set<String> ciPlaceholderKeys = ciPlaceholders.keySet();
                    Set<String> missingCIPlaceholderKeys = ciPlaceholderKeys;
                    if (ciPlaceholderValues != null) {
                        missingCIPlaceholderKeys.removeAll(ciPlaceholderValues.keySet());
                    }

                    // explicit fixes for backwards compatibility; ignore placeholders in settings that
                    // are not actually used
                    eliminateKnownIrrelevantPlaceholders(wn, missingCIPlaceholderKeys);

                    for (String missingKey : missingCIPlaceholderKeys) {
                        missingPlaceholderValues
                            .add(StringUtils.format("\"%s\" -> \"%s\" (%s)", componentId, missingKey, compInstanceId.toString()));
                    }
                }
                // apply available values
                if (ciPlaceholderValues != null && !ciPlaceholderValues.isEmpty()) {
                    wn.getComponentDescription().getConfigurationDescription().setPlaceholders(ciPlaceholderValues);
                    log.appliedPlaceholdersToWorkflowNode(wn, ciPlaceholderValues);
                }
            }
        }

        // check if missing set contains entries -> fail if it does
        if (!missingPlaceholderValues.isEmpty()) {
            throw new PlaceholderValuesMissingException(missingPlaceholderValues);
        }
    }

    private String createComponentInstancePlaceholderKey(WorkflowNode wn) {
        return wn.getComponentDescription().getIdentifier() + "/" + wn.getName();
    }

    private void eliminateKnownIrrelevantPlaceholders(WorkflowNode wn, Set<String> missingCIPlaceholderKeys) {
        Iterator<String> phKeysIterator = missingCIPlaceholderKeys.iterator();
        while (phKeysIterator.hasNext()) {
            String phKey = phKeysIterator.next();
            // Check if the key is active
            if (!WorkflowPlaceholderHandler.isActivePlaceholder(phKey, wn.getComponentDescription().getConfigurationDescription())) {
                phKeysIterator.remove();
            }
        }
    }

    private void verifyFileExistsAndIsReadable(File file) throws WorkflowFileException {
        if (!file.isFile()) {
            throw new WorkflowFileException(StringUtils.format("File doesn't exis: %s", file.getAbsolutePath()));
        }
        if (!file.canRead()) {
            throw new WorkflowFileException(StringUtils.format("File can not be read: %s", file.getAbsolutePath()));
        }
    }

    private Map<String, Map<String, String>> parsePlaceholdersFile(File placeholdersFile) throws PlaceholderFileParsingFailedException {
        ObjectMapper mapper = JsonUtils.getDefaultObjectMapper();
        try {
            return mapper.readValue(placeholdersFile, new TypeReference<HashMap<String, Map<String, String>>>() {
            });
        } catch (IOException e) {
            throw new PlaceholderFileParsingFailedException(placeholdersFile, e);
        }
    }

    @Reference
    public void bindPlatformService(PlatformService newService) {
        this.platformService = newService;
    }

    @Reference
    public void bindComponentKnowledgeService(DistributedComponentKnowledgeService newService) {
        this.componentKnowledgeService = newService;
    }

    @Reference
    public void bindWorkflowDescriptionLoaderService(PersistentWorkflowDescriptionLoaderService newService) {
        this.workflowDescriptionLoaderService = newService;
    }

    @Reference
    public void bindWorkflowDescriptionValidator(WorkflowDescriptionValidationService newService) {
        this.validator = newService;
    }
}
