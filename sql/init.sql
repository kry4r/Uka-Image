-- Uka Image Database Migration Script
-- Purpose: Clean rebuild for enhanced image upload with manual naming
-- Version: 2.0.0
-- Author: Uka Team

-- Drop existing database and recreate
DROP DATABASE IF EXISTS uka_image_db;
CREATE DATABASE uka_image_db 
CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;
USE uka_image_db;

-- Users table for basic user management
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'User ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT 'Username',
    email VARCHAR(100) NOT NULL UNIQUE COMMENT 'Email address',
    password VARCHAR(255) NOT NULL COMMENT 'Encrypted password',
    avatar_url VARCHAR(500) COMMENT 'User avatar URL',
    status TINYINT DEFAULT 1 COMMENT 'User status: 0-disabled, 1-active',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
    deleted TINYINT DEFAULT 0 COMMENT 'Logical delete flag: 0-not deleted, 1-deleted',
    INDEX idx_username (username),
    INDEX idx_email (email),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='User information table';

-- Enhanced Images table with support for custom naming and hash IDs
CREATE TABLE images (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Image ID',
    user_id BIGINT NOT NULL COMMENT 'Uploader user ID',
    original_name VARCHAR(255) NOT NULL COMMENT 'Original filename',
    file_name VARCHAR(255) NOT NULL COMMENT 'Final stored filename with hash ID',
    file_path VARCHAR(500) NOT NULL COMMENT 'File storage path',
    cos_url VARCHAR(500) NOT NULL COMMENT 'COS access URL',
    thumbnail_url VARCHAR(500) COMMENT 'Thumbnail URL',
    file_size BIGINT NOT NULL COMMENT 'File size in bytes',
    file_type VARCHAR(50) NOT NULL COMMENT 'File MIME type',
    width INT COMMENT 'Image width in pixels',
    height INT COMMENT 'Image height in pixels',
    description TEXT COMMENT 'Image description',
    tags VARCHAR(500) COMMENT 'Image tags, comma separated',
    upload_ip VARCHAR(45) COMMENT 'Upload IP address',
    download_count INT DEFAULT 0 COMMENT 'Download count',
    view_count INT DEFAULT 0 COMMENT 'View count',
    status TINYINT DEFAULT 1 COMMENT 'Image status: 0-disabled, 1-active, 2-processing',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'Upload time',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
    deleted TINYINT DEFAULT 0 COMMENT 'Logical delete flag: 0-not deleted, 1-deleted',
    INDEX idx_user_id (user_id),
    INDEX idx_file_name (file_name),
    INDEX idx_file_type (file_type),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at),
    INDEX idx_tags (tags),
    FULLTEXT idx_description (description),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Enhanced image metadata table';

