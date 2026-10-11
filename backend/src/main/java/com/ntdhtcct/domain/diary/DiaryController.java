package com.ntdhtcct.domain.diary;

import com.ntdhtcct.auth.annotation.RequireProjectRole;
import com.ntdhtcct.auth.context.UserSecurityContext;
import com.ntdhtcct.common.response.ApiResponse;
import com.ntdhtcct.domain.diary.dto.CreateDiaryEntryRequest;
import com.ntdhtcct.domain.diary.dto.DiaryEntryResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/projects/{projectId}/diary-entries")
public class DiaryController {

    private final DiaryService diaryService;

    public DiaryController(DiaryService diaryService) {
        this.diaryService = diaryService;
    }

    @PostMapping
    @RequireProjectRole({"ADMIN", "PROJECT_MANAGER", "SITE_ENGINEER"})
    public ResponseEntity<ApiResponse<DiaryEntryResponse>> createDiaryEntry(
            @PathVariable UUID projectId,
            @Valid @RequestBody CreateDiaryEntryRequest request
    ) {
        UUID authorId = UserSecurityContext.getUserId();
        DiaryEntryResponse response = diaryService.createDiaryEntry(projectId, request, authorId);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Tạo nhật ký công trường thành công", response));
    }
}
