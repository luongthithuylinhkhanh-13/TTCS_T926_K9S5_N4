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
        return createTask(projectId, categoryId, name, duration, null, null, null);
    }

    @Transactional
    public Task createTask(
            UUID projectId,
            UUID categoryId,
            String name,
            Integer duration,
            Double quantity,
            String unit,
            Double unitPrice
    ) {
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
        if (quantity != null && quantity < 0) {
            throw new IllegalArgumentException("Khối lượng công việc không được âm");
        }
        if (unitPrice != null && unitPrice < 0) {
            throw new IllegalArgumentException("Đơn giá không được âm");
        }
        if (unit != null && unit.length() > 50) {
            throw new IllegalArgumentException("Đơn vị tính không được vượt quá 50 ký tự");
        }
        if (quantity != null && quantity > 0) {
            if (unit == null || unit.trim().isEmpty()) {
                throw new IllegalArgumentException("Vui lòng nhập đơn vị tính khi đã khai báo khối lượng");
            }
        }

        String trimmedUnit = unit != null ? unit.trim() : null;
        return taskRepository.save(new Task(category, name.trim(), duration, quantity, trimmedUnit, unitPrice));
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