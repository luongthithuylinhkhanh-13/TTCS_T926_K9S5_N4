package com.ntdhtcct.domain.sitediary;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SiteDiaryLockRepository extends JpaRepository<SiteDiaryLock, UUID> {

    /**
     * Tìm bản ghi khóa sổ theo dự án và ngày cụ thể.
     */
    Optional<SiteDiaryLock> findByProjectIdAndLockDate(UUID projectId, LocalDate lockDate);

    /**
     * Kiểm tra nhanh trạng thái khóa của ngày trong dự án.
     */
    boolean existsByProjectIdAndLockDateAndStatus(UUID projectId, LocalDate lockDate, String status);

    /**
     * Danh sách tất cả các ngày đã thiết lập khóa sổ của dự án, sắp xếp ngày giảm dần.
     */
    List<SiteDiaryLock> findByProjectIdOrderByLockDateDesc(UUID projectId);

    /**
     * Tìm các bản ghi khóa sổ cho danh sách ngày trong dự án (phục vụ kiểm tra hàng loạt).
     */
    List<SiteDiaryLock> findByProjectIdAndLockDateIn(UUID projectId, Collection<LocalDate> lockDates);
}
