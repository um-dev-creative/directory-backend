-- Migration: saved searches and last search of a consumer (RF-08)
-- Parameters are plain columns (no JSON) so that they can be validated and replayed against /businesses/search, /nearby and /map.
-- Idempotent: apply by hand (Flyway is not enabled).

CREATE TABLE IF NOT EXISTS directory_site.saved_search (
    id           UUID         NOT NULL DEFAULT directory_site.uuid_generate_v4(),
    user_fk      UUID         NOT NULL REFERENCES general."user" (id) ON DELETE CASCADE,
    name         VARCHAR(80)  NOT NULL,
    q            VARCHAR(100),
    category_id  UUID,
    city_id      UUID,
    city         VARCHAR(100),
    lat          DOUBLE PRECISION,
    lng          DOUBLE PRECISION,
    radius_m     INTEGER,
    bbox         VARCHAR(100),
    sort         VARCHAR(10),
    created_at   TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at   TIMESTAMP    NOT NULL DEFAULT now(),
    last_used_at TIMESTAMP,
    CONSTRAINT pk_saved_search PRIMARY KEY (id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_saved_search_user_name ON directory_site.saved_search (user_fk, lower(name));

-- One row per user: the last search the user ran (upsert).
CREATE TABLE IF NOT EXISTS directory_site.last_search (
    user_fk     UUID         NOT NULL REFERENCES general."user" (id) ON DELETE CASCADE,
    q           VARCHAR(100),
    category_id UUID,
    city_id     UUID,
    city        VARCHAR(100),
    lat         DOUBLE PRECISION,
    lng         DOUBLE PRECISION,
    radius_m    INTEGER,
    bbox        VARCHAR(100),
    sort        VARCHAR(10),
    updated_at  TIMESTAMP    NOT NULL DEFAULT now(),
    CONSTRAINT pk_last_search PRIMARY KEY (user_fk)
);
