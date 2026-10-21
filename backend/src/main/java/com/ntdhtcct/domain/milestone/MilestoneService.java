package com.ntdhtcct.domain.milestone;

import com.ntdhtcct.domain.milestone.dto.CreateMilestoneRequest;
import com.ntdhtcct.domain.milestone.dto.MilestoneResponse;
import com.ntdhtcct.domain.milestone.dto.MilestoneWarningResponse;
import com.ntdhtcct.domain.milestone.dto.TaskDelayInfo;
import com.ntdhtcct.domain.milestone.dto.UpdateMilestoneRequest;
import com.ntdhtcct.domain.project.Project;
import com.ntdhtcct.domain.project.ProjectRepository;
import com.ntdhtcct.domain.wbs.CpmEngine;
import com.ntdhtcct.domain.wbs.WbsItem;
import com.ntdhtcct.domain.wbs.WbsItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

@Service
public class MilestoneService {

    private final MilestoneRepository milestoneRepository;
    private final ProjectRepository projectRepository;
    private final WbsItemRepository wbsItemRepository;
    private final CpmEngine cpmEngine;

    public MilestoneService(
            MilestoneRepository milestoneRepository,
            ProjectRepository projectRepository,
            WbsItemRepository wbsItemRepository,
            CpmEngine cpmEngine
    ) {
        this.milestoneRepository = milestoneRepository;
        this.projectRepository = projectRepository;
        this.wbsItemRepository = wbsItemRepository;
        this.cpmEngine = cpmEngine;
    }

    /**
     * T-43 (NTDHTCT-185): Tạo mốc gắn hạng mục với ngày bắt buộc.
     */
    @Transactional
    public Milestone createMilestone(UUID projectId, CreateMilestoneRequest request) {
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
                request.description() != null ? request.description().trim() : null
        );

