package com.ntdhtcct.domain.milestone;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ntdhtcct.domain.milestone.dto.CreateMilestoneRequest;
import com.ntdhtcct.domain.milestone.dto.MilestoneWarningResponse;
import com.ntdhtcct.domain.milestone.dto.TaskDelayInfo;
import com.ntdhtcct.domain.milestone.dto.UpdateMilestoneRequest;
import com.ntdhtcct.domain.wbs.CpmEngine;
import com.ntdhtcct.domain.wbs.WbsItem;
import com.ntdhtcct.domain.wbs.WbsItemRepository;

@Service
public class MilestoneService {

    private final MilestoneRepository milestoneRepository;
    private final WbsItemRepository wbsItemRepository;
    private final CpmEngine cpmEngine;

    public MilestoneService(
            MilestoneRepository milestoneRepository,
            WbsItemRepository wbsItemRepository,
            CpmEngine cpmEngine
    ) {
        this.milestoneRepository = milestoneRepository;
        this.wbsItemRepository = wbsItemRepository;
        this.cpmEngine = cpmEngine;
    }

    /**
     * T-43 (NTDHTCT-185): Tạo mốc gắn hạng mục với ngày bắt buộc.
     */
    @Transactional
    public Milestone createMilestone(Long projectId, CreateMilestoneRequest request) {

        requireProject(projectId);

        if (request.categoryId() == null) {
            throw new IllegalArgumentException("Hạng mục không được để trống");
        }

        if (request.name() == null || request.name().isBlank()) {
            throw new IllegalArgumentException("Tên mốc tiến độ không được để trống");
        }

        if (request.targetDate() == null) {
            throw new IllegalArgumentException("Ngày bắt buộc của mốc không được để trống");
        }

        WbsItem category = requireCategory(projectId, request.categoryId());

        Milestone milestone = new Milestone(
                projectId,
                category,
                request.name().trim(),
                request.targetDate(),
                request.description() != null
                        ? request.description().trim()
                        : null
        );

        return milestoneRepository.save(milestone);
    }

    @Transactional(readOnly = true)
    public List<Milestone> getMilestones(Long projectId) {

        requireProject(projectId);

        return milestoneRepository.findByProjectIdOrderByTargetDateAsc(projectId);
    }

    @Transactional(readOnly = true)
    public Milestone getMilestone(Long projectId, UUID milestoneId) {

        requireProject(projectId);

        return requireMilestone(projectId, milestoneId);
    }

    @Transactional
    public Milestone updateMilestone(
            Long projectId,
            UUID milestoneId,
            UpdateMilestoneRequest request
    ) {

        requireProject(projectId);

        Milestone milestone = requireMilestone(projectId, milestoneId);

        if (request.categoryId() != null
                && !request.categoryId().equals(milestone.getCategory().getId())) {

            WbsItem newCategory =
                    requireCategory(projectId, request.categoryId());

            milestone.setCategory(newCategory);
        }

        if (request.name() != null) {

            if (request.name().isBlank()) {
                throw new IllegalArgumentException(
                        "Tên mốc tiến độ không được để trống"
                );
            }

            milestone.setName(request.name().trim());
        }

        if (request.targetDate() != null) {
            milestone.setTargetDate(request.targetDate());
        }

        if (request.description() != null) {
            milestone.setDescription(request.description().trim());
        }

        return milestoneRepository.save(milestone);
    }

    @Transactional
    public void deleteMilestone(Long projectId, UUID milestoneId) {

        requireProject(projectId);

        Milestone milestone =
                requireMilestone(projectId, milestoneId);

        milestoneRepository.delete(milestone);
    }

