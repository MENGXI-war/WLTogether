-- V2: Add uid column to users
ALTER TABLE users ADD COLUMN uid VARCHAR(8) UNIQUE;

-- Generate UIDs for existing users
UPDATE users SET uid = LPAD(FLOOR(RAND() * 90000000 + 10000000), 8, '0')
WHERE uid IS NULL;

-- Make uid NOT NULL
ALTER TABLE users MODIFY COLUMN uid VARCHAR(8) NOT NULL;

CREATE INDEX idx_users_uid ON users(uid);
