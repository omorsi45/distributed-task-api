package com.taskapi.service;

import com.taskapi.domain.IdempotencyKey;
import com.taskapi.repository.IdempotencyKeyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class IdempotencyService {

    private final IdempotencyKeyRepository repository;

    @Value("${app.idempotency.ttl-hours:24}")
    private int ttlHours;

    @Transactional
    public Optional<UUID> findExistingResponseTaskId(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return Optional.empty();
        }
        String hash = hashKey(idempotencyKey);
        return repository.findByKeyHash(hash)
            .filter(k -> k.getExpiresAt().isAfter(Instant.now()))
            .map(IdempotencyKey::getResponseTaskId)
            .filter(id -> id != null);
    }

    @Transactional
    public void storeKey(String idempotencyKey, UUID responseTaskId) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return;
        }
        String hash = hashKey(idempotencyKey);
        Instant expiresAt = Instant.now().plusSeconds(ttlHours * 3600L);
        IdempotencyKey key = IdempotencyKey.builder()
            .keyHash(hash)
            .responseTaskId(responseTaskId)
            .expiresAt(expiresAt)
            .build();
        repository.save(key);
        log.debug("Stored idempotency key for task {}", responseTaskId);
    }

    @Scheduled(cron = "${app.idempotency.cleanup-cron:0 0 * * * ?}") // hourly
    @Transactional
    public void cleanupExpired() {
        repository.deleteByExpiresAtBefore(Instant.now());
    }

    private static String hashKey(String key) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(key.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
