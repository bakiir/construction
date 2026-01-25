package com.example.construction.service;

import com.example.construction.Enums.TaskStatus;
import com.example.construction.dto.TaskCreateDto;
import com.example.construction.dto.TaskDto;
import com.example.construction.mapper.TaskMapper;
import com.example.construction.model.SubObject;
import com.example.construction.model.Task;
import com.example.construction.model.TaskApproval;
import com.example.construction.reposirtories.SubObjectRepository;
import com.example.construction.reposirtories.TaskApprovalRepository;
import com.example.construction.reposirtories.TaskRepository;
import com.example.construction.reposirtories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final SubObjectRepository subObjectRepository;
    private final UserRepository userRepository;
    private final TaskApprovalRepository taskApprovalRepository;
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
            boolean collision = existingTasks.stream().anyMatch(t -> t.getIndex().equals(dto.getIndex()));

            if (collision) {
                existingTasks.stream()
                        .filter(t -> t.getIndex() >= dto.getIndex())
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
            if (task.getIndex() > 0) {
                boolean previousTaskIncomplete = taskRepository.findBySubObjectId(subObject.getId()).stream()
                        .filter(t -> t.getIndex() == task.getIndex() - 1)
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
            task.setAssignees(
                    new HashSet<>(userRepository.findAllById(dto.getAssigneeIds())));
        }

        return toDtoWithRejection(taskRepository.save(task));
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
            task.setAssignees(
                    new HashSet<>(userRepository.findAllById(dto.getAssigneeIds())));
        }

        return toDtoWithRejection(taskRepository.save(task));
    }

    public void delete(Long id) {
        taskRepository.deleteById(id);
    }

    private TaskDto toDtoWithRejection(Task task) {
        TaskDto dto = mapper.toDto(task);
        if (task.getStatus() == TaskStatus.REWORK || task.getStatus() == TaskStatus.UNDER_REVIEW_FOREMAN) {
            taskApprovalRepository.findTopByTaskAndDecisionOrderByCreatedAtDesc(task, "REJECTED")
                    .ifPresent(approval -> dto.setRejectionReason(approval.getComment()));
        }
        return dto;
    }
}
