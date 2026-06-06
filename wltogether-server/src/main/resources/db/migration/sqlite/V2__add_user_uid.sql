-- V2: Add uid column to users
-- NOTE: SQLite ALTER TABLE ADD COLUMN does NOT support UNIQUE constraint;
-- we add the column first then create a unique index separately.
ALTER TABLE users ADD COLUMN uid VARCHAR(8);

-- Generate UIDs for existing users (8-digit random number from id)
-- SQLite doesn't have a good random function, use id-based generation
UPDATE users SET uid = SUBSTR('00000000' || CAST((id * 2654435761 % 90000000 + 10000000) AS TEXT), -8, 8)
WHERE uid IS NULL;

-- Enforce uniqueness via a unique index (SQLite workaround for ADD COLUMN UNIQUE)
CREATE UNIQUE INDEX idx_users_uid ON users(uid);
