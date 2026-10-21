package com.ntdhtcct.domain.wbs;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TeamAssignmentConflictDetectorTest {

    private final UUID crewId = UUID.randomUUID();

    @Test
    void findsInclusiveOverlapsForTheSameCrewAndIgnoresCompletedWork() {
        WbsItem task = task("1.1", "Current task", "2026-10-10", "2026-10-15");
        WbsItem overlap = task("1.2", "Overlapping task", "2026-10-15", "2026-10-18");
        overlap.setAssignedTeamMemberId(crewId);
        WbsItem otherCrew = task("1.3", "Other crew task", "2026-10-11", "2026-10-12");
        otherCrew.setAssignedTeamMemberId(UUID.randomUUID());
        WbsItem completed = task("1.4", "Completed task", "2026-10-12", "2026-10-13");
        completed.setAssignedTeamMemberId(crewId);
        completed.setStatus("completed");

        List<TeamAssignmentConflictDetector.Overlap> overlaps =
                TeamAssignmentConflictDetector.findOverlaps(
                        task,
                        crewId,
                        List.of(task, overlap, otherCrew, completed)
                );

        assertThat(overlaps)
                .extracting(TeamAssignmentConflictDetector.Overlap::taskName)
                .containsExactly("Overlapping task");
    }

    @Test
    void doesNotReportOverlapsWhenTheNewTaskHasNoCompletePlannedWindow() {
        WbsItem task = task("1.1", "Unscheduled", null, null);

        assertThat(TeamAssignmentConflictDetector.findOverlaps(
                task,
                crewId,
                List.of(task)
        )).isEmpty();
    }

    private WbsItem task(
            String code,
            String name,
            String startDate,
            String endDate
    ) {
        WbsItem item = new WbsItem();
        item.setWbsCode(code);
        item.setName(name);
        item.setType("task");
        item.setStatus("in_progress");
        if (startDate != null) item.setStartDate(LocalDate.parse(startDate));
        if (endDate != null) item.setEndDate(LocalDate.parse(endDate));
        return item;
    }
}
