package com.prodsight.api.outbox.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prodsight.api.config.AppProps;
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

  private volatile String queueUrl;
  private final String lockId;
  private final ObjectMapper om;
  private final AppProps props;

  public OutboxPublisher(
      OutboxEventRepository repo,
      SqsClient sqs,
      ObjectMapper om,
      AppProps props
  ) {
    this.repo = repo;
    this.sqs = sqs;
    this.om = om; 
    this.props = props;
    
    this.batchSize = props.outbox() != null ? props.outbox().batchSize() : 20;
    this.lockId = (props.instanceId() == null || props.instanceId().isBlank()) ? "local" : props.instanceId();

    // Don’t force-resolve at startup; just store if provided
    if (props.sqs() != null && props.sqs().queueUrl() != null && !props.sqs().queueUrl().isBlank()) {
      this.queueUrl = props.sqs().queueUrl();
    } else {
      this.queueUrl = null; // will resolve from queueName when first needed
    }
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
    	String queueUrl = getQueueUrl();
    	
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
  }

  private String getQueueUrl() {
	if (queueUrl != null) return queueUrl;
	
	var sqsProps = props.sqs();
	if (sqsProps == null || sqsProps.queueName() == null || sqsProps.queueName().isBlank()) {
	  throw new IllegalStateException("Missing SQS configuration. Set app.sqs.queueUrl (AWS) or app.sqs.queueName (LocalStack/AWS).");
	}
	
	// Resolve once and cache
	String resolved = sqs.getQueueUrl(r -> r.queueName(sqsProps.queueName())).queueUrl();
	this.queueUrl = resolved;
	return resolved;
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
