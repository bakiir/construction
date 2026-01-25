package com.example.construction.service;

import com.example.construction.Enums.Role;
import com.example.construction.Enums.TaskStatus;
import com.example.construction.dto.ApprovalDto;
import com.example.construction.model.Task;
import com.example.construction.model.User;
import com.example.construction.reposirtories.TaskApprovalRepository;
import com.example.construction.reposirtories.TaskRepository;
import com.example.construction.reposirtories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Optional;
import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class WorkflowServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TaskApprovalRepository taskApprovalRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private WorkflowService workflowService;

    private Task task;
    private User worker;
    private User foreman;
    private User pm;

    @BeforeEach
    void setUp() {
        worker = new User();
        worker.setId(1L);
        worker.setEmail("worker@example.com");
        worker.setRole(Role.WORKER);

        foreman = new User();
        foreman.setId(2L);
        foreman.setEmail("foreman@example.com");
        foreman.setRole(Role.FOREMAN);

        pm = new User();
        pm.setId(3L);
        pm.setEmail("pm@example.com");
        pm.setRole(Role.PM);

        task = new Task();
        task.setId(100L);
        task.setTitle("Test Task");
        task.setAssignees(Set.of(worker));
    }

    @Test
    void rejectTask_ForemanRejects_ShouldReturnToWorker() {
        // Arrange
        task.setStatus(TaskStatus.UNDER_REVIEW_FOREMAN);
        String reason = "Fix pipe";
        ApprovalDto dto = new ApprovalDto();
        dto.setComment(reason);

        when(taskRepository.findById(100L)).thenReturn(Optional.of(task));
        when(userRepository.findByEmail(foreman.getEmail())).thenReturn(Optional.of(foreman));

        // Act
        workflowService.rejectTask(100L, foreman.getEmail(), dto);

        // Assert
        assertEquals(TaskStatus.REWORK, task.getStatus());
        verify(notificationService).createNotification(eq(worker), contains("Sent for rework by Foreman"));
        verify(taskRepository).save(task);
    }

    @Test
    void rejectTask_PMRejects_ShouldReturnToForeman() {
        // Arrange
        task.setStatus(TaskStatus.UNDER_REVIEW_PM);
        String reason = "Check regulations";
        ApprovalDto dto = new ApprovalDto();
        dto.setComment(reason);

        when(taskRepository.findById(100L)).thenReturn(Optional.of(task));
        when(userRepository.findByEmail(pm.getEmail())).thenReturn(Optional.of(pm));
        when(userRepository.findAllByRole(Role.FOREMAN)).thenReturn(List.of(foreman));

        // Act
        workflowService.rejectTask(100L, pm.getEmail(), dto);

        // Assert
        assertEquals(TaskStatus.UNDER_REVIEW_FOREMAN, task.getStatus());
        verify(notificationService).createNotification(eq(foreman), contains("returned to Foreman by PM"));
        verify(notificationService, never()).createNotification(eq(worker), anyString());
        verify(taskRepository).save(task);
    }
}