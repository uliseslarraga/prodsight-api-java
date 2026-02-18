package com.prodsight.api.events.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface ActivityEventRepository extends JpaRepository<ActivityEventEntity, UUID> {

  Page<ActivityEventEntity> findByUserIdAndStartedAtBetween(UUID userId, Instant from, Instant to, Pageable pageable);

  Optional<ActivityEventEntity> findByIdAndUserId(UUID eventId, UUID userId);
}
