-- Migration: add profile_image_ref to business
-- Stores the Cloudflare R2 public URL (or object key) for the business profile image.

ALTER TABLE directory_site.business
  ADD COLUMN IF NOT EXISTS profile_image_ref VARCHAR(512) NULL;

-- No data migration required. New column is nullable; existing records remain valid.
