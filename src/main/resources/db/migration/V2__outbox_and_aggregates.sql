-- V2__outbox_and_aggregates.sql
-- Flyway migration: worker aggregates + outbox pattern tables

CREATE TABLE IF NOT EXISTS daily_aggregates (
  user_id UUID NOT NULL,
  day TIMESTAMPTZ NOT NULL,
  type TEXT NOT NULL,
  event_count BIGINT NOT NULL DEFAULT 0,
  duration_seconds BIGINT NOT NULL DEFAULT 0,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  PRIMARY KEY (user_id, day, type)
);

CREATE INDEX IF NOT EXISTS idx_daily_aggregates_user_day
  ON daily_aggregates (user_id, day DESC);

CREATE TABLE IF NOT EXISTS outbox_events (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  aggregate_type TEXT NOT NULL,
  aggregate_id UUID NOT NULL,
  event_type TEXT NOT NULL,
  payload JSONB NOT NULL,
  headers JSONB NOT NULL DEFAULT '{}'::jsonb,
  status TEXT NOT NULL DEFAULT 'PENDING',
  attempt_count INT NOT NULL DEFAULT 0,
  available_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  locked_at TIMESTAMPTZ NULL,
  locked_by TEXT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  sent_at TIMESTAMPTZ NULL,
  last_error TEXT NULL
);

CREATE INDEX IF NOT EXISTS idx_outbox_status_available
  ON outbox_events (status, available_at);

CREATE INDEX IF NOT EXISTS idx_outbox_created_at
  ON outbox_events (created_at DESC);
