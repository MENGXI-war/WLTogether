-- WLTogether Schema V1: SQLite

CREATE TABLE users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    email VARCHAR(255) UNIQUE NOT NULL,
    username VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    nickname VARCHAR(100),
    avatar_url TEXT,
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    public_key TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT (datetime('now'))
);

CREATE TABLE groups_table (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name VARCHAR(100) NOT NULL,
    owner_id BIGINT NOT NULL REFERENCES users(id),
    description VARCHAR(500),
    avatar_url TEXT,
    announcement VARCHAR(500),
    last_message_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT (datetime('now'))
);

CREATE TABLE group_members (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    group_id BIGINT NOT NULL REFERENCES groups_table(id),
    user_id BIGINT NOT NULL REFERENCES users(id),
    role VARCHAR(20) NOT NULL DEFAULT 'MEMBER',
    nickname_in_group VARCHAR(50),
    unread_count INT NOT NULL DEFAULT 0,
    muted_until TIMESTAMP,
    last_read_at TIMESTAMP,
    joined_at TIMESTAMP NOT NULL DEFAULT (datetime('now')),
    UNIQUE (group_id, user_id)
);

CREATE TABLE sessions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    group_id BIGINT NOT NULL REFERENCES groups_table(id),
    host_id BIGINT NOT NULL REFERENCES users(id),
    file_hash VARCHAR(64) NOT NULL,
    file_name VARCHAR(500),
    file_size BIGINT,
    duration_seconds INT,
    media_type VARCHAR(10) NOT NULL,
    video_codec VARCHAR(50),
    audio_codec VARCHAR(50),
    magnet_uri TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'CREATED',
    started_at TIMESTAMP,
    ended_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT (datetime('now'))
);

CREATE TABLE session_participants (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    session_id BIGINT NOT NULL REFERENCES sessions(id),
    user_id BIGINT NOT NULL REFERENCES users(id),
    file_status VARCHAR(20) NOT NULL DEFAULT 'LOCAL',
    joined_at TIMESTAMP NOT NULL DEFAULT (datetime('now')),
    UNIQUE (session_id, user_id)
);

CREATE TABLE playlists (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    group_id BIGINT NOT NULL REFERENCES groups_table(id),
    name VARCHAR(200) NOT NULL,
    creator_id BIGINT NOT NULL REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT (datetime('now'))
);

CREATE TABLE playlist_tracks (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    playlist_id BIGINT NOT NULL REFERENCES playlists(id),
    file_hash VARCHAR(64),
    file_name VARCHAR(500),
    duration_seconds INT,
    artist VARCHAR(200),
    album VARCHAR(200),
    sort_order INT NOT NULL DEFAULT 0
);

CREATE TABLE chat_messages (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    group_id BIGINT NOT NULL REFERENCES groups_table(id),
    sender_id BIGINT NOT NULL REFERENCES users(id),
    content TEXT,
    message_type VARCHAR(20) NOT NULL DEFAULT 'TEXT',
    reply_to_id BIGINT,
    session_id BIGINT,
    is_pinned BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT (datetime('now'))
);

CREATE TABLE message_reactions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    message_id BIGINT NOT NULL REFERENCES chat_messages(id),
    user_id BIGINT NOT NULL REFERENCES users(id),
    emoji VARCHAR(10) NOT NULL,
    UNIQUE (message_id, user_id)
);

CREATE TABLE playback_history (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    session_id BIGINT NOT NULL REFERENCES sessions(id),
    position_seconds INT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP NOT NULL DEFAULT (datetime('now'))
);

CREATE TABLE file_metadata (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    file_hash VARCHAR(64) UNIQUE NOT NULL,
    file_name VARCHAR(500),
    file_size BIGINT,
    duration_seconds INT,
    media_type VARCHAR(10),
    video_codec VARCHAR(50),
    audio_codec VARCHAR(50),
    first_shared_by BIGINT REFERENCES users(id),
    share_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT (datetime('now'))
);

CREATE TABLE announcements (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    title VARCHAR(200) NOT NULL,
    content TEXT,
    created_by BIGINT NOT NULL REFERENCES users(id),
    is_pinned BOOLEAN NOT NULL DEFAULT FALSE,
    is_published BOOLEAN NOT NULL DEFAULT FALSE,
    published_at TIMESTAMP,
    expired_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT (datetime('now'))
);

CREATE TABLE wallpapers (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    image_url TEXT NOT NULL,
    title VARCHAR(200),
    description TEXT,
    publish_date DATE,
    created_by BIGINT NOT NULL REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT (datetime('now'))
);

CREATE TABLE error_logs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    error_type VARCHAR(100),
    message TEXT,
    stack_trace TEXT,
    source VARCHAR(20) NOT NULL DEFAULT 'SERVER',
    severity VARCHAR(20) NOT NULL DEFAULT 'ERROR',
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    handled_by_degradation BOOLEAN NOT NULL DEFAULT FALSE,
    resolution_note TEXT,
    occurred_at TIMESTAMP NOT NULL DEFAULT (datetime('now')),
    resolved_at TIMESTAMP
);

CREATE TABLE server_config (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    config_key VARCHAR(100) UNIQUE NOT NULL,
    config_value TEXT,
    value_type VARCHAR(20) NOT NULL DEFAULT 'STRING',
    description VARCHAR(255),
    updated_by BIGINT REFERENCES users(id),
    updated_at TIMESTAMP NOT NULL DEFAULT (datetime('now'))
);

-- Default server config values
INSERT INTO server_config (config_key, config_value, value_type, description) VALUES
    ('transfer.server_relay.enabled', 'false', 'BOOL', 'Server relay transfer switch'),
    ('transfer.server_relay.max_file_mb', '20480', 'INT', 'Max file size for server relay (MB)'),
    ('transfer.upload.max_per_minute', '5', 'INT', 'Max uploads per minute'),
    ('storage.temp_retention_hours', '24', 'INT', 'Temp file retention hours'),
    ('session.max_participants', '20', 'INT', 'Max session participants');

-- Indexes
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_group_members_group ON group_members(group_id);
CREATE INDEX idx_group_members_user ON group_members(user_id);
CREATE INDEX idx_sessions_group ON sessions(group_id);
CREATE INDEX idx_chat_messages_group ON chat_messages(group_id);
CREATE INDEX idx_chat_messages_created ON chat_messages(created_at);
CREATE INDEX idx_file_metadata_hash ON file_metadata(file_hash);
CREATE INDEX idx_error_logs_status ON error_logs(status);
CREATE INDEX idx_server_config_key ON server_config(config_key);
