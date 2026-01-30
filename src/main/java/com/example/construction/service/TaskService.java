package com.example.construction.service;

import com.example.construction.Enums.TaskStatus;
import com.example.construction.dto.TaskCreateDto;
import com.example.construction.dto.TaskDto;
import com.example.construction.mapper.TaskMapper;
import com.example.construction.model.SubObject;
import com.example.construction.model.Task;
import com.example.construction.model.User;
import com.example.construction.model.ChecklistItem;
import com.example.construction.reposirtories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final SubObjectRepository subObjectRepository;
    private final UserRepository userRepository;
    private final TaskApprovalRepository taskApprovalRepository;
    private final ChecklistItemRepository checklistItemRepository;
    private final TaskMapper mapper;

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

        // Sequential Logic
        if (dto.getTaskType() == com.example.construction.Enums.TaskType.SEQUENTIAL) {
            // Check previous task (index - 1)
            // If index is 0, it's always ACTIVE (unless stopped by something else, but here
            // ok)
            if (task.getIndex() != null && task.getIndex() > 0) {
                boolean previousTaskIncomplete = taskRepository.findBySubObjectId(subObject.getId()).stream()
                        .filter(t -> t.getIndex() != null && t.getIndex().equals(task.getIndex() - 1))
                        .findFirst()
                        .map(t -> t.getStatus() != TaskStatus.COMPLETED)
                        .orElse(false); // If no prev task found (gap?), treat as not blocking (or blocking? assume not)

                if (previousTaskIncomplete) {
                    task.setStatus(TaskStatus.LOCKED);
                } else {
                    task.setStatus(TaskStatus.ACTIVE);
                }
            } else {
                task.setStatus(TaskStatus.ACTIVE);
            }
        } else {
            task.setStatus(TaskStatus.ACTIVE);
        }

        if (dto.getAssigneeIds() != null) {
            Set<User> assignees = new HashSet<>(userRepository.findAllById(dto.getAssigneeIds()));

            // Validate: Assignees must be from sub-object workers
            Set<User> subObjectWorkers = subObject.getWorkers();
            for (User assignee : assignees) {
                if (!subObjectWorkers.contains(assignee)) {
                    throw new IllegalArgumentException(
                            "Worker " + assignee.getFullName() + " is not assigned to this sub-object");
                }
            }

            task.setAssignees(assignees);
        }

        Task savedTask = taskRepository.save(task);

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

        return toDtoWithRejection(savedTask);
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

        return toDtoWithRejection(taskRepository.save(task));
    }

    public void delete(Long id) {
        taskRepository.deleteById(id);
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

        // Populate foremanNote for PM review (latest note from Foreman when sending to
        // PM)
        if (task.getStatus() == TaskStatus.UNDER_REVIEW_PM) {
            taskApprovalRepository.findTopByTaskAndDecisionAndRoleAtTimeOfApprovalOrderByCreatedAtDesc(
                    task, "APPROVED", com.example.construction.Enums.Role.FOREMAN)
                    .ifPresent(approval -> dto.setForemanNote(approval.getComment()));
        }

        return dto;
    }

    public TaskDto updateFinalPhoto(Long taskId, String photoUrl) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        task.setFinalPhotoUrl(photoUrl);
        task.setUpdatedAt(LocalDateTime.now());
        taskRepository.save(task);

        return mapper.toDto(task);
    }
}
