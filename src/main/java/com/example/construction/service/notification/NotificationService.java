package com.example.construction.service.notification;

import com.example.construction.model.Notification;
import com.example.construction.model.User;
import com.example.construction.reposirtories.notification.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    @Transactional
    public void createNotification(User user, String message) {
        createNotification(user, null, message, com.example.construction.Enums.NotificationCategory.SYSTEM, null);
    }

    @Transactional
    public void createNotification(User user, String message, com.example.construction.model.Task task) {
        createNotification(user, null, message, com.example.construction.Enums.NotificationCategory.TASK_UPDATE, task);
    }

    @Transactional
    public void createNotification(User user, String title, String message,
            com.example.construction.Enums.NotificationCategory category, com.example.construction.model.Task task) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setMessage(message);
        notification.setTask(task);
        if (task != null && task.getSubObject() != null && task.getSubObject().getConstructionObject() != null) {
            notification.setProject(task.getSubObject().getConstructionObject().getProject());
        }
        Notification saved = notificationRepository.save(notification);

        // Publish event for Async delivery
        eventPublisher.publishEvent(new com.example.construction.event.NotificationCreatedEvent(this, saved));
    }

    @Transactional(readOnly = true)
    public List<Notification> getUnreadNotifications(Long userId) {
        return notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);
    }

    @Transactional
    public void markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        notification.setRead(true);
        notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public List<Notification> getAllNotifications(Long userId) {
        // In a real app, use Pageable to limit results (e.g., top 50)
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        List<Notification> unread = notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);
        unread.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(unread);
    }

    @Transactional
    public void deleteNotification(Long id) {
        notificationRepository.deleteById(id);
    }

    @Transactional
    public void deleteAllNotifications(Long userId) {
        List<Notification> all = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
        notificationRepository.deleteAll(all);
    }
}
