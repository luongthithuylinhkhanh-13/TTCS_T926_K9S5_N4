package com.ntdhtcct.domain.acceptance;

import com.ntdhtcct.domain.acceptance.dto.AcceptanceItemRequest;
import com.ntdhtcct.domain.acceptance.dto.AcceptanceReportResponse;
import com.ntdhtcct.domain.acceptance.dto.AcceptanceSummaryResponse;
import com.ntdhtcct.domain.acceptance.dto.CreateAcceptanceReportRequest;
import com.ntdhtcct.domain.acceptance.dto.UpdateAcceptanceReportRequest;
import com.ntdhtcct.domain.project.ProjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service xử lý nghiệp vụ Lập phiếu nghiệm thu theo đợt (Story S-36: NTDHTCT-220)
 *   - NTDHTCT-268 (T-83): Tạo phiếu nghiệm thu theo từng đợt
 *   - NTDHTCT-269 (T-84): Chọn hạng mục và nhập khối lượng nghiệm thu
 *   - NTDHTCT-270 (T-85): Tự động tính giá trị nghiệm thu của phiếu (itemAmount & totalAmount)
 */
@Service
public class AcceptanceReportService {

    private final AcceptanceReportRepository acceptanceReportRepository;
    private final ProjectRepository projectRepository;

    public AcceptanceReportService(
            AcceptanceReportRepository acceptanceReportRepository,
            ProjectRepository projectRepository
    ) {
        this.acceptanceReportRepository = acceptanceReportRepository;
        this.projectRepository = projectRepository;
    }

    /**
     * T-83, T-84, T-85: Tạo phiếu nghiệm thu theo đợt và tự động tính toán giá trị
     */
    @Transactional
    public AcceptanceReportResponse createAcceptanceReport(
            UUID projectId,
            UUID creatorId,
            CreateAcceptanceReportRequest request
    ) {
        if (!projectRepository.existsById(projectId)) {
            throw new IllegalArgumentException("Dự án không tồn tại");
        }

        String reportNumber = request.getReportNumber();
        if (reportNumber == null || reportNumber.isBlank()) {
            int nextPeriod = getNextPeriodNumber(projectId);
            reportNumber = String.format("NT-%02d", nextPeriod);
        } else {
            reportNumber = reportNumber.trim();
        }

        if (acceptanceReportRepository.existsByProjectIdAndReportNumber(projectId, reportNumber)) {
            throw new IllegalArgumentException("Mã số biên bản nghiệm thu đã tồn tại trong dự án: " + reportNumber);
        }

        Integer periodNumber = request.getPeriodNumber();
        if (periodNumber == null || periodNumber < 1) {
            periodNumber = getNextPeriodNumber(projectId);
        }

        AcceptanceReport report = new AcceptanceReport(
                projectId,
                reportNumber,
                request.getName(),
                periodNumber,
                request.getAcceptanceDate(),
                request.getStatus(),
                creatorId,
                request.getNotes()
        );

        // T-84 & T-85: Thêm các hạng mục và tự động tính giá trị
        if (request.getItems() != null && !request.getItems().isEmpty()) {
            for (AcceptanceItemRequest itemReq : request.getItems()) {
                validateItemRequest(itemReq);
                AcceptanceItem item = new AcceptanceItem(
                        itemReq.getWbsItemId(),
                        itemReq.getTaskId(),
                        itemReq.getItemName(),
                        itemReq.getUnit(),
                        itemReq.getPlannedQuantity(),
                        itemReq.getAcceptedQuantity(),
                        itemReq.getUnitPrice(),
                        itemReq.getCumulativeQuantity(),
                        itemReq.getNotes()
                );
                report.addItem(item);
            }
        }

        // T-85: Tự động tính toán tổng giá trị nghiệm thu
        report.calculateTotalAmount();

        AcceptanceReport saved = acceptanceReportRepository.save(report);
        return AcceptanceReportResponse.from(saved);
    }

