-- V1__core_tables.sql
-- Flyway migration: core schema for ProdSight
-- Notes:
-- - No DROP TABLE statements (Flyway migrations are forward-only)
-- - Uses pgcrypto for gen_random_uuid()

CREATE EXTENSION IF NOT EXISTS pgcrypto;

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
CREATE TABLE IF NOT EXISTS activity_events (
  id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id          UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  type             TEXT NOT NULL,
  source           TEXT NOT NULL DEFAULT 'manual',
  started_at       TIMESTAMPTZ NOT NULL,
  ended_at         TIMESTAMPTZ NULL,
  duration_seconds BIGINT NOT NULL DEFAULT 0,
  tags             JSONB NOT NULL DEFAULT '[]'::jsonb,
  metadata         JSONB NOT NULL DEFAULT '{}'::jsonb,
  created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  CONSTRAINT activity_events_time_chk CHECK (ended_at IS NULL OR ended_at >= started_at),
  CONSTRAINT activity_events_duration_chk CHECK (duration_seconds >= 0),
  CONSTRAINT activity_events_tags_is_array_chk CHECK (jsonb_typeof(tags) = 'array'),
  CONSTRAINT activity_events_metadata_is_object_chk CHECK (jsonb_typeof(metadata) = 'object')
);

CREATE INDEX IF NOT EXISTS idx_activity_events_user_started_at
  ON activity_events (user_id, started_at DESC);

CREATE INDEX IF NOT EXISTS idx_activity_events_user_type_started_at
  ON activity_events (user_id, type, started_at DESC);

CREATE INDEX IF NOT EXISTS idx_activity_events_tags_gin
  ON activity_events USING GIN (tags);

CREATE INDEX IF NOT EXISTS idx_activity_events_metadata_gin
  ON activity_events USING GIN (metadata);

CREATE OR REPLACE FUNCTION activity_events_set_duration()
RETURNS TRIGGER AS $$
BEGIN
  IF NEW.ended_at IS NULL THEN
    NEW.duration_seconds := 0;
  ELSE
    NEW.duration_seconds := GREATEST(
      0,
      FLOOR(EXTRACT(EPOCH FROM (NEW.ended_at - NEW.started_at)))::bigint
    );
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
CREATE TABLE IF NOT EXISTS idempotency_keys (
  id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  idempotency_key TEXT NOT NULL,
  request_hash    TEXT NOT NULL,
  response_code   INTEGER NOT NULL,
  response_body   JSONB NULL,
  created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  CONSTRAINT idempotency_key_len_chk CHECK (char_length(idempotency_key) BETWEEN 8 AND 200)
);

CREATE UNIQUE INDEX IF NOT EXISTS idempotency_keys_user_key_uq
  ON idempotency_keys (user_id, idempotency_key);

CREATE INDEX IF NOT EXISTS idx_idempotency_keys_created_at
  ON idempotency_keys (created_at DESC);
