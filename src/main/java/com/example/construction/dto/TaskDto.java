package com.example.construction.dto;

import com.example.construction.Enums.TaskStatus;
import com.example.construction.Enums.TaskType;
import com.example.construction.Enums.Priority;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Setter
@Getter
public class TaskDto {

    private Long id;
    private String title;
    private TaskType taskType;
    private Integer index;
    private LocalDate deadline;
    private TaskStatus status;
    private Priority priority;

    private Long subObjectId;
    private String subObjectName;
    private String objectName;
    private String objectAddress;
    private String projectName;
    private Set<Long> assigneeIds;
    private List<ChecklistItemDto> checklist;
    private String finalPhotoUrl;
    private ReportDto report;
    private String rejectionReason;
    private String rejectedByFullName;
    private String foremanNote;
}
