package com.example.construction.dto;


import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ConstructionObjectCreateDto {
    private String name;
    private String address;
    private Long projectId;
}
