package com.ntdhtcct.domain.diary;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.ntdhtcct.common.exception.DiaryDateLockedException;
import com.ntdhtcct.domain.diary.dto.DiaryImageResponse;
import com.ntdhtcct.domain.sitediary.dto.SiteDiaryUpdateRequest;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
class DiaryEntryJpaTest {

    @Autowired
    private DiaryEntryRepository diaryEntryRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private DiaryService diaryService;

    @Autowired
    private DiaryImageRepository diaryImageRepository;

    @Autowired
    private com.ntdhtcct.domain.project.ProjectRepository projectRepository;

    @Autowired
    private com.ntdhtcct.domain.sitediary.SiteDiaryRepository siteDiaryRepository;

    @Autowired
    private com.ntdhtcct.domain.sitediary.SiteDiarySyncService siteDiarySyncService;

    @Autowired
    private com.ntdhtcct.domain.sitediary.SiteDiaryLockRepository siteDiaryLockRepository;

    @Test
    @Transactional
    @DisplayName("Persistable: Lưu DiaryEntry với ID được gán trước (bridge từ SiteDiary) thành công mà không ném StaleObjectStateException")
    void testSaveBridgedDiaryEntryWithAssignedId_Success() {
        UUID assignedId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();

        DiaryEntry bridged = new DiaryEntry(
                projectId,
                authorId,
                LocalDate.now(),
                "Nhật ký thi công cầu nối",
                "Nội dung thi công",
                "Nắng",
                null
        );
        bridged.setId(assignedId);
        bridged.setNew(true);

        DiaryEntry saved = diaryEntryRepository.save(bridged);
        entityManager.flush();

        assertThat(saved).isNotNull();
        assertThat(saved.getId()).isEqualTo(assignedId);
        assertThat(saved.isNew()).isFalse();

        DiaryEntry found = diaryEntryRepository.findById(assignedId).orElse(null);
        assertThat(found).isNotNull();
        assertThat(found.getId()).isEqualTo(assignedId);
    }

    @Test
    @DisplayName("Real JPA End-to-End: uploadDisplayImage và uploadOriginalImage qua DiaryService với persistence context thực tế")
    void testUploadDisplayAndOriginalImage_RealJpa_StatusCompleted() throws Exception {
        com.ntdhtcct.domain.project.Project project = new com.ntdhtcct.domain.project.Project("PRJ-JPA-01", "Dự án JPA Test");
        project = projectRepository.save(project);
        UUID projectId = project.getId();

        UUID authorId = UUID.randomUUID();
        LocalDate today = LocalDate.of(2026, 10, 16);
        com.ntdhtcct.domain.sitediary.SiteDiary siteDiary = new com.ntdhtcct.domain.sitediary.SiteDiary(
                projectId,
                authorId,
                today,
                "Nắng",
                "32°C",
                10,
                "",
                "Thi công trụ T1",
                "",
                "sync-jpa-test-01",
                null
        );
        siteDiary = siteDiaryRepository.save(siteDiary);
        UUID siteDiaryId = siteDiary.getId();

        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(10, 10, java.awt.image.BufferedImage.TYPE_INT_RGB);
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(img, "jpg", baos);
        byte[] validJpgBytes = baos.toByteArray();

        org.springframework.mock.web.MockMultipartFile displayFile =
                new org.springframework.mock.web.MockMultipartFile("file", "test.jpg", "image/jpeg", validJpgBytes);

        String clientImgId = "client-img-real-1";
        DiaryImageResponse displayRes = diaryService.uploadDisplayImage(projectId, siteDiaryId, clientImgId, "test.jpg", displayFile);

        assertThat(displayRes).isNotNull();
        assertThat(displayRes.displayStatus()).isEqualTo("COMPLETED");

        DiaryImage loadedAfterDisplay = diaryImageRepository.findById(displayRes.id()).orElseThrow();
        assertThat(loadedAfterDisplay.getDisplayStatus()).isEqualTo("COMPLETED");
        assertThat(loadedAfterDisplay.getDisplayPath()).isNotNull();

        org.springframework.mock.web.MockMultipartFile origFile =
                new org.springframework.mock.web.MockMultipartFile("file", "test_orig.jpg", "image/jpeg", validJpgBytes);

        DiaryImageResponse origRes = diaryService.uploadOriginalImage(projectId, siteDiaryId, clientImgId, "test.jpg", origFile);

        assertThat(origRes).isNotNull();
        assertThat(origRes.originalStatus()).isEqualTo("COMPLETED");
        assertThat(origRes.displayStatus()).isEqualTo("COMPLETED");

        DiaryImage loadedAfterBoth = diaryImageRepository.findById(displayRes.id()).orElseThrow();
        assertThat(loadedAfterBoth.getOriginalStatus()).isEqualTo("COMPLETED");
        assertThat(loadedAfterBoth.getDisplayStatus()).isEqualTo("COMPLETED");
        assertThat(loadedAfterBoth.getDisplayPath()).isNotNull();
        assertThat(loadedAfterBoth.getOriginalPath()).isNotNull();
    }

