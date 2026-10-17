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
        OffsetDateTime capturedAt,
        Double latitude,
        Double longitude,
        String metadataStatus,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public DiaryImageResponse(
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
            OffsetDateTime capturedAt,
            Double latitude,
            Double longitude,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
        this(id, diaryEntryId, clientImageId, originalFilename, displayStatus, originalStatus,
                displayFileSize, originalFileSize, width, height, capturedAt, latitude, longitude,
                computeMetadataStatus(capturedAt, latitude, longitude), createdAt, updatedAt);
    }

    public DiaryImageResponse(
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
        this(id, diaryEntryId, clientImageId, originalFilename, displayStatus, originalStatus,
                displayFileSize, originalFileSize, width, height, null, null, null, createdAt, updatedAt);
    }

    public static String computeMetadataStatus(OffsetDateTime capturedAt, Double latitude, Double longitude) {
        if (capturedAt == null) {
            return "MISSING";
        }
        OffsetDateTime maxAllowedFuture = OffsetDateTime.now().plusMinutes(5);
        if (capturedAt.isAfter(maxAllowedFuture) || capturedAt.getYear() < 2000) {
            return "INVALID";
        }
        if ((latitude == null && longitude != null) || (latitude != null && longitude == null)) {
            return "INVALID";
        }
        if (latitude != null && longitude != null) {
            if (Double.isNaN(latitude) || Double.isInfinite(latitude) || latitude < -90.0 || latitude > 90.0) {
                return "INVALID";
            }
            if (Double.isNaN(longitude) || Double.isInfinite(longitude) || longitude < -180.0 || longitude > 180.0) {
                return "INVALID";
            }
        }
        return "VALID";
    }
}
