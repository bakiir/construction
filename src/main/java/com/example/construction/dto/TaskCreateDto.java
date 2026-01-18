package com.example.construction.dto;

import com.example.construction.Enums.TaskStatus;
import com.example.construction.Enums.TaskType;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.Set;

@Setter
@Getter
public class TaskCreateDto {

    private String title;

    private TaskType taskType;

    private Integer index;

    private LocalDate deadline;

    private TaskStatus status;

    // связь по ID
    private Long subObjectId;

    // пользователи по ID
    private Set<Long> assigneeIds;
}
