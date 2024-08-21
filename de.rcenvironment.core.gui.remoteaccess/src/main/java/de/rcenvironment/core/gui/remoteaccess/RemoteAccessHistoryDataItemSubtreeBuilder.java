/*
 * Copyright 2006-2023 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.gui.remoteaccess;

import org.eclipse.swt.graphics.Image;

import de.rcenvironment.core.component.sshremoteaccess.SshRemoteAccessConstants;
import de.rcenvironment.core.gui.datamanagement.browser.spi.ComponentHistoryDataItemSubtreeBuilder;
import de.rcenvironment.core.gui.datamanagement.browser.spi.DefaultHistoryDataItemSubtreeBuilder;
import de.rcenvironment.core.gui.resources.api.ImageManager;
import de.rcenvironment.core.gui.resources.api.StandardImages;

/**
 * Implementation of {@link ComponentHistoryDataItemSubtreeBuilder} for the Remote Access component.
 *
 * @author Brigitte Boden
 */
public class RemoteAccessHistoryDataItemSubtreeBuilder extends DefaultHistoryDataItemSubtreeBuilder {

    @Override
    public String[] getSupportedHistoryDataItemIdentifier() {
        return new String[] { SshRemoteAccessConstants.COMPONENT_ID.replace(".", "\\.") + ".*" };
    }

    @Override
    public Image getComponentIcon(String historyDataItemIdentifier) {
        return ImageManager.getInstance().getSharedImage(StandardImages.INTEGRATED_TOOL_DEFAULT_16);
    }

}
