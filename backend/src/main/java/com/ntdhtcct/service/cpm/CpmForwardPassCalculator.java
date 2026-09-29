package com.ntdhtcct.service.cpm;

import com.ntdhtcct.common.exception.BadRequestException;
import com.ntdhtcct.entity.DependencyType;

import java.util.*;

/**
 * T-19 (NTDHTCT-145): Bộ tính toán thuật toán duyệt tiến (Forward Pass Calculation)
 * trong phương pháp đường găng CPM / PDM.
 *
 * Chức năng:
 * 1. Kiểm tra tính toàn vẹn của đồ thị mạng công việc (Validation).
 * 2. Phát hiện chu trình phụ thuộc (Cycle Detection) - mạng công việc phải là DAG (Directed Acyclic Graph).
 * 3. Sắp xếp thứ tự Topo (Topological Sort).
 * 4. Tính toán thời điểm Khởi sớm (Early Start - ES) và Kết thúc sớm (Early Finish - EF) cho tất cả công việc.
 * 5. Xác định tổng thời lượng dự án (Project Duration).
 */
public class CpmForwardPassCalculator {

    /**
     * Dữ liệu đầu vào cho 1 công việc khi tính toán
     */
    public record TaskInput(
            Long id,
            String code,
            String name,
            double duration
    ) {}

    /**
     * Dữ liệu đầu vào cho 1 quan hệ phụ thuộc
     */
    public record DependencyInput(
            Long id,
            Long predecessorId,
            Long successorId,
            DependencyType type,
            double lag
    ) {}

    /**
     * Kết quả tính toán ES và EF cho 1 công việc
     */
    public record TaskScheduleResult(
            Long taskId,
            String code,
            String name,
            double duration,
            double earlyStart,
            double earlyFinish
    ) {}

    /**
     * Kết quả tổng thể của thuật toán Forward Pass
     */
    public record ForwardPassOutput(
            double projectDuration,
            Map<Long, TaskScheduleResult> taskResults,
            List<TaskScheduleResult> orderedResults
    ) {}

    /**
     * Thực hiện thuật toán Forward Pass tính toán ES và EF cho toàn bộ mạng công việc.
     *
     * @param tasks        Danh sách công việc dự án
     * @param dependencies Danh sách các quan hệ phụ thuộc
     * @return ForwardPassOutput chứa ES, EF của từng task và tổng thời lượng dự án
     */
    public ForwardPassOutput calculate(List<TaskInput> tasks, List<DependencyInput> dependencies) {
        if (tasks == null || tasks.isEmpty()) {
            return new ForwardPassOutput(0.0, Collections.emptyMap(), Collections.emptyList());
        }

        // 1. Kiểm tra tính hợp lệ của dữ liệu đầu vào
        Map<Long, TaskInput> taskMap = new LinkedHashMap<>();
        for (TaskInput task : tasks) {
            if (task.id() == null) {
                throw new BadRequestException("INVALID_TASK", "ID công việc không được null");
            }
            if (task.duration() < 0) {
                throw new BadRequestException("INVALID_DURATION",
                        "Thời lượng của công việc [" + task.code() + "] không được âm: " + task.duration());
            }
            taskMap.put(task.id(), task);
        }

        // 2. Xây dựng danh sách kề và danh sách tiền nhiệm
        // Adjacency: predecessor -> list of (successor, type, lag)
        Map<Long, List<DependencyInput>> outgoingDeps = new HashMap<>();
        // Incoming: successor -> list of (predecessor, type, lag)
        Map<Long, List<DependencyInput>> incomingDeps = new HashMap<>();

        for (TaskInput t : tasks) {
            outgoingDeps.put(t.id(), new ArrayList<>());
            incomingDeps.put(t.id(), new ArrayList<>());
        }

        if (dependencies != null) {
            for (DependencyInput dep : dependencies) {
                if (dep.predecessorId().equals(dep.successorId())) {
                    TaskInput selfTask = taskMap.get(dep.predecessorId());
                    String name = selfTask != null ? selfTask.code() : dep.predecessorId().toString();
                    throw new BadRequestException("SELF_DEPENDENCY_NOT_ALLOWED",
                            "Công việc [" + name + "] không thể tự phụ thuộc vào chính mình");
                }

                if (!taskMap.containsKey(dep.predecessorId())) {
                    throw new BadRequestException("INVALID_DEPENDENCY",
                            "Công việc tiền nhiệm [ID=" + dep.predecessorId() + "] không tồn tại trong danh sách công việc");
                }
                if (!taskMap.containsKey(dep.successorId())) {
                    throw new BadRequestException("INVALID_DEPENDENCY",
                            "Công việc kế nhiệm [ID=" + dep.successorId() + "] không tồn tại trong danh sách công việc");
                }

                outgoingDeps.get(dep.predecessorId()).add(dep);
                incomingDeps.get(dep.successorId()).add(dep);
            }
        }

        // 3. Kiểm tra chu trình (Cycle Detection) và tìm thứ tự Topo (Topological Order)
        List<Long> topoOrder = detectCycleAndTopologicalSort(taskMap.keySet(), outgoingDeps);

        // 4. Thuật toán duyệt tiến (Forward Pass Calculation)
        Map<Long, TaskScheduleResult> results = new HashMap<>();
        List<TaskScheduleResult> orderedResults = new ArrayList<>();
        double projectDuration = 0.0;

        for (Long taskId : topoOrder) {
            TaskInput currentTask = taskMap.get(taskId);
            List<DependencyInput> predecessors = incomingDeps.get(taskId);

            double earlyStart;
            if (predecessors.isEmpty()) {
                // Không có công việc tiền nhiệm: Bắt đầu từ mốc 0.0
                earlyStart = 0.0;
            } else {
                // Có các công việc tiền nhiệm: Duyệt qua tất cả các liên kết và tìm giá trị lớn nhất
                double maxRequiredEs = 0.0;
                for (DependencyInput dep : predecessors) {
                    TaskScheduleResult predResult = results.get(dep.predecessorId());
                    if (predResult == null) {
                        throw new IllegalStateException("Lỗi nội bộ: Không tìm thấy kết quả tính toán của tiền nhiệm [ID="
                                + dep.predecessorId() + "]");
                    }

                    // T-18: Áp dụng công thức tương ứng theo loại quan hệ FS, SS, FF, SF
                    double requiredEs = DependencyFormulaCalculator.calculateEarlyStartRequirement(
                            predResult.earlyStart(),
                            predResult.earlyFinish(),
                            currentTask.duration(),
                            dep.type(),
                            dep.lag()
                    );

                    if (requiredEs > maxRequiredEs) {
                        maxRequiredEs = requiredEs;
                    }
                }
                // Thời điểm bắt đầu không được âm so với mốc dự án
                earlyStart = Math.max(0.0, maxRequiredEs);
            }

            // EF = ES + Duration
            double earlyFinish = earlyStart + currentTask.duration();

            TaskScheduleResult result = new TaskScheduleResult(
                    currentTask.id(),
                    currentTask.code(),
                    currentTask.name(),
                    currentTask.duration(),
                    earlyStart,
                    earlyFinish
            );

            results.put(taskId, result);
            orderedResults.add(result);

            if (earlyFinish > projectDuration) {
                projectDuration = earlyFinish;
            }
        }

        return new ForwardPassOutput(projectDuration, results, orderedResults);
    }