    @Test
    @DisplayName("Real JPA End-to-End: Hai luồng đồng thời upload display và original cho cùng clientImageId")
    void testConcurrentDisplayAndOriginalUpload_RealJpa() throws Exception {
        com.ntdhtcct.domain.project.Project project = new com.ntdhtcct.domain.project.Project("PRJ-JPA-02", "Dự án JPA Concurrency");
        project = projectRepository.save(project);
        UUID projectId = project.getId();

        UUID authorId = UUID.randomUUID();
        LocalDate today = LocalDate.of(2026, 10, 16);
        com.ntdhtcct.domain.sitediary.SiteDiary siteDiary = new com.ntdhtcct.domain.sitediary.SiteDiary(
                projectId,
                authorId,
                today,
                "Nắng",
                "32°C",
                10,
                "",
                "Thi công trụ T2",
                "",
                "sync-jpa-test-02",
                null
        );
        siteDiary = siteDiaryRepository.save(siteDiary);
        UUID siteDiaryId = siteDiary.getId();

        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(10, 10, java.awt.image.BufferedImage.TYPE_INT_RGB);
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(img, "jpg", baos);
        byte[] validJpgBytes = baos.toByteArray();

        String clientImgId = "client-img-real-concurrent";

        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(2);
        java.util.concurrent.CountDownLatch startLatch = new java.util.concurrent.CountDownLatch(1);

        java.util.concurrent.Future<DiaryImageResponse> displayFuture = executor.submit(() -> {
            startLatch.await();
            org.springframework.mock.web.MockMultipartFile displayFile =
                    new org.springframework.mock.web.MockMultipartFile("file", "test.jpg", "image/jpeg", validJpgBytes);
            return diaryService.uploadDisplayImage(projectId, siteDiaryId, clientImgId, "test.jpg", displayFile);
        });

        java.util.concurrent.Future<DiaryImageResponse> origFuture = executor.submit(() -> {
            startLatch.await();
            org.springframework.mock.web.MockMultipartFile origFile =
                    new org.springframework.mock.web.MockMultipartFile("file", "test_orig.jpg", "image/jpeg", validJpgBytes);
            return diaryService.uploadOriginalImage(projectId, siteDiaryId, clientImgId, "test.jpg", origFile);
        });

        startLatch.countDown();
        executor.shutdown();
        assertThat(executor.awaitTermination(10, java.util.concurrent.TimeUnit.SECONDS)).isTrue();

        DiaryImageResponse displayRes = displayFuture.get();
        DiaryImageResponse origRes = origFuture.get();

        assertThat(displayRes).isNotNull();
        assertThat(origRes).isNotNull();
        assertThat(displayRes.id()).isEqualTo(origRes.id());

        DiaryImage loaded = diaryImageRepository.findById(displayRes.id()).orElseThrow();
        assertThat(loaded.getDisplayStatus()).isEqualTo("COMPLETED");
        assertThat(loaded.getOriginalStatus()).isEqualTo("COMPLETED");
        assertThat(loaded.getDisplayPath()).isNotNull();
        assertThat(loaded.getOriginalPath()).isNotNull();
    }

