package com.example.construction.service;

import com.example.construction.Enums.Role;
import com.example.construction.Enums.TaskStatus;
import com.example.construction.dto.ApprovalDto;
import com.example.construction.model.Project;
import com.example.construction.model.Task;
import com.example.construction.model.TaskApproval;
import com.example.construction.model.User;
import com.example.construction.reposirtories.TaskApprovalRepository;
import com.example.construction.reposirtories.TaskRepository;
import com.example.construction.reposirtories.UserRepository;
import com.example.construction.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class WorkflowService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final TaskApprovalRepository taskApprovalRepository;
    private final NotificationService notificationService;
    private final ChecklistService checklistService;
    private final com.example.construction.reposirtories.ReportRepository reportRepository;
    private final TaskService taskService;

    @Transactional
    public void submitTaskForReview(Long taskId, ApprovalDto submissionDto, String submitterPhone) {
        Task task = findTaskById(taskId);

        if (task.getStatus() != TaskStatus.ACTIVE
                && task.getStatus() != TaskStatus.REWORK_FOREMAN
                && task.getStatus() != TaskStatus.REWORK_PM) {
            throw new IllegalStateException(
                    "Task must be in ACTIVE, REWORK_FOREMAN, or REWORK_PM status to be submitted for review.");
        }

        // Validate submitter is assigned to the task
        // Validate submitter permissions
        if (submitterPhone != null) {
            User submitter = findUserByPhone(submitterPhone);
            if (submitter != null) {
                if (submitter.getRole() == com.example.construction.Enums.Role.WORKER) {
                    if (!task.getAssignees().contains(submitter)) {
                        throw new IllegalStateException(
                                "Вы не назначены на эту задачу и не можете отправить её на проверку.");
                    }
                } else if (submitter.getRole() == com.example.construction.Enums.Role.FOREMAN) {
                    // Foreman can submit if assigned OR if they are a foreman on the project
                    boolean isAssigned = task.getAssignees().contains(submitter);
                    boolean isProjectForeman = false;
                    if (task.getSubObject().getConstructionObject().getProject().getForemen().contains(submitter)) {
                        isProjectForeman = true;
                    }
                    if (!isAssigned && !isProjectForeman) {
                        throw new IllegalStateException(
                                "Вы не являетесь исполнителем или прорабом проекта для этой задачи.");
                    }
                }
            }
        }

        // Validate checklists
        boolean isWorkerSubmit = false;
        if (submitterPhone != null) {
            User submitter = findUserByPhone(submitterPhone);
            if (submitter != null && submitter.getRole() == com.example.construction.Enums.Role.WORKER) {
                isWorkerSubmit = true;
            }
        }

        if (isWorkerSubmit) {
            // Worker: Only check if checkboxes are ticked (photos optional at this stage)
            if (!checklistService.areAllChecklistsChecked(taskId)) {
                throw new IllegalStateException("Not all checklist items are marked as completed.");
            }
            // Worker: Final photo is optional at this stage (foreman can add it later)
        } else {
            // Foreman/PM: Strict check (checkboxes + photos)
            if (!checklistService.areAllChecklistsCompleted(taskId)) {
                throw new IllegalStateException("Not all checklist items are completed or photos are missing.");
            }
            // Foreman/PM: Final photo is mandatory
            if (task.getFinalPhotoUrl() == null) {
                throw new IllegalStateException("Final photo is mandatory for submission.");
            }
        }

        if (isWorkerSubmit) {
            task.setStatus(TaskStatus.UNDER_REVIEW_FOREMAN);
        } else {
            // Foreman submitting -> Goes directly to PM
            task.setStatus(TaskStatus.UNDER_REVIEW_PM);
        }

        // Handle submission comment via Report
        if (submissionDto != null && submissionDto.getComment() != null) {
            com.example.construction.model.Report report = task.getReport();
            if (report == null) {
                report = new com.example.construction.model.Report();
                report.setTask(task);

                // Set author from submitterPhone if available, otherwise fallback to assignee
                if (submitterPhone != null) {
                    report.setAuthor(findUserByPhone(submitterPhone));
                } else if (!task.getAssignees().isEmpty()) {
                    report.setAuthor(task.getAssignees().iterator().next());
                } else {
                    // This should ideally not happen if security is enabled
                    throw new IllegalStateException("Cannot create report: no author identified.");
                }
            }
            report.setComment(submissionDto.getComment());
            reportRepository.save(report);
        }

        taskRepository.save(task);

        if (isWorkerSubmit) {
            // Notify project foremen
            Project project = task.getSubObject().getConstructionObject().getProject();
            if (project != null) {
                Set<User> foremen = project.getForemen();
                if (foremen != null) {
                    foremen.forEach(foreman -> notificationService.createNotification(foreman,
                            "Задача '" + task.getTitle() + "' готова к проверке.", task));
                }
            }
        } else {
            // Notify PMs
            List<User> pms = userRepository.findAllByRole(Role.PM);
            pms.forEach(pm -> notificationService.createNotification(pm,
                    "Прораб отправил задачу '" + task.getTitle() + "' на финальную проверку.",
                    task));
        }
    }

    @Transactional
    public void approveTask(Long taskId, String approverPhone, ApprovalDto approvalDto) {
        Task task = findTaskById(taskId);
        User approver = findUserByPhone(approverPhone);

        TaskStatus currentStatus = task.getStatus();
        Role approverRole = approver.getRole();
        String comment = (approvalDto != null) ? approvalDto.getComment() : null;

        if (approverRole == Role.FOREMAN
                && currentStatus == TaskStatus.UNDER_REVIEW_FOREMAN) {
            // Foreman is approving -> Handing over to PM
            // MUST ensure all photos are present now
            if (!checklistService.areAllChecklistsCompleted(taskId)) {
                throw new IllegalStateException("Cannot approve: Not all checklist items have required photos.");
            }
            if (task.getFinalPhotoUrl() == null) {
                throw new IllegalStateException("Cannot approve: Final photo is missing.");
            }

            task.setStatus(TaskStatus.UNDER_REVIEW_PM);
            createApprovalRecord(task, approver, "APPROVED", comment);

            // Notify all PMs
            List<User> pms = userRepository.findAllByRole(Role.PM);
            pms.forEach(pm -> notificationService.createNotification(pm,
                    "Задача '" + task.getTitle() + "' принята прорабом и ожидает финальной проверки.",
                    task));

        } else if ((approverRole == Role.PM || approverRole == Role.SUPER_ADMIN)
                && currentStatus == TaskStatus.UNDER_REVIEW_PM) {
            task.setStatus(TaskStatus.COMPLETED);
            createApprovalRecord(task, approver, "APPROVED", comment);

            // Notify assignees
            task.getAssignees().forEach(assignee -> notificationService.createNotification(assignee,
                    "Задача '" + task.getTitle() + "' завершена.", task));

            taskService.recalculateTaskStatuses(task.getSubObject().getId());
        } else {
            throw new IllegalStateException(
                    "User " + approverPhone + " cannot approve this task at its current status.");
        }

        taskRepository.save(task);
    }

    @Transactional
    public void rejectTask(Long taskId, String approverPhone, ApprovalDto approvalDto) {
        Task task = findTaskById(taskId);
        User approver = findUserByPhone(approverPhone);

        // Validate that rejection comment is provided
        if (approvalDto == null || approvalDto.getComment() == null || approvalDto.getComment().trim().isEmpty()) {
            throw new IllegalArgumentException("Rejection comment is required when rejecting a task.");
        }

        TaskStatus currentStatus = task.getStatus();
        Role approverRole = approver.getRole();

        if (approverRole == Role.FOREMAN
                && currentStatus == TaskStatus.UNDER_REVIEW_FOREMAN) {
            // Foreman rejects -> Back to Worker (REWORK_FOREMAN)
            task.setStatus(TaskStatus.REWORK_FOREMAN);
            createApprovalRecord(task, approver, "REJECTED", approvalDto.getComment());

            // Notify assignees (Workers)
            String message = "Задача '" + task.getTitle() + "' отправлена на доработку прорабом. Комментарий: "
                    + approvalDto.getComment();
            task.getAssignees().forEach(assignee -> notificationService.createNotification(assignee, message, task));

        } else if ((approverRole == Role.PM || approverRole == Role.SUPER_ADMIN)
                && currentStatus == TaskStatus.UNDER_REVIEW_PM) {
            // PM rejects -> Goes directly to Worker with REWORK_PM status
            // Worker edits and resubmits, Foreman only observes
            task.setStatus(TaskStatus.REWORK_PM);
            createApprovalRecord(task, approver, "REJECTED", approvalDto.getComment());

            // Notify assignees (Workers) - they need to fix
            String workerMessage = "Задача '" + task.getTitle()
                    + "' возвращена менеджером на доработку. Комментарий: "
                    + approvalDto.getComment();
            task.getAssignees()
                    .forEach(assignee -> notificationService.createNotification(assignee, workerMessage, task));

            // Notify Project Foremen - just for their awareness
            String foremanMessage = "Задача '" + task.getTitle()
                    + "' возвращена менеджером работнику на доработку. Комментарий: "
                    + approvalDto.getComment();

            Project project = task.getSubObject().getConstructionObject().getProject();
            if (project != null) {
                Set<User> foremen = project.getForemen();
                if (foremen != null) {
                    foremen.forEach(foreman -> notificationService.createNotification(foreman, foremanMessage, task));
                }
            }

        } else {
            throw new IllegalStateException(
                    "User " + approverPhone + " cannot reject this task at its current status.");
        }

        taskRepository.save(task);
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

    private User findUserByPhone(String phone) {
        return userRepository.findByPhone(phone)
                .orElseThrow(() -> new RuntimeException("User not found with phone: " + phone));
    }
}
