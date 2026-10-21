package com.ntdhtcct.domain.budget;

import com.ntdhtcct.domain.project.Project;
import com.ntdhtcct.domain.project.ProjectRepository;
import com.ntdhtcct.domain.wbs.WbsItem;
import com.ntdhtcct.domain.wbs.WbsItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BudgetServiceTest {

    @Mock
    private WbsItemRepository wbsItemRepository;

    @Mock
    private ProjectRepository projectRepository;

    @InjectMocks
    private BudgetService budgetService;

    private UUID projectId;
    private Project project;

    @BeforeEach
    void setUp() throws Exception {
        projectId = UUID.randomUUID();
        project = new Project("DA-001", "Dự án Tòa nhà Green Tower");
        setField(project, "id", projectId);

        lenient().when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private WbsItem createItem(UUID id, UUID parentId, String wbsCode, String name, String type, Double volume, String unit, Double unitPrice) throws Exception {
        WbsItem item = new WbsItem();
        setField(item, "id", id);
        setField(item, "projectId", projectId);
        setField(item, "parentId", parentId);
        setField(item, "wbsCode", wbsCode);
        setField(item, "name", name);
        setField(item, "type", type);
        setField(item, "totalVolume", volume);
        setField(item, "unit", unit);
        setField(item, "unitPrice", unitPrice);
        return item;
    }

    @Test
    @DisplayName("T-92: Tính đúng thành tiền dự toán của hạng mục lá (quantity * unitPrice)")
    void testLeafBudgetCalculation_Success() throws Exception {
        UUID parentId = UUID.randomUUID();
        UUID leafId = UUID.randomUUID();

        WbsItem parent = createItem(parentId, null, "1.0", "Phần móng", "phase", null, null, null);
        WbsItem leaf = createItem(leafId, parentId, "1.1", "Đào đất móng", "task", 150.0, "m3", 250000.0);

        when(wbsItemRepository.findByProjectIdOrderByWbsCodeAsc(projectId)).thenReturn(List.of(parent, leaf));

        List<BudgetItemResponse> tree = budgetService.getProjectBudgetTree(projectId);

        assertThat(tree).hasSize(1);
        BudgetItemResponse parentNode = tree.get(0);
        assertThat(parentNode.isLeaf()).isFalse();
        assertThat(parentNode.totalPrice()).isEqualTo(37500000.0); // Rollup from child
        assertThat(parentNode.children()).hasSize(1);

        BudgetItemResponse leafNode = parentNode.children().get(0);
        assertThat(leafNode.isLeaf()).isTrue();
        assertThat(leafNode.quantity()).isEqualTo(150.0);
        assertThat(leafNode.unit()).isEqualTo("m3");
        assertThat(leafNode.unitPrice()).isEqualTo(250000.0);
        assertThat(leafNode.totalPrice()).isEqualTo(37500000.0);
        assertThat(leafNode.directTotalPrice()).isEqualTo(37500000.0);
    }

    @Test
    @DisplayName("T-92: Cộng dồn dự toán từ nhiều hạng mục lá lên hạng mục cha (Rollup Aggregation)")
    void testRollupToParent_TwoLevels() throws Exception {
        UUID parentId = UUID.randomUUID();
        UUID leaf1Id = UUID.randomUUID();
        UUID leaf2Id = UUID.randomUUID();

        WbsItem parent = createItem(parentId, null, "1.0", "Phần kết cấu", "phase", null, null, null);
        // Task 1: 50 * 1,200,000 = 60,000,000
        WbsItem leaf1 = createItem(leaf1Id, parentId, "1.1", "Bê tông dầm", "task", 50.0, "m3", 1200000.0);
        // Task 2: 10 * 18,500,000 = 185,000,000
        WbsItem leaf2 = createItem(leaf2Id, parentId, "1.2", "Cốt thép dầm", "task", 10.0, "tấn", 18500000.0);

        when(wbsItemRepository.findByProjectIdOrderByWbsCodeAsc(projectId)).thenReturn(List.of(parent, leaf1, leaf2));

        List<BudgetItemResponse> tree = budgetService.getProjectBudgetTree(projectId);

        assertThat(tree).hasSize(1);
        BudgetItemResponse parentNode = tree.get(0);
        assertThat(parentNode.totalPrice()).isEqualTo(245000000.0); // 60M + 185M
        assertThat(parentNode.rollupTotalPrice()).isEqualTo(245000000.0);
    }

    @Test
    @DisplayName("T-92: Cộng dồn dự toán đa cấp (Cụ -> Ông -> Cha -> Con lá)")
    void testMultiLevelRollup() throws Exception {
        UUID rootId = UUID.randomUUID();
        UUID subPhaseId = UUID.randomUUID();
        UUID leaf1Id = UUID.randomUUID();
        UUID leaf2Id = UUID.randomUUID();

        WbsItem root = createItem(rootId, null, "1", "Khối công trình chính", "project_phase", null, null, null);
        WbsItem subPhase = createItem(subPhaseId, rootId, "1.1", "Phần thô", "phase", null, null, null);
        WbsItem leaf1 = createItem(leaf1Id, subPhaseId, "1.1.1", "Xây tường", "task", 200.0, "m2", 180000.0); // 36,000,000
        WbsItem leaf2 = createItem(leaf2Id, subPhaseId, "1.1.2", "Trát tường", "task", 400.0, "m2", 90000.0);   // 36,000,000

        when(wbsItemRepository.findByProjectIdOrderByWbsCodeAsc(projectId)).thenReturn(List.of(root, subPhase, leaf1, leaf2));

        List<BudgetItemResponse> tree = budgetService.getProjectBudgetTree(projectId);

        assertThat(tree).hasSize(1);
        BudgetItemResponse rootNode = tree.get(0);
        assertThat(rootNode.totalPrice()).isEqualTo(72000000.0);

        BudgetItemResponse subPhaseNode = rootNode.children().get(0);
        assertThat(subPhaseNode.totalPrice()).isEqualTo(72000000.0);
    }

    @Test
    @DisplayName("T-92, T-93: Chặn nhập dự toán trực tiếp cho hạng mục cha, ném IllegalArgumentException")
    void testInputBudgetOnParentItem_ThrowsException() throws Exception {
        UUID parentId = UUID.randomUUID();
        UUID leafId = UUID.randomUUID();

        WbsItem parent = createItem(parentId, null, "1.0", "Phần móng", "phase", null, null, null);
        WbsItem leaf = createItem(leafId, parentId, "1.1", "Đào móng", "task", 10.0, "m3", 100000.0);

        when(wbsItemRepository.findById(parentId)).thenReturn(Optional.of(parent));
        when(wbsItemRepository.findByProjectIdOrderByWbsCodeAsc(projectId)).thenReturn(List.of(parent, leaf));

        UpdateBudgetItemRequest request = new UpdateBudgetItemRequest(100.0, "gói", 50000000.0);

        assertThatThrownBy(() -> budgetService.updateBudgetItem(projectId, parentId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Chỉ được nhập dự toán cho các hạng mục lá");

        verify(wbsItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("T-93: Cập nhật thành công dự toán cho hạng mục lá")
    void testUpdateBudgetItem_Leaf_Success() throws Exception {
        UUID parentId = UUID.randomUUID();
        UUID leafId = UUID.randomUUID();

        WbsItem parent = createItem(parentId, null, "1.0", "Giai đoạn 1", "phase", null, null, null);
        WbsItem leaf = createItem(leafId, parentId, "1.1", "Lắp đặt cốt pha", "task", null, null, null);

        when(wbsItemRepository.findById(leafId)).thenReturn(Optional.of(leaf));
        when(wbsItemRepository.findByProjectIdOrderByWbsCodeAsc(projectId)).thenReturn(List.of(parent, leaf));
        when(wbsItemRepository.save(any(WbsItem.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateBudgetItemRequest request = new UpdateBudgetItemRequest(85.5, "m2", 320000.0);
        BudgetItemResponse result = budgetService.updateBudgetItem(projectId, leafId, request);

        assertThat(result.isLeaf()).isTrue();
        assertThat(result.quantity()).isEqualTo(85.5);
        assertThat(result.unit()).isEqualTo("m2");
        assertThat(result.unitPrice()).isEqualTo(320000.0);
        assertThat(result.totalPrice()).isEqualTo(27360000.0);

        verify(wbsItemRepository).save(leaf);
    }

    @Test
    @DisplayName("T-93: Kiểm tra tính hợp lệ dữ liệu dự toán (khối lượng âm, đơn giá âm, thiếu ĐVT)")
    void testValidation_Errors() throws Exception {
        UUID parentId = UUID.randomUUID();
        UUID leafId = UUID.randomUUID();

        WbsItem parent = createItem(parentId, null, "1.0", "Phase 1", "phase", null, null, null);
        WbsItem leaf = createItem(leafId, parentId, "1.1", "Công việc 1", "task", null, null, null);

        when(wbsItemRepository.findById(leafId)).thenReturn(Optional.of(leaf));
        when(wbsItemRepository.findByProjectIdOrderByWbsCodeAsc(projectId)).thenReturn(List.of(parent, leaf));

        // 1. Khối lượng âm
        assertThatThrownBy(() -> budgetService.updateBudgetItem(projectId, leafId, new UpdateBudgetItemRequest(-10.0, "m3", 100000.0)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Khối lượng công việc không được âm");

        // 2. Đơn giá âm
        assertThatThrownBy(() -> budgetService.updateBudgetItem(projectId, leafId, new UpdateBudgetItemRequest(10.0, "m3", -50000.0)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Đơn giá không được âm");

        // 3. Có khối lượng nhưng không có ĐVT
        assertThatThrownBy(() -> budgetService.updateBudgetItem(projectId, leafId, new UpdateBudgetItemRequest(10.0, "", 100000.0)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Vui lòng nhập đơn vị tính khi đã khai báo khối lượng");

        // 4. ĐVT dài quá 50 ký tự
        String longUnit = "a".repeat(51);
        assertThatThrownBy(() -> budgetService.updateBudgetItem(projectId, leafId, new UpdateBudgetItemRequest(10.0, longUnit, 100000.0)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Đơn vị tính không được vượt quá 50 ký tự");
    }

    @Test
    @DisplayName("T-92, T-93: Cập nhật hàng loạt (Batch update) dự toán cho nhiều hạng mục lá")
    void testBatchUpdateBudget_Success() throws Exception {
        UUID parentId = UUID.randomUUID();
        UUID leaf1Id = UUID.randomUUID();
        UUID leaf2Id = UUID.randomUUID();

        WbsItem parent = createItem(parentId, null, "1.0", "Phần móng", "phase", null, null, null);
        WbsItem leaf1 = createItem(leaf1Id, parentId, "1.1", "Đào móng", "task", null, null, null);
        WbsItem leaf2 = createItem(leaf2Id, parentId, "1.2", "Đổ bê tông lót", "task", null, null, null);

        when(wbsItemRepository.findByProjectIdOrderByWbsCodeAsc(projectId)).thenReturn(List.of(parent, leaf1, leaf2));

        BatchUpdateBudgetRequest request = new BatchUpdateBudgetRequest(List.of(
                new BatchUpdateBudgetRequest.ItemBudgetEntry(leaf1Id, 100.0, "m3", 150000.0), // 15,000,000
                new BatchUpdateBudgetRequest.ItemBudgetEntry(leaf2Id, 40.0, "m3", 950000.0)   // 38,000,000
        ));

        List<BudgetItemResponse> tree = budgetService.batchUpdateBudget(projectId, request);

        assertThat(tree).hasSize(1);
        BudgetItemResponse parentNode = tree.get(0);
        assertThat(parentNode.totalPrice()).isEqualTo(53000000.0); // 15M + 38M
        verify(wbsItemRepository).saveAll(any());
    }

    @Test
    @DisplayName("T-91: Thống kê tổng hợp số liệu dự toán (Budget Summary)")
    void testGetBudgetSummary_CalculatesCorrectStats() throws Exception {
        UUID parent1Id = UUID.randomUUID();
        UUID parent2Id = UUID.randomUUID();
        UUID leaf1Id = UUID.randomUUID();
        UUID leaf2Id = UUID.randomUUID();
        UUID leaf3Id = UUID.randomUUID();

        WbsItem parent1 = createItem(parent1Id, null, "1.0", "Giai đoạn 1", "phase", null, null, null);
        WbsItem parent2 = createItem(parent2Id, null, "2.0", "Giai đoạn 2", "phase", null, null, null);
        WbsItem leaf1 = createItem(leaf1Id, parent1Id, "1.1", "Hạng mục 1", "task", 10.0, "m3", 1000000.0); // 10M
        WbsItem leaf2 = createItem(leaf2Id, parent1Id, "1.2", "Hạng mục 2", "task", 20.0, "tấn", 2000000.0); // 40M
        WbsItem leaf3 = createItem(leaf3Id, parent2Id, "2.1", "Hạng mục 3 chưa có dự toán", "task", null, null, null);

        when(wbsItemRepository.findByProjectIdOrderByWbsCodeAsc(projectId))
                .thenReturn(List.of(parent1, parent2, leaf1, leaf2, leaf3));

        ProjectBudgetSummaryResponse summary = budgetService.getBudgetSummary(projectId);

        assertThat(summary.projectId()).isEqualTo(projectId);
        assertThat(summary.totalBudget()).isEqualTo(50000000.0);
        assertThat(summary.totalItems()).isEqualTo(5);
        assertThat(summary.totalParentItems()).isEqualTo(2);
        assertThat(summary.totalLeafItems()).isEqualTo(3);
        assertThat(summary.budgetedLeafItems()).isEqualTo(2);
        assertThat(summary.completionRate()).isEqualTo(66.7);
    }
}
