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
}