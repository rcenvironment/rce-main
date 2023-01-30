/*
 * Copyright 2019-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.communication.api;

import de.rcenvironment.core.communication.common.InstanceNodeSessionId;
import de.rcenvironment.core.communication.common.LogicalNodeId;
import de.rcenvironment.core.communication.common.LogicalNodeSessionId;

/**
 * A service for creating and managing {@link LogicalNodeId}s and {@link LogicalNodeSessionId}s on the local node. Includes the option of
 * assigning display names to logical node ids, which are then propagated to all reachable nodes in the network.
 *
 * @author Robert Mischke
 */
public interface LogicalNodeManagementService {

    // TODO add methods for generating *session* ids, too, and consider removing these non-session methods

    /**
     * @return A new {@link LogicalNodeId} based on the given qualifier. For different qualifiers, generated {@link LogicalNodeId}s should
     *         be collision-free. To achieve this, this method may impose length or character restrictions for accepted qualifiers.
     * @param optionalDisplayName if set, this display name will be announced to all connected instances for association with this logical
     *        node; pass null for logical nodes that do not need an associated display name
     * @param qualifier the string by which the generated logical id can be recognized (in the sense of "reconnected to") after the local
     *        instance is restarted
     */
    LogicalNodeId createRecognizableLocalLogicalNodeId(String qualifier, String optionalDisplayName);

    /**
     * @param optionalDisplayName if set, this display name will be announced to all connected instances for association with this logical
     *        node; pass null for logical nodes that do not need an associated display name
     * @return A new {@link LogicalNodeId} with an automatically-generated logical node part. {@link LogicalNodeId}s generated by this
     *         method are expected to be virtually collision-free among each other, and guaranteed to be collision-free with ids generated
     *         by {@link #createRecognizableLocalLogicalNodeId(String)}.
     */
    LogicalNodeId createTransientLocalLogicalNodeId(String optionalDisplayName);

    /**
     * Applies a new display name to an existing local logical node. Unlike the "create" methods, the new display name must not be null.
     * 
     * @param logicalNodeId the existing logical node's id
     * @param newDisplayName the new display name to set
     */
    void updateDisplayNameForLocalLogicalNodeId(LogicalNodeId logicalNodeId, String newDisplayName);

    /**
     * Unregisters a logical node id once it is not needed anymore. Should be called whenever possible to conserve resources.
     * 
     * @param id the id to release
     */
    void releaseLogicalNodeId(LogicalNodeId id);

    /**
     * Convenience access to the local instance's {@link InstanceNodeSessionId}.
     * 
     * @return the local node's {@link InstanceNodeSessionId}
     */
    InstanceNodeSessionId getLocalInstanceSessionId();

}
