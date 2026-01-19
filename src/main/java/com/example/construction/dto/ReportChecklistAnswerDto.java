package com.example.construction.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReportChecklistAnswerDto {
    private Long checklistItemId;
    private boolean completed;
}