    @Test
    @DisplayName("Robustness: Bridge SiteDiary với workSummary > 255 ký tự và weather > 50 ký tự sang DiaryEntry thành công không ném lỗi Bean Validation")
    void testBridgeWithLongWorkSummaryAndWeather_Success() throws Exception {
        com.ntdhtcct.domain.project.Project project = new com.ntdhtcct.domain.project.Project("PRJ-LONG-01", "Dự án Long Text Test");
        project = projectRepository.save(project);
        UUID projectId = project.getId();

        UUID authorId = UUID.randomUUID();
        LocalDate today = LocalDate.of(2026, 10, 17);

        String longSummary = "Hôm nay tiến hành đổ bê tông dầm sàn tầng 3 theo đúng biện pháp kỹ thuật được phê duyệt. " +
                "Đội ngũ kỹ sư hiện trường đã kiểm tra nghiệm thu cốt thép, cốp pha dầm D1 đến D8, độ sụt bê tông đạt 12±2 cm. " +
                "Công tác an toàn lao động và vệ sinh môi trường được đảm bảo xuyên suốt ca làm việc kéo dài đến 18h30 cùng ngày.";
        assertThat(longSummary.length()).isGreaterThan(255);

        String longWeather = "Trời nhiều mây, có lúc có mưa rào rải rác và dông, nhiệt độ dao động 28-34 độ C";
        assertThat(longWeather.length()).isGreaterThan(50);

        com.ntdhtcct.domain.sitediary.SiteDiary siteDiary = new com.ntdhtcct.domain.sitediary.SiteDiary(
                projectId,
                authorId,
                today,
                longWeather,
                "32°C",
                15,
                "",
                longSummary,
                "",
                "sync-long-test-01",
                null
        );
        siteDiary = siteDiaryRepository.save(siteDiary);
        UUID siteDiaryId = siteDiary.getId();

        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(10, 10, java.awt.image.BufferedImage.TYPE_INT_RGB);
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(img, "jpg", baos);
        byte[] validJpgBytes = baos.toByteArray();

        org.springframework.mock.web.MockMultipartFile displayFile =
                new org.springframework.mock.web.MockMultipartFile("file", "test_long.jpg", "image/jpeg", validJpgBytes);

        DiaryImageResponse res = diaryService.uploadDisplayImage(projectId, siteDiaryId, "client-img-long-1", "test_long.jpg", displayFile);
        assertThat(res).isNotNull();

        DiaryEntry bridged = diaryEntryRepository.findById(siteDiaryId).orElseThrow();
        assertThat(bridged.getTitle()).isNotNull();
        assertThat(bridged.getTitle().length()).isLessThanOrEqualTo(255);
        assertThat(bridged.getWeather()).isNotNull();
        assertThat(bridged.getWeather().length()).isLessThanOrEqualTo(50);
        assertThat(bridged.getContent()).isEqualTo(longSummary);
    }

