package com.ntdhtcct.domain.diary.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DiaryImageResponse(
        UUID id,
        UUID diaryEntryId,
        String clientImageId,
        String originalFilename,
        String displayStatus,
        String originalStatus,
        Long displayFileSize,
        Long originalFileSize,
        Integer width,
        Integer height,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
