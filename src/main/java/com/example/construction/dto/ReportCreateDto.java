package com.example.construction.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ReportCreateDto {
    private String comment;
    private List<ReportChecklistAnswerDto> checklistAnswers;
}
