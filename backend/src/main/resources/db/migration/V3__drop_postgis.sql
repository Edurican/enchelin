-- PostGIS 의존성 제거 (Railway PostgreSQL 미지원)
DROP INDEX IF EXISTS idx_restaurant_location;
ALTER TABLE restaurants DROP COLUMN IF EXISTS location;
DROP EXTENSION IF EXISTS postgis;
