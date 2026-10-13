package com.ntdhtcct.domain.wbs;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("S-35 / NTDHTCT-219: Khai báo khối lượng và đơn giá CategoryTask")
class CategoryTaskVolumeTest {

    private final TaskRepository taskRepository = mock(TaskRepository.class);
    private final WbsItemRepository wbsItemRepository = mock(WbsItemRepository.class);
    private final CategoryTaskService service = new CategoryTaskService(taskRepository, wbsItemRepository);

    private final UUID projectId = UUID.randomUUID();
    private final UUID categoryId = UUID.randomUUID();
    private WbsItem category;

    @BeforeEach
    void setUp() {
        category = new WbsItem();
        category.setProjectId(projectId);
        category.setType("phase");
        category.setName("Hạng mục Kết cấu");

        when(wbsItemRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("T-81: Tạo Task thuộc Category có khối lượng, đơn vị tính và đơn giá")
    void createsTaskWithQuantityAndPrice() {
        Task task = service.createTask(
                projectId,
                categoryId,
                "Lắp đặt cốt thép dầm",
                4,
                25.5,
                "tấn",
                18500000.0
        );

        assertThat(task.getName()).isEqualTo("Lắp đặt cốt thép dầm");
        assertThat(task.getDuration()).isEqualTo(4);
        assertThat(task.getQuantity()).isEqualTo(25.5);
        assertThat(task.getUnit()).isEqualTo("tấn");
        assertThat(task.getUnitPrice()).isEqualTo(18500000.0);
        assertThat(task.getTotalPrice()).isEqualTo(471750000.0);
        verify(taskRepository).save(any(Task.class));
    }

    @ParameterizedTest
    @ValueSource(doubles = {-0.5, -100.0})
    @DisplayName("T-82: Từ chối Task có khối lượng âm")
    void rejectsNegativeTaskQuantity(double negativeQty) {
        assertThatThrownBy(() -> service.createTask(
                projectId,
                categoryId,
                "Đổ bê tông",
                3,
                negativeQty,
                "m3",
                2000000.0
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Khối lượng công việc không được âm");

        verify(taskRepository, never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(doubles = {-10.0, -100000.0})
    @DisplayName("T-82: Từ chối Task có đơn giá âm")
    void rejectsNegativeTaskUnitPrice(double negativePrice) {
        assertThatThrownBy(() -> service.createTask(
                projectId,
                categoryId,
                "Đổ bê tông",
                3,
                50.0,
                "m3",
                negativePrice
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Đơn giá không được âm");

        verify(taskRepository, never()).save(any());
    }

    @Test
    @DisplayName("T-82: Bắt buộc đơn vị tính khi có khối lượng > 0")
    void requiresUnitWhenTaskQuantityIsSet() {
        assertThatThrownBy(() -> service.createTask(
                projectId,
                categoryId,
                "Đổ bê tông",
                3,
                50.0,
                "  ",
                2000000.0
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Vui lòng nhập đơn vị tính khi đã khai báo khối lượng");

        verify(taskRepository, never()).save(any());
    }
}
