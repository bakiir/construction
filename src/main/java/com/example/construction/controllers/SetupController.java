package com.example.construction.controllers;

import com.example.construction.Enums.Role;
import com.example.construction.dto.UserCreateDto;
import com.example.construction.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class SetupController {

    private final UserService userService;

    @PostMapping("/setup")
    public ResponseEntity<String> setupInitialUsers() {
        createIfNotExists("0000000001", "123456", "Super Admin", Role.SUPER_ADMIN);
        createIfNotExists("0000000002", "123456", "Project Manager", Role.PM);
        createIfNotExists("0000000003", "123456", "Estimator", Role.ESTIMATOR);
        createIfNotExists("0000000004", "123456", "Foreman", Role.FOREMAN);
        createIfNotExists("0000000005", "123456", "Worker", Role.WORKER);

        return ResponseEntity.ok("Initial users setup complete");
    }

    private void createIfNotExists(String phone, String password, String fullName, Role role) {
        try {
            userService.getUserIdByPhone(phone);
            // User exists, skip
        } catch (RuntimeException e) {
            // User not found, create
            UserCreateDto dto = new UserCreateDto();
            dto.setPhone(phone);
            dto.setPassword(password);
            dto.setFullName(fullName);
            dto.setRole(role);
            userService.create(dto);
        }
    }
}
