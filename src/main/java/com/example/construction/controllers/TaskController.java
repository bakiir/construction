package com.example.construction.controllers;

import com.example.construction.dto.TaskCreateDto;
import com.example.construction.dto.TaskDto;
import com.example.construction.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService service;

    @PostMapping
    @PreAuthorize("hasAnyRole('ESTIMATOR', 'PM', 'SUPER_ADMIN')")
    public TaskDto create(@RequestBody TaskCreateDto dto) {
        return service.create(dto);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ESTIMATOR', 'FOREMAN', 'PM', 'SUPER_ADMIN', 'WORKER')")
    public TaskDto getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ESTIMATOR', 'FOREMAN', 'PM', 'SUPER_ADMIN', 'WORKER')")
    public List<TaskDto> getAll() {
        return service.getAll();
    }

    @GetMapping("/sub-object/{subObjectId}")
    @PreAuthorize("hasAnyRole('ESTIMATOR', 'FOREMAN', 'PM', 'SUPER_ADMIN', 'WORKER')")
    public List<TaskDto> getBySubObject(
            @PathVariable Long subObjectId,
            @RequestParam(required = false) Long userId) {
        if (userId != null) {
            return service.getBySubObjectForUser(subObjectId, userId);
        }
        return service.getBySubObject(subObjectId);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ESTIMATOR', 'PM', 'SUPER_ADMIN')")
    public TaskDto update(
            @PathVariable Long id,
            @RequestBody TaskCreateDto dto) {
        return service.update(id, dto);
    }

    @PreAuthorize("hasAnyRole('ESTIMATOR', 'PM', 'SUPER_ADMIN')")
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    @PutMapping("/{id}/final-photo")
    @PreAuthorize("hasAnyRole('WORKER', 'FOREMAN', 'PM', 'ESTIMATOR', 'SUPER_ADMIN')")
    public TaskDto updateFinalPhoto(@PathVariable Long id, @RequestBody java.util.Map<String, String> body) {
        String photoUrl = body.get("photoUrl");
        return service.updateFinalPhoto(id, photoUrl);
    }
}
