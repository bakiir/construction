package com.example.construction.controllers;

import com.example.construction.dto.SubObjectCreateDto;
import com.example.construction.dto.SubObjectDto;
import com.example.construction.service.SubObjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/sub-objects")
@RequiredArgsConstructor
public class SubObjectController {

    private final SubObjectService service;

    @PostMapping
    public SubObjectDto create(@RequestBody SubObjectCreateDto dto) {
        return service.create(dto);
    }

    @GetMapping("/{id}")
    public SubObjectDto getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @GetMapping("/object/{objectId}")
    public List<SubObjectDto> getByObject(@PathVariable Long objectId) {
        return service.getByObject(objectId);
    }

    @PutMapping("/{id}")
    public SubObjectDto update(
            @PathVariable Long id,
            @RequestBody SubObjectCreateDto dto) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
