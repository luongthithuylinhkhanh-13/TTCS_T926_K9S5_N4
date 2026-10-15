package com.ntdhtcct.domain.diary;

import com.ntdhtcct.common.exception.BadRequestException;
import com.ntdhtcct.common.exception.ResourceNotFoundException;
import com.ntdhtcct.domain.diary.dto.CreateDiaryEntryRequest;
import com.ntdhtcct.domain.diary.dto.DiaryEntryResponse;
import com.ntdhtcct.domain.diary.dto.DiaryImageResponse;
import com.ntdhtcct.domain.project.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import com.ntdhtcct.domain.sitediary.SiteDiary;
import org.mockito.ArgumentCaptor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DiaryServiceTest {

    @Mock
    private DiaryEntryRepository diaryEntryRepository;

    @Mock
    private DiaryImageRepository diaryImageRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private com.ntdhtcct.domain.sitediary.SiteDiaryRepository siteDiaryRepository;

    @Mock
    private com.ntdhtcct.domain.sitediary.SiteDiaryLockService siteDiaryLockService;

    @Mock
    private com.ntdhtcct.domain.sitediary.SiteDiaryAdvisoryLockManager advisoryLockManager;

    @InjectMocks
    private DiaryService diaryService;

    private UUID projectId;
    private UUID authorId;
    private UUID entryId;
    private DiaryEntry entry;

    @BeforeEach
    void setUp() throws Exception {
        projectId = UUID.randomUUID();
        authorId = UUID.randomUUID();
        entryId = UUID.randomUUID();

        entry = new DiaryEntry(
                projectId,
                authorId,
                LocalDate.of(2026, 10, 11),
                "Nhật ký thi công",
                "Nội dung thi công",
                "Nắng",
                null
        );
        Field idField = DiaryEntry.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(entry, entryId);
    }

    @Test
    void createDiaryEntry_Success() {
        CreateDiaryEntryRequest request = new CreateDiaryEntryRequest(
                null,
                LocalDate.of(2026, 10, 11),
                "Nhật ký thi công ngày 11/10",
                "Đổ bê tông sàn tầng 2",
                "Nắng nhẹ"
        );

        when(projectRepository.existsById(projectId)).thenReturn(true);
        when(diaryEntryRepository.save(any(DiaryEntry.class))).thenAnswer(invocation -> {
            DiaryEntry e = invocation.getArgument(0);
            Field idF = DiaryEntry.class.getDeclaredField("id");
            idF.setAccessible(true);
            idF.set(e, UUID.randomUUID());
            return e;
        });

        DiaryEntryResponse response = diaryService.createDiaryEntry(projectId, request, authorId);

        assertThat(response).isNotNull();
        assertThat(response.id()).isNotNull();
        assertThat(response.projectId()).isEqualTo(projectId);
        assertThat(response.authorId()).isEqualTo(authorId);
        assertThat(response.title()).isEqualTo("Nhật ký thi công ngày 11/10");
        assertThat(response.weather()).isEqualTo("Nắng nhẹ");
        verify(diaryEntryRepository).save(any(DiaryEntry.class));
    }

    @Test
    void createDiaryEntry_ProjectNotFound_ThrowsException() {
        CreateDiaryEntryRequest request = new CreateDiaryEntryRequest(
                null,
                LocalDate.of(2026, 10, 11),
                "Tiêu đề",
                "Nội dung",
                "Nắng"
        );

        when(projectRepository.existsById(projectId)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () ->
                diaryService.createDiaryEntry(projectId, request, authorId)
        );
    }

    @Test
    void uploadDisplayImage_RejectsFiles1MiBOrGreater() {
        MockMultipartFile file1MiB = new MockMultipartFile(
                "file",
                "photo.jpg",
                "image/jpeg",
                new byte[1024 * 1024] // đúng 1,048,576 bytes
        );

        when(diaryEntryRepository.findById(entryId)).thenReturn(Optional.of(entry));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                diaryService.uploadDisplayImage(projectId, entryId, "img-1", "photo.jpg", file1MiB)
        );
        assertThat(ex.getCode()).isEqualTo("FILE_TOO_LARGE");
    }

    @Test
    void uploadOriginalImage_RejectsFilesOver20MiB() {
        MockMultipartFile file21MiB = new MockMultipartFile(
                "file",
                "raw.jpg",
                "image/jpeg",
                new byte[20 * 1024 * 1024 + 1] // > 20 MiB
        );

        when(diaryEntryRepository.findById(entryId)).thenReturn(Optional.of(entry));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                diaryService.uploadOriginalImage(projectId, entryId, "img-1", "raw.jpg", file21MiB)
        );
        assertThat(ex.getCode()).isEqualTo("FILE_TOO_LARGE");
    }

    @Test
    void uploadDisplayImage_PreventsOldAttemptFromOverwritingNewResult() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "photo.jpg",
                "image/jpeg",
                new byte[500 * 1024] // 500 KB < 1 MiB
        );

        DiaryImage image = new DiaryImage(entryId, "img-1", "photo.jpg", "image/jpeg");
        image.setId(UUID.randomUUID());

        when(diaryEntryRepository.findById(entryId)).thenReturn(Optional.of(entry));
        when(diaryImageRepository.findByDiaryEntryIdAndClientImageId(entryId, "img-1"))
                .thenReturn(Optional.of(image));
        when(fileStorageService.storeDisplayFile(eq(entryId), eq("img-1"), any(), any()))
                .thenReturn(new FileStorageService.StoredDisplayResult("p", "tp", 512000L, 800, 600));

        // Giả lập attempt cũ không match vì đã có attempt mới hơn can thiệp
        when(diaryImageRepository.updateDisplayCompletedIfAttemptMatches(
                eq(image.getId()), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(0);
        when(diaryImageRepository.findById(image.getId())).thenReturn(Optional.of(image));

        DiaryImageResponse response = diaryService.uploadDisplayImage(projectId, entryId, "img-1", "photo.jpg", file);

        assertThat(response).isNotNull();
        verify(diaryImageRepository).updateDisplayCompletedIfAttemptMatches(eq(image.getId()), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void getDisplayImageResource_RejectsWhenNotCompleted() {
        DiaryImage image = new DiaryImage(entryId, "img-1", "photo.jpg", "image/jpeg");
        image.setId(UUID.randomUUID());
        image.setDisplayStatus("UPLOADING"); // Chưa COMPLETED

        when(diaryEntryRepository.findById(entryId)).thenReturn(Optional.of(entry));
        when(diaryImageRepository.findById(image.getId())).thenReturn(Optional.of(image));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                diaryService.getDisplayImageResource(projectId, entryId, image.getId(), false)
        );
        assertThat(ex.getCode()).isEqualTo("IMAGE_NOT_READY");
    }

    @Test
    void getOriginalImageResource_RejectsWhenNotCompleted() {
        DiaryImage image = new DiaryImage(entryId, "img-1", "photo.jpg", "image/jpeg");
        image.setId(UUID.randomUUID());
        image.setOriginalStatus("FAILED"); // Chưa COMPLETED

        when(diaryEntryRepository.findById(entryId)).thenReturn(Optional.of(entry));
        when(diaryImageRepository.findById(image.getId())).thenReturn(Optional.of(image));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                diaryService.getOriginalImageResource(projectId, entryId, image.getId())
        );
        assertThat(ex.getCode()).isEqualTo("IMAGE_NOT_READY");
    }

    @Test
    void validateEntryBelongsToProject_RejectsUnauthorizedEntry() {
        UUID wrongProjectId = UUID.randomUUID();
        when(diaryEntryRepository.findById(entryId)).thenReturn(Optional.of(entry));

        assertThrows(ResourceNotFoundException.class, () ->
                diaryService.getOriginalImageResource(wrongProjectId, entryId, UUID.randomUUID())
        );
    }

    @Test
    void getDiaryImages_ReturnsOrderedImageList() {
        DiaryImage img1 = new DiaryImage(entryId, "img-1", "p1.jpg", "image/jpeg");
        img1.setId(UUID.randomUUID());
        img1.setDisplayStatus("COMPLETED");

        DiaryImage img2 = new DiaryImage(entryId, "img-2", "p2.jpg", "image/jpeg");
        img2.setId(UUID.randomUUID());
        img2.setDisplayStatus("COMPLETED");

        when(diaryEntryRepository.findById(entryId)).thenReturn(Optional.of(entry));
        when(diaryImageRepository.findByDiaryEntryIdOrderBySortOrderAscCreatedAtAsc(entryId))
                .thenReturn(java.util.List.of(img1, img2));

        java.util.List<DiaryImageResponse> list = diaryService.getDiaryImages(projectId, entryId);

        assertThat(list).hasSize(2);
        assertThat(list.get(0).clientImageId()).isEqualTo("img-1");
        assertThat(list.get(1).clientImageId()).isEqualTo("img-2");
    }

    @Test
    void createDiaryEntry_RejectsWhenDateLocked() {
        CreateDiaryEntryRequest request = new CreateDiaryEntryRequest(
                null,
                LocalDate.of(2026, 10, 11),
                "Tiêu đề",
                "Nội dung",
                "Nắng"
        );
        when(projectRepository.existsById(projectId)).thenReturn(true);
        when(siteDiaryLockService.isDateLocked(projectId, LocalDate.of(2026, 10, 11))).thenReturn(true);

        com.ntdhtcct.common.exception.DiaryDateLockedException ex = assertThrows(
                com.ntdhtcct.common.exception.DiaryDateLockedException.class,
                () -> diaryService.createDiaryEntry(projectId, request, authorId)
        );
        assertThat(ex.getCode()).isEqualTo("DIARY_DATE_LOCKED");
    }

    @Test
    void uploadDisplayImage_RejectsWhenDateLocked() {
        when(diaryEntryRepository.findById(entryId)).thenReturn(Optional.of(entry));
        when(siteDiaryLockService.isDateLocked(projectId, entry.getEntryDate())).thenReturn(true);

        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", new byte[]{1, 2});

        com.ntdhtcct.common.exception.DiaryDateLockedException ex = assertThrows(
                com.ntdhtcct.common.exception.DiaryDateLockedException.class,
                () -> diaryService.uploadDisplayImage(projectId, entryId, "client-img-1", "test.jpg", file)
        );
        assertThat(ex.getCode()).isEqualTo("DIARY_DATE_LOCKED");
    }

    @Test
    void uploadOriginalImage_RejectsWhenDateLocked() {
        when(diaryEntryRepository.findById(entryId)).thenReturn(Optional.of(entry));
        when(siteDiaryLockService.isDateLocked(projectId, entry.getEntryDate())).thenReturn(true);

        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", new byte[]{1, 2});

        com.ntdhtcct.common.exception.DiaryDateLockedException ex = assertThrows(
                com.ntdhtcct.common.exception.DiaryDateLockedException.class,
                () -> diaryService.uploadOriginalImage(projectId, entryId, "client-img-1", "test.jpg", file)
        );
        assertThat(ex.getCode()).isEqualTo("DIARY_DATE_LOCKED");
    }

    @Test
    void deleteDiaryImage_SuccessWhenNotLocked() {
        when(diaryEntryRepository.findById(entryId)).thenReturn(Optional.of(entry));
        when(siteDiaryLockService.isDateLocked(projectId, entry.getEntryDate())).thenReturn(false);

        DiaryImage image = new DiaryImage(entryId, "img-1", "photo.jpg", "image/jpeg");
        image.setId(UUID.randomUUID());
        image.setDisplayPath("e/display.jpg");
        image.setThumbnailPath("e/thumb.jpg");
        image.setOriginalPath("e/orig.bin");

        when(diaryImageRepository.findById(image.getId())).thenReturn(Optional.of(image));

        diaryService.deleteDiaryImage(projectId, entryId, image.getId());

        verify(fileStorageService).deleteFile("e/thumb.jpg");
        verify(fileStorageService).deleteFile("e/display.jpg");
        verify(fileStorageService).deleteFile("e/orig.bin");
        verify(diaryImageRepository).delete(image);
    }

    @Test
    void deleteDiaryImage_RejectsWhenDateLocked() {
        when(diaryEntryRepository.findById(entryId)).thenReturn(Optional.of(entry));
        when(siteDiaryLockService.isDateLocked(projectId, entry.getEntryDate())).thenReturn(true);

        UUID imageId = UUID.randomUUID();

        com.ntdhtcct.common.exception.DiaryDateLockedException ex = assertThrows(
                com.ntdhtcct.common.exception.DiaryDateLockedException.class,
                () -> diaryService.deleteDiaryImage(projectId, entryId, imageId)
        );
        assertThat(ex.getCode()).isEqualTo("DIARY_DATE_LOCKED");
    }

    @Test
    void uploadDisplayImage_WhenEntryIsSiteDiary_BridgesToDiaryEntrySuccessfully() throws IOException {
        UUID siteDiaryId = UUID.randomUUID();
        LocalDate diaryDate = LocalDate.of(2026, 10, 15);
        SiteDiary siteDiary = new SiteDiary(
                projectId,
                authorId,
                diaryDate,
                "Nắng ráo",
                "30°C",
                25,
                "Máy đào",
                "Đổ bê tông móng trụ P1",
                "Không",
                "sync-client-01",
                null
        );
        siteDiary.setId(siteDiaryId);

        when(diaryEntryRepository.findById(siteDiaryId)).thenReturn(Optional.empty());
        when(siteDiaryRepository.findById(siteDiaryId)).thenReturn(Optional.of(siteDiary));
        when(siteDiaryLockService.isDateLocked(projectId, diaryDate)).thenReturn(false);

        when(diaryEntryRepository.save(any(DiaryEntry.class))).thenAnswer(inv -> inv.getArgument(0));

        DiaryImage image = new DiaryImage(siteDiaryId, "img-bridge-1", "bridge.jpg", "image/jpeg");
        image.setId(UUID.randomUUID());
        when(diaryImageRepository.findByDiaryEntryIdAndClientImageId(eq(siteDiaryId), eq("img-bridge-1")))
                .thenReturn(Optional.empty());
        when(diaryImageRepository.save(any(DiaryImage.class))).thenReturn(image);
        when(fileStorageService.storeDisplayFile(any(), any(), any(), any()))
                .thenReturn(new FileStorageService.StoredDisplayResult("p", "tp", 1000L, 800, 600));
        when(diaryImageRepository.updateDisplayCompletedIfAttemptMatches(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(1);
        when(diaryImageRepository.findById(image.getId())).thenReturn(Optional.of(image));

        MockMultipartFile file = new MockMultipartFile("file", "bridge.jpg", "image/jpeg", new byte[]{1, 2, 3});

        DiaryImageResponse response = diaryService.uploadDisplayImage(projectId, siteDiaryId, "img-bridge-1", "bridge.jpg", file);

        assertThat(response).isNotNull();
        ArgumentCaptor<DiaryEntry> entryCaptor = ArgumentCaptor.forClass(DiaryEntry.class);
        verify(diaryEntryRepository).save(entryCaptor.capture());
        DiaryEntry savedEntry = entryCaptor.getValue();
        assertThat(savedEntry.getId()).isEqualTo(siteDiaryId);
        assertThat(savedEntry.isNew()).isTrue();
        assertThat(savedEntry.getEntryDate()).isEqualTo(diaryDate);
        assertThat(savedEntry.getTitle()).isEqualTo("Đổ bê tông móng trụ P1");
    }

    @Test
    void uploadDisplayImage_WhenEntryIsSiteDiaryOnLockedDate_RejectsAndDoesNotBridge() {
        UUID siteDiaryId = UUID.randomUUID();
        LocalDate diaryDate = LocalDate.of(2026, 10, 15);
        SiteDiary siteDiary = new SiteDiary(
                projectId,
                authorId,
                diaryDate,
                "Nắng",
                "30°C",
                25,
                "",
                "Nội dung thi công",
                "",
                "sync-locked-01",
                null
        );
        siteDiary.setId(siteDiaryId);

        when(diaryEntryRepository.findById(siteDiaryId)).thenReturn(Optional.empty());
        when(siteDiaryRepository.findById(siteDiaryId)).thenReturn(Optional.of(siteDiary));
        when(siteDiaryLockService.isDateLocked(projectId, diaryDate)).thenReturn(true);

        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", new byte[]{1, 2});

        com.ntdhtcct.common.exception.DiaryDateLockedException ex = assertThrows(
                com.ntdhtcct.common.exception.DiaryDateLockedException.class,
                () -> diaryService.uploadDisplayImage(projectId, siteDiaryId, "img-1", "test.jpg", file)
        );

        assertThat(ex.getCode()).isEqualTo("DIARY_DATE_LOCKED");
        verify(diaryEntryRepository, never()).save(any(DiaryEntry.class));
    }

    @Test
    void uploadDisplayImage_ConcurrentUploadsForSameSiteDiary_BridgesSafelyAndIdempotently() throws Exception {
        UUID siteDiaryId = UUID.randomUUID();
        LocalDate diaryDate = LocalDate.of(2026, 10, 15);
        SiteDiary siteDiary = new SiteDiary(
                projectId,
                authorId,
                diaryDate,
                "Nắng",
                "32°C",
                30,
                "Cẩu",
                "Lắp kết cấu thép",
                "",
                "sync-c-01",
                null
        );
        siteDiary.setId(siteDiaryId);

        java.util.concurrent.atomic.AtomicReference<DiaryEntry> bridgedRef = new java.util.concurrent.atomic.AtomicReference<>();
        when(diaryEntryRepository.findById(siteDiaryId)).thenAnswer(inv -> Optional.ofNullable(bridgedRef.get()));
        when(siteDiaryRepository.findById(siteDiaryId)).thenReturn(Optional.of(siteDiary));
        when(siteDiaryLockService.isDateLocked(projectId, diaryDate)).thenReturn(false);

        when(diaryEntryRepository.save(any(DiaryEntry.class))).thenAnswer(inv -> {
            DiaryEntry e = inv.getArgument(0);
            bridgedRef.set(e);
            return e;
        });

        when(fileStorageService.storeDisplayFile(any(), any(), any(), any()))
                .thenReturn(new FileStorageService.StoredDisplayResult("p", "tp", 1000L, 800, 600));
        when(diaryImageRepository.updateDisplayCompletedIfAttemptMatches(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(1);

        java.util.concurrent.ConcurrentHashMap<String, DiaryImage> imageStore = new java.util.concurrent.ConcurrentHashMap<>();
        when(diaryImageRepository.findByDiaryEntryIdAndClientImageId(eq(siteDiaryId), any()))
                .thenAnswer(inv -> Optional.ofNullable(imageStore.get(inv.getArgument(1))));
        when(diaryImageRepository.save(any(DiaryImage.class))).thenAnswer(inv -> {
            DiaryImage img = inv.getArgument(0);
            if (img.getId() == null) {
                img.setId(UUID.randomUUID());
            }
            imageStore.put(img.getClientImageId(), img);
            return img;
        });
        when(diaryImageRepository.findById(any())).thenAnswer(inv -> Optional.ofNullable(
                imageStore.values().stream().filter(i -> i.getId().equals(inv.getArgument(0))).findFirst().orElse(null)
        ));

        int threadCount = 5;
        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(threadCount);
        java.util.concurrent.CountDownLatch startLatch = new java.util.concurrent.CountDownLatch(1);
        java.util.List<java.util.concurrent.Future<DiaryImageResponse>> futures = new java.util.ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            final String clientImgId = "concurrent-img-" + i;
            futures.add(executor.submit(() -> {
                startLatch.await();
                MockMultipartFile file = new MockMultipartFile("file", clientImgId + ".jpg", "image/jpeg", new byte[]{1, 2});
                return diaryService.uploadDisplayImage(projectId, siteDiaryId, clientImgId, clientImgId + ".jpg", file);
            }));
        }

        startLatch.countDown();
        executor.shutdown();
        assertThat(executor.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)).isTrue();

        for (java.util.concurrent.Future<DiaryImageResponse> future : futures) {
            DiaryImageResponse res = future.get();
            assertThat(res).isNotNull();
        }

        // Cầu nối DiaryEntry chỉ được tạo và lưu DUY NHẤT 1 LẦN (idempotent)
        verify(diaryEntryRepository, times(1)).save(any(DiaryEntry.class));
        assertThat(imageStore).hasSize(threadCount);
    }

    @Test
    @DisplayName("S-23 / Concurrency: Upload song song ảnh hiển thị và ảnh gốc cho cùng một clientImageId hoạt động an toàn và hoàn tất cả hai giai đoạn")
    void uploadDisplayAndOriginalImage_ConcurrentlyForSameImage_CreatesSingleRecordAndCompletesBothStages() throws Exception {
        UUID siteDiaryId = UUID.randomUUID();
        LocalDate diaryDate = LocalDate.of(2026, 10, 15);
        SiteDiary siteDiary = new SiteDiary(
                projectId,
                authorId,
                diaryDate,
                "Nắng",
                "32°C",
                20,
                "Cẩu",
                "Thi công bê tông cốt thép",
                "",
                "sync-c-02",
                null
        );
        siteDiary.setId(siteDiaryId);

        entry.setEntryDate(diaryDate);
        entry.setId(siteDiaryId);
        when(diaryEntryRepository.findById(siteDiaryId)).thenReturn(Optional.of(entry));
        when(siteDiaryLockService.isDateLocked(projectId, diaryDate)).thenReturn(false);

        java.util.concurrent.atomic.AtomicReference<DiaryImage> imageRef = new java.util.concurrent.atomic.AtomicReference<>();
        String sharedClientImgId = "concurrent-display-orig-img";

        when(diaryImageRepository.findByDiaryEntryIdAndClientImageId(eq(siteDiaryId), eq(sharedClientImgId)))
                .thenAnswer(inv -> Optional.ofNullable(imageRef.get()));
        when(diaryImageRepository.save(any(DiaryImage.class))).thenAnswer(inv -> {
            DiaryImage img = inv.getArgument(0);
            if (img.getId() == null) {
                img.setId(UUID.randomUUID());
            }
            imageRef.set(img);
            return img;
        });
        when(diaryImageRepository.findById(any())).thenAnswer(inv -> Optional.ofNullable(imageRef.get()));

        when(fileStorageService.storeDisplayFile(any(), any(), any(), any()))
                .thenReturn(new FileStorageService.StoredDisplayResult("display/p.jpg", "thumb/tp.jpg", 500L, 800, 600));
        when(diaryImageRepository.updateDisplayCompletedIfAttemptMatches(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(1);

        when(fileStorageService.storeOriginalFile(any(), any(), any(), any()))
                .thenReturn(new FileStorageService.StoredOriginalResult("original/orig.jpg", 15000L));
        when(diaryImageRepository.updateOriginalCompletedIfAttemptMatches(any(), any(), any(), any(), any()))
                .thenReturn(1);

        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(2);
        java.util.concurrent.CountDownLatch startLatch = new java.util.concurrent.CountDownLatch(1);

        java.util.concurrent.Future<DiaryImageResponse> displayFuture = executor.submit(() -> {
            startLatch.await();
            MockMultipartFile displayFile = new MockMultipartFile("file", "img.jpg", "image/jpeg", new byte[]{1, 2});
            return diaryService.uploadDisplayImage(projectId, siteDiaryId, sharedClientImgId, "img.jpg", displayFile);
        });

        java.util.concurrent.Future<DiaryImageResponse> originalFuture = executor.submit(() -> {
            startLatch.await();
            MockMultipartFile origFile = new MockMultipartFile("file", "img_orig.jpg", "image/jpeg", new byte[]{1, 2, 3, 4});
            return diaryService.uploadOriginalImage(projectId, siteDiaryId, sharedClientImgId, "img.jpg", origFile);
        });

        startLatch.countDown();
        executor.shutdown();
        assertThat(executor.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)).isTrue();

        DiaryImageResponse displayRes = displayFuture.get();
        DiaryImageResponse originalRes = originalFuture.get();

        assertThat(displayRes).isNotNull();
        assertThat(originalRes).isNotNull();
        assertThat(displayRes.id()).isEqualTo(originalRes.id());
        assertThat(displayRes.clientImageId()).isEqualTo(sharedClientImgId);

        // DiaryImage chỉ được khởi tạo và lưu DUY NHẤT 1 LẦN cho cả hai giai đoạn
        verify(diaryImageRepository, times(1)).save(any(DiaryImage.class));
        verify(diaryImageRepository).updateDisplayCompletedIfAttemptMatches(any(), any(), any(), any(), any(), any(), any(), any());
        verify(diaryImageRepository).updateOriginalCompletedIfAttemptMatches(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Cluster Concurrency Fallback: Khi luồng khác/node khác đã tạo bridge và ném ngoại lệ xung đột, service tự động tải lại bản ghi đã commit thành công")
    void getOrCreateBridgedEntry_WhenClusterConflictOccurs_RecoversCommittedEntry() throws Exception {
        UUID siteDiaryId = UUID.randomUUID();
        LocalDate diaryDate = LocalDate.of(2026, 10, 15);
        SiteDiary siteDiary = new SiteDiary(
                projectId,
                authorId,
                diaryDate,
                "Nắng",
                "30°C",
                10,
                "",
                "Hạng mục thử nghiệm",
                "",
                "sync-cluster-01",
                null
        );
        siteDiary.setId(siteDiaryId);

        DiaryEntry committedEntry = new DiaryEntry(
                projectId, authorId, diaryDate, "Hạng mục thử nghiệm", "Nội dung", "Nắng", null
        );
        committedEntry.setId(siteDiaryId);

        when(siteDiaryRepository.findById(siteDiaryId)).thenReturn(Optional.of(siteDiary));
        when(siteDiaryLockService.isDateLocked(projectId, diaryDate)).thenReturn(false);

        java.util.concurrent.atomic.AtomicInteger findCallCount = new java.util.concurrent.atomic.AtomicInteger(0);
        when(diaryEntryRepository.findById(siteDiaryId)).thenAnswer(inv -> {
            int count = findCallCount.incrementAndGet();
            if (count >= 6) {
                return Optional.of(committedEntry);
            }
            return Optional.empty();
        });

        when(diaryEntryRepository.save(any(DiaryEntry.class))).thenThrow(
                new org.springframework.dao.DataIntegrityViolationException("duplicate key value violates unique constraint")
        );

        DiaryImage image = new DiaryImage(siteDiaryId, "img-fallback-1", "test.jpg", "image/jpeg");
        image.setId(UUID.randomUUID());
        when(diaryImageRepository.findByDiaryEntryIdAndClientImageId(any(), any())).thenReturn(Optional.of(image));
        when(diaryImageRepository.findById(any())).thenReturn(Optional.of(image));
        when(fileStorageService.storeDisplayFile(any(), any(), any(), any()))
                .thenReturn(new FileStorageService.StoredDisplayResult("p", "tp", 100L, 100, 100));
        when(diaryImageRepository.updateDisplayCompletedIfAttemptMatches(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(1);

        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", new byte[]{1, 2});

        DiaryImageResponse response = diaryService.uploadDisplayImage(projectId, siteDiaryId, "img-fallback-1", "test.jpg", file);
        assertThat(response).isNotNull();
    }
}
