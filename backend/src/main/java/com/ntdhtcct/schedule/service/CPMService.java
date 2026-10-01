package com.ntdhtcct.schedule.service;

import com.ntdhtcct.schedule.entity.Task;
import com.ntdhtcct.schedule.repository.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class CPMService {

    private final TaskRepository taskRepository;

    public CPMService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @Transactional
    public void calculateAndSaveCPM() {
        List<Task> tasks = taskRepository.findAll();
        if (tasks.isEmpty()) return;

        calculateCPM(tasks);
        taskRepository.saveAll(tasks);
    }

    public void calculateCPM(List<Task> tasks) {
        Map<Long, Task> taskMap = new HashMap<>();
        Map<Long, List<Task>> successorsMap = new HashMap<>();

        for (Task t : tasks) {
            taskMap.put(t.getId(), t);
            successorsMap.putIfAbsent(t.getId(), new ArrayList<>());
        }

        // Build successors
        for (Task t : tasks) {
            for (Task pred : t.getPredecessors()) {
                if (successorsMap.containsKey(pred.getId())) {
                    successorsMap.get(pred.getId()).add(t);
                }
            }
        }

        // 1. Forward Pass (Calculate ES and EF)
        Queue<Task> queue = new LinkedList<>();
        Map<Long, Integer> inDegree = new HashMap<>();

        for (Task t : tasks) {
            int degree = t.getPredecessors().size();
            inDegree.put(t.getId(), degree);
            if (degree == 0) {
                t.setEarlyStart(0);
                t.setEarlyFinish(t.getDuration());
                queue.add(t);
            }
        }

        List<Task> topologicalOrder = new ArrayList<>();

        while (!queue.isEmpty()) {
            Task current = queue.poll();
            topologicalOrder.add(current);

            for (Task succ : successorsMap.get(current.getId())) {
                succ.setEarlyStart(Math.max(succ.getEarlyStart(), current.getEarlyFinish()));
                succ.setEarlyFinish(succ.getEarlyStart() + succ.getDuration());

                int degree = inDegree.get(succ.getId()) - 1;
                inDegree.put(succ.getId(), degree);
                if (degree == 0) {
                    queue.add(succ);
                }
            }
        }

        // Find Project Duration (Max EF)
        int projectDuration = 0;
        for (Task t : tasks) {
            projectDuration = Math.max(projectDuration, t.getEarlyFinish());
        }

        // 2. Backward Pass (Calculate LS and LF) - Task T-20
        // Initialize terminal tasks
        for (Task t : tasks) {
            t.setLateFinish(projectDuration);
            t.setLateStart(projectDuration - t.getDuration());
        }

        // Traverse in reverse topological order
        for (int i = topologicalOrder.size() - 1; i >= 0; i--) {
            Task current = topologicalOrder.get(i);

            List<Task> successors = successorsMap.get(current.getId());
            if (successors != null && !successors.isEmpty()) {
                int minLS = Integer.MAX_VALUE;
                for (Task succ : successors) {
                    minLS = Math.min(minLS, succ.getLateStart());
                }
                current.setLateFinish(minLS);
                current.setLateStart(current.getLateFinish() - current.getDuration());
            }
        }

        // 3. Calculate Float and Critical Task - Task T-21
        for (Task t : tasks) {
            t.setTotalFloat(t.getLateFinish() - t.getEarlyFinish()); // LF - EF
            t.setIsCritical(t.getTotalFloat() == 0);
        }
    }
}
