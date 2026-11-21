-- Migration: add soft-delete audit fields to user_favorite
-- Adds deleted_at TIMESTAMP and deleted_by UUID for audit of soft-deletes

ALTER TABLE directory_site.user_favorite
  ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP NULL,
  ADD COLUMN IF NOT EXISTS deleted_by UUID NULL;

-- No data migration required. New columns are nullable to avoid blocking deployments.

