package com.example.construction.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "c_sub_objects")
public class SubObject {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "object_id", nullable = false)
    private ConstructionObject constructionObject;

    private String name;

    @OneToMany(mappedBy = "subObject", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Task> tasks;

    // Workers assigned to this sub-object
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "sub_object_workers", joinColumns = @JoinColumn(name = "sub_object_id"), inverseJoinColumns = @JoinColumn(name = "user_id"))
    private Set<User> workers = new HashSet<>();

}
