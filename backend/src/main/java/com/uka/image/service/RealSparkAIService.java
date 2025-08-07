package com.uka.image.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uka.image.entity.Image;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Real iFlytek Spark AI Service Implementation
 * Integrates with iFlytek Spark AI API for intelligent image search
 * 
 * @author Uka Team
 */
@Slf4j
@Service
public class RealSparkAIService {

    @Value("${spark.ai.endpoint}")
    private String apiEndpoint;

    @Value("${spark.ai.api-password}")
    private String apiPassword;

    @Value("${spark.ai.model}")
    private String model;

    @Value("${spark.ai.enabled:true}")
    private boolean enabled;

    @Value("${spark.ai.timeout:30000}")
    private int timeout;

    @Value("${spark.ai.temperature:0.2}")
    private double temperature;

    @Value("${spark.ai.max-tokens:2048}")
    private int maxTokens;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public RealSparkAIService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Find relevant images using iFlytek Spark AI
     * 
     * @param searchQuery User's search input
     * @param images List of available images with metadata
     * @return List of relevant image IDs sorted by AI relevance
     */
    public List<Long> findRelevantImages(String searchQuery, List<Image> images) {
        if (!enabled || !isValidConfiguration()) {
            log.warn("iFlytek Spark AI is disabled or not properly configured, falling back to metadata search");
            return performFallbackSearch(searchQuery, images);
        }

        try {
            log.info("iFlytek Spark AI Search - Query: '{}', Total images: {}", searchQuery, images.size());
            
            // Call iFlytek Spark AI API
            List<Long> aiResults = callSparkAI(searchQuery, images);
            
            if (aiResults != null && !aiResults.isEmpty()) {
                log.info("iFlytek Spark AI Search - Found {} relevant images", aiResults.size());
                return aiResults;
            } else {
                log.warn("iFlytek Spark AI returned no results, falling back to metadata search");
                return performFallbackSearch(searchQuery, images);
            }
            
        } catch (Exception e) {
            log.error("iFlytek Spark AI search failed for query '{}': {}", searchQuery, e.getMessage(), e);
            log.info("Falling back to metadata search due to AI service error");
            return performFallbackSearch(searchQuery, images);
        }
    }

    /**
     * Call iFlytek Spark AI API for intelligent image matching
     */
    private List<Long> callSparkAI(String searchQuery, List<Image> images) throws Exception {
        // Prepare image metadata for AI analysis
        String imageMetadata = prepareImageMetadata(images);
        
        // Create enhanced AI prompt with format analysis
        String formatAnalysis = analyzeFormatCharacteristics(searchQuery);
        String prompt = createSearchPrompt(searchQuery, formatAnalysis, imageMetadata);
        
        // Prepare request payload
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("temperature", temperature);
        requestBody.put("max_tokens", maxTokens);
        
        List<Map<String, String>> messages = new ArrayList<>();
        Map<String, String> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", prompt);
        messages.add(message);
        requestBody.put("messages", messages);

        // Set headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiPassword);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        log.debug("Calling iFlytek Spark AI API: {}", apiEndpoint);
        log.debug("Request payload: {}", objectMapper.writeValueAsString(requestBody));

        // Make API call
        ResponseEntity<String> response = restTemplate.exchange(
            apiEndpoint, 
            HttpMethod.POST, 
            entity, 
            String.class
        );

        if (response.getStatusCode() == HttpStatus.OK) {
            return parseAIResponse(response.getBody(), images);
        } else {
            throw new RuntimeException("iFlytek Spark AI API returned status: " + response.getStatusCode());
        }
    }

