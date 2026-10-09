package com.ntdhtcct.domain.wbs;

import com.ntdhtcct.domain.project.Project;
import com.ntdhtcct.domain.project.ProjectRepository;
import com.ntdhtcct.repository.ProjectMemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WbsServiceDurationTest {

    private final WbsItemRepository wbsItemRepository = mock(WbsItemRepository.class);
    private final ProjectRepository projectRepository = mock(ProjectRepository.class);
    private final ProjectMemberRepository projectMemberRepository =
            mock(ProjectMemberRepository.class);
    private final CpmEngine cpmEngine = new CpmEngine();
    private final WbsService service = new WbsService(
            wbsItemRepository,
            projectRepository,
            projectMemberRepository,
            cpmEngine
    );
    private final UUID projectId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        when(projectRepository.findById(projectId))
                .thenReturn(Optional.of(new Project("TEST", "Test project")));
        when(wbsItemRepository.existsByProjectIdAndWbsCode(projectId, "1.1"))
                .thenReturn(false);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -5})
    @NullSource
    void rejectsMissingOrNonPositiveTaskDuration(Integer duration) {
        WbsItem item = new WbsItem();
        item.setWbsCode("1.1");
        item.setName("Task");
        item.setDuration(duration);

        assertThatThrownBy(() -> service.createTask(projectId, item))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Thời lượng thực hiện phải lớn hơn 0");

        verify(wbsItemRepository, never()).save(item);
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 101})
    void rejectsProgressPercentOutsideRange(int progressPercent) {
        WbsItem item = new WbsItem();
        item.setWbsCode("1.1");
        item.setName("Task");
        item.setDuration(1);
        item.setProgressPercent(progressPercent);

        assertThatThrownBy(() -> service.createTask(projectId, item))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Tiến độ phải nằm trong khoảng 0 đến 100");

        verify(wbsItemRepository, never()).save(item);
    }

    @Test
    void rejectsActualEndDateWithoutActualStartDate() {
        WbsItem item = new WbsItem();
        item.setWbsCode("1.1");
        item.setName("Task");
        item.setDuration(1);
        item.setActualStartDate(null);
        item.setActualEndDate(LocalDate.parse("2026-10-10"));

        assertThatThrownBy(() -> service.createTask(projectId, item))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Không thể có ngày kết thúc thực tế khi chưa có ngày bắt đầu thực tế");

        verify(wbsItemRepository, never()).save(item);
    }

    @Test
    void rejectsActualEndDateBeforeActualStartDate() {
        WbsItem item = new WbsItem();
        item.setWbsCode("1.1");
        item.setName("Task");
        item.setDuration(1);
        item.setActualStartDate(LocalDate.parse("2026-10-10"));
        item.setActualEndDate(LocalDate.parse("2026-10-09"));

        assertThatThrownBy(() -> service.createTask(projectId, item))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Ngày kết thúc thực tế phải sau hoặc cùng ngày với ngày bắt đầu thực tế");

        verify(wbsItemRepository, never()).save(item);
    }

    @Test
    void persistsActualDatesAndProgressPercentOnUpdate() {
        UUID itemId = UUID.randomUUID();
        WbsItem existing = new WbsItem();
        existing.setProjectId(projectId);
        existing.setType("task");
        existing.setDuration(1);
        when(wbsItemRepository.findById(itemId)).thenReturn(Optional.of(existing));
        when(wbsItemRepository.findByProjectIdOrderByWbsCodeAsc(projectId))
                .thenReturn(List.of(), List.of());

        WbsItem request = new WbsItem();
        request.setName("Task");
        request.setType("task");
        request.setDuration(1);
        request.setProgressPercent(45);
        request.setActualStartDate(LocalDate.parse("2026-10-09"));
        request.setActualEndDate(LocalDate.parse("2026-10-10"));

        service.update(projectId, itemId, request);

        assertThat(existing.getActualStartDate()).isEqualTo(LocalDate.parse("2026-10-09"));
        assertThat(existing.getActualEndDate()).isEqualTo(LocalDate.parse("2026-10-10"));
        assertThat(existing.getProgress()).isEqualTo(45);
        assertThat(existing.getProgressPercent()).isEqualTo(45);
    }
}
