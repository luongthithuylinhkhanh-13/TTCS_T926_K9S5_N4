package com.ntdhtcct.domain.wbs;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Component
public class CpmEngine {

    private Set<LocalDate> holidays = new HashSet<>();

    public CpmEngine() {
    }

    CpmEngine(Set<LocalDate> holidays) {
        this.holidays = new HashSet<>(holidays);
    }

    @Value("${schedule.holidays:}")
    public void configureHolidays(String holidayValues) {
        holidays.clear();
        if (holidayValues.isBlank()) {
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
                .filter(id -> id != null)
                .forEach(parentIds::add);

        Map<UUID, WbsItem> activities = new HashMap<>();
        int unscheduledTaskCount = 0;
        for (WbsItem item : items) {
            clearCpmValues(item);
            if (!"task".equalsIgnoreCase(item.getType())
                    || parentIds.contains(item.getId())) {
                continue;
            }

            Integer duration = getDuration(item);
            if (duration == null || duration <= 0) {
                unscheduledTaskCount++;
                continue;
            }
            item.setDuration(duration);
            activities.put(item.getId(), item);
        }

        if (activities.isEmpty()) {
            return new Result(0, "0", unscheduledTaskCount == 0, unscheduledTaskCount);
        }

        Map<UUID, List<UUID>> successors = new HashMap<>();
        Map<UUID, Integer> indegree = new HashMap<>();
        boolean complete = unscheduledTaskCount == 0;
        activities.keySet().forEach(id -> {
            successors.put(id, new ArrayList<>());
            indegree.put(id, 0);
        });

        for (WbsItem activity : activities.values()) {
            for (UUID predecessorId : activity.getPredecessorIds()) {
                if (!activities.containsKey(predecessorId)) {
                    complete = false;
                    continue;
                }
                successors.get(predecessorId).add(activity.getId());
                indegree.compute(activity.getId(), (id, count) -> count + 1);
            }
        }

        ArrayDeque<UUID> ready = new ArrayDeque<>();
        indegree.forEach((id, count) -> {
            if (count == 0) ready.add(id);
        });

        List<UUID> order = new ArrayList<>(activities.size());
        while (!ready.isEmpty()) {
            UUID id = ready.removeFirst();
            order.add(id);
            WbsItem activity = activities.get(id);
            int earlyStart = activity.getPredecessorIds().stream()
                    .filter(activities::containsKey)
                    .map(activities::get)
                    .mapToInt(predecessor -> predecessor.getEf())
                    .max()
                    .orElse(0);
            activity.setEs(earlyStart);
            activity.setEf(earlyStart + activity.getDuration());

            for (UUID successorId : successors.get(id)) {
                int remaining = indegree.compute(successorId, (key, count) -> count - 1);
                if (remaining == 0) ready.addLast(successorId);
            }
        }

        if (order.size() != activities.size()) {
            throw new IllegalStateException("Quan hệ tiền nhiệm tạo thành vòng lặp");
        }

        int projectDuration = activities.values().stream()
                .mapToInt(WbsItem::getEf)
                .max()
                .orElse(0);
        Map<UUID, BigInteger> pathCounts = countLongestPaths(order, activities, successors);
        BigInteger criticalPathCount = activities.values().stream()
                .filter(activity -> successors.get(activity.getId()).isEmpty())
                .filter(activity -> activity.getEf() == projectDuration)
                .map(activity -> pathCounts.get(activity.getId()))
                .reduce(BigInteger.ZERO, BigInteger::add);

        for (int index = order.size() - 1; index >= 0; index--) {
            UUID id = order.get(index);
            WbsItem activity = activities.get(id);
            int lateFinish = successors.get(id).isEmpty()
                    ? projectDuration
                    : successors.get(id).stream()
                            .map(activities::get)
                            .mapToInt(WbsItem::getLs)
                            .min()
                            .orElse(projectDuration);
            int lateStart = lateFinish - activity.getDuration();
            int slack = lateStart - activity.getEs();
            activity.setLf(lateFinish);
            activity.setLs(lateStart);
            activity.setSlack(slack);
            activity.setCritical(slack == 0);
        }

        return new Result(projectDuration, criticalPathCount.toString(), complete, unscheduledTaskCount);
    }

    private Map<UUID, BigInteger> countLongestPaths(
            List<UUID> order,
            Map<UUID, WbsItem> activities,
            Map<UUID, List<UUID>> successors
    ) {
        Map<UUID, BigInteger> counts = new HashMap<>();
        for (UUID id : order) {
            WbsItem activity = activities.get(id);
            BigInteger count = activity.getPredecessorIds().stream()
                    .filter(activities::containsKey)
                    .map(activities::get)
                    .filter(predecessor -> predecessor.getEf().equals(activity.getEs()))
                    .map(predecessor -> counts.get(predecessor.getId()))
                    .reduce(BigInteger.ZERO, BigInteger::add);
            counts.put(id, count.signum() == 0 ? BigInteger.ONE : count);
        }
        return counts;
    }

    private Integer getDuration(WbsItem item) {
        if (item.getStartDate() != null && item.getEndDate() != null) {
            long workdays = 0;
            LocalDate date = item.getStartDate();
            LocalDate endDate = item.getEndDate();

            while (!date.isAfter(endDate)) {
                if (!isNonWorkingDay(date)) {
                    workdays++;
                }
                date = date.plusDays(1);
            }

            return workdays > 0 && workdays <= Integer.MAX_VALUE ? (int) workdays : null;
        }
        return item.getDuration();
    }

    private boolean isNonWorkingDay(LocalDate date) {
        return date.getDayOfWeek() == DayOfWeek.SUNDAY || holidays.contains(date);
    }

    private void clearCpmValues(WbsItem item) {
        item.setEs(null);
        item.setEf(null);
        item.setLs(null);
        item.setLf(null);
        item.setSlack(null);
        item.setCritical(false);
    }

    public record Result(
            int durationDays,
            String criticalPathCount,
            boolean complete,
            int unscheduledTaskCount
    ) {
    }
}