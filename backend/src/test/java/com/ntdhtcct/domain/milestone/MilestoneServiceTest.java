package com.ntdhtcct.domain.milestone;

import com.ntdhtcct.domain.milestone.dto.CreateMilestoneRequest;
import com.ntdhtcct.domain.milestone.dto.MilestoneWarningResponse;
import com.ntdhtcct.domain.project.Project;
import com.ntdhtcct.domain.project.ProjectRepository;
import com.ntdhtcct.domain.wbs.CpmEngine;
import com.ntdhtcct.domain.wbs.WbsItem;
import com.ntdhtcct.domain.wbs.WbsItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MilestoneServiceTest {

    @Mock
    private MilestoneRepository milestoneRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private WbsItemRepository wbsItemRepository;

    @Mock
    private CpmEngine cpmEngine;

    @InjectMocks
    private MilestoneService milestoneService;

    private UUID projectId;
    private Project project;
    private WbsItem category;

    @BeforeEach
    void setUp() {
        projectId = UUID.randomUUID();
        project = new Project("DA-01", "Dự án Tòa nhà phức hợp");
        setId(project, projectId);
        project.setStartDate(LocalDate.of(2026, 1, 1));

        category = new WbsItem();
        setId(category, UUID.randomUUID());
        category.setProjectId(projectId);
        category.setName("Hạng mục Kết cấu Móng");
        category.setWbsCode("1.1");
        category.setType("category");
    }

    @Test
    void t43_createMilestoneSuccessfullyLinkedToCategory() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(wbsItemRepository.findById(category.getId())).thenReturn(Optional.of(category));
        when(milestoneRepository.save(any(Milestone.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LocalDate targetDate = LocalDate.of(2026, 1, 10);
        CreateMilestoneRequest request = new CreateMilestoneRequest(
                category.getId(),
                "Mốc cất đài móng",
                targetDate,
                "Hoàn thành toàn bộ đài móng"
        );

        Milestone created = milestoneService.createMilestone(projectId, request);

        assertThat(created).isNotNull();
        assertThat(created.getName()).isEqualTo("Mốc cất đài móng");
        assertThat(created.getTargetDate()).isEqualTo(targetDate);
        assertThat(created.getCategory().getId()).isEqualTo(category.getId());
        verify(milestoneRepository).save(any(Milestone.class));
    }

    @Test
    void t43_rejectsMilestoneIfAttachedDirectlyToTaskInsteadOfCategory() {
        WbsItem taskItem = new WbsItem();
        setId(taskItem, UUID.randomUUID());
        taskItem.setProjectId(projectId);
        taskItem.setType("task");

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(wbsItemRepository.findById(taskItem.getId())).thenReturn(Optional.of(taskItem));

        CreateMilestoneRequest request = new CreateMilestoneRequest(
                taskItem.getId(),
                "Mốc sai",
                LocalDate.of(2026, 1, 10),
                null
        );

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                milestoneService.createMilestone(projectId, request)
        );
        assertThat(ex.getMessage()).contains("Chỉ có thể gắn mốc vào hạng mục");
    }

    @Test
    void t44_and_t45_comparesLastTaskEarlyFinishWithMilestoneAndTracesDelayChain() {
        // Tạo các công việc trong hạng mục Kết cấu Móng:
        // Task A (Đào móng): ES = 0, EF = 3 (ngày 1 đến 3)
        // Task B (Đổ bê tông lót): ES = 3, EF = 6 (ngày 4 đến 6), phụ thuộc A
        // Task C (Đổ đài móng): ES = 6, EF = 12 (ngày 7 đến 12), phụ thuộc B (Việc cuối hạng mục)
        WbsItem taskA = createTask("1.1.1", "Đào móng", 0, 3, Set.of());
        WbsItem taskB = createTask("1.1.2", "Đổ bê tông lót", 3, 6, Set.of(taskA.getId()));
        WbsItem taskC = createTask("1.1.3", "Đổ đài móng", 6, 12, Set.of(taskB.getId()));

        List<WbsItem> allItems = List.of(category, taskA, taskB, taskC);

        // Mốc yêu cầu hoàn thành vào ngày 2026-01-10 (tức ngày thứ 10)
        // Nhưng việc cuối (Task C) có EF = 12 -> ngày hoàn thành là 2026-01-12
        // Số ngày vượt: 12 - 10 = 2 ngày
        LocalDate milestoneTargetDate = LocalDate.of(2026, 1, 10);
        Milestone milestone = new Milestone(projectId, category, "Mốc đài móng", milestoneTargetDate, "Hoàn tất móng");
        setId(milestone, UUID.randomUUID());

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(milestoneRepository.findByProjectIdOrderByTargetDateAsc(projectId)).thenReturn(List.of(milestone));
        when(wbsItemRepository.findByProjectIdOrderByWbsCodeAsc(projectId)).thenReturn(new ArrayList<>(allItems));

        List<MilestoneWarningResponse> warnings = milestoneService.getMilestoneWarnings(projectId);

        assertThat(warnings).hasSize(1);
        MilestoneWarningResponse warning = warnings.get(0);

        // T-44: Xác minh so kết sớm việc cuối và số ngày vượt
        assertThat(warning.isOverrun()).isTrue();
        assertThat(warning.overrunDays()).isEqualTo(2);
        assertThat(warning.status()).isEqualTo("OVERRUN");
        assertThat(warning.lastTaskId()).isEqualTo(taskC.getId());
        assertThat(warning.lastTaskName()).isEqualTo("Đổ đài móng");
        assertThat(warning.lastTaskEarlyFinishDate()).isEqualTo(LocalDate.of(2026, 1, 12));

        // T-45: Xác minh chuỗi việc gây chậm từ việc đầu đến việc cuối
        assertThat(warning.delayChain()).hasSize(3);
        assertThat(warning.delayChain().get(0).id()).isEqualTo(taskA.getId());
        assertThat(warning.delayChain().get(1).id()).isEqualTo(taskB.getId());
        assertThat(warning.delayChain().get(2).id()).isEqualTo(taskC.getId());
    }

    @Test
    void t44_returnsOnTrackWhenLastTaskFinishesOnOrBeforeMilestone() {
        // Task hoàn thành vào ngày 5, mốc yêu cầu ngày 10 -> Không bị vượt
        WbsItem taskA = createTask("1.1.1", "Đào móng nhanh", 0, 5, Set.of());
        List<WbsItem> allItems = List.of(category, taskA);

        LocalDate milestoneTargetDate = LocalDate.of(2026, 1, 10);
        Milestone milestone = new Milestone(projectId, category, "Mốc hoàn thành sớm", milestoneTargetDate, null);
        setId(milestone, UUID.randomUUID());

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(milestoneRepository.findByProjectIdOrderByTargetDateAsc(projectId)).thenReturn(List.of(milestone));
        when(wbsItemRepository.findByProjectIdOrderByWbsCodeAsc(projectId)).thenReturn(new ArrayList<>(allItems));

        List<MilestoneWarningResponse> warnings = milestoneService.getMilestoneWarnings(projectId);

        assertThat(warnings).hasSize(1);
        MilestoneWarningResponse warning = warnings.get(0);

        assertThat(warning.isOverrun()).isFalse();
        assertThat(warning.overrunDays()).isZero();
        assertThat(warning.status()).isEqualTo("ON_TRACK");
        assertThat(warning.lastTaskId()).isEqualTo(taskA.getId());
    }

    private WbsItem createTask(String code, String name, int es, int ef, Set<UUID> predecessors) {
        WbsItem item = new WbsItem();
        setId(item, UUID.randomUUID());
        item.setProjectId(projectId);
        item.setParentId(category.getId());
        item.setWbsCode(code);
        item.setName(name);
        item.setType("task");
        item.setDuration(ef - es);
        item.setEs(es);
        item.setEf(ef);
        item.setCritical(true);
        item.setPredecessorIds(predecessors);
        return item;
    }

    private void setId(Object target, UUID id) {
        try {
            Field field = target.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(target, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