    @Test
    @DisplayName("Concurrency: SiteDiarySyncService.updateDiary và DiaryService.uploadDisplayImage chạy đồng thời không gây xung đột transaction hay rollback")
    void testConcurrentUpdateDiaryAndUploadImage_MaintainsConsistencyAndNoRollback() throws Exception {
        com.ntdhtcct.domain.project.Project project = new com.ntdhtcct.domain.project.Project("PRJ-CONCUR-03", "Dự án Concurrency Update & Upload");
        project = projectRepository.save(project);
        UUID projectId = project.getId();

        UUID authorId = UUID.randomUUID();
        LocalDate today = LocalDate.of(2026, 10, 17);
        com.ntdhtcct.domain.sitediary.SiteDiary siteDiary = new com.ntdhtcct.domain.sitediary.SiteDiary(
                projectId,
                authorId,
                today,
                "Nắng ráo",
                "30°C",
                8,
                "",
                "Thi công ban đầu",
                "",
                "sync-concurrent-update-upload",
                null
        );
        siteDiary = siteDiaryRepository.save(siteDiary);
        UUID siteDiaryId = siteDiary.getId();

        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(10, 10, java.awt.image.BufferedImage.TYPE_INT_RGB);
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(img, "jpg", baos);
        byte[] validJpgBytes = baos.toByteArray();

        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(2);
        java.util.concurrent.CountDownLatch startLatch = new java.util.concurrent.CountDownLatch(1);

        String updatedSummary = "Đã cập nhật tiến độ thi công hoàn thành toàn bộ móng trụ M1";
        java.util.concurrent.Future<com.ntdhtcct.domain.sitediary.dto.SiteDiaryResponse> updateFuture = executor.submit(() -> {
            startLatch.await();
            SiteDiaryUpdateRequest req = new SiteDiaryUpdateRequest(
                    today, "Nắng đẹp", "31°C", 2, 10, 2, "Đội cốp pha", "Máy trộn hoạt động tốt", "Bình thường", updatedSummary, ""
            );
            return siteDiarySyncService.updateDiary(projectId, siteDiaryId, req).orElseThrow();
        });

        java.util.concurrent.Future<DiaryImageResponse> uploadFuture = executor.submit(() -> {
            startLatch.await();
            org.springframework.mock.web.MockMultipartFile displayFile =
                    new org.springframework.mock.web.MockMultipartFile("file", "concurrent_up.jpg", "image/jpeg", validJpgBytes);
            return diaryService.uploadDisplayImage(projectId, siteDiaryId, "img-concur-up-1", "concurrent_up.jpg", displayFile);
        });

        startLatch.countDown();
        executor.shutdown();
        assertThat(executor.awaitTermination(10, java.util.concurrent.TimeUnit.SECONDS)).isTrue();

        com.ntdhtcct.domain.sitediary.dto.SiteDiaryResponse updateRes = updateFuture.get();
        DiaryImageResponse uploadRes = uploadFuture.get();

        assertThat(updateRes).isNotNull();
        assertThat(uploadRes).isNotNull();

        // Kiểm tra tính nhất quán giữa SiteDiary và DiaryEntry
        com.ntdhtcct.domain.sitediary.SiteDiary finalSiteDiary = siteDiaryRepository.findById(siteDiaryId).orElseThrow();
        DiaryEntry finalDiaryEntry = diaryEntryRepository.findById(siteDiaryId).orElseThrow();

        assertThat(finalSiteDiary.getWorkSummary()).isEqualTo(updatedSummary);
        assertThat(finalDiaryEntry.getContent()).isEqualTo(updatedSummary);
    }

    @Test
    @DisplayName("S-24 Date Lock: Thao tác tải ảnh bị từ chối với DiaryDateLockedException khi ngày mục nhật ký bị khóa sổ")
    void testUploadImage_WhenDateIsLocked_RejectsWithDiaryDateLockedException() throws Exception {
        com.ntdhtcct.domain.project.Project project = new com.ntdhtcct.domain.project.Project("PRJ-LOCK-01", "Dự án Lock Test");
        project = projectRepository.save(project);
        UUID projectId = project.getId();

        UUID authorId = UUID.randomUUID();
        LocalDate lockedDate = LocalDate.of(2026, 10, 10);

        // Khóa ngày này trong cơ sở dữ liệu
        com.ntdhtcct.domain.sitediary.SiteDiaryLock lock = new com.ntdhtcct.domain.sitediary.SiteDiaryLock(
                projectId, lockedDate, authorId, "Đã nghiệm thu xong toàn bộ công tác ngày 10/10"
        );
        siteDiaryLockRepository.save(lock);

        com.ntdhtcct.domain.sitediary.SiteDiary siteDiary = new com.ntdhtcct.domain.sitediary.SiteDiary(
                projectId,
                authorId,
                lockedDate,
                "Nắng",
                "30°C",
                5,
                "",
                "Nhật ký ngày đã bị khóa",
                "",
                "sync-locked-test-01",
                null
        );
        siteDiary = siteDiaryRepository.save(siteDiary);
        UUID siteDiaryId = siteDiary.getId();

        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(10, 10, java.awt.image.BufferedImage.TYPE_INT_RGB);
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(img, "jpg", baos);
        byte[] validJpgBytes = baos.toByteArray();

        org.springframework.mock.web.MockMultipartFile displayFile =
                new org.springframework.mock.web.MockMultipartFile("file", "test_locked.jpg", "image/jpeg", validJpgBytes);

        assertThrows(DiaryDateLockedException.class, () ->
                diaryService.uploadDisplayImage(projectId, siteDiaryId, "img-locked-1", "test_locked.jpg", displayFile)
        );
    }
}
