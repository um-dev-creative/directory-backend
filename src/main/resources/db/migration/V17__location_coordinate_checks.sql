-- Migration: coordinate range checks on directory_site.location (D4)
-- The application already validates the range (and a Canada bounding box, property umdc.location.bounds.enforced); these constraints
-- stop a bad value written by any other path. The Canada box stays in the application on purpose: it is configurable and a wider
-- area may be allowed later.
-- Idempotent: each constraint is created only when missing.
-- On a big table, NOT VALID adds the constraint without scanning (new and updated rows are checked at once). Then, in a quiet moment:
--   ALTER TABLE directory_site.location VALIDATE CONSTRAINT ck_location_latitude;
--   ALTER TABLE directory_site.location VALIDATE CONSTRAINT ck_location_longitude;
-- VALIDATE fails if an old row is out of range: find them first with
--   SELECT id, latitude, longitude FROM directory_site.location WHERE latitude NOT BETWEEN -90 AND 90 OR longitude NOT BETWEEN -180 AND 180;
-- A NULL coordinate passes a CHECK, so locations without a point stay valid.

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_location_latitude'
                   AND conrelid = 'directory_site.location'::regclass) THEN
        ALTER TABLE directory_site.location
            ADD CONSTRAINT ck_location_latitude CHECK (latitude BETWEEN -90 AND 90) NOT VALID;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_location_longitude'
                   AND conrelid = 'directory_site.location'::regclass) THEN
        ALTER TABLE directory_site.location
            ADD CONSTRAINT ck_location_longitude CHECK (longitude BETWEEN -180 AND 180) NOT VALID;
    END IF;
END
$$;
