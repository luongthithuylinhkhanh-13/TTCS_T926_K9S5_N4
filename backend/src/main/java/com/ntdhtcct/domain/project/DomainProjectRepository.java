package com.ntdhtcct.domain.project;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DomainProjectRepository extends JpaRepository<Project, UUID> {

    Optional<Project> findByCode(String code);

    boolean existsByCode(String code);
}