package com.ntdhtcct.domain.wbs;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Handles two-way conversion between LocalDate and integer working-day offsets
 * based on a project baseline date, excluding Sundays and configured holidays.
 */
public class CalendarConverter {

    private final LocalDate baseline;
    private final Set<LocalDate> holidays;

    public CalendarConverter(LocalDate baseline, Set<LocalDate> holidays) {
        this.holidays = holidays == null ? Collections.emptySet() : new HashSet<>(holidays);
        if (baseline != null) {
            LocalDate adjusted = baseline;
            while (isNonWorkingDay(adjusted)) {
                adjusted = adjusted.plusDays(1);
            }
            this.baseline = adjusted;
        } else {
            this.baseline = null;
        }
    }

    public LocalDate getBaseline() {
        return baseline;
    }

    public Set<LocalDate> getHolidays() {
        return Collections.unmodifiableSet(holidays);
    }

    public boolean isWorkingDay(LocalDate date) {
        return date != null && !isNonWorkingDay(date);
    }

    public boolean isNonWorkingDay(LocalDate date) {
        if (date == null) {
            return false;
        }
        return date.getDayOfWeek() == DayOfWeek.SUNDAY || holidays.contains(date);
    }

    /**
     * Converts a LocalDate to an integer working-day offset relative to the baseline.
     * Returns null if date or baseline is null.
     */
    public Integer toOffset(LocalDate date) {
        if (date == null || baseline == null) {
            return null;
        }
        if (date.isEqual(baseline)) {
            return 0;
        }
        if (date.isAfter(baseline)) {
            int count = 0;
            LocalDate current = baseline.plusDays(1);
            while (!current.isAfter(date)) {
                if (isWorkingDay(current)) {
                    count++;
                }
                current = current.plusDays(1);
            }
            return count;
        } else {
            int count = 0;
            LocalDate current = date;
            while (current.isBefore(baseline)) {
                if (isWorkingDay(current)) {
                    count++;
                }
                current = current.plusDays(1);
            }
            return -count;
        }
    }

    /**
     * Converts an integer working-day offset back to a LocalDate relative to the baseline.
     * Returns null if baseline is null.
     */
    public LocalDate toDate(int offset) {
        if (baseline == null) {
            return null;
        }
        if (offset == 0) {
            return baseline;
        }
        LocalDate current = baseline;
        if (offset > 0) {
            int count = 0;
            while (count < offset) {
                current = current.plusDays(1);
                if (isWorkingDay(current)) {
                    count++;
                }
            }
            return current;
        } else {
            int count = 0;
            while (count < -offset) {
                current = current.minusDays(1);
                if (isWorkingDay(current)) {
                    count++;
                }
            }
            return current;
        }
    }

    /**
     * Counts working days inclusively between start and end date.
     * Returns null if either date is null, end is before start, or count is 0.
     */
    public Integer countWorkingDaysInclusive(LocalDate start, LocalDate end) {
        if (start == null || end == null || end.isBefore(start)) {
            return null;
        }
        int workdays = 0;
        LocalDate current = start;
        while (!current.isAfter(end)) {
            if (isWorkingDay(current)) {
                workdays++;
            }
            current = current.plusDays(1);
        }
        return workdays > 0 ? workdays : null;
    }
}
