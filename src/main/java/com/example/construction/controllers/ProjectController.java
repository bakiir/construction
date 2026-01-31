package com.example.construction.controllers;

import com.example.construction.dto.ProjectCreateDto;
import com.example.construction.dto.ProjectDto;
import com.example.construction.dto.ProjectUpdateDto;
import com.example.construction.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import com.example.construction.dto.UserDto;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ESTIMATOR', 'FOREMAN', 'PM', 'SUPER_ADMIN')")
    public List<ProjectDto> getAllProjects() {
        return projectService.getAllProjects();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ESTIMATOR', 'FOREMAN', 'PM', 'SUPER_ADMIN')")
    public Optional<ProjectDto> getProjectById(@PathVariable Long id) {
        return projectService.getProjectById(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ESTIMATOR')")
    public ProjectDto createProject(@RequestBody ProjectCreateDto project) {
        return projectService.createProject(project);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ESTIMATOR')")
    public ProjectDto updateProject(@PathVariable Long id, @RequestBody ProjectUpdateDto project) {
        return projectService.updateProject(project, id);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ESTIMATOR')")
    public void deleteProject(@PathVariable Long id) {
        projectService.deleteProject(id);
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasRole('ESTIMATOR')")
    public ResponseEntity<Void> publishProject(@PathVariable Long id) {
        projectService.publishProject(id);
        return ResponseEntity.ok().build();
    }

    // Assignment endpoints

    @PostMapping("/{projectId}/assign-pm")
    @PreAuthorize("hasAnyRole('ESTIMATOR', 'SUPER_ADMIN')")
    public ResponseEntity<Void> assignProjectManager(
            @PathVariable Long projectId,
            @RequestParam Long userId) {
        projectService.assignProjectManager(projectId, userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{projectId}/foremen/{foremanId}")
    @PreAuthorize("hasAnyRole('ESTIMATOR', 'PM', 'SUPER_ADMIN')")
    public ResponseEntity<Void> addForeman(
            @PathVariable Long projectId,
            @PathVariable Long foremanId) {
        projectService.addForeman(projectId, foremanId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{projectId}/foremen/{foremanId}")
    @PreAuthorize("hasAnyRole('ESTIMATOR', 'PM', 'SUPER_ADMIN')")
    public ResponseEntity<Void> removeForeman(
            @PathVariable Long projectId,
            @PathVariable Long foremanId) {
        projectService.removeForeman(projectId, foremanId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{projectId}/foremen")
    @PreAuthorize("hasAnyRole('ESTIMATOR', 'PM', 'SUPER_ADMIN', 'FOREMAN')")
    public ResponseEntity<List<UserDto>> getForemen(@PathVariable Long projectId) {
        return ResponseEntity.ok(projectService.getForemen(projectId));
    }

}
