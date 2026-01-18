package com.example.construction.dto;

import com.example.construction.model.Object;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ProjectDto {
    private Long id;
    private String name;
    private String description;
    private Long createdBy;
    private Long progress;
    private LocalDate deadline;
    private List<Object> objects; // Можно заменить Object на конкретный DTO, если есть
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
