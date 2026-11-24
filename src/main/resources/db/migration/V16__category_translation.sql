-- Migration: translated category names (F4-D, internationalisation)
-- A category keeps its base name in directory_site.category.name; each row here is the name in one locale
-- (BCP 47 tag as the API resolves it: 'en', 'es', 'fr-CA'). GET /api/v1/categories* answers the name in the request
-- locale (Accept-Language) and falls back to the base name when there is no row.
-- Idempotent: apply by hand (Flyway is not enabled). Only the directory_site schema is touched.

CREATE TABLE IF NOT EXISTS directory_site.category_translation (
    id          UUID         NOT NULL DEFAULT directory_site.uuid_generate_v4(),
    category_fk UUID         NOT NULL,
    locale      VARCHAR(10)  NOT NULL,
    name        VARCHAR(255) NOT NULL,
    CONSTRAINT pk_category_translation PRIMARY KEY (id),
    CONSTRAINT fk_category_translation_category FOREIGN KEY (category_fk)
        REFERENCES directory_site.category (id) ON DELETE CASCADE,
    CONSTRAINT uq_category_translation UNIQUE (category_fk, locale)
);

-- Seed: intentionally NOT included. The names of the categories live in the database, not in this repository, so Spanish
-- and fr-CA names cannot be derived safely here. Load them by hand once the catalogue is known, for example:
--
-- INSERT INTO directory_site.category_translation (category_fk, locale, name)
-- SELECT c.id, 'es', 'Restaurantes' FROM directory_site.category c WHERE c.name = 'Restaurants'
-- ON CONFLICT (category_fk, locale) DO NOTHING;
-- INSERT INTO directory_site.category_translation (category_fk, locale, name)
-- SELECT c.id, 'fr-CA', 'Restaurants' FROM directory_site.category c WHERE c.name = 'Restaurants'
-- ON CONFLICT (category_fk, locale) DO NOTHING;
