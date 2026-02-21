package com.example.construction.service;

import com.example.construction.model.*;
import com.example.construction.reposirtories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TemplateService {

    private final SubObjectTemplateRepository subObjectTemplateRepository;
    private final TaskTemplateRepository taskTemplateRepository;
    private final ChecklistTemplateRepository checklistTemplateRepository;
    private final SubObjectRepository subObjectRepository;
    private final ChecklistService checklistService;
    private final TaskService taskService;

    // --- SubObject Template Management ---

    @Transactional
    public SubObjectTemplate createSubObjectTemplate(String name) {
        SubObjectTemplate template = new SubObjectTemplate();
        template.setName(name);
        return subObjectTemplateRepository.save(template);
    }

    @Transactional(readOnly = true)
    public List<SubObjectTemplate> getAllSubObjectTemplates() {
        return subObjectTemplateRepository.findAll();
    }

    @Transactional
    public SubObjectTemplate updateSubObjectTemplate(Long id, String name) {
        SubObjectTemplate template = subObjectTemplateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("SubObject Template not found"));
        template.setName(name);
        return subObjectTemplateRepository.save(template);
    }

    @Transactional
    public TaskTemplate addTaskTemplateToSubObject(Long subObjectTemplateId, String taskName, Long checklistTemplateId,
            Integer orderIndex) {
        SubObjectTemplate subObjectTemplate = subObjectTemplateRepository.findById(subObjectTemplateId)
                .orElseThrow(() -> new RuntimeException("SubObject Template not found"));

        ChecklistTemplate checklistTemplate = checklistTemplateRepository.findById(checklistTemplateId)
                .orElseThrow(() -> new RuntimeException("Checklist Template not found"));

        TaskTemplate taskTemplate = new TaskTemplate();
        taskTemplate.setName(taskName);
        taskTemplate.setSubObjectTemplate(subObjectTemplate);
        taskTemplate.setChecklistTemplate(checklistTemplate);
        taskTemplate.setOrderIndex(orderIndex);

        return taskTemplateRepository.save(taskTemplate);
    }

    @Transactional
    public void deleteSubObjectTemplate(Long id) {
        subObjectTemplateRepository.deleteById(id);
    }

    @Transactional
    public void deleteTaskTemplate(Long id) {
        taskTemplateRepository.deleteById(id);
    }

    // --- Apply Template Logic ---

    @Transactional
    public List<Task> applyTemplateToSubObject(Long subObjectId, Long subObjectTemplateId) {
        SubObject subObject = subObjectRepository.findById(subObjectId)
                .orElseThrow(() -> new RuntimeException("SubObject not found"));

        SubObjectTemplate template = subObjectTemplateRepository.findById(subObjectTemplateId)
                .orElseThrow(() -> new RuntimeException("Template not found"));

        List<TaskTemplate> taskTemplates = template.getTaskTemplates();

        // Create a task for each template item
        return taskTemplates.stream().map(taskTemplate -> {
            // 1. Create the Task
            Task newTask = new Task();
            newTask.setTitle(taskTemplate.getName());
            newTask.setSubObject(subObject);
            newTask.setStatus(com.example.construction.Enums.TaskStatus.ACTIVE);
            newTask.setTaskType(com.example.construction.Enums.TaskType.SEQUENTIAL);

            // Note: We are NOT setting assignee or start/end dates here yet.
            // The user will have to set them later, or we could pass them as arguments if
            // needed.
            // For now, we create them unassigned.

            Task savedTask = taskService.createTaskFromTemplate(newTask); // Need to helper in TaskService or just repo
                                                                          // save?
            // Actually let's use taskService.createTask logic but simpler?
            // Let's assume we can save it directly or via a simpler service method.

            // 2. Create Checklists from ChecklistTemplate
            ChecklistTemplate checklistTemplate = taskTemplate.getChecklistTemplate();
            if (checklistTemplate != null) {
                checklistTemplate.getItems().forEach(item -> {
                    checklistService.createChecklistItem(
                            savedTask.getId(),
                            item.getDescription(),
                            item.getMethodology(),
                            item.getOrderIndex(),
                            item.getIsPhotoRequired());
                });
            }

            return savedTask;
        }).toList();
    }
}
