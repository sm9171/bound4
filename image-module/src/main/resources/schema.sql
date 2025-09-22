CREATE TABLE images (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    original_filename VARCHAR(255),
    file_hash VARCHAR(64) UNIQUE,
    file_size BIGINT,
    mime_type VARCHAR(100),
    image_data BLOB,
    thumbnail_data BLOB,
    status VARCHAR(20) DEFAULT 'READY',
    tags VARCHAR(1000),
    memo TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL
);

CREATE INDEX idx_project_created ON images (project_id, created_at);
CREATE INDEX idx_hash ON images (file_hash);