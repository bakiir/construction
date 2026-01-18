package com.example.construction.controllers;

import com.example.construction.dto.ConstructionObjectCreateDto;
import com.example.construction.dto.ConstructionObjectDto;
import com.example.construction.mapper.ConstructionObjectMapper;
import com.example.construction.service.ConstructionObjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/objects")
public class ConstructionObjectController {
    private final ConstructionObjectMapper mapper;
    private final ConstructionObjectService service;

    @PostMapping
    public ConstructionObjectDto create(@RequestBody ConstructionObjectCreateDto dto) {
        return service.create(dto);
    }

    @GetMapping("/{id}")
    public ConstructionObjectDto getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @GetMapping("/project/{projectId}")
    public List<ConstructionObjectDto> getByProject(@PathVariable Long projectId) {
        return service.getByProject(projectId);
    }

    @PutMapping("/{id}")
    public ConstructionObjectDto update(
            @PathVariable Long id,
            @RequestBody ConstructionObjectCreateDto dto) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

}
