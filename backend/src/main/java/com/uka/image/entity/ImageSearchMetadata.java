package com.uka.image.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Image search metadata entity class for AI search functionality
 * 
 * @author Uka Team
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "image_search_metadata")
@TableName("image_search_metadata")
public class ImageSearchMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @TableId(type = IdType.AUTO)
    private Long id;

    @Column(name = "image_id", nullable = false)
    private Long imageId;

    @Column(name = "ai_description", columnDefinition = "TEXT")
    private String aiDescription;

    @Column(name = "ai_tags", length = 1000)
    private String aiTags;

    @Column(name = "color_palette", columnDefinition = "JSON")
    private String colorPalette;

    @Column(name = "objects_detected", columnDefinition = "JSON")
    private String objectsDetected;

    @Column(name = "scene_classification", length = 100)
    private String sceneClassification;

    @Column(name = "text_content", columnDefinition = "TEXT")
    private String textContent;

    @Column(name = "embedding_vector", columnDefinition = "JSON")
    private String embeddingVector;

    @Column(name = "confidence_score", precision = 5, scale = 4)
    private BigDecimal confidenceScore;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

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
        if (processedAt == null) {
            processedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}