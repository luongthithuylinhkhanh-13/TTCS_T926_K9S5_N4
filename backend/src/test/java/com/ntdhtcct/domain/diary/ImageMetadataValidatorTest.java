package com.ntdhtcct.domain.diary;

import com.ntdhtcct.common.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ImageMetadataValidatorTest {

    private ImageMetadataValidator validator;

    @BeforeEach
    void setUp() {
        validator = new ImageMetadataValidator();
    }

    @Test
    @DisplayName("T-74: Hợp lệ khi metadata đầy đủ thông tin thời gian chụp và GPS")
    void validate_SuccessWithFullMetadata() {
        ImageMetadataValidator.ImageMetadata meta = new ImageMetadataValidator.ImageMetadata(
                "foundation_inspection.jpg",
                "image/jpeg",
                1024L * 1024L,
                OffsetDateTime.now().minusHours(2),
                21.028511,
                105.854167,
                1920,
                1080
        );

        assertDoesNotThrow(() -> validator.validate(meta, true, true));
    }

    @Test
    @DisplayName("T-74: Chấp nhận ảnh không có GPS khi requireGps = false")
    void validate_SuccessWithoutGpsWhenOptional() {
        ImageMetadataValidator.ImageMetadata meta = new ImageMetadataValidator.ImageMetadata(
                "wall_crack.png",
                "image/png",
                500_000L,
                OffsetDateTime.now().minusMinutes(30),
                null,
                null,
                800,
                600
        );

        assertDoesNotThrow(() -> validator.validate(meta, false, true));
    }

    @Test
    @DisplayName("T-74: Bắt lỗi khi thiếu GPS trong chế độ nghiệp vụ bắt buộc GPS")
    void validate_FailsWhenGpsMissingAndRequired() {
        ImageMetadataValidator.ImageMetadata meta = new ImageMetadataValidator.ImageMetadata(
                "site_view.webp",
                "image/webp",
                300_000L,
                OffsetDateTime.now().minusMinutes(10),
                null,
                null,
                800,
                600
        );

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                validator.validate(meta, true, true)
        );
        assertThat(ex.getCode()).isEqualTo("MISSING_GPS_METADATA");
    }

    @Test
    @DisplayName("T-74: Phát hiện dấu hiệu bất thường khi thời gian chụp ở tương lai")
    void validate_FailsWhenCapturedAtInFuture() {
        ImageMetadataValidator.ImageMetadata meta = new ImageMetadataValidator.ImageMetadata(
                "future_beam.jpg",
                "image/jpeg",
                500_000L,
                OffsetDateTime.now().plusDays(2), // 2 ngày sau
                21.02,
                105.85,
                1920,
                1080
        );

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                validator.validate(meta, false, false)
        );
        assertThat(ex.getCode()).isEqualTo("FUTURE_CAPTURE_TIME");
    }

    @Test
    @DisplayName("T-74: Bắt lỗi khi thời gian chụp quá cũ (trước năm 2000)")
    void validate_FailsWhenCapturedAtTooOld() {
        ImageMetadataValidator.ImageMetadata meta = new ImageMetadataValidator.ImageMetadata(
                "old_scan.jpg",
                "image/jpeg",
                500_000L,
                OffsetDateTime.of(1995, 5, 20, 10, 0, 0, 0, ZoneOffset.UTC),
                21.02,
                105.85,
                1920,
                1080
        );

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                validator.validate(meta, false, false)
        );
        assertThat(ex.getCode()).isEqualTo("ANCIENT_CAPTURE_TIME");
    }

    @Test
    @DisplayName("T-74: Bắt lỗi khi tọa độ GPS chỉ có 1 phần (thiếu latitude hoặc longitude)")
    void validate_FailsWhenGpsIncomplete() {
        ImageMetadataValidator.ImageMetadata metaOnlyLat = new ImageMetadataValidator.ImageMetadata(
                "partial_gps.jpg",
                "image/jpeg",
                500_000L,
                OffsetDateTime.now().minusMinutes(5),
                21.02,
                null,
                800,
                600
        );

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                validator.validate(metaOnlyLat, false, false)
        );
        assertThat(ex.getCode()).isEqualTo("INCOMPLETE_GPS_COORDINATES");
    }

    @Test
    @DisplayName("T-74: Bắt lỗi khi vĩ độ GPS vượt quá giới hạn [-90, +90]")
    void validate_FailsWhenLatitudeOutOfRange() {
        ImageMetadataValidator.ImageMetadata meta = new ImageMetadataValidator.ImageMetadata(
                "bad_lat.jpg",
                "image/jpeg",
                500_000L,
                OffsetDateTime.now().minusMinutes(5),
                95.123, // > 90
                105.85,
                800,
                600
        );

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                validator.validate(meta, false, false)
        );
        assertThat(ex.getCode()).isEqualTo("INVALID_GPS_COORDINATES");
    }

    @Test
    @DisplayName("T-74: Bắt lỗi khi kinh độ GPS vượt quá giới hạn [-180, +180]")
    void validate_FailsWhenLongitudeOutOfRange() {
        ImageMetadataValidator.ImageMetadata meta = new ImageMetadataValidator.ImageMetadata(
                "bad_lon.jpg",
                "image/jpeg",
                500_000L,
                OffsetDateTime.now().minusMinutes(5),
                21.02,
                185.0, // > 180
                800,
                600
        );

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                validator.validate(meta, false, false)
        );
        assertThat(ex.getCode()).isEqualTo("INVALID_GPS_COORDINATES");
    }

    @Test
    @DisplayName("T-74: Bắt lỗi khi tệp ảnh có đuôi không hợp lệ hoặc rỗng")
    void validate_FailsWhenInvalidFilenameOrEmpty() {
        ImageMetadataValidator.ImageMetadata invalidExt = new ImageMetadataValidator.ImageMetadata(
                "script.sh",
                "image/jpeg",
                1000L,
                OffsetDateTime.now(),
                null,
                null,
                null,
                null
        );

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                validator.validate(invalidExt, false, false)
        );
        assertThat(ex.getCode()).isEqualTo("INVALID_IMAGE_FORMAT");

        ImageMetadataValidator.ImageMetadata emptyFile = new ImageMetadataValidator.ImageMetadata(
                "photo.jpg",
                "image/jpeg",
                0L,
                OffsetDateTime.now(),
                null,
                null,
                null,
                null
        );
        BadRequestException exEmpty = assertThrows(BadRequestException.class, () ->
                validator.validate(emptyFile, false, false)
        );
        assertThat(exEmpty.getCode()).isEqualTo("EMPTY_FILE");
    }
}
