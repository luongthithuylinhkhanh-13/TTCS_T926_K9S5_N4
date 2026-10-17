package com.ntdhtcct.domain.diary;

import com.ntdhtcct.common.exception.BadRequestException;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Validator kiểm tra siêu dữ liệu (metadata) bắt buộc và phát hiện dấu hiệu bất thường của ảnh hiện trường (T-74 / S-42).
 */
@Component
public class ImageMetadataValidator {

    public static final long MAX_ORIGINAL_FILE_SIZE = 20L * 1024 * 1024; // 20 MiB
    public static final long MAX_DISPLAY_FILE_SIZE = 1024 * 1024; // 1 MiB

    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    private static final Pattern ALLOWED_EXTENSIONS_PATTERN =
            Pattern.compile(".*\\.(jpe?g|png|webp)$", Pattern.CASE_INSENSITIVE);

    private static final OffsetDateTime MIN_REASONABLE_DATE =
            OffsetDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);

    public record ImageMetadata(
            String filename,
            String contentType,
            Long fileSize,
            OffsetDateTime capturedAt,
            Double latitude,
            Double longitude,
            Integer width,
            Integer height
    ) {
    }

    /**
     * Kiểm tra toàn diện siêu dữ liệu bắt buộc và tính bất thường của ảnh.
     *
     * @param metadata          Đối tượng metadata đã tổng hợp từ tham số hoặc trích xuất từ tệp
     * @param requireGps        Có bắt buộc GPS hay không (theo yêu cầu nghiệp vụ)
     * @param requireCapturedAt Có bắt buộc thời gian chụp hay không
     */
    public void validate(ImageMetadata metadata, boolean requireGps, boolean requireCapturedAt) {
        if (metadata == null) {
            throw new BadRequestException("INVALID_METADATA", "Dữ liệu metadata của ảnh không được để trống");
        }

        // 1. Kiểm tra tên tệp
        if (metadata.filename() == null || metadata.filename().isBlank()) {
            throw new BadRequestException("INVALID_FILENAME", "Tên tệp ảnh không được để trống");
        }

        if (!ALLOWED_EXTENSIONS_PATTERN.matcher(metadata.filename().trim()).matches()) {
            throw new BadRequestException(
                    "INVALID_IMAGE_FORMAT",
                    "Phần mở rộng tệp ảnh không hợp lệ. Chỉ chấp nhận định dạng JPEG, PNG hoặc WebP."
            );
        }

        // 2. Kiểm tra Content-Type
        if (metadata.contentType() == null
                || !ALLOWED_MIME_TYPES.contains(metadata.contentType().toLowerCase())) {
            throw new BadRequestException(
                    "INVALID_MIME_TYPE",
                    "Định dạng MIME không được hỗ trợ (chỉ chấp nhận image/jpeg, image/png, image/webp)"
            );
        }

        // 3. Kiểm tra kích thước tệp
        if (metadata.fileSize() == null || metadata.fileSize() <= 0) {
            throw new BadRequestException("EMPTY_FILE", "Tệp ảnh không được rỗng (0 byte)");
        }

        if (metadata.fileSize() > MAX_ORIGINAL_FILE_SIZE) {
            throw new BadRequestException("FILE_TOO_LARGE", "Dung lượng ảnh vượt quá giới hạn tối đa 20 MiB");
        }

        // 4. Kiểm tra thời gian chụp (capturedAt)
        OffsetDateTime capturedAt = metadata.capturedAt();
        if (capturedAt == null) {
            if (requireCapturedAt) {
                throw new BadRequestException(
                        "MISSING_CAPTURED_TIME",
                        "Ảnh thiếu thời gian chụp (capturedAt). Đây là siêu dữ liệu bắt buộc của ảnh hiện trường."
                );
            }
        } else {
            // Dấu hiệu bất thường: thời gian chụp xảy ra trong tương lai (cho phép sai lệch đồng hồ tối đa 5 phút)
            OffsetDateTime maxAllowedFuture = OffsetDateTime.now().plusMinutes(5);
            if (capturedAt.isAfter(maxAllowedFuture)) {
                throw new BadRequestException(
                        "FUTURE_CAPTURE_TIME",
                        "Thời gian chụp ảnh hiện trường (" + capturedAt + ") xảy ra trong tương lai. Đây là dấu hiệu bất thường."
                );
            }

            // Kiểm tra thời gian chụp quá xa trong quá khứ (trước năm 2000)
            if (capturedAt.isBefore(MIN_REASONABLE_DATE)) {
                throw new BadRequestException(
                        "ANCIENT_CAPTURE_TIME",
                        "Thời gian chụp ảnh hiện trường (" + capturedAt + ") quá cũ (trước năm 2000)."
                );
            }
        }

        // 5. Kiểm tra thông tin vị trí / GPS (latitude, longitude)
        Double lat = metadata.latitude();
        Double lon = metadata.longitude();

        if (requireGps) {
            if (lat == null || lon == null) {
                throw new BadRequestException(
                        "MISSING_GPS_METADATA",
                        "Ảnh thiếu thông tin vị trí GPS (vĩ độ, kinh độ). Nghiệp vụ yêu cầu ảnh hiện trường phải có tọa độ GPS."
                );
            }
        }

        if ((lat == null && lon != null) || (lat != null && lon == null)) {
            throw new BadRequestException(
                    "INCOMPLETE_GPS_COORDINATES",
                    "Tọa độ GPS không đồng bộ: yêu cầu cung cấp đầy đủ cả vĩ độ (latitude) và kinh độ (longitude)."
            );
        }

        if (lat != null && lon != null) {
            if (Double.isNaN(lat) || Double.isInfinite(lat) || lat < -90.0 || lat > 90.0) {
                throw new BadRequestException(
                        "INVALID_GPS_COORDINATES",
                        "Vĩ độ GPS không hợp lệ (" + lat + "). Vĩ độ phải nằm trong khoảng từ -90 đến +90 độ."
                );
            }

            if (Double.isNaN(lon) || Double.isInfinite(lon) || lon < -180.0 || lon > 180.0) {
                throw new BadRequestException(
                        "INVALID_GPS_COORDINATES",
                        "Kinh độ GPS không hợp lệ (" + lon + "). Kinh độ phải nằm trong khoảng từ -180 đến +180 độ."
                );
            }
        }
    }

    /**
     * Định nghĩa 3 trạng thái metadata theo T-75.
     */
    public enum MetadataStatus {
        VALID("Meta: Hợp lệ"),
        MISSING("Meta: Thiếu"),
        INVALID("Meta: Không hợp lệ");

        private final String label;

        MetadataStatus(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    /**
     * Xác định trạng thái metadata của ảnh (T-75) theo 3 trạng thái: HỢP LỆ, THIẾU, KHÔNG HỢP LỆ.
     *
     * @param metadata   Dữ liệu metadata
     * @param requireGps Có bắt buộc tọa độ GPS hay không
     * @return MetadataStatus
     */
    public MetadataStatus determineStatus(ImageMetadata metadata, boolean requireGps) {
        if (metadata == null) {
            return MetadataStatus.MISSING;
        }

        // B. THIẾU METADATA BẮT BUỘC
        if (metadata.filename() == null || metadata.filename().isBlank()) {
            return MetadataStatus.MISSING;
        }

        if (metadata.capturedAt() == null) {
            return MetadataStatus.MISSING;
        }

        Double lat = metadata.latitude();
        Double lon = metadata.longitude();

        if (requireGps && (lat == null || lon == null)) {
            return MetadataStatus.MISSING;
        }

        // C. METADATA KHÔNG HỢP LỆ (có metadata nhưng sai / bất thường)
        if (!ALLOWED_EXTENSIONS_PATTERN.matcher(metadata.filename().trim()).matches()) {
            return MetadataStatus.INVALID;
        }

        if (metadata.contentType() == null
                || !ALLOWED_MIME_TYPES.contains(metadata.contentType().toLowerCase())) {
            return MetadataStatus.INVALID;
        }

        if (metadata.fileSize() == null || metadata.fileSize() <= 0 || metadata.fileSize() > MAX_ORIGINAL_FILE_SIZE) {
            return MetadataStatus.INVALID;
        }

        OffsetDateTime capturedAt = metadata.capturedAt();
        OffsetDateTime maxAllowedFuture = OffsetDateTime.now().plusMinutes(5);
        if (capturedAt.isAfter(maxAllowedFuture) || capturedAt.isBefore(MIN_REASONABLE_DATE)) {
            return MetadataStatus.INVALID;
        }

        if ((lat == null && lon != null) || (lat != null && lon == null)) {
            return MetadataStatus.INVALID;
        }

        if (lat != null && lon != null) {
            if (Double.isNaN(lat) || Double.isInfinite(lat) || lat < -90.0 || lat > 90.0) {
                return MetadataStatus.INVALID;
            }
            if (Double.isNaN(lon) || Double.isInfinite(lon) || lon < -180.0 || lon > 180.0) {
                return MetadataStatus.INVALID;
            }
        }

        // A. HỢP LỆ
        return MetadataStatus.VALID;
    }
}