        return milestoneRepository.save(milestone);
    }

    @Transactional(readOnly = true)
    public List<Milestone> getMilestones(UUID projectId) {
        requireProject(projectId);
        return milestoneRepository.findByProjectIdOrderByTargetDateAsc(projectId);
    }

    @Transactional(readOnly = true)
    public Milestone getMilestone(UUID projectId, UUID milestoneId) {
        requireProject(projectId);
        return requireMilestone(projectId, milestoneId);
    }

    @Transactional
    public Milestone updateMilestone(UUID projectId, UUID milestoneId, UpdateMilestoneRequest request) {
        requireProject(projectId);
        Milestone milestone = requireMilestone(projectId, milestoneId);

        if (request.categoryId() != null && !request.categoryId().equals(milestone.getCategory().getId())) {
            WbsItem newCategory = requireCategory(projectId, request.categoryId());
            milestone.setCategory(newCategory);
        }

        if (request.name() != null) {
            if (request.name().isBlank()) {
                throw new IllegalArgumentException("Tên mốc tiến độ không được để trống");
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
    public void deleteMilestone(UUID projectId, UUID milestoneId) {
        requireProject(projectId);
        Milestone milestone = requireMilestone(projectId, milestoneId);
        milestoneRepository.delete(milestone);
    }

    /**
     * T-44 (NTDHTCT-186) & T-45 (NTDHTCT-187):
     * So kết sớm của việc cuối hạng mục với mốc, tính số ngày vượt và xây dựng
     * danh sách cảnh báo kèm chuỗi công việc gây chậm.
     */
    @Transactional
    public List<MilestoneWarningResponse> getMilestoneWarnings(UUID projectId) {
        Project project = requireProject(projectId);
        List<Milestone> milestones = milestoneRepository.findByProjectIdOrderByTargetDateAsc(projectId);

        if (milestones.isEmpty()) {
            return List.of();
        }

        // Đảm bảo thông số CPM (ES, EF, Critical) được tính toán mới nhất
        List<WbsItem> allItems = wbsItemRepository.findByProjectIdOrderByWbsCodeAsc(projectId);
        if (!allItems.isEmpty()) {
            cpmEngine.calculate(allItems);
            wbsItemRepository.saveAll(allItems);
        }

        Map<UUID, WbsItem> itemMap = allItems.stream()
                .collect(Collectors.toMap(WbsItem::getId, Function.identity(), (existing, replacing) -> existing));

        List<MilestoneWarningResponse> responses = new ArrayList<>();

        for (Milestone milestone : milestones) {
            WbsItem category = milestone.getCategory();
            UUID categoryId = category.getId();

            // Tìm các công việc con thuộc hạng mục này
            List<WbsItem> categoryTasks = findTasksInCategory(categoryId, allItems);

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

            // T-44: Xác định việc cuối hạng mục (Last Task) có thời điểm kết thúc sớm muộn nhất
            WbsItem lastTask = findLastTaskOfCategory(categoryTasks, project.getStartDate());

            LocalDate lastTaskEfDate = calculateEarlyFinishDate(lastTask, project.getStartDate());
            Integer lastTaskEfDays = lastTask.getEf();

            // Tính số ngày vượt mốc
            long overrunDays = 0;
            boolean isOverrun = false;

            if (lastTaskEfDate != null) {
                long diff = ChronoUnit.DAYS.between(milestone.getTargetDate(), lastTaskEfDate);
                if (diff > 0) {
                    overrunDays = diff;
                    isOverrun = true;
                }
            } else if (lastTaskEfDays != null && project.getStartDate() != null) {
                LocalDate estimatedEfDate = project.getStartDate().plusDays(Math.max(0, lastTaskEfDays - 1));
                long diff = ChronoUnit.DAYS.between(milestone.getTargetDate(), estimatedEfDate);
                if (diff > 0) {
                    overrunDays = diff;
                    isOverrun = true;
                }
                lastTaskEfDate = estimatedEfDate;
            }

            // T-45: Truy vết chuỗi việc gây chậm (Delay Chain) từ việc cuối ngược về tiền nhiệm
            List<TaskDelayInfo> delayChain = traceDelayChain(lastTask, itemMap);

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
     * Tìm tất cả các task thuộc về một category (bao gồm cả phân cấp con).
     */
    private List<WbsItem> findTasksInCategory(UUID categoryId, List<WbsItem> allItems) {
        Set<UUID> categorySubtreeIds = new HashSet<>();
        categorySubtreeIds.add(categoryId);

        // Mở rộng tất cả các cấp con của category
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

        // Lấy tất cả task (hoặc item cấp lá) thuộc cây hạng mục này
        Set<UUID> parentIds = allItems.stream()
                .map(WbsItem::getParentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        return allItems.stream()
                .filter(item -> categorySubtreeIds.contains(item.getId()) && !item.getId().equals(categoryId))
                .filter(item -> "task".equalsIgnoreCase(item.getType()) || !parentIds.contains(item.getId()))
                .toList();
    }

    /**
     * T-44: Tìm việc cuối hạng mục - là việc có ngày kết sớm lớn nhất.
     */
    private WbsItem findLastTaskOfCategory(List<WbsItem> tasks, LocalDate projectStartDate) {
        return tasks.stream().max((taskA, taskB) -> {
            LocalDate dateA = calculateEarlyFinishDate(taskA, projectStartDate);
            LocalDate dateB = calculateEarlyFinishDate(taskB, projectStartDate);

            if (dateA != null && dateB != null) {
                int cmp = dateA.compareTo(dateB);
                if (cmp != 0) return cmp;
            } else if (dateA != null) {
                return 1;
            } else if (dateB != null) {
                return -1;
            }

            int efA = taskA.getEf() != null ? taskA.getEf() : 0;
            int efB = taskB.getEf() != null ? taskB.getEf() : 0;
            return Integer.compare(efA, efB);
        }).orElse(tasks.get(0));
    }

    /**
     * Tính ngày kết sớm của task dựa vào EF của CPM hoặc endDate.
     */
    private LocalDate calculateEarlyFinishDate(WbsItem task, LocalDate projectStartDate) {
        if (projectStartDate != null && task.getEf() != null && task.getEf() > 0) {
            return projectStartDate.plusDays(task.getEf() - 1);
        }
        if (task.getEndDate() != null) {
            return task.getEndDate();
        }
        if (task.getStartDate() != null && task.getDuration() != null && task.getDuration() > 0) {
            return task.getStartDate().plusDays(task.getDuration() - 1);
        }
        return null;
    }

    /**
     * T-45: Truy vết chuỗi công việc gây chậm (Driving Predecessor Path).
     * Bắt đầu từ việc cuối hạng mục, truy ngược theo tiền nhiệm có EF = ES của công việc hiện tại.
     */
    private List<TaskDelayInfo> traceDelayChain(WbsItem lastTask, Map<UUID, WbsItem> itemMap) {
        List<WbsItem> chain = new ArrayList<>();
        Set<UUID> visited = new HashSet<>();
        WbsItem current = lastTask;

        while (current != null && visited.add(current.getId())) {
            chain.add(current);

            Set<UUID> predecessorIds = current.getPredecessorIds();
            if (predecessorIds == null || predecessorIds.isEmpty()) {
                break;
            }

            WbsItem drivingPredecessor = null;
            int currentEs = current.getEs() != null ? current.getEs() : -1;

            // Tìm tiền nhiệm chi phối thời điểm bắt đầu sớm nhất (EF của tiền nhiệm == ES của current)
            for (UUID predId : predecessorIds) {
                WbsItem pred = itemMap.get(predId);
                if (pred == null) continue;

                if (pred.getEf() != null && pred.getEf() == currentEs) {
                    // Ưu tiên chọn công việc nằm trên đường găng (critical)
                    if (drivingPredecessor == null || (!drivingPredecessor.isCritical() && pred.isCritical())) {
                        drivingPredecessor = pred;
                    }
                }
            }

            // Nếu không tìm thấy tiền nhiệm khớp chính xác EF == ES (ví dụ do độ trễ hoặc làm tròn),
            // lấy tiền nhiệm có EF lớn nhất
            if (drivingPredecessor == null) {
                drivingPredecessor = predecessorIds.stream()
                        .map(itemMap::get)
                        .filter(Objects::nonNull)
                        .max(Comparator.comparingInt(p -> p.getEf() != null ? p.getEf() : 0))
                        .orElse(null);
            }

            current = drivingPredecessor;
        }

        // Đảo ngược chuỗi để có thứ tự từ công việc đầu -> việc cuối
        Collections.reverse(chain);

        return chain.stream()
                .map(TaskDelayInfo::from)
                .toList();
    }

    private Project requireProject(UUID projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Dự án không tồn tại với ID: " + projectId));
    }

    private WbsItem requireCategory(UUID projectId, UUID categoryId) {
        WbsItem item = wbsItemRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hạng mục với ID: " + categoryId));

        if (!projectId.equals(item.getProjectId())) {
            throw new IllegalArgumentException("Hạng mục không thuộc dự án này");
        }
        if ("task".equalsIgnoreCase(item.getType())) {
            throw new IllegalArgumentException("Chỉ có thể gắn mốc vào hạng mục (không gắn trực tiếp vào task đơn lẻ)");
        }
        return item;
    }

    private Milestone requireMilestone(UUID projectId, UUID milestoneId) {
        return milestoneRepository.findByIdAndProjectId(milestoneId, projectId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy mốc tiến độ với ID: " + milestoneId));
    }
}
