# ProdSight Java API -- cURL Workflow Examples

This document provides step‑by‑step **end‑to‑end cURL examples** to test
the Java API locally.

Assumptions:

-   API running at: http://localhost:8080
-   PostgreSQL running
-   LocalStack SQS running at http://localhost:4566
-   Queue name: prodsight-events-queue

------------------------------------------------------------------------

# 0️⃣ Optional -- Reset Local Environment

## Purge SQS Queue

``` bash
QUEUE_URL="http://localhost:4566/000000000000/prodsight-events-queue"

aws --endpoint-url=http://localhost:4566 sqs purge-queue   --queue-url "$QUEUE_URL"
```

## Reset Aggregates (Optional)

``` bash
psql "postgresql://prodsight:prodsight@localhost:5432/prodsight"   -c "TRUNCATE TABLE daily_aggregates;"
```

------------------------------------------------------------------------

# 1️⃣ Create User

``` bash
USER_JSON=$(curl -s -X POST http://localhost:8080/api/v1/users   -H "Content-Type: application/json"   -d '{
    "email":"curl@test.com",
    "displayName":"Curl Test",
    "timeZone":"America/Mexico_City"
  }')

echo "$USER_JSON"
```

Extract user ID:

``` bash
USER_ID=$(echo "$USER_JSON" | python -c 'import sys,json; print(json.load(sys.stdin)["id"])')
echo "USER_ID=$USER_ID"
```

------------------------------------------------------------------------

# 2️⃣ Get User

``` bash
curl -X GET http://localhost:8080/api/v1/users/$USER_ID
```

------------------------------------------------------------------------

# 3️⃣ Create Activity Event

``` bash
EVENT_JSON=$(curl -s -X POST   http://localhost:8080/api/v1/users/$USER_ID/events   -H "Content-Type: application/json"   -d '{
    "type": "CODING",
    "source": "manual",
    "startedAt": "2026-02-17T12:00:00Z",
    "endedAt": "2026-02-17T13:00:00Z",
    "tags": ["curl-test"],
    "metadata": {"origin":"curl"}
  }')

echo "$EVENT_JSON"
```

Extract event ID:

``` bash
EVENT_ID=$(echo "$EVENT_JSON" | python -c 'import sys,json; print(json.load(sys.stdin)["id"])')
echo "EVENT_ID=$EVENT_ID"
```

------------------------------------------------------------------------

# 4️⃣ Verify Database Insert

``` bash
psql "postgresql://prodsight:prodsight@localhost:5432/prodsight" -c "SELECT id, user_id, type, duration_seconds  FROM activity_events  WHERE id='$EVENT_ID';"
```

------------------------------------------------------------------------

# 5️⃣ Verify Outbox Record

``` bash
psql "postgresql://prodsight:prodsight@localhost:5432/prodsight" -c "SELECT id, event_type, status, attempt_count, sent_at, last_error  FROM outbox_events  ORDER BY created_at DESC  LIMIT 5;"
```

Expected lifecycle:

    PENDING → SENT

------------------------------------------------------------------------

# 6️⃣ Confirm SQS Message Exists

``` bash
aws --endpoint-url=http://localhost:4566 sqs receive-message   --queue-url "$QUEUE_URL"   --max-number-of-messages 10
```

If worker is running, it may consume quickly.

------------------------------------------------------------------------

# 7️⃣ Verify Worker Updated Aggregates

``` bash
psql "postgresql://prodsight:prodsight@localhost:5432/prodsight" -c "SELECT user_id, day, type, event_count, duration_seconds  FROM daily_aggregates  WHERE user_id='$USER_ID';"
```

------------------------------------------------------------------------

# 8️⃣ Update Event

``` bash
curl -X PATCH http://localhost:8080/api/v1/users/$USER_ID/events/$EVENT_ID   -H "Content-Type: application/json"   -d '{
    "endedAt": "2026-02-17T14:00:00Z"
  }'
```

------------------------------------------------------------------------

# 9️⃣ Delete Event

``` bash
curl -X DELETE   http://localhost:8080/api/v1/users/$USER_ID/events/$EVENT_ID
```

------------------------------------------------------------------------

# 🔟 Check Stats Endpoint

``` bash
curl -X GET "http://localhost:8080/api/v1/users/$USER_ID/stats/summary?from=2026-02-17T00:00:00Z&to=2026-02-18T00:00:00Z&groupBy=day"
```

------------------------------------------------------------------------

# Troubleshooting

### Outbox Stuck in PENDING

-   Verify @EnableScheduling
-   Verify SQS endpoint config
-   Check last_error column

### Worker Not Updating Aggregates

-   Verify SQS queue
-   Verify DATABASE_URL
-   Purge queue if needed

------------------------------------------------------------------------

# End-to-End Flow Recap

    curl → API
          → activity_events
          → outbox_events
          → SQS
          → Worker
          → daily_aggregates

------------------------------------------------------------------------

# License

Personal DevOps / SRE practice project.