-- Image search metadata for AI search functionality
CREATE TABLE image_search_metadata (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Search metadata ID',
    image_id BIGINT NOT NULL COMMENT 'Associated image ID',
    ai_description TEXT COMMENT 'AI generated description',
    ai_tags VARCHAR(1000) COMMENT 'AI generated tags',
    color_palette JSON COMMENT 'Dominant colors in JSON format',
    objects_detected JSON COMMENT 'Detected objects in JSON format',
    scene_classification VARCHAR(100) COMMENT 'Scene classification result',
    text_content TEXT COMMENT 'OCR extracted text content',
    embedding_vector JSON COMMENT 'Feature embedding vector for similarity search',
    confidence_score DECIMAL(5,4) COMMENT 'AI analysis confidence score',
    processed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'AI processing time',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
    INDEX idx_image_id (image_id),
    INDEX idx_scene_classification (scene_classification),
    INDEX idx_confidence_score (confidence_score),
    INDEX idx_processed_at (processed_at),
    FULLTEXT idx_ai_description (ai_description),
    FULLTEXT idx_text_content (text_content),
    FOREIGN KEY (image_id) REFERENCES images(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Image AI search metadata table';

-- Albums table for organizing images
CREATE TABLE albums (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Album ID',
    user_id BIGINT NOT NULL COMMENT 'Album owner user ID',
    name VARCHAR(100) NOT NULL COMMENT 'Album name',
    description TEXT COMMENT 'Album description',
    cover_image_id BIGINT COMMENT 'Cover image ID',
    is_public TINYINT DEFAULT 0 COMMENT 'Public album: 0-private, 1-public',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
    deleted TINYINT DEFAULT 0 COMMENT 'Logical delete flag: 0-not deleted, 1-deleted',
    INDEX idx_user_id (user_id),
    INDEX idx_created_at (created_at),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Image albums table';

-- Album-Image relationship table
CREATE TABLE album_images (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Relationship ID',
    album_id BIGINT NOT NULL COMMENT 'Album ID',
    image_id BIGINT NOT NULL COMMENT 'Image ID',
    sort_order INT DEFAULT 0 COMMENT 'Sort order in album',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'Addition time',
    INDEX idx_album_id (album_id),
    INDEX idx_image_id (image_id),
    INDEX idx_sort_order (sort_order),
    UNIQUE KEY uk_album_image (album_id, image_id),
    FOREIGN KEY (album_id) REFERENCES albums(id) ON DELETE CASCADE,
    FOREIGN KEY (image_id) REFERENCES images(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Album-Image relationship table';

-- Storage configuration table for multi-cloud support
CREATE TABLE storage_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Config ID',
    provider VARCHAR(50) NOT NULL COMMENT 'Storage provider: local, tencent_cos, aws_s3, alibaba_oss',
    config_name VARCHAR(100) NOT NULL COMMENT 'Configuration name',
    config_data JSON NOT NULL COMMENT 'Configuration data in JSON format',
    is_active TINYINT DEFAULT 0 COMMENT 'Active configuration: 0-inactive, 1-active',
    is_default TINYINT DEFAULT 0 COMMENT 'Default configuration: 0-no, 1-yes',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
    deleted TINYINT DEFAULT 0 COMMENT 'Logical delete flag: 0-not deleted, 1-deleted',
    INDEX idx_provider (provider),
    INDEX idx_is_active (is_active),
    INDEX idx_is_default (is_default)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Storage configuration table';

-- System configuration table
CREATE TABLE system_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Config ID',
    config_key VARCHAR(100) NOT NULL UNIQUE COMMENT 'Configuration key',
    config_value TEXT COMMENT 'Configuration value',
    description VARCHAR(255) COMMENT 'Configuration description',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
    INDEX idx_config_key (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='System configuration table';

-- Insert default system configurations
INSERT INTO system_config (config_key, config_value, description) VALUES
('max_file_size', '52428800', 'Maximum file size in bytes (50MB)'),
('allowed_file_types', 'jpg,jpeg,png,gif,webp,bmp', 'Allowed image file types'),
('thumbnail_width', '300', 'Thumbnail width in pixels'),
('thumbnail_height', '300', 'Thumbnail height in pixels'),
('ai_search_enabled', '1', 'AI search functionality enabled: 0-disabled, 1-enabled'),
('batch_upload_limit', '20', 'Maximum number of files in batch upload'),
('spark_ai_enabled', '1', 'Spark AI integration enabled: 0-disabled, 1-enabled'),
('spark_ai_model', 'lite', 'Spark AI model version'),
('spark_ai_endpoint', 'https://spark-api-open.xf-yun.com/v1/chat/completions', 'Spark AI API endpoint');

-- Insert default local storage configuration
INSERT INTO storage_config (provider, config_name, config_data, is_active, is_default) VALUES
('local', 'Default Local Storage', '{"uploadPath": "uploads", "baseUrl": "http://localhost:8080/api/files"}', 1, 1);

-- Create default admin user (password: admin123)
INSERT INTO users (username, email, password, status) VALUES
('admin', 'admin@uka-image.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9P2.nRZ.E.Uo.Iq', 1);

-- Create sample album for admin user
INSERT INTO albums (user_id, name, description, is_public) VALUES
(1, 'Default Album', 'Default album for uploaded images', 0);

COMMIT;