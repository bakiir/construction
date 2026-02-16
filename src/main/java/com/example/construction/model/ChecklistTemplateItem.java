package com.example.construction.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "checklist_template_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChecklistTemplateItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    @JsonIgnore
    private ChecklistTemplate template;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(name = "is_photo_required", nullable = false)
    private Boolean isPhotoRequired = false;

    @Column(name = "order_index")
    private Integer orderIndex;

    @Column(columnDefinition = "TEXT")
    private String methodology;
}
