package com.uka.image.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * Storage configuration entity class
 * 
 * @author Uka Team
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "storage_config")
@TableName("storage_config")
public class StorageConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @TableId(type = IdType.AUTO)
    private Long id;

    @Column(name = "provider_type", nullable = false, length = 50)
    private String providerType;

    @Column(name = "region", nullable = false, length = 100)
    private String region;

    @Column(name = "access_key_id", nullable = false, length = 255)
    private String accessKeyId;

    @Column(name = "access_key_secret", nullable = false, length = 500)
    private String accessKeySecret;

    @Column(name = "bucket_name", nullable = false, length = 255)
    private String bucketName;

    @Column(name = "base_url", length = 500)
    private String baseUrl;

    @Column(name = "custom_domain", length = 500)
    private String customDomain;

    @Column(name = "use_https")
    private Boolean useHttps;

    @Column(name = "enable_cdn")
    private Boolean enableCdn;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "is_encrypted")
    private Boolean isEncrypted;

    @Column(name = "additional_settings", columnDefinition = "TEXT")
    private String additionalSettings;

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
}
