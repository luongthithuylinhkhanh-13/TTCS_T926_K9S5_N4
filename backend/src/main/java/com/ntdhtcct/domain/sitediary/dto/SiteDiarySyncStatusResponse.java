package com.ntdhtcct.domain.sitediary.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DTO trả về thống kê trạng thái đồng bộ nhật ký công trường.
 * Phục vụ Story S-31 (NTDHTCT-215):
 *   - NTDHTCT-257 (T-72): Trạng thái chưa đồng bộ của nhật ký
 *   - NTDHTCT-258 (T-73): Số lượng mục đang chờ đồng bộ
 */
public class SiteDiarySyncStatusResponse {

    private UUID projectId;
    private long totalDiaries;
    private long syncedCount;
    private long pendingCount;
    private OffsetDateTime lastSyncedAt;

    public SiteDiarySyncStatusResponse() {
    }

    public SiteDiarySyncStatusResponse(UUID projectId, long totalDiaries, long syncedCount, long pendingCount, OffsetDateTime lastSyncedAt) {
        this.projectId = projectId;
        this.totalDiaries = totalDiaries;
        this.syncedCount = syncedCount;
        this.pendingCount = pendingCount;
        this.lastSyncedAt = lastSyncedAt;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public void setProjectId(UUID projectId) {
        this.projectId = projectId;
    }

    public long getTotalDiaries() {
        return totalDiaries;
    }

    public void setTotalDiaries(long totalDiaries) {
        this.totalDiaries = totalDiaries;
    }

    public long getSyncedCount() {
        return syncedCount;
    }

    public void setSyncedCount(long syncedCount) {
        this.syncedCount = syncedCount;
    }

    public long getPendingCount() {
        return pendingCount;
    }

    public void setPendingCount(long pendingCount) {
        this.pendingCount = pendingCount;
    }

    public OffsetDateTime getLastSyncedAt() {
        return lastSyncedAt;
    }

    public void setLastSyncedAt(OffsetDateTime lastSyncedAt) {
        this.lastSyncedAt = lastSyncedAt;
    }
}
