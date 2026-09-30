package com.ntdhtcct.domain.wbs;

import com.ntdhtcct.domain.project.Project;
import com.ntdhtcct.domain.project.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WbsServiceDurationTest {

    private final WbsItemRepository wbsItemRepository = mock(WbsItemRepository.class);
    private final ProjectRepository projectRepository = mock(ProjectRepository.class);
        private final CpmEngine cpmEngine = new CpmEngine();
    private final WbsService service = new WbsService(
            wbsItemRepository,
            projectRepository,
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
}
