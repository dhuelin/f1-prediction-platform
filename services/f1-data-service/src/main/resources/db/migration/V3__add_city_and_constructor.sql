-- #162 Add city to races and constructor_name to drivers
-- city is nullable so existing rows are unaffected until populated via admin API / seed data
ALTER TABLE races ADD COLUMN IF NOT EXISTS city VARCHAR(100);
ALTER TABLE drivers ADD COLUMN IF NOT EXISTS constructor_name VARCHAR(100);
