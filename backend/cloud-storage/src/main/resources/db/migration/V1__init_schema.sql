CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE users (
    id            UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email         VARCHAR(255) NOT NULL UNIQUE,
    username      VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE TABLE storage_nodes (
    id             VARCHAR(50) PRIMARY KEY,
    host           VARCHAR(255) NOT NULL,
    port           INT NOT NULL,
    status         VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    last_heartbeat TIMESTAMP WITH TIME ZONE,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE TABLE file_metadata (
    id                UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    owner_id          UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    original_filename VARCHAR(500) NOT NULL,
    content_type      VARCHAR(255),
    total_size_bytes  BIGINT NOT NULL,
    chunk_count       INT NOT NULL,
    replication_factor INT NOT NULL DEFAULT 3,
    status            VARCHAR(20) NOT NULL DEFAULT 'UPLOADING',
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE TABLE file_chunks (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    file_id     UUID NOT NULL REFERENCES file_metadata(id) ON DELETE CASCADE,
    chunk_index INT NOT NULL,
    node_id     VARCHAR(50) NOT NULL REFERENCES storage_nodes(id),
    checksum    VARCHAR(64) NOT NULL,
    size_bytes  INT NOT NULL,
    UNIQUE (file_id, chunk_index, node_id)
);

CREATE TABLE encrypted_keys (
    id            UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    file_id       UUID NOT NULL UNIQUE REFERENCES file_metadata(id) ON DELETE CASCADE,
    encrypted_dek TEXT NOT NULL,
    kek_salt      VARCHAR(64) NOT NULL,
    iv            VARCHAR(32) NOT NULL,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);
