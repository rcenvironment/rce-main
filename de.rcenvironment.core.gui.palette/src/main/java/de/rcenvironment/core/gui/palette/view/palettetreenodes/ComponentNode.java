/*
 * Copyright 2006-2026 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.gui.palette.view.palettetreenodes;

import java.util.Optional;

import org.eclipse.jface.viewers.TreeNode;

import de.rcenvironment.core.gui.palette.toolidentification.ToolIdentification;
import de.rcenvironment.core.gui.palette.toolidentification.ToolType;
import de.rcenvironment.core.gui.palette.view.PaletteView;
import de.rcenvironment.core.gui.workflow.editor.WorkflowEditor;


/**
 * PaletteTreeNode extension representing workflow components.
 *
 * @author Jan Flink
 */
public abstract class ComponentNode extends PaletteTreeNode {

    private ToolIdentification toolIdentification;

    protected ComponentNode(TreeNode parent, ToolIdentification toolIdentification) {
        super(parent, toolIdentification.getToolName());
        this.toolIdentification = toolIdentification;
    }

    public ToolIdentification getToolIdentification() {
        return toolIdentification;
    }

    @Override
    public int compareTo(PaletteTreeNode o) {
        return getQualifiedGroupName().compareTo(o.getQualifiedGroupName());
    }

    public PaletteTreeNode getParentGroupNode() {
        PaletteTreeNode parent = getPaletteParent();
        if (parent.isGroup()) {
            return parent;
        }
        return null;
    }

    public abstract boolean isLocal();

    public ToolType getType() {
        return toolIdentification.getType();
    }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) {
            return false;
        }
        ComponentNode other = (ComponentNode) obj;
        return getToolIdentification().equals(other.getToolIdentification());
    }

    @Override
    public int hashCode() {
        StringBuilder builder = new StringBuilder();
        builder.append(getQualifiedGroupName());
        builder.append(getType().toString());
        return builder.toString().hashCode();
    }

    @Override
    public void handleDoubleclick(PaletteView paletteView) {
        // TODO Auto-generated method stub

    }

    @Override
    public void handleWidgetSelected(WorkflowEditor editor) {
        // TODO Auto-generated method stub

    }

    @Override
    public void handleEditEvent() {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean canHandleEditEvent() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Optional<String> getHelpContextID() {
        // TODO Auto-generated method stub
        return Optional.empty();
    }

    @Override
    public boolean isCustomized() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void dispose() {
        getIcon().ifPresent(icon -> icon.dispose());
        super.dispose();
    }

}
