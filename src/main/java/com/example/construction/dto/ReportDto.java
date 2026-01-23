package com.example.construction.dto;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class ReportDto {
    private Long id;
    private String comment;
    private List<String> photos;
    private String authorName;
}
