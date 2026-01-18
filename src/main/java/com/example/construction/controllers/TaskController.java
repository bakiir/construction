package com.example.construction.controllers;

import com.example.construction.dto.TaskCreateDto;
import com.example.construction.dto.TaskDto;
import com.example.construction.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService service;

    @PostMapping
    public TaskDto create(@RequestBody TaskCreateDto dto) {
        return service.create(dto);
    }

    @GetMapping("/{id}")
    public TaskDto getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @GetMapping("/sub-object/{subObjectId}")
    public List<TaskDto> getBySubObject(@PathVariable Long subObjectId) {
        return service.getBySubObject(subObjectId);
    }

    @PutMapping("/{id}")
    public TaskDto update(
            @PathVariable Long id,
            @RequestBody TaskCreateDto dto) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
