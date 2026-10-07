package com.ntdhtcct.domain.wbs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

@Component
public class CpmEngine {

    private static final Logger log = LoggerFactory.getLogger(CpmEngine.class);

    private Set<LocalDate> holidays = new HashSet<>();

    public CpmEngine() {
    }

    CpmEngine(Set<LocalDate> holidays) {
        this.holidays = new HashSet<>(holidays);
    }

    @Value("${schedule.holidays:}")
    public void configureHolidays(String holidayValues) {
        holidays.clear();
        if (holidayValues == null || holidayValues.isBlank()) {
            return;
        }

        Arrays.stream(holidayValues.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(LocalDate::parse)
                .forEach(holidays::add);
    }

    public Result calculate(List<WbsItem> items) {
        Set<UUID> parentIds = new HashSet<>();
        items.stream()
                .map(WbsItem::getParentId)
                .filter(Objects::nonNull)
                .forEach(parentIds::add);

        // Phase 1: Boundary Conversion (LocalDate -> Integer Working-Day Offsets)
        List<WbsItem> leafItems = items.stream()
                .filter(item -> "task".equalsIgnoreCase(item.getType()) && !parentIds.contains(item.getId()))
                .toList();

        // Baseline T0 = min(all actualStartDate, all planned startDate) across all project items
        LocalDate baselineDate = items.stream()
                .flatMap(item -> Stream.of(item.getActualStartDate(), item.getStartDate()))
                .filter(Objects::nonNull)
                .min(LocalDate::compareTo)
                .orElse(null);

        CalendarConverter calendarConverter = new CalendarConverter(baselineDate, holidays);

        Map<UUID, ActivityNode> activities = new HashMap<>();
        Map<UUID, WbsItem> itemLookup = new HashMap<>();
        int unscheduledTaskCount = 0;

        for (WbsItem item : items) {
            clearCpmValues(item);
            if (!"task".equalsIgnoreCase(item.getType()) || parentIds.contains(item.getId())) {
                continue;
            }

            Integer duration = resolveDuration(item, calendarConverter);
            if (duration == null || duration <= 0) {
                unscheduledTaskCount++;
                continue;
            }

            Integer plannedStartOffset = calendarConverter.toOffset(item.getStartDate());
            Integer actualStartOffset = calendarConverter.toOffset(item.getActualStartDate());
            Integer actualEndOffset = calendarConverter.toOffset(item.getActualEndDate());

            ActivityNode node = new ActivityNode(
                    item.getId(),
                    item.getWbsCode(),
                    item.getPredecessorIds() == null ? Set.of() : new HashSet<>(item.getPredecessorIds()),
                    duration,
                    plannedStartOffset,
                    actualStartOffset,
                    actualEndOffset
            );

            activities.put(item.getId(), node);
            itemLookup.put(item.getId(), item);
        }

        if (activities.isEmpty()) {
            return new Result(0, "0", unscheduledTaskCount == 0, unscheduledTaskCount);
        }

        // Phase 2: Pure Integer CPM Core (Zero LocalDate arithmetic in CPM loop)
        Map<UUID, List<UUID>> successors = new HashMap<>();
        Map<UUID, Integer> indegree = new HashMap<>();
        boolean complete = unscheduledTaskCount == 0;

        activities.keySet().forEach(id -> {
            successors.put(id, new ArrayList<>());
            indegree.put(id, 0);
        });

        for (ActivityNode activity : activities.values()) {
            for (UUID predecessorId : activity.predecessorIds()) {
                if (!activities.containsKey(predecessorId)) {
                    complete = false;
                    continue;
                }
                successors.get(predecessorId).add(activity.id());
                indegree.compute(activity.id(), (id, count) -> count + 1);
            }
        }

        ArrayDeque<UUID> ready = new ArrayDeque<>();
        indegree.forEach((id, count) -> {
            if (count == 0) ready.add(id);
        });

        List<UUID> order = new ArrayList<>(activities.size());
        Map<UUID, Integer> esMap = new HashMap<>();
        Map<UUID, Integer> efMap = new HashMap<>();

        while (!ready.isEmpty()) {
            UUID id = ready.removeFirst();
            order.add(id);
            ActivityNode activity = activities.get(id);

            int predecessorEf = 0;
            for (UUID predId : activity.predecessorIds()) {
                if (activities.containsKey(predId)) {
                    Integer ef = efMap.get(predId);
                    if (ef != null && ef > predecessorEf) {
                        predecessorEf = ef;
                    }
                }
            }

            // Dependency Guard: ES = max(predecessorEF, actualStartOffset, plannedStartOffset)
            int earlyStart = predecessorEf;
            if (activity.actualStartOffset() != null) {
                if (activity.actualStartOffset() < predecessorEf) {
                    log.warn("Out-of-sequence execution detected for task {} (ID {}): actual start offset {} is earlier than predecessor EF {}",
                            activity.wbsCode(), activity.id(), activity.actualStartOffset(), predecessorEf);
                }
                earlyStart = Math.max(earlyStart, activity.actualStartOffset());
            }
            if (activity.plannedStartOffset() != null) {
                earlyStart = Math.max(earlyStart, activity.plannedStartOffset());
            }

            int earlyFinish = earlyStart + activity.duration();
            esMap.put(id, earlyStart);
            efMap.put(id, earlyFinish);

            for (UUID successorId : successors.get(id)) {
                int remaining = indegree.compute(successorId, (key, count) -> count - 1);
                if (remaining == 0) {
                    ready.addLast(successorId);
                }
            }
        }

        if (order.size() != activities.size()) {
            throw new IllegalStateException("Quan hệ tiền nhiệm tạo thành vòng lặp");
        }

        int projectDuration = 0;
        for (int ef : efMap.values()) {
            if (ef > projectDuration) {
                projectDuration = ef;
            }
        }

        Map<UUID, BigInteger> pathCounts = countLongestPaths(order, activities, esMap, efMap);
        BigInteger criticalPathCount = BigInteger.ZERO;
        for (ActivityNode activity : activities.values()) {
            if (successors.get(activity.id()).isEmpty() && efMap.get(activity.id()) == projectDuration) {
                criticalPathCount = criticalPathCount.add(pathCounts.get(activity.id()));
            }
        }

        Map<UUID, Integer> lsMap = new HashMap<>();
        Map<UUID, Integer> lfMap = new HashMap<>();
        Map<UUID, Integer> slackMap = new HashMap<>();
        Map<UUID, Boolean> criticalMap = new HashMap<>();

        for (int index = order.size() - 1; index >= 0; index--) {
            UUID id = order.get(index);
            ActivityNode activity = activities.get(id);

            List<UUID> succs = successors.get(id);
            int lateFinish = projectDuration;
            if (!succs.isEmpty()) {
                int minLs = Integer.MAX_VALUE;
                for (UUID succId : succs) {
                    Integer ls = lsMap.get(succId);
                    if (ls != null && ls < minLs) {
                        minLs = ls;
                    }
                }
                if (minLs != Integer.MAX_VALUE) {
                    lateFinish = minLs;
                }
            }

            int lateStart = lateFinish - activity.duration();
            int earlyStart = esMap.get(id);
            int slack = lateStart - earlyStart;
            boolean critical = (slack == 0);

            lfMap.put(id, lateFinish);
            lsMap.put(id, lateStart);
            slackMap.put(id, slack);
            criticalMap.put(id, critical);
        }

        // Phase 3: Boundary Mapping (Map Integer Results Back to WbsItem Entities)
        for (UUID id : activities.keySet()) {
            WbsItem item = itemLookup.get(id);
            ActivityNode node = activities.get(id);
            item.setDuration(node.duration());
            item.setEs(esMap.get(id));
            item.setEf(efMap.get(id));
            item.setLs(lsMap.get(id));
            item.setLf(lfMap.get(id));
            item.setSlack(slackMap.get(id));
            item.setCritical(criticalMap.get(id));
        }

        return new Result(projectDuration, criticalPathCount.toString(), complete, unscheduledTaskCount);
    }

    private Integer resolveDuration(WbsItem item, CalendarConverter calendarConverter) {
        // Priority 1: Completed task (actualStartDate + actualEndDate) -> actual working days
        if (item.getActualStartDate() != null && item.getActualEndDate() != null) {
            Integer actualWorkingDays = calendarConverter.countWorkingDaysInclusive(
                    item.getActualStartDate(), item.getActualEndDate());
            if (actualWorkingDays != null && actualWorkingDays > 0) {
                return actualWorkingDays;
            }
        }

        // Priority 2: Planned dates (startDate + endDate) -> planned working days
        if (item.getStartDate() != null && item.getEndDate() != null) {
            Integer plannedWorkingDays = calendarConverter.countWorkingDaysInclusive(
                    item.getStartDate(), item.getEndDate());
            if (plannedWorkingDays != null && plannedWorkingDays > 0) {
                return plannedWorkingDays;
            }
        }

        // Priority 3: Fallback to item.getDuration()
        return item.getDuration();
    }

    private Map<UUID, BigInteger> countLongestPaths(
            List<UUID> order,
            Map<UUID, ActivityNode> activities,
            Map<UUID, Integer> esMap,
            Map<UUID, Integer> efMap
    ) {
        Map<UUID, BigInteger> counts = new HashMap<>();
        for (UUID id : order) {
            ActivityNode activity = activities.get(id);
            int currentEs = esMap.get(id);
            BigInteger count = BigInteger.ZERO;
            for (UUID predId : activity.predecessorIds()) {
                if (activities.containsKey(predId) && efMap.get(predId) == currentEs) {
                    BigInteger predCount = counts.get(predId);
                    if (predCount != null) {
                        count = count.add(predCount);
                    }
                }
            }
            counts.put(id, count.signum() == 0 ? BigInteger.ONE : count);
        }
        return counts;
    }

    private void clearCpmValues(WbsItem item) {
        item.setEs(null);
        item.setEf(null);
        item.setLs(null);
        item.setLf(null);
        item.setSlack(null);
        item.setCritical(false);
    }

    /**
     * Immutable calculation node representing an activity in the CPM graph.
     * Keeps CPM core decoupled from mutable JPA entities.
     */
    public record ActivityNode(
            UUID id,
            String wbsCode,
            Set<UUID> predecessorIds,
            int duration,
            Integer plannedStartOffset,
            Integer actualStartOffset,
            Integer actualEndOffset
    ) {
    }

    public record Result(
            int durationDays,
            String criticalPathCount,
            boolean complete,
            int unscheduledTaskCount
    ) {
    }
}