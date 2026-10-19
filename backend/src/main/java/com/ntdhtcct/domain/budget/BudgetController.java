package com.ntdhtcct.domain.budget;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/projects/{projectId}/budget")
public class BudgetController {

    private final BudgetService budgetService;

    public BudgetController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    /**
     * T-91, T-92: Lấy cây WBS dự toán kèm các giá trị cộng dồn lên cấp cha và toàn dự án.
     */
    @GetMapping
    public ResponseEntity<List<BudgetItemResponse>> getBudgetTree(@PathVariable UUID projectId) {
        return ResponseEntity.ok(budgetService.getProjectBudgetTree(projectId));
    }

    /**
     * T-91: Lấy tổng hợp số liệu thống kê dự toán của dự án.
     */
    @GetMapping("/summary")
    public ResponseEntity<ProjectBudgetSummaryResponse> getBudgetSummary(@PathVariable UUID projectId) {
        return ResponseEntity.ok(budgetService.getBudgetSummary(projectId));
    }

    /**
     * Lấy chi tiết một node dự toán theo ID.
     */
    @GetMapping("/{itemId}")
    public ResponseEntity<BudgetItemResponse> getBudgetItem(
            @PathVariable UUID projectId,
            @PathVariable UUID itemId
    ) {
        return ResponseEntity.ok(budgetService.getBudgetItem(projectId, itemId));
    }

    /**
     * T-92, T-93: Cập nhật dự toán cho một hạng mục lá.
     * Tự động chặn nhập nếu là hạng mục cha và trả về HTTP 400.
     */
    @PutMapping("/{itemId}")
    public ResponseEntity<?> updateBudgetItem(
            @PathVariable UUID projectId,
            @PathVariable UUID itemId,
            @Valid @RequestBody UpdateBudgetItemRequest request
    ) {
        try {
            BudgetItemResponse response = budgetService.updateBudgetItem(projectId, itemId, request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * T-92, T-93: Cập nhật hàng loạt (Batch update) dự toán cho nhiều hạng mục lá.
     */
    @PutMapping("/batch")
    public ResponseEntity<?> batchUpdateBudget(
            @PathVariable UUID projectId,
            @Valid @RequestBody BatchUpdateBudgetRequest request
    ) {
        try {
            List<BudgetItemResponse> response = budgetService.batchUpdateBudget(projectId, request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }
}