    /**
     * Prepare enhanced image metadata for comprehensive AI analysis
     */
    private String prepareImageMetadata(List<Image> images) {
        StringBuilder metadata = new StringBuilder();
        metadata.append("ENHANCED IMAGE METADATA ANALYSIS:\n\n");
        
        for (int i = 0; i < Math.min(images.size(), 40); i++) { // Optimized limit for enhanced metadata
            Image image = images.get(i);
            
            metadata.append(String.format("--- IMAGE ID: %d ---\n", image.getId()));
            
            // Basic Information
            metadata.append(String.format("Original Name: %s\n", 
                image.getOriginalName() != null ? image.getOriginalName() : "Unknown"));
            metadata.append(String.format("Current Filename: %s\n", 
                image.getFileName() != null ? image.getFileName() : "Unknown"));
            
            // Format & Technical Details
            metadata.append(String.format("Format: %s | Type: %s\n", 
                image.getFileFormat() != null ? image.getFileFormat() : "Unknown",
                image.getFileType() != null ? image.getFileType() : "Unknown"));
            
            // Resolution & Size Information
            if (image.getWidth() != null && image.getHeight() != null) {
                metadata.append(String.format("Dimensions: %dx%d | Aspect: %.2f\n", 
                    image.getWidth(), image.getHeight(), 
                    (double) image.getWidth() / image.getHeight()));
            }
            
            if (image.getResolutionCategory() != null) {
                metadata.append(String.format("Resolution Category: %s\n", image.getResolutionCategory()));
            }
            
            if (image.getFileSize() != null) {
                metadata.append(String.format("File Size: %d bytes (%.2f MB)\n", 
                    image.getFileSize(), image.getFileSize() / (1024.0 * 1024.0)));
            }
            
            // Visual Properties
            if (image.getOrientation() != null) {
                metadata.append(String.format("Orientation: %s\n", image.getOrientation()));
            }
            
            if (image.getBrightnessLevel() != null) {
                metadata.append(String.format("Brightness: %.2f\n", image.getBrightnessLevel()));
            }
            
            if (image.getColorProfile() != null) {
                metadata.append(String.format("Color Profile: %s\n", image.getColorProfile()));
            }
            
            // Content Analysis
            metadata.append(String.format("Description: %s\n", 
                image.getDescription() != null && !image.getDescription().trim().isEmpty() 
                    ? image.getDescription() : "No description available"));
            
            metadata.append(String.format("Tags: %s\n", 
                image.getTags() != null && !image.getTags().trim().isEmpty() 
                    ? image.getTags() : "No tags"));
            
            if (image.getAiGeneratedTags() != null && !image.getAiGeneratedTags().trim().isEmpty()) {
                metadata.append(String.format("AI Tags: %s\n", image.getAiGeneratedTags()));
            }
            
            if (image.getSemanticKeywords() != null && !image.getSemanticKeywords().trim().isEmpty()) {
                metadata.append(String.format("Keywords: %s\n", image.getSemanticKeywords()));
            }
            
            if (image.getContentCategory() != null) {
                metadata.append(String.format("Category: %s\n", image.getContentCategory()));
            }
            
            // Upload Time Context
            if (image.getCreatedAt() != null) {
                metadata.append(String.format("Upload Time: %s\n", image.getCreatedAt().toString()));
            }
            
            // Quality Indicators
            if (image.getViewCount() != null && image.getViewCount() > 0) {
                metadata.append(String.format("Views: %d\n", image.getViewCount()));
            }
            
            metadata.append("\n");
        }
        
        return metadata.toString();
    }

    /**
     * Create enhanced AI prompt for intelligent image search with format recognition
     */
    private String createSearchPrompt(String searchQuery, String imageMetadata, String formatAnalysis) {
        return String.format("""
            You are an advanced AI image search assistant with expertise in format recognition, metadata analysis, and content understanding.
            
            SEARCH QUERY: "%s"
            
            %s
            
            AVAILABLE IMAGES:
            %s
            
            ENHANCED ANALYSIS INSTRUCTIONS:
            
            1. FORMAT RECOGNITION & ANALYSIS:
               - Identify image formats (JPEG, PNG, WebP, GIF, BMP, TIFF) with high accuracy
               - Consider format-specific characteristics (transparency for PNG, animation for GIF, compression for JPEG)
               - Match format requirements in search queries (e.g., "transparent image" → PNG preference)
               - Analyze format suitability for intended use (web, print, editing)
            
            2. RESOLUTION & SIZE DETECTION:
               - Parse resolution information from metadata and filenames
               - Identify quality levels: LOW (<1MP), MEDIUM (1-5MP), HIGH (5-20MP), ULTRA_HIGH (>20MP)
               - Detect dimension patterns in filenames (e.g., "1920x1080", "4K", "HD")
               - Consider aspect ratios and orientation preferences
            
            3. FILENAME INTELLIGENCE:
               - Extract semantic meaning from filenames beyond keywords
               - Recognize naming conventions (IMG_001, DSC_, photo_, screenshot_)
               - Identify date/time patterns in filenames
               - Parse technical specifications embedded in names
               - Consider file naming context and patterns
            
            4. DESCRIPTION & CONTENT ANALYSIS:
               - Perform deep semantic analysis of descriptions
               - Identify visual elements, subjects, and composition
               - Recognize artistic styles, moods, and themes
               - Match conceptual queries with descriptive content
               - Consider emotional and aesthetic qualities
            
            5. UPLOAD TIME CONTEXT:
               - Factor in temporal relevance for time-sensitive queries
               - Prioritize recent uploads for "new", "latest", "recent" queries
               - Consider seasonal relevance and time-based context
               - Balance freshness with relevance for general queries
            
            6. MULTI-DIMENSIONAL MATCHING:
               - Combine format, resolution, content, and temporal factors
               - Weight different aspects based on query intent
               - Provide holistic relevance scoring
               - Consider user intent beyond literal keywords
            
            QUERY ANALYSIS FRAMEWORK:
            - Technical queries: Prioritize format, resolution, file properties
            - Visual queries: Focus on content, composition, aesthetic qualities  
            - Semantic queries: Emphasize description matching and conceptual understanding
            - Temporal queries: Weight upload time and freshness factors
            - Hybrid queries: Balance multiple factors intelligently
            
            OUTPUT REQUIREMENTS:
            - Return ONLY numeric image IDs separated by commas
            - Order by comprehensive relevance score (highest first)
            - Maximum 20 results for optimal performance
            - Return "NO_MATCHES" if no relevant images found
            - NO explanations, formatting, or additional text
            
            EXAMPLES:
            ✓ Correct: "15,7,23,1"
            ✓ Correct: "42"
            ✓ Correct: "NO_MATCHES"
            ✗ Wrong: "Image 15 matches best"
            ✗ Wrong: "IDs: 15,7,23"
            
            RESPONSE: [Image IDs only]
            """, searchQuery, formatAnalysis, imageMetadata);
    }

