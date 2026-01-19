package com.example.construction.model;


import com.example.construction.Enums.Role;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "c_users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    private String fullName;

    @Enumerated(EnumType.STRING)
    private Role role;

    private boolean isActive = true;

    @ManyToMany(mappedBy = "assignees")
    private Set<Task> tasks = new HashSet<>();

    @OneToMany(mappedBy = "author")
    private List<Report> reports = new ArrayList<>();

}
