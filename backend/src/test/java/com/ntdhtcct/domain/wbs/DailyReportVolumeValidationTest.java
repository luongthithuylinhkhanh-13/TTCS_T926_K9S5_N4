package com.ntdhtcct.domain.wbs;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("S-37 / T-86..T-88: kiểm tra khối lượng nghiệm thu và khối lượng còn lại")
class DailyReportVolumeValidationTest {

    private final DailyReportRepository dailyReportRepository = mock(DailyReportRepository.class);
    private final WbsItemRepository wbsItemRepository = mock(WbsItemRepository.class);
    private final DailyReportService service = new DailyReportService(dailyReportRepository, wbsItemRepository);

    @Test
    @DisplayName("T-87: từ chối khối lượng nghiệm thu âm")
    void rejectsNegativeReportedVolume() {
        UUID taskId = UUID.randomUUID();
        UUID reporterId = UUID.randomUUID();
        WbsItem task = new WbsItem();
        task.setId(taskId);
        task.setTotalVolume(100.0);

        when(wbsItemRepository.findById(taskId)).thenReturn(Optional.of(task));

        DailyReportRequest request = new DailyReportRequest();
        request.setWbsItemId(taskId);
        request.setReportDate(LocalDate.now());
        request.setReportedVolume(-10.0);

        assertThatThrownBy(() -> service.createReport(reporterId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Khối lượng nghiệm thu không được âm");
    }

    @Test
    @DisplayName("T-86 + T-88: tính khối lượng đã nghiệm thu và khối lượng còn lại")
    void calculatesApprovedAndRemainingVolume() {
        UUID taskId = UUID.randomUUID();
        UUID reporterId = UUID.randomUUID();
        WbsItem task = new WbsItem();
        task.setId(taskId);
        task.setTotalVolume(100.0);

        when(wbsItemRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(dailyReportRepository.getCumulativeVolumeByWbsItemId(taskId)).thenReturn(30.0, 50.0);
        when(dailyReportRepository.save(any(DailyReport.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(wbsItemRepository.save(any(WbsItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DailyReportRequest request = new DailyReportRequest();
        request.setWbsItemId(taskId);
        request.setReportDate(LocalDate.now());
        request.setReportedVolume(20.0);

        DailyReportResponse response = service.createReport(reporterId, request);

        assertThat(response).isNotNull();
        assertThat(service.getApprovedVolume(taskId)).isEqualTo(50.0);
        assertThat(service.getRemainingVolume(taskId)).isEqualTo(50.0);
    }
}