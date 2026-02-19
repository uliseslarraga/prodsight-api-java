package com.prodsight.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProps(
    Outbox outbox,
    Sqs sqs,
    String instanceId
) {
  public record Outbox(int batchSize) {}
  public record Sqs(String queueUrl, String queueName) {}
}