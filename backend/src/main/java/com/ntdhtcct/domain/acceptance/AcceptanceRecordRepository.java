package com.ntdhtcct.domain.acceptance;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AcceptanceRecordRepository extends JpaRepository<AcceptanceRecord, UUID> {

    List<AcceptanceRecord> findByProjectIdOrderByUpdatedAtDesc(UUID projectId);

    Optional<AcceptanceRecord> findByIdAndProjectId(UUID id, UUID projectId);

    Optional<AcceptanceRecord> findByProjectIdAndTitle(UUID projectId, String title);
}
