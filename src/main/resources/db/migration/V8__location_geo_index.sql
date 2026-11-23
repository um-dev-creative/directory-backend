-- Migration: partial index for the geographic search (nearby and map): bbox pre-filter on the coordinates
CREATE INDEX IF NOT EXISTS idx_location_geo ON directory_site.location (latitude, longitude) WHERE latitude IS NOT NULL;
