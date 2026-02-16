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
        createIfNotExists("superadmin@bauberg.com", "123456", "Super Admin", Role.SUPER_ADMIN);
        createIfNotExists("pm@bauberg.com", "123456", "Project Manager", Role.PM);
        createIfNotExists("estimator@bauberg.com", "123456", "Estimator", Role.ESTIMATOR);
        createIfNotExists("foreman@bauberg.com", "123456", "Foreman", Role.FOREMAN);
        createIfNotExists("worker@bauberg.com", "123456", "Worker", Role.WORKER);

        return ResponseEntity.ok("Initial users setup complete");
    }

    private void createIfNotExists(String email, String password, String fullName, Role role) {
        try {
            userService.getUserIdByEmail(email);
            // User exists, skip or maybe update password? For now, skip.
        } catch (RuntimeException e) {
            // User not found, create
            UserCreateDto dto = new UserCreateDto();
            dto.setEmail(email);
            dto.setPassword(password);
            dto.setFullName(fullName);
            dto.setRole(role);
            userService.create(dto);
        }
    }
}
