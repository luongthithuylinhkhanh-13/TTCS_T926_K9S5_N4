package com.ntdhtcct.domain.wbs;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CpmEngineTest {

    private final CpmEngine engine = new CpmEngine();

    @Test
    void calculatesForwardBackwardPassSlackAndPersistsCriticalFlags() {
        WbsItem first = task("1.1", "Foundation", "2026-01-01", "2026-01-03");
        WbsItem critical = task("1.2", "Frame", "2026-01-04", "2026-01-07", first);
        WbsItem parallel = task("1.3", "Survey", "2026-01-01", "2026-01-02");

        CpmEngine.Result result = engine.calculate(List.of(first, critical, parallel));

        assertThat(result.durationDays()).isEqualTo(7);
        assertThat(result.criticalPathCount()).isEqualTo("1");
        assertThat(first.getEs()).isZero();
        assertThat(first.getEf()).isEqualTo(3);
        assertThat(first.getLs()).isZero();
        assertThat(first.getLf()).isEqualTo(3);
        assertThat(first.getSlack()).isZero();
        assertThat(first.isCritical()).isTrue();
        assertThat(critical.getEs()).isEqualTo(3);
        assertThat(critical.getEf()).isEqualTo(7);
        assertThat(critical.getSlack()).isZero();
        assertThat(critical.isCritical()).isTrue();
        assertThat(parallel.getSlack()).isEqualTo(5);
        assertThat(parallel.isCritical()).isFalse();
    }

    @Test
    void countsDistinctTiedCriticalPaths() {
        WbsItem firstBranch = task("1.1", "North foundation", "2026-01-01", "2026-01-02");
        WbsItem secondBranch = task("1.2", "South foundation", "2026-01-01", "2026-01-02");
        WbsItem firstFinish = task("1.3", "North frame", "2026-01-03", "2026-01-04", firstBranch);
        WbsItem secondFinish = task("1.4", "South frame", "2026-01-03", "2026-01-04", secondBranch);

        CpmEngine.Result result = engine.calculate(
                List.of(firstBranch, secondBranch, firstFinish, secondFinish)
        );

        assertThat(result.durationDays()).isEqualTo(4);
        assertThat(result.criticalPathCount()).isEqualTo("2");
        assertThat(List.of(firstBranch, secondBranch, firstFinish, secondFinish))
                .allMatch(WbsItem::isCritical)
                .allMatch(item -> item.getSlack() == 0);
    }

    private WbsItem task(String code, String name, String start, String end, WbsItem... predecessors) {
        WbsItem item = new WbsItem();
        setId(item, UUID.randomUUID());
        item.setType("task");
        item.setWbsCode(code);
        item.setName(name);
        item.setStartDate(LocalDate.parse(start));
        item.setEndDate(LocalDate.parse(end));
        item.setPredecessorIds(java.util.Arrays.stream(predecessors)
                .map(WbsItem::getId)
                .collect(java.util.stream.Collectors.toSet()));
        return item;
    }

    private void setId(WbsItem item, UUID id) {
        try {
            Field field = WbsItem.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(item, id);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Could not initialize test task id", e);
        }
    }
}