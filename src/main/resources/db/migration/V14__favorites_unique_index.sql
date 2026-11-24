-- Migration: one active favorite per user and target, and exactly one target per favorite
-- Closes the concurrency gap documented in docs/technical/f0-favorites.md. Idempotent: apply by hand.
-- Columns of directory_site.user_favorite: user_fk, business_fk (store), product_fk, campaign_fk (offer), active, created_at.
--
-- 0) Report duplicates BEFORE running (read-only). Every row returned is an active favorite that is not the newest
--    of its user and target; step 1 marks exactly those rows inactive:
--
--   SELECT f.user_fk, f.business_fk, f.product_fk, f.campaign_fk, count(*) AS active_rows
--     FROM directory_site.user_favorite f
--    WHERE f.active
--    GROUP BY f.user_fk, f.business_fk, f.product_fk, f.campaign_fk
--   HAVING count(*) > 1;
--
-- 0b) Report rows that do not have exactly one target (the CHECK below would reject them; fix them by hand):
--
--   SELECT id, user_fk FROM directory_site.user_favorite
--    WHERE num_nonnulls(business_fk, product_fk, campaign_fk) <> 1;

-- 1) De-duplicate WITHOUT deleting: older duplicates are marked inactive (soft delete), the newest stays active.
UPDATE directory_site.user_favorite f
   SET active = false, deleted_at = COALESCE(f.deleted_at, now()), updated_at = now()
 WHERE f.active
   AND EXISTS (SELECT 1 FROM directory_site.user_favorite n
                WHERE n.user_fk = f.user_fk AND n.active
                  AND n.business_fk IS NOT DISTINCT FROM f.business_fk
                  AND n.product_fk  IS NOT DISTINCT FROM f.product_fk
                  AND n.campaign_fk IS NOT DISTINCT FROM f.campaign_fk
                  AND (n.created_at > f.created_at OR (n.created_at = f.created_at AND n.id > f.id)));

-- 2) One active favorite per user and target (partial unique indexes).
CREATE UNIQUE INDEX IF NOT EXISTS uq_fav_user_business ON directory_site.user_favorite (user_fk, business_fk)
  WHERE business_fk IS NOT NULL AND active;
CREATE UNIQUE INDEX IF NOT EXISTS uq_fav_user_product ON directory_site.user_favorite (user_fk, product_fk)
  WHERE product_fk IS NOT NULL AND active;
CREATE UNIQUE INDEX IF NOT EXISTS uq_fav_user_campaign ON directory_site.user_favorite (user_fk, campaign_fk)
  WHERE campaign_fk IS NOT NULL AND active;

-- 3) Exactly one item reference per row. NOT VALID first so existing rows do not block the deployment; the
--    constraint is enforced for every new or updated row. Run VALIDATE once step 0b returns no rows.
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_fav_one_target'
                      AND conrelid = 'directory_site.user_favorite'::regclass) THEN
        ALTER TABLE directory_site.user_favorite ADD CONSTRAINT ck_fav_one_target
            CHECK (num_nonnulls(business_fk, product_fk, campaign_fk) = 1) NOT VALID;
    END IF;
END $$;
-- ALTER TABLE directory_site.user_favorite VALIDATE CONSTRAINT ck_fav_one_target;
