package com.ntdhtcct.domain.diary;

import com.ntdhtcct.common.exception.BadRequestException;
import com.ntdhtcct.common.exception.ResourceNotFoundException;
import com.ntdhtcct.domain.diary.dto.CreateDiaryEntryRequest;
import com.ntdhtcct.domain.diary.dto.DiaryEntryResponse;
import com.ntdhtcct.domain.diary.dto.DiaryImageResponse;
import com.ntdhtcct.domain.project.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
                eq(image.getId()), any(), any(), any(), any(), any(), any()))
                .thenReturn(0);
        when(diaryImageRepository.findById(image.getId())).thenReturn(Optional.of(image));

        DiaryImageResponse response = diaryService.uploadDisplayImage(projectId, entryId, "img-1", "photo.jpg", file);

        assertThat(response).isNotNull();
        verify(diaryImageRepository).updateDisplayCompletedIfAttemptMatches(eq(image.getId()), any(), any(), any(), any(), any(), any());
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
}
