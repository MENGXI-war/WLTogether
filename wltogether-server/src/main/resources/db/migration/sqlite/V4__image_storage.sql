-- V4: Image storage support
ALTER TABLE chat_messages ADD COLUMN expires_at TIMESTAMP;
ALTER TABLE groups_table ADD COLUMN storage_used_bytes BIGINT NOT NULL DEFAULT 0;
ALTER TABLE chat_messages ADD COLUMN image_thumbnail_url TEXT;

-- Add storage config defaults
INSERT OR IGNORE INTO server_config (config_key, config_value, value_type, description) VALUES
    ('storage.max_per_group_mb', '1024', 'INT', 'Max storage per group (MB)'),
    ('storage.message_retention_days', '7', 'INT', 'Message file retention days');
