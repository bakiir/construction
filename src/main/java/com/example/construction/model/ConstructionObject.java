package com.example.construction.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "c_objects")
public class ConstructionObject {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String address;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    @JsonBackReference
    private Project project;

    // Lead Foreman for this construction object (optional)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lead_foreman_id")
    private User leadForeman;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = null;

}
