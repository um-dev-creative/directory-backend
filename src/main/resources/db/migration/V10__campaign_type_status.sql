-- Migration: campaign type and lifecycle status (RF-03). Idempotent; apply by hand (Flyway is off by default).
-- type: PROMOTION | EVENT | ANNOUNCEMENT. status: only DRAFT is stored; SCHEDULED, ACTIVE, PAUSED and EXPIRED are computed
-- from active, start_date and end_date, so any other stored status (old free text such as ACTIVE or INACTIVE) is cleared.

ALTER TABLE directory_site.campaign ADD COLUMN IF NOT EXISTS type   VARCHAR(50);
ALTER TABLE directory_site.campaign ADD COLUMN IF NOT EXISTS status VARCHAR(12);

UPDATE directory_site.campaign SET type = 'PROMOTION'
 WHERE type IS NULL OR upper(type) NOT IN ('PROMOTION', 'EVENT', 'ANNOUNCEMENT');
UPDATE directory_site.campaign SET type = upper(type) WHERE type <> upper(type);
UPDATE directory_site.campaign SET status = NULL WHERE status IS NOT NULL AND status <> 'DRAFT';

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_campaign_type') THEN
        ALTER TABLE directory_site.campaign
            ADD CONSTRAINT ck_campaign_type CHECK (type IN ('PROMOTION', 'EVENT', 'ANNOUNCEMENT'));
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_campaign_status') THEN
        ALTER TABLE directory_site.campaign
            ADD CONSTRAINT ck_campaign_status CHECK (status IS NULL OR status = 'DRAFT');
    END IF;
END $$;

ALTER TABLE directory_site.campaign ALTER COLUMN type SET DEFAULT 'PROMOTION';

CREATE INDEX IF NOT EXISTS idx_campaign_business_active_end ON directory_site.campaign (business_fk, active, end_date);
