package com.uka.image.mcp;

import com.alibaba.fastjson.JSON;
import com.uka.image.entity.Image;
import com.uka.image.entity.ImageSearchMetadata;
import com.uka.image.mapper.ImageMapper;
import com.uka.image.mapper.ImageSearchMetadataMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * MCP Server module for AI-powered image search functionality
 * This module simulates AI search capabilities and provides search_image interface
 * 
 * @author Uka Team
 */
@Slf4j
@Component
public class McpServer {

    @Autowired
    private ImageMapper imageMapper;

    @Autowired
    private ImageSearchMetadataMapper searchMetadataMapper;

    // Mock AI models and data for simulation
    private static final List<String> SCENE_CLASSIFICATIONS = Arrays.asList(
        "landscape", "portrait", "architecture", "nature", "urban", "indoor", "outdoor",
        "food", "animal", "vehicle", "technology", "art", "sports", "travel"
    );

    private static final List<String> OBJECT_CATEGORIES = Arrays.asList(
        "person", "car", "building", "tree", "flower", "animal", "food", "furniture",
        "electronics", "clothing", "book", "tool", "toy", "instrument"
    );

    private static final Map<String, List<String>> COLOR_PALETTES = new HashMap<String, List<String>>() {{
        put("warm", Arrays.asList("#FF6B6B", "#FFE66D", "#FF8E53", "#C7CEEA"));
        put("cool", Arrays.asList("#4ECDC4", "#45B7D1", "#96CEB4", "#FFEAA7"));
        put("neutral", Arrays.asList("#DDD6FE", "#E5E7EB", "#F3F4F6", "#9CA3AF"));
        put("vibrant", Arrays.asList("#FF0080", "#00FF80", "#8000FF", "#FF8000"));
    }};

    /**
     * Main search_image interface - AI-powered image search
     * 
     * @param request Search request containing query and parameters
     * @return Search response with matched images and metadata
     */
    public SearchImageResponse searchImage(SearchImageRequest request) {
        log.info("Processing search_image request: {}", request.getQuery());
        
        try {
            SearchImageResponse response = new SearchImageResponse();
            response.setQuery(request.getQuery());
            response.setTimestamp(System.currentTimeMillis());
            
            // Perform different types of searches based on query
            List<SearchResult> results = new ArrayList<>();
            
            // 1. Semantic search (simulated)
            results.addAll(performSemanticSearch(request.getQuery(), request.getLimit()));
            
            // 2. Visual similarity search (simulated)
            if (request.getImageId() != null) {
                results.addAll(performVisualSimilaritySearch(request.getImageId(), request.getLimit()));
            }
            
            // 3. Color-based search
            if (request.getColorQuery() != null) {
                results.addAll(performColorSearch(request.getColorQuery(), request.getLimit()));
            }
            
            // 4. Scene classification search
            if (request.getSceneType() != null) {
                results.addAll(performSceneSearch(request.getSceneType(), request.getLimit()));
            }
            
            // Remove duplicates and sort by relevance
            results = results.stream()
                    .collect(Collectors.toMap(
                        r -> r.getImageId(),
                        r -> r,
                        (existing, replacement) -> existing.getConfidence() > replacement.getConfidence() ? existing : replacement
                    ))
                    .values()
                    .stream()
                    .sorted((a, b) -> Double.compare(b.getConfidence(), a.getConfidence()))
                    .limit(request.getLimit())
                    .collect(Collectors.toList());
            
            response.setResults(results);
            response.setTotalCount(results.size());
            response.setProcessingTimeMs(System.currentTimeMillis() - response.getTimestamp());
            
            log.info("Search completed: {} results found", results.size());
            return response;
            
        } catch (Exception e) {
            log.error("Error in search_image: {}", e.getMessage(), e);
            throw new RuntimeException("Search failed: " + e.getMessage());
        }
    }

