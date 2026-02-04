package com.example.construction.bot;

import com.example.construction.model.User;
import com.example.construction.reposirtories.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.Optional;

@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {

    @Value("${bot.username}")
    private String botUsername;

    @Value("${bot.token}")
    private String botToken;

    private final UserRepository userRepository;

    public TelegramBot(@Value("${bot.token}") String botToken, UserRepository userRepository) {
        super(botToken);
        this.userRepository = userRepository;
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String text = update.getMessage().getText();
            Long chatId = update.getMessage().getChatId();

            if (text.startsWith("/start")) {
                handleStartCommand(chatId, text);
            }
        }
    }

    private void handleStartCommand(Long chatId, String text) {
        String[] parts = text.split(" ");
        if (parts.length == 2) {
            try {
                Long userId = Long.parseLong(parts[1]);
                Optional<User> userOpt = userRepository.findById(userId);

                if (userOpt.isPresent()) {
                    User user = userOpt.get();
                    user.setTelegramChatId(chatId);
                    userRepository.save(user); // Persistence
                    sendMessage(chatId, "✅ Ваш аккаунт успешно привязан! Теперь вы будете получать уведомления здесь.");
                    log.info("Linked user {} to chat {}", userId, chatId);
                } else {
                    sendMessage(chatId, "❌ Пользователь не найден.");
                }
            } catch (NumberFormatException e) {
                sendMessage(chatId, "❌ Неверный формат ссылки.");
            }
        } else {
            sendMessage(chatId,
                    "👋 Привет! Используйте кнопку 'Подключить Telegram' в веб-приложении для начала работы.");
        }
    }

    public void sendNotification(Long chatId, String message) {
        if (chatId == null)
            return;
        sendMessage(chatId, message);
    }

    private void sendMessage(Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Failed to send telegram message to {}", chatId, e);
        }
    }
}
