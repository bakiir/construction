package com.example.construction.service;

import com.example.construction.Enums.TaskStatus;
import com.example.construction.dto.TaskCreateDto;
import com.example.construction.dto.TaskDto;
import com.example.construction.dto.TaskApprovalDto;
import com.example.construction.mapper.TaskMapper;
import com.example.construction.model.SubObject;
import com.example.construction.model.Task;
import com.example.construction.dto.ChecklistItemDto;
import com.example.construction.model.User;
import com.example.construction.model.ChecklistItem;
import com.example.construction.reposirtories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final SubObjectRepository subObjectRepository;
    private final UserRepository userRepository;
    private final TaskApprovalRepository taskApprovalRepository;
    private final ChecklistItemRepository checklistItemRepository;
    private final TaskMapper mapper;
    private final NotificationService notificationService;
    private final FileStorageService fileStorageService;

    public TaskDto create(TaskCreateDto dto) {
        SubObject subObject = subObjectRepository.findById(dto.getSubObjectId())
                .orElseThrow(() -> new RuntimeException("SubObject not found"));

        Task task = new Task();
        task.setTitle(dto.getTitle());
        task.setTaskType(dto.getTaskType());
        task.setPriority(dto.getPriority());
        task.setDeadline(dto.getDeadline());
        task.setSubObject(subObject);

        // Auto-Index logic with Shifting
        if (dto.getIndex() == null) {
            long count = taskRepository.findBySubObjectId(subObject.getId()).size();
            task.setIndex((int) count);
        } else {
            // Shift existing tasks if necessary
            List<Task> existingTasks = taskRepository.findBySubObjectId(subObject.getId());
            boolean collision = existingTasks.stream()
                    .anyMatch(t -> t.getIndex() != null && t.getIndex().equals(dto.getIndex()));

            if (collision) {
                existingTasks.stream()
                        .filter(t -> t.getIndex() != null && t.getIndex() >= dto.getIndex())
                        .forEach(t -> t.setIndex(t.getIndex() + 1));
                taskRepository.saveAll(existingTasks);
            }
            task.setIndex(dto.getIndex());
        }

        // Pre-initialize mandatory fields to satisfy DB constraints before
        // recalculation
        task.setStatus(TaskStatus.LOCKED);
        if (task.getTaskType() == null) {
            task.setTaskType(com.example.construction.Enums.TaskType.SEQUENTIAL);
        }

        // 1. Initial save to get ID
        Task taskToSave = taskRepository.save(task);

        // 2. Centralized recalculation (includes the now-saved task)
        recalculateTaskStatuses(subObject.getId());

        // 3. Fetch refined task state
        Task savedTask = taskRepository.findById(taskToSave.getId()).orElse(taskToSave);

        // 4. Update Assignees if present (this is a simple field update)
        if (dto.getAssigneeIds() != null) {
            Set<User> assignees = new HashSet<>(userRepository.findAllById(dto.getAssigneeIds()));
            Set<User> subObjectWorkers = subObject.getWorkers();
            for (User assignee : assignees) {
                if (!subObjectWorkers.contains(assignee)) {
                    throw new IllegalArgumentException(
                            "Worker " + assignee.getFullName() + " is not assigned to this sub-object");
                }
            }
            savedTask.setAssignees(assignees);
            savedTask = taskRepository.save(savedTask);
        }

        // 5. Checklist processing
        if (dto.getChecklist() != null && !dto.getChecklist().isEmpty()) {
            for (int i = 0; i < dto.getChecklist().size(); i++) {
                com.example.construction.dto.ChecklistItemDto itemDto = dto.getChecklist().get(i);
                ChecklistItem item = new ChecklistItem();
                item.setTask(savedTask);
                item.setDescription(itemDto.getDescription());
                item.setIsPhotoRequired(itemDto.getIsPhotoRequired() != null ? itemDto.getIsPhotoRequired() : false);
                item.setOrderIndex(i);
                item.setIsCompleted(false);
                checklistItemRepository.save(item);
            }
        }

        // Final effectively final copy for notifications
        final Task taskForNotification = savedTask;

        // 6. Notifications
        com.example.construction.model.Project project = subObject.getConstructionObject().getProject();
        if (project != null && project.getStatus() == com.example.construction.Enums.ProjectStatus.PUBLISHED) {
            if (taskForNotification.getAssignees() != null) {
                String message = "Вам назначена новая задача: '" + taskForNotification.getTitle() + "'";
                taskForNotification.getAssignees()
                        .forEach(assignee -> notificationService.createNotification(assignee, message,
                                taskForNotification));
            }
        }

        return toDtoWithRejection(taskRepository.findById(taskForNotification.getId()).orElse(taskForNotification));
    }

    public TaskDto getById(Long id) {
        return taskRepository.findById(id)
                .map(this::toDtoWithRejection)
                .orElseThrow(() -> new RuntimeException("Task not found"));
    }

    public List<TaskDto> getAll() {
        return taskRepository.findAll()
                .stream()
                .map(this::toDtoWithRejection)
                .toList();
    }

    public List<TaskDto> getBySubObject(Long subObjectId) {
        return taskRepository.findBySubObjectId(subObjectId)
                .stream()
                .map(this::toDtoWithRejection)
                .toList();
    }

    public TaskDto update(Long id, TaskCreateDto dto) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        task.setTitle(dto.getTitle());
        task.setTaskType(dto.getTaskType());
        task.setPriority(dto.getPriority());
        task.setIndex(dto.getIndex());
        task.setDeadline(dto.getDeadline());
        // Status update usually happens via specific endpoints, but allowing here for
        // edit
        if (dto.getStatus() != null)
            task.setStatus(dto.getStatus());
        task.setUpdatedAt(LocalDateTime.now());

        if (dto.getAssigneeIds() != null) {
            Set<User> assignees = new HashSet<>(userRepository.findAllById(dto.getAssigneeIds()));

            // Validate: Assignees must be from sub-object workers
            Set<User> subObjectWorkers = task.getSubObject().getWorkers();
            for (User assignee : assignees) {
                if (!subObjectWorkers.contains(assignee)) {
                    throw new IllegalArgumentException(
                            "Worker " + assignee.getFullName() + " is not assigned to this sub-object");
                }
            }

            task.setAssignees(assignees);
        }

        // Checklist Synchronization
        if (dto.getChecklist() != null) {
            List<ChecklistItem> existingItems = checklistItemRepository.findByTaskIdOrderByOrderIndexAsc(id);
            Set<Long> dtoItemIds = dto.getChecklist().stream()
                    .map(ChecklistItemDto::getId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            // 1. Delete items not in DTO
            existingItems.stream()
                    .filter(item -> !dtoItemIds.contains(item.getId()))
                    .forEach(checklistItemRepository::delete);

            // 2. Add or Update items
            for (int i = 0; i < dto.getChecklist().size(); i++) {
                ChecklistItemDto itemDto = dto.getChecklist().get(i);
                ChecklistItem item;

                if (itemDto.getId() != null) {
                    item = checklistItemRepository.findById(itemDto.getId())
                            .orElse(new ChecklistItem());
                } else {
                    item = new ChecklistItem();
                    item.setTask(task);
                }

                item.setDescription(itemDto.getDescription());
                item.setIsPhotoRequired(itemDto.getIsPhotoRequired() != null ? itemDto.getIsPhotoRequired() : false);
                item.setOrderIndex(i);
                if (itemDto.getId() == null) {
                    item.setIsCompleted(false);
                } else if (itemDto.getIsCompleted() != null) {
                    item.setIsCompleted(itemDto.getIsCompleted());
                }

                checklistItemRepository.save(item);
            }
        }

        Task updatedTask = taskRepository.save(task);
        recalculateTaskStatuses(updatedTask.getSubObject().getId());
        return toDtoWithRejection(taskRepository.findById(updatedTask.getId()).orElse(updatedTask));
    }

    public void recalculateTaskStatuses(Long subObjectId) {
        List<Task> tasks = taskRepository.findBySubObjectId(subObjectId);
        tasks.sort(Comparator.comparing(t -> t.getIndex() != null ? t.getIndex() : Integer.MAX_VALUE));

        boolean allPreviousCompleted = true;
        for (Task t : tasks) {
            com.example.construction.Enums.TaskType type = t.getTaskType() != null
                    ? t.getTaskType()
                    : com.example.construction.Enums.TaskType.SEQUENTIAL;

            if (type == com.example.construction.Enums.TaskType.SEQUENTIAL) {
                if (allPreviousCompleted) {
                    if (t.getStatus() == TaskStatus.LOCKED) {
                        t.setStatus(TaskStatus.ACTIVE);
                    }
                } else {
                    t.setStatus(TaskStatus.LOCKED);
                }
            } else { // PARALLEL
                if (t.getStatus() == TaskStatus.LOCKED) {
                    t.setStatus(TaskStatus.ACTIVE);
                }
            }

            if (t.getStatus() != TaskStatus.COMPLETED) {
                allPreviousCompleted = false;
            }
        }
        taskRepository.saveAll(tasks);
    }

    public void delete(Long id) {
        taskRepository.findById(id).ifPresent(task -> {
            Long subObjectId = task.getSubObject().getId();
            taskRepository.delete(task);
            recalculateTaskStatuses(subObjectId);
        });
    }

    private TaskDto toDtoWithRejection(Task task) {
        TaskDto dto = mapper.toDto(task);
        if (task.getStatus() == TaskStatus.REWORK_FOREMAN
                || task.getStatus() == TaskStatus.REWORK_PM) {
            taskApprovalRepository.findTopByTaskAndDecisionOrderByCreatedAtDesc(task, "REJECTED")
                    .ifPresent(approval -> {
                        dto.setRejectionReason(approval.getComment());
                        if (approval.getApprover() != null) {
                            dto.setRejectedByFullName(approval.getApprover().getFullName());
                        }
                    });
        }

        // Populate foremanNote for PM review
        if (task.getStatus() == TaskStatus.UNDER_REVIEW_PM) {
            taskApprovalRepository.findTopByTaskAndDecisionAndRoleAtTimeOfApprovalOrderByCreatedAtDesc(
                    task, "APPROVED", com.example.construction.Enums.Role.FOREMAN)
                    .ifPresent(approval -> dto.setForemanNote(approval.getComment()));
        }

        // Populating Approvals History
        List<TaskApprovalDto> approvalDtos = taskApprovalRepository.findAllByTaskOrderByCreatedAtDesc(task)
                .stream()
                .map(approval -> {
                    TaskApprovalDto ad = new TaskApprovalDto();
                    ad.setId(approval.getId());
                    ad.setUserId(approval.getApprover().getId());
                    ad.setUserFullName(approval.getApprover().getFullName());
                    ad.setRoleAtTimeOfApproval(approval.getRoleAtTimeOfApproval());
                    ad.setDecision(approval.getDecision());
                    ad.setComment(approval.getComment());
                    ad.setCreatedAt(approval.getCreatedAt());
                    return ad;
                })
                .collect(Collectors.toList());
        dto.setApprovals(approvalDtos);

        return dto;
    }

    public TaskDto updateFinalPhoto(Long taskId, String photoUrl) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        String storedFileName = fileStorageService.storeBase64File(photoUrl);
        task.setFinalPhotoUrl(storedFileName);
        task.setUpdatedAt(LocalDateTime.now());
        taskRepository.save(task);

        return mapper.toDto(task);
    }
}
