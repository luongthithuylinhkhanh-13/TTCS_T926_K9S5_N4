package com.ntdhtcct.domain.sitediary.dto;

import java.util.ArrayList;
import java.util.List;

public class SiteDiaryBatchSyncResponse {

    private int totalProcessed;
    private int createdCount;
    private int duplicateCount;
    private int failedCount;
    private List<SiteDiarySyncResult> results = new ArrayList<>();

    public SiteDiaryBatchSyncResponse() {
    }

    public SiteDiaryBatchSyncResponse(int totalProcessed, int createdCount, int duplicateCount, int failedCount, List<SiteDiarySyncResult> results) {
        this.totalProcessed = totalProcessed;
        this.createdCount = createdCount;
        this.duplicateCount = duplicateCount;
        this.failedCount = failedCount;
        this.results = results;
    }

    public int getTotalProcessed() {
        return totalProcessed;
    }

    public void setTotalProcessed(int totalProcessed) {
        this.totalProcessed = totalProcessed;
    }

    public int getCreatedCount() {
        return createdCount;
    }

    public void setCreatedCount(int createdCount) {
        this.createdCount = createdCount;
    }

    public int getDuplicateCount() {
        return duplicateCount;
    }

    public void setDuplicateCount(int duplicateCount) {
        this.duplicateCount = duplicateCount;
    }

    public int getFailedCount() {
        return failedCount;
    }

    public void setFailedCount(int failedCount) {
        this.failedCount = failedCount;
    }

    public List<SiteDiarySyncResult> getResults() {
        return results;
    }

    public void setResults(List<SiteDiarySyncResult> results) {
        this.results = results;
    }
}
