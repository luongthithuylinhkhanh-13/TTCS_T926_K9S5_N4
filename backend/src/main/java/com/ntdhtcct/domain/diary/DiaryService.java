package com.ntdhtcct.domain.diary;

import com.ntdhtcct.common.exception.ResourceNotFoundException;
import com.ntdhtcct.domain.diary.dto.CreateDiaryEntryRequest;
import com.ntdhtcct.domain.diary.dto.DiaryEntryResponse;
import com.ntdhtcct.domain.project.ProjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class DiaryService {

    private final DiaryEntryRepository diaryEntryRepository;
    private final ProjectRepository projectRepository;

    public DiaryService(DiaryEntryRepository diaryEntryRepository, ProjectRepository projectRepository) {
        this.diaryEntryRepository = diaryEntryRepository;
        this.projectRepository = projectRepository;
    }

    @Transactional
    public DiaryEntryResponse createDiaryEntry(UUID projectId, CreateDiaryEntryRequest request, UUID authorId) {
        if (!projectRepository.existsById(projectId)) {
            throw new ResourceNotFoundException("PROJECT_NOT_FOUND", "Dự án không tồn tại");
        }

        DiaryEntry diaryEntry = new DiaryEntry(
                projectId,
                authorId,
                request.entryDate(),
                request.title(),
                request.content(),
                request.weather(),
                request.wbsItemId()
        );

        DiaryEntry saved = diaryEntryRepository.save(diaryEntry);

        return new DiaryEntryResponse(
                saved.getId(),
                saved.getProjectId(),
                saved.getWbsItemId(),
                saved.getAuthorId(),
                saved.getEntryDate(),
                saved.getTitle(),
                saved.getContent(),
                saved.getWeather(),
                saved.getCreatedAt(),
                saved.getUpdatedAt()
        );
    }
}
