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
        
        // Create AI prompt for image search
        String prompt = createSearchPrompt(searchQuery, imageMetadata);
        
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
     * Prepare image metadata for AI analysis
     */
    private String prepareImageMetadata(List<Image> images) {
        StringBuilder metadata = new StringBuilder();
        metadata.append("Available Images:\n");
        
        for (int i = 0; i < Math.min(images.size(), 50); i++) { // Limit to 50 images to avoid token limits
            Image image = images.get(i);
            metadata.append(String.format("ID: %d, Name: %s, Description: %s, Tags: %s, Type: %s\n",
                image.getId(),
                image.getOriginalName() != null ? image.getOriginalName() : "Unknown",
                image.getDescription() != null ? image.getDescription() : "No description",
                image.getTags() != null ? image.getTags() : "No tags",
                image.getFileType() != null ? image.getFileType() : "Unknown"
            ));
        }
        
        return metadata.toString();
    }

    /**
     * Create AI prompt for image search
     */
    private String createSearchPrompt(String searchQuery, String imageMetadata) {
        return String.format("""
            You are an intelligent image search assistant. Based on the user's search query and the available image metadata, 
            identify the most relevant images and return their IDs in order of relevance.
            
            User Search Query: "%s"
            
            %s
            
            Instructions:
            1. Analyze the search query to understand what the user is looking for
            2. Match the query against image names, descriptions, tags, and file types
            3. Consider semantic similarity, not just exact keyword matches
            4. Return ONLY the numeric image IDs of relevant matches, separated by commas
            5. Order the results by relevance (most relevant first)
            6. If no images are relevant, return "NO_MATCHES"
            7. IMPORTANT: Return ONLY numbers separated by commas, no other text or formatting
            
            Examples:
            - Good response: "2,1,3"
            - Good response: "2"
            - Good response: "NO_MATCHES"
            - Bad response: "ID1,ID2,ID3"
            - Bad response: "Image 2 is most relevant"
            
            Response format: Just the numbers separated by commas, nothing else.
            """, searchQuery, imageMetadata);
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