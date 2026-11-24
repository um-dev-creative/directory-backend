-- Migration: business moderation (S8) and ownership verification
-- Idempotent: apply by hand. Existing businesses stay public (DEFAULT 'APPROVED'); nothing changes until
-- umdc.moderation.enabled=true, which makes NEW businesses start as PENDING.
-- Only the directory_site schema is touched.

ALTER TABLE directory_site.business
  ADD COLUMN IF NOT EXISTS moderation_status VARCHAR(16) NOT NULL DEFAULT 'APPROVED';

ALTER TABLE directory_site.business
  ADD COLUMN IF NOT EXISTS verification_code_hash VARCHAR(64) NULL;
ALTER TABLE directory_site.business
  ADD COLUMN IF NOT EXISTS verification_expires_at TIMESTAMP NULL;
ALTER TABLE directory_site.business
  ADD COLUMN IF NOT EXISTS verification_attempts INTEGER NULL;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_business_moderation_status'
                      AND conrelid = 'directory_site.business'::regclass) THEN
        ALTER TABLE directory_site.business ADD CONSTRAINT ck_business_moderation_status
            CHECK (moderation_status IN ('PENDING', 'APPROVED', 'REJECTED'));
    END IF;
END $$;

-- Public listings filter by status when moderation is on.
CREATE INDEX IF NOT EXISTS idx_business_moderation_status ON directory_site.business (moderation_status);
