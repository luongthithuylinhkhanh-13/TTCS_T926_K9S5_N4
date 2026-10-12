package com.ntdhtcct.domain.acceptance;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AcceptanceItemRepository extends JpaRepository<AcceptanceItem, UUID> {

    List<AcceptanceItem> findByAcceptanceReportId(UUID acceptanceReportId);

    List<AcceptanceItem> findByWbsItemId(UUID wbsItemId);

    List<AcceptanceItem> findByTaskId(UUID taskId);
}
