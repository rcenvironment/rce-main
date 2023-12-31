/*
 * Copyright 2006-2023 DLR, Germany
 * 
 * All rights reserved
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.component.execution.api;

import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

/**
 * 
 * A (possibly empty) sequence of workflow graph hops. We call a path consistent if, for each hop, the target of the hop is the source of
 * its succeeding hop. There is no guarantee that the path represented by an object of this class is consistent.
 *
 * @author Alexander Weinert
 */
public class WorkflowGraphPath implements Iterable<WorkflowGraphHop>, Serializable {
    
    /**
     * Autogenerated serial version UID.
     */
    private static final long serialVersionUID = -5971231913876896803L;

    private final Deque<WorkflowGraphHop> deque;

    /**
     * Creates a WorkflowGraphPath with empty deque. Only to be used internally. Use the factory method
     * {@link WorkflowGraphPath#createEmpty()} to create an empty path for external use.
     */
    protected WorkflowGraphPath() {
        this.deque = new ArrayDeque<>();
    }

    /**
     * Creates a WorkflowGraphPath with a copy of the deque from other. Only to be used internally. Use the factory method
     * {@link WorkflowGraphPath#createCopy()} to create an object for external use.
     * 
     * @param other The WorkflowGraphPath from which to copy the sequence of {@link WorkflowGraphHop}s.
     */
    protected WorkflowGraphPath(WorkflowGraphPath other) {
        this.deque = new ArrayDeque<>(other.deque);
    }
    
    /**
     * @return An empty path, i.e., one that does not contain {@link WorkflowGraphHop}s.
     */
    public static WorkflowGraphPath createEmpty() {
        return new WorkflowGraphPath();
    }
    
    /**
     * @param other The {@link WorkflowGraphPath} to be copied.
     * @return A shallow copy of the given path.
     */
    public static WorkflowGraphPath createCopy(WorkflowGraphPath other) {
        return new WorkflowGraphPath(other);
    }
    
    /**
     * Appends the given hop to this path. Does not check whether the path remains consistent.
     * 
     * @param hop The hop to be appended to this path.
     */
    public void append(WorkflowGraphHop hop) {
        this.deque.add(hop);
    }
    
    /**
     * @return The last hop of this path.
     * @throws NoSuchElementException If this path is empty.
     */
    public WorkflowGraphHop getLast() throws NoSuchElementException {
        return this.deque.getLast();
    }

    @Override
    public Iterator<WorkflowGraphHop> iterator() {
        return this.deque.iterator();
    }

    /**
     * @return The number of hops in this path.
     */
    public int size() {
        return this.deque.size();
    }

    /**
     * @return The first hop of this path. Also removes this element from the path.
     */
    public WorkflowGraphHop poll() {
        return this.deque.poll();
    }
    
    /**
     * @return The first hop of this path. Does not remove this element from the path.
     */
    public WorkflowGraphHop peek() {
        return this.deque.peek();
    }

    /**
     * @return True if this path is empty, false otherwise.
     */
    public boolean isEmpty() {
        return this.deque.isEmpty();
    }
    
    @Override
    public String toString() {
        return this.deque.stream().map(item -> item.toString()).collect(Collectors.joining(", "));
    }

    /**
     * @return True, if the source node of the first hop is identical to the target node of the final hop. False otherwise.
     */
    public boolean isCircular() {
        
        if (this.isEmpty()) {
            return false;
        }
    
        WorkflowGraphHop firstElement = this.peek();
        WorkflowGraphHop lastElement = this.getLast();
    
        return firstElement.getHopExecutionIdentifier().equals(lastElement.getTargetExecutionIdentifier());
    }
}