    /**
     * Perform semantic search based on text query
     * Simulates AI understanding of image content
     */
    private List<SearchResult> performSemanticSearch(String query, int limit) {
        List<SearchResult> results = new ArrayList<>();
        
        // Search in AI descriptions and tags
        List<ImageSearchMetadata> metadataList = searchMetadataMapper.searchByAiDescription(query);
        
        for (ImageSearchMetadata metadata : metadataList) {
            Image image = imageMapper.selectById(metadata.getImageId());
            if (image != null && image.getStatus() == Image.Status.ACTIVE) {
                SearchResult result = new SearchResult();
                result.setImageId(image.getId());
                result.setImageUrl(image.getCosUrl());
                result.setThumbnailUrl(image.getThumbnailUrl());
                result.setDescription(image.getDescription());
                result.setAiDescription(metadata.getAiDescription());
                result.setConfidence(calculateSemanticConfidence(query, metadata));
                result.setMatchType("semantic");
                result.setMatchedFeatures(extractMatchedFeatures(query, metadata));
                
                results.add(result);
            }
        }
        
        // If no AI metadata exists, fall back to basic text search
        if (results.isEmpty()) {
            results.addAll(performBasicTextSearch(query, limit));
        }
        
        return results.stream().limit(limit).collect(Collectors.toList());
    }

