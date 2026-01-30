package com.example.construction.controllers;

import com.example.construction.dto.ChecklistItemDto;
import com.example.construction.model.ChecklistItem;
import com.example.construction.service.ChecklistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/checklist")
@RequiredArgsConstructor
public class ChecklistController {

    private final ChecklistService checklistService;

    @GetMapping("/task/{taskId}")
    @PreAuthorize("hasAnyRole('ESTIMATOR', 'FOREMAN', 'PM', 'SUPER_ADMIN', 'WORKER')")
    public ResponseEntity<List<ChecklistItemDto>> getChecklistsByTask(@PathVariable Long taskId) {
        List<ChecklistItem> items = checklistService.getChecklistsByTaskId(taskId);
        List<ChecklistItemDto> dtos = items.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PostMapping("/task/{taskId}")
    @PreAuthorize("hasAnyRole('ESTIMATOR', 'FOREMAN', 'PM', 'SUPER_ADMIN')")
    public ResponseEntity<ChecklistItemDto> createItem(@PathVariable Long taskId, @RequestBody ChecklistItemDto dto) {
        ChecklistItem item = checklistService.createChecklistItem(taskId, dto.getDescription(), dto.getOrderIndex(),
                dto.getIsPhotoRequired());
        return ResponseEntity.ok(toDTO(item));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ESTIMATOR', 'FOREMAN', 'PM', 'SUPER_ADMIN')")
    public ResponseEntity<ChecklistItemDto> updateItem(@PathVariable Long id, @RequestBody ChecklistItemDto dto) {
        ChecklistItem item = checklistService.updateChecklistItem(id, dto.getDescription(), dto.getOrderIndex(),
                dto.getIsPhotoRequired());
        return ResponseEntity.ok(toDTO(item));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ESTIMATOR', 'FOREMAN', 'PM', 'SUPER_ADMIN')")
    public ResponseEntity<Void> deleteChecklistItem(@PathVariable Long id) {
        checklistService.deleteChecklistItem(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('WORKER', 'SUPER_ADMIN')")
    public ResponseEntity<ChecklistItemDto> toggleComplete(
            @PathVariable Long id,
            @RequestParam Boolean completed) {
        ChecklistItem item = checklistService.markAsCompleted(id, completed);
        return ResponseEntity.ok(toDTO(item));
    }

    @PutMapping("/{id}/photo")
    @PreAuthorize("hasAnyRole('WORKER', 'FOREMAN', 'PM', 'ESTIMATOR', 'SUPER_ADMIN')")
    public ResponseEntity<ChecklistItemDto> updatePhoto(
            @PathVariable Long id,
            @RequestBody java.util.Map<String, String> body) {
        String photoUrl = body.get("photoUrl");
        ChecklistItem item = checklistService.updatePhoto(id, photoUrl);
        return ResponseEntity.ok(toDTO(item));
    }

    @PutMapping("/{id}/remark")
    @PreAuthorize("hasAnyRole('FOREMAN', 'PM', 'SUPER_ADMIN')")
    public ResponseEntity<ChecklistItemDto> updateRemark(@PathVariable Long id,
            @RequestBody java.util.Map<String, String> payload) {
        String remark = payload.get("remark");
        ChecklistItem item = checklistService.updateRemark(id, remark);
        return ResponseEntity.ok(toDTO(item));
    }

    private ChecklistItemDto toDTO(ChecklistItem item) {
        ChecklistItemDto dto = new ChecklistItemDto();
        dto.setId(item.getId());
        dto.setDescription(item.getDescription());
        dto.setOrderIndex(item.getOrderIndex());
        dto.setIsCompleted(item.getIsCompleted());
        dto.setPhotoUrl(item.getPhotoUrl());
        dto.setRemark(item.getRemark());
        dto.setIsPhotoRequired(item.getIsPhotoRequired());
        return dto;
    }
}
