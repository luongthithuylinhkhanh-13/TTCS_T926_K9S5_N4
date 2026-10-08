package com.ntdhtcct.domain.wbs;

import com.ntdhtcct.domain.project.Project;
import com.ntdhtcct.domain.project.ProjectRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class WbsScheduleBenchmarkIntegrationTest {

    @Autowired
    private WbsService wbsService;

    @Autowired
    private WbsItemRepository wbsItemRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    @DisplayName("Measures End-to-End Database Transaction for 500 tasks recalculation")
    void measuresEndToEndDatabaseTransactionFor500Tasks() {
        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);

        // 1. Setup Project & 500 tasks in DB
        Project project = projectRepository.save(new Project("BENCH-500", "Benchmark 500 tasks project"));
        UUID projectId = project.getId();

        List<WbsItem> fixtureTasks = create500Tasks(projectId);
        wbsItemRepository.saveAll(fixtureTasks);

        // 2. Measure complete End-to-End Recalculation Flow:
        //    (query by projectId + CPM calculate + saveAll batch update + commit)
        long startE2e = System.nanoTime();
        ProjectScheduleResponse response = txTemplate.execute(status -> wbsService.getSchedule(projectId));
        long e2eDurationMs = (System.nanoTime() - startE2e) / 1_000_000;

        System.out.println("==================================================");
        System.out.println("END-TO-END DATABASE TRANSACTION BENCHMARK (500 Tasks):");
        System.out.println("  - SLA Target from Approved Plan: < 300 ms");
        System.out.println(String.format("  - Measured E2E Transaction Time: %d ms (SLA < 300 ms: %s)",
                e2eDurationMs, e2eDurationMs < 300 ? "PASS" : "FAIL"));
        System.out.println("==================================================");

        assertThat(response).isNotNull();
        assertThat(response.success()).isTrue();
        assertThat(response.summary().totalTasks()).isEqualTo(500);

        // Clean up benchmark data
        txTemplate.executeWithoutResult(status -> {
            wbsItemRepository.deleteAll(wbsItemRepository.findByProjectIdOrderByWbsCodeAsc(projectId));
            projectRepository.deleteById(projectId);
        });
    }

    private List<WbsItem> create500Tasks(UUID projectId) {
        List<WbsItem> allTasks = new ArrayList<>(500);

        // 1. Sequential chains: 8 chains x 40 tasks = 320 tasks
        for (int c = 0; c < 8; c++) {
            WbsItem prev = null;
            for (int t = 0; t < 40; t++) {
                int globalIndex = c * 40 + t;
                WbsItem current = createItem(projectId, "Chain-" + c + "-" + t, globalIndex, 2);
                if (prev != null) {
                    current.getPredecessorIds().add(prev.getId());
                }
                prev = current;
                allTasks.add(current);
            }
        }

        // 2. Fan-out subgraph: 1 root + 50 children = 51 tasks
        WbsItem fanOutRoot = createItem(projectId, "FanOut-Root", 320, 3);
        allTasks.add(fanOutRoot);
        for (int i = 321; i <= 370; i++) {
            WbsItem child = createItem(projectId, "FanOut-Child-" + i, i, 2);
            child.getPredecessorIds().add(fanOutRoot.getId());
            allTasks.add(child);
        }

        // 3. Fan-in subgraph: 50 parents + 1 sink = 51 tasks
        List<WbsItem> fanInParents = new ArrayList<>(50);
        for (int i = 371; i <= 420; i++) {
            WbsItem parent = createItem(projectId, "FanIn-Parent-" + i, i, 2);
            fanInParents.add(parent);
            allTasks.add(parent);
        }
        WbsItem fanInSink = createItem(projectId, "FanIn-Sink", 421, 2);
        for (WbsItem parent : fanInParents) {
            fanInSink.getPredecessorIds().add(parent.getId());
        }
        allTasks.add(fanInSink);

        // 4. Complex mesh: 48 tasks (422 to 469), 6 layers
        List<List<WbsItem>> meshLayers = new ArrayList<>(6);
        int currentTaskIdx = 422;
        for (int l = 0; l < 6; l++) {
            List<WbsItem> layer = new ArrayList<>(8);
            for (int k = 0; k < 8; k++) {
                WbsItem node = createItem(projectId, "Mesh-L" + l + "-" + k, currentTaskIdx++, 2);
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
                }
            }
        }

        // 5. Isolated subgraph: 30 tasks (470 to 499)
        for (int i = 470; i < 500; i++) {
            WbsItem isolated = createItem(projectId, "Isolated-" + i, i, 2);
            allTasks.add(isolated);
        }

        // Distribution:
        // 100 completed (0..99)
        for (int i = 0; i < 100; i++) {
            WbsItem t = allTasks.get(i);
            t.setStartDate(LocalDate.parse("2026-01-05"));
            t.setEndDate(LocalDate.parse("2026-01-07"));
            t.setActualStartDate(LocalDate.parse("2026-01-05"));
            t.setActualEndDate(LocalDate.parse("2026-01-07"));
        }

        // 50 in-progress (100..149)
        for (int i = 100; i < 150; i++) {
            WbsItem t = allTasks.get(i);
            t.setStartDate(LocalDate.parse("2026-01-08"));
            t.setEndDate(LocalDate.parse("2026-01-10"));
            t.setActualStartDate(LocalDate.parse(i < 105 ? "2026-01-06" : "2026-01-08"));
        }

        // 300 future planned (150..449)
        for (int i = 150; i < 450; i++) {
            WbsItem t = allTasks.get(i);
            t.setStartDate(LocalDate.parse("2026-01-12"));
            t.setEndDate(LocalDate.parse("2026-01-14"));
        }

        // 50 auxiliary duration-only (450..499)
        for (int i = 450; i < 500; i++) {
            WbsItem t = allTasks.get(i);
            t.setStartDate(null);
            t.setEndDate(null);
            t.setActualStartDate(null);
            t.setActualEndDate(null);
            t.setDuration(3);
        }

        return allTasks;
    }

    private WbsItem createItem(UUID projectId, String name, int index, int duration) {
        WbsItem item = new WbsItem();
        item.setProjectId(projectId);
        item.setType("task");
        item.setWbsCode("T" + String.format("%03d", index));
        item.setName(name);
        item.setDuration(duration);
        item.setPredecessorIds(new HashSet<>());
        return item;
    }
}
