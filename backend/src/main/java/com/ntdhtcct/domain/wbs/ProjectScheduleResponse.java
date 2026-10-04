package com.ntdhtcct.domain.wbs;

import java.util.List;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ProjectScheduleResponse(
        boolean success,
        Summary summary,
        List<WbsItem> tasks,
        OffsetDateTime baselineCapturedAt,
        List<BaselineTask> baselineTasks
) {
    public record BaselineTask(
            UUID itemId,
            String wbsCode,
            String name,
            LocalDate startDate,
            LocalDate endDate,
            Integer duration
    ) {
    }

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