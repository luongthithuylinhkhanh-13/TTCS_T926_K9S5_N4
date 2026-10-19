package com.ntdhtcct.domain.acceptance;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AcceptanceReportRepository extends JpaRepository<AcceptanceReport, UUID> {

    /**
     * Lấy danh sách phiếu nghiệm thu theo dự án sắp xếp theo số đợt giảm dần.
     */
    List<AcceptanceReport> findByProjectIdOrderByPeriodNumberDesc(UUID projectId);

    /**
     * Lấy danh sách phiếu nghiệm thu theo dự án sắp xếp theo ngày nghiệm thu giảm dần.
     */
    List<AcceptanceReport> findByProjectIdOrderByAcceptanceDateDesc(UUID projectId);

    /**
     * Tìm phiếu nghiệm thu theo mã số biên bản trong dự án.
     */
    Optional<AcceptanceReport> findByProjectIdAndReportNumber(UUID projectId, String reportNumber);

    /**
     * Kiểm tra mã biên bản nghiệm thu đã tồn tại trong dự án chưa.
     */
    boolean existsByProjectIdAndReportNumber(UUID projectId, String reportNumber);

    /**
     * Đếm tổng số phiếu nghiệm thu của dự án.
     */
    long countByProjectId(UUID projectId);

    /**
     * Đếm số phiếu theo trạng thái.
     */
    long countByProjectIdAndStatus(UUID projectId, String status);

    /**
     * Tính tổng giá trị các phiếu nghiệm thu đã được duyệt (APPROVED) của dự án.
     */
    @Query("SELECT COALESCE(SUM(r.totalAmount), 0) FROM AcceptanceReport r WHERE r.projectId = :projectId AND r.status = 'APPROVED'")
    BigDecimal sumApprovedAmountByProjectId(@Param("projectId") UUID projectId);

    /**
     * Tính tổng giá trị tất cả phiếu nghiệm thu của dự án.
     */
    @Query("SELECT COALESCE(SUM(r.totalAmount), 0) FROM AcceptanceReport r WHERE r.projectId = :projectId")
    BigDecimal sumTotalAmountByProjectId(@Param("projectId") UUID projectId);

    /**
     * Lấy số đợt nghiệm thu lớn nhất hiện tại của dự án để tự động tăng số đợt tiếp theo.
     */
    @Query("SELECT COALESCE(MAX(r.periodNumber), 0) FROM AcceptanceReport r WHERE r.projectId = :projectId")
    Integer findMaxPeriodNumberByProjectId(@Param("projectId") UUID projectId);
}
