package com.ntdhtcct.domain.wbs;

import java.util.List;

public record CriticalPathResponse(
        Long projectId,
        int durationDays,
        String criticalPathCount,
        boolean complete,
        int unscheduledTaskCount,
        List<WbsItem> criticalTasks
) {
}