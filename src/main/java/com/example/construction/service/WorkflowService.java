package com.example.construction.service;

import com.example.construction.Enums.Role;
import com.example.construction.Enums.TaskStatus;
import com.example.construction.Enums.TaskType;
import com.example.construction.dto.ApprovalDto;
import com.example.construction.model.Task;
import com.example.construction.model.TaskApproval;
import com.example.construction.model.User;
import com.example.construction.reposirtories.TaskApprovalRepository;
import com.example.construction.reposirtories.TaskRepository;
import com.example.construction.reposirtories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WorkflowService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final TaskApprovalRepository taskApprovalRepository;
    private final NotificationService notificationService;

    @Transactional
    public void submitTaskForReview(Long taskId) {
        Task task = findTaskById(taskId);
        if (task.getStatus() != TaskStatus.ACTIVE && task.getStatus() != TaskStatus.REWORK) {
            throw new IllegalStateException("Task must be in ACTIVE or REWORK status to be submitted for review.");
        }
        task.setStatus(TaskStatus.UNDER_REVIEW_FOREMAN);
        taskRepository.save(task);

        // Notify all foremen
        List<User> foremen = userRepository.findAllByRole(Role.FOREMAN);
        foremen.forEach(foreman -> notificationService.createNotification(foreman,
                "Task '" + task.getTitle() + "' is ready for review."));
    }

    @Transactional
    public void approveTask(Long taskId, String approverEmail) {
        Task task = findTaskById(taskId);
        User approver = findUserByEmail(approverEmail);

        TaskStatus currentStatus = task.getStatus();
        Role approverRole = approver.getRole();

        if (approverRole == Role.FOREMAN && currentStatus == TaskStatus.UNDER_REVIEW_FOREMAN) {
            task.setStatus(TaskStatus.UNDER_REVIEW_PM);
            createApprovalRecord(task, approver, "APPROVED", null);

            // Notify all PMs
            List<User> pms = userRepository.findAllByRole(Role.PM);
            pms.forEach(pm -> notificationService.createNotification(pm,
                    "Task '" + task.getTitle() + "' has been approved by foreman and is ready for final review."));

        } else if ((approverRole == Role.PM || approverRole == Role.SUPER_ADMIN)
                && currentStatus == TaskStatus.UNDER_REVIEW_PM) {
            task.setStatus(TaskStatus.COMPLETED);
            createApprovalRecord(task, approver, "APPROVED", null);

            // Notify assignees
            task.getAssignees().forEach(assignee -> notificationService.createNotification(assignee,
                    "Task '" + task.getTitle() + "' has been completed."));

            activateNextTask(task);
        } else {
            throw new IllegalStateException(
                    "User " + approverEmail + " cannot approve this task at its current status.");
        }

        taskRepository.save(task);
    }

    @Transactional
    public void rejectTask(Long taskId, String approverEmail, ApprovalDto approvalDto) {
        Task task = findTaskById(taskId);
        User approver = findUserByEmail(approverEmail);

        TaskStatus currentStatus = task.getStatus();
        Role approverRole = approver.getRole();

        if (approverRole == Role.FOREMAN && currentStatus == TaskStatus.UNDER_REVIEW_FOREMAN) {
            // Foreman rejects -> Back to Worker (REWORK)
            task.setStatus(TaskStatus.REWORK);
            createApprovalRecord(task, approver, "REJECTED", approvalDto.getComment());

            // Notify assignees (Workers)
            String message = "Task '" + task.getTitle() + "' was sent for rework by Foreman. Comment: "
                    + approvalDto.getComment();
            task.getAssignees().forEach(assignee -> notificationService.createNotification(assignee, message));

        } else if ((approverRole == Role.PM || approverRole == Role.SUPER_ADMIN)
                && currentStatus == TaskStatus.UNDER_REVIEW_PM) {
            // PM rejects -> Back to Foreman (UNDER_REVIEW_FOREMAN)
            task.setStatus(TaskStatus.UNDER_REVIEW_FOREMAN);
            createApprovalRecord(task, approver, "REJECTED", approvalDto.getComment());

            // Notify all Foremen
            String message = "Task '" + task.getTitle() + "' was returned to Foreman by PM. Comment: "
                    + approvalDto.getComment();
            List<User> foremen = userRepository.findAllByRole(Role.FOREMAN);
            foremen.forEach(foreman -> notificationService.createNotification(foreman, message));

        } else {
            throw new IllegalStateException(
                    "User " + approverEmail + " cannot reject this task at its current status.");
        }

        taskRepository.save(task);
    }

    private void activateNextTask(Task completedTask) {
        if (completedTask.getTaskType() != TaskType.SEQUENTIAL) {
            return;
        }

        Integer currentTaskIndex = completedTask.getIndex();
        if (currentTaskIndex == null) {
            return;
        }

        Long subObjectId = completedTask.getSubObject().getId();
        taskRepository.findBySubObjectIdAndIndex(subObjectId, currentTaskIndex + 1)
                .ifPresent(nextTask -> {
                    if (nextTask.getStatus() == TaskStatus.LOCKED) {
                        nextTask.setStatus(TaskStatus.ACTIVE);
                        taskRepository.save(nextTask);
                    }
                });
    }

    private void createApprovalRecord(Task task, User approver, String decision, String comment) {
        TaskApproval approval = new TaskApproval();
        approval.setTask(task);
        approval.setApprover(approver);
        approval.setRoleAtTimeOfApproval(approver.getRole());
        approval.setDecision(decision);
        approval.setComment(comment);
        taskApprovalRepository.save(approval);
    }

    private Task findTaskById(Long taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found with id: " + taskId));
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
    }
}
