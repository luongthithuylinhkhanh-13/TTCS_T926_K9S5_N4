package com.ntdhtcct.domain.diary;

import com.ntdhtcct.common.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FileStorageServiceTest {

    @TempDir
    Path tempDir;

    private FileStorageService fileStorageService;
    private UUID entryId;

    @BeforeEach
    void setUp() {
        fileStorageService = new FileStorageService(tempDir.toString());
        entryId = UUID.randomUUID();
    }

    @Test
    void storeDisplayFile_ValidJpeg_StoresFileAndGeneratesThumbnail() throws IOException {
        byte[] jpegBytes = createTestImageBytes("jpg", 100, 100);
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", jpegBytes);

        UUID attemptId = UUID.randomUUID();
        FileStorageService.StoredDisplayResult result =
                fileStorageService.storeDisplayFile(entryId, "img-valid", attemptId, file);

        assertThat(result.displayPath()).contains(entryId.toString());
        assertThat(result.thumbnailPath()).contains(entryId.toString());
        assertThat(result.width()).isEqualTo(100);
        assertThat(result.height()).isEqualTo(100);

        Path savedDisplay = fileStorageService.loadFile(result.displayPath());
        Path savedThumb = fileStorageService.loadFile(result.thumbnailPath());
        assertThat(Files.exists(savedDisplay)).isTrue();
        assertThat(Files.exists(savedThumb)).isTrue();
    }

    @Test
    void storeDisplayFile_CorruptImageBytes_ThrowsBadRequest() {
        byte[] corruptBytes = new byte[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11};
        MockMultipartFile file = new MockMultipartFile("file", "corrupt.jpg", "image/jpeg", corruptBytes);

        assertThrows(BadRequestException.class, () ->
                fileStorageService.storeDisplayFile(entryId, "img-corrupt", UUID.randomUUID(), file)
        );
    }

    @Test
    void storeOriginalFile_UnsupportedFormat_ThrowsBadRequest() {
        byte[] textBytes = "This is a plain text file pretending to be image".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "fake.png", "image/png", textBytes);

        assertThrows(BadRequestException.class, () ->
                fileStorageService.storeOriginalFile(entryId, "img-fake", UUID.randomUUID(), file)
        );
    }

    @Test
    void storeOriginalFile_ValidPng_PreservesExactBytes() throws IOException {
        byte[] pngBytes = createTestImageBytes("png", 50, 50);
        MockMultipartFile file = new MockMultipartFile("file", "original.png", "image/png", pngBytes);

        UUID attemptId = UUID.randomUUID();
        FileStorageService.StoredOriginalResult result =
                fileStorageService.storeOriginalFile(entryId, "img-png", attemptId, file);

        Path savedFile = fileStorageService.loadFile(result.originalPath());
        byte[] readBytes = Files.readAllBytes(savedFile);
        assertThat(readBytes).isEqualTo(pngBytes);
    }

    private byte[] createTestImageBytes(String format, int width, int height) throws IOException {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.BLUE);
        g.fillRect(0, 0, width, height);
        g.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, format, baos);
        return baos.toByteArray();
    }
}
