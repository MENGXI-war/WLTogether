-- V3: Group enhancements - join mode, tags
ALTER TABLE groups_table ADD COLUMN join_mode VARCHAR(20) NOT NULL DEFAULT 'INVITE_ONLY';
ALTER TABLE groups_table ADD COLUMN tags VARCHAR(500);
