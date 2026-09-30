package com.ntdhtcct.domain.wbs;

import java.util.List;
import java.util.UUID;

public record CriticalPathResponse(
        UUID projectId,
        int durationDays,
        String criticalPathCount,
        boolean complete,
        int unscheduledTaskCount,
        List<WbsItem> criticalTasks
) {
}
