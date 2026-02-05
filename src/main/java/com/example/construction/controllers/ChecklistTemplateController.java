package com.example.construction.controllers;

import com.example.construction.model.ChecklistTemplate;
import com.example.construction.model.ChecklistTemplateItem;
import com.example.construction.reposirtories.ChecklistTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/templates")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class ChecklistTemplateController {

    private final ChecklistTemplateRepository templateRepository;

    @GetMapping
    public List<ChecklistTemplate> getAllTemplates() {
        return templateRepository.findAll();
    }

    @PostMapping
    public ChecklistTemplate createTemplate(@RequestBody ChecklistTemplate template) {
        if (template.getItems() != null) {
            for (ChecklistTemplateItem item : template.getItems()) {
                item.setTemplate(template);
            }
        }
        return templateRepository.save(template);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTemplate(@PathVariable Long id) {
        templateRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }
}
