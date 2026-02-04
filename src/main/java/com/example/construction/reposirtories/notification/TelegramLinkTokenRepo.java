package com.example.construction.reposirtories.notification;

import com.example.construction.model.TelegramLinkToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface TelegramLinkTokenRepo extends JpaRepository<TelegramLinkToken, Long> {
    Optional<TelegramLinkToken> findByTokenAndUsedFalse(String token);
    void deleteByExpiresAtBefore(LocalDateTime now);

}
