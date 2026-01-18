package com.example.construction.controllers;

import com.example.construction.dto.ConstructionObjectCreateDto;
import com.example.construction.dto.ConstructionObjectDto;
import com.example.construction.mapper.ConstructionObjectMapper;
import com.example.construction.service.ConstructionObjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
