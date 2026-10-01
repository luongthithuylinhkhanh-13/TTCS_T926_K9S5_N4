package com.ntdhtcct.domain.dependency;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateDependencyRequest(
    @NotNull(message = "Predecessor ID is required")
    UUID predecessorId,

    DependencyType dependencyType,
    Integer lagDays
) {}