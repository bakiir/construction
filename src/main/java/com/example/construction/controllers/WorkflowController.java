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
    @PreAuthorize("hasAnyRole('FOREMAN', 'PM')")
    public ResponseEntity<Void> approveTask(@PathVariable Long taskId, Authentication authentication) {
        String approverEmail = authentication.getName();
        workflowService.approveTask(taskId, approverEmail);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/tasks/{taskId}/reject")
    @PreAuthorize("hasAnyRole('FOREMAN', 'PM')")
    public ResponseEntity<Void> rejectTask(
            @PathVariable Long taskId,
            @RequestBody ApprovalDto approvalDto,
            Authentication authentication) {
        String approverEmail = authentication.getName();
        workflowService.rejectTask(taskId, approverEmail, approvalDto);
        return ResponseEntity.ok().build();
    }
}
