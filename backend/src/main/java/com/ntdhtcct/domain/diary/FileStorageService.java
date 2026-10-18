package com.ntdhtcct.domain.diary;

import com.ntdhtcct.common.exception.BadRequestException;
import com.ntdhtcct.common.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path rootStorageDir;

    public FileStorageService(@Value("${app.storage.dir:uploads/diary}") String storageDir) {
        this.rootStorageDir = Paths.get(storageDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.rootStorageDir);
        } catch (IOException e) {
            throw new RuntimeException("Không thể khởi tạo thư mục lưu trữ: " + this.rootStorageDir, e);
        }
    }

    /**
     * Lưu file hiển thị và sinh thumbnail.
     * Trả về kết quả gồm đường dẫn tương đối và metadata.
     */
    public StoredDisplayResult storeDisplayFile(UUID entryId, String clientImageId, UUID attemptId, MultipartFile file) throws IOException {
        byte[] bytes = file.getBytes();
        if (bytes.length == 0) {
            throw new BadRequestException("INVALID_IMAGE_DATA", "File ảnh trống");
        }

        if (!isSupportedImageFormat(bytes)) {
            throw new BadRequestException("INVALID_IMAGE_FORMAT", "Định dạng file không được hỗ trợ (chỉ chấp nhận JPEG, PNG, WebP)");
        }

        BufferedImage img;
        try {
            img = ImageIO.read(new ByteArrayInputStream(bytes));
        } catch (Exception e) {
            throw new BadRequestException("INVALID_IMAGE_FORMAT", "File ảnh bị lỗi hoặc không thể đọc");
        }

        if (img == null) {
            throw new BadRequestException("INVALID_IMAGE_FORMAT", "Dữ liệu không phải là định dạng ảnh hợp lệ hoặc không thể giải mã");
        }

        long totalPixels = (long) img.getWidth() * img.getHeight();
        if (totalPixels > 100_000_000L) {
            throw new BadRequestException("IMAGE_TOO_LARGE_PIXELS", "Kích thước ảnh giải mã quá lớn");
        }

        int width = img.getWidth();
        int height = img.getHeight();

        Path entryDir = rootStorageDir.resolve(entryId.toString());
        Files.createDirectories(entryDir);

        // Lưu display file với attemptId duy nhất
        String displayFileName = clientImageId + "_display_" + attemptId + ".jpg";
        Path displayPath = entryDir.resolve(displayFileName);
        Files.write(displayPath, bytes);

        // Sinh thumbnail
        String thumbFileName = clientImageId + "_thumb_" + attemptId + ".jpg";
        Path thumbPath = entryDir.resolve(thumbFileName);
        byte[] thumbBytes = generateThumbnail(img, 200, 200);
        Files.write(thumbPath, thumbBytes);

        String relativeDisplayPath = entryId + "/" + displayFileName;
        String relativeThumbPath = entryId + "/" + thumbFileName;

        return new StoredDisplayResult(relativeDisplayPath, relativeThumbPath, (long) bytes.length, width, height);
    }

    /**
     * Lưu nguyên trạng bytes file gốc với attemptId duy nhất.
     */
    public StoredOriginalResult storeOriginalFile(UUID entryId, String clientImageId, UUID attemptId, MultipartFile file) throws IOException {
        byte[] bytes = file.getBytes();
        if (bytes.length == 0) {
            throw new BadRequestException("INVALID_IMAGE_DATA", "File ảnh gốc trống");
        }

        // Kiểm tra magic bytes cơ bản để bảo đảm định dạng ảnh (JPEG, PNG, WebP)
        if (!isSupportedImageFormat(bytes)) {
            throw new BadRequestException("INVALID_IMAGE_FORMAT", "Định dạng file không được hỗ trợ (chỉ chấp nhận JPEG, PNG, WebP)");
        }

        // Kiểm tra khả năng giải mã ảnh và chống bom nén / pixel quá lớn
        try {
            BufferedImage testImg = ImageIO.read(new ByteArrayInputStream(bytes));
            if (testImg != null) {
                long totalPixels = (long) testImg.getWidth() * testImg.getHeight();
                if (totalPixels > 100_000_000L) { // > 100 Megapixels
                    throw new BadRequestException("IMAGE_TOO_LARGE_PIXELS", "Kích thước ảnh giải mã quá lớn");
                }
            }
        } catch (BadRequestException bre) {
            throw bre;
        } catch (Exception e) {
            throw new BadRequestException("INVALID_IMAGE_FORMAT", "File ảnh bị lỗi hoặc không thể đọc");
        }

        Path entryDir = rootStorageDir.resolve(entryId.toString());
        Files.createDirectories(entryDir);

        String originalFileName = clientImageId + "_orig_" + attemptId + ".bin";
        Path targetPath = entryDir.resolve(originalFileName);
        Files.write(targetPath, bytes);

        String relativeOriginalPath = entryId + "/" + originalFileName;
        return new StoredOriginalResult(relativeOriginalPath, (long) bytes.length);
    }

    /**
     * Đọc resource file đã lưu nếu trạng thái COMPLETED.
     */
    public Path loadFile(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            throw new ResourceNotFoundException("FILE_NOT_FOUND", "Đường dẫn file không hợp lệ");
        }
        Path filePath = rootStorageDir.resolve(relativePath).normalize();
        if (!filePath.startsWith(rootStorageDir) || !Files.exists(filePath)) {
            throw new ResourceNotFoundException("FILE_NOT_FOUND", "Không tìm thấy file trên hệ thống lưu trữ");
        }
        return filePath;
    }

    /**
     * Xóa file trên hệ thống lưu trữ nếu tồn tại.
     */
    public void deleteFile(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return;
        }
        try {
            Path filePath = rootStorageDir.resolve(relativePath).normalize();
            if (filePath.startsWith(rootStorageDir)) {
                Files.deleteIfExists(filePath);
            }
        } catch (Exception ignored) {
        }
    }

    private byte[] generateThumbnail(BufferedImage source, int maxW, int maxH) throws IOException {
        int w = source.getWidth();
        int h = source.getHeight();

        double scale = Math.min((double) maxW / w, (double) maxH / h);
        int targetW = Math.max(1, (int) Math.round(w * scale));
        int targetH = Math.max(1, (int) Math.round(h * scale));

        BufferedImage thumb = new BufferedImage(targetW, targetH, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = thumb.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.drawImage(source, 0, 0, targetW, targetH, null);
        } finally {
            g.dispose();
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(thumb, "jpg", baos);
        return baos.toByteArray();
    }

    private boolean isSupportedImageFormat(byte[] bytes) {
        if (bytes == null || bytes.length < 12) {
            return false;
        }
        // JPEG: FF D8 FF
        if ((bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF) {
            return true;
        }
        // PNG: 89 50 4E 47 0D 0A 1A 0A
        if ((bytes[0] & 0xFF) == 0x89 && bytes[1] == 'P' && bytes[2] == 'N' && bytes[3] == 'G') {
            return true;
        }
        // WebP: RIFF .... WEBP
        if (bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') {
            return true;
        }
        return false;
    }

    public record StoredDisplayResult(String displayPath, String thumbnailPath, Long fileSize, Integer width, Integer height) {}
    public record StoredOriginalResult(String originalPath, Long fileSize) {}
}
