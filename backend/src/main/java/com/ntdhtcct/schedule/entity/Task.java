package com.ntdhtcct.schedule.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tasks")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Integer duration;

    // Computed fields for CPM (Critical Path Method)
    @Column(name = "early_start")
    private Integer earlyStart = 0;

    @Column(name = "early_finish")
    private Integer earlyFinish = 0;

    @Column(name = "late_start")
    private Integer lateStart = 0;

    @Column(name = "late_finish")
    private Integer lateFinish = 0;

    @Column(name = "total_float")
    private Integer totalFloat = 0;

    @Column(name = "is_critical")
    private Boolean isCritical = false;

    // Dependencies (Predecessors)
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "task_dependencies",
        joinColumns = @JoinColumn(name = "task_id"),
        inverseJoinColumns = @JoinColumn(name = "predecessor_id")
    )
    private List<Task> predecessors = new ArrayList<>();
}
