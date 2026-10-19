package com.ntdhtcct.domain.budget;

import com.ntdhtcct.domain.project.Project;
import com.ntdhtcct.domain.project.ProjectRepository;
import com.ntdhtcct.domain.wbs.WbsItem;
import com.ntdhtcct.domain.wbs.WbsItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class BudgetService {

    private final WbsItemRepository wbsItemRepository;
    private final ProjectRepository projectRepository;

    public BudgetService(WbsItemRepository wbsItemRepository, ProjectRepository projectRepository) {
        this.wbsItemRepository = wbsItemRepository;
        this.projectRepository = projectRepository;
    }

    /**
     * T-91, T-92: Lấy toàn bộ cây WBS với dữ liệu dự toán đã được tính toán và cộng dồn từ lá lên cha.
     */
    @Transactional(readOnly = true)
    public List<BudgetItemResponse> getProjectBudgetTree(UUID projectId) {
        requireProject(projectId);
        List<WbsItem> items = wbsItemRepository.findByProjectIdOrderByWbsCodeAsc(projectId);

        Set<UUID> parentIds = getParentIds(items);
        Map<UUID, List<WbsItem>> childrenByParent = items.stream()
                .filter(item -> item.getParentId() != null)
                .collect(Collectors.groupingBy(WbsItem::getParentId));

        List<WbsItem> roots = items.stream()
                .filter(item -> item.getParentId() == null)
                .toList();

        return roots.stream()
                .map(root -> buildBudgetItemNode(root, childrenByParent, parentIds))
                .toList();
    }

    /**
     * T-91: Lấy thông tin thống kê tổng quan dự toán của dự án.
     */
    @Transactional(readOnly = true)
    public ProjectBudgetSummaryResponse getBudgetSummary(UUID projectId) {
        Project project = requireProject(projectId);
        List<WbsItem> items = wbsItemRepository.findByProjectIdOrderByWbsCodeAsc(projectId);

        Set<UUID> parentIds = getParentIds(items);
        List<WbsItem> leafItems = items.stream()
                .filter(item -> !parentIds.contains(item.getId()))
                .toList();

        int totalItems = items.size();
        int totalLeafItems = leafItems.size();
        int totalParentItems = totalItems - totalLeafItems;

        int budgetedLeafItems = 0;
        double totalBudget = 0.0;

        for (WbsItem leaf : leafItems) {
            Double qty = leaf.getTotalVolume();
            Double price = leaf.getUnitPrice();
            if (qty != null && qty > 0 && price != null && price > 0) {
                budgetedLeafItems++;
                totalBudget += Math.round(qty * price * 100.0) / 100.0;
            }
        }
        totalBudget = Math.round(totalBudget * 100.0) / 100.0;

        double completionRate = totalLeafItems > 0
                ? Math.round(((double) budgetedLeafItems / totalLeafItems) * 1000.0) / 10.0
                : 0.0;

        return new ProjectBudgetSummaryResponse(
                project.getId(),
                project.getCode(),
                project.getName(),
                totalBudget,
                totalItems,
                totalLeafItems,
                budgetedLeafItems,
                totalParentItems,
                completionRate
        );
    }

    /**
     * T-92, T-93: Cập nhật dự toán cho một hạng mục cụ thể.
     * Quy tắc bắt buộc của S-46: Chỉ cho phép nhập dự toán ở hạng mục lá (leaf node).
     * Nếu là hạng mục cha -> ném IllegalArgumentException.
     */
    @Transactional
    public BudgetItemResponse updateBudgetItem(UUID projectId, UUID itemId, UpdateBudgetItemRequest request) {
        requireProject(projectId);
        WbsItem item = requireItem(projectId, itemId);

        List<WbsItem> allItems = wbsItemRepository.findByProjectIdOrderByWbsCodeAsc(projectId);
        Set<UUID> parentIds = getParentIds(allItems);

        if (parentIds.contains(itemId)) {
            throw new IllegalArgumentException(
                    "Chỉ được nhập dự toán cho các hạng mục lá. Dự toán của hạng mục cha được tự động cộng dồn từ các hạng mục con."
            );
        }

        validateBudgetData(request.quantity(), request.unit(), request.unitPrice());

        item.setTotalVolume(request.quantity());
        item.setUnit(request.unit() != null ? request.unit().trim() : null);
        item.setUnitPrice(request.unitPrice());

        wbsItemRepository.save(item);

        // Nạp lại danh sách và tái cấu trúc cây để tính toán lại tổng cộng dồn
        List<WbsItem> refreshedItems = wbsItemRepository.findByProjectIdOrderByWbsCodeAsc(projectId);
        Map<UUID, List<WbsItem>> childrenByParent = refreshedItems.stream()
                .filter(i -> i.getParentId() != null)
                .collect(Collectors.groupingBy(WbsItem::getParentId));

        WbsItem updatedItem = refreshedItems.stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElse(item);

        return buildBudgetItemNode(updatedItem, childrenByParent, parentIds);
    }

    /**
     * T-92, T-93: Cập nhật hàng loạt (Batch update) dự toán cho nhiều hạng mục lá cùng lúc.
     */
    @Transactional
    public List<BudgetItemResponse> batchUpdateBudget(UUID projectId, BatchUpdateBudgetRequest request) {
        requireProject(projectId);
        List<WbsItem> allItems = wbsItemRepository.findByProjectIdOrderByWbsCodeAsc(projectId);
        Set<UUID> parentIds = getParentIds(allItems);
        Map<UUID, WbsItem> itemMap = allItems.stream()
                .collect(Collectors.toMap(WbsItem::getId, item -> item));

        for (BatchUpdateBudgetRequest.ItemBudgetEntry entry : request.items()) {
            WbsItem item = itemMap.get(entry.itemId());
            if (item == null) {
                throw new IllegalArgumentException("Công việc " + entry.itemId() + " không tồn tại trong dự án");
            }
            if (parentIds.contains(item.getId())) {
                throw new IllegalArgumentException(
                        "Không thể nhập dự toán cho hạng mục cha: \"" + item.getName() + "\" (" + item.getWbsCode() + "). Chỉ được nhập cho các hạng mục lá."
                );
            }
            validateBudgetData(entry.quantity(), entry.unit(), entry.unitPrice());

            item.setTotalVolume(entry.quantity());
            item.setUnit(entry.unit() != null ? entry.unit().trim() : null);
            item.setUnitPrice(entry.unitPrice());
        }

        wbsItemRepository.saveAll(itemMap.values());

        return getProjectBudgetTree(projectId);
    }

    /**
     * Lấy thông tin một node dự toán theo ID.
     */
    @Transactional(readOnly = true)
    public BudgetItemResponse getBudgetItem(UUID projectId, UUID itemId) {
        requireProject(projectId);
        WbsItem item = requireItem(projectId, itemId);
        List<WbsItem> allItems = wbsItemRepository.findByProjectIdOrderByWbsCodeAsc(projectId);
        Set<UUID> parentIds = getParentIds(allItems);
        Map<UUID, List<WbsItem>> childrenByParent = allItems.stream()
                .filter(i -> i.getParentId() != null)
                .collect(Collectors.groupingBy(WbsItem::getParentId));

        return buildBudgetItemNode(item, childrenByParent, parentIds);
    }

    private BudgetItemResponse buildBudgetItemNode(
            WbsItem item,
            Map<UUID, List<WbsItem>> childrenByParent,
            Set<UUID> parentIds
    ) {
        boolean isLeaf = !parentIds.contains(item.getId());
        List<WbsItem> rawChildren = childrenByParent.getOrDefault(item.getId(), Collections.emptyList());

        List<BudgetItemResponse> childResponses = new ArrayList<>();
        double rollupTotal = 0.0;

        for (WbsItem child : rawChildren) {
            BudgetItemResponse childNode = buildBudgetItemNode(child, childrenByParent, parentIds);
            childResponses.add(childNode);
            if (childNode.totalPrice() != null) {
                rollupTotal += childNode.totalPrice();
            }
        }
        rollupTotal = Math.round(rollupTotal * 100.0) / 100.0;

        Double directTotal = null;
        Double totalPrice;
        Double quantity = null;
        String unit = null;
        Double unitPrice = null;

        if (isLeaf) {
            quantity = item.getTotalVolume();
            unit = item.getUnit();
            unitPrice = item.getUnitPrice();
            if (quantity != null && unitPrice != null) {
                directTotal = Math.round(quantity * unitPrice * 100.0) / 100.0;
                totalPrice = directTotal;
            } else {
                directTotal = 0.0;
                totalPrice = 0.0;
            }
        } else {
            // Hạng mục cha: Thành tiền là tổng cộng dồn từ các nhánh con
            totalPrice = rollupTotal;
        }

        return new BudgetItemResponse(
                item.getId(),
                item.getProjectId(),
                item.getParentId(),
                item.getWbsCode(),
                item.getName(),
                item.getType(),
                item.getStatus(),
                isLeaf,
                quantity,
                unit,
                unitPrice,
                totalPrice,
                isLeaf ? directTotal : null,
                !isLeaf ? rollupTotal : null,
                childResponses.size(),
                childResponses
        );
    }

    private Set<UUID> getParentIds(List<WbsItem> items) {
        return items.stream()
                .map(WbsItem::getParentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private void validateBudgetData(Double quantity, String unit, Double unitPrice) {
        if (quantity != null && quantity < 0) {
            throw new IllegalArgumentException("Khối lượng công việc không được âm");
        }
        if (unitPrice != null && unitPrice < 0) {
            throw new IllegalArgumentException("Đơn giá không được âm");
        }
        if (unit != null && unit.length() > 50) {
            throw new IllegalArgumentException("Đơn vị tính không được vượt quá 50 ký tự");
        }
        if (quantity != null && quantity > 0) {
            if (unit == null || unit.trim().isEmpty()) {
                throw new IllegalArgumentException("Vui lòng nhập đơn vị tính khi đã khai báo khối lượng");
            }
        }
    }

    private Project requireProject(UUID projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Dự án không tồn tại"));
    }

    private WbsItem requireItem(UUID projectId, UUID itemId) {
        WbsItem item = wbsItemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Công việc không tồn tại"));

        if (!item.getProjectId().equals(projectId)) {
            throw new IllegalArgumentException("Công việc không thuộc dự án này");
        }

        return item;
    }
}
