package com.example.construction.dto;

import com.example.construction.Enums.Role;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class TaskApprovalDto {
    private Long id;
    private Long userId;
    private String userFullName;
    private Role roleAtTimeOfApproval;
    private String decision;
    private String comment;
    private LocalDateTime createdAt;
}
