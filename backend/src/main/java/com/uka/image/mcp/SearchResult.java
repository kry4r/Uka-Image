package com.uka.image.mcp;

import lombok.Data;
import java.util.List;

/**
 * Individual search result DTO
 * 
 * @author Uka Team
 */
@Data
public class SearchResult {
    
    private Long imageId;                    // Image ID
    private String imageUrl;                 // Full image URL
    private String thumbnailUrl;             // Thumbnail URL
    private String description;              // User-provided description
    private String aiDescription;            // AI-generated description
    private double confidence;               // Search confidence score (0.0 - 1.0)
    private String matchType;                // Type of match: semantic, visual, color, scene, text
    private List<String> matchedFeatures;    // Features that matched the query
    private String sceneClassification;      // Scene classification
    private String dominantColors;           // Dominant colors in the image
    private List<String> detectedObjects;    // Objects detected in the image
    private String ocrText;                  // OCR extracted text
    
    public SearchResult() {}
    
    public SearchResult(Long imageId, String imageUrl, double confidence) {
        this.imageId = imageId;
        this.imageUrl = imageUrl;
        this.confidence = confidence;
    }
}