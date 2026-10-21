package com.ntdhtcct.domain.milestone.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record MilestoneWarningResponse(
        UUID milestoneId,
        String milestoneName,
        LocalDate targetDate,
        UUID categoryId,
        String categoryName,
        String categoryWbsCode,
        UUID lastTaskId,
        String lastTaskName,
        String lastTaskWbsCode,
        LocalDate lastTaskEarlyFinishDate,
        Integer lastTaskEfDays,
        long overrunDays,
        boolean isOverrun,
        String status,
        List<TaskDelayInfo> delayChain
) {
}
