package com.ntdhtcct.domain.cost;

import com.ntdhtcct.domain.cost.dto.ActualCostSummaryResponse;
import com.ntdhtcct.domain.cost.dto.CreateActualCostRequest;
import com.ntdhtcct.domain.wbs.Task;
import com.ntdhtcct.domain.wbs.TaskRepository;
import com.ntdhtcct.domain.wbs.WbsItem;
import com.ntdhtcct.domain.wbs.WbsItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("S-47 / NTDHTCT-237: Ghi nhận chi phí thực tế gắn với hạng mục")
class ActualCostServiceTest {

    private final ActualCostRepository actualCostRepository = mock(ActualCostRepository.class);
    private final WbsItemRepository wbsItemRepository = mock(WbsItemRepository.class);
    private final TaskRepository taskRepository = mock(TaskRepository.class);

    private final ActualCostService service = new ActualCostService(
            actualCostRepository,
            wbsItemRepository,
            taskRepository
    );

    private final UUID projectId = UUID.randomUUID();
    private final UUID wbsItemId = UUID.randomUUID();
    private WbsItem wbsItem;

    @BeforeEach
    void setUp() {
        wbsItem = new WbsItem();
        wbsItem.setProjectId(projectId);
        wbsItem.setName("Thi công móng đài cọc");
        wbsItem.setType("phase");

        when(wbsItemRepository.findById(wbsItemId)).thenReturn(Optional.of(wbsItem));
        when(actualCostRepository.save(any(ActualCost.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("T-94: Nhập chi phí thực tế hợp lệ thành công")
    void createsActualCostSuccessfully() {
        CreateActualCostRequest request = new CreateActualCostRequest(
                "Chi phí bê tông thương phẩm R28 mác 300",
                45000000.0,
                "VẬT TƯ",
                LocalDate.of(2026, 10, 15),
                null,
                null,
                "Đổ bê tông sàn tầng 1"
        );

        ActualCost result = service.createActualCost(projectId, request, "user-admin");

        assertThat(result.getName()).isEqualTo("Chi phí bê tông thương phẩm R28 mác 300");
        assertThat(result.getAmount()).isEqualTo(45000000.0);
        assertThat(result.getCostType()).isEqualTo("VẬT TƯ");
        assertThat(result.getIncurredDate()).isEqualTo(LocalDate.of(2026, 10, 15));
        assertThat(result.getCreatedBy()).isEqualTo("user-admin");
        verify(actualCostRepository).save(any(ActualCost.class));
    }

    @Test
    @DisplayName("T-94: Báo lỗi khi số tiền chi phí <= 0")
    void rejectsNonPositiveAmount() {
        CreateActualCostRequest request = new CreateActualCostRequest(
                "Chi phí nhân công",
                -1000.0,
                "NHÂN CÔNG",
                LocalDate.now(),
                null,
                null,
                null
        );

        assertThatThrownBy(() -> service.createActualCost(projectId, request, "user-admin"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Số tiền chi phí phải lớn hơn 0");
    }

    @Test
    @DisplayName("T-94: Báo lỗi khi tên chi phí để trống")
    void rejectsEmptyName() {
        CreateActualCostRequest request = new CreateActualCostRequest(
                "   ",
                5000000.0,
                "VẬT TƯ",
                LocalDate.now(),
                null,
                null,
                null
        );

        assertThatThrownBy(() -> service.createActualCost(projectId, request, "user-admin"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Tên chi phí không được để trống");
    }

    @Test
    @DisplayName("T-95: Liên kết chi phí với hạng mục tương ứng thành công")
    void linksCostToWbsItemSuccessfully() {
        CreateActualCostRequest request = new CreateActualCostRequest(
                "Chi phí sắt thép D20",
                82000000.0,
                "VẬT TƯ",
                LocalDate.of(2026, 10, 16),
                wbsItemId,
                null,
                "Thép phục vụ đài cọc móng"
        );

        ActualCost result = service.createActualCost(projectId, request, "user-admin");

        assertThat(result.getWbsItem()).isNotNull();
        assertThat(result.getWbsItem().getName()).isEqualTo("Thi công móng đài cọc");
        assertThat(result.getAmount()).isEqualTo(82000000.0);
        verify(wbsItemRepository).findById(wbsItemId);
    }

    @Test
    @DisplayName("T-95: Từ chối khi hạng mục không thuộc dự án hiện tại")
    void rejectsWbsItemFromDifferentProject() {
        UUID otherProjectId = UUID.randomUUID();
        WbsItem otherItem = new WbsItem();
        otherItem.setProjectId(otherProjectId);
        UUID otherItemId = UUID.randomUUID();
        when(wbsItemRepository.findById(otherItemId)).thenReturn(Optional.of(otherItem));

        CreateActualCostRequest request = new CreateActualCostRequest(
                "Chi phí coppha",
                12000000.0,
                "VẬT TƯ",
                LocalDate.now(),
                otherItemId,
                null,
                null
        );

        assertThatThrownBy(() -> service.createActualCost(projectId, request, "user-admin"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Hạng mục công việc không thuộc dự án này");
    }

    @Test
    @DisplayName("T-96: Lấy danh sách lịch sử chi phí và tính tổng chi phí thực tế")
    void retrievesCostHistoryAndSummary() {
        ActualCost cost1 = new ActualCost(projectId, wbsItem, null, "Chi phí 1", 10000000.0, "VẬT TƯ", LocalDate.now(), null, "admin");
        ActualCost cost2 = new ActualCost(projectId, wbsItem, null, "Chi phí 2", 15000000.0, "NHÂN CÔNG", LocalDate.now(), null, "admin");

        when(actualCostRepository.filterActualCosts(eq(projectId), eq(wbsItemId), isNull(), isNull(), isNull()))
                .thenReturn(List.of(cost1, cost2));
        when(actualCostRepository.sumTotalByProjectIdAndWbsItemId(projectId, wbsItemId))
                .thenReturn(25000000.0);
        when(actualCostRepository.findByProjectIdAndWbsItemIdOrderByIncurredDateDescCreatedAtDesc(projectId, wbsItemId))
                .thenReturn(List.of(cost1, cost2));

        List<ActualCost> history = service.getActualCosts(projectId, wbsItemId, null, null, null);
        assertThat(history).hasSize(2);

        ActualCostSummaryResponse summary = service.getSummary(projectId, wbsItemId);
        assertThat(summary.totalActualCost()).isEqualTo(25000000.0);
        assertThat(summary.totalRecords()).isEqualTo(2L);
    }
}
