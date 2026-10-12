package com.ntdhtcct.domain.diary;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "diary_images")
public class DiaryImage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @NotNull(message = "ID mục nhật ký không được để trống")
    @Column(name = "diary_entry_id", nullable = false)
    private UUID diaryEntryId;

    @NotBlank(message = "clientImageId không được để trống")
    @Size(max = 100)
    @Column(name = "client_image_id", nullable = false, length = 100)
    private String clientImageId;

    @NotBlank(message = "Tên file gốc không được để trống")
    @Size(max = 255)
    @Column(name = "original_filename", nullable = false)
    private String originalFilename;

    @Size(max = 500)
    @Column(name = "thumbnail_path", length = 500)
    private String thumbnailPath;

    @Size(max = 500)
    @Column(name = "display_path", length = 500)
    private String displayPath;

    @Size(max = 500)
    @Column(name = "original_path", length = 500)
    private String originalPath;

    @Column(name = "display_file_size")
    private Long displayFileSize;

    @NotNull
    @Column(name = "original_file_size", nullable = false)
    private Long originalFileSize = 0L;

    @NotBlank(message = "Content type không được để trống")
    @Size(max = 100)
    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "width")
    private Integer width;

    @Column(name = "height")
    private Integer height;

    @Column(name = "captured_at")
    private OffsetDateTime capturedAt;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @NotBlank
    @Size(max = 30)
    @Column(name = "display_status", nullable = false, length = 30)
    private String displayStatus = "PENDING";

    @NotBlank
    @Size(max = 30)
    @Column(name = "original_status", nullable = false, length = 30)
    private String originalStatus = "PENDING";

    @Column(name = "display_attempt_id")
    private UUID displayAttemptId;

    @Column(name = "original_attempt_id")
    private UUID originalAttemptId;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public DiaryImage() {
    }

    public DiaryImage(UUID diaryEntryId, String clientImageId, String originalFilename, String contentType) {
        this.diaryEntryId = diaryEntryId;
        this.clientImageId = clientImageId;
        this.originalFilename = originalFilename;
        this.contentType = contentType;
    }

    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getDiaryEntryId() {
        return diaryEntryId;
    }

    public void setDiaryEntryId(UUID diaryEntryId) {
        this.diaryEntryId = diaryEntryId;
    }

    public String getClientImageId() {
        return clientImageId;
    }

    public void setClientImageId(String clientImageId) {
        this.clientImageId = clientImageId;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public String getThumbnailPath() {
        return thumbnailPath;
    }

    public void setThumbnailPath(String thumbnailPath) {
        this.thumbnailPath = thumbnailPath;
    }

    public String getDisplayPath() {
        return displayPath;
    }

    public void setDisplayPath(String displayPath) {
        this.displayPath = displayPath;
    }

    public String getOriginalPath() {
        return originalPath;
    }

    public void setOriginalPath(String originalPath) {
        this.originalPath = originalPath;
    }

    public Long getDisplayFileSize() {
        return displayFileSize;
    }

    public void setDisplayFileSize(Long displayFileSize) {
        this.displayFileSize = displayFileSize;
    }

    public Long getOriginalFileSize() {
        return originalFileSize;
    }

    public void setOriginalFileSize(Long originalFileSize) {
        this.originalFileSize = originalFileSize;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }

    public OffsetDateTime getCapturedAt() {
        return capturedAt;
    }

    public void setCapturedAt(OffsetDateTime capturedAt) {
        this.capturedAt = capturedAt;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public String getDisplayStatus() {
        return displayStatus;
    }

    public void setDisplayStatus(String displayStatus) {
        this.displayStatus = displayStatus;
    }

    public String getOriginalStatus() {
        return originalStatus;
    }

    public void setOriginalStatus(String originalStatus) {
        this.originalStatus = originalStatus;
    }

    public UUID getDisplayAttemptId() {
        return displayAttemptId;
    }

    public void setDisplayAttemptId(UUID displayAttemptId) {
        this.displayAttemptId = displayAttemptId;
    }

    public UUID getOriginalAttemptId() {
        return originalAttemptId;
    }

    public void setOriginalAttemptId(UUID originalAttemptId) {
        this.originalAttemptId = originalAttemptId;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
