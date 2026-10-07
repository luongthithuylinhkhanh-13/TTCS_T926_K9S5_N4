package com.ntdhtcct.domain.wbs;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConstructionTeamScheduleService {

    private final WbsItemRepository wbsItemRepository;
    private final ConstructionTeamRepository constructionTeamRepository;

    public ConstructionTeamScheduleService(
            WbsItemRepository wbsItemRepository,
            ConstructionTeamRepository constructionTeamRepository
    ) {
        this.wbsItemRepository = wbsItemRepository;
        this.constructionTeamRepository = constructionTeamRepository;
    }

    /*
     * T-49:
     * Kiểm tra toàn bộ công việc của một đội thi công
     * có bị chồng thời gian hay không.
     */
    @Transactional(readOnly = true)
    public List<ScheduleConflict> checkTeamSchedule(
            Long projectId,
            UUID teamId
    ) {
        constructionTeamRepository
                .findByProjectIdAndId(projectId, teamId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Đội thi công không tồn tại trong dự án"
                        )
                );

        List<WbsItem> items =
                wbsItemRepository
                        .findByProjectIdAndTeamIdOrderByStartDateAsc(
                                projectId,
                                teamId
                        );

        List<WbsItem> scheduledItems = items.stream()
                .filter(item ->
                        item.getStartDate() != null
                                && item.getEndDate() != null
                )
                .sorted(
                        Comparator.comparing(
                                WbsItem::getStartDate
                        )
                )
                .toList();

        List<ScheduleConflict> conflicts = new ArrayList<>();

        for (int i = 0; i < scheduledItems.size(); i++) {

            WbsItem first = scheduledItems.get(i);

            for (int j = i + 1; j < scheduledItems.size(); j++) {

                WbsItem second = scheduledItems.get(j);

                /*
                 * Vì danh sách đã sắp xếp theo startDate,
                 * nếu công việc tiếp theo bắt đầu sau
                 * endDate của công việc hiện tại thì
                 * những công việc sau nữa cũng không cần
                 * kiểm tra với first.
                 */
                if (second.getStartDate()
                        .isAfter(first.getEndDate())) {
                    break;
                }

                if (isOverlapping(first, second)) {
                    conflicts.add(
                            new ScheduleConflict(
                                    first.getId(),
                                    first.getName(),
                                    first.getStartDate(),
                                    first.getEndDate(),
                                    second.getId(),
                                    second.getName(),
                                    second.getStartDate(),
                                    second.getEndDate(),
                                    "Hai công việc của cùng một đội bị chồng lịch"
                            )
                    );
                }
            }
        }

        return conflicts;
    }

    /*
     * T-49:
     * Kiểm tra một công việc mới có bị chồng
     * với các công việc hiện tại của đội hay không.
     */
    @Transactional(readOnly = true)
    public List<ScheduleConflict> checkNewSchedule(
            Long projectId,
            UUID teamId,
            LocalDate startDate,
            LocalDate endDate,
            UUID excludeWbsId
    ) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException(
                    "Ngày bắt đầu và ngày kết thúc không được để trống"
            );
        }

        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException(
                    "Ngày kết thúc không được trước ngày bắt đầu"
            );
        }

        constructionTeamRepository
                .findByProjectIdAndId(projectId, teamId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Đội thi công không tồn tại trong dự án"
                        )
                );

        List<WbsItem> items =
                wbsItemRepository
                        .findByProjectIdAndTeamIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                                projectId,
                                teamId,
                                endDate,
                                startDate
                        );

        return items.stream()
                .filter(item ->
                        excludeWbsId == null
                                || !excludeWbsId.equals(item.getId())
                )
                .map(item ->
                        new ScheduleConflict(
                                item.getId(),
                                item.getName(),
                                item.getStartDate(),
                                item.getEndDate(),
                                null,
                                null,
                                startDate,
                                endDate,
                                "Công việc mới bị chồng lịch với công việc hiện tại của đội"
                        )
                )
                .toList();
    }

    private boolean isOverlapping(
            WbsItem first,
            WbsItem second
    ) {
        return !first.getEndDate()
                .isBefore(second.getStartDate())
                && !second.getEndDate()
                .isBefore(first.getStartDate());
    }

    public record ScheduleConflict(
            UUID firstWbsId,
            String firstWbsName,
            LocalDate firstStartDate,
            LocalDate firstEndDate,

            UUID secondWbsId,
            String secondWbsName,
            LocalDate secondStartDate,
            LocalDate secondEndDate,

            String message
    ) {
    }
}