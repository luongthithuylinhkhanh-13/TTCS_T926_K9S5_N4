package com.ntdhtcct.domain.wbs;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, UUID> {

    List<Task> findByCategoryId(UUID categoryId);

    List<Task> findByCategoryIdOrderByCreatedAtAsc(UUID categoryId);
}