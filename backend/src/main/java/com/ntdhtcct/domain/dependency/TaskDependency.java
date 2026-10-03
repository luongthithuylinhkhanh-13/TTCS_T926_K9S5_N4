package com.ntdhtcct.domain.dependency;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "task_dependencies")
public class TaskDependency {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "predecessor_id", nullable = false)
    private UUID predecessorId;

    @Column(name = "successor_id", nullable = false)
    private UUID successorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "dependency_type", nullable = false, length = 10)
    private DependencyType dependencyType = DependencyType.FS;

    @Column(name = "lag_days", nullable = false)
    private int lagDays = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected TaskDependency() {
    }

    public TaskDependency(UUID projectId, UUID predecessorId, UUID successorId, DependencyType type, int lagDays) {
        this.projectId = projectId;
        this.predecessorId = predecessorId;
        this.successorId = successorId;
        this.dependencyType = (type != null) ? type : DependencyType.FS;
        this.lagDays = lagDays;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public UUID getPredecessorId() {
        return predecessorId;
    }

    public UUID getSuccessorId() {
        return successorId;
    }

    public DependencyType getDependencyType() {
        return dependencyType;
    }

    public void setDependencyType(DependencyType dependencyType) {
        this.dependencyType = dependencyType;
    }

    public int getLagDays() {
        return lagDays;
    }

    public void setLagDays(int lagDays) {
        this.lagDays = lagDays;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}