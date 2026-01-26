package com.example.construction.service;

import com.example.construction.model.ChecklistItem;
import com.example.construction.model.Task;
import com.example.construction.reposirtories.ChecklistItemRepository;
import com.example.construction.reposirtories.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChecklistService {

    private final ChecklistItemRepository checklistItemRepository;
    private final TaskRepository taskRepository;

    @Transactional(readOnly = true)
    public List<ChecklistItem> getChecklistsByTaskId(Long taskId) {
        return checklistItemRepository.findByTaskIdOrderByOrderIndexAsc(taskId);
    }

    @Transactional
    public ChecklistItem createChecklistItem(Long taskId, String description, Integer orderIndex) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        ChecklistItem item = new ChecklistItem();
        item.setTask(task);
        item.setDescription(description);
        item.setOrderIndex(orderIndex);
        item.setIsCompleted(false);

        return checklistItemRepository.save(item);
    }

    @Transactional
    public ChecklistItem updateChecklistItem(Long id, String description, Integer orderIndex) {
        ChecklistItem item = checklistItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Checklist item not found"));

        if (description != null) {
            item.setDescription(description);
        }
        if (orderIndex != null) {
            item.setOrderIndex(orderIndex);
        }

        return checklistItemRepository.save(item);
    }

    @Transactional
    public void deleteChecklistItem(Long id) {
        checklistItemRepository.deleteById(id);
    }

    @Transactional
    public ChecklistItem markAsCompleted(Long id, Boolean completed) {
        ChecklistItem item = checklistItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Checklist item not found"));

        item.setIsCompleted(completed);
        return checklistItemRepository.save(item);
    }

    @Transactional
    public ChecklistItem updatePhoto(Long id, String photoUrl) {
        ChecklistItem item = checklistItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Checklist item not found"));

        item.setPhotoUrl(photoUrl);
        return checklistItemRepository.save(item);
    }

    @Transactional(readOnly = true)
    public boolean areAllChecklistsCompleted(Long taskId) {
        List<ChecklistItem> items = checklistItemRepository.findByTaskIdOrderByOrderIndexAsc(taskId);

        if (items.isEmpty()) {
            return true; // No checklists means all are "completed"
        }

        // Check if all are completed and have photos
        return items.stream()
                .allMatch(item -> Boolean.TRUE.equals(item.getIsCompleted()) && item.getPhotoUrl() != null);
    }
}
