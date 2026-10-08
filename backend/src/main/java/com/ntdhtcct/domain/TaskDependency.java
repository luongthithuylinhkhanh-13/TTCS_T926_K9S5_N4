package com.ntdhtcct.domain;

import java.time.Instant;
import java.util.UUID;

@Deprecated
public class TaskDependency {

    private Long id;
    private UUID predecessorId;
    private UUID successorId;
    private DependencyType dependencyType;
    private Integer lagDays;
    private Instant createdAt;

    public TaskDependency() {
    }

    public TaskDependency(UUID predecessorId, UUID successorId, DependencyType dependencyType, Integer lagDays) {
        this.predecessorId = predecessorId;
        this.successorId = successorId;
        this.dependencyType = dependencyType != null ? dependencyType : DependencyType.FS;
        this.lagDays = lagDays != null ? lagDays : 0;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UUID getPredecessorId() {
        return predecessorId;
    }

    public void setPredecessorId(UUID predecessorId) {
        this.predecessorId = predecessorId;
    }

    public UUID getSuccessorId() {
        return successorId;
    }

    public void setSuccessorId(UUID successorId) {
        this.successorId = successorId;
    }

    public DependencyType getDependencyType() {
        return dependencyType;
    }

    public void setDependencyType(DependencyType dependencyType) {
        this.dependencyType = dependencyType;
    }

    public Integer getLagDays() {
        return lagDays;
    }

    public void setLagDays(Integer lagDays) {
        this.lagDays = lagDays;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}