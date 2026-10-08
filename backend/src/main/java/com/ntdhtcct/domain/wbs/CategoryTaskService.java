package com.ntdhtcct.domain.wbs;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class CategoryTaskService {

    private final TaskRepository taskRepository;
    private final WbsItemRepository wbsItemRepository;

    public CategoryTaskService(
            TaskRepository taskRepository,
            WbsItemRepository wbsItemRepository
    ) {
        this.taskRepository = taskRepository;
        this.wbsItemRepository = wbsItemRepository;
    }

    @Transactional(readOnly = true)
    public List<Task> getTasks(UUID projectId, UUID categoryId) {
        requireCategory(projectId, categoryId);
        return taskRepository.findByCategoryIdOrderByCreatedAtAsc(categoryId);
    }

    @Transactional
    public Task createTask(UUID projectId, UUID categoryId, String name, Integer duration) {
        WbsItem category = requireCategory(projectId, categoryId);

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Tên công việc không được để trống");
        }
        if (name.length() > 255) {
            throw new IllegalArgumentException("Tên công việc không được vượt quá 255 ký tự");
        }
        if (duration == null || duration <= 0) {
            throw new IllegalArgumentException("Thời lượng phải lớn hơn 0");
        }

        return taskRepository.save(new Task(category, name.trim(), duration));
    }

    private WbsItem requireCategory(UUID projectId, UUID categoryId) {
        WbsItem category = wbsItemRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hạng mục"));

        if (!projectId.equals(category.getProjectId())) {
            throw new IllegalArgumentException("Hạng mục không thuộc dự án này");
        }
        if ("task".equalsIgnoreCase(category.getType())) {
            throw new IllegalArgumentException("Chỉ có thể gắn công việc vào hạng mục");
        }

        return category;
    }
}