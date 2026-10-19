package com.ntdhtcct.domain.sitediary.dto;

import com.ntdhtcct.domain.sitediary.SiteDiaryLock;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record SiteDiaryLockResponse(
        UUID id,
        UUID projectId,
        LocalDate lockDate,
        String status,
        boolean locked,
        UUID lockedBy,
        OffsetDateTime lockedAt,
        String lockReason,
        UUID unlockedBy,
        OffsetDateTime unlockedAt,
        String unlockReason,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static SiteDiaryLockResponse from(SiteDiaryLock lock) {
        return new SiteDiaryLockResponse(
                lock.getId(),
                lock.getProjectId(),
                lock.getLockDate(),
                lock.getStatus(),
                lock.isLocked(),
                lock.getLockedBy(),
                lock.getLockedAt(),
                lock.getLockReason(),
                lock.getUnlockedBy(),
                lock.getUnlockedAt(),
                lock.getUnlockReason(),
                lock.getCreatedAt(),
                lock.getUpdatedAt()
        );
    }
}
