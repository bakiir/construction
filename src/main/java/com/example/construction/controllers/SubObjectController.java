package com.example.construction.controllers;

import com.example.construction.dto.SubObjectCreateDto;
import com.example.construction.dto.SubObjectDto;
import com.example.construction.service.SubObjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import com.example.construction.dto.UserDto;

@RestController
@RequestMapping("/api/sub-objects")
@RequiredArgsConstructor
public class SubObjectController {

    private final SubObjectService service;

    @PostMapping
    @PreAuthorize("hasRole('ESTIMATOR')")
    public SubObjectDto create(@RequestBody SubObjectCreateDto dto) {
        return service.create(dto);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ESTIMATOR', 'FOREMAN', 'PM', 'SUPER_ADMIN', 'WORKER')")
    public SubObjectDto getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @GetMapping("/object/{objectId}")
    @PreAuthorize("hasAnyRole('ESTIMATOR', 'FOREMAN', 'PM', 'SUPER_ADMIN', 'WORKER')")
    public List<SubObjectDto> getByObject(@PathVariable Long objectId) {
        return service.getByObject(objectId);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ESTIMATOR')")
    public SubObjectDto update(
            @PathVariable Long id,
            @RequestBody SubObjectCreateDto dto) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ESTIMATOR')")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    // Worker assignment endpoints

    @PostMapping("/{subObjectId}/workers/{workerId}")
    @PreAuthorize("hasAnyRole('ESTIMATOR', 'FOREMAN', 'PM', 'SUPER_ADMIN')")
    public ResponseEntity<Void> addWorker(
            @PathVariable Long subObjectId,
            @PathVariable Long workerId) {
        service.addWorker(subObjectId, workerId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{subObjectId}/workers/{workerId}")
    @PreAuthorize("hasAnyRole('ESTIMATOR', 'FOREMAN', 'PM', 'SUPER_ADMIN')")
    public ResponseEntity<Void> removeWorker(
            @PathVariable Long subObjectId,
            @PathVariable Long workerId) {
        service.removeWorker(subObjectId, workerId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{subObjectId}/workers")
    @PreAuthorize("hasAnyRole('ESTIMATOR', 'FOREMAN', 'PM', 'SUPER_ADMIN', 'WORKER')")
    public ResponseEntity<List<UserDto>> getWorkers(@PathVariable Long subObjectId) {
        return ResponseEntity.ok(service.getWorkers(subObjectId));
    }
}
