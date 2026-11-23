-- Migration: aggregated click events per business, campaign, type and UTC day (RF-10)
-- It stores only a counter: no user id, IP address, user agent or any other personal data, and no per-click rows.
-- campaign_id holds the nil UUID (00000000-0000-0000-0000-000000000000) when the event is not tied to a campaign, so it can be part of
-- the primary key; it is not a foreign key for that reason (the service checks that the campaign belongs to the business).

CREATE TABLE IF NOT EXISTS directory_site.business_event_daily (
    business_fk  UUID        NOT NULL REFERENCES directory_site.business (id) ON DELETE CASCADE,
    campaign_id  UUID        NOT NULL DEFAULT '00000000-0000-0000-0000-000000000000',
    event_type   VARCHAR(16) NOT NULL,
    event_date   DATE        NOT NULL,
    event_count  BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT pk_business_event_daily PRIMARY KEY (business_fk, event_date, campaign_id, event_type),
    CONSTRAINT ck_business_event_daily_type CHECK (event_type IN ('ROUTE', 'CALL', 'WEB', 'SAVE')),
    CONSTRAINT ck_business_event_daily_count CHECK (event_count >= 0)
);

-- Range reads of one business (the events summary) use the primary key prefix (business_fk, event_date): no extra index needed.
