package com.example.construction.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ConstructionObjectDto {
    private Long id;
    private String name;
    private String address;
    private Long projectId;
}

