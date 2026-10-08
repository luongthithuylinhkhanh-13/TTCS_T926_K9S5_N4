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

        if (wbsItem.getTotalVolume() == null || wbsItem.getTotalVolume() == 0) {
            throw new IllegalArgumentException("Cannot report volume: Total volume is not set for this task");
        }

        Double cumulativeVolume = dailyReportRepository.getCumulativeVolumeByWbsItemId(wbsItem.getId());
        if (cumulativeVolume + request.getReportedVolume() > wbsItem.getTotalVolume()) {
            throw new IllegalArgumentException("Cảnh báo vượt khối lượng! Khối lượng lũy kế (" + 
                    (cumulativeVolume + request.getReportedVolume()) + ") vượt quá tổng khối lượng (" + 
                    wbsItem.getTotalVolume() + ")");
        }

        DailyReport report = new DailyReport(
                wbsItem.getId(),
                reporterId,
                request.getReportDate(),
                request.getReportedVolume(),
                request.getNotes()
        );

        report = dailyReportRepository.save(report);

        // Update progress of wbsItem
        double newProgress = ((cumulativeVolume + request.getReportedVolume()) / wbsItem.getTotalVolume()) * 100;
        wbsItem.setProgress((int) Math.round(newProgress));
        wbsItemRepository.save(wbsItem);

        return new DailyReportResponse(report);
    }

    @Transactional(readOnly = true)
    public List<DailyReportResponse> getReportsForTask(UUID wbsItemId) {
        return dailyReportRepository.findByWbsItemIdOrderByReportDateDesc(wbsItemId)
                .stream()
                .map(DailyReportResponse::new)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<WbsItem> getAssignedTasksForCaptain(UUID captainId, LocalDate date) {
        return wbsItemRepository.findAssignedTasksByDate(captainId.toString(), date);
    }
}