    /**
     * Parse AI response and extract image IDs
     */
    private List<Long> parseAIResponse(String responseBody, List<Image> images) throws Exception {
        log.debug("iFlytek Spark AI response: {}", responseBody);
        
        JsonNode responseJson = objectMapper.readTree(responseBody);
        JsonNode choices = responseJson.get("choices");
        
        if (choices != null && choices.isArray() && choices.size() > 0) {
            JsonNode firstChoice = choices.get(0);
            JsonNode message = firstChoice.get("message");
            
            if (message != null) {
                String content = message.get("content").asText().trim();
                log.info("iFlytek Spark AI response content: {}", content);
                
                if ("NO_MATCHES".equals(content)) {
                    return new ArrayList<>();
                }
                
                // Parse comma-separated IDs with improved parsing
                List<Long> resultIds = new ArrayList<>();
                String[] idStrings = content.split("[,\\s]+"); // Split by comma or whitespace
                
                for (String idString : idStrings) {
                    String cleanId = idString.trim();
                    
                    // Extract numbers from strings like "ID1", "Image2", etc.
                    String numberOnly = cleanId.replaceAll("[^0-9]", "");
                    
                    if (!numberOnly.isEmpty()) {
                        try {
                            Long id = Long.parseLong(numberOnly);
                            // Verify the ID exists in our image list
                            if (images.stream().anyMatch(img -> img.getId().equals(id))) {
                                resultIds.add(id);
                                log.debug("Added valid image ID: {}", id);
                            } else {
                                log.warn("AI returned non-existent image ID: {}", id);
                            }
                        } catch (NumberFormatException e) {
                            log.warn("Could not parse number from AI response part: {}", cleanId);
                        }
                    }
                }
                
                return resultIds;
            }
        }
        
        throw new RuntimeException("Invalid response format from iFlytek Spark AI");
    }

    /**
     * Fallback to metadata-based search when AI is not available
     */
    private List<Long> performFallbackSearch(String searchQuery, List<Image> images) {
        log.info("Performing fallback metadata-based search");
        
        String normalizedQuery = searchQuery.toLowerCase().trim();
        List<ImageMatch> matches = new ArrayList<>();
        
        for (Image image : images) {
            double score = calculateMetadataScore(image, normalizedQuery);
            if (score > 0.0) {
                ImageMatch match = new ImageMatch();
                match.setImageId(image.getId());
                match.setScore(score);
                matches.add(match);
            }
        }
        
        // Sort by score (highest first)
        matches.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
        
        return matches.stream()
            .map(ImageMatch::getImageId)
            .collect(Collectors.toList());
    }

    /**
     * Calculate metadata-based relevance score
     */
    private double calculateMetadataScore(Image image, String query) {
        double score = 0.0;
        
        // Check filename
        if (image.getFileName() != null && image.getFileName().toLowerCase().contains(query)) {
            score += 0.3;
        }
        
        // Check original name
        if (image.getOriginalName() != null && image.getOriginalName().toLowerCase().contains(query)) {
            score += 0.3;
        }
        
        // Check description
        if (image.getDescription() != null && image.getDescription().toLowerCase().contains(query)) {
            score += 0.4;
        }
        
        // Check tags
        if (image.getTags() != null && image.getTags().toLowerCase().contains(query)) {
            score += 0.5;
        }
        
        // Check file type
        if (image.getFileType() != null && image.getFileType().toLowerCase().contains(query)) {
            score += 0.1;
        }
        
        return Math.min(score, 1.0); // Cap at 1.0
    }

