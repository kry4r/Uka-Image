-- Create storage configuration table
CREATE TABLE IF NOT EXISTS storage_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Storage config ID',
    provider_type VARCHAR(50) NOT NULL COMMENT 'Storage provider type',
    region VARCHAR(100) NOT NULL COMMENT 'Storage region',
    access_key_id VARCHAR(255) NOT NULL COMMENT 'Access key ID',
    access_key_secret VARCHAR(500) NOT NULL COMMENT 'Access key secret (encrypted)',
    bucket_name VARCHAR(255) NOT NULL COMMENT 'Bucket name',
    base_url VARCHAR(500) COMMENT 'Base URL for storage service',
    custom_domain VARCHAR(500) COMMENT 'Custom domain for CDN',
    use_https BOOLEAN DEFAULT TRUE COMMENT 'Whether to use HTTPS',
    enable_cdn BOOLEAN DEFAULT FALSE COMMENT 'Whether to enable CDN',
    is_active BOOLEAN DEFAULT FALSE COMMENT 'Whether this configuration is active',
    is_encrypted BOOLEAN DEFAULT TRUE COMMENT 'Whether secrets are encrypted',
    additional_settings TEXT COMMENT 'Additional provider-specific settings',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
    
    INDEX idx_provider_type (provider_type),
    INDEX idx_is_active (is_active),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Storage provider configuration table';

-- Insert default Tencent COS configuration
INSERT INTO storage_config (
    provider_type, 
    region, 
    access_key_id, 
    access_key_secret, 
    bucket_name, 
    base_url,
    use_https,
    enable_cdn,
    is_active,
    is_encrypted
) VALUES (
    'tencent_cos',
    'ap-beijing',
    'your-secret-id',
    'your-secret-key',
    'uka-image-bucket',
    'https://uka-image-bucket.cos.ap-beijing.myqcloud.com',
    TRUE,
    FALSE,
    TRUE,
    FALSE
) ON DUPLICATE KEY UPDATE id=id;

-- Update system_config table to add new fields if they don't exist
ALTER TABLE system_config 
ADD COLUMN IF NOT EXISTS config_type VARCHAR(50) DEFAULT 'string' COMMENT 'Configuration type',
ADD COLUMN IF NOT EXISTS is_encrypted BOOLEAN DEFAULT FALSE COMMENT 'Whether value is encrypted';

-- Insert storage-related system configurations
INSERT INTO system_config (config_key, config_value, config_type, description) VALUES
('storage.default_provider', 'tencent_cos', 'string', 'Default storage provider'),
('storage.upload.max_file_size', '52428800', 'number', 'Maximum file size in bytes (50MB)'),
('storage.upload.allowed_types', 'jpg,jpeg,png,gif,webp,bmp', 'string', 'Allowed image file types'),
('storage.upload.generate_thumbnail', '1', 'boolean', 'Whether to generate thumbnails'),
('storage.upload.thumbnail_size', '300x300', 'string', 'Thumbnail size in pixels'),
('storage.connection_pool.max_connections', '50', 'number', 'Maximum connections in pool'),
('storage.connection_pool.connection_timeout', '30000', 'number', 'Connection timeout in milliseconds'),
('storage.connection_pool.socket_timeout', '30000', 'number', 'Socket timeout in milliseconds'),
('storage.connection_pool.max_error_retry', '3', 'number', 'Maximum error retry attempts')
ON DUPLICATE KEY UPDATE config_value=VALUES(config_value);