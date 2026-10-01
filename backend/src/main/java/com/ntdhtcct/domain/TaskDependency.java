package com.ntdhtcct.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "task_dependencies")
public class TaskDependency {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "predecessor_id", nullable = false)
    private UUID predecessorId;

    @Column(name = "successor_id", nullable = false)
    private UUID successorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "dependency_type", nullable = false, length = 10)
    private DependencyType dependencyType;

    @Column(name = "lag_days", nullable = false)
    private Integer lagDays;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    public TaskDependency() {
    }

    public TaskDependency(UUID predecessorId, UUID successorId, DependencyType dependencyType, Integer lagDays) {
        this.predecessorId = predecessorId;
        this.successorId = successorId;
        this.dependencyType = dependencyType != null ? dependencyType : DependencyType.FS;
        this.lagDays = lagDays != null ? lagDays : 0;
    }

    @PrePersist
    protected void onCreate() {
        if (this.lagDays == null) {
            this.lagDays = 0;
        }
        if (this.dependencyType == null) {
            this.dependencyType = DependencyType.FS;
        }
        this.createdAt = Instant.now();
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