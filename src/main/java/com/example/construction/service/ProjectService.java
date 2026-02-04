package com.example.construction.service;

import com.example.construction.Enums.Role;
import com.example.construction.dto.ProjectCreateDto;
import com.example.construction.dto.ProjectDto;
import com.example.construction.dto.ProjectUpdateDto;
import com.example.construction.dto.UserDto;
import com.example.construction.mapper.ProjectMapper;
import com.example.construction.mapper.UserMapper;
import com.example.construction.model.Project;
import com.example.construction.model.User;
import com.example.construction.reposirtories.ProjectRepository;
import com.example.construction.reposirtories.UserRepository;
import com.example.construction.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectService {
    private final ProjectRepository projectRepository;
    private final ProjectMapper projectMapper;
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final NotificationService notificationService;

    public Optional<ProjectDto> getProjectById(Long id) {
        return projectRepository.findById(id)
                .map(projectMapper::toDto);
    }

    public List<ProjectDto> getAllProjects() {
        return projectRepository.findAll()
                .stream()
                .map(projectMapper::toDto)
                .collect(Collectors.toList());

    }

    public ProjectDto createProject(ProjectCreateDto projectCreateDto) {
        Project project = projectMapper.toEntity(projectCreateDto);
        project.setCreatedAt(LocalDateTime.now());
        // project.setCreatedBy(getCurrentUserId()); // если есть метод получения
        // текущего пользователя

        Project savedProject = projectRepository.save(project);

        return projectMapper.toDto(savedProject);

    }

    @Transactional
    public ProjectDto updateProject(ProjectUpdateDto projectUpdateDto, Long id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found"));
        validateProjectAccess(project);
        projectMapper.updateEntityFromDto(projectUpdateDto, project);
        project.setUpdatedAt(LocalDateTime.now());
        Project updatedProject = projectRepository.save(project);
        return projectMapper.toDto(updatedProject);
    }

    public void deleteProject(Long id) {
        Project project = projectRepository.findById(id).orElseThrow(() -> new RuntimeException("Project not found"));
        validateProjectAccess(project);
        projectRepository.delete(project);
    }

    // Assignment methods

    @Transactional
    public void assignProjectManager(Long projectId, Long userId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getRole() != Role.PM && user.getRole() != Role.SUPER_ADMIN) {
            throw new IllegalArgumentException("User must have PM or SUPER_ADMIN role");
        }

        project.setProjectManager(user);
        projectRepository.save(project);
    }

    @Transactional
    public void addForeman(Long projectId, Long foremanId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));
        User foreman = userRepository.findById(foremanId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (foreman.getRole() != Role.FOREMAN) {
            throw new IllegalArgumentException("User must have FOREMAN role");
        }

        project.getForemen().add(foreman);
        projectRepository.save(project);
    }

    @Transactional
    public void removeForeman(Long projectId, Long foremanId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        project.getForemen().removeIf(f -> f.getId().equals(foremanId));
        projectRepository.save(project);
    }

    public List<UserDto> getForemen(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        return project.getForemen().stream()
                .map(userMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void publishProject(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        if (project.getStatus() == com.example.construction.Enums.ProjectStatus.PUBLISHED) {
            return; // Already published
        }

        project.setStatus(com.example.construction.Enums.ProjectStatus.PUBLISHED);
        projectRepository.save(project);

        // Notify Admins and PMs
        List<User> adminsAndPms = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.PM || u.getRole() == Role.SUPER_ADMIN)
                .collect(Collectors.toList());

        adminsAndPms.forEach(u -> notificationService.createNotification(u,
                "Новый проект опубликован: '" + project.getName() + "'", null));

        // Notify Assigned Foremen
        if (project.getForemen() != null) {
            project.getForemen().forEach(foreman -> notificationService.createNotification(foreman,
                    "Вы назначены на новый проект: '" + project.getName() + "'", null));
        }

        // Notify Workers for all tasks in this project
        if (project.getConstructionObjects() != null) {
            project.getConstructionObjects().forEach(co -> {
                if (co.getSubObjects() != null) {
                    co.getSubObjects().forEach(so -> {
                        if (so.getTasks() != null) {
                            so.getTasks().forEach(task -> {
                                if (task.getAssignees() != null) {
                                    task.getAssignees().forEach(worker -> notificationService.createNotification(worker,
                                            "Проект опубликован. Вам назначена задача: '" + task.getTitle() + "'",
                                            task));
                                }
                            });
                        }
                    });
                }
            });
        }
    }

    private void validateProjectAccess(Project project) {
        String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication()
                .getName();
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getRole() == Role.SUPER_ADMIN)
            return;
        if (user.getRole() == Role.ESTIMATOR)
            return;

        if (user.getRole() == Role.PM) {
            if (project.getProjectManager() != null && project.getProjectManager().getId().equals(user.getId())) {
                return;
            }
        }

        throw new org.springframework.security.access.AccessDeniedException(
                "You do not have permission to modify this project.");
    }
}
