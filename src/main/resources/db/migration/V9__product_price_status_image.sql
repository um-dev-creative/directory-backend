-- Migration: product price, image reference and creator (RF-02)
-- price/currency are nullable (a product without price); price has two decimals, currency is an ISO 4217 code.
-- created_by is the user who created the product, so it can be edited before it is linked to a business.

-- Legacy rows: the API created products with active = FALSE by mistake (no way to deactivate existed), which would hide them
-- from the public list. They are activated once, only the first time this script runs (guarded by the price column).
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_schema = 'directory_site' AND table_name = 'product' AND column_name = 'price') THEN
        UPDATE directory_site.product SET active = TRUE WHERE active = FALSE;
    END IF;
END $$;

ALTER TABLE directory_site.product ADD COLUMN IF NOT EXISTS price      NUMERIC(12, 2) NULL;
ALTER TABLE directory_site.product ADD COLUMN IF NOT EXISTS currency   CHAR(3)        NULL;
ALTER TABLE directory_site.product ADD COLUMN IF NOT EXISTS image_ref  VARCHAR(512)   NULL;
ALTER TABLE directory_site.product ADD COLUMN IF NOT EXISTS created_by UUID           NULL;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_product_price') THEN
        ALTER TABLE directory_site.product ADD CONSTRAINT ck_product_price CHECK (
            (price IS NULL AND currency IS NULL) OR (price IS NOT NULL AND price >= 0 AND currency IS NOT NULL));
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_product_active ON directory_site.product (active);
