package com.ntdhtcct.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * T-18 (NTDHTCT-144): Entity ánh xạ bảng `task_dependencies`.
 * Đại diện cho mối quan hệ phụ thuộc giữa hai công việc trong tiến độ thi công.
 * Bao gồm loại quan hệ (FS, SS, FF, SF) và độ trễ (Lag).
 */
@Entity
@Table(name = "task_dependencies", uniqueConstraints = {
        @UniqueConstraint(name = "uk_task_dependencies_pred_succ", columnNames = {"predecessor_id", "successor_id"})
})
public class TaskDependency {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "predecessor_id", nullable = false)
    private Task predecessor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "successor_id", nullable = false)
    private Task successor;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 10)
    private DependencyType type = DependencyType.FS;

    @Column(name = "lag", nullable = false)
    private Double lag = 0.0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public TaskDependency() {
    }

    public TaskDependency(Project project, Task predecessor, Task successor, DependencyType type, Double lag) {
        this.project = project;
        this.predecessor = predecessor;
        this.successor = successor;
        this.type = type != null ? type : DependencyType.FS;
        this.lag = lag != null ? lag : 0.0;
    }

    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.type == null) {
            this.type = DependencyType.FS;
        }
        if (this.lag == null) {
            this.lag = 0.0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public Task getPredecessor() {
        return predecessor;
    }

    public void setPredecessor(Task predecessor) {
        this.predecessor = predecessor;
    }

    public Task getSuccessor() {
        return successor;
    }

    public void setSuccessor(Task successor) {
        this.successor = successor;
    }

    public DependencyType getType() {
        return type;
    }

    public void setType(DependencyType type) {
        this.type = type;
    }

    public Double getLag() {
        return lag;
    }

    public void setLag(Double lag) {
        this.lag = lag;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TaskDependency that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
