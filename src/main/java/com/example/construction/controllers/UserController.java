package com.example.construction.controllers;

import com.example.construction.dto.UserCreateDto;
import com.example.construction.dto.UserDto;
import com.example.construction.dto.UserUpdateDto;
import com.example.construction.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService service;

    @PostMapping
    public UserDto create(@RequestBody UserCreateDto dto) {
        return service.create(dto);
    }

    @GetMapping("/{id}")
    public UserDto getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @GetMapping
    public List<UserDto> getAll() {
        return service.getAll();
    }

    @PutMapping("/{id}")
    public UserDto update(
            @PathVariable Long id,
            @RequestBody UserUpdateDto dto) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public void deactivate(@PathVariable Long id) {
        service.deactivate(id);
    }
}