    /**
     * Lấy danh sách phiếu nghiệm thu theo dự án (sắp xếp theo đợt giảm dần)
     */
    @Transactional(readOnly = true)
    public List<AcceptanceReportResponse> getAcceptanceReportsByProject(UUID projectId) {
        return acceptanceReportRepository.findByProjectIdOrderByPeriodNumberDesc(projectId)
                .stream()
                .map(AcceptanceReportResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * Lấy chi tiết một phiếu nghiệm thu
     */
    @Transactional(readOnly = true)
    public Optional<AcceptanceReportResponse> getAcceptanceReportById(UUID projectId, UUID reportId) {
        return acceptanceReportRepository.findById(Objects.requireNonNull(reportId))
                .filter(r -> r.getProjectId().equals(projectId))
                .map(AcceptanceReportResponse::from);
    }

    /**
     * Cập nhật thông tin phiếu nghiệm thu và các hạng mục
     */
    @Transactional
    public Optional<AcceptanceReportResponse> updateAcceptanceReport(
            UUID projectId,
            UUID reportId,
            UpdateAcceptanceReportRequest request
    ) {
        return acceptanceReportRepository.findById(Objects.requireNonNull(reportId))
                .filter(r -> r.getProjectId().equals(projectId))
                .map(report -> {
                    report.setName(request.getName());
                    report.setAcceptanceDate(request.getAcceptanceDate());
                    if (request.getStatus() != null && !request.getStatus().isBlank()) {
                        report.setStatus(request.getStatus());
                    }
                    report.setNotes(request.getNotes());

                    // Cập nhật lại danh sách hạng mục (T-84, T-85)
                    if (request.getItems() != null) {
                        report.clearItems();
                        for (AcceptanceItemRequest itemReq : request.getItems()) {
                            validateItemRequest(itemReq);
                            AcceptanceItem item = new AcceptanceItem(
                                    itemReq.getWbsItemId(),
                                    itemReq.getTaskId(),
                                    itemReq.getItemName(),
                                    itemReq.getUnit(),
                                    itemReq.getPlannedQuantity(),
                                    itemReq.getAcceptedQuantity(),
                                    itemReq.getUnitPrice(),
                                    itemReq.getCumulativeQuantity(),
                                    itemReq.getNotes()
                            );
                            report.addItem(item);
                        }
                    }

                    // T-85: Tính lại tổng tiền
                    report.calculateTotalAmount();
                    AcceptanceReport updated = acceptanceReportRepository.save(report);
                    return AcceptanceReportResponse.from(updated);
                });
    }

    /**
     * Xóa phiếu nghiệm thu (chỉ cho phép xóa khi ở trạng thái DRAFT hoặc REJECTED)
     */
    @Transactional
    public boolean deleteAcceptanceReport(UUID projectId, UUID reportId) {
        Optional<AcceptanceReport> opt = acceptanceReportRepository.findById(Objects.requireNonNull(reportId))
                .filter(r -> r.getProjectId().equals(projectId));

        if (opt.isEmpty()) {
            return false;
        }

        AcceptanceReport report = opt.get();
        if ("APPROVED".equalsIgnoreCase(report.getStatus())) {
            throw new IllegalStateException("Không thể xóa phiếu nghiệm thu đã được phê duyệt");
        }

        acceptanceReportRepository.delete(report);
        return true;
    }

    /**
     * Lấy số đợt nghiệm thu tiếp theo được gợi ý cho dự án
     */
    @Transactional(readOnly = true)
    public int getNextPeriodNumber(UUID projectId) {
        Integer maxPeriod = acceptanceReportRepository.findMaxPeriodNumberByProjectId(projectId);
        return (maxPeriod != null ? maxPeriod : 0) + 1;
    }

    /**
     * Thống kê tổng hợp nghiệm thu của dự án
     */
    @Transactional(readOnly = true)
    public AcceptanceSummaryResponse getProjectAcceptanceSummary(UUID projectId) {
        long total = acceptanceReportRepository.countByProjectId(projectId);
        long draft = acceptanceReportRepository.countByProjectIdAndStatus(projectId, "DRAFT");
        long submitted = acceptanceReportRepository.countByProjectIdAndStatus(projectId, "SUBMITTED");
        long approved = acceptanceReportRepository.countByProjectIdAndStatus(projectId, "APPROVED");
        BigDecimal totalAmount = acceptanceReportRepository.sumTotalAmountByProjectId(projectId);
        BigDecimal approvedAmount = acceptanceReportRepository.sumApprovedAmountByProjectId(projectId);

        return new AcceptanceSummaryResponse(
                projectId,
                total,
                draft,
                submitted,
                approved,
                totalAmount,
                approvedAmount
        );
    }

    private void validateItemRequest(AcceptanceItemRequest item) {
        if (item.getAcceptedQuantity() != null && item.getAcceptedQuantity().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Khối lượng nghiệm thu không được âm: " + item.getItemName());
        }
        if (item.getUnitPrice() != null && item.getUnitPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Đơn giá không được âm: " + item.getItemName());
        }
    }
}
