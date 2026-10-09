package com.ntdhtcct.domain.sitediary;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SiteDiaryRepository extends JpaRepository<SiteDiary, UUID> {

    /**
     * Tìm nhật ký theo clientSyncId để kiểm tra trùng lặp (Idempotency).
     */
    Optional<SiteDiary> findByClientSyncId(String clientSyncId);

    /**
     * Kiểm tra xem clientSyncId đã được đồng bộ lên máy chủ chưa.
     */
    boolean existsByClientSyncId(String clientSyncId);

    /**
     * Lấy danh sách nhật ký công trường theo dự án, sắp xếp theo ngày giảm dần.
     */
    List<SiteDiary> findByProjectIdOrderByDiaryDateDesc(UUID projectId);

    /**
     * Lấy nhật ký theo dự án và ngày cụ thể.
     */
    List<SiteDiary> findByProjectIdAndDiaryDate(UUID projectId, LocalDate diaryDate);
}
