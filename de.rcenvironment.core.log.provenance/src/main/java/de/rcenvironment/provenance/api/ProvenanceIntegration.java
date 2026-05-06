/*
 * Copyright 2026 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.provenance.api;

import java.util.Optional;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * A static access class to centralize the boilerplate code for acquiring an optional {@link ProvenanceEventListener}.
 *
 * @author Robert Mischke
 */
@Component
public class ProvenanceIntegration {

    // null if no listener is present
    private static ProvenanceEventListener sharedProvenanceEventListener;

    @Reference
    // This injects any installed listener service via OSGi-DS once it is registered. If no listener is present,
    // this is simply never called, as the OSGi-DS reference is unfulfilled, and the static field remains null.
    public void setEventListener(ProvenanceEventListener injectedInstance) {
        synchronized (ProvenanceIntegration.class) {
            ProvenanceIntegration.sharedProvenanceEventListener = injectedInstance;
        }
    }

    /**
     * @return true if a {@link ProvenanceEventListener} plugin is installed and loaded
     */
    public static boolean isEventListenerPresent() {
        synchronized (ProvenanceIntegration.class) {
            return sharedProvenanceEventListener != null;
        }
    }

    /**
     * @return the {@link ProvenanceEventListener} plugin, if one is installed and loaded; null otherwise
     */
    public static ProvenanceEventListener getEventListener() {
        synchronized (ProvenanceIntegration.class) {
            return sharedProvenanceEventListener;
        }
    }

    /**
     * @return an {@link Optional} containing the {@link ProvenanceEventListener} plugin, if one is installed and loaded
     */
    public static Optional<ProvenanceEventListener> getOptionalEventListener() {
        synchronized (ProvenanceIntegration.class) {
            return Optional.ofNullable(sharedProvenanceEventListener);
        }
    }

}
