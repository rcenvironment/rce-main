/*
 * Copyright 2006-2026 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.palette.view.palettetreenodes;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

import org.eclipse.jface.viewers.DecorationOverlayIcon;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.TreeNode;
import org.eclipse.swt.graphics.Image;

import de.rcenvironment.core.gui.palette.view.PaletteView;
import de.rcenvironment.core.gui.resources.api.ImageManager;
import de.rcenvironment.core.gui.resources.api.StandardImages;
import de.rcenvironment.core.gui.workflow.editor.WorkflowEditor;
import de.rcenvironment.core.utils.common.StringUtils;

/**
 * {@link PaletteTreeNode} for Group Nodes.
 * 
 * @author Kathrin Schaffert
 * @author Jan Flink
 *
 */
public class GroupNode extends PaletteTreeNode {

    private boolean customGroup = false;

    private Image folderIcon;

    private Image folderClosedIcon;

    protected GroupNode(PaletteTreeNode parent, String nodeName) {
        super(parent, nodeName);
    }


    @Override
    public void handleDoubleclick(PaletteView paletteView) {
        paletteView.setExpandedState(this, !paletteView.getPaletteTreeViewer().getExpandedState(this));
    }

    @Override
    public void handleEditEvent() {
        throw new UnsupportedOperationException(
            StringUtils.format("Unexpected edit event on %s", this.getClass().getCanonicalName()));
    }

    @Override
    public boolean canHandleEditEvent() {
        return false;
    }

    @Override
    public void handleWidgetSelected(WorkflowEditor editor) {
        editor.getViewer().getEditDomain().loadDefaultTool();

    }

    @Override
    public Optional<Image> getIcon() {
        boolean showExpanded =
            isExpandable() && getContentProvider().getExpandedState(this);
        return Optional.ofNullable(getFolderIcon(showExpanded));
    }

    private boolean isExpandable() {
        return getContentProvider().isExpandable(this);
    }

    public Image getFolderIcon(boolean showExpanded) {
        if (folderIcon == null || folderClosedIcon == null) {
            createIcons(isCustomGroup());
        }
        if (showExpanded) {
            return folderIcon;
        }
        return folderClosedIcon;
    }

    private Image getOverlayIcon(Image baseIcon) {
        DecorationOverlayIcon overlayIcon =
            new DecorationOverlayIcon(baseIcon,
                ImageManager.getInstance().getImageDescriptor(StandardImages.DECORATOR_CUSTOM), IDecoration.BOTTOM_LEFT);
        return overlayIcon.createImage();
    }

    public boolean isExpanded() {
        return getContentProvider().getExpandedState(this);
    }

    public boolean isCustomGroup() {
        return customGroup;
    }

    public void setCustomGroup(boolean isCustomGroup) {
        if (this.customGroup != isCustomGroup) {
            createIcons(isCustomGroup);
        }
        this.customGroup = isCustomGroup;
    }

    @Override
    public boolean isCustomized() {
        if (isCustomGroup()) {
            return true;
        }
        if (!hasChildren()) {
            return false;
        }
        PaletteTreeNode[] children = (PaletteTreeNode[]) this.getChildren();
        return Stream.of(children).anyMatch(PaletteTreeNode::isCustomized);
    }

    @Override
    public void removeChild(TreeNode child) {
        if (!hasChildren()) {
            getContentProvider().setExpandedState(this, false);
        }
        super.removeChild(child);
    }

    public Image getMenuIcon() {
        return folderIcon;
    }


    @Override
    public void dispose() {
        if (isCustomGroup()) {
            if (folderIcon != null && !folderIcon.isDisposed()) {
                folderIcon.dispose();
            }
            if (folderClosedIcon != null && !folderClosedIcon.isDisposed()) {
                folderClosedIcon.dispose();
            }
        }
        super.dispose();
    }

    @Override
    public Optional<String> getHelpContextID() {
        return Optional.empty();
    }

    public Stream<ComponentNode> getAllComponentNodes() {
        return Arrays.stream(this.getChildren()).filter(ComponentNode.class::isInstance).map(ComponentNode.class::cast);
    }


    private void createIcons(boolean isCustomGroup) {
        if (isCustomGroup) {
            folderIcon = getOverlayIcon(ImageManager.getInstance().getSharedImage(StandardImages.FOLDER_16));
            folderClosedIcon = getOverlayIcon(ImageManager.getInstance().getSharedImage(StandardImages.FOLDER_CLOSED_16));
        } else {
            folderClosedIcon = ImageManager.getInstance().getSharedImage(StandardImages.FOLDER_CLOSED_16);
            folderIcon = ImageManager.getInstance().getSharedImage(StandardImages.FOLDER_16);
        }
    }
}
