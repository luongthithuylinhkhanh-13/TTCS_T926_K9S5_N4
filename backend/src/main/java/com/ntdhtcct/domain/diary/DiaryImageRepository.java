package com.ntdhtcct.domain.diary;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DiaryImageRepository extends JpaRepository<DiaryImage, UUID> {

    List<DiaryImage> findByDiaryEntryIdOrderBySortOrderAscCreatedAtAsc(UUID diaryEntryId);

    Optional<DiaryImage> findByDiaryEntryIdAndClientImageId(UUID diaryEntryId, String clientImageId);

    @Modifying
    @Query("UPDATE DiaryImage i SET i.displayStatus = 'COMPLETED', i.displayPath = :path, " +
           "i.displayFileSize = :size, i.thumbnailPath = :thumbPath, i.width = :w, i.height = :h, " +
           "i.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE i.id = :id AND i.displayAttemptId = :attemptId")
    int updateDisplayCompletedIfAttemptMatches(@Param("id") UUID id,
                                              @Param("attemptId") UUID attemptId,
                                              @Param("path") String path,
                                              @Param("size") Long size,
                                              @Param("thumbPath") String thumbPath,
                                              @Param("w") Integer w,
                                              @Param("h") Integer h);

    @Modifying
    @Query("UPDATE DiaryImage i SET i.originalStatus = 'COMPLETED', i.originalPath = :path, " +
           "i.originalFileSize = :size, i.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE i.id = :id AND i.originalAttemptId = :attemptId")
    int updateOriginalCompletedIfAttemptMatches(@Param("id") UUID id,
                                               @Param("attemptId") UUID attemptId,
                                               @Param("path") String path,
                                               @Param("size") Long size);
}