    /**
     * Thuật toán phát hiện chu trình (Cycle Detection) bằng thuật toán Kahn (In-Degree).
     * Trả về thứ tự sắp xếp Topo (Topological Sort) nếu không có chu trình.
     * Ném ngoại lệ BadRequestException nếu phát hiện chu trình phụ thuộc vòng kín.
     */
    private List<Long> detectCycleAndTopologicalSort(Set<Long> taskIds, Map<Long, List<DependencyInput>> outgoingDeps) {
        Map<Long, Integer> inDegree = new HashMap<>();
        for (Long id : taskIds) {
            inDegree.put(id, 0);
        }

        for (List<DependencyInput> deps : outgoingDeps.values()) {
            for (DependencyInput dep : deps) {
                inDegree.put(dep.successorId(), inDegree.get(dep.successorId()) + 1);
            }
        }

        // Hàng đợi cho các nút có bậc vào = 0 (nút gốc/không có tiền nhiệm)
        Queue<Long> queue = new ArrayDeque<>();
        for (Long id : taskIds) {
            if (inDegree.get(id) == 0) {
                queue.add(id);
            }
        }

        List<Long> topoOrder = new ArrayList<>();
        while (!queue.isEmpty()) {
            Long current = queue.poll();
            topoOrder.add(current);

            for (DependencyInput dep : outgoingDeps.get(current)) {
                Long neighbor = dep.successorId();
                int newDegree = inDegree.get(neighbor) - 1;
                inDegree.put(neighbor, newDegree);
                if (newDegree == 0) {
                    queue.add(neighbor);
                }
            }
        }

        // Nếu số lượng phần tử trong topoOrder nhỏ hơn tổng số task => Tồn tại chu trình!
        if (topoOrder.size() < taskIds.size()) {
            List<Long> cyclicTaskIds = new ArrayList<>();
            for (Map.Entry<Long, Integer> entry : inDegree.entrySet()) {
                if (entry.getValue() > 0) {
                    cyclicTaskIds.add(entry.getKey());
                }
            }
            throw new BadRequestException("SCHEDULE_CYCLE_DETECTED",
                    "Phát hiện chu trình phụ thuộc vòng kín (Circular Dependency / Cycle) trong mạng công việc. Các công việc liên quan: "
                            + cyclicTaskIds);
        }

        return topoOrder;
    }
}