    /**
     * Perform visual similarity search based on image features
     * Simulates AI visual understanding
     */
    private List<SearchResult> performVisualSimilaritySearch(Long imageId, int limit) {
        List<SearchResult> results = new ArrayList<>();
        
        // Get reference image metadata
        ImageSearchMetadata refMetadata = searchMetadataMapper.findByImageId(imageId);
        if (refMetadata == null) {
            return results;
        }
        
        // Find similar images based on scene classification and objects
        List<ImageSearchMetadata> similarMetadata = searchMetadataMapper.findBySceneClassification(
            refMetadata.getSceneClassification());
        
        for (ImageSearchMetadata metadata : similarMetadata) {
            if (!metadata.getImageId().equals(imageId)) {
                Image image = imageMapper.selectById(metadata.getImageId());
                if (image != null && image.getStatus() == Image.Status.ACTIVE) {
                    SearchResult result = new SearchResult();
                    result.setImageId(image.getId());
                    result.setImageUrl(image.getCosUrl());
                    result.setThumbnailUrl(image.getThumbnailUrl());
                    result.setDescription(image.getDescription());
                    result.setAiDescription(metadata.getAiDescription());
                    result.setConfidence(calculateVisualSimilarity(refMetadata, metadata));
                    result.setMatchType("visual_similarity");
                    result.setMatchedFeatures(Arrays.asList("scene", "objects", "colors"));
                    
                    results.add(result);
                }
            }
        }
        
        return results.stream()
                .sorted((a, b) -> Double.compare(b.getConfidence(), a.getConfidence()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Perform color-based search
     */
    private List<SearchResult> performColorSearch(String colorQuery, int limit) {
        List<SearchResult> results = new ArrayList<>();
        
        // This would normally analyze color palettes in the database
        // For simulation, we'll match based on color keywords
        List<Image> images = imageMapper.findRecentImages(100); // Get sample images
        
        for (Image image : images) {
            if (image.getTags() != null && image.getTags().toLowerCase().contains(colorQuery.toLowerCase())) {
                SearchResult result = new SearchResult();
                result.setImageId(image.getId());
                result.setImageUrl(image.getCosUrl());
                result.setThumbnailUrl(image.getThumbnailUrl());
                result.setDescription(image.getDescription());
                result.setConfidence(0.7 + Math.random() * 0.3); // Simulated confidence
                result.setMatchType("color");
                result.setMatchedFeatures(Arrays.asList("color_palette"));
                
                results.add(result);
            }
        }
        
        return results.stream().limit(limit).collect(Collectors.toList());
    }

    /**
     * Perform scene classification search
     */
    private List<SearchResult> performSceneSearch(String sceneType, int limit) {
        List<SearchResult> results = new ArrayList<>();
        
        List<ImageSearchMetadata> metadataList = searchMetadataMapper.findBySceneClassification(sceneType);
        
        for (ImageSearchMetadata metadata : metadataList) {
            Image image = imageMapper.selectById(metadata.getImageId());
            if (image != null && image.getStatus() == Image.Status.ACTIVE) {
                SearchResult result = new SearchResult();
                result.setImageId(image.getId());
                result.setImageUrl(image.getCosUrl());
                result.setThumbnailUrl(image.getThumbnailUrl());
                result.setDescription(image.getDescription());
                result.setAiDescription(metadata.getAiDescription());
                result.setConfidence(metadata.getConfidenceScore().doubleValue());
                result.setMatchType("scene");
                result.setMatchedFeatures(Arrays.asList("scene_classification"));
                
                results.add(result);
            }
        }
        
        return results.stream().limit(limit).collect(Collectors.toList());
    }

    /**
     * Fallback basic text search
     */
    private List<SearchResult> performBasicTextSearch(String query, int limit) {
        List<SearchResult> results = new ArrayList<>();
        
        // Search in image descriptions and tags
        List<Image> images = imageMapper.findRecentImages(100);
        
        for (Image image : images) {
            double confidence = 0.0;
            List<String> matchedFeatures = new ArrayList<>();
            
            if (image.getDescription() != null && 
                image.getDescription().toLowerCase().contains(query.toLowerCase())) {
                confidence += 0.8;
                matchedFeatures.add("description");
            }
            
            if (image.getTags() != null && 
                image.getTags().toLowerCase().contains(query.toLowerCase())) {
                confidence += 0.6;
                matchedFeatures.add("tags");
            }
            
            if (confidence > 0) {
                SearchResult result = new SearchResult();
                result.setImageId(image.getId());
                result.setImageUrl(image.getCosUrl());
                result.setThumbnailUrl(image.getThumbnailUrl());
                result.setDescription(image.getDescription());
                result.setConfidence(Math.min(confidence, 1.0));
                result.setMatchType("text");
                result.setMatchedFeatures(matchedFeatures);
                
                results.add(result);
            }
        }
        
        return results.stream()
                .sorted((a, b) -> Double.compare(b.getConfidence(), a.getConfidence()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Generate AI metadata for an image (simulation)
     * This would normally call actual AI services
     */
    public ImageSearchMetadata generateAiMetadata(Image image) {
        log.info("Generating AI metadata for image: {}", image.getId());
        
        ImageSearchMetadata metadata = new ImageSearchMetadata();
        metadata.setImageId(image.getId());
        
        // Simulate AI analysis
        metadata.setAiDescription(generateMockDescription(image));
        metadata.setAiTags(generateMockTags(image));
        metadata.setSceneClassification(getRandomScene());
        metadata.setObjectsDetected(generateMockObjects());
        metadata.setColorPalette(generateMockColorPalette());
        metadata.setTextContent(generateMockOcrText(image));
        metadata.setEmbeddingVector(generateMockEmbedding());
        metadata.setConfidenceScore(BigDecimal.valueOf(0.7 + Math.random() * 0.3));
        metadata.setProcessedAt(LocalDateTime.now());
        
        // Save to database
        searchMetadataMapper.insert(metadata);
        
        log.info("AI metadata generated and saved for image: {}", image.getId());
        return metadata;
    }

    // Helper methods for simulation
    private double calculateSemanticConfidence(String query, ImageSearchMetadata metadata) {
        double confidence = 0.5;
        
        if (metadata.getAiDescription() != null && 
            metadata.getAiDescription().toLowerCase().contains(query.toLowerCase())) {
            confidence += 0.4;
        }
        
        if (metadata.getAiTags() != null && 
            metadata.getAiTags().toLowerCase().contains(query.toLowerCase())) {
            confidence += 0.3;
        }
        
        return Math.min(confidence + Math.random() * 0.2, 1.0);
    }

    private double calculateVisualSimilarity(ImageSearchMetadata ref, ImageSearchMetadata target) {
        double similarity = 0.5;
        
        if (Objects.equals(ref.getSceneClassification(), target.getSceneClassification())) {
            similarity += 0.3;
        }
        
        // Simulate object similarity
        similarity += Math.random() * 0.4;
        
        return Math.min(similarity, 1.0);
    }

    private List<String> extractMatchedFeatures(String query, ImageSearchMetadata metadata) {
        List<String> features = new ArrayList<>();
        
        if (metadata.getAiDescription() != null && 
            metadata.getAiDescription().toLowerCase().contains(query.toLowerCase())) {
            features.add("ai_description");
        }
        
        if (metadata.getAiTags() != null && 
            metadata.getAiTags().toLowerCase().contains(query.toLowerCase())) {
            features.add("ai_tags");
        }
        
        return features;
    }

    private String generateMockDescription(Image image) {
        String[] templates = {
            "A beautiful image showing %s with %s elements",
            "This image captures %s in a %s setting",
            "An artistic composition featuring %s with %s lighting",
            "A detailed view of %s with %s characteristics"
        };
        
        String template = templates[(int) (Math.random() * templates.length)];
        String subject = OBJECT_CATEGORIES.get((int) (Math.random() * OBJECT_CATEGORIES.size()));
        String attribute = Arrays.asList("natural", "artificial", "vibrant", "subtle", "dramatic", "soft")
                .get((int) (Math.random() * 6));
        
        return String.format(template, subject, attribute);
    }

    private String generateMockTags(Image image) {
        List<String> tags = new ArrayList<>();
        
        // Add random tags
        for (int i = 0; i < 3 + (int) (Math.random() * 3); i++) {
            String tag = OBJECT_CATEGORIES.get((int) (Math.random() * OBJECT_CATEGORIES.size()));
            if (!tags.contains(tag)) {
                tags.add(tag);
            }
        }
        
        return String.join(",", tags);
    }

    private String getRandomScene() {
        return SCENE_CLASSIFICATIONS.get((int) (Math.random() * SCENE_CLASSIFICATIONS.size()));
    }

    private String generateMockObjects() {
        List<Map<String, Object>> objects = new ArrayList<>();
        
        for (int i = 0; i < 2 + (int) (Math.random() * 3); i++) {
            Map<String, Object> obj = new HashMap<>();
            obj.put("name", OBJECT_CATEGORIES.get((int) (Math.random() * OBJECT_CATEGORIES.size())));
            obj.put("confidence", 0.7 + Math.random() * 0.3);
            obj.put("bbox", Arrays.asList(
                (int) (Math.random() * 100),
                (int) (Math.random() * 100),
                50 + (int) (Math.random() * 100),
                50 + (int) (Math.random() * 100)
            ));
            objects.add(obj);
        }
        
        return JSON.toJSONString(objects);
    }

    private String generateMockColorPalette() {
        String paletteType = Arrays.asList("warm", "cool", "neutral", "vibrant")
                .get((int) (Math.random() * 4));
        
        Map<String, Object> palette = new HashMap<>();
        palette.put("dominant_colors", COLOR_PALETTES.get(paletteType));
        palette.put("palette_type", paletteType);
        
        return JSON.toJSONString(palette);
    }

    private String generateMockOcrText(Image image) {
        // Simulate OCR text extraction
        String[] mockTexts = {
            "Sample text content",
            "Image contains readable text",
            "No text detected",
            "Multiple text regions found"
        };
        
        return mockTexts[(int) (Math.random() * mockTexts.length)];
    }

    private String generateMockEmbedding() {
        // Generate mock 512-dimensional embedding vector
        List<Double> embedding = new ArrayList<>();
        for (int i = 0; i < 512; i++) {
            embedding.add(Math.random() * 2 - 1); // Values between -1 and 1
        }
        
        return JSON.toJSONString(embedding);
    }
}