    /**
     * T-44 (NTDHTCT-186) & T-45 (NTDHTCT-187):
     * So kết sớm của việc cuối hạng mục với mốc.
     */
    @Transactional
    public List<MilestoneWarningResponse> getMilestoneWarnings(Long projectId) {

        requireProject(projectId);

        List<Milestone> milestones =
                milestoneRepository.findByProjectIdOrderByTargetDateAsc(projectId);

        if (milestones.isEmpty()) {
            return List.of();
        }

        // Đảm bảo thông số CPM được tính toán mới nhất
        List<WbsItem> allItems =
                wbsItemRepository.findByProjectIdOrderByWbsCodeAsc(projectId);

        if (!allItems.isEmpty()) {
            cpmEngine.calculate(allItems);
            wbsItemRepository.saveAll(allItems);
        }

        Map<UUID, WbsItem> itemMap =
                allItems.stream()
                        .collect(Collectors.toMap(
                                WbsItem::getId,
                                Function.identity(),
                                (existing, replacing) -> existing
                        ));

        List<MilestoneWarningResponse> responses =
                new ArrayList<>();

        for (Milestone milestone : milestones) {

            WbsItem category = milestone.getCategory();
            UUID categoryId = category.getId();

            List<WbsItem> categoryTasks =
                    findTasksInCategory(categoryId, allItems);

            if (categoryTasks.isEmpty()) {

                responses.add(new MilestoneWarningResponse(
                        milestone.getId(),
                        milestone.getName(),
                        milestone.getTargetDate(),
                        categoryId,
                        category.getName(),
                        category.getWbsCode(),
                        null,
                        null,
                        null,
                        null,
                        null,
                        0,
                        false,
                        "NO_TASKS",
                        List.of()
                ));

                continue;
            }

            WbsItem lastTask =
                    findLastTaskOfCategory(
                            categoryTasks,
                            getProjectStartDate(projectId)
                    );

            LocalDate projectStartDate =
                    getProjectStartDate(projectId);

            LocalDate lastTaskEfDate =
                    calculateEarlyFinishDate(
                            lastTask,
                            projectStartDate
                    );

            Integer lastTaskEfDays = lastTask.getEf();

            long overrunDays = 0;
            boolean isOverrun = false;

            if (lastTaskEfDate != null) {

                long diff = ChronoUnit.DAYS.between(
                        milestone.getTargetDate(),
                        lastTaskEfDate
                );

                if (diff > 0) {
                    overrunDays = diff;
                    isOverrun = true;
                }

            } else if (
                    lastTaskEfDays != null
                            && projectStartDate != null
            ) {

                LocalDate estimatedEfDate =
                        projectStartDate.plusDays(
                                Math.max(0, lastTaskEfDays - 1)
                        );

                long diff = ChronoUnit.DAYS.between(
                        milestone.getTargetDate(),
                        estimatedEfDate
                );

                if (diff > 0) {
                    overrunDays = diff;
                    isOverrun = true;
                }

                lastTaskEfDate = estimatedEfDate;
            }

            List<TaskDelayInfo> delayChain =
                    traceDelayChain(lastTask, itemMap);

            responses.add(new MilestoneWarningResponse(
                    milestone.getId(),
                    milestone.getName(),
                    milestone.getTargetDate(),
                    categoryId,
                    category.getName(),
                    category.getWbsCode(),
                    lastTask.getId(),
                    lastTask.getName(),
                    lastTask.getWbsCode(),
                    lastTaskEfDate,
                    lastTaskEfDays,
                    overrunDays,
                    isOverrun,
                    isOverrun ? "OVERRUN" : "ON_TRACK",
                    delayChain
            ));
        }

        return responses;
    }

    /**
     * Lấy ngày bắt đầu dự án.
     *
     * Project hiện tại dùng UUID ở module project,
     * nên chưa truy cập trực tiếp bằng projectId Long.
     *
     * Tạm thời lấy ngày nhỏ nhất từ các WBS item.
     */
    private LocalDate getProjectStartDate(Long projectId) {

        List<WbsItem> items =
                wbsItemRepository.findByProjectIdOrderByWbsCodeAsc(projectId);

        return items.stream()
                .map(WbsItem::getStartDate)
                .filter(Objects::nonNull)
                .min(LocalDate::compareTo)
                .orElse(null);
    }

    /**
     * Kiểm tra project tồn tại thông qua WBS.
     */
    private void requireProject(Long projectId) {

        if (projectId == null) {
            throw new IllegalArgumentException(
                    "ID dự án không được để trống"
            );
        }

        List<WbsItem> items =
                wbsItemRepository.findByProjectIdOrderByWbsCodeAsc(projectId);

        if (items.isEmpty()) {
            throw new IllegalArgumentException(
                    "Dự án không tồn tại với ID: " + projectId
            );
        }
    }

    private WbsItem requireCategory(
            Long projectId,
            UUID categoryId
    ) {

        WbsItem item =
                wbsItemRepository.findById(categoryId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Không tìm thấy hạng mục với ID: "
                                                + categoryId
                                )
                        );

        if (!projectId.equals(item.getProjectId())) {
            throw new IllegalArgumentException(
                    "Hạng mục không thuộc dự án này"
            );
        }

        if ("task".equalsIgnoreCase(item.getType())) {
            throw new IllegalArgumentException(
                    "Chỉ có thể gắn mốc vào hạng mục "
                            + "(không gắn trực tiếp vào task đơn lẻ)"
            );
        }

        return item;
    }

    private Milestone requireMilestone(
            Long projectId,
            UUID milestoneId
    ) {

        return milestoneRepository
                .findByIdAndProjectId(milestoneId, projectId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Không tìm thấy mốc tiến độ với ID: "
                                        + milestoneId
                        )
                );
    }

    private List<WbsItem> findTasksInCategory(
            UUID categoryId,
            List<WbsItem> allItems
    ) {

        Set<UUID> categorySubtreeIds =
                new HashSet<>();

        categorySubtreeIds.add(categoryId);

        boolean expanded = true;

        while (expanded) {

            expanded = false;

            for (WbsItem item : allItems) {

                if (item.getParentId() != null
                        && categorySubtreeIds.contains(item.getParentId())
                        && !categorySubtreeIds.contains(item.getId())) {

                    categorySubtreeIds.add(item.getId());
                    expanded = true;
                }
            }
        }

        Set<UUID> parentIds =
                allItems.stream()
                        .map(WbsItem::getParentId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());

        return allItems.stream()
                .filter(item ->
                        categorySubtreeIds.contains(item.getId())
                                && !item.getId().equals(categoryId)
                )
                .filter(item ->
                        "task".equalsIgnoreCase(item.getType())
                                || !parentIds.contains(item.getId())
                )
                .toList();
    }

    private WbsItem findLastTaskOfCategory(
            List<WbsItem> tasks,
            LocalDate projectStartDate
    ) {

        return tasks.stream()
                .max((taskA, taskB) -> {

                    LocalDate dateA =
                            calculateEarlyFinishDate(
                                    taskA,
                                    projectStartDate
                            );

                    LocalDate dateB =
                            calculateEarlyFinishDate(
                                    taskB,
                                    projectStartDate
                            );

                    if (dateA != null && dateB != null) {

                        int cmp =
                                dateA.compareTo(dateB);

                        if (cmp != 0) {
                            return cmp;
                        }

                    } else if (dateA != null) {
                        return 1;

                    } else if (dateB != null) {
                        return -1;
                    }

                    int efA =
                            taskA.getEf() != null
                                    ? taskA.getEf()
                                    : 0;

                    int efB =
                            taskB.getEf() != null
                                    ? taskB.getEf()
                                    : 0;

                    return Integer.compare(efA, efB);
                })
                .orElse(tasks.get(0));
    }

    private LocalDate calculateEarlyFinishDate(
            WbsItem task,
            LocalDate projectStartDate
    ) {

        if (projectStartDate != null
                && task.getEf() != null
                && task.getEf() > 0) {

            return projectStartDate.plusDays(
                    task.getEf() - 1
            );
        }

        if (task.getEndDate() != null) {
            return task.getEndDate();
        }

        if (task.getStartDate() != null
                && task.getDuration() != null
                && task.getDuration() > 0) {

            return task.getStartDate().plusDays(
                    task.getDuration() - 1
            );
        }

        return null;
    }

    private List<TaskDelayInfo> traceDelayChain(
            WbsItem lastTask,
            Map<UUID, WbsItem> itemMap
    ) {

        List<WbsItem> chain =
                new ArrayList<>();

        Set<UUID> visited =
                new HashSet<>();

        WbsItem current = lastTask;

        while (
                current != null
                        && visited.add(current.getId())
        ) {

            chain.add(current);

            Set<UUID> predecessorIds =
                    current.getPredecessorIds();

            if (predecessorIds == null
                    || predecessorIds.isEmpty()) {
                break;
            }

            WbsItem drivingPredecessor = null;

            int currentEs =
                    current.getEs() != null
                            ? current.getEs()
                            : -1;

            for (UUID predId : predecessorIds) {

                WbsItem pred =
                        itemMap.get(predId);

                if (pred == null) {
                    continue;
                }

                if (pred.getEf() != null
                        && pred.getEf() == currentEs) {

                    if (drivingPredecessor == null
                            || (
                            !drivingPredecessor.isCritical()
                                    && pred.isCritical()
                    )) {

                        drivingPredecessor = pred;
                    }
                }
            }

            if (drivingPredecessor == null) {

                drivingPredecessor =
                        predecessorIds.stream()
                                .map(itemMap::get)
                                .filter(Objects::nonNull)
                                .max(
                                        Comparator.comparingInt(
                                                p -> p.getEf() != null
                                                        ? p.getEf()
                                                        : 0
                                        )
                                )
                                .orElse(null);
            }

            current = drivingPredecessor;
        }

        Collections.reverse(chain);

        return chain.stream()
                .map(TaskDelayInfo::from)
                .toList();
    }
}