package com.ntdhtcct.domain;

import com.ntdhtcct.domain.wbs.WbsItem;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Immutable, in-memory dependency graph for one project's WBS items.
 * Edges are directed from predecessor to successor.
 */
public final class TaskDependencyGraph {

    private final Map<UUID, WbsItem> nodes;
    private final Map<UUID, List<TaskDependency>> outgoingDependencies;
    private final Map<UUID, List<TaskDependency>> incomingDependencies;

    public TaskDependencyGraph(
            Collection<WbsItem> wbsItems,
            Collection<TaskDependency> dependencies
    ) {
        Objects.requireNonNull(wbsItems, "WBS items must not be null");
        Objects.requireNonNull(dependencies, "Dependencies must not be null");

        Map<UUID, WbsItem> nodeMap = new LinkedHashMap<>();
        Map<UUID, List<TaskDependency>> outgoingMap = new LinkedHashMap<>();
        Map<UUID, List<TaskDependency>> incomingMap = new LinkedHashMap<>();

        for (WbsItem wbsItem : wbsItems) {
            Objects.requireNonNull(wbsItem, "WBS item must not be null");

            UUID taskId = Objects.requireNonNull(
                    wbsItem.getId(),
                    "WBS item ID must not be null"
            );

            if (nodeMap.putIfAbsent(taskId, wbsItem) != null) {
                throw new IllegalArgumentException(
                        "Duplicate WBS item ID in dependency graph: " + taskId
                );
            }

            outgoingMap.put(taskId, new ArrayList<>());
            incomingMap.put(taskId, new ArrayList<>());
        }

        for (TaskDependency dependency : dependencies) {
            Objects.requireNonNull(dependency, "Dependency must not be null");

            UUID predecessorId = Objects.requireNonNull(
                    dependency.getPredecessorId(),
                    "Dependency predecessor ID must not be null"
            );
            UUID successorId = Objects.requireNonNull(
                    dependency.getSuccessorId(),
                    "Dependency successor ID must not be null"
            );

            if (!nodeMap.containsKey(predecessorId)
                    || !nodeMap.containsKey(successorId)) {
                throw new IllegalArgumentException(
                        "Dependency endpoints must belong to the dependency graph"
                );
            }

            outgoingMap.get(predecessorId).add(dependency);
            incomingMap.get(successorId).add(dependency);
        }

        this.nodes = Collections.unmodifiableMap(new LinkedHashMap<>(nodeMap));
        this.outgoingDependencies = immutableAdjacencyMap(outgoingMap);
        this.incomingDependencies = immutableAdjacencyMap(incomingMap);
    }

    public Map<UUID, WbsItem> getNodes() {
        return nodes;
    }

    public boolean containsNode(UUID nodeId) {
        return nodeId != null && nodes.containsKey(nodeId);
    }

    public Optional<WbsItem> findNode(UUID nodeId) {
        if (nodeId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(nodes.get(nodeId));
    }

    public int getInDegree(UUID nodeId) {
        validateNodeExists(nodeId);
        return incomingDependencies.get(nodeId).size();
    }

    public int getOutDegree(UUID nodeId) {
        validateNodeExists(nodeId);
        return outgoingDependencies.get(nodeId).size();
    }

    public Map<UUID, Integer> getInDegrees() {
        Map<UUID, Integer> inDegrees = new LinkedHashMap<>();
        nodes.keySet().forEach(nodeId ->
                inDegrees.put(nodeId, incomingDependencies.get(nodeId).size())
        );
        return Collections.unmodifiableMap(inDegrees);
    }

    public List<TaskDependency> getOutgoingDependencies(UUID nodeId) {
        validateNodeExists(nodeId);
        return outgoingDependencies.get(nodeId);
    }

    public List<TaskDependency> getIncomingDependencies(UUID nodeId) {
        validateNodeExists(nodeId);
        return incomingDependencies.get(nodeId);
    }

    private void validateNodeExists(UUID nodeId) {
        Objects.requireNonNull(nodeId, "Node ID must not be null");
        if (!nodes.containsKey(nodeId)) {
            throw new IllegalArgumentException(
                    "Node not found in dependency graph: " + nodeId
            );
        }
    }

    private Map<UUID, List<TaskDependency>> immutableAdjacencyMap(
            Map<UUID, List<TaskDependency>> adjacencyMap
    ) {
        Map<UUID, List<TaskDependency>> immutableMap = new LinkedHashMap<>();

        adjacencyMap.forEach((taskId, dependencies) ->
                immutableMap.put(taskId, List.copyOf(dependencies))
        );

        return Collections.unmodifiableMap(immutableMap);
    }
}
