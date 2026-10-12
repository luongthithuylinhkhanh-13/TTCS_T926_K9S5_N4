package com.ntdhtcct.controller;

import com.ntdhtcct.auth.service.AuthorizationService;
import com.ntdhtcct.domain.auth.AuthTokenService;
import com.ntdhtcct.service.ImageUploadService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/api/projects/{projectId}/images")
public class ImageUploadController {

    private final ImageUploadService imageUploadService;
    private final AuthTokenService authTokenService;
    private final AuthorizationService authorizationService;

    public ImageUploadController(
            ImageUploadService imageUploadService,
            AuthTokenService authTokenService,
            AuthorizationService authorizationService
    ) {
        this.imageUploadService = imageUploadService;
        this.authTokenService = authTokenService;
        this.authorizationService = authorizationService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadImage(
            @PathVariable UUID projectId,
            @RequestHeader(value = "Authorization", required = false)
            String authorization,
            @RequestParam("file") MultipartFile file
    ) {
        UUID userId = authenticatedUserId(authorization);

        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse(false, "Bạn cần đăng nhập"));
        }

        try {
            authorizationService.checkProjectAccess(
                    projectId, userId, null
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse(
                            false, "Bạn không có quyền truy cập dự án"
                    ));
        }

        try {
            String imageUrl = imageUploadService.upload(file);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiResponse(true, imageUrl));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Không thể lưu ảnh"));
        }
    }

    private UUID authenticatedUserId(String authorization) {
        if (authorization == null
                || !authorization.startsWith("Bearer ")) {
            return null;
        }

        String token = authorization.substring(7).trim();

        try {
            if (token.isEmpty()
                    || !authTokenService.isTokenValid(token)) {
                return null;
            }

            return authTokenService.getUserIdFromToken(token);
        } catch (Exception e) {
            return null;
        }
    }

    public record ApiResponse(boolean success, String message) {
    }
}
