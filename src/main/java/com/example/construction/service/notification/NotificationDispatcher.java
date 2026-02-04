package com.example.construction.service.notification;

import com.example.construction.bot.TelegramBot;
import com.example.construction.event.NotificationCreatedEvent;
import com.example.construction.model.Notification;
import com.example.construction.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@Slf4j
@RequiredArgsConstructor
public class NotificationDispatcher {

    private final TelegramBot telegramBot;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleNotificationCreated(NotificationCreatedEvent event) {
        Notification notification = event.getNotification();
        User user = notification.getUser();

        // Send to Telegram if connected
        if (user.getTelegramChatId() != null) {
            String text = buildTelegramMessage(notification);
            try {
                telegramBot.sendNotification(user.getTelegramChatId(), text);
            } catch (Exception e) {
                log.error("Failed to send notification to user {}", user.getId(), e);
            }
        }
    }

    private String buildTelegramMessage(Notification n) {
        StringBuilder sb = new StringBuilder();
        if (n.getTitle() != null) {
            sb.append("<b>").append(n.getTitle()).append("</b>\n\n");
        }
        sb.append(n.getMessage());
        return sb.toString();
    }
}
