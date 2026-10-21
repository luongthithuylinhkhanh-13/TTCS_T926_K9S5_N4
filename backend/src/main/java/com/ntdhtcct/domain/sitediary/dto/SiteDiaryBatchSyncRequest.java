package com.ntdhtcct.domain.sitediary.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.ArrayList;
import java.util.List;

public class SiteDiaryBatchSyncRequest {

    @NotEmpty(message = "Danh sách đồng bộ không được để trống")
    @Valid
    private List<SiteDiarySyncRequest> items = new ArrayList<>();

    public SiteDiaryBatchSyncRequest() {
    }

    public SiteDiaryBatchSyncRequest(List<SiteDiarySyncRequest> items) {
        this.items = items;
    }

    public List<SiteDiarySyncRequest> getItems() {
        return items;
    }

    public void setItems(List<SiteDiarySyncRequest> items) {
        this.items = items;
    }
}
