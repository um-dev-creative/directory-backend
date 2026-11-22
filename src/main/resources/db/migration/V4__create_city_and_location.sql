-- Migration: cities catalogue and business locations (sedes)
-- city: catalogue the search by city is built on. location: one row per branch of a business, pointing to its city.

CREATE TABLE IF NOT EXISTS directory_site.city (
    id           UUID         PRIMARY KEY DEFAULT directory_site.uuid_generate_v4(),
    name         VARCHAR(100) NOT NULL,
    province     VARCHAR(100) NOT NULL,
    country      VARCHAR(2)   NOT NULL DEFAULT 'CA',
    created_date TIMESTAMP    NOT NULL DEFAULT now(),
    last_update  TIMESTAMP    NOT NULL DEFAULT now(),
    CONSTRAINT uq_city_name_province_country UNIQUE (name, province, country)
);

CREATE INDEX IF NOT EXISTS idx_city_lower_name ON directory_site.city (lower(name));

CREATE TABLE IF NOT EXISTS directory_site.location (
    id           UUID          PRIMARY KEY DEFAULT directory_site.uuid_generate_v4(),
    business_fk  UUID          NOT NULL REFERENCES directory_site.business (id) ON DELETE CASCADE,
    city_fk      UUID          NOT NULL REFERENCES directory_site.city (id),
    name         VARCHAR(120)  NULL,
    address_line VARCHAR(200)  NOT NULL,
    postal_code  VARCHAR(10)   NULL,
    phone        VARCHAR(30)   NULL,
    latitude     NUMERIC(9, 6) NULL,
    longitude    NUMERIC(9, 6) NULL,
    main         BOOLEAN       NOT NULL DEFAULT FALSE,
    created_date TIMESTAMP     NOT NULL DEFAULT now(),
    last_update  TIMESTAMP     NOT NULL DEFAULT now(),
    CONSTRAINT ck_location_latitude  CHECK (latitude  IS NULL OR latitude  BETWEEN -90  AND 90),
    CONSTRAINT ck_location_longitude CHECK (longitude IS NULL OR longitude BETWEEN -180 AND 180),
    CONSTRAINT ck_location_coordinates_pair CHECK ((latitude IS NULL) = (longitude IS NULL))
);

CREATE INDEX IF NOT EXISTS idx_location_business ON directory_site.location (business_fk);
CREATE INDEX IF NOT EXISTS idx_location_city ON directory_site.location (city_fk);
-- At most one main location per business
CREATE UNIQUE INDEX IF NOT EXISTS uq_location_one_main_per_business ON directory_site.location (business_fk) WHERE main;

-- Initial catalogue: Toronto and the surrounding Ontario cities (the launch market)
INSERT INTO directory_site.city (name, province, country) VALUES
    ('Toronto', 'Ontario', 'CA'),
    ('Mississauga', 'Ontario', 'CA'),
    ('Brampton', 'Ontario', 'CA'),
    ('Markham', 'Ontario', 'CA'),
    ('Vaughan', 'Ontario', 'CA'),
    ('Richmond Hill', 'Ontario', 'CA'),
    ('Oakville', 'Ontario', 'CA'),
    ('Hamilton', 'Ontario', 'CA'),
    ('Ottawa', 'Ontario', 'CA')
ON CONFLICT (name, province, country) DO NOTHING;
