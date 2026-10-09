package com.ntdhtcct.domain.wbs;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

final class TeamAssignmentConflictDetector {

    private TeamAssignmentConflictDetector() {
    }

    static List<Overlap> findOverlaps(
            WbsItem task,
            UUID teamMemberId,
            List<WbsItem> projectItems
    ) {
        if (teamMemberId == null
                || task.getStartDate() == null
                || task.getEndDate() == null) {
            return List.of();
        }

        return projectItems.stream()
                .filter(candidate -> !sameTask(task, candidate))
                .filter(candidate -> "task".equalsIgnoreCase(candidate.getType()))
                .filter(candidate -> teamMemberId.equals(candidate.getAssignedTeamMemberId()))
                .filter(candidate -> !isCompleted(candidate))
                .filter(candidate -> overlaps(task, candidate))
                .map(candidate -> new Overlap(
                        candidate.getId(),
                        candidate.getWbsCode(),
                        candidate.getName(),
                        candidate.getStartDate(),
                        candidate.getEndDate()
                ))
                .toList();
    }

    private static boolean sameTask(WbsItem left, WbsItem right) {
        return left == right
                || (left.getId() != null && left.getId().equals(right.getId()));
    }

    private static boolean isCompleted(WbsItem item) {
        return item.getProgress() >= 100
                || "completed".equalsIgnoreCase(item.getStatus());
    }

    private static boolean overlaps(WbsItem task, WbsItem other) {
        LocalDate taskStart = task.getStartDate();
        LocalDate taskEnd = task.getEndDate();
        LocalDate otherStart = other.getStartDate();
        LocalDate otherEnd = other.getEndDate();

        return otherStart != null
                && otherEnd != null
                && !taskStart.isAfter(otherEnd)
                && !taskEnd.isBefore(otherStart);
    }

    record Overlap(
            UUID taskId,
            String wbsCode,
            String taskName,
            LocalDate startDate,
            LocalDate endDate
    ) {
    }
}
