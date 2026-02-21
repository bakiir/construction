package com.example.construction.controllers;

import com.example.construction.dto.ApprovalDto;
import com.example.construction.service.WorkflowService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class WorkflowController {

    private final WorkflowService workflowService;

    @PostMapping("/tasks/{taskId}/approve")
    @PreAuthorize("hasAnyRole('FOREMAN', 'PM', 'SUPER_ADMIN')")
    public ResponseEntity<Void> approveTask(
            @PathVariable Long taskId,
            @RequestBody(required = false) ApprovalDto approvalDto,
            Authentication authentication) {
        String approverPhone = authentication.getName();
        workflowService.approveTask(taskId, approverPhone, approvalDto);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/tasks/{taskId}/reject")
    @PreAuthorize("hasAnyRole('FOREMAN', 'PM', 'SUPER_ADMIN')")
    public ResponseEntity<Void> rejectTask(
            @PathVariable Long taskId,
            @RequestBody ApprovalDto approvalDto,
            Authentication authentication) {
        String approverPhone = authentication.getName();
        workflowService.rejectTask(taskId, approverPhone, approvalDto);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/tasks/{taskId}/submit")
    @PreAuthorize("hasAnyRole('WORKER', 'FOREMAN', 'PM', 'SUPER_ADMIN')")
    public ResponseEntity<Void> submitTask(
            @PathVariable Long taskId,
            @RequestBody(required = false) ApprovalDto approvalDto,
            Authentication authentication) {
        String submitterPhone = authentication != null ? authentication.getName() : null;
        workflowService.submitTaskForReview(taskId, approvalDto, submitterPhone);
        return ResponseEntity.ok().build();
    }
}
