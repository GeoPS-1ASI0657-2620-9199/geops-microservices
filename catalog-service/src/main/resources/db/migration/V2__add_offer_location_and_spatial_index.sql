ALTER TABLE offers ADD COLUMN location geography(Point, 4326);

CREATE INDEX ix_offers_location ON offers USING GIST (location);
