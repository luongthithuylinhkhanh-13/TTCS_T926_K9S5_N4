package com.ntdhtcct.domain.cost;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ActualCostRepository extends JpaRepository<ActualCost, UUID> {

    List<ActualCost> findByProjectIdOrderByIncurredDateDescCreatedAtDesc(UUID projectId);

    List<ActualCost> findByProjectIdAndWbsItemIdOrderByIncurredDateDescCreatedAtDesc(UUID projectId, UUID wbsItemId);

    List<ActualCost> findByProjectIdAndTaskIdOrderByIncurredDateDescCreatedAtDesc(UUID projectId, UUID taskId);

    @Query("SELECT COALESCE(SUM(c.amount), 0.0) FROM ActualCost c WHERE c.projectId = :projectId")
    Double sumTotalByProjectId(@Param("projectId") UUID projectId);

    @Query("SELECT COALESCE(SUM(c.amount), 0.0) FROM ActualCost c WHERE c.projectId = :projectId AND c.wbsItem.id = :wbsItemId")
    Double sumTotalByProjectIdAndWbsItemId(@Param("projectId") UUID projectId, @Param("wbsItemId") UUID wbsItemId);

    @Query("SELECT c FROM ActualCost c WHERE c.projectId = :projectId " +
           "AND (:wbsItemId IS NULL OR c.wbsItem.id = :wbsItemId) " +
           "AND (:costType IS NULL OR c.costType = :costType) " +
           "AND (:startDate IS NULL OR c.incurredDate >= :startDate) " +
           "AND (:endDate IS NULL OR c.incurredDate <= :endDate) " +
           "ORDER BY c.incurredDate DESC, c.createdAt DESC")
    List<ActualCost> filterActualCosts(
            @Param("projectId") UUID projectId,
            @Param("wbsItemId") UUID wbsItemId,
            @Param("costType") String costType,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
