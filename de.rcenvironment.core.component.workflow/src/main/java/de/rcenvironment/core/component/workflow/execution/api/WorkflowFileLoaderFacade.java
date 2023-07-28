/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
package de.rcenvironment.core.component.workflow.execution.api;

import java.io.File;
import java.util.Collection;

import de.rcenvironment.core.component.workflow.model.api.WorkflowDescription;
import de.rcenvironment.core.utils.common.InvalidFilenameException;
import de.rcenvironment.core.utils.common.StringUtils;
import de.rcenvironment.core.utils.common.textstream.TextOutputReceiver;

/**
 * Convenience facade for {@link PersistentWorkflowDescriptionLoaderService} that loads the workflow file, updates it if required,
 * substitutes placeholders present in the workflow file according to a given placeholder file, and validates that the workflow description
 * resulting from that process is indeed executable given the currently visible components.
 * 
 * @author Alexander Weinert
 */
public interface WorkflowFileLoaderFacade {

    /**
     * Abstract base class for exceptions thrown during loading or parsing a placeholders file.
     * 
     * @author Alexander Weinert
     */
    abstract class PlaceholderFileException extends Exception {

        private static final long serialVersionUID = 2336426999013756804L;

        protected PlaceholderFileException(String message) {
            super(message);
        }

        protected PlaceholderFileException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Thrown if parsing the placeholders file failed. This points to an invalid format of that file.
     * 
     * @author Alexander Weinert
     */
    class PlaceholderFileParsingFailedException extends PlaceholderFileException {

        private static final long serialVersionUID = -3742880799659570272L;

        public PlaceholderFileParsingFailedException(File placeholdersFile, Throwable cause) {
            super(StringUtils.format("Failed to parse placeholders file: %s", placeholdersFile.getAbsolutePath()), cause);
        }

    }

    /**
     * Thrown if the given placeholders file contains type placeholders. These are not yet supported.
     * 
     * @author Alexander Weinert
     */
    class TypePlaceholdersPresentException extends PlaceholderFileException {

        private static final long serialVersionUID = -1696644338107828434L;

        public TypePlaceholdersPresentException() {
            super("This workflow uses component *type* placeholders which are not supported in headless execution yet");
        }
    }

    /**
     * Thrown if the given workflow file contains placeholders for which no values were present in the given placeholders file.
     * 
     * @author Alexander Weinert
     */
    class PlaceholderValuesMissingException extends PlaceholderFileException {

        private static final long serialVersionUID = -961448075538283456L;

        public PlaceholderValuesMissingException(Collection<String> missingPlaceholderValues) {
            super("The workflow requires additional placeholder values "
                + "(listed as <component id>/<version> -> <placeholder key> (<instance id>)): " + missingPlaceholderValues);
        }
    }

    /**
     * See documentation of {@link WorkflowFileLoaderFacade}.
     * 
     * @param workflowFile The workflow file to be loaded
     * @param placeholdersFile The file containing values for the placeholders contained in <code>workflowFile</code>
     * @param allowUpdates If true, the workflow file will be updated to the newest format during loading, if required
     * @param output A receiver that displays error messages to the user
     * @return The parsed, expanded, and validated workflow description
     * @throws WorkflowFileException Thrown if loading or parsing the workflowFile failed.
     * @throws PlaceholderFileException Thrown if loading or parsing the placeholdersFile failed.
     * @throws WorkflowExecutionException Thrown if validating the parsed workflow description failed
     * @throws InvalidFilenameException Thrown if the given filename is not valid on both Windows and Linux platforms
     */
    WorkflowDescription loadAndValidateWorkflowDescription(File workflowFile, File placeholdersFile, boolean allowUpdates,
        TextOutputReceiver output) throws WorkflowFileException, PlaceholderFileException, WorkflowExecutionException, InvalidFilenameException;

}
