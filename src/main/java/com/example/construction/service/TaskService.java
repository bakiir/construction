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
import com.example.construction.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    private final ChecklistTemplateRepository checklistTemplateRepository;
    private final TaskMapper mapper;
    private final NotificationService notificationService;
    private final FileStorageService fileStorageService;

    @org.springframework.transaction.annotation.Transactional
    public Task createTaskFromTemplate(Task task) {
        // Initial save to get ID
        Task savedTask = taskRepository.save(task);

        // Auto-Index logic: appends to the end
        List<Task> existingTasks = taskRepository.findBySubObjectId(task.getSubObject().getId());

        // Filter out the current task if it's already in the list
        long count = existingTasks.stream().filter(t -> !t.getId().equals(savedTask.getId())).count();
        savedTask.setIndex((int) count);

        // Set default status
        savedTask.setStatus(TaskStatus.LOCKED); // Will be updated by recalculate
        if (savedTask.getTaskType() == null) {
            savedTask.setTaskType(com.example.construction.Enums.TaskType.SEQUENTIAL);
        }

        Task finalSavedTask = taskRepository.save(savedTask);
        recalculateTaskStatuses(finalSavedTask.getSubObject().getId());

        return finalSavedTask;
    }

    @org.springframework.transaction.annotation.Transactional
    public TaskDto create(TaskCreateDto dto) {
        SubObject subObject = subObjectRepository.findById(dto.getSubObjectId())
                .orElseThrow(() -> new RuntimeException("SubObject not found"));

        Task task = new Task();
        task.setTitle(dto.getTitle());
        task.setTaskType(dto.getTaskType());
        task.setPriority(dto.getPriority());
        task.setDeadline(dto.getDeadline());
        task.setDeadline(dto.getDeadline());
        task.setSubObject(subObject);

        if (dto.getTemplateId() != null) {
            checklistTemplateRepository.findById(dto.getTemplateId())
                    .ifPresent(task::setTemplate);
        }

        // Auto-Index logic with Shifting
        if (dto.getIndex() == null) {
            List<Task> existingTasks = taskRepository.findBySubObjectId(subObject.getId());

            if (dto.getTaskType() == com.example.construction.Enums.TaskType.PARALLEL && !existingTasks.isEmpty()) {
                // Find the last task by index
                Task lastTask = existingTasks.stream()
                        .max(Comparator.comparing(t -> t.getIndex() != null ? t.getIndex() : -1))
                        .orElse(null);

                // If last task is also PARALLEL, use the same index (join the queue)
                if (lastTask != null &&
                        lastTask.getTaskType() == com.example.construction.Enums.TaskType.PARALLEL) {
                    task.setIndex(lastTask.getIndex());
                } else {
                    // Otherwise, assign next index
                    task.setIndex(existingTasks.size());
                }
            } else {
                // Sequential or first task: assign next index
                task.setIndex(existingTasks.size());
            }
        } else {
            // Smart Shift: only shift if NOT joining an existing parallel group
            if (!shouldJoinParallelGroup(subObject.getId(), dto.getIndex(), dto.getTaskType())) {
                shiftIndices(subObject.getId(), dto.getIndex(), 1, null);
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

            // Validate parallel worker assignment
            if (savedTask.getTaskType() == com.example.construction.Enums.TaskType.PARALLEL
                    && savedTask.getIndex() != null) {
                Set<Long> assigneeIds = assignees.stream().map(User::getId).collect(Collectors.toSet());
                validateParallelWorkerAssignment(subObject.getId(), savedTask.getIndex(), assigneeIds, null);
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
                item.setMethodology(itemDto.getMethodology());
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

    public List<TaskDto> getBySubObjectForUser(Long subObjectId, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return taskRepository.findBySubObjectId(subObjectId)
                .stream()
                .filter(task -> task.getAssignees().contains(user))
                .map(this::toDtoWithRejection)
                .toList();
    }

    @org.springframework.transaction.annotation.Transactional
    public TaskDto update(Long id, TaskCreateDto dto) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        validateTaskAccess(task);

        task.setTitle(dto.getTitle());
        task.setTaskType(dto.getTaskType());
        task.setPriority(dto.getPriority());

        Long oldSubObjectId = task.getSubObject().getId();
        Long newSubObjectId = dto.getSubObjectId();

        if (newSubObjectId != null && !newSubObjectId.equals(oldSubObjectId)) {
            SubObject newSubObject = subObjectRepository.findById(newSubObjectId)
                    .orElseThrow(() -> new RuntimeException("New SubObject not found"));
            task.setSubObject(newSubObject);

            // When moving, we usually put it at the end unless index specified
            if (dto.getIndex() == null) {
                long count = taskRepository.findBySubObjectId(newSubObjectId).size();
                task.setIndex((int) count);
            } else {
                // Smart Shift when moving to new sub-object
                if (!shouldJoinParallelGroup(newSubObjectId, dto.getIndex(), dto.getTaskType())) {
                    shiftIndices(newSubObjectId, dto.getIndex(), 1, null);
                }
                task.setIndex(dto.getIndex());
            }
        } else {
            // Same sub-object, check if index shifted
            if (dto.getIndex() != null && !dto.getIndex().equals(task.getIndex())) {
                // Smart Shift within same sub-object
                if (!shouldJoinParallelGroup(oldSubObjectId, dto.getIndex(), dto.getTaskType())) {
                    shiftIndices(oldSubObjectId, dto.getIndex(), 1, task.getId());
                }
                task.setIndex(dto.getIndex());
            }
        }

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

            // Validate parallel worker assignment
            if (task.getTaskType() == com.example.construction.Enums.TaskType.PARALLEL && task.getIndex() != null) {
                Set<Long> assigneeIds = assignees.stream().map(User::getId).collect(Collectors.toSet());
                validateParallelWorkerAssignment(task.getSubObject().getId(), task.getIndex(), assigneeIds,
                        task.getId());
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

            // 1. Delete items not in DTO (including photos from disk)
            existingItems.stream()
                    .filter(item -> !dtoItemIds.contains(item.getId()))
                    .forEach(item -> {
                        fileStorageService.delete(item.getPhotoUrl());
                        checklistItemRepository.delete(item);
                    });

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
                item.setMethodology(itemDto.getMethodology());
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
        if (!updatedTask.getSubObject().getId().equals(oldSubObjectId)) {
            recalculateTaskStatuses(oldSubObjectId);
        }
        return toDtoWithRejection(taskRepository.findById(updatedTask.getId()).orElse(updatedTask));
    }

    private void validateParallelWorkerAssignment(Long subObjectId, Integer index, Set<Long> assigneeIds,
            Long excludeTaskId) {
        List<Task> tasksAtIndex = taskRepository.findBySubObjectId(subObjectId).stream()
                .filter(t -> Objects.equals(t.getIndex(), index))
                .filter(t -> excludeTaskId == null || !t.getId().equals(excludeTaskId))
                .filter(t -> t.getTaskType() == com.example.construction.Enums.TaskType.PARALLEL)
                .collect(Collectors.toList());

        for (Task task : tasksAtIndex) {
            Set<Long> existingAssigneeIds = task.getAssignees().stream()
                    .map(User::getId)
                    .collect(Collectors.toSet());

            for (Long assigneeId : assigneeIds) {
                if (existingAssigneeIds.contains(assigneeId)) {
                    User worker = userRepository.findById(assigneeId).orElse(null);
                    String workerName = worker != null ? worker.getFullName() : "Unknown";
                    throw new IllegalArgumentException(
                            "Работник " + workerName + " уже назначен на другую параллельную задачу с индексом "
                                    + index);
                }
            }
        }
    }

    private void shiftIndices(Long subObjectId, Integer startIndex, int delta, Long excludeTaskId) {
        List<Task> existingTasks = taskRepository.findBySubObjectId(subObjectId);
        existingTasks.stream()
                .filter(t -> !t.getId().equals(excludeTaskId))
                .filter(t -> t.getIndex() != null && t.getIndex() >= startIndex)
                .forEach(t -> t.setIndex(t.getIndex() + delta));
        taskRepository.saveAll(existingTasks);
    }

    @org.springframework.transaction.annotation.Transactional
    public void recalculateTaskStatuses(Long subObjectId) {
        List<Task> tasks = taskRepository.findBySubObjectId(subObjectId);
        tasks.sort(Comparator.comparing(t -> t.getIndex() != null ? t.getIndex() : Integer.MAX_VALUE));

        // Group tasks by index
        Map<Integer, List<Task>> tasksByIndex = new LinkedHashMap<>();
        for (Task task : tasks) {
            Integer index = task.getIndex() != null ? task.getIndex() : Integer.MAX_VALUE;
            tasksByIndex.computeIfAbsent(index, k -> new ArrayList<>()).add(task);
        }

        boolean allPreviousCompleted = true;

        for (Map.Entry<Integer, List<Task>> entry : tasksByIndex.entrySet()) {
            List<Task> tasksAtIndex = entry.getValue();

            // Unlock or lock all tasks at this index based on previous completion
            for (Task t : tasksAtIndex) {
                if (allPreviousCompleted && t.getStatus() == TaskStatus.LOCKED) {
                    t.setStatus(TaskStatus.ACTIVE);
                } else if (!allPreviousCompleted && t.getStatus() != TaskStatus.COMPLETED) {
                    t.setStatus(TaskStatus.LOCKED);
                }
            }

            // Check if all tasks at this index are completed
            boolean allAtIndexCompleted = tasksAtIndex.stream()
                    .allMatch(t -> t.getStatus() == TaskStatus.COMPLETED);

            if (!allAtIndexCompleted) {
                allPreviousCompleted = false;
            }
        }

        taskRepository.saveAll(tasks);
    }

    @org.springframework.transaction.annotation.Transactional
    public void delete(Long id) {
        taskRepository.findById(id).ifPresent(task -> {
            validateTaskAccess(task);
            // Delete associated files from storage
            // Delete associated files from storage
            fileStorageService.delete(task.getFinalPhotoUrl());
            if (task.getChecklistItems() != null) {
                task.getChecklistItems().forEach(item -> fileStorageService.delete(item.getPhotoUrl()));
                checklistItemRepository.deleteAll(task.getChecklistItems());
                task.getChecklistItems().clear();
            }
            if (task.getReport() != null && task.getReport().getPhotos() != null) {
                task.getReport().getPhotos().forEach(photo -> fileStorageService.delete(photo.getFilePath()));
            }

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

    @org.springframework.transaction.annotation.Transactional
    public TaskDto updateFinalPhoto(Long taskId, String photoUrl) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        // Delete old final photo from disk if it exists
        if (task.getFinalPhotoUrl() != null) {
            fileStorageService.delete(task.getFinalPhotoUrl());
        }

        String extension = ".png";
        if (photoUrl.contains("image/jpeg")) {
            extension = ".jpg";
        } else if (photoUrl.contains("image/webp")) {
            extension = ".webp";
        }

        Base64MultipartFile file = new Base64MultipartFile(photoUrl, "task-final-" + taskId + extension);
        String storedFileName = fileStorageService.upload(file, "tasks");

        task.setFinalPhotoUrl(storedFileName);
        task.setUpdatedAt(LocalDateTime.now());
        taskRepository.save(task);

        return mapper.toDto(task);
    }

    private boolean shouldJoinParallelGroup(Long subObjectId, Integer index,
            com.example.construction.Enums.TaskType type) {
        if (type != com.example.construction.Enums.TaskType.PARALLEL || index == null)
            return false;
        List<Task> existing = taskRepository.findBySubObjectIdAndIndex(subObjectId, index);
        return !existing.isEmpty() && existing.stream()
                .allMatch(t -> t.getTaskType() == com.example.construction.Enums.TaskType.PARALLEL);
    }

    private void validateTaskAccess(Task task) {
        String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication()
                .getName();
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getRole() == com.example.construction.Enums.Role.SUPER_ADMIN)
            return;
        if (user.getRole() == com.example.construction.Enums.Role.ESTIMATOR)
            return;

        com.example.construction.model.Project project = task.getSubObject().getConstructionObject().getProject();

        if (user.getRole() == com.example.construction.Enums.Role.PM) {
            if (project.getProjectManager() != null && project.getProjectManager().getId().equals(user.getId())) {
                return;
            }
        }

        // Foremen can only edit tasks if assigned? (Simplification: PM/Admin/Estimator
        // ownership needed for structural changes)
        // If Foreman needs to update status, that's different. But update() is generic.
        // For now, restricting structural updates to PM/Admin/Estimator + correct
        // ownership.

        throw new org.springframework.security.access.AccessDeniedException(
                "You do not have permission to modify this task.");
    }
}
