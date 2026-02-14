package com.example.construction.controllers;

import com.example.construction.model.SubObjectTemplate;
import com.example.construction.model.Task;
import com.example.construction.model.TaskTemplate;
import com.example.construction.service.TemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/templates/sub-object")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class SubObjectTemplateController {

    private final TemplateService templateService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ESTIMATOR', 'SUPER_ADMIN')")
    public ResponseEntity<SubObjectTemplate> createTemplate(@RequestParam String name) {
        return ResponseEntity.ok(templateService.createSubObjectTemplate(name));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ESTIMATOR', 'SUPER_ADMIN', 'PM', 'FOREMAN')")
    public ResponseEntity<List<SubObjectTemplate>> getAllTemplates() {
        return ResponseEntity.ok(templateService.getAllSubObjectTemplates());
    }

    @PostMapping("/{templateId}/tasks")
    @PreAuthorize("hasAnyRole('ESTIMATOR', 'SUPER_ADMIN')")
    public ResponseEntity<TaskTemplate> addTaskTemplate(
            @PathVariable Long templateId,
            @RequestParam String taskName,
            @RequestParam Long checklistTemplateId,
            @RequestParam Integer orderIndex) {
        return ResponseEntity
                .ok(templateService.addTaskTemplateToSubObject(templateId, taskName, checklistTemplateId, orderIndex));
    }

    @DeleteMapping("/{templateId}")
    @PreAuthorize("hasAnyRole('ESTIMATOR', 'SUPER_ADMIN')")
    public ResponseEntity<Void> deleteTemplate(@PathVariable Long templateId) {
        templateService.deleteSubObjectTemplate(templateId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{templateId}/apply/{subObjectId}")
    @PreAuthorize("hasAnyRole('ESTIMATOR', 'SUPER_ADMIN', 'PM')")
    public ResponseEntity<List<Task>> applyTemplate(
            @PathVariable Long templateId,
            @PathVariable Long subObjectId) {
        return ResponseEntity.ok(templateService.applyTemplateToSubObject(subObjectId, templateId));
    }
}
