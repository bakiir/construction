package com.example.construction.dto;

import lombok.Data;

@Data
public class ChecklistItemDto {
    private Long id;
    private String description;
    private Integer orderIndex;
    private Boolean isCompleted;
    private String photoUrl;
}
