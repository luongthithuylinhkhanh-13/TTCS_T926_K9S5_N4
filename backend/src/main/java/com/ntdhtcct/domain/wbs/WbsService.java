package com.ntdhtcct.domain.wbs;

import com.ntdhtcct.domain.project.Project;
import com.ntdhtcct.domain.project.ProjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class WbsService {

    private final WbsItemRepository wbsItemRepository;
    private final ProjectRepository projectRepository;

    public WbsService(
            WbsItemRepository wbsItemRepository,
            ProjectRepository projectRepository
    ) {
        this.wbsItemRepository = wbsItemRepository;
        this.projectRepository = projectRepository;
    }

    public List<Project> getProjects() {
        return projectRepository.findAll();
    }

    public List<WbsItem> getWbsByProject(UUID projectId) {
        requireProject(projectId);
        return wbsItemRepository.findByProjectIdOrderByWbsCodeAsc(projectId);
    }

    @Transactional
    public WbsItem create(UUID projectId, WbsItem item) {
        requireProject(projectId);

        if (item.getWbsCode() == null || item.getWbsCode().isBlank()) {
            throw new RuntimeException("Mã WBS không được để trống");
        }

        if (item.getName() == null || item.getName().isBlank()) {
            throw new RuntimeException("Tên công việc không được để trống");
        }

        if (wbsItemRepository.existsByProjectIdAndWbsCode(
                projectId, item.getWbsCode())) {
            throw new RuntimeException("Mã WBS đã tồn tại trong dự án");
        }

        if (item.getProgress() < 0 || item.getProgress() > 100) {
            throw new RuntimeException("Tiến độ phải nằm trong khoảng 0 đến 100");
        }

        if (item.getStartDate() != null
                && item.getEndDate() != null
                && item.getEndDate().isBefore(item.getStartDate())) {
            throw new RuntimeException("Ngày kết thúc phải sau hoặc bằng ngày bắt đầu");
        }

        if (item.getParentId() != null) {
            WbsItem parent = wbsItemRepository.findById(item.getParentId())
                    .orElseThrow(() ->
                            new RuntimeException("Công việc cha không tồn tại"));

            if (!parent.getProjectId().equals(projectId)) {
                throw new RuntimeException("Công việc cha không thuộc dự án này");
            }
        }

        item.setProjectId(projectId);

        if (item.getType() == null || item.getType().isBlank()) {
            item.setType(item.getParentId() == null ? "phase" : "task");
        }

        if (item.getStatus() == null || item.getStatus().isBlank()) {
            item.setStatus("not_started");
        }

        return wbsItemRepository.save(item);
    }

    @Transactional
    public WbsItem update(UUID projectId, UUID itemId, WbsItem request) {
        requireProject(projectId);

        WbsItem item = requireItem(projectId, itemId);

        if (request.getName() == null || request.getName().isBlank()) {
            throw new RuntimeException("Tên công việc không được để trống");
        }

        if (request.getProgress() < 0 || request.getProgress() > 100) {
            throw new RuntimeException("Tiến độ phải nằm trong khoảng 0 đến 100");
        }

        if (request.getStartDate() != null
                && request.getEndDate() != null
                && request.getEndDate().isBefore(request.getStartDate())) {
            throw new RuntimeException("Ngày kết thúc phải sau hoặc bằng ngày bắt đầu");
        }

        item.setName(request.getName());
        item.setType(request.getType());
        item.setAssigneeId(request.getAssigneeId());
        item.setAssigneeName(request.getAssigneeName());
        item.setAssigneeInitials(request.getAssigneeInitials());
        item.setStatus(request.getStatus());
        item.setProgress(request.getProgress());
        item.setStartDate(request.getStartDate());
        item.setEndDate(request.getEndDate());
        item.setDescription(request.getDescription());
        item.setImage(request.getImage());

        return wbsItemRepository.save(item);
    }

    @Transactional
    public void delete(UUID projectId, UUID itemId) {
        requireProject(projectId);
        WbsItem item = requireItem(projectId, itemId);
        wbsItemRepository.delete(item);
    }

    private Project requireProject(UUID projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Dự án không tồn tại"));
    }

    private WbsItem requireItem(UUID projectId, UUID itemId) {
        WbsItem item = wbsItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Công việc không tồn tại"));

        if (!item.getProjectId().equals(projectId)) {
            throw new RuntimeException("Công việc không thuộc dự án này");
        }

        return item;
    }
}