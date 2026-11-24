-- Migration: directory_site must not depend on any other schema (general belongs to Backbone).
-- Drops EVERY foreign key of schema directory_site that references a table in another schema
-- (e.g. business.user_fk, user_favorite.user_fk, digital_contact.contact_type_fk, saved_search.user_fk),
-- whatever its name, and any column default that still calls a function of the general schema.
-- Known to exist in QA (confirmed): business.fk_business_user_1 (-> schema general, table user),
-- digital_contact.fk_contact_business_contact_type_1 (-> schema general, table contact_type) and
-- user_favorite.user_favorite_user_fk_fkey (-> schema general, table user). They are NOT hard-coded below:
-- the block finds every cross-schema foreign key through the catalog, so any other one is dropped too.
-- No data is deleted. Idempotent: running it again finds nothing to drop. Apply by hand with the owner/DDL role.

DO $$
DECLARE
    fk RECORD;
    col RECORD;
BEGIN
    FOR fk IN
        SELECT c.conrelid::regclass AS tbl, c.conname
        FROM pg_constraint c
        JOIN pg_class ref ON ref.oid = c.confrelid
        WHERE c.contype = 'f'
          AND c.connamespace = 'directory_site'::regnamespace
          AND ref.relnamespace <> 'directory_site'::regnamespace
    LOOP
        EXECUTE format('ALTER TABLE %s DROP CONSTRAINT IF EXISTS %I', fk.tbl, fk.conname);
        RAISE NOTICE 'Dropped cross-schema foreign key % on %', fk.conname, fk.tbl;
    END LOOP;

    -- Column defaults calling a function of schema general (directory_site.uuid_generate_v4 is left alone).
    FOR col IN
        SELECT a.attrelid::regclass AS tbl, a.attname
        FROM pg_attrdef d
        JOIN pg_attribute a ON a.attrelid = d.adrelid AND a.attnum = d.adnum
        JOIN pg_class t ON t.oid = d.adrelid
        WHERE t.relnamespace = 'directory_site'::regnamespace
          AND pg_get_expr(d.adbin, d.adrelid) ~* ('(^|[^a-z_])(general|' || chr(34) || 'general' || chr(34) || ')[.]')
    LOOP
        EXECUTE format('ALTER TABLE %s ALTER COLUMN %I DROP DEFAULT', col.tbl, col.attname);
        RAISE NOTICE 'Dropped default of general schema on %.%', col.tbl, col.attname;
    END LOOP;
END
$$;
