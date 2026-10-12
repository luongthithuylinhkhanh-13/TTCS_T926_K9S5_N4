package com.ntdhtcct.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ImageUploadService {

    private static final long MAX_FILE_SIZE = 20L * 1024 * 1024;

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif"
    );

    private final Path uploadDirectory =
            Paths.get("uploads", "images").toAbsolutePath().normalize();

    public String upload(MultipartFile file) throws IOException {
        validateFile(file);

        Files.createDirectories(uploadDirectory);

        String originalFilename = file.getOriginalFilename();
        String extension = getExtension(originalFilename);

        String filename = UUID.randomUUID() + extension;
        Path destination = uploadDirectory.resolve(filename).normalize();

        if (!destination.startsWith(uploadDirectory)) {
            throw new IllegalArgumentException("Tên file không hợp lệ");
        }

        file.transferTo(destination);

        return "/uploads/images/" + filename;
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn ảnh");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException(
                    "Dung lượng ảnh không được vượt quá 20 MB"
            );
        }

        String contentType = file.getContentType();

        if (contentType == null
                || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException(
                    "Chỉ chấp nhận ảnh JPEG, PNG, WEBP hoặc GIF"
            );
        }

        String extension = getExtension(file.getOriginalFilename());

        if (extension.isEmpty()) {
            throw new IllegalArgumentException("File ảnh thiếu phần mở rộng");
        }

        String normalizedExtension = extension.toLowerCase();

        if (!Set.of(".jpg", ".jpeg", ".png", ".webp", ".gif")
                .contains(normalizedExtension)) {
            throw new IllegalArgumentException(
                    "Phần mở rộng file ảnh không được hỗ trợ"
            );
        }
    }

    private String getExtension(String filename) {
        if (filename == null) {
            return "";
        }

        int dotIndex = filename.lastIndexOf('.');

        if (dotIndex < 0 || dotIndex == filename.length() - 1) {
            return "";
        }

        return filename.substring(dotIndex);
    }
}