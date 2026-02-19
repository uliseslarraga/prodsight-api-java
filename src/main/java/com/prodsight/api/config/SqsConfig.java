package com.prodsight.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
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
        .region(Region.of(region));

    // LocalStack only: endpoint override + dummy creds
    if (endpoint != null && !endpoint.isBlank()) {
      builder = builder
          .endpointOverride(URI.create(endpoint))
          .credentialsProvider(
              StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test"))
          );
    } else {
      // AWS (ECS): use task role creds
      builder = builder.credentialsProvider(DefaultCredentialsProvider.create());
    }

    return builder.build();
  }
}