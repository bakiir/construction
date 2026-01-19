package com.example.construction.controllers;

import com.example.construction.dto.ConstructionObjectCreateDto;
import com.example.construction.dto.ConstructionObjectDto;
import com.example.construction.mapper.ConstructionObjectMapper;
import com.example.construction.service.ConstructionObjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/objects")
public class ConstructionObjectController {
    private final ConstructionObjectMapper mapper;
    private final ConstructionObjectService service;

    @PostMapping
    @PreAuthorize("hasRole('ESTIMATOR')")
    public ConstructionObjectDto create(@RequestBody ConstructionObjectCreateDto dto) {
        return service.create(dto);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ESTIMATOR', 'FOREMAN', 'PM', 'SUPER_ADMIN')")
    public ConstructionObjectDto getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @GetMapping("/project/{projectId}")
    @PreAuthorize("hasAnyRole('ESTIMATOR', 'FOREMAN', 'PM', 'SUPER_ADMIN')")
    public List<ConstructionObjectDto> getByProject(@PathVariable Long projectId) {
        return service.getByProject(projectId);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ESTIMATOR')")
    public ConstructionObjectDto update(
            @PathVariable Long id,
            @RequestBody ConstructionObjectCreateDto dto) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ESTIMATOR')")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

}
