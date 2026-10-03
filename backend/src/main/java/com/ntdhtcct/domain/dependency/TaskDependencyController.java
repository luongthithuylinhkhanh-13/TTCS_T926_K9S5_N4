package com.ntdhtcct.domain.dependency;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tasks")
public class TaskDependencyController {

    private final TaskDependencyService service;

    public TaskDependencyController(TaskDependencyService service) {
        this.service = service;
    }

    @GetMapping("/{taskId}/dependencies")
    public ResponseEntity<List<TaskDependencyResponse>> getTaskDependencies(@PathVariable UUID taskId) {
        return ResponseEntity.ok(service.getDependenciesByTask(taskId));
    }

    @PostMapping("/{taskId}/dependencies")
    public ResponseEntity<TaskDependencyResponse> addTaskDependency(
            @PathVariable UUID taskId,
            @Valid @RequestBody CreateDependencyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.addDependency(taskId, request));
    }

    @DeleteMapping("/dependencies/{dependencyId}")
    public ResponseEntity<Void> deleteTaskDependency(@PathVariable UUID dependencyId) {
        service.deleteDependency(dependencyId);
        return ResponseEntity.noContent().build();
    }
}