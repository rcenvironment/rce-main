/*
 * Copyright 2006-2024 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.components.script.execution.python;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import de.rcenvironment.components.script.common.pythonAgentInstanceManager.PythonAgentInstanceManager;
import de.rcenvironment.components.script.common.registry.ScriptExecutor;
import de.rcenvironment.components.script.common.registry.ScriptExecutorFactory;
import de.rcenvironment.components.script.execution.python.internal.PythonAgentScriptExecutor;
import de.rcenvironment.core.utils.scripting.ScriptLanguage;

/**
 * Factory to create {@link PythonAgentScriptExecutor} objects.
 * 
 * @author Adrian Stock
 * @author Robert Mischke (minor OSGi-DS changes)
 */
@Component
public class PythonAgentScriptExecutorFactory implements ScriptExecutorFactory {

    /**
     * The {@linkPythonInstanceManager} so that the {@linkPythonAgentScriptExecutor} objects created within this factory have access to the
     * python instances.
     */
    @Reference
    private PythonAgentInstanceManager pythonInstanceManager;

    @Override
    public ScriptLanguage getSupportingScriptLanguage() {
        return ScriptLanguage.PythonExp;
    }

    @Override
    public ScriptExecutor createScriptExecutor() {
        return new PythonAgentScriptExecutor(pythonInstanceManager);
    }

}
