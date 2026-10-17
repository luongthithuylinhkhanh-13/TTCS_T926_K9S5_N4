package com.ntdhtcct.domain.acceptance;

import com.ntdhtcct.domain.acceptance.dto.AcceptanceItemRequest;
import com.ntdhtcct.domain.acceptance.dto.AcceptanceReportResponse;
import com.ntdhtcct.domain.acceptance.dto.AcceptanceSummaryResponse;
import com.ntdhtcct.domain.acceptance.dto.CreateAcceptanceReportRequest;
import com.ntdhtcct.domain.acceptance.dto.UpdateAcceptanceReportRequest;
import com.ntdhtcct.domain.project.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AcceptanceReportServiceTest {

    @Mock
    private AcceptanceReportRepository acceptanceReportRepository;

    @Mock
    private AcceptanceRecordRepository acceptanceRecordRepository;

    @Mock
    private ProjectRepository projectRepository;

    private AcceptanceReportService acceptanceReportService;

    private final UUID projectId = UUID.randomUUID();
    private final UUID creatorId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        acceptanceReportService = new AcceptanceReportService(
                acceptanceReportRepository,
                acceptanceRecordRepository,
                projectRepository
        );
    }

    @Test
    @DisplayName("TC-ACC-01 (T-83): Tạo phiếu nghiệm thu theo từng đợt thành công")
    void createAcceptanceReport_Success() {
        when(projectRepository.existsById(projectId)).thenReturn(true);
        when(acceptanceReportRepository.existsByProjectIdAndReportNumber(projectId, "NT-01")).thenReturn(false);
        when(acceptanceReportRepository.save(any(AcceptanceReport.class))).thenAnswer(invocation -> {
            AcceptanceReport report = invocation.getArgument(0);
            report.setId(UUID.randomUUID());
            return report;
        });

        CreateAcceptanceReportRequest request = new CreateAcceptanceReportRequest(
                "NT-01",
                "Nghiệm thu khối lượng đợt 1",
                1,
                LocalDate.now(),
                "DRAFT",
                "Nghiệm thu phần móng",
                List.of()
        );

        AcceptanceReportResponse response = acceptanceReportService.createAcceptanceReport(projectId, creatorId, request);

        assertThat(response.getId()).isNotNull();
        assertThat(response.getReportNumber()).isEqualTo("NT-01");
        assertThat(response.getName()).isEqualTo("Nghiệm thu khối lượng đợt 1");
        assertThat(response.getPeriodNumber()).isEqualTo(1);
        assertThat(response.getTotalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("TC-ACC-02 (T-83): Từ chối tạo phiếu khi mã biên bản đã tồn tại trong dự án")
    void createAcceptanceReport_DuplicateReportNumber_ShouldThrowException() {
        when(projectRepository.existsById(projectId)).thenReturn(true);
        when(acceptanceReportRepository.existsByProjectIdAndReportNumber(projectId, "NT-01")).thenReturn(true);

        CreateAcceptanceReportRequest request = new CreateAcceptanceReportRequest(
                "NT-01",
                "Nghiệm thu trùng mã",
                1,
                LocalDate.now(),
                "DRAFT",
                "",
                List.of()
        );

        assertThatThrownBy(() -> acceptanceReportService.createAcceptanceReport(projectId, creatorId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Mã số biên bản nghiệm thu đã tồn tại");
    }

    @Test
    @DisplayName("TC-ACC-03 (T-84, T-85): Chọn hạng mục, nhập khối lượng và tự động tính thành tiền và tổng giá trị")
    void createAcceptanceReport_WithItems_ShouldAutoCalculateAmounts() {
        when(projectRepository.existsById(projectId)).thenReturn(true);
        when(acceptanceReportRepository.existsByProjectIdAndReportNumber(projectId, "NT-02")).thenReturn(false);
        when(acceptanceReportRepository.save(any(AcceptanceReport.class))).thenAnswer(invocation -> {
            AcceptanceReport report = invocation.getArgument(0);
            report.setId(UUID.randomUUID());
            return report;
        });

        // Hạng mục 1: 50 m3 bê tông * 2,000,000 = 100,000,000
        AcceptanceItemRequest item1 = new AcceptanceItemRequest(
                UUID.randomUUID(),
                null,
                "Đổ bê tông dầm sàn B25",
                "m3",
                new BigDecimal("60.00"),
                new BigDecimal("50.00"),
                new BigDecimal("2000000.00"),
                BigDecimal.ZERO,
                "Đạt chuẩn thiết kế"
        );

        // Hạng mục 2: 10 tấn cốt thép * 18,000,000 = 180,000,000
        AcceptanceItemRequest item2 = new AcceptanceItemRequest(
                UUID.randomUUID(),
                null,
                "Gia công lắp dựng cốt thép CB400",
                "tấn",
                new BigDecimal("12.00"),
                new BigDecimal("10.00"),
                new BigDecimal("18000000.00"),
                BigDecimal.ZERO,
                "Nghiệm thu cốt thép dầm sàn"
        );

        CreateAcceptanceReportRequest request = new CreateAcceptanceReportRequest(
                "NT-02",
                "Nghiệm thu dầm sàn tầng 2",
                2,
                LocalDate.now(),
                "SUBMITTED",
                "Đầy đủ biên bản thí nghiệm",
                List.of(item1, item2)
        );

        AcceptanceReportResponse response = acceptanceReportService.createAcceptanceReport(projectId, creatorId, request);

        assertThat(response.getItemCount()).isEqualTo(2);
        // T-85: Kiểm tra từng dòng hạng mục
        assertThat(response.getItems().get(0).getAmount()).isEqualByComparingTo(new BigDecimal("100000000.00"));
        assertThat(response.getItems().get(1).getAmount()).isEqualByComparingTo(new BigDecimal("180000000.00"));

        // T-85: Kiểm tra tổng giá trị của toàn phiếu = 100,000,000 + 180,000,000 = 280,000,000
        assertThat(response.getTotalAmount()).isEqualByComparingTo(new BigDecimal("280000000.00"));
    }

    @Test
    @DisplayName("TC-ACC-04 (T-84): Từ chối khi khối lượng nghiệm thu âm")
    void createAcceptanceReport_NegativeQuantity_ShouldThrowException() {
        when(projectRepository.existsById(projectId)).thenReturn(true);
        when(acceptanceReportRepository.existsByProjectIdAndReportNumber(projectId, "NT-03")).thenReturn(false);

        AcceptanceItemRequest invalidItem = new AcceptanceItemRequest(
                UUID.randomUUID(),
                null,
                "Hạng mục lỗi",
                "m3",
                BigDecimal.TEN,
                new BigDecimal("-5.00"), // Âm
                new BigDecimal("100000.00"),
                BigDecimal.ZERO,
                ""
        );

        CreateAcceptanceReportRequest request = new CreateAcceptanceReportRequest(
                "NT-03",
                "Phiếu lỗi",
                3,
                LocalDate.now(),
                "DRAFT",
                "",
                List.of(invalidItem)
        );

        assertThatThrownBy(() -> acceptanceReportService.createAcceptanceReport(projectId, creatorId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Khối lượng nghiệm thu không được âm");
    }

    @Test
    @DisplayName("TC-ACC-05 (T-85): Cập nhật hạng mục và tính lại tổng giá trị nghiệm thu")
    void updateAcceptanceReport_ShouldRecalculateTotalAmount() {
        UUID reportId = UUID.randomUUID();
        AcceptanceReport existingReport = new AcceptanceReport(
                projectId,
                "NT-04",
                "Phiếu nghiệm thu cũ",
                4,
                LocalDate.now(),
                "DRAFT",
                creatorId,
                ""
        );
        existingReport.setId(reportId);

        when(acceptanceReportRepository.findById(reportId)).thenReturn(Optional.of(existingReport));
        when(acceptanceReportRepository.save(any(AcceptanceReport.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AcceptanceItemRequest newItem = new AcceptanceItemRequest(
                UUID.randomUUID(),
                null,
                "Xây tường gạch ống",
                "m2",
                new BigDecimal("100.00"),
                new BigDecimal("80.00"),
                new BigDecimal("250000.00"),
                BigDecimal.ZERO,
                ""
        );

        UpdateAcceptanceReportRequest updateRequest = new UpdateAcceptanceReportRequest(
                "Phiếu nghiệm thu cập nhật",
                LocalDate.now(),
                "SUBMITTED",
                "Ghi chú mới",
                List.of(newItem)
        );

        Optional<AcceptanceReportResponse> updated = acceptanceReportService.updateAcceptanceReport(projectId, reportId, updateRequest);

        assertThat(updated).isPresent();
        assertThat(updated.get().getName()).isEqualTo("Phiếu nghiệm thu cập nhật");
        assertThat(updated.get().getStatus()).isEqualTo("SUBMITTED");
        // 80 * 250,000 = 20,000,000
        assertThat(updated.get().getTotalAmount()).isEqualByComparingTo(new BigDecimal("20000000.00"));
    }

    @Test
    @DisplayName("TC-ACC-06: Thống kê tổng hợp nghiệm thu theo dự án")
    void getProjectAcceptanceSummary_ShouldReturnSummary() {
        when(acceptanceReportRepository.countByProjectId(projectId)).thenReturn(5L);
        when(acceptanceReportRepository.countByProjectIdAndStatus(projectId, "DRAFT")).thenReturn(2L);
        when(acceptanceReportRepository.countByProjectIdAndStatus(projectId, "SUBMITTED")).thenReturn(1L);
        when(acceptanceReportRepository.countByProjectIdAndStatus(projectId, "APPROVED")).thenReturn(2L);
        when(acceptanceReportRepository.sumTotalAmountByProjectId(projectId)).thenReturn(new BigDecimal("500000000.00"));
        when(acceptanceReportRepository.sumApprovedAmountByProjectId(projectId)).thenReturn(new BigDecimal("350000000.00"));

        AcceptanceSummaryResponse summary = acceptanceReportService.getProjectAcceptanceSummary(projectId);

        assertThat(summary.getTotalReports()).isEqualTo(5L);
        assertThat(summary.getDraftCount()).isEqualTo(2L);
        assertThat(summary.getApprovedCount()).isEqualTo(2L);
        assertThat(summary.getTotalAmount()).isEqualByComparingTo(new BigDecimal("500000000.00"));
        assertThat(summary.getApprovedAmount()).isEqualByComparingTo(new BigDecimal("350000000.00"));
    }

    @Test
    @DisplayName("T-85 (S-45): Tạo phiếu nghiệm thu với các giá trị tỷ lệ tạm giữ hợp lệ (5, 0, 100, 7.5)")
    void createAcceptanceReport_ValidRetentionRates_Success() {
        when(projectRepository.existsById(projectId)).thenReturn(true);
        when(acceptanceReportRepository.existsByProjectIdAndReportNumber(any(UUID.class), any(String.class))).thenReturn(false);
        when(acceptanceReportRepository.save(any(AcceptanceReport.class))).thenAnswer(invocation -> {
            AcceptanceReport report = invocation.getArgument(0);
            report.setId(UUID.randomUUID());
            return report;
        });

        List<BigDecimal> validRates = List.of(
                new BigDecimal("5"),
                new BigDecimal("0"),
                new BigDecimal("100"),
                new BigDecimal("7.5")
        );

        for (int i = 0; i < validRates.size(); i++) {
            BigDecimal rate = validRates.get(i);
            CreateAcceptanceReportRequest req = new CreateAcceptanceReportRequest(
                    "NT-RATE-" + i,
                    "Phiếu test rate " + rate,
                    i + 1,
                    LocalDate.now(),
                    "DRAFT",
                    "",
                    List.of()
            );
            req.setRetentionRate(rate);

            AcceptanceReportResponse res = acceptanceReportService.createAcceptanceReport(projectId, creatorId, req);
            assertThat(res.getRetentionRate()).isEqualByComparingTo(rate);
        }
    }

    @Test
    @DisplayName("T-85 (S-45): Từ chối tạo phiếu khi tỷ lệ tạm giữ âm (retentionRate = -1)")
    void createAcceptanceReport_NegativeRetentionRate_ThrowsException() {
        when(projectRepository.existsById(projectId)).thenReturn(true);

        CreateAcceptanceReportRequest req = new CreateAcceptanceReportRequest(
                "NT-NEG",
                "Phiếu test rate âm",
                1,
                LocalDate.now(),
                "DRAFT",
                "",
                List.of()
        );
        req.setRetentionRate(new BigDecimal("-1"));

        assertThatThrownBy(() -> acceptanceReportService.createAcceptanceReport(projectId, creatorId, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Tỷ lệ tạm giữ phải từ 0% đến 100%");
    }

    @Test
    @DisplayName("T-85 (S-45): Từ chối tạo phiếu khi tỷ lệ tạm giữ vượt quá 100% (retentionRate = 101)")
    void createAcceptanceReport_Over100RetentionRate_ThrowsException() {
        when(projectRepository.existsById(projectId)).thenReturn(true);

        CreateAcceptanceReportRequest req = new CreateAcceptanceReportRequest(
                "NT-OVER",
                "Phiếu test rate vượt 100",
                1,
                LocalDate.now(),
                "DRAFT",
                "",
                List.of()
        );
        req.setRetentionRate(new BigDecimal("101"));

        assertThatThrownBy(() -> acceptanceReportService.createAcceptanceReport(projectId, creatorId, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Tỷ lệ tạm giữ phải từ 0% đến 100%");
    }

    @Test
    @DisplayName("T-85 (S-45): Cập nhật tỷ lệ tạm giữ thành công")
    void updateAcceptanceReport_RetentionRate_Success() {
        UUID reportId = UUID.randomUUID();
        AcceptanceReport existingReport = new AcceptanceReport(
                projectId,
                "NT-05",
                "Phiếu nghiệm thu cũ",
                5,
                LocalDate.now(),
                "DRAFT",
                creatorId,
                ""
        );
        existingReport.setId(reportId);
        existingReport.setRetentionRate(new BigDecimal("5.00"));

        when(acceptanceReportRepository.findById(reportId)).thenReturn(Optional.of(existingReport));
        when(acceptanceReportRepository.save(any(AcceptanceReport.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateAcceptanceReportRequest updateRequest = new UpdateAcceptanceReportRequest(
                "Phiếu cập nhật tỷ lệ",
                LocalDate.now(),
                "DRAFT",
                "",
                List.of()
        );
        updateRequest.setRetentionRate(new BigDecimal("7.50"));

        Optional<AcceptanceReportResponse> updated = acceptanceReportService.updateAcceptanceReport(projectId, reportId, updateRequest);

        assertThat(updated).isPresent();
        assertThat(updated.get().getRetentionRate()).isEqualByComparingTo(new BigDecimal("7.50"));
    }
}
