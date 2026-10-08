package com.ntdhtcct.domain.wbs;

import com.ntdhtcct.domain.project.Project;
import com.ntdhtcct.domain.project.ProjectRepository;
import com.ntdhtcct.repository.ProjectMemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class WbsService {

    private final WbsItemRepository wbsItemRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final CpmEngine cpmEngine;

    public WbsService(
            WbsItemRepository wbsItemRepository,
            ProjectRepository projectRepository,
            ProjectMemberRepository projectMemberRepository,
            CpmEngine cpmEngine
    ) {
        this.wbsItemRepository = wbsItemRepository;
        this.projectRepository = projectRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.cpmEngine = cpmEngine;
    }

    public List<Project> getProjects() {
        return projectRepository.findAll();
    }

    public List<Project> getProjectsForUser(UUID userId) {
        return projectMemberRepository.findActiveProjectsByUserId(userId);
    }

    public List<WbsItem> getWbsByProject(UUID projectId) {
        requireProject(projectId);
        return wbsItemRepository.findByProjectIdOrderByWbsCodeAsc(projectId);
    }

    @Transactional
    public ProjectScheduleResponse getSchedule(UUID projectId) {
        requireProject(projectId);
        List<WbsItem> items = wbsItemRepository.findByProjectIdOrderByWbsCodeAsc(projectId);
        CpmEngine.Result result = cpmEngine.calculate(items);
        wbsItemRepository.saveAll(items);

        Set<UUID> parentIds = new HashSet<>();
        items.stream()
            .map(WbsItem::getParentId)
            .filter(id -> id != null)
            .forEach(parentIds::add);
        List<WbsItem> tasks = items.stream()
            .filter(item -> "task".equalsIgnoreCase(item.getType()))
            .filter(item -> !parentIds.contains(item.getId()))
            .toList();
        int criticalTasksCount = (int) tasks.stream()
            .filter(WbsItem::isCritical)
            .count();

        return new ProjectScheduleResponse(
            true,
            new ProjectScheduleResponse.Summary(
                tasks.size(),
                criticalTasksCount,
                result.durationDays(),
                result.criticalPathCount(),
                result.complete(),
                result.unscheduledTaskCount()
            ),
            tasks
        );
    }

    @Transactional
    public CriticalPathResponse getCriticalPathProgress(UUID projectId) {
        requireProject(projectId);
        List<WbsItem> items = wbsItemRepository.findByProjectIdOrderByWbsCodeAsc(projectId);
        CpmEngine.Result result = cpmEngine.calculate(items);
        wbsItemRepository.saveAll(items);

        List<WbsItem> criticalTasks = items.stream()
                .filter(WbsItem::isCritical)
                .toList();

        return new CriticalPathResponse(
                projectId,
                result.durationDays(),
                result.criticalPathCount(),
                result.complete(),
                result.unscheduledTaskCount(),
                criticalTasks
        );
    }

    @Transactional
    public WbsItem createTask(UUID projectId, WbsItem item) {
        item.setType("task");
        return create(projectId, item);
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

        validateActualProgress(item);

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

        validatePredecessors(projectId, null, item.getPredecessorIds());

        item.setProjectId(projectId);
        item.setAssignedTeamMemberId(null);
        item.setAssignedTeamName(null);

        if (item.getType() == null || item.getType().isBlank()) {
            item.setType(item.getParentId() == null ? "phase" : "task");
        }

        if ("task".equalsIgnoreCase(item.getType())
                && (item.getDuration() == null || item.getDuration() <= 0)) {
            throw new IllegalArgumentException("Thời lượng thực hiện phải lớn hơn 0");
        }

        if (item.getStatus() == null || item.getStatus().isBlank()) {
            item.setStatus("not_started");
        }

        WbsItem saved = wbsItemRepository.save(item);
        recalculateCpm(projectId);
        return saved;
    }

    @Transactional
    public WbsItem update(UUID projectId, UUID itemId, WbsItem request) {
        requireProject(projectId);

        WbsItem item = requireItem(projectId, itemId);

        if ("task".equalsIgnoreCase(request.getType())
                && (request.getDuration() == null || request.getDuration() <= 0)) {
            throw new IllegalArgumentException("Thời lượng thực hiện phải lớn hơn 0");
        }

        if (request.getName() == null || request.getName().isBlank()) {
            throw new RuntimeException("Tên công việc không được để trống");
        }

        if (request.getProgress() < 0 || request.getProgress() > 100) {
            throw new RuntimeException("Tiến độ phải nằm trong khoảng 0 đến 100");
        }

        validateActualProgress(request);

        if (request.getStartDate() != null
                && request.getEndDate() != null
                && request.getEndDate().isBefore(request.getStartDate())) {
            throw new RuntimeException("Ngày kết thúc phải sau hoặc bằng ngày bắt đầu");
        }

        validatePredecessors(projectId, itemId, request.getPredecessorIds());

        item.setName(request.getName());
        item.setType(request.getType());
        item.setAssigneeId(request.getAssigneeId());
        item.setAssigneeName(request.getAssigneeName());
        item.setAssigneeInitials(request.getAssigneeInitials());
        item.setStatus(request.getStatus());
        item.setProgress(request.getProgress());
        item.setStartDate(request.getStartDate());
        item.setEndDate(request.getEndDate());
        item.setActualStartDate(request.getActualStartDate());
        item.setActualEndDate(request.getActualEndDate());
        item.setDuration(request.getDuration());
        item.setPredecessorIds(request.getPredecessorIds());
        item.setDescription(request.getDescription());
        item.setImage(request.getImage());

        WbsItem saved = wbsItemRepository.save(item);
        recalculateCpm(projectId);
        return saved;
    }

    @Transactional
    public void delete(UUID projectId, UUID itemId) {
        requireProject(projectId);
        WbsItem item = requireItem(projectId, itemId);
        wbsItemRepository.delete(item);
        recalculateCpm(projectId);
    }

    private Project requireProject(UUID projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Dự án không tồn tại"));
    }

    private void validateActualProgress(WbsItem item) {
        if (item.getActualEndDate() != null && item.getActualStartDate() == null) {
            throw new IllegalArgumentException(
                    "Không thể có ngày kết thúc thực tế khi chưa có ngày bắt đầu thực tế"
            );
        }
        if (item.getActualStartDate() != null
                && item.getActualEndDate() != null
                && item.getActualEndDate().isBefore(item.getActualStartDate())) {
            throw new IllegalArgumentException(
                    "Ngày kết thúc thực tế phải sau hoặc cùng ngày với ngày bắt đầu thực tế"
            );
        }
    }

    private WbsItem requireItem(UUID projectId, UUID itemId) {
        WbsItem item = wbsItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Công việc không tồn tại"));

        if (!item.getProjectId().equals(projectId)) {
            throw new RuntimeException("Công việc không thuộc dự án này");
        }

        return item;
    }

    private void validatePredecessors(
            UUID projectId,
            UUID itemId,
            Set<UUID> requestedPredecessors
    ) {
        Set<UUID> predecessors = requestedPredecessors == null
                ? Set.of()
                : requestedPredecessors;

        List<WbsItem> projectItems = wbsItemRepository.findByProjectIdOrderByWbsCodeAsc(projectId);
        Set<UUID> parentIds = new HashSet<>();
        projectItems.stream()
            .map(WbsItem::getParentId)
            .filter(id -> id != null)
            .forEach(parentIds::add);

        for (UUID predecessorId : predecessors) {
            if (predecessorId.equals(itemId)) {
                throw new RuntimeException("Công việc không thể phụ thuộc vào chính nó");
            }
            WbsItem predecessor = requireItem(projectId, predecessorId);
            if (!"task".equalsIgnoreCase(predecessor.getType()) || parentIds.contains(predecessorId)) {
                throw new RuntimeException("Công việc tiền nhiệm phải là một task cấp cuối");
            }
        }

        if (itemId == null) {
            return;
        }

        Map<UUID, Set<UUID>> dependencyGraph = new HashMap<>();
        for (WbsItem existing : projectItems) {
            dependencyGraph.put(existing.getId(), existing.getPredecessorIds());
        }
        dependencyGraph.put(itemId, predecessors);

        if (hasDependencyCycle(itemId, dependencyGraph, new HashSet<>(), new HashSet<>())) {
            throw new RuntimeException("Quan hệ phụ thuộc tạo thành vòng lặp");
        }
    }

    private boolean hasDependencyCycle(
            UUID itemId,
            Map<UUID, Set<UUID>> dependencyGraph,
            Set<UUID> visiting,
            Set<UUID> visited
    ) {
        if (visiting.contains(itemId)) {
            return true;
        }
        if (visited.contains(itemId)) {
            return false;
        }

        visiting.add(itemId);
        for (UUID predecessorId : dependencyGraph.getOrDefault(itemId, Set.of())) {
            if (hasDependencyCycle(predecessorId, dependencyGraph, visiting, visited)) {
                return true;
            }
        }
        visiting.remove(itemId);
        visited.add(itemId);
        return false;
    }

    private void recalculateCpm(UUID projectId) {
        List<WbsItem> items = wbsItemRepository.findByProjectIdOrderByWbsCodeAsc(projectId);
        cpmEngine.calculate(items);
        wbsItemRepository.saveAll(items);
    }
}