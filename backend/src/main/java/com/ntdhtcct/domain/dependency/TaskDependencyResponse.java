package com.ntdhtcct.domain.dependency;

import java.time.OffsetDateTime;
import java.util.UUID;

public record TaskDependencyResponse(
    UUID id,
    UUID projectId,
    UUID predecessorId,
    UUID successorId,
    DependencyType dependencyType,
    int lagDays,
    OffsetDateTime createdAt
) {
    public static TaskDependencyResponse from(TaskDependency dep) {
        return new TaskDependencyResponse(
            dep.getId(),
            dep.getProjectId(),
            dep.getPredecessorId(),
            dep.getSuccessorId(),
            dep.getDependencyType(),
            dep.getLagDays(),
            dep.getCreatedAt()
        );
    }
}