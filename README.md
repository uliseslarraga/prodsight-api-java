# ProdSight Java API

Spring Boot service for the **ProdSight** system.

This service:

-   Exposes REST endpoints for Users and Activity Events
-   Persists data to PostgreSQL
-   Implements the **Outbox Pattern**
-   Publishes domain events to SQS (LocalStack locally, AWS in cloud)
-   Works together with the Python Worker for async processing

------------------------------------------------------------------------

# Architecture Overview

Client → Java API → PostgreSQL\
↳ Outbox → SQS → Python Worker → Aggregates

The API is responsible for:

-   Validating requests
-   Writing business data
-   Writing outbox records in the same DB transaction
-   Publishing outbox events asynchronously via scheduled job

------------------------------------------------------------------------

# Tech Stack

-   Java 17+
-   Spring Boot 3.x
-   Spring Data JPA
-   PostgreSQL
-   AWS SDK v2 (SQS)
-   Outbox Pattern
-   Docker

------------------------------------------------------------------------

# Local Development

## 1. Start Infrastructure

You need:

-   PostgreSQL (Docker)
-   LocalStack (SQS)

Example:

``` bash
docker compose up -d postgres localstack
```

Verify LocalStack:

``` bash
curl http://localhost:4566/health
```

------------------------------------------------------------------------

## 2. Create SQS Queue

``` bash
aws --endpoint-url=http://localhost:4566 sqs create-queue   --queue-name prodsight-events-queue
```

Get queue URL:

``` bash
aws --endpoint-url=http://localhost:4566 sqs get-queue-url   --queue-name prodsight-events-queue
```

------------------------------------------------------------------------

## 3. Application Configuration

Example `application.yml`:

``` yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/prodsight
    username: prodsight
    password: prodsight
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        jdbc:
          time_zone: UTC

app:
  aws:
    region: us-east-1
  sqs:
    endpoint: http://localhost:4566
    queueUrl: http://localhost:4566/000000000000/prodsight-events-queue
  outbox:
    pollMs: 2000
    batchSize: 20
  instanceId: api-local-1
```

If API runs inside Docker, use:

    endpoint: http://localstack:4566

------------------------------------------------------------------------

# Running the API

Using Maven:

``` bash
mvn spring-boot:run
```

Application runs at:

    http://localhost:8080

------------------------------------------------------------------------

# Endpoints (MVP)

Base path:

    /api/v1

### Create User

    POST /api/v1/users

### Create Activity Event

    POST /api/v1/users/{userId}/events

Creating an event:

-   Inserts into `activity_events`
-   Inserts into `outbox_events`
-   Scheduled publisher sends message to SQS
-   Marks outbox row as SENT

------------------------------------------------------------------------

# Verifying Outbox

After creating an event:

``` sql
SELECT id, status, attempt_count, sent_at, last_error
FROM outbox_events
ORDER BY created_at DESC
LIMIT 1;
```

Expected flow:

    PENDING → SENT

If stuck in PENDING:

-   Verify @EnableScheduling
-   Verify SQS endpoint configuration
-   Check last_error column

------------------------------------------------------------------------

# Database Tables

## activity_events

Primary business table for events.

## outbox_events

Implements Outbox Pattern.

Key columns:

-   status (PENDING, SENT, FAILED, DEAD)
-   attempt_count
-   available_at
-   sent_at
-   last_error

------------------------------------------------------------------------

# Docker

Build:

``` bash
docker build -t prodsight-api .
```

Run:

``` bash
docker run --rm -p 8080:8080   -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/prodsight   -e SPRING_DATASOURCE_USERNAME=prodsight   -e SPRING_DATASOURCE_PASSWORD=prodsight   prodsight-api
```

------------------------------------------------------------------------

# CI

CI pipeline performs:

-   Maven build
-   Unit tests
-   Docker image build
-   Publish to GitHub Container Registry (GHCR)

Image tags:

    ghcr.io/<owner>/<repo>:main
    ghcr.io/<owner>/<repo>:sha-<commit>
    ghcr.io/<owner>/<repo>:vX.Y.Z

------------------------------------------------------------------------

# Production Notes

When migrating to AWS:

-   Remove SQS endpoint override
-   Use IAM roles instead of static credentials
-   Use real AWS SQS queue URL
-   Consider enabling metrics and distributed tracing
-   Add monitoring on outbox lag

------------------------------------------------------------------------

# License

Personal DevOps / SRE practice project.
