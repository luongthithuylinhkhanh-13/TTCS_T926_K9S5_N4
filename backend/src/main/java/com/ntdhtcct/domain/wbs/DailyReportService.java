package com.ntdhtcct.domain.wbs;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DailyReportService {

    private final DailyReportRepository dailyReportRepository;
    private final WbsItemRepository wbsItemRepository;

    public DailyReportService(DailyReportRepository dailyReportRepository, WbsItemRepository wbsItemRepository) {
        this.dailyReportRepository = dailyReportRepository;
        this.wbsItemRepository = wbsItemRepository;
    }

    @Transactional
    public DailyReportResponse createReport(UUID reporterId, DailyReportRequest request) {
        WbsItem wbsItem = wbsItemRepository.findById(request.getWbsItemId())
                .orElseThrow(() -> new IllegalArgumentException("WbsItem not found"));

        validateReportedVolume(wbsItem, request.getReportedVolume());

        Double cumulativeVolume = getApprovedVolume(wbsItem.getId());
        Double nextCumulativeVolume = cumulativeVolume + request.getReportedVolume();

        if (nextCumulativeVolume > wbsItem.getTotalVolume()) {
            throw new IllegalArgumentException("Cảnh báo vượt khối lượng! Khối lượng lũy kế (" +
                    formatVolume(nextCumulativeVolume) + ") vượt quá tổng khối lượng (" +
                    formatVolume(wbsItem.getTotalVolume()) + ")");
        }

        DailyReport report = new DailyReport(
                wbsItem.getId(),
                reporterId,
                request.getReportDate(),
                request.getReportedVolume(),
                request.getNotes()
        );

        report = dailyReportRepository.save(report);

        double newProgress = ((nextCumulativeVolume) / wbsItem.getTotalVolume()) * 100;
        wbsItem.setProgress((int) Math.round(newProgress));
        wbsItemRepository.save(wbsItem);

        return new DailyReportResponse(report);
    }

    @Transactional(readOnly = true)
    public List<DailyReportResponse> getReportsForTask(UUID wbsItemId) {
        Double approvedVolume = getApprovedVolume(wbsItemId);
        Double remainingVolume = getRemainingVolume(wbsItemId);

        return dailyReportRepository.findByWbsItemIdOrderByReportDateDesc(wbsItemId)
                .stream()
                .map(report -> new DailyReportResponse(report, approvedVolume, remainingVolume))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Double getApprovedVolume(UUID wbsItemId) {
        Double cumulative = dailyReportRepository.getCumulativeVolumeByWbsItemId(wbsItemId);
        return cumulative == null ? 0.0 : cumulative;
    }

    @Transactional(readOnly = true)
    public Double getRemainingVolume(UUID wbsItemId) {
        WbsItem wbsItem = wbsItemRepository.findById(wbsItemId)
                .orElseThrow(() -> new IllegalArgumentException("WbsItem not found"));

        if (wbsItem.getTotalVolume() == null) {
            return null;
        }

        double approvedVolume = getApprovedVolume(wbsItemId);
        return Math.max(0.0, wbsItem.getTotalVolume() - approvedVolume);
    }

    @Transactional(readOnly = true)
    public List<WbsItem> getAssignedTasksForCaptain(UUID captainId, LocalDate date) {
        return wbsItemRepository.findAssignedTasksByDate(captainId.toString(), date);
    }

    private void validateReportedVolume(WbsItem wbsItem, Double reportedVolume) {
        if (wbsItem.getTotalVolume() == null || wbsItem.getTotalVolume() == 0) {
            throw new IllegalArgumentException("Cannot report volume: Total volume is not set for this task");
        }
        if (reportedVolume == null) {
            throw new IllegalArgumentException("Khối lượng nghiệm thu không được để trống");
        }
        if (reportedVolume < 0) {
            throw new IllegalArgumentException("Khối lượng nghiệm thu không được âm");
        }
    }

    private String formatVolume(Double value) {
        if (value == null) {
            return "0";
        }
        if (Math.abs(value - Math.rint(value)) < 1e-9) {
            return String.format("%.0f", value);
        }
        return String.format("%.2f", value).replaceAll("\\.?0+$", "");
    }
}
