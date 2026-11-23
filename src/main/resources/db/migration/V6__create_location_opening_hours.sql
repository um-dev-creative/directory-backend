-- Migration: opening hours per location (RF-09)
-- One row per time slot: a day may have several slots (e.g. 09:00-13:00 and 15:00-19:00). A closed day is one row with closed = TRUE and no times.
-- Hours are local time of the business time zone. A slot that goes past midnight is modelled as a slot until 23:59 plus a slot on the next day.

CREATE TABLE IF NOT EXISTS directory_site.location_opening_hours (
    id           UUID     NOT NULL DEFAULT directory_site.uuid_generate_v4(),
    location_fk  UUID     NOT NULL REFERENCES directory_site.location (id) ON DELETE CASCADE,
    day_of_week  SMALLINT NOT NULL,
    opens_at     TIME,
    closes_at    TIME,
    closed       BOOLEAN  NOT NULL DEFAULT FALSE,
    CONSTRAINT pk_location_opening_hours PRIMARY KEY (id),
    CONSTRAINT ck_location_opening_hours_day CHECK (day_of_week BETWEEN 1 AND 7),
    CONSTRAINT ck_location_opening_hours_times CHECK (
        (closed AND opens_at IS NULL AND closes_at IS NULL)
        OR (NOT closed AND opens_at IS NOT NULL AND closes_at IS NOT NULL AND opens_at < closes_at)
    )
);

CREATE INDEX IF NOT EXISTS idx_location_opening_hours_location ON directory_site.location_opening_hours (location_fk, day_of_week, opens_at);
