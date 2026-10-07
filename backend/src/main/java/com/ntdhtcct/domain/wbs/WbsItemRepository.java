package com.ntdhtcct.domain.wbs;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface WbsItemRepository extends JpaRepository<WbsItem, UUID> {

    List<WbsItem> findByProjectIdOrderByWbsCodeAsc(Long projectId);

    boolean existsByProjectIdAndWbsCode(
            Long projectId,
            String wbsCode
    );

    List<WbsItem> findByParentId(UUID parentId);

    /*
     * T-49:
     * Lấy các công việc được giao cho cùng một đội thi công
     */
    List<WbsItem> findByProjectIdAndTeamIdOrderByStartDateAsc(
            Long projectId,
            UUID teamId
    );

    /*
     * T-49:
     * Lấy các công việc của một đội trong khoảng thời gian
     */
    List<WbsItem> findByProjectIdAndTeamIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            Long projectId,
            UUID teamId,
            java.time.LocalDate endDate,
            java.time.LocalDate startDate
    );
}