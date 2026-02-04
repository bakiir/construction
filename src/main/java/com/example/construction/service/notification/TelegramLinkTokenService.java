package com.example.construction.service.notification;

import com.example.construction.model.TelegramLinkToken;
import com.example.construction.model.User;
import com.example.construction.reposirtories.notification.TelegramLinkTokenRepo;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TelegramLinkTokenService {

    private final TelegramLinkTokenRepo repository;
    private static final int TOKEN_TTL_MINUTES = 10;
    private static final SecureRandom RANDOM = new SecureRandom();


    @Transactional
    public TelegramLinkToken generate(User user){
        TelegramLinkToken token = new TelegramLinkToken();
        token.setUser(user);
        token.setToken(generateCode());
        token.setExpiresAt(
                LocalDateTime.now().plusMinutes(TOKEN_TTL_MINUTES)
        );
        token.setUsed(true);

        return repository.save(token);
    }

    private String generateCode(){
        int number = RANDOM.nextInt(900_100)+100_000;
        return "TG"+number;
    }

}
