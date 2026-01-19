package com.example.construction.controllers;


import com.example.construction.dto.ProjectCreateDto;
import com.example.construction.dto.ProjectDto;
import com.example.construction.model.Project;
import com.example.construction.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ESTIMATOR', 'FOREMAN', 'PM', 'SUPER_ADMIN')")
    public List<ProjectDto> getAllProjects(){
        return projectService.getAllProjects();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ESTIMATOR', 'FOREMAN', 'PM', 'SUPER_ADMIN')")
    public Optional<ProjectDto> getProjectById(@PathVariable Long id){
        return projectService.getProjectById(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ESTIMATOR')")
    public ProjectDto createProject(@RequestBody ProjectCreateDto project){
        return projectService.createProject(project);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ESTIMATOR')")
    public ProjectDto updateProject(@PathVariable Long id, @RequestBody Project project){
        return projectService.updateProject(project, id);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ESTIMATOR')")
    public void deleteProject(@PathVariable Long id){
        projectService.deleteProject(id);
    }


}
