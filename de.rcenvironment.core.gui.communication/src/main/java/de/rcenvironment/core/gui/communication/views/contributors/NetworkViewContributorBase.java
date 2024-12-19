/*
 * Copyright 2006-2023 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.communication.views.contributors;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import de.rcenvironment.core.gui.communication.views.model.NetworkViewModel;
import de.rcenvironment.core.gui.communication.views.spi.NetworkViewContributor;

/**
 * Convenience base class for {@link NetworkViewContributor}s.
 * 
 * @author Robert Mischke
 */
public abstract class NetworkViewContributorBase implements NetworkViewContributor {

    protected static final Object[] EMPTY_ARRAY = new Object[0];

    protected NetworkViewModel currentModel;

    protected TreeViewer treeViewer;

    protected Display display = Display.getCurrent();

    protected Shell parentShellForDialogs;

    @Override
    public void setCurrentModel(NetworkViewModel currentModel) {
        this.currentModel = currentModel;
    }

    @Override
    public void setTreeViewer(TreeViewer viewer) {
        this.treeViewer = viewer;
        this.parentShellForDialogs = treeViewer.getTree().getShell();
    }

    protected RuntimeException newUnexpectedCallException() {
        return new IllegalStateException("Unexpected call to contributor method");
    }

}
