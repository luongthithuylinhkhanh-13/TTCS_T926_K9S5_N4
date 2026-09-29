package com.ntdhtcct.service;

import com.ntdhtcct.domain.DependencyType;
import com.ntdhtcct.domain.TaskDependency;
import com.ntdhtcct.dto.CreateDependencyRequest;
import com.ntdhtcct.repository.TaskDependencyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskDependencyServiceTest {

    @Mock
    private TaskDependencyRepository dependencyRepository;

    @InjectMocks
    private TaskDependencyService dependencyService;

    private UUID taskA;
    private UUID taskB;
    private UUID taskC;
    private CreateDependencyRequest validRequest;

    @BeforeEach
    void setUp() {
        taskA = UUID.randomUUID();
        taskB = UUID.randomUUID();
        taskC = UUID.randomUUID();
        validRequest = new CreateDependencyRequest(taskA, DependencyType.FS, -2);
    }

    @Test
    @DisplayName("Tạo quan hệ thành công với lag âm (Lead time)")
    void shouldCreateDependencyWithNegativeLag() {
        when(dependencyRepository.existsByPredecessorIdAndSuccessorId(taskA, taskB)).thenReturn(false);
        when(dependencyRepository.findByPredecessorId(taskB)).thenReturn(Collections.emptyList());
        when(dependencyRepository.save(any(TaskDependency.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TaskDependency result = dependencyService.addDependency(taskB, validRequest);

        assertNotNull(result);
        assertEquals(taskA, result.getPredecessorId());
        assertEquals(taskB, result.getSuccessorId());
        assertEquals(DependencyType.FS, result.getDependencyType());
        assertEquals(-2, result.getLagDays());
        verify(dependencyRepository, times(1)).save(any(TaskDependency.class));
    }

    @Test
    @DisplayName("Ném lỗi khi một công việc tự phụ thuộc vào chính nó")
    void shouldThrowExceptionWhenSelfDependency() {
        CreateDependencyRequest selfRequest = new CreateDependencyRequest(taskA, DependencyType.FS, 0);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                dependencyService.addDependency(taskA, selfRequest)
        );

        assertEquals("Một công việc không thể tự phụ thuộc vào chính nó.", ex.getMessage());
        verify(dependencyRepository, never()).save(any());
    }

    @Test
    @DisplayName("Ném lỗi khi quan hệ giữa 2 công việc đã tồn tại")
    void shouldThrowExceptionWhenDuplicateDependency() {
        when(dependencyRepository.existsByPredecessorIdAndSuccessorId(taskA, taskB)).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                dependencyService.addDependency(taskB, validRequest)
        );

        assertEquals("Quan hệ phụ thuộc giữa 2 công việc này đã tồn tại.", ex.getMessage());
        verify(dependencyRepository, never()).save(any());
    }

    @Test
    @DisplayName("Phát hiện chu trình trực tiếp 2 node: A -> B, cố gắng thêm B -> A")
    void shouldDetectDirectCycle() {
        TaskDependency existingEdge = new TaskDependency(taskB, taskA, DependencyType.FS, 0);
        when(dependencyRepository.existsByPredecessorIdAndSuccessorId(taskA, taskB)).thenReturn(false);
        when(dependencyRepository.findByPredecessorId(taskB)).thenReturn(List.of(existingEdge));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                dependencyService.addDependency(taskB, validRequest)
        );

        assertTrue(ex.getMessage().contains("vòng lặp tuần hoàn"));
        verify(dependencyRepository, never()).save(any());
    }

    @Test
    @DisplayName("Phát hiện chu trình gián tiếp 3 node: A -> B -> C, cố gắng thêm C -> A")
    void shouldDetectIndirectCycle() {
        CreateDependencyRequest cycleRequest = new CreateDependencyRequest(taskC, DependencyType.FS, 0);

        TaskDependency edgeAtoB = new TaskDependency(taskA, taskB, DependencyType.FS, 0);
        TaskDependency edgeBtoC = new TaskDependency(taskB, taskC, DependencyType.FS, 0);

        when(dependencyRepository.existsByPredecessorIdAndSuccessorId(taskC, taskA)).thenReturn(false);
        when(dependencyRepository.findByPredecessorId(taskA)).thenReturn(List.of(edgeAtoB));
        when(dependencyRepository.findByPredecessorId(taskB)).thenReturn(List.of(edgeBtoC));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                dependencyService.addDependency(taskA, cycleRequest)
        );

        assertTrue(ex.getMessage().contains("vòng lặp tuần hoàn"));
        verify(dependencyRepository, never()).save(any());
    }
}