package com.taskapi.repository;

import com.taskapi.domain.IdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, java.util.UUID> {

    Optional<IdempotencyKey> findByKeyHash(String keyHash);

    void deleteByExpiresAtBefore(Instant cutoff);
}
