package com.ntdhtcct.domain.wbs;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CalendarConverterTest {

    @Test
    void adjustsBaselineIfFallingOnSundayOrHoliday() {
        // 2026-01-04 is Sunday
        LocalDate sunday = LocalDate.of(2026, 1, 4);
        CalendarConverter converter = new CalendarConverter(sunday, Set.of());
        // Adjusted to Monday 2026-01-05
        assertThat(converter.getBaseline()).isEqualTo(LocalDate.of(2026, 1, 5));
    }

    @Test
    void adjustsBaselineIfFallingOnHoliday() {
        LocalDate monday = LocalDate.of(2026, 1, 5);
        // Monday 2026-01-05 is a holiday
        CalendarConverter converter = new CalendarConverter(monday, Set.of(monday));
        // Adjusted to Tuesday 2026-01-06
        assertThat(converter.getBaseline()).isEqualTo(LocalDate.of(2026, 1, 6));
    }

    @Test
    void convertsDateToOffsetAndBackAccurately() {
        // Monday 2026-01-05
        LocalDate baseline = LocalDate.of(2026, 1, 5);
        Set<LocalDate> holidays = Set.of(LocalDate.of(2026, 1, 8)); // Thursday holiday
        CalendarConverter converter = new CalendarConverter(baseline, holidays);

        // Baseline itself is offset 0
        assertThat(converter.toOffset(baseline)).isZero();
        assertThat(converter.toDate(0)).isEqualTo(baseline);

        // Tue 2026-01-06: 1 working day after baseline -> offset 1
        assertThat(converter.toOffset(LocalDate.of(2026, 1, 6))).isEqualTo(1);
        assertThat(converter.toDate(1)).isEqualTo(LocalDate.of(2026, 1, 6));

        // Wed 2026-01-07: offset 2
        assertThat(converter.toOffset(LocalDate.of(2026, 1, 7))).isEqualTo(2);
        assertThat(converter.toDate(2)).isEqualTo(LocalDate.of(2026, 1, 7));

        // Thu 2026-01-08 is holiday: offset 2 (does not increment)
        assertThat(converter.toOffset(LocalDate.of(2026, 1, 8))).isEqualTo(2);

        // Fri 2026-01-09: offset 3
        assertThat(converter.toOffset(LocalDate.of(2026, 1, 9))).isEqualTo(3);
        assertThat(converter.toDate(3)).isEqualTo(LocalDate.of(2026, 1, 9));

        // Sat 2026-01-10: offset 4
        assertThat(converter.toOffset(LocalDate.of(2026, 1, 10))).isEqualTo(4);
        assertThat(converter.toDate(4)).isEqualTo(LocalDate.of(2026, 1, 10));

        // Sun 2026-01-11: Sunday (non-working), offset stays 4
        assertThat(converter.toOffset(LocalDate.of(2026, 1, 11))).isEqualTo(4);

        // Mon 2026-01-12: offset 5
        assertThat(converter.toOffset(LocalDate.of(2026, 1, 12))).isEqualTo(5);
        assertThat(converter.toDate(5)).isEqualTo(LocalDate.of(2026, 1, 12));
    }

    @Test
    void handlesNegativeOffsetsForDatesBeforeBaseline() {
        // Wednesday 2026-01-07 as baseline
        LocalDate baseline = LocalDate.of(2026, 1, 7);
        CalendarConverter converter = new CalendarConverter(baseline, Set.of());

        // Tue 2026-01-06 is 1 day before baseline -> offset -1
        assertThat(converter.toOffset(LocalDate.of(2026, 1, 6))).isEqualTo(-1);
        assertThat(converter.toDate(-1)).isEqualTo(LocalDate.of(2026, 1, 6));

        // Mon 2026-01-05 is 2 days before baseline -> offset -2
        assertThat(converter.toOffset(LocalDate.of(2026, 1, 5))).isEqualTo(-2);
        assertThat(converter.toDate(-2)).isEqualTo(LocalDate.of(2026, 1, 5));
    }

    @Test
    void countsWorkingDaysInclusive() {
        CalendarConverter converter = new CalendarConverter(
                LocalDate.of(2026, 1, 5),
                Set.of(LocalDate.of(2026, 1, 7))
        );

        // From Mon 2026-01-05 to Sat 2026-01-10 (6 calendar days)
        // With Wed 2026-01-07 as holiday:
        // Mon (yes), Tue (yes), Wed (no), Thu (yes), Fri (yes), Sat (yes) = 5 working days
        Integer count = converter.countWorkingDaysInclusive(
                LocalDate.of(2026, 1, 5),
                LocalDate.of(2026, 1, 10)
        );
        assertThat(count).isEqualTo(5);
    }
}
