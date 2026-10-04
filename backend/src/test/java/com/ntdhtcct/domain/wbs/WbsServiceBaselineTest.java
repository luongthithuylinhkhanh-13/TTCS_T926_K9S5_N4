package com.ntdhtcct.domain.wbs;

import com.ntdhtcct.domain.project.Project;
import com.ntdhtcct.domain.project.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WbsServiceBaselineTest {

    private final WbsItemRepository wbsItemRepository = mock(WbsItemRepository.class);
    private final ProjectScheduleBaselineRepository baselineRepository =
            mock(ProjectScheduleBaselineRepository.class);
    private final ProjectRepository projectRepository = mock(ProjectRepository.class);
    private final WbsService service = new WbsService(
            wbsItemRepository,
            baselineRepository,
            projectRepository,
            new CpmEngine()
    );

    private final UUID projectId = UUID.randomUUID();
    private final UUID taskId = UUID.randomUUID();
    private List<ProjectScheduleBaselineItem> persistedBaselineItems;
    private Project project;
    private WbsItem task;

    @BeforeEach
    void setUp() {
        project = new Project("TEST", "Test project");
        task = new WbsItem();
        ReflectionTestUtils.setField(task, "id", taskId);
        task.setProjectId(projectId);
        task.setWbsCode("1.1");
        task.setName("Foundation");
        task.setType("task");
        task.setDuration(5);
        task.setStartDate(LocalDate.parse("2026-10-01"));
        task.setEndDate(LocalDate.parse("2026-10-05"));

        when(projectRepository.findByIdForUpdate(projectId))
                .thenReturn(Optional.of(project));
        when(wbsItemRepository.findByProjectIdOrderByWbsCodeAsc(projectId))
                .thenReturn(List.of(task));
        when(baselineRepository.findByProjectIdOrderByWbsCodeAsc(projectId))
                .thenReturn(List.of());
        when(baselineRepository.saveAll(anyList()))
                .thenAnswer(this::saveBaselineItems);
    }

    private List<ProjectScheduleBaselineItem> saveBaselineItems(InvocationOnMock invocation) {
        persistedBaselineItems = invocation.getArgument(0);
        return persistedBaselineItems;
    }

    @Test
    void capturesTheInitialPlanWhenScheduleIsFirstCalculated() {
        ProjectScheduleResponse response = service.getSchedule(projectId);

        verify(baselineRepository).saveAll(anyList());

        ProjectScheduleBaselineItem savedBaseline = persistedBaselineItems.get(0);
        assertThat(savedBaseline.getProjectId()).isEqualTo(projectId);
        assertThat(savedBaseline.getItemId()).isEqualTo(taskId);
        assertThat(savedBaseline.getStartDate()).isEqualTo(LocalDate.parse("2026-10-01"));
        assertThat(savedBaseline.getEndDate()).isEqualTo(LocalDate.parse("2026-10-05"));
        assertThat(response.baselineCapturedAt()).isNotNull();
        assertThat(response.baselineTasks()).hasSize(1);
        assertThat(project.getScheduleBaselineCapturedAt()).isEqualTo(response.baselineCapturedAt());
    }

    @Test
    void recalculatingDoesNotOverwriteTheCapturedPlan() {
        ProjectScheduleBaselineItem savedBaseline = new ProjectScheduleBaselineItem(
                projectId,
                taskId,
                "1.1",
                "Foundation",
                LocalDate.parse("2026-10-01"),
                LocalDate.parse("2026-10-05"),
                5,
                java.time.OffsetDateTime.parse("2026-10-01T00:00:00Z")
        );
        project.setScheduleBaselineCapturedAt(savedBaseline.getCapturedAt());
        task.setStartDate(LocalDate.parse("2026-10-10"));
        task.setEndDate(LocalDate.parse("2026-10-14"));
        when(baselineRepository.findByProjectIdOrderByWbsCodeAsc(projectId))
                .thenReturn(List.of(savedBaseline));

        ProjectScheduleResponse response = service.getSchedule(projectId);

        verify(baselineRepository, never()).saveAll(anyList());
        assertThat(response.tasks().get(0).getStartDate()).isEqualTo(LocalDate.parse("2026-10-10"));
        assertThat(response.baselineTasks().get(0).startDate()).isEqualTo(LocalDate.parse("2026-10-01"));
        assertThat(response.baselineTasks().get(0).endDate()).isEqualTo(LocalDate.parse("2026-10-05"));
    }
}
