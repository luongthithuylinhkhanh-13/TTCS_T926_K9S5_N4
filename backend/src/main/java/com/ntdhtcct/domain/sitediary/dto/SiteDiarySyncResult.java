package com.ntdhtcct.domain.sitediary.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public class SiteDiarySyncResult {

    private String clientSyncId;
    private UUID serverDiaryId;
    private String status; // "CREATED", "ALREADY_EXISTS", "ERROR"
    private String message;
    private OffsetDateTime syncedAt;

    public SiteDiarySyncResult() {
    }

    public SiteDiarySyncResult(String clientSyncId, UUID serverDiaryId, String status, String message, OffsetDateTime syncedAt) {
        this.clientSyncId = clientSyncId;
        this.serverDiaryId = serverDiaryId;
        this.status = status;
        this.message = message;
        this.syncedAt = syncedAt;
    }

    public static SiteDiarySyncResult created(String clientSyncId, UUID serverDiaryId) {
        return new SiteDiarySyncResult(
                clientSyncId,
                serverDiaryId,
                "CREATED",
                "Đồng bộ thành công và tạo mới bản ghi trên máy chủ",
                OffsetDateTime.now()
        );
    }

    public static SiteDiarySyncResult alreadyExists(String clientSyncId, UUID serverDiaryId) {
        return new SiteDiarySyncResult(
                clientSyncId,
                serverDiaryId,
                "ALREADY_EXISTS",
                "Bản ghi đã được đồng bộ trước đó (Idempotent - không bị trùng lặp)",
                OffsetDateTime.now()
        );
    }

    public static SiteDiarySyncResult error(String clientSyncId, String message) {
        return new SiteDiarySyncResult(
                clientSyncId,
                null,
                "ERROR",
                message,
                OffsetDateTime.now()
        );
    }

    public String getClientSyncId() {
        return clientSyncId;
    }

    public void setClientSyncId(String clientSyncId) {
        this.clientSyncId = clientSyncId;
    }

    public UUID getServerDiaryId() {
        return serverDiaryId;
    }

    public void setServerDiaryId(UUID serverDiaryId) {
        this.serverDiaryId = serverDiaryId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public OffsetDateTime getSyncedAt() {
        return syncedAt;
    }

    public void setSyncedAt(OffsetDateTime syncedAt) {
        this.syncedAt = syncedAt;
    }
}
