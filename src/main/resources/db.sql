-- prodsight_schema.sql
-- PostgreSQL schema for ProdSight (Personal Analytics API)
-- Safe to run on a fresh database. If you want re-runnable, keep the DROP statements (optional).

-- Optional: uncomment if you want a clean reset in dev
DROP TABLE IF EXISTS idempotency_keys CASCADE;
DROP TABLE IF EXISTS activity_events CASCADE;
DROP TABLE IF EXISTS users CASCADE;

-- UUID generation
CREATE EXTENSION IF NOT EXISTS pgcrypto; -- provides gen_random_uuid()

-- (Optional) keep everything in its own schema
--CREATE SCHEMA IF NOT EXISTS prodsight;
--SET search_path TO prodsight;

-- =========================
-- USERS
-- =========================
CREATE TABLE IF NOT EXISTS users (
  id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  email        TEXT NOT NULL,
  display_name TEXT NOT NULL,
  time_zone    TEXT NOT NULL DEFAULT 'America/Mexico_City',
  created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),

  CONSTRAINT users_email_format_chk CHECK (position('@' in email) > 1)
);

CREATE UNIQUE INDEX IF NOT EXISTS users_email_uq ON users (lower(email));

-- Keep updated_at current
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = NOW();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_users_set_updated_at ON users;
CREATE TRIGGER trg_users_set_updated_at
BEFORE UPDATE ON users
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();

-- =========================
-- ACTIVITY EVENTS
-- =========================
-- Notes:
-- - type is TEXT to keep it flexible; validate allowed values in the app (or later add a lookup table)
-- - tags + metadata are JSONB for flexibility and easy evolution

CREATE TABLE IF NOT EXISTS activity_events (
  id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id          UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,

  type             TEXT NOT NULL,
  source           TEXT NOT NULL DEFAULT 'manual',

  started_at       TIMESTAMPTZ NOT NULL,
  ended_at         TIMESTAMPTZ NULL,

  -- If you store it derived, you can compute in queries; storing helps performance for stats.
  duration_seconds BIGINT NOT NULL DEFAULT 0,

  tags             JSONB NOT NULL DEFAULT '[]'::jsonb,
  metadata         JSONB NOT NULL DEFAULT '{}'::jsonb,

  created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),

  CONSTRAINT activity_events_time_chk CHECK (ended_at IS NULL OR ended_at >= started_at),
  CONSTRAINT activity_events_duration_chk CHECK (duration_seconds >= 0),
  CONSTRAINT activity_events_tags_is_array_chk CHECK (jsonb_typeof(tags) = 'array'),
  CONSTRAINT activity_events_metadata_is_object_chk CHECK (jsonb_typeof(metadata) = 'object')
);

-- Helpful indexes for common queries (by user + time, by user + type + time)
CREATE INDEX IF NOT EXISTS idx_activity_events_user_started_at
  ON activity_events (user_id, started_at DESC);

CREATE INDEX IF NOT EXISTS idx_activity_events_user_type_started_at
  ON activity_events (user_id, type, started_at DESC);

-- JSONB search acceleration (optional, but often handy later)
CREATE INDEX IF NOT EXISTS idx_activity_events_tags_gin
  ON activity_events USING GIN (tags);

CREATE INDEX IF NOT EXISTS idx_activity_events_metadata_gin
  ON activity_events USING GIN (metadata);

-- Ensure duration_seconds matches timestamps if both provided (optional strictness).
-- If you prefer to compute in app only, comment this out.
CREATE OR REPLACE FUNCTION activity_events_set_duration()
RETURNS TRIGGER AS $$
BEGIN
  IF NEW.ended_at IS NULL THEN
    NEW.duration_seconds := 0;
  ELSE
    NEW.duration_seconds := GREATEST(0, FLOOR(EXTRACT(EPOCH FROM (NEW.ended_at - NEW.started_at)))::bigint);
  END IF;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_activity_events_set_duration ON activity_events;
CREATE TRIGGER trg_activity_events_set_duration
BEFORE INSERT OR UPDATE OF started_at, ended_at ON activity_events
FOR EACH ROW
EXECUTE FUNCTION activity_events_set_duration();

-- =========================
-- IDEMPOTENCY KEYS
-- =========================
-- Store idempotency keys for POST create operations.
-- request_hash prevents reusing same key for different payloads.
-- response_body can be stored so you can return identical responses on retries.

CREATE TABLE IF NOT EXISTS idempotency_keys (
  id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id        UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  idempotency_key TEXT NOT NULL,

  request_hash   TEXT NOT NULL,
  response_code  INTEGER NOT NULL,
  response_body  JSONB NULL,

  created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),

  CONSTRAINT idempotency_key_len_chk CHECK (char_length(idempotency_key) BETWEEN 8 AND 200)
);

-- Enforce uniqueness per user + key
CREATE UNIQUE INDEX IF NOT EXISTS idempotency_keys_user_key_uq
  ON idempotency_keys (user_id, idempotency_key);

-- Fast cleanup queries / lookups
CREATE INDEX IF NOT EXISTS idx_idempotency_keys_created_at
  ON idempotency_keys (created_at DESC);

-- =========================
-- OPTIONAL: SIMPLE MATERIALIZED VIEW FOR STATS (future-ish)
-- =========================
-- You can ignore this for MVP; leaving as a placeholder.
-- CREATE MATERIALIZED VIEW prodsight.mv_daily_totals AS
-- SELECT
--   user_id,
--   date_trunc('day', started_at) AS day,
--   type,
--   COUNT(*) AS event_count,
--   SUM(duration_seconds) AS duration_seconds
-- FROM prodsight.activity_events
-- GROUP BY user_id, date_trunc('day', started_at), type;

-- =========================
-- PERMISSIONS (optional)
-- =========================
-- If you want an app role, uncomment and adjust.
-- CREATE ROLE prodsight_app LOGIN PASSWORD 'change-me';
-- GRANT USAGE ON SCHEMA prodsight TO prodsight_app;
-- GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA prodsight TO prodsight_app;
-- ALTER DEFAULT PRIVILEGES IN SCHEMA prodsight GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO prodsight_app;

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

