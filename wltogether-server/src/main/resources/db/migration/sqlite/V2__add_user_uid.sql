-- V2: Add uid column to users
-- SQLite does not support ALTER TABLE ADD COLUMN with UNIQUE constraint
ALTER TABLE users ADD COLUMN uid VARCHAR(8);

-- Generate UIDs for existing users (8-digit random number from id)
-- SQLite doesn't have a good random function, use id-based generation
UPDATE users SET uid = SUBSTR('00000000' || CAST((id * 2654435761 % 90000000 + 10000000) AS TEXT), -8, 8)
WHERE uid IS NULL;

-- Use UNIQUE INDEX instead of column-level UNIQUE constraint (SQLite compatible)
CREATE UNIQUE INDEX idx_users_uid ON users(uid);
