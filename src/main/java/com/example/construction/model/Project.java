package com.example.construction.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "c_projects")
public class Project {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String description;

    private Long createdBy;

    private Long progress;

    private LocalDate deadline;

    @OneToMany(mappedBy = "project", fetch = FetchType.LAZY)
    private List<Object> objects;

    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt = null;

}
