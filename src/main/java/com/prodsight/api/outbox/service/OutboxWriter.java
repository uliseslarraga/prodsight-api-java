package com.prodsight.api.outbox.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prodsight.api.outbox.persistence.OutboxEventEntity;
import com.prodsight.api.outbox.persistence.OutboxEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
public class OutboxWriter {

  private final OutboxEventRepository repo;
  private final ObjectMapper om;

  public OutboxWriter(OutboxEventRepository repo, ObjectMapper om) {
    this.repo = repo;
    this.om = om;
  }

  @Transactional
  public void enqueue(String aggregateType, UUID aggregateId, String eventType, Object payloadObj) {
    try {
      String payload = om.writeValueAsString(payloadObj);

      OutboxEventEntity e = new OutboxEventEntity();
      e.setAggregateType(aggregateType);
      e.setAggregateId(aggregateId);
      e.setEventType(eventType);
      e.setPayload(om.convertValue(payloadObj, Map.class));
      e.setHeaders(Map.of("contentType", "application/json"));
      e.setStatus("PENDING");
      e.setAvailableAt(Instant.now());

      repo.save(e);
    } catch (Exception ex) {
    	  throw new IllegalStateException(
    	      "Unable to serialize outbox payload: " + ex.getClass().getName() + ": " + ex.getMessage(),
    	      ex
    	  );
    	}

  }
}
