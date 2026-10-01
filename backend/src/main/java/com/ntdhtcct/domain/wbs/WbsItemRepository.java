package com.ntdhtcct.domain.wbs;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WbsItemRepository extends JpaRepository<WbsItem, UUID> {

    List<WbsItem> findByProjectIdOrderByWbsCodeAsc(UUID projectId);

    boolean existsByProjectIdAndWbsCode(UUID projectId, String wbsCode);

    List<WbsItem> findByParentId(UUID parentId);
}