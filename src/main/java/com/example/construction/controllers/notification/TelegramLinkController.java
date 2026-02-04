package com.example.construction.controllers.notification;

import com.example.construction.model.TelegramLinkToken;
import com.example.construction.model.User;
import com.example.construction.service.notification.TelegramLinkTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("api/telegram")
@RequiredArgsConstructor
public class TelegramLinkController {

    private final TelegramLinkTokenService tokenService;

    @PostMapping("/link-token")
    public Map<String, Object> generateToken(@AuthenticationPrincipal User user){
        TelegramLinkToken token = tokenService.generate(user);
        return Map.of(
                "token", token.getToken(),
                "expiresAt", token.getExpiresAt()
        );
    }
}
