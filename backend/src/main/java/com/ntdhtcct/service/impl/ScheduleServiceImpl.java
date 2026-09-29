package com.ntdhtcct.service.impl;

import com.ntdhtcct.common.exception.BadRequestException;
import com.ntdhtcct.common.exception.ResourceNotFoundException;
import com.ntdhtcct.dto.*;
import com.ntdhtcct.entity.Project;
import com.ntdhtcct.entity.Task;
import com.ntdhtcct.entity.TaskDependency;
import com.ntdhtcct.repository.ProjectRepository;
import com.ntdhtcct.repository.TaskDependencyRepository;
import com.ntdhtcct.repository.TaskRepository;
import com.ntdhtcct.service.ScheduleService;
import com.ntdhtcct.service.cpm.CpmForwardPassCalculator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * T-18 & T-19 (NTDHTCT-144, NTDHTCT-145): Thực thi quản lý tiến độ và tính toán ES/EF.
 */
@Service
public class ScheduleServiceImpl implements ScheduleService {

    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final TaskDependencyRepository taskDependencyRepository;
    private final CpmForwardPassCalculator cpmCalculator = new CpmForwardPassCalculator();

    public ScheduleServiceImpl(ProjectRepository projectRepository,
                               TaskRepository taskRepository,
                               TaskDependencyRepository taskDependencyRepository) {
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
        this.taskDependencyRepository = taskDependencyRepository;
    }

    @Override
    @Transactional
    public TaskResponse createTask(Long projectId, CreateTaskRequest request) {
        Project project = getProjectOrThrow(projectId);

        if (taskRepository.existsByProjectIdAndCode(projectId, request.getCode().trim())) {
            throw new BadRequestException("TASK_CODE_EXISTS",
                    "Mã công việc [" + request.getCode() + "] đã tồn tại trong dự án [ID=" + projectId + "]");
        }

        Task task = new Task(project, request.getCode().trim(), request.getName().trim(), request.getDuration());
        task.setDescription(request.getDescription());

        Task saved = taskRepository.save(task);
        return TaskResponse.fromEntity(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskResponse> getTasks(Long projectId) {
        getProjectOrThrow(projectId);
        List<Task> tasks = taskRepository.findByProjectId(projectId);
        return tasks.stream().map(TaskResponse::fromEntity).toList();
    }

    @Override
    @Transactional
    public TaskDependencyResponse createDependency(Long projectId, CreateDependencyRequest request) {
        Project project = getProjectOrThrow(projectId);

        if (request.getPredecessorId().equals(request.getSuccessorId())) {
            throw new BadRequestException("SELF_DEPENDENCY_NOT_ALLOWED",
                    "Công việc không thể tự tạo liên kết phụ thuộc vào chính mình");
        }

        Task pred = taskRepository.findByIdAndProjectId(request.getPredecessorId(), projectId)
                .orElseThrow(() -> new ResourceNotFoundException("TASK_NOT_FOUND",
                        "Công việc tiền nhiệm [ID=" + request.getPredecessorId() + "] không tồn tại trong dự án"));

        Task succ = taskRepository.findByIdAndProjectId(request.getSuccessorId(), projectId)
                .orElseThrow(() -> new ResourceNotFoundException("TASK_NOT_FOUND",
                        "Công việc kế nhiệm [ID=" + request.getSuccessorId() + "] không tồn tại trong dự án"));

        if (taskDependencyRepository.existsByPredecessorIdAndSuccessorId(pred.getId(), succ.getId())) {
            throw new BadRequestException("DEPENDENCY_EXISTS",
                    "Mối quan hệ phụ thuộc giữa công việc [" + pred.getCode() + "] và [" + succ.getCode() + "] đã tồn tại");
        }

        TaskDependency dependency = new TaskDependency(project, pred, succ, request.getType(), request.getLag());
        TaskDependency saved = taskDependencyRepository.save(dependency);

        return TaskDependencyResponse.fromEntity(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskDependencyResponse> getDependencies(Long projectId) {
        getProjectOrThrow(projectId);
        List<TaskDependency> deps = taskDependencyRepository.findByProjectId(projectId);
        return deps.stream().map(TaskDependencyResponse::fromEntity).toList();
    }

    @Override
    @Transactional
    public ScheduleCalculationResponse calculateForwardPass(Long projectId, boolean saveToDb) {
        getProjectOrThrow(projectId);

        List<Task> tasks = taskRepository.findByProjectId(projectId);
        if (tasks.isEmpty()) {
            return new ScheduleCalculationResponse(projectId, 0.0, 0, List.of());
        }

        List<TaskDependency> dependencies = taskDependencyRepository.findByProjectId(projectId);

        // Chuyển đổi dữ liệu sang format đầu vào của thuật toán
        List<CpmForwardPassCalculator.TaskInput> taskInputs = tasks.stream()
                .map(t -> new CpmForwardPassCalculator.TaskInput(t.getId(), t.getCode(), t.getName(), t.getDuration()))
                .toList();

        List<CpmForwardPassCalculator.DependencyInput> depInputs = dependencies.stream()
                .map(d -> new CpmForwardPassCalculator.DependencyInput(
                        d.getId(),
                        d.getPredecessor().getId(),
                        d.getSuccessor().getId(),
                        d.getType(),
                        d.getLag() != null ? d.getLag() : 0.0
                ))
                .toList();

        // T-19: Chạy thuật toán Forward Pass
        CpmForwardPassCalculator.ForwardPassOutput output = cpmCalculator.calculate(taskInputs, depInputs);

        // Nếu saveToDb = true: cập nhật lại entity trong Database
        if (saveToDb) {
            Map<Long, CpmForwardPassCalculator.TaskScheduleResult> resultMap = output.taskResults();
            for (Task task : tasks) {
                CpmForwardPassCalculator.TaskScheduleResult res = resultMap.get(task.getId());
                if (res != null) {
                    task.setEarlyStart(res.earlyStart());
                    task.setEarlyFinish(res.earlyFinish());
                }
            }
            taskRepository.saveAll(tasks);
        }

        List<TaskScheduleDto> scheduleList = output.orderedResults().stream()
                .map(r -> new TaskScheduleDto(
                        r.taskId(),
                        r.code(),
                        r.name(),
                        r.duration(),
                        r.earlyStart(),
                        r.earlyFinish()
                ))
                .toList();

        return new ScheduleCalculationResponse(projectId, output.projectDuration(), tasks.size(), scheduleList);
    }

    @Override
    @Transactional(readOnly = true)
    public ScheduleCalculationResponse getSchedule(Long projectId) {
        getProjectOrThrow(projectId);
        List<Task> tasks = taskRepository.findByProjectId(projectId);
        if (tasks.isEmpty()) {
            return new ScheduleCalculationResponse(projectId, 0.0, 0, List.of());
        }

        double maxEf = 0.0;
        List<TaskScheduleDto> dtos = new ArrayList<>();
        for (Task t : tasks) {
            if (t.getEarlyFinish() != null && t.getEarlyFinish() > maxEf) {
                maxEf = t.getEarlyFinish();
            }
            dtos.add(new TaskScheduleDto(
                    t.getId(),
                    t.getCode(),
                    t.getName(),
                    t.getDuration(),
                    t.getEarlyStart(),
                    t.getEarlyFinish()
            ));
        }

        return new ScheduleCalculationResponse(projectId, maxEf, tasks.size(), dtos);
    }

    private Project getProjectOrThrow(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("PROJECT_NOT_FOUND",
                        "Dự án không tồn tại [ID=" + projectId + "]"));
    }
}
