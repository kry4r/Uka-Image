package com.uka.image.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * System configuration entity class
 * 
 * @author Uka Team
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "system_config")
@TableName("system_config")
public class SystemConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @TableId(type = IdType.AUTO)
    private Long id;

    @Column(name = "config_key", nullable = false, unique = true, length = 100)
    private String configKey;

    @Column(name = "config_value", columnDefinition = "TEXT")
    private String configValue;

    @Column(name = "config_type", length = 50)
    private String configType;

    @Column(length = 255)
    private String description;

    @Column(name = "is_encrypted")
    private Boolean isEncrypted;

    @TableField(fill = FieldFill.INSERT)
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Common configuration keys
     */
    public static class Keys {
        public static final String MAX_FILE_SIZE = "max_file_size";
        public static final String ALLOWED_FILE_TYPES = "allowed_file_types";
        public static final String THUMBNAIL_WIDTH = "thumbnail_width";
        public static final String THUMBNAIL_HEIGHT = "thumbnail_height";
        public static final String AI_SEARCH_ENABLED = "ai_search_enabled";
        public static final String BATCH_UPLOAD_LIMIT = "batch_upload_limit";
    }
}