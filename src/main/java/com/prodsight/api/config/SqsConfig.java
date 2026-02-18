package com.prodsight.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;

import java.net.URI;

@Configuration
public class SqsConfig {

  @Bean
  public SqsClient sqsClient(
      @Value("${app.aws.region:us-east-1}") String region,
      @Value("${app.sqs.endpoint:}") String endpoint
  ) {
    var builder = SqsClient.builder()
        .region(Region.of(region))
        .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")));

    // For LocalStack: set endpoint (e.g. http://localhost:4566)
    if (endpoint != null && !endpoint.isBlank()) {
      builder = builder.endpointOverride(URI.create(endpoint));
    }
    return builder.build();
  }
}
