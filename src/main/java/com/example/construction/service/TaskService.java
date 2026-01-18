package com.example.construction.service;

import com.example.construction.dto.TaskCreateDto;
import com.example.construction.dto.TaskDto;
import com.example.construction.mapper.TaskMapper;
import com.example.construction.model.SubObject;
import com.example.construction.model.Task;
import com.example.construction.reposirtories.SubObjectRepository;
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
    private final TaskMapper mapper;

    public TaskDto create(TaskCreateDto dto) {
        SubObject subObject = subObjectRepository.findById(dto.getSubObjectId())
                .orElseThrow(() -> new RuntimeException("SubObject not found"));

        Task task = new Task();
        task.setTitle(dto.getTitle());
        task.setTaskType(dto.getTaskType());
        task.setIndex(dto.getIndex());
        task.setDeadline(dto.getDeadline());
        task.setStatus(dto.getStatus());
        task.setSubObject(subObject);

        if (dto.getAssigneeIds() != null) {
            task.setAssignees(
                    new HashSet<>(userRepository.findAllById(dto.getAssigneeIds()))
            );
        }

        return mapper.toDto(taskRepository.save(task));
    }

    public TaskDto getById(Long id) {
        return taskRepository.findById(id)
                .map(mapper::toDto)
                .orElseThrow(() -> new RuntimeException("Task not found"));
    }

    public List<TaskDto> getBySubObject(Long subObjectId) {
        return taskRepository.findBySubObjectId(subObjectId)
                .stream()
                .map(mapper::toDto)
                .toList();
    }

    public TaskDto update(Long id, TaskCreateDto dto) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        task.setTitle(dto.getTitle());
        task.setTaskType(dto.getTaskType());
        task.setIndex(dto.getIndex());
        task.setDeadline(dto.getDeadline());
        task.setStatus(dto.getStatus());
        task.setUpdatedAt(LocalDateTime.now());

        if (dto.getAssigneeIds() != null) {
            task.setAssignees(
                    new HashSet<>(userRepository.findAllById(dto.getAssigneeIds()))
            );
        }

        return mapper.toDto(taskRepository.save(task));
    }

    public void delete(Long id) {
        taskRepository.deleteById(id);
    }
}
