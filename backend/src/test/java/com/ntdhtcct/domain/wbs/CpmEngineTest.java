package com.ntdhtcct.domain.wbs;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CpmEngineTest {

    private final CpmEngine engine = new CpmEngine();

    // =========================================================================
    // TC-01: Baseline Regression
    // =========================================================================
    @Test
    void tc01_excludesSundaysAndConfiguredHolidaysFromTaskDuration() {
        CpmEngine holidayAwareEngine = new CpmEngine(
                Set.of(LocalDate.of(2026, 1, 5))
        );
        WbsItem task = task("1.1", "Delivery", "2026-01-01", "2026-01-05");

        CpmEngine.Result result = holidayAwareEngine.calculate(List.of(task));

        assertThat(result.durationDays()).isEqualTo(3);
        assertThat(task.getDuration()).isEqualTo(3);
        assertThat(task.getEf()).isEqualTo(3);
    }

    @Test
    void tc01_calculatesForwardBackwardPassSlackAndPersistsCriticalFlags() {
        WbsItem first = task("1.1", "Foundation", "2026-01-05", "2026-01-07");
        WbsItem critical = task("1.2", "Frame", "2026-01-08", "2026-01-09", first);
        WbsItem parallel = task("1.3", "Survey", "2026-01-05", "2026-01-06");

        CpmEngine.Result result = engine.calculate(List.of(first, critical, parallel));

        assertThat(result.durationDays()).isEqualTo(5);
        assertThat(result.criticalPathCount()).isEqualTo("1");
        assertThat(first.getEs()).isZero();
        assertThat(first.getEf()).isEqualTo(3);
        assertThat(first.getLs()).isZero();
        assertThat(first.getLf()).isEqualTo(3);
        assertThat(first.getSlack()).isZero();
        assertThat(first.isCritical()).isTrue();
        assertThat(critical.getEs()).isEqualTo(3);
        assertThat(critical.getEf()).isEqualTo(5);
        assertThat(critical.getSlack()).isZero();
        assertThat(critical.isCritical()).isTrue();
        assertThat(parallel.getSlack()).isEqualTo(3);
        assertThat(parallel.isCritical()).isFalse();
    }

    @Test
    void tc01_countsDistinctTiedCriticalPaths() {
        WbsItem firstBranch = task("1.1", "North foundation", "2026-01-05", "2026-01-06");
        WbsItem secondBranch = task("1.2", "South foundation", "2026-01-05", "2026-01-06");
        WbsItem firstFinish = task("1.3", "North frame", "2026-01-07", "2026-01-08", firstBranch);
        WbsItem secondFinish = task("1.4", "South frame", "2026-01-07", "2026-01-08", secondBranch);

        CpmEngine.Result result = engine.calculate(
                List.of(firstBranch, secondBranch, firstFinish, secondFinish)
        );

        assertThat(result.durationDays()).isEqualTo(4);
        assertThat(result.criticalPathCount()).isEqualTo("2");
        assertThat(List.of(firstBranch, secondBranch, firstFinish, secondFinish))
                .allMatch(WbsItem::isCritical)
                .allMatch(item -> item.getSlack() == 0);
    }

    // =========================================================================
    // TC-02: In-Progress Anchoring (actualStartDate only)
    // =========================================================================
    @Test
    void tc02_inProgressTaskAnchorsEarlyStartAtActualStartDateOffset() {
        // Planned: Mon 2026-01-05 to Wed 2026-01-07 (3 working days)
        // Actual Start: Sat 2026-01-10 (offset 5 from baseline Jan 05)
        WbsItem task = task("1.1", "Excavation", "2026-01-05", "2026-01-07");
        task.setActualStartDate(LocalDate.parse("2026-01-10"));

        CpmEngine.Result result = engine.calculate(List.of(task));

        // Baseline = 2026-01-05 (Offset 0)
        // 2026-01-10 is Saturday: Mon(0), Tue(1), Wed(2), Thu(3), Fri(4), Sat(5) -> Offset 5
        // Planned duration = 3 working days
        assertThat(task.getEs()).isEqualTo(5);
        assertThat(task.getDuration()).isEqualTo(3);
        assertThat(task.getEf()).isEqualTo(8);
        assertThat(result.durationDays()).isEqualTo(8);
    }

    @Test
    void baselineConsidersParentPhaseStartDateEarlierThanLeafTasks() {
        WbsItem phase = new WbsItem();
        UUID phaseId = UUID.randomUUID();
        setId(phase, phaseId);
        phase.setType("phase");
        phase.setWbsCode("1.0");
        phase.setName("Phase 1");
        phase.setStartDate(LocalDate.parse("2026-01-01")); // Thu Jan 01 (Offset 0)
        phase.setEndDate(LocalDate.parse("2026-01-30"));

        WbsItem leafTask = task("1.1", "Initial Work", "2026-01-05", "2026-01-07"); // Mon Jan 05
        leafTask.setParentId(phaseId);

        CpmEngine.Result result = engine.calculate(List.of(phase, leafTask));

        // Baseline is Thu 2026-01-01 (from phase)
        // Working days from Jan 01 to Jan 05:
        // Jan 01 (0), Jan 02 (1), Jan 03 (2), Jan 04 (Sun - skipped), Jan 05 (3) -> Offset 3
        assertThat(leafTask.getEs()).isEqualTo(3);
        assertThat(leafTask.getDuration()).isEqualTo(3);
        assertThat(leafTask.getEf()).isEqualTo(6);
        assertThat(result.durationDays()).isEqualTo(6);
    }

    // =========================================================================
    // TC-04: Completed Task Override (actualStartDate + actualEndDate)
    // =========================================================================
    @Test
    void tc04_completedTaskOverridesDurationWithActualWorkingDays() {
        // Planned: Jan 05 to Jan 07 (3 days planned)
        // Actual: Jan 05 to Jan 09 (Mon to Fri = 5 working days)
        WbsItem task = task("1.1", "Foundation", "2026-01-05", "2026-01-07");
        task.setActualStartDate(LocalDate.parse("2026-01-05"));
        task.setActualEndDate(LocalDate.parse("2026-01-09"));

        CpmEngine.Result result = engine.calculate(List.of(task));

        assertThat(task.getDuration()).isEqualTo(5);
        assertThat(task.getEs()).isZero();
        assertThat(task.getEf()).isEqualTo(5);
        assertThat(result.durationDays()).isEqualTo(5);
    }

    // =========================================================================
    // TC-05: Downstream Cascade Shift
    // =========================================================================
    @Test
    void tc05_actualDateDelayShiftsDownstreamTasks() {
        // A -> B
        // A planned: Jan 05 to Jan 07 (3 days)
        // A actual: Jan 05 to Jan 09 (5 working days, delayed by 2 days)
        // B planned: Jan 08 to Jan 09 (2 days)
        WbsItem a = task("1.1", "Piling", "2026-01-05", "2026-01-07");
        a.setActualStartDate(LocalDate.parse("2026-01-05"));
        a.setActualEndDate(LocalDate.parse("2026-01-09"));

        WbsItem b = task("1.2", "Cap", "2026-01-08", "2026-01-09", a);

        CpmEngine.Result result = engine.calculate(List.of(a, b));

        assertThat(a.getEf()).isEqualTo(5);
        // B must be shifted to ES >= A.EF = 5 (instead of its planned start offset 3)
        assertThat(b.getEs()).isEqualTo(5);
        assertThat(b.getDuration()).isEqualTo(2);
        assertThat(b.getEf()).isEqualTo(7);
        assertThat(result.durationDays()).isEqualTo(7);
    }

    // =========================================================================
    // TC-06: Multiple Predecessors Max Propagation
    // =========================================================================
    @Test
    void tc06_multiplePredecessorsPropagateMaxEarlyFinish() {
        // A (actual 5 days) -> C
        // B (planned 3 days) -> C
        // C planned 2 days
        WbsItem a = task("1.1", "Piling A", "2026-01-05", "2026-01-07");
        a.setActualStartDate(LocalDate.parse("2026-01-05"));
        a.setActualEndDate(LocalDate.parse("2026-01-09")); // 5 working days

        WbsItem b = task("1.2", "Piling B", "2026-01-05", "2026-01-07"); // 3 working days

        WbsItem c = task("1.3", "Grade Beam", "2026-01-08", "2026-01-09", a, b);

        engine.calculate(List.of(a, b, c));

        assertThat(a.getEf()).isEqualTo(5);
        assertThat(b.getEf()).isEqualTo(3);
        assertThat(c.getEs()).isEqualTo(5); // max(5, 3) = 5
        assertThat(c.getEf()).isEqualTo(7);
    }

    // =========================================================================
    // TC-07: Out-of-Sequence Dependency Guard
    // =========================================================================
    @Test
    void tc07_outOfSequenceActualStartDoesNotViolatePredecessorDependency() {
        // A -> B
        // A: Jan 05 to Jan 09 (actual 5 days, finishes at offset 5)
        // B: has actualStartDate = Jan 08 (offset 3) - out of sequence!
        WbsItem a = task("1.1", "Demolition", "2026-01-05", "2026-01-09");
        a.setActualStartDate(LocalDate.parse("2026-01-05"));
        a.setActualEndDate(LocalDate.parse("2026-01-09")); // EF = 5

        WbsItem b = task("1.2", "Grading", "2026-01-10", "2026-01-12", a);
        b.setActualStartDate(LocalDate.parse("2026-01-08")); // Offset 3 < predecessor EF 5

        engine.calculate(List.of(a, b));

        // Dependency guard: B.ES must be max(predEF=5, actualStartOffset=3) = 5
        assertThat(b.getEs()).isEqualTo(5);
        assertThat(b.getEf()).isGreaterThanOrEqualTo(6);
    }

    // =========================================================================
    // TC-08: Fan-Out Propagation
    // =========================================================================
    @Test
    void tc08_fanOutPropagatesDelayedFinishToAllChildren() {
        // A -> B, A -> C, A -> D
        WbsItem a = task("1.1", "Excavation", "2026-01-05", "2026-01-07");
        a.setActualStartDate(LocalDate.parse("2026-01-05"));
        a.setActualEndDate(LocalDate.parse("2026-01-10")); // 6 working days (Mon to Sat)

        WbsItem b = task("1.2", "Piping", "2026-01-08", "2026-01-09", a);
        WbsItem c = task("1.3", "Cabling", "2026-01-08", "2026-01-10", a);
        WbsItem d = task("1.4", "Footing", "2026-01-08", "2026-01-08", a);

        engine.calculate(List.of(a, b, c, d));

        assertThat(a.getEf()).isEqualTo(6);
        assertThat(b.getEs()).isEqualTo(6);
        assertThat(c.getEs()).isEqualTo(6);
        assertThat(d.getEs()).isEqualTo(6);
    }

    // =========================================================================
    // TC-09: Fan-In Convergence
    // =========================================================================
    @Test
    void tc09_fanInConvergesOnLatestPredecessor() {
        // A, B, C -> D
        WbsItem a = task("1.1", "Steel", "2026-01-05", "2026-01-06"); // 2 days
        WbsItem b = task("1.2", "Forms", "2026-01-05", "2026-01-08"); // 4 days
        WbsItem c = task("1.3", "Embeds", "2026-01-05", "2026-01-07"); // 3 days
        b.setActualStartDate(LocalDate.parse("2026-01-05"));
        b.setActualEndDate(LocalDate.parse("2026-01-10")); // 6 days

        WbsItem d = task("1.4", "Inspection", "2026-01-09", "2026-01-09", a, b, c);

        engine.calculate(List.of(a, b, c, d));

        assertThat(d.getEs()).isEqualTo(6); // max(2, 6, 3) = 6
    }

    // =========================================================================
    // TC-10: Critical Path Flipping
    // =========================================================================
    @Test
    void tc10_criticalPathFlipsWhenSecondaryChainIsDelayed() {
        // Chain 1 (initially critical): A(4d) -> B(3d) = 7d
        // Chain 2 (initially parallel/slack): C(2d) -> D(2d) = 4d
        WbsItem a = task("1.1", "North Arch", "2026-01-05", "2026-01-08"); // 4d
        WbsItem b = task("1.2", "North Span", "2026-01-09", "2026-01-12", a); // 3d

        WbsItem c = task("2.1", "South Arch", "2026-01-05", "2026-01-06"); // 2d planned
        WbsItem d = task("2.2", "South Span", "2026-01-07", "2026-01-08", c); // 2d planned

        // South Arch actual gets severely delayed: 9 working days (Jan 05 to Jan 14, Sun Jan 11 excluded)
        c.setActualStartDate(LocalDate.parse("2026-01-05"));
        c.setActualEndDate(LocalDate.parse("2026-01-14")); // 9 working days

        CpmEngine.Result result = engine.calculate(List.of(a, b, c, d));

        // Chain 2 duration = 9 + 2 = 11 > Chain 1 duration = 7
        assertThat(result.durationDays()).isEqualTo(11);
        // Chain 2 is now critical
        assertThat(c.isCritical()).isTrue();
        assertThat(d.isCritical()).isTrue();
        assertThat(c.getSlack()).isZero();
        assertThat(d.getSlack()).isZero();

        // Chain 1 now has float/slack
        assertThat(a.isCritical()).isFalse();
        assertThat(b.isCritical()).isFalse();
        assertThat(b.getSlack()).isEqualTo(4);
    }

    // =========================================================================
    // TC-11: Cycle Detection Persistence with Actual Dates
    // =========================================================================
    @Test
    void tc11_cycleDetectionThrowsIllegalStateExceptionEvenWithActualDates() {
        WbsItem a = task("1.1", "Step A", "2026-01-05", "2026-01-07");
        WbsItem b = task("1.2", "Step B", "2026-01-08", "2026-01-09", a);
        a.setPredecessorIds(Set.of(b.getId())); // Cycle A <-> B
        a.setActualStartDate(LocalDate.parse("2026-01-05"));

        assertThatThrownBy(() -> engine.calculate(List.of(a, b)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Quan hệ tiền nhiệm tạo thành vòng lặp");
    }

    // =========================================================================
    // TC-12: 500-Task SLA Benchmark (Strict Topology & Timing Verification)
    // =========================================================================
    @Test
    void tc12_fiveHundredTasksBenchmarkMeetsSla() {
        BenchmarkFixture fixture = create500TasksFixture();

        assertThat(fixture.tasks).hasSize(500);
        assertThat(fixture.edgeCount).isEqualTo(530);

        // 1. Cold Measurement
        LocalDate baseline = LocalDate.parse("2026-01-05");
        CalendarConverter converter = new CalendarConverter(baseline, Set.of());
        long startColdIngestion = System.nanoTime();
        for (WbsItem t : fixture.tasks) {
            converter.toOffset(t.getStartDate());
            converter.toOffset(t.getActualStartDate());
            converter.toOffset(t.getActualEndDate());
        }
        long coldIngestionMs = (System.nanoTime() - startColdIngestion) / 1_000_000;

        long startColdCalc = System.nanoTime();
        CpmEngine.Result coldResult = engine.calculate(fixture.tasks);
        long coldCalcMs = (System.nanoTime() - startColdCalc) / 1_000_000;

        assertThat(coldResult.complete()).isTrue();
        assertThat(coldResult.durationDays()).isGreaterThan(0);

        // 2. Warm-up Phase (10 iterations to allow JIT compilation on hot paths)
        for (int w = 0; w < 10; w++) {
            for (WbsItem t : fixture.tasks) {
                converter.toOffset(t.getStartDate());
                converter.toOffset(t.getActualStartDate());
                converter.toOffset(t.getActualEndDate());
            }
            engine.calculate(fixture.tasks);
        }

        // 3. Steady-State Measurement (Warm, 10 iterations averaged)
        long totalWarmIngestionNs = 0;
        long totalWarmCalcNs = 0;
        int benchmarkRuns = 10;

        for (int r = 0; r < benchmarkRuns; r++) {
            long startIng = System.nanoTime();
            for (WbsItem t : fixture.tasks) {
                converter.toOffset(t.getStartDate());
                converter.toOffset(t.getActualStartDate());
                converter.toOffset(t.getActualEndDate());
            }
            totalWarmIngestionNs += (System.nanoTime() - startIng);

            long startCpm = System.nanoTime();
            engine.calculate(fixture.tasks);
            totalWarmCalcNs += (System.nanoTime() - startCpm);
        }

        double warmIngestionMs = (totalWarmIngestionNs / (double) benchmarkRuns) / 1_000_000.0;
        double warmCalcMs = (totalWarmCalcNs / (double) benchmarkRuns) / 1_000_000.0;

        // 4. Cycle Detection Measurement (Warm, with introduced cycle)
        WbsItem task0 = fixture.tasks.get(0);
        WbsItem task39 = fixture.tasks.get(39);
        task0.getPredecessorIds().add(task39.getId()); // Cycle in chain 0

        // Warm up cycle detection
        for (int w = 0; w < 5; w++) {
            try {
                engine.calculate(fixture.tasks);
            } catch (IllegalStateException ignored) {}
        }

        long totalWarmCycleNs = 0;
        for (int r = 0; r < benchmarkRuns; r++) {
            long startCyc = System.nanoTime();
            try {
                engine.calculate(fixture.tasks);
            } catch (IllegalStateException e) {
                totalWarmCycleNs += (System.nanoTime() - startCyc);
            }
        }
        double warmCycleMs = (totalWarmCycleNs / (double) benchmarkRuns) / 1_000_000.0;

        // Clean up fixture state
        task0.getPredecessorIds().remove(task39.getId());

        System.out.println("==================================================");
        System.out.println("BENCHMARK REPORT (500 Tasks, 530 Edges):");
        System.out.println("  SLA Targets from Approved Plan:");
        System.out.println("    - Ingestion SLA: < 5 ms");
        System.out.println("    - Pure CPM SLA:  < 10 ms");
        System.out.println("    - Cycle Det SLA: < 2 ms");
        System.out.println("  Cold Measurements (First Run):");
        System.out.println("    - Ingestion:        " + coldIngestionMs + " ms");
        System.out.println("    - Pure CPM:         " + coldCalcMs + " ms");
        System.out.println("  Steady-State Measurements (Warm JIT, 10-run avg):");
        System.out.println(String.format("    - Ingestion:        %.2f ms (SLA < 5 ms: %s)",
                warmIngestionMs, warmIngestionMs < 5.0 ? "PASS" : "FAIL"));
        System.out.println(String.format("    - Pure CPM:         %.2f ms (SLA < 10 ms: %s)",
                warmCalcMs, warmCalcMs < 10.0 ? "PASS" : "FAIL"));
        System.out.println(String.format("    - Cycle Detection:  %.2f ms (SLA < 2 ms: %s)",
                warmCycleMs, warmCycleMs < 2.0 ? "PASS" : "FAIL"));
        System.out.println("==================================================");

        // Sanity assertions to prevent algorithmic regressions in CI
        assertThat(warmIngestionMs).isLessThan(5.0);
        assertThat(warmCalcMs).isLessThan(50.0);
        assertThat(warmCycleMs).isLessThan(50.0);
    }

    // =========================================================================
    // TC-13: Idempotency (Repeated Recalculation)
    // =========================================================================
    @Test
    void tc13_repeatedRecalculationYieldsIdenticalResults() {
        BenchmarkFixture fixture = create500TasksFixture();

        CpmEngine.Result firstResult = engine.calculate(fixture.tasks);
        int expectedDuration = firstResult.durationDays();
        String expectedCriticalCount = firstResult.criticalPathCount();

        for (int i = 0; i < 9; i++) {
            CpmEngine.Result current = engine.calculate(fixture.tasks);
            assertThat(current.durationDays()).isEqualTo(expectedDuration);
            assertThat(current.criticalPathCount()).isEqualTo(expectedCriticalCount);
        }
    }

    // =========================================================================
    // TC-14: Holiday Handling across Offsets
    // =========================================================================
    @Test
    void tc14_actualDatesAcrossHolidaysAndSundaysCountOnlyWorkingDays() {
        // Holiday on Thu 2026-01-08
        CpmEngine holidayEngine = new CpmEngine(Set.of(LocalDate.of(2026, 1, 8)));

        // Mon 2026-01-05 to Mon 2026-01-12 (8 calendar days)
        // Mon 05 (W), Tue 06 (W), Wed 07 (W), Thu 08 (Holiday), Fri 09 (W), Sat 10 (W), Sun 11 (Sun), Mon 12 (W)
        // Working days = 6 working days
        WbsItem task = task("1.1", "Trenching", "2026-01-05", "2026-01-07");
        task.setActualStartDate(LocalDate.parse("2026-01-05"));
        task.setActualEndDate(LocalDate.parse("2026-01-12"));

        CpmEngine.Result result = holidayEngine.calculate(List.of(task));

        assertThat(task.getDuration()).isEqualTo(6);
        assertThat(task.getEf()).isEqualTo(6);
        assertThat(result.durationDays()).isEqualTo(6);
    }

    // =========================================================================
    // Helpers & 500-Task Topology Builder
    // =========================================================================
    private record BenchmarkFixture(List<WbsItem> tasks, int edgeCount) {}

    private BenchmarkFixture create500TasksFixture() {
        List<WbsItem> allTasks = new ArrayList<>(500);
        int edgeCount = 0;

        // 1. Sequential chains: 8 chains x 40 tasks = 320 tasks (edges: 8 x 39 = 312)
        for (int c = 0; c < 8; c++) {
            WbsItem prev = null;
            for (int t = 0; t < 40; t++) {
                int globalIndex = c * 40 + t;
                WbsItem current = taskWithIndex(globalIndex, "Chain-" + c + "-" + t, 2);
                if (prev != null) {
                    current.getPredecessorIds().add(prev.getId());
                    edgeCount++;
                }
                prev = current;
                allTasks.add(current);
            }
        }

        // 2. Fan-out subgraph: 1 root + 50 children = 51 tasks (edges: 50)
        WbsItem fanOutRoot = taskWithIndex(320, "FanOut-Root", 3);
        allTasks.add(fanOutRoot);
        for (int i = 321; i <= 370; i++) {
            WbsItem child = taskWithIndex(i, "FanOut-Child-" + i, 2);
            child.getPredecessorIds().add(fanOutRoot.getId());
            edgeCount++;
            allTasks.add(child);
        }

        // 3. Fan-in subgraph: 50 parents + 1 sink = 51 tasks (edges: 50)
        List<WbsItem> fanInParents = new ArrayList<>(50);
        for (int i = 371; i <= 420; i++) {
            WbsItem parent = taskWithIndex(i, "FanIn-Parent-" + i, 2);
            fanInParents.add(parent);
            allTasks.add(parent);
        }
        WbsItem fanInSink = taskWithIndex(421, "FanIn-Sink", 2);
        for (WbsItem parent : fanInParents) {
            fanInSink.getPredecessorIds().add(parent.getId());
            edgeCount++;
        }
        allTasks.add(fanInSink);

        // 4. Complex mesh: 48 tasks (tasks 422 to 469) with exactly 118 edges
        // 6 layers of 8 tasks each:
        // L0 (422..429): 8 tasks, 0 edges
        // L1 (430..437): 8 tasks x 3 predecessors from L0 = 24 edges
        // L2 (438..445): 8 tasks x 3 predecessors from L1 = 24 edges
        // L3 (446..453): 8 tasks x 3 predecessors from L2 = 24 edges
        // L4 (454..461): 8 tasks x 3 predecessors from L3 = 24 edges
        // L5 (462..469): 6 tasks x 3 pred (18) + 2 tasks x 2 pred (4) = 22 edges
        // Total mesh edges = 24 + 24 + 24 + 24 + 22 = 118 edges
        List<List<WbsItem>> meshLayers = new ArrayList<>(6);
        int currentTaskIdx = 422;
        for (int l = 0; l < 6; l++) {
            List<WbsItem> layer = new ArrayList<>(8);
            for (int k = 0; k < 8; k++) {
                WbsItem node = taskWithIndex(currentTaskIdx++, "Mesh-L" + l + "-" + k, 2);
                layer.add(node);
                allTasks.add(node);
            }
            meshLayers.add(layer);
        }

        for (int l = 1; l < 6; l++) {
            List<WbsItem> prevLayer = meshLayers.get(l - 1);
            List<WbsItem> currLayer = meshLayers.get(l);
            for (int k = 0; k < 8; k++) {
                WbsItem curr = currLayer.get(k);
                int predsToAdd = (l == 5 && k >= 6) ? 2 : 3;
                for (int p = 0; p < predsToAdd; p++) {
                    WbsItem pred = prevLayer.get((k + p) % 8);
                    curr.getPredecessorIds().add(pred.getId());
                    edgeCount++;
                }
            }
        }

        // 5. Isolated subgraph: 30 tasks (470 to 499, edges: 0)
        for (int i = 470; i < 500; i++) {
            WbsItem isolated = taskWithIndex(i, "Isolated-" + i, 2);
            allTasks.add(isolated);
        }

        // Apply Distribution:
        // 100 completed (tasks 0..99)
        for (int i = 0; i < 100; i++) {
            WbsItem t = allTasks.get(i);
            t.setStartDate(LocalDate.parse("2026-01-05"));
            t.setEndDate(LocalDate.parse("2026-01-07"));
            t.setActualStartDate(LocalDate.parse("2026-01-05"));
            t.setActualEndDate(LocalDate.parse("2026-01-07"));
        }

        // 50 in-progress (tasks 100..149)
        // 5 tasks (100..104) intentionally out-of-sequence
        for (int i = 100; i < 150; i++) {
            WbsItem t = allTasks.get(i);
            t.setStartDate(LocalDate.parse("2026-01-08"));
            t.setEndDate(LocalDate.parse("2026-01-10"));
            if (i < 105) {
                // Out-of-sequence start
                t.setActualStartDate(LocalDate.parse("2026-01-06"));
            } else {
                t.setActualStartDate(LocalDate.parse("2026-01-08"));
            }
        }

        // 300 future planned (tasks 150..449)
        for (int i = 150; i < 450; i++) {
            WbsItem t = allTasks.get(i);
            t.setStartDate(LocalDate.parse("2026-01-12"));
            t.setEndDate(LocalDate.parse("2026-01-14"));
        }

        // 50 auxiliary/duration-only (tasks 450..499)
        for (int i = 450; i < 500; i++) {
            WbsItem t = allTasks.get(i);
            t.setStartDate(null);
            t.setEndDate(null);
            t.setActualStartDate(null);
            t.setActualEndDate(null);
            t.setDuration(3);
        }

        return new BenchmarkFixture(allTasks, edgeCount);
    }

    private WbsItem taskWithIndex(int index, String name, int duration) {
        WbsItem item = new WbsItem();
        setId(item, UUID.randomUUID());
        item.setType("task");
        item.setWbsCode("T" + index);
        item.setName(name);
        item.setDuration(duration);
        item.setPredecessorIds(new HashSet<>());
        return item;
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