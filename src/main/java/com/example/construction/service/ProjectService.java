package com.example.construction.service;

import com.example.construction.dto.ProjectCreateDto;
import com.example.construction.dto.ProjectDto;
import com.example.construction.mapper.ProjectMapper;
import com.example.construction.model.Project;
import com.example.construction.reposirtories.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectService {
    private final ProjectRepository projectRepository;
    private final ProjectMapper projectMapper;

    public Optional<ProjectDto> getProjectById(Long id){
        return projectRepository.findById(id)
                .map(projectMapper::toDto);
    }

    public List<ProjectDto> getAllProjects(){
        return projectRepository.findAll()
                .stream()
                .map(projectMapper::toDto)
                .collect(Collectors.toList());

    }

    public ProjectDto createProject(ProjectCreateDto projectCreateDto){
        Project project = projectMapper.toEntity(projectCreateDto);
        project.setCreatedAt(LocalDateTime.now());
//        project.setCreatedBy(getCurrentUserId()); // если есть метод получения текущего пользователя

        Project savedProject = projectRepository.save(project);

        return projectMapper.toDto(savedProject);

    }

    public ProjectDto updateProject(Project project, Long id){
        project.setId(id);
        Project updatedProject = projectRepository.save(project);
        return projectMapper.toDto(updatedProject);
    }

    public void deleteProject(Long id){
        projectRepository.deleteById(id);
    }


}
