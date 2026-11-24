-- Migration: favorite lists of a consumer (RF-11)
-- A user keeps up to 20 named lists (enforced by the application); names are unique per user ignoring case.
-- A favorite (store or product) may be in several lists; deleting a list or a favorite row removes its items.
-- Idempotent: apply by hand (Flyway is not enabled).
-- user_fk is a plain UUID: users belong to Backbone; directory never references another schema.

CREATE TABLE IF NOT EXISTS directory_site.favorite_list (
    id         UUID        NOT NULL DEFAULT directory_site.uuid_generate_v4(),
    user_fk    UUID        NOT NULL,
    name       VARCHAR(80) NOT NULL,
    created_at TIMESTAMP   NOT NULL DEFAULT now(),
    updated_at TIMESTAMP   NOT NULL DEFAULT now(),
    CONSTRAINT pk_favorite_list PRIMARY KEY (id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_favorite_list_user_name ON directory_site.favorite_list (user_fk, lower(name));

CREATE TABLE IF NOT EXISTS directory_site.favorite_list_item (
    id          UUID      NOT NULL DEFAULT directory_site.uuid_generate_v4(),
    list_fk     UUID      NOT NULL,
    favorite_fk UUID      NOT NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT pk_favorite_list_item PRIMARY KEY (id),
    CONSTRAINT fk_favorite_list_item_list FOREIGN KEY (list_fk)
        REFERENCES directory_site.favorite_list (id) ON DELETE CASCADE,
    CONSTRAINT fk_favorite_list_item_favorite FOREIGN KEY (favorite_fk)
        REFERENCES directory_site.user_favorite (id) ON DELETE CASCADE,
    CONSTRAINT uq_favorite_list_item UNIQUE (list_fk, favorite_fk)
);

CREATE INDEX IF NOT EXISTS idx_favorite_list_item_favorite ON directory_site.favorite_list_item (favorite_fk);
