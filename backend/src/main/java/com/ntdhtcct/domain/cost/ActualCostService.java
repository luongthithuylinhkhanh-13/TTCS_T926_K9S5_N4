package com.ntdhtcct.domain.cost;

import com.ntdhtcct.domain.cost.dto.ActualCostSummaryResponse;
import com.ntdhtcct.domain.cost.dto.CreateActualCostRequest;
import com.ntdhtcct.domain.wbs.Task;
import com.ntdhtcct.domain.wbs.TaskRepository;
import com.ntdhtcct.domain.wbs.WbsItem;
import com.ntdhtcct.domain.wbs.WbsItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ActualCostService {

    private final ActualCostRepository actualCostRepository;
    private final WbsItemRepository wbsItemRepository;
    private final TaskRepository taskRepository;

    public ActualCostService(
            ActualCostRepository actualCostRepository,
            WbsItemRepository wbsItemRepository,
            TaskRepository taskRepository
    ) {
        this.actualCostRepository = actualCostRepository;
        this.wbsItemRepository = wbsItemRepository;
        this.taskRepository = taskRepository;
    }

    /**
     * T-94: Xây dựng chức năng nhập chi phí thực tế
     * T-95: Liên kết chi phí với hạng mục tương ứng
     */
    public ActualCost createActualCost(UUID projectId, CreateActualCostRequest request, String createdBy) {
        if (projectId == null) {
            throw new IllegalArgumentException("ID dự án không được để trống");
        }

        if (request.name() == null || request.name().trim().isEmpty()) {
            throw new IllegalArgumentException("Tên chi phí không được để trống");
        }

        if (request.amount() == null || request.amount() <= 0) {
            throw new IllegalArgumentException("Số tiền chi phí phải lớn hơn 0");
        }

        if (request.costType() == null || request.costType().trim().isEmpty()) {
            throw new IllegalArgumentException("Loại chi phí không được để trống");
        }

        if (request.incurredDate() == null) {
            throw new IllegalArgumentException("Ngày phát sinh chi phí không được để trống");
        }

        WbsItem wbsItem = null;
        if (request.wbsItemId() != null) {
            wbsItem = wbsItemRepository.findById(request.wbsItemId())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hạng mục công việc"));

            if (!projectId.equals(wbsItem.getProjectId())) {
                throw new IllegalArgumentException("Hạng mục công việc không thuộc dự án này");
            }
        }

        Task task = null;
        if (request.taskId() != null) {
            task = taskRepository.findById(request.taskId())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy công việc"));
        }

        ActualCost actualCost = new ActualCost(
                projectId,
                wbsItem,
                task,
                request.name().trim(),
                request.amount(),
                request.costType().trim(),
                request.incurredDate(),
                request.note(),
                createdBy
        );

        return actualCostRepository.save(actualCost);
    }

    /**
     * T-96: Lưu và hiển thị lịch sử chi phí
     */
    @Transactional(readOnly = true)
    public List<ActualCost> getActualCosts(
            UUID projectId,
            UUID wbsItemId,
            String costType,
            LocalDate startDate,
            LocalDate endDate
    ) {
        if (projectId == null) {
            throw new IllegalArgumentException("ID dự án không được để trống");
        }
        return actualCostRepository.filterActualCosts(projectId, wbsItemId, costType, startDate, endDate);
    }

    /**
     * Lấy tổng chi phí thực tế theo dự án hoặc theo hạng mục
     */
    @Transactional(readOnly = true)
    public ActualCostSummaryResponse getSummary(UUID projectId, UUID wbsItemId) {
        if (projectId == null) {
            throw new IllegalArgumentException("ID dự án không được để trống");
        }

        Double totalAmount;
        long count;
        if (wbsItemId != null) {
            totalAmount = actualCostRepository.sumTotalByProjectIdAndWbsItemId(projectId, wbsItemId);
            count = actualCostRepository.findByProjectIdAndWbsItemIdOrderByIncurredDateDescCreatedAtDesc(projectId, wbsItemId).size();
        } else {
            totalAmount = actualCostRepository.sumTotalByProjectId(projectId);
            count = actualCostRepository.findByProjectIdOrderByIncurredDateDescCreatedAtDesc(projectId).size();
        }

        return new ActualCostSummaryResponse(projectId, wbsItemId, totalAmount, count);
    }
}
