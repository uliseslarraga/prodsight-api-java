package com.prodsight.api.outbox.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEventEntity, UUID> {

  @Query(
      value = """
      SELECT * FROM outbox_events
      WHERE status = 'PENDING'
        AND available_at <= :now
      ORDER BY created_at
      FOR UPDATE SKIP LOCKED
      LIMIT :limit
      """,
      nativeQuery = true
  )
  List<OutboxEventEntity> lockNextBatch(@Param("now") Instant now, @Param("limit") int limit);
}
