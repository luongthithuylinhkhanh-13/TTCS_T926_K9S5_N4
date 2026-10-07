package com.ntdhtcct.controller;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import com.ntdhtcct.domain.TaskDependency;
import com.ntdhtcct.dto.CreateDependencyRequest;
import com.ntdhtcct.service.TaskDependencyService;

import jakarta.validation.Valid;

@RequestMapping("/api/tasks/legacy")
@CrossOrigin(origins = "*")
public class TaskDependencyController {

    private final TaskDependencyService dependencyService;

    public TaskDependencyController(TaskDependencyService dependencyService) {
        this.dependencyService = dependencyService;
    }

    @PostMapping("/{taskId}/dependencies")
    public ResponseEntity<?> addDependency(
            @PathVariable UUID taskId,
            @Valid @RequestBody CreateDependencyRequest request) {
        try {
            TaskDependency saved = dependencyService.addDependency(taskId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @GetMapping("/{taskId}/dependencies")
    public ResponseEntity<List<TaskDependency>> getDependencies(@PathVariable UUID taskId) {
        return ResponseEntity.ok(dependencyService.getDependencies(taskId));
    }

    @DeleteMapping("/dependencies/{dependencyId}")
    public ResponseEntity<?> deleteDependency(@PathVariable Long dependencyId) {
        try {
            dependencyService.deleteDependency(dependencyId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }
}