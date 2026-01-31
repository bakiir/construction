package com.example.construction.controllers;

import com.example.construction.model.Notification;
import com.example.construction.service.NotificationService;
import com.example.construction.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final UserService userService;

    @GetMapping
    public ResponseEntity<List<Notification>> getUnreadNotifications(Authentication authentication) {
        // We need to get the User ID from the email in the authentication principal
        Long userId = userService.getUserIdByEmail(authentication.getName());
        List<Notification> notifications = notificationService.getUnreadNotifications(userId);
        return ResponseEntity.ok(notifications);
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id, Authentication authentication) {
        // Optional: Add validation to ensure the notification belongs to the
        // authenticated user
        notificationService.markAsRead(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/all")
    public ResponseEntity<List<Notification>> getAllNotifications(Authentication authentication) {
        Long userId = userService.getUserIdByEmail(authentication.getName());
        List<Notification> notifications = notificationService.getAllNotifications(userId);
        return ResponseEntity.ok(notifications);
    }

    @PostMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(Authentication authentication) {
        Long userId = userService.getUserIdByEmail(authentication.getName());
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok().build();
    }
}
