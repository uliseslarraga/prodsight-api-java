package com.prodsight.api.outbox.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prodsight.api.outbox.persistence.OutboxEventEntity;
import com.prodsight.api.outbox.persistence.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
public class OutboxPublisher {
	
  private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

  private final OutboxEventRepository repo;
  private final SqsClient sqs;

  private final String queueUrl;
  private final String lockId;
  private final ObjectMapper om;

  public OutboxPublisher(
      OutboxEventRepository repo,
      SqsClient sqs,
      ObjectMapper om,
      @Value("${app.outbox.batchSize:20}") int batchSize,
      @Value("${app.sqs.queueName}") String queueName,
      @Value("${app.instanceId:local}") String instanceId
  ) {
    this.repo = repo;
    this.sqs = sqs;
    this.om = om; 
    this.queueUrl = sqs.getQueueUrl(r -> r.queueName(queueName)).queueUrl();
    this.lockId = instanceId;
    this.batchSize = batchSize;
  }

  private final int batchSize;

  @Scheduled(fixedDelayString = "${app.outbox.pollMs:2000}")
  @Transactional
  public void tick() {
    publishBatch();
  }

  public void publishBatch() {
    List<OutboxEventEntity> batch = repo.lockNextBatch(Instant.now(), batchSize);

    for (OutboxEventEntity e : batch) {
      try {
	    log.info("publishing_event id={} status={} attempt={}",
			    e.getId(), e.getStatus(), e.getAttemptCount());
        // mark locked (optional visibility)
        e.setLockedAt(Instant.now());
        e.setLockedBy(lockId);
        String body = om.writeValueAsString(e.getPayload());
        // send
        sqs.sendMessage(SendMessageRequest.builder()
            .queueUrl(queueUrl)
            .messageBody(body)
            .build());

        // mark sent
        e.setStatus("SENT");
        e.setSentAt(Instant.now());
        e.setLastError(null);

      } catch (Exception ex) {
    	log.error("Failed publishing event id={} attempt={}: {}",
    		      e.getId(), e.getAttemptCount(), ex.toString(), ex);
        // retry w/ backoff
        int attempts = e.getAttemptCount() + 1;
        e.setAttemptCount(attempts);
        e.setLastError(truncate(ex.toString(), 2000));

        if (attempts >= 10) {
          e.setStatus("DEAD");
        } else {
          e.setStatus("PENDING");
          e.setAvailableAt(Instant.now().plus(backoff(attempts)));
        }
      }
    }
    // transaction commits: state updates persist
  }

  private Duration backoff(int attempt) {
    // exponential backoff: 1s, 2s, 4s, ... capped at 60s
    long seconds = Math.min(60, (long) Math.pow(2, Math.max(0, attempt - 1)));
    return Duration.ofSeconds(seconds);
  }

  private String truncate(String s, int max) {
    if (s == null) return null;
    return s.length() <= max ? s : s.substring(0, max);
  }
}
