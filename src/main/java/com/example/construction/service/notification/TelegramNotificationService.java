package com.example.construction.service.notification;

import com.example.construction.bot.TelegramBot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TelegramNotificationService {
    private final TelegramBot telegramBot;


}
