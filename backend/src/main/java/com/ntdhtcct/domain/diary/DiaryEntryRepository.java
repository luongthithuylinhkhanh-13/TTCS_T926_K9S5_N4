package com.ntdhtcct.domain.diary;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DiaryEntryRepository extends JpaRepository<DiaryEntry, UUID> {
    List<DiaryEntry> findByProjectIdOrderByEntryDateDescCreatedAtDesc(UUID projectId);
}
