package com.ntdhtcct.domain.acceptance;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AcceptanceRecordAuditLogRepository extends JpaRepository<AcceptanceRecordAuditLog, UUID> {

    List<AcceptanceRecordAuditLog> findByAcceptanceRecordIdOrderByPerformedAtAscIdAsc(UUID acceptanceRecordId);
}
