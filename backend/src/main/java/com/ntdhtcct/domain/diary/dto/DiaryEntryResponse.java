package com.ntdhtcct.domain.diary.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record DiaryEntryResponse(
        UUID id,
        UUID projectId,
        UUID wbsItemId,
        UUID authorId,
        LocalDate entryDate,
        String title,
        String content,
        String weather,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
