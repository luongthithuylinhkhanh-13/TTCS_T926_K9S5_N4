package com.ntdhtcct.domain.diary.dto;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
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
        List<String> metadataReasons,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        Double distanceToCenterMeters,
        Double excessDistanceMeters,
        String locationStatus,
        Boolean isSuspicious,
        String locationMessage
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
            String metadataStatus,
            List<String> metadataReasons,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
        this(id, diaryEntryId, clientImageId, originalFilename, displayStatus, originalStatus,
                displayFileSize, originalFileSize, width, height, capturedAt, latitude, longitude,
                metadataStatus != null ? metadataStatus : computeMetadataStatus(capturedAt, latitude, longitude),
                metadataReasons != null ? metadataReasons : computeMetadataReasons(capturedAt, latitude, longitude),
                createdAt, updatedAt,
                null, null, null, false, null);
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
            OffsetDateTime capturedAt,
            Double latitude,
            Double longitude,
            String metadataStatus,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
        this(id, diaryEntryId, clientImageId, originalFilename, displayStatus, originalStatus,
                displayFileSize, originalFileSize, width, height, capturedAt, latitude, longitude,
                metadataStatus != null ? metadataStatus : computeMetadataStatus(capturedAt, latitude, longitude),
                computeMetadataReasons(capturedAt, latitude, longitude),
                createdAt, updatedAt,
                null, null, null, false, null);
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
            OffsetDateTime capturedAt,
            Double latitude,
            Double longitude,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
        this(id, diaryEntryId, clientImageId, originalFilename, displayStatus, originalStatus,
                displayFileSize, originalFileSize, width, height, capturedAt, latitude, longitude,
                computeMetadataStatus(capturedAt, latitude, longitude),
                computeMetadataReasons(capturedAt, latitude, longitude),
                createdAt, updatedAt,
                null, null, null, false, null);
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
            OffsetDateTime capturedAt,
            Double latitude,
            Double longitude,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt,
            Double distanceToCenterMeters,
            Double excessDistanceMeters,
            String locationStatus,
            Boolean isSuspicious,
            String locationMessage
    ) {
        this(id, diaryEntryId, clientImageId, originalFilename, displayStatus, originalStatus,
                displayFileSize, originalFileSize, width, height, capturedAt, latitude, longitude,
                computeMetadataStatus(capturedAt, latitude, longitude),
                computeMetadataReasons(capturedAt, latitude, longitude),
                createdAt, updatedAt,
                distanceToCenterMeters, excessDistanceMeters, locationStatus, isSuspicious, locationMessage);
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

    public static List<String> computeMetadataReasons(OffsetDateTime capturedAt, Double latitude, Double longitude) {
        List<String> reasons = new ArrayList<>();
        if (capturedAt == null) {
            reasons.add("Thiếu thời gian chụp");
        } else {
            OffsetDateTime maxAllowedFuture = OffsetDateTime.now().plusMinutes(5);
            if (capturedAt.isAfter(maxAllowedFuture)) {
                reasons.add("Thời gian chụp ở tương lai");
            }
            if (capturedAt.getYear() < 2000) {
                reasons.add("Thời gian chụp trước năm 2000");
            }
        }

        if (latitude == null && longitude != null) {
            reasons.add("Thiếu vĩ độ GPS (tọa độ GPS không đầy đủ)");
        } else if (latitude != null && longitude == null) {
            reasons.add("Thiếu kinh độ GPS (tọa độ GPS không đầy đủ)");
        } else if (latitude != null && longitude != null) {
            if (Double.isNaN(latitude) || Double.isInfinite(latitude) || latitude < -90.0 || latitude > 90.0) {
                reasons.add("Vĩ độ GPS ngoài phạm vi -90 đến 90");
            }
            if (Double.isNaN(longitude) || Double.isInfinite(longitude) || longitude < -180.0 || longitude > 180.0) {
                reasons.add("Kinh độ GPS ngoài phạm vi -180 đến 180");
            }
        }
        return reasons;
    }
}
