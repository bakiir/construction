package com.example.construction.controllers.notification;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/telegram")
@RequiredArgsConstructor
public class TelegramController {

    @Value("${bot.username}")
    private String botUsername;

    @GetMapping("/config")
    public Map<String, String> getConfig() {
        return Map.of("botUsername", botUsername);
    }
}
