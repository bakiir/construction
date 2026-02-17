package com.example.construction.config;

import com.example.construction.bot.TelegramBot;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Configuration
@RequiredArgsConstructor
public class TelegramBotConfig {

    private final TelegramBot telegramBot;

    @PostConstruct
    public void register() {
        try {
            TelegramBotsApi api = new TelegramBotsApi(DefaultBotSession.class);
            api.registerBot(telegramBot);
        } catch (TelegramApiException e) {
            // Log error but do not crash the application
            // Use a logger if available, or print stack trace for now
            e.printStackTrace(); 
        }
    }
}
