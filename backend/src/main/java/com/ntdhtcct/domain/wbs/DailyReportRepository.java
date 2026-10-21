package com.ntdhtcct.domain.wbs;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface DailyReportRepository extends JpaRepository<DailyReport, UUID> {

    List<DailyReport> findByWbsItemIdOrderByReportDateDesc(UUID wbsItemId);

    @Query("SELECT COALESCE(SUM(dr.reportedVolume), 0) FROM DailyReport dr WHERE dr.wbsItemId = :wbsItemId")
    Double getCumulativeVolumeByWbsItemId(@Param("wbsItemId") UUID wbsItemId);

    @Query("SELECT dr FROM DailyReport dr WHERE dr.reporterId = :reporterId AND dr.reportDate = :date")
    List<DailyReport> findByReporterIdAndDate(@Param("reporterId") UUID reporterId, @Param("date") LocalDate date);
}
