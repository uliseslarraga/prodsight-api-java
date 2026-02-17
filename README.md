# ProdSight Java API (MVP)

A minimal, enterprise-style **Spring Boot + Postgres** API for tracking
personal activity events and basic stats.

------------------------------------------------------------------------

## What's in the MVP

-   Users
    -   Create user (idempotent by email)
    -   Get user
    -   Update user
-   Activity events
    -   Create event
    -   List events (by time range)
    -   Update event
    -   Delete event
-   Stats (skeleton)
    -   `GET /stats/summary` placeholder implementation
-   Common error model + global exception handler

------------------------------------------------------------------------

## Prerequisites

-   Java 17+
-   Maven 3.9+
-   PostgreSQL 14+ (local or Docker)

------------------------------------------------------------------------

## Database Setup

This project assumes all tables are created in the **default `public`
schema**.

Example:

``` sql
CREATE TABLE users (...);
CREATE TABLE activity_events (...);
CREATE TABLE idempotency_keys (...);
```

Apply schema:

``` bash
psql "postgresql://<user>:<pass>@<host>:5432/<db>" -f schema.sql
```

------------------------------------------------------------------------

## Spring Configuration

Since you're using the default schema, remove:

``` yaml
hibernate:
  default_schema: prodsight
```

### Minimal application.yml

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

management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus
```
------------------------------------------------------------------------

## Run the API

``` bash
mvn spring-boot:run
```

Runs at:

    http://localhost:8080

Health check:

    GET /actuator/health

------------------------------------------------------------------------

## API Base Path

    /api/v1

### Create User

    POST /api/v1/users

### Create Event

    POST /api/v1/users/{userId}/events

