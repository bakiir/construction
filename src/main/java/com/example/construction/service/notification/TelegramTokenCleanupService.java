package com.example.construction.service.notification;


import com.example.construction.reposirtories.notification.TelegramLinkTokenRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TelegramTokenCleanupService {
    private final TelegramLinkTokenRepo repository;

    @Scheduled(cron = "0 */30 * * * *")
    public void cleanupExpiredTokens() {
        repository.deleteByExpiresAtBefore(LocalDateTime.now());
    }
}
