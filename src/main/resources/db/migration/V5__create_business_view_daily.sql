-- Migration: daily view counter per business (statistics)
-- One row per business and UTC day. It stores only a number: no user id, IP address, user agent or any other personal data.

CREATE TABLE IF NOT EXISTS directory_site.business_view_daily (
    business_fk UUID    NOT NULL REFERENCES directory_site.business (id) ON DELETE CASCADE,
    view_date   DATE    NOT NULL,
    views       BIGINT  NOT NULL DEFAULT 0,
    CONSTRAINT pk_business_view_daily PRIMARY KEY (business_fk, view_date),
    CONSTRAINT ck_business_view_daily_views CHECK (views >= 0)
);

-- Range reads of one business (the statistics summary) use the primary key (business_fk, view_date): no extra index needed.
