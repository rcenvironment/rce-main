/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * All rights reserved
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.component.execution.api;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import de.rcenvironment.core.utils.common.StringUtils;

/**
 * We could simply hold a Collection<WorkflowGraphEdge> to keep track of the edges contained in the graph. Instead, we decide to use this
 * thin wrapper object to increase efficency of determining outgoing edges of certain nodes.
 *
 * @author Alexander Weinert
 */
class WorkflowGraphEdges implements Serializable {

    /**
     * Autogenerated serial version UID.
     */
    private static final long serialVersionUID = 3520020437759252474L;

    private final Map<String, Set<WorkflowGraphEdge>> values = new HashMap<>();

    /**
     * Creates a key out of the {@link WorkflowGraphEdge}, which can be used as key of a map.
     * 
     * @param edge {@link WorkflowGraphEdge} to get the identifier for
     * @return key for the {@link WorkflowGraphEdge}
     */
    private static String createEdgeKey(WorkflowGraphEdge edge) {
        return StringUtils.escapeAndConcat(edge.getSourceExecutionIdentifier().toString(), edge.getOutputIdentifier());
    }

    /**
     * Creates a key out of the {@link WorkflowGraphNode}, which can be used as key of a map.
     * 
     * @param node             source {@link WorkflowGraphNode} of the edge to get the identifier for
     * @param outputIdentifier source endpoint of the edge to get the identifier for
     * @return key for the {@link WorkflowGraphEdge}
     */
    private static String createEdgeKey(WorkflowGraphNode node, String outputIdentifier) {
        return StringUtils.escapeAndConcat(node.getExecutionIdentifier().toString(), outputIdentifier);
    }

    public static WorkflowGraphEdges create(Set<WorkflowGraphEdge> edgeSet) {
        final WorkflowGraphEdges returnValue = new WorkflowGraphEdges();

        for (WorkflowGraphEdge edge : edgeSet) {
            returnValue.addEdge(edge);
        }

        return returnValue;
    }

    public boolean containsEdge(final WorkflowGraphEdge edge) {
        return this.values.containsKey(createEdgeKey(edge));
    }

    public boolean containsOutgoingEdge(final WorkflowGraphNode node, final String outputIdentifier) {
        return this.values.containsKey(createEdgeKey(node, outputIdentifier));
    }

    public Iterable<WorkflowGraphEdge> getOutgoingEdges(final WorkflowGraphNode node, final String outputIdentifier) {
        final String edgeKey = createEdgeKey(node, outputIdentifier);
        if (this.values.containsKey(edgeKey)) {
            return this.values.get(edgeKey);
        } else {
            return new HashSet<>();
        }

    }

    public void addEdge(final WorkflowGraphEdge edge) {
        final String edgeKey = createEdgeKey(edge);
        if (!this.values.containsKey(edgeKey)) {
            this.values.put(edgeKey, new HashSet<WorkflowGraphEdge>());
        }
        this.values.get(edgeKey).add(edge);
    }

    public Iterable<Set<WorkflowGraphEdge>> getAllEdges() {
        return this.values.values();
    }

    @Override
    public int hashCode() {
        // Implementation auto-generated by Eclipse, simplified adapted for Checkstyle by Alexander Weinert
        final int prime = 31;
        return prime + values.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        // Implementation auto-generated by Eclipse, adapted for Checkstyle by Alexander Weinert
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        WorkflowGraphEdges other = (WorkflowGraphEdges) obj;
        return values.equals(other.values);
    }
}
