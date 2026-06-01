-- V2: Add uid column to users
ALTER TABLE users ADD COLUMN uid VARCHAR(8) UNIQUE;

-- Generate UIDs for existing users (8-digit random number from id)
-- SQLite doesn't have a good random function, use id-based generation
UPDATE users SET uid = SUBSTR('00000000' || CAST((id * 2654435761 % 90000000 + 10000000) AS TEXT), -8, 8)
WHERE uid IS NULL;

-- Make uid NOT NULL after population
-- SQLite doesn't support ALTER COLUMN, so we rely on application-level enforcement
