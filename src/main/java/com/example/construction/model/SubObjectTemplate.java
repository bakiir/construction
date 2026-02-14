package com.example.construction.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sub_object_templates")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubObjectTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @OneToMany(mappedBy = "subObjectTemplate", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TaskTemplate> taskTemplates = new ArrayList<>();
}
