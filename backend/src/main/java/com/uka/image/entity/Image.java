package com.uka.image.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * Image entity class
 * 
 * @author Uka Team
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "images")
@TableName("images")
public class Image {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @TableId(type = IdType.AUTO)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "original_name", nullable = false)
    private String originalName;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    @Column(name = "cos_url", nullable = false, length = 500)
    private String cosUrl;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "file_type", nullable = false, length = 50)
    private String fileType;

    @Column
    private Integer width;

    @Column
    private Integer height;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 500)
    private String tags;

    // Enhanced metadata fields for AI search optimization
    @Column(name = "color_profile", length = 100)
    private String colorProfile;

    @Column(name = "dominant_colors", length = 200)
    private String dominantColors;

    @Column(name = "aspect_ratio")
    private Double aspectRatio;

    @Column(name = "resolution_category", length = 20)
    private String resolutionCategory;

    @Column(name = "file_format", length = 10)
    private String fileFormat;

    @Column(name = "compression_quality")
    private Integer compressionQuality;

    @Column(name = "has_transparency")
    private Boolean hasTransparency;

    @Column(name = "is_animated")
    private Boolean isAnimated;

    @Column(name = "orientation", length = 20)
    private String orientation;

    @Column(name = "camera_make", length = 100)
    private String cameraMake;

    @Column(name = "camera_model", length = 100)
    private String cameraModel;

    @Column(name = "focal_length")
    private Double focalLength;

    @Column(name = "aperture")
    private String aperture;

    @Column(name = "iso_speed")
    private Integer isoSpeed;

    @Column(name = "exposure_time", length = 50)
    private String exposureTime;

    @Column(name = "gps_latitude")
    private Double gpsLatitude;

    @Column(name = "gps_longitude")
    private Double gpsLongitude;

    @Column(name = "content_category", length = 100)
    private String contentCategory;

    @Column(name = "ai_generated_tags", columnDefinition = "TEXT")
    private String aiGeneratedTags;
    
    @Column(name = "ai_generated_description", columnDefinition = "TEXT")
    private String aiGeneratedDescription;

    @Column(name = "semantic_keywords", columnDefinition = "TEXT")
    private String semanticKeywords;

    @Column(name = "visual_complexity_score")
    private Double visualComplexityScore;

    @Column(name = "color_temperature")
    private Integer colorTemperature;

    @Column(name = "brightness_level")
    private Double brightnessLevel;

    @Column(name = "contrast_level")
    private Double contrastLevel;

    @Column(name = "saturation_level")
    private Double saturationLevel;

    @Column(name = "upload_ip", length = 45)
    private String uploadIp;

    @Column(name = "download_count", columnDefinition = "INT DEFAULT 0")
    private Integer downloadCount;

    @Column(name = "view_count", columnDefinition = "INT DEFAULT 0")
    private Integer viewCount;

    @Column(columnDefinition = "TINYINT DEFAULT 1")
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @TableLogic
    @Column(columnDefinition = "TINYINT DEFAULT 0")
    private Integer deleted;

    // Transient fields for additional information
    @Transient
    @TableField(exist = false)
    private String uploaderUsername;

    @Transient
    @TableField(exist = false)
    private ImageSearchMetadata searchMetadata;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (downloadCount == null) downloadCount = 0;
        if (viewCount == null) viewCount = 0;
        if (status == null) status = 1;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Image status constants
     */
    public static class Status {
        public static final int DISABLED = 0;
        public static final int ACTIVE = 1;
        public static final int PROCESSING = 2;
    }

    /**
     * Resolution category constants
     */
    public static class ResolutionCategory {
        public static final String LOW = "LOW";           // < 1MP
        public static final String MEDIUM = "MEDIUM";     // 1-5MP
        public static final String HIGH = "HIGH";         // 5-20MP
        public static final String ULTRA_HIGH = "ULTRA_HIGH"; // > 20MP
    }

    /**
     * Orientation constants
     */
    public static class Orientation {
        public static final String LANDSCAPE = "LANDSCAPE";
        public static final String PORTRAIT = "PORTRAIT";
        public static final String SQUARE = "SQUARE";
        public static final String PANORAMIC = "PANORAMIC";
    }

    /**
     * Content category constants
     */
    public static class ContentCategory {
        public static final String PHOTOGRAPHY = "PHOTOGRAPHY";
        public static final String ARTWORK = "ARTWORK";
        public static final String SCREENSHOT = "SCREENSHOT";
        public static final String DOCUMENT = "DOCUMENT";
        public static final String GRAPHIC_DESIGN = "GRAPHIC_DESIGN";
        public static final String ILLUSTRATION = "ILLUSTRATION";
        public static final String LOGO = "LOGO";
        public static final String ICON = "ICON";
        public static final String TEXTURE = "TEXTURE";
        public static final String PATTERN = "PATTERN";
    }

    // Helper methods for enhanced metadata processing
    
    /**
     * Calculate and set aspect ratio based on width and height
     */
    public void calculateAspectRatio() {
        if (width != null && height != null && height != 0) {
            this.aspectRatio = (double) width / height;
        }
    }

    /**
     * Determine and set resolution category based on dimensions
     */
    public void calculateResolutionCategory() {
        if (width != null && height != null) {
            long megapixels = (long) width * height / 1_000_000;
            if (megapixels < 1) {
                this.resolutionCategory = ResolutionCategory.LOW;
            } else if (megapixels <= 5) {
                this.resolutionCategory = ResolutionCategory.MEDIUM;
            } else if (megapixels <= 20) {
                this.resolutionCategory = ResolutionCategory.HIGH;
            } else {
                this.resolutionCategory = ResolutionCategory.ULTRA_HIGH;
            }
        }
    }

    /**
     * Determine and set orientation based on aspect ratio
     */
    public void calculateOrientation() {
        if (aspectRatio != null) {
            if (aspectRatio > 2.0) {
                this.orientation = Orientation.PANORAMIC;
            } else if (aspectRatio > 1.2) {
                this.orientation = Orientation.LANDSCAPE;
            } else if (aspectRatio < 0.8) {
                this.orientation = Orientation.PORTRAIT;
            } else {
                this.orientation = Orientation.SQUARE;
            }
        }
    }

    /**
     * Extract file format from file type
     */
    public void extractFileFormat() {
        if (fileType != null) {
            String[] parts = fileType.split("/");
            if (parts.length > 1) {
                this.fileFormat = parts[1].toUpperCase();
            }
        }
    }

    /**
     * Get formatted file size for display
     */
    public String getFormattedFileSize() {
        if (fileSize == null || fileSize == 0) {
            return "Unknown size";
        }
        
        String[] units = {"B", "KB", "MB", "GB"};
        double size = fileSize.doubleValue();
        int unitIndex = 0;
        
        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }
        
        return String.format("%.1f %s", size, units[unitIndex]);
    }

    /**
     * Get comprehensive metadata summary for AI analysis
     */
    public String getMetadataSummary() {
        StringBuilder summary = new StringBuilder();
        
        summary.append("Technical Specs: ");
        if (width != null && height != null) {
            summary.append(String.format("%dx%d", width, height));
        }
        if (fileFormat != null) {
            summary.append(String.format(" %s", fileFormat));
        }
        if (fileSize != null) {
            summary.append(String.format(" %s", getFormattedFileSize()));
        }
        
        if (colorProfile != null) {
            summary.append(String.format(" | Color: %s", colorProfile));
        }
        if (orientation != null) {
            summary.append(String.format(" | %s", orientation));
        }
        if (resolutionCategory != null) {
            summary.append(String.format(" | %s resolution", resolutionCategory));
        }
        
        return summary.toString();
    }

    /**
     * Check if image has location data
     */
    public boolean hasLocationData() {
        return gpsLatitude != null && gpsLongitude != null;
    }

    /**
     * Check if image has camera metadata
     */
    public boolean hasCameraMetadata() {
        return cameraMake != null || cameraModel != null || 
               focalLength != null || aperture != null || 
               isoSpeed != null || exposureTime != null;
    }

    /**
     * Get search relevance keywords combining all text fields
     */
    public String getAllSearchableText() {
        StringBuilder searchText = new StringBuilder();
        
        if (fileName != null) searchText.append(fileName).append(" ");
        if (originalName != null) searchText.append(originalName).append(" ");
        if (description != null) searchText.append(description).append(" ");
        if (tags != null) searchText.append(tags).append(" ");
        if (aiGeneratedTags != null) searchText.append(aiGeneratedTags).append(" ");
        if (semanticKeywords != null) searchText.append(semanticKeywords).append(" ");
        if (contentCategory != null) searchText.append(contentCategory).append(" ");
        
        return searchText.toString().trim();
    }
}
