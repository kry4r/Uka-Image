-- Enhanced metadata fields for AI image search optimization
-- Migration script to add comprehensive metadata columns to images table

-- Add color and visual analysis fields
ALTER TABLE images ADD COLUMN color_profile VARCHAR(100) COMMENT 'Color profile information (sRGB, Adobe RGB, etc.)';
ALTER TABLE images ADD COLUMN dominant_colors VARCHAR(200) COMMENT 'Comma-separated list of dominant colors in hex format';
ALTER TABLE images ADD COLUMN aspect_ratio DECIMAL(10,6) COMMENT 'Width/height ratio for orientation analysis';
ALTER TABLE images ADD COLUMN resolution_category VARCHAR(20) COMMENT 'LOW, MEDIUM, HIGH, ULTRA_HIGH based on megapixels';

-- Add file format and technical specifications
ALTER TABLE images ADD COLUMN file_format VARCHAR(10) COMMENT 'File format extracted from MIME type (JPEG, PNG, etc.)';
ALTER TABLE images ADD COLUMN compression_quality INT COMMENT 'Compression quality percentage (0-100)';
ALTER TABLE images ADD COLUMN has_transparency BOOLEAN DEFAULT FALSE COMMENT 'Whether image supports transparency';
ALTER TABLE images ADD COLUMN is_animated BOOLEAN DEFAULT FALSE COMMENT 'Whether image is animated (GIF, APNG, etc.)';
ALTER TABLE images ADD COLUMN orientation VARCHAR(20) COMMENT 'LANDSCAPE, PORTRAIT, SQUARE, PANORAMIC';

-- Add camera and EXIF metadata fields
ALTER TABLE images ADD COLUMN camera_make VARCHAR(100) COMMENT 'Camera manufacturer from EXIF data';
ALTER TABLE images ADD COLUMN camera_model VARCHAR(100) COMMENT 'Camera model from EXIF data';
ALTER TABLE images ADD COLUMN focal_length DECIMAL(8,2) COMMENT 'Focal length in millimeters';
ALTER TABLE images ADD COLUMN aperture VARCHAR(10) COMMENT 'Aperture value (f-stop)';
ALTER TABLE images ADD COLUMN iso_speed INT COMMENT 'ISO sensitivity setting';
ALTER TABLE images ADD COLUMN exposure_time VARCHAR(50) COMMENT 'Shutter speed/exposure time';

-- Add GPS location fields
ALTER TABLE images ADD COLUMN gps_latitude DECIMAL(10,8) COMMENT 'GPS latitude coordinate';
ALTER TABLE images ADD COLUMN gps_longitude DECIMAL(11,8) COMMENT 'GPS longitude coordinate';

-- Add AI analysis and content categorization fields
ALTER TABLE images ADD COLUMN content_category VARCHAR(100) COMMENT 'AI-determined content category';
ALTER TABLE images ADD COLUMN ai_generated_tags TEXT COMMENT 'AI-generated descriptive tags';
ALTER TABLE images ADD COLUMN semantic_keywords TEXT COMMENT 'Semantic keywords for enhanced search';
ALTER TABLE images ADD COLUMN visual_complexity_score DECIMAL(5,3) COMMENT 'Visual complexity score (0.0-1.0)';

-- Add color analysis fields
ALTER TABLE images ADD COLUMN color_temperature INT COMMENT 'Color temperature in Kelvin';
ALTER TABLE images ADD COLUMN brightness_level DECIMAL(5,3) COMMENT 'Average brightness level (0.0-1.0)';
ALTER TABLE images ADD COLUMN contrast_level DECIMAL(5,3) COMMENT 'Contrast level (0.0-1.0)';
ALTER TABLE images ADD COLUMN saturation_level DECIMAL(5,3) COMMENT 'Saturation level (0.0-1.0)';

-- Add indexes for enhanced search performance
CREATE INDEX idx_images_content_category ON images(content_category);
CREATE INDEX idx_images_resolution_category ON images(resolution_category);
CREATE INDEX idx_images_orientation ON images(orientation);
CREATE INDEX idx_images_file_format ON images(file_format);
CREATE INDEX idx_images_aspect_ratio ON images(aspect_ratio);
CREATE INDEX idx_images_color_profile ON images(color_profile);
CREATE INDEX idx_images_has_transparency ON images(has_transparency);
CREATE INDEX idx_images_is_animated ON images(is_animated);

-- Add composite indexes for common search patterns
CREATE INDEX idx_images_search_metadata ON images(content_category, resolution_category, orientation, file_format);
CREATE INDEX idx_images_visual_analysis ON images(brightness_level, contrast_level, saturation_level);
CREATE INDEX idx_images_camera_metadata ON images(camera_make, camera_model);

-- Update existing records with calculated values where possible
UPDATE images 
SET aspect_ratio = CASE 
    WHEN height > 0 THEN CAST(width AS DECIMAL(10,6)) / CAST(height AS DECIMAL(10,6))
    ELSE NULL 
END
WHERE width IS NOT NULL AND height IS NOT NULL;

UPDATE images 
SET resolution_category = CASE 
    WHEN (width * height) < 1000000 THEN 'LOW'
    WHEN (width * height) <= 5000000 THEN 'MEDIUM'
    WHEN (width * height) <= 20000000 THEN 'HIGH'
    ELSE 'ULTRA_HIGH'
END
WHERE width IS NOT NULL AND height IS NOT NULL;

UPDATE images 
SET orientation = CASE 
    WHEN aspect_ratio > 2.0 THEN 'PANORAMIC'
    WHEN aspect_ratio > 1.2 THEN 'LANDSCAPE'
    WHEN aspect_ratio < 0.8 THEN 'PORTRAIT'
    ELSE 'SQUARE'
END
WHERE aspect_ratio IS NOT NULL;

UPDATE images 
SET file_format = CASE 
    WHEN file_type LIKE '%jpeg%' OR file_type LIKE '%jpg%' THEN 'JPEG'
    WHEN file_type LIKE '%png%' THEN 'PNG'
    WHEN file_type LIKE '%gif%' THEN 'GIF'
    WHEN file_type LIKE '%webp%' THEN 'WEBP'
    WHEN file_type LIKE '%bmp%' THEN 'BMP'
    WHEN file_type LIKE '%tiff%' THEN 'TIFF'
    WHEN file_type LIKE '%svg%' THEN 'SVG'
    ELSE 'OTHER'
END
WHERE file_type IS NOT NULL;

UPDATE images 
SET has_transparency = CASE 
    WHEN file_format IN ('PNG', 'GIF', 'WEBP', 'SVG') THEN TRUE
    ELSE FALSE
END
WHERE file_format IS NOT NULL;

UPDATE images 
SET is_animated = CASE 
    WHEN file_format = 'GIF' THEN TRUE
    ELSE FALSE
END
WHERE file_format IS NOT NULL;

-- Add comments for documentation
ALTER TABLE images COMMENT = 'Enhanced images table with comprehensive metadata for AI-powered search optimization';