package com.ntdhtcct.domain.milestone.dto;

import com.ntdhtcct.domain.wbs.WbsItem;

import java.time.LocalDate;
import java.util.UUID;

public record TaskDelayInfo(
        UUID id,
        String wbsCode,
        String name,
        Integer duration,
        Integer es,
        Integer ef,
        LocalDate startDate,
        LocalDate endDate,
        boolean critical
) {
    public static TaskDelayInfo from(WbsItem item) {
        return new TaskDelayInfo(
                item.getId(),
                item.getWbsCode(),
                item.getName(),
                item.getDuration(),
                item.getEs(),
                item.getEf(),
                item.getStartDate(),
                item.getEndDate(),
                item.isCritical()
        );
    }
}