    /**
     * Check if iFlytek Spark AI service is available
     */
    public boolean isServiceAvailable() {
        if (!enabled || !isValidConfiguration()) {
            return false;
        }

        try {
            // Simple health check - try to make a minimal API call
            Map<String, Object> testRequest = new HashMap<>();
            testRequest.put("model", model);
            testRequest.put("max_tokens", 10);
            
            List<Map<String, String>> messages = new ArrayList<>();
            Map<String, String> message = new HashMap<>();
            message.put("role", "user");
            message.put("content", "Hello");
            messages.add(message);
            testRequest.put("messages", messages);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiPassword);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(testRequest, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                apiEndpoint, 
                HttpMethod.POST, 
                entity, 
                String.class
            );

            return response.getStatusCode() == HttpStatus.OK;
            
        } catch (Exception e) {
            log.error("iFlytek Spark AI health check failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Analyze format-specific characteristics for enhanced AI understanding
     */
    private String analyzeFormatCharacteristics(String searchQuery) {
        StringBuilder analysis = new StringBuilder();
        analysis.append("FORMAT ANALYSIS CONTEXT:\n");
        
        String lowerQuery = searchQuery.toLowerCase();
        
        // Format-specific requirements detection
        if (lowerQuery.contains("transparent") || lowerQuery.contains("transparency")) {
            analysis.append("- TRANSPARENCY REQUIRED: Prioritize PNG, WebP formats\n");
        }
        
        if (lowerQuery.contains("animated") || lowerQuery.contains("animation") || lowerQuery.contains("gif")) {
            analysis.append("- ANIMATION REQUIRED: Prioritize GIF, WebP formats\n");
        }
        
        if (lowerQuery.contains("high quality") || lowerQuery.contains("lossless")) {
            analysis.append("- HIGH QUALITY: Prioritize PNG, TIFF, uncompressed formats\n");
        }
        
        if (lowerQuery.contains("web") || lowerQuery.contains("website") || lowerQuery.contains("online")) {
            analysis.append("- WEB OPTIMIZED: Prioritize JPEG, WebP, optimized PNG\n");
        }
        
        if (lowerQuery.contains("print") || lowerQuery.contains("printing")) {
            analysis.append("- PRINT QUALITY: Prioritize TIFF, high-res JPEG, PNG\n");
        }
        
        // Resolution requirements
        if (lowerQuery.contains("hd") || lowerQuery.contains("1080") || lowerQuery.contains("720")) {
            analysis.append("- HD RESOLUTION: Look for 1280x720 or 1920x1080 dimensions\n");
        }
        
        if (lowerQuery.contains("4k") || lowerQuery.contains("ultra") || lowerQuery.contains("uhd")) {
            analysis.append("- 4K/ULTRA: Look for 3840x2160 or higher dimensions\n");
        }
        
        if (lowerQuery.contains("thumbnail") || lowerQuery.contains("small") || lowerQuery.contains("icon")) {
            analysis.append("- SMALL SIZE: Prioritize images under 500x500 pixels\n");
        }
        
        // Time-based context
        if (lowerQuery.contains("recent") || lowerQuery.contains("new") || lowerQuery.contains("latest")) {
            analysis.append("- TEMPORAL PRIORITY: Weight recent uploads higher\n");
        }
        
        if (lowerQuery.contains("old") || lowerQuery.contains("vintage") || lowerQuery.contains("classic")) {
            analysis.append("- HISTORICAL CONTEXT: Consider older uploads or vintage content\n");
        }
        
        return analysis.toString();
    }

    /**
     * Check if AI configuration is valid
     */
    private boolean isValidConfiguration() {
        return apiEndpoint != null && !apiEndpoint.isEmpty() &&
               apiPassword != null && !apiPassword.equals("your-api-password-here") &&
               model != null && !model.isEmpty();
    }

    /**
     * Helper class for storing image matches
     */
    private static class ImageMatch {
        private Long imageId;
        private double score;
        
        public Long getImageId() { return imageId; }
        public void setImageId(Long imageId) { this.imageId = imageId; }
        
        public double getScore() { return score; }
        public void setScore(double score) { this.score = score; }
    }
}