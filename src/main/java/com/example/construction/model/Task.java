package com.example.construction.model;


import com.example.construction.Enums.TaskStatus;
import com.example.construction.Enums.TaskType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "c_tasks")
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskType taskType;

    private Integer index;

    private LocalDate deadline;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sub_obj_id", nullable = false)
    private SubObject subObject;

    @ManyToMany
    @JoinTable(
            name = "task_assignees",
            joinColumns = @JoinColumn(name = "task_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> assignees = new HashSet<>();

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = null;

}
