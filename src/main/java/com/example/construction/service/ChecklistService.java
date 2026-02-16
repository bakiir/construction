package com.example.construction.service;

import com.example.construction.model.ChecklistItem;
import com.example.construction.model.Task;
import com.example.construction.model.User;
import com.example.construction.reposirtories.ChecklistItemRepository;
import com.example.construction.reposirtories.TaskRepository;
import com.example.construction.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChecklistService {

    private final ChecklistItemRepository checklistItemRepository;
    private final TaskRepository taskRepository;
    private final FileStorageService fileStorageService;
    private final NotificationService notificationService;
    private final com.example.construction.reposirtories.UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<ChecklistItem> getChecklistsByTaskId(Long taskId) {
        return checklistItemRepository.findByTaskIdOrderByOrderIndexAsc(taskId);
    }

    @Transactional
    public ChecklistItem createChecklistItem(Long taskId, String description, String methodology, Integer orderIndex,
            Boolean isPhotoRequired) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        ChecklistItem item = new ChecklistItem();
        item.setTask(task);
        item.setDescription(description);
        item.setMethodology(methodology);
        item.setOrderIndex(orderIndex);
        item.setIsCompleted(false);
        item.setIsPhotoRequired(isPhotoRequired != null ? isPhotoRequired : false);

        return checklistItemRepository.save(item);
    }

    @Transactional
    public ChecklistItem updateChecklistItem(Long id, String description, String methodology, Integer orderIndex,
            Boolean isPhotoRequired) {
        ChecklistItem item = checklistItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Checklist item not found"));

        if (description != null) {
            item.setDescription(description);
        }
        if (methodology != null) {
            item.setMethodology(methodology);
        }
        if (orderIndex != null) {
            item.setOrderIndex(orderIndex);
        }
        if (isPhotoRequired != null) {
            item.setIsPhotoRequired(isPhotoRequired);
        }

        return checklistItemRepository.save(item);
    }

    @Transactional
    public void deleteChecklistItem(Long id) {
        checklistItemRepository.findById(id).ifPresent(item -> {
            fileStorageService.delete(item.getPhotoUrl());
            checklistItemRepository.delete(item);
        });
    }

    @Transactional
    public ChecklistItem markAsCompleted(Long id, Boolean completed) {
        ChecklistItem item = checklistItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Checklist item not found"));

        item.setIsCompleted(completed);
        if (Boolean.TRUE.equals(completed)) {
            item.setRemark(null);
        }
        ChecklistItem savedItem = checklistItemRepository.save(item);

        if (Boolean.TRUE.equals(completed)) {
            try {
                String email = SecurityContextHolder.getContext().getAuthentication().getName();
                userRepository.findByEmail(email).ifPresent(user -> {
                    if (user.getRole() == com.example.construction.Enums.Role.WORKER) {
                        notifyForeman(item, user);
                    }
                });
            } catch (Exception e) {
                // Log error but don't fail the transaction
                System.err.println("Failed to send notification: " + e.getMessage());
                e.printStackTrace();
            }
        }

        return savedItem;
    }

    private void notifyForeman(ChecklistItem item, User worker) {
        Task task = item.getTask();
        // Check SubObject -> ConstructionObject -> LeadForeman
        if (task.getSubObject() != null && task.getSubObject().getConstructionObject() != null) {
            com.example.construction.model.ConstructionObject co = task.getSubObject().getConstructionObject();
            if (co.getLeadForeman() != null) {
                notificationService.createNotification(
                        co.getLeadForeman(),
                        "Работник " + worker.getFullName() + " выполнил пункт: " + item.getDescription(),
                        task);
                return;
            }
            // Fallback to Project Foremen
            if (co.getProject() != null && co.getProject().getForemen() != null) {
                for (User foreman : co.getProject().getForemen()) {
                    notificationService.createNotification(
                            foreman,
                            "Работник " + worker.getFullName() + " выполнил пункт: " + item.getDescription(),
                            task);
                }
            }
        }
    }

    @Transactional
    public ChecklistItem updatePhoto(Long id, String photoUrl) {
        ChecklistItem item = checklistItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Checklist item not found"));

        // Delete old photo file if it exists
        fileStorageService.delete(item.getPhotoUrl());

        String extension = ".png";
        if (photoUrl.contains("image/jpeg")) {
            extension = ".jpg";
        } else if (photoUrl.contains("image/webp")) {
            extension = ".webp";
        }

        // Convert Base64 string to MultipartFile
        Base64MultipartFile file = new Base64MultipartFile(photoUrl, "checklist-" + id + extension);

        String storedFileName = fileStorageService.upload(file, "checklists");
        item.setPhotoUrl(storedFileName);
        return checklistItemRepository.save(item);
    }

    @Transactional(readOnly = true)
    public boolean areAllChecklistsChecked(Long taskId) {
        List<ChecklistItem> items = checklistItemRepository.findByTaskIdOrderByOrderIndexAsc(taskId);
        if (items.isEmpty())
            return true;
        // Check ONLY if IS_COMPLETED is true, ignore photos
        return items.stream().allMatch(item -> Boolean.TRUE.equals(item.getIsCompleted()));
    }

    @Transactional(readOnly = true)
    public boolean areAllChecklistsCompleted(Long taskId) {
        List<ChecklistItem> items = checklistItemRepository.findByTaskIdOrderByOrderIndexAsc(taskId);

        if (items.isEmpty()) {
            return true; // No checklists means all are "completed"
        }

        // Check if all are completed and have photos only if required
        return items.stream()
                .allMatch(item -> Boolean.TRUE.equals(item.getIsCompleted()) &&
                        (!Boolean.TRUE.equals(item.getIsPhotoRequired()) || item.getPhotoUrl() != null));
    }

    @Transactional
    public ChecklistItem updateRemark(Long id, String remark) {
        ChecklistItem item = checklistItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Checklist item not found"));

        item.setRemark(remark);
        return checklistItemRepository.save(item);
    }
}
