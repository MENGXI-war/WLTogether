-- WLTogether Schema V1: MySQL

CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    username VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    nickname VARCHAR(100),
    avatar_url TEXT,
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    public_key TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_users_email (email),
    INDEX idx_users_username (username)
);

CREATE TABLE groups_table (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    owner_id BIGINT NOT NULL,
    description VARCHAR(500),
    avatar_url TEXT,
    announcement VARCHAR(500),
    last_message_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (owner_id) REFERENCES users(id)
);

CREATE TABLE group_members (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    group_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'MEMBER',
    nickname_in_group VARCHAR(50),
    unread_count INT NOT NULL DEFAULT 0,
    muted_until TIMESTAMP NULL,
    last_read_at TIMESTAMP NULL,
    joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (group_id, user_id),
    FOREIGN KEY (group_id) REFERENCES groups_table(id),
    FOREIGN KEY (user_id) REFERENCES users(id),
    INDEX idx_group_members_group (group_id),
    INDEX idx_group_members_user (user_id)
);

CREATE TABLE sessions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    group_id BIGINT NOT NULL,
    host_id BIGINT NOT NULL,
    file_hash VARCHAR(64) NOT NULL,
    file_name VARCHAR(500),
    file_size BIGINT,
    duration_seconds INT,
    media_type VARCHAR(10) NOT NULL,
    video_codec VARCHAR(50),
    audio_codec VARCHAR(50),
    magnet_uri TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'CREATED',
    started_at TIMESTAMP NULL,
    ended_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (group_id) REFERENCES groups_table(id),
    FOREIGN KEY (host_id) REFERENCES users(id),
    INDEX idx_sessions_group (group_id)
);

CREATE TABLE session_participants (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    file_status VARCHAR(20) NOT NULL DEFAULT 'LOCAL',
    joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (session_id, user_id),
    FOREIGN KEY (session_id) REFERENCES sessions(id),
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE playlists (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    group_id BIGINT NOT NULL,
    name VARCHAR(200) NOT NULL,
    creator_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (group_id) REFERENCES groups_table(id),
    FOREIGN KEY (creator_id) REFERENCES users(id)
);

CREATE TABLE playlist_tracks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    playlist_id BIGINT NOT NULL,
    file_hash VARCHAR(64),
    file_name VARCHAR(500),
    duration_seconds INT,
    artist VARCHAR(200),
    album VARCHAR(200),
    sort_order INT NOT NULL DEFAULT 0,
    FOREIGN KEY (playlist_id) REFERENCES playlists(id)
);

CREATE TABLE chat_messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    group_id BIGINT NOT NULL,
    sender_id BIGINT NOT NULL,
    content TEXT,
    message_type VARCHAR(20) NOT NULL DEFAULT 'TEXT',
    reply_to_id BIGINT NULL,
    session_id BIGINT NULL,
    is_pinned BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (group_id) REFERENCES groups_table(id),
    FOREIGN KEY (sender_id) REFERENCES users(id),
    INDEX idx_chat_messages_group (group_id),
    INDEX idx_chat_messages_created (created_at)
);

CREATE TABLE message_reactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    message_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    emoji VARCHAR(10) NOT NULL,
    UNIQUE (message_id, user_id),
    FOREIGN KEY (message_id) REFERENCES chat_messages(id),
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE playback_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id BIGINT NOT NULL,
    position_seconds INT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (session_id) REFERENCES sessions(id)
);

CREATE TABLE file_metadata (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    file_hash VARCHAR(64) UNIQUE NOT NULL,
    file_name VARCHAR(500),
    file_size BIGINT,
    duration_seconds INT,
    media_type VARCHAR(10),
    video_codec VARCHAR(50),
    audio_codec VARCHAR(50),
    first_shared_by BIGINT,
    share_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (first_shared_by) REFERENCES users(id),
    INDEX idx_file_metadata_hash (file_hash)
);

CREATE TABLE announcements (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    content TEXT,
    created_by BIGINT NOT NULL,
    is_pinned BOOLEAN NOT NULL DEFAULT FALSE,
    is_published BOOLEAN NOT NULL DEFAULT FALSE,
    published_at TIMESTAMP NULL,
    expired_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (created_by) REFERENCES users(id)
);

CREATE TABLE wallpapers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    image_url TEXT NOT NULL,
    title VARCHAR(200),
    description TEXT,
    publish_date DATE,
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (created_by) REFERENCES users(id)
);

CREATE TABLE error_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    error_type VARCHAR(100),
    message TEXT,
    stack_trace TEXT,
    source VARCHAR(20) NOT NULL DEFAULT 'SERVER',
    severity VARCHAR(20) NOT NULL DEFAULT 'ERROR',
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    handled_by_degradation BOOLEAN NOT NULL DEFAULT FALSE,
    resolution_note TEXT,
    occurred_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP NULL,
    INDEX idx_error_logs_status (status)
);

CREATE TABLE server_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    config_key VARCHAR(100) UNIQUE NOT NULL,
    config_value TEXT,
    value_type VARCHAR(20) NOT NULL DEFAULT 'STRING',
    description VARCHAR(255),
    updated_by BIGINT,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (updated_by) REFERENCES users(id),
    INDEX idx_server_config_key (config_key)
);

-- Default server config values
INSERT INTO server_config (config_key, config_value, value_type, description) VALUES
    ('transfer.server_relay.enabled', 'false', 'BOOL', 'Server relay transfer switch'),
    ('transfer.server_relay.max_file_mb', '20480', 'INT', 'Max file size for server relay (MB)'),
    ('transfer.upload.max_per_minute', '5', 'INT', 'Max uploads per minute'),
    ('storage.temp_retention_hours', '24', 'INT', 'Temp file retention hours'),
    ('session.max_participants', '20', 'INT', 'Max session participants');
