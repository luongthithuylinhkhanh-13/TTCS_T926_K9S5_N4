package com.ntdhtcct.domain.wbs;

import java.util.List;
import java.util.UUID;

public record ProjectScheduleResponse(
        boolean success,
        Summary summary,
        List<WbsItem> tasks
) {
    public record Summary(
            int totalTasks,
            int criticalTasksCount,
            int projectDuration,
            String criticalPathCount,
            boolean complete,
            int unscheduledTaskCount
    ) {
    }
}