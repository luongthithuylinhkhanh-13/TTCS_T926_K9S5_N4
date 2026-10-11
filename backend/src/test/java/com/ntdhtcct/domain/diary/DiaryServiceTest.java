package com.ntdhtcct.domain.diary;

import com.ntdhtcct.common.exception.ResourceNotFoundException;
import com.ntdhtcct.domain.diary.dto.CreateDiaryEntryRequest;
import com.ntdhtcct.domain.diary.dto.DiaryEntryResponse;
import com.ntdhtcct.domain.project.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DiaryServiceTest {

    @Mock
    private DiaryEntryRepository diaryEntryRepository;

    @Mock
    private ProjectRepository projectRepository;

    @InjectMocks
    private DiaryService diaryService;

    private UUID projectId;
    private UUID authorId;

    @BeforeEach
    void setUp() {
        projectId = UUID.randomUUID();
        authorId = UUID.randomUUID();
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
            DiaryEntry entry = invocation.getArgument(0);
            Field idField = DiaryEntry.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(entry, UUID.randomUUID());
            return entry;
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
}
