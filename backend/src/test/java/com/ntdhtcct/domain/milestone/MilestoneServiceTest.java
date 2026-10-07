package com.ntdhtcct.domain.milestone;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ntdhtcct.domain.milestone.dto.CreateMilestoneRequest;
import com.ntdhtcct.domain.milestone.dto.MilestoneWarningResponse;
import com.ntdhtcct.domain.wbs.CpmEngine;
import com.ntdhtcct.domain.wbs.WbsItem;
import com.ntdhtcct.domain.wbs.WbsItemRepository;

@ExtendWith(MockitoExtension.class)
class MilestoneServiceTest {

    @Mock
    private MilestoneRepository milestoneRepository;

    @Mock
    private WbsItemRepository wbsItemRepository;

    @Mock
    private CpmEngine cpmEngine;

    @InjectMocks
    private MilestoneService milestoneService;

    private Long projectId;
    private WbsItem category;

    @BeforeEach
    void setUp() {
        projectId = 1L;

        category = new WbsItem();
        setId(category, UUID.randomUUID());
        category.setProjectId(projectId);
        category.setName("Hạng mục Kết cấu Móng");
        category.setWbsCode("1.1");
        category.setType("category");
    }

    @Test
    void t43_createMilestoneSuccessfullyLinkedToCategory() {
        when(wbsItemRepository.findByProjectIdOrderByWbsCodeAsc(projectId))
                .thenReturn(List.of(category));

        when(wbsItemRepository.findById(category.getId()))
                .thenReturn(java.util.Optional.of(category));

        when(milestoneRepository.save(any(Milestone.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        LocalDate targetDate = LocalDate.of(2026, 1, 10);

        CreateMilestoneRequest request = new CreateMilestoneRequest(
                category.getId(),
                "Mốc cất đài móng",
                targetDate,
                "Hoàn thành toàn bộ đài móng"
        );

        Milestone created =
                milestoneService.createMilestone(projectId, request);

        assertThat(created).isNotNull();
        assertThat(created.getProjectId()).isEqualTo(projectId);
        assertThat(created.getName()).isEqualTo("Mốc cất đài móng");
        assertThat(created.getTargetDate()).isEqualTo(targetDate);
        assertThat(created.getCategory().getId())
                .isEqualTo(category.getId());

        verify(milestoneRepository).save(any(Milestone.class));
    }

    @Test
    void t43_rejectsMilestoneIfAttachedDirectlyToTaskInsteadOfCategory() {

        WbsItem taskItem = new WbsItem();
        setId(taskItem, UUID.randomUUID());
        taskItem.setProjectId(projectId);
        taskItem.setType("task");

        when(wbsItemRepository.findByProjectIdOrderByWbsCodeAsc(projectId))
                .thenReturn(List.of(category));

        when(wbsItemRepository.findById(taskItem.getId()))
                .thenReturn(java.util.Optional.of(taskItem));

        CreateMilestoneRequest request = new CreateMilestoneRequest(
                taskItem.getId(),
                "Mốc sai",
                LocalDate.of(2026, 1, 10),
                null
        );

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> milestoneService.createMilestone(projectId, request)
        );

        assertThat(ex.getMessage())
                .contains("Chỉ có thể gắn mốc vào hạng mục");
    }

    @Test
    void t44_and_t45_comparesLastTaskEarlyFinishWithMilestoneAndTracesDelayChain() {

        WbsItem taskA =
                createTask("1.1.1", "Đào móng", 0, 3, Set.of());

        WbsItem taskB =
                createTask(
                        "1.1.2",
                        "Đổ bê tông lót",
                        3,
                        6,
                        Set.of(taskA.getId())
                );

        WbsItem taskC =
                createTask(
                        "1.1.3",
                        "Đổ đài móng",
                        6,
                        12,
                        Set.of(taskB.getId())
                );

        List<WbsItem> allItems =
                List.of(category, taskA, taskB, taskC);

        LocalDate milestoneTargetDate =
                LocalDate.of(2026, 1, 10);

        Milestone milestone =
                new Milestone(
                        projectId,
                        category,
                        "Mốc đài móng",
                        milestoneTargetDate,
                        "Hoàn tất móng"
                );

        setId(milestone, UUID.randomUUID());

        when(milestoneRepository
                .findByProjectIdOrderByTargetDateAsc(projectId))
                .thenReturn(List.of(milestone));

        when(wbsItemRepository
                .findByProjectIdOrderByWbsCodeAsc(projectId))
                .thenReturn(new ArrayList<>(allItems));

        List<MilestoneWarningResponse> warnings =
                milestoneService.getMilestoneWarnings(projectId);

        assertThat(warnings).hasSize(1);

        MilestoneWarningResponse warning =
                warnings.get(0);

        assertThat(warning.isOverrun()).isTrue();
        assertThat(warning.overrunDays()).isEqualTo(2);
        assertThat(warning.status()).isEqualTo("OVERRUN");
        assertThat(warning.lastTaskId())
                .isEqualTo(taskC.getId());
        assertThat(warning.lastTaskName())
                .isEqualTo("Đổ đài móng");
        assertThat(warning.lastTaskEarlyFinishDate())
                .isEqualTo(LocalDate.of(2026, 1, 12));

        assertThat(warning.delayChain()).hasSize(3);
        assertThat(warning.delayChain().get(0).id())
                .isEqualTo(taskA.getId());
        assertThat(warning.delayChain().get(1).id())
                .isEqualTo(taskB.getId());
        assertThat(warning.delayChain().get(2).id())
                .isEqualTo(taskC.getId());
    }

    @Test
    void t44_returnsOnTrackWhenLastTaskFinishesOnOrBeforeMilestone() {

        WbsItem taskA =
                createTask(
                        "1.1.1",
                        "Đào móng nhanh",
                        0,
                        5,
                        Set.of()
                );

        List<WbsItem> allItems =
                List.of(category, taskA);

        LocalDate milestoneTargetDate =
                LocalDate.of(2026, 1, 10);

        Milestone milestone =
                new Milestone(
                        projectId,
                        category,
                        "Mốc hoàn thành sớm",
                        milestoneTargetDate,
                        null
                );

        setId(milestone, UUID.randomUUID());

        when(milestoneRepository
                .findByProjectIdOrderByTargetDateAsc(projectId))
                .thenReturn(List.of(milestone));

        when(wbsItemRepository
                .findByProjectIdOrderByWbsCodeAsc(projectId))
                .thenReturn(new ArrayList<>(allItems));

        List<MilestoneWarningResponse> warnings =
                milestoneService.getMilestoneWarnings(projectId);

        assertThat(warnings).hasSize(1);

        MilestoneWarningResponse warning =
                warnings.get(0);

        assertThat(warning.isOverrun()).isFalse();
        assertThat(warning.overrunDays()).isZero();
        assertThat(warning.status()).isEqualTo("ON_TRACK");
        assertThat(warning.lastTaskId())
                .isEqualTo(taskA.getId());
    }

    private WbsItem createTask(
            String code,
            String name,
            int es,
            int ef,
            Set<UUID> predecessors
    ) {
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
            Field field =
                    target.getClass().getDeclaredField("id");

            field.setAccessible(true);
            field.set(target, id);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}