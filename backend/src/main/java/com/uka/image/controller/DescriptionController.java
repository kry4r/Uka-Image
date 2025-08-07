package com.uka.image.controller;

import com.uka.image.dto.ApiResponse;
import com.uka.image.entity.Image;
import com.uka.image.entity.ImageSearchMetadata;
import com.uka.image.mcp.McpServer;
import com.uka.image.service.ImageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Image Description Management Controller
 * Handles manual description editing and AI description generation
 * 
 * @author Uka Team
 */
@Slf4j
@RestController
@RequestMapping("/descriptions")
@CrossOrigin(origins = "*")
public class DescriptionController {

    @Autowired
    private ImageService imageService;

    @Autowired
    private McpServer mcpServer;

    /**
     * Update image description manually
     * 
     * @param imageId Image ID
     * @param request Description update request
     * @return API response with updated image
     */
    @PutMapping("/{imageId}")
    public ApiResponse<Image> updateDescription(
            @PathVariable Long imageId,
            @RequestBody DescriptionUpdateRequest request) {
        
        try {
            if (imageId == null || imageId <= 0) {
                return ApiResponse.badRequest("Valid image ID is required");
            }
            
            if (request == null || request.getDescription() == null) {
                return ApiResponse.badRequest("Description is required");
            }
            
            // Validate description length
            if (request.getDescription().length() > 1000) {
                return ApiResponse.badRequest("Description cannot exceed 1000 characters");
            }
            
            Image updatedImage = imageService.updateImageDescription(imageId, request.getDescription(), request.getTags());
            
            if (updatedImage == null) {
                return ApiResponse.notFound("Image not found");
            }
            
            log.info("Description updated for image {}: '{}'", imageId, request.getDescription());
            return ApiResponse.success("Description updated successfully", updatedImage);
            
        } catch (Exception e) {
            log.error("Failed to update description for image {}: {}", imageId, e.getMessage(), e);
            return ApiResponse.error("Failed to update description: " + e.getMessage());
        }
    }

    /**
     * Generate AI description for an image
     * 
     * @param imageId Image ID
     * @param regenerate Whether to regenerate if AI description already exists
     * @return API response with AI-generated metadata
     */
    @PostMapping("/{imageId}/ai-generate")
    public ApiResponse<AiDescriptionResponse> generateAiDescription(
            @PathVariable Long imageId,
            @RequestParam(value = "regenerate", defaultValue = "false") boolean regenerate) {
        
        try {
            if (imageId == null || imageId <= 0) {
                return ApiResponse.badRequest("Valid image ID is required");
            }
            
            Image image = imageService.getImageById(imageId);
            if (image == null) {
                return ApiResponse.notFound("Image not found");
            }
            
            // Check if AI metadata already exists
            if (!regenerate) {
                // This would check if AI metadata already exists
                // For now, we'll always generate new metadata
            }
            
            // Generate AI metadata using MCP Server
            ImageSearchMetadata aiMetadata = mcpServer.generateAiMetadata(image);
            
            AiDescriptionResponse response = new AiDescriptionResponse();
            response.setImageId(imageId);
            response.setAiDescription(aiMetadata.getAiDescription());
            response.setAiTags(aiMetadata.getAiTags());
            response.setSceneClassification(aiMetadata.getSceneClassification());
            response.setObjectsDetected(aiMetadata.getObjectsDetected());
            response.setColorPalette(aiMetadata.getColorPalette());
            response.setTextContent(aiMetadata.getTextContent());
            response.setConfidenceScore(aiMetadata.getConfidenceScore().doubleValue());
            response.setProcessedAt(aiMetadata.getProcessedAt());
            
            log.info("AI description generated for image {}: '{}'", imageId, aiMetadata.getAiDescription());
            return ApiResponse.success("AI description generated successfully", response);
            
        } catch (Exception e) {
            log.error("Failed to generate AI description for image {}: {}", imageId, e.getMessage(), e);
            return ApiResponse.error("Failed to generate AI description: " + e.getMessage());
        }
    }

    /**
     * Get current description and AI metadata for an image
     * 
     * @param imageId Image ID
     * @return API response with description information
     */
    @GetMapping("/{imageId}")
    public ApiResponse<DescriptionInfo> getDescriptionInfo(@PathVariable Long imageId) {
        try {
            if (imageId == null || imageId <= 0) {
                return ApiResponse.badRequest("Valid image ID is required");
            }
            
            Image image = imageService.getImageById(imageId);
            if (image == null) {
                return ApiResponse.notFound("Image not found");
            }
            
            DescriptionInfo info = new DescriptionInfo();
            info.setImageId(imageId);
            info.setManualDescription(image.getDescription());
            info.setTags(image.getTags());
            
            // Get AI metadata if available
            // This would query the ImageSearchMetadata table
            // For now, we'll return basic info
            
            return ApiResponse.success("Description information retrieved", info);
            
        } catch (Exception e) {
            log.error("Failed to get description info for image {}: {}", imageId, e.getMessage(), e);
            return ApiResponse.error("Failed to get description info: " + e.getMessage());
        }
    }

    /**
     * Batch update descriptions for multiple images
     * 
     * @param request Batch update request
     * @return API response with update results
     */
    @PutMapping("/batch")
    public ApiResponse<BatchUpdateResult> batchUpdateDescriptions(@RequestBody BatchDescriptionUpdateRequest request) {
        try {
            if (request == null || request.getUpdates() == null || request.getUpdates().isEmpty()) {
                return ApiResponse.badRequest("Update list is required");
            }
            
            if (request.getUpdates().size() > 50) {
                return ApiResponse.badRequest("Cannot update more than 50 images at once");
            }
            
            BatchUpdateResult result = new BatchUpdateResult();
            result.setTotalRequested(request.getUpdates().size());
            
            int successCount = 0;
            int failureCount = 0;
            
            for (DescriptionUpdate update : request.getUpdates()) {
                try {
                    Image updatedImage = imageService.updateImageDescription(
                        update.getImageId(), 
                        update.getDescription(), 
                        update.getTags()
                    );
                    
                    if (updatedImage != null) {
                        successCount++;
                    } else {
                        failureCount++;
                    }
                    
                } catch (Exception e) {
                    log.warn("Failed to update description for image {}: {}", update.getImageId(), e.getMessage());
                    failureCount++;
                }
            }
            
            result.setSuccessCount(successCount);
            result.setFailureCount(failureCount);
            
            log.info("Batch description update completed: {} success, {} failures", successCount, failureCount);
            return ApiResponse.success("Batch update completed", result);
            
        } catch (Exception e) {
            log.error("Batch description update failed: {}", e.getMessage(), e);
            return ApiResponse.error("Batch update failed: " + e.getMessage());
        }
    }

    /**
     * Generate AI descriptions for multiple images
     * 
     * @param request Batch AI generation request
     * @return API response with generation results
     */
    @PostMapping("/batch/ai-generate")
    public ApiResponse<BatchAiGenerationResult> batchGenerateAiDescriptions(@RequestBody BatchAiGenerationRequest request) {
        try {
            if (request == null || request.getImageIds() == null || request.getImageIds().isEmpty()) {
                return ApiResponse.badRequest("Image IDs list is required");
            }
            
            if (request.getImageIds().size() > 20) {
                return ApiResponse.badRequest("Cannot process more than 20 images at once");
            }
            
            BatchAiGenerationResult result = new BatchAiGenerationResult();
            result.setTotalRequested(request.getImageIds().size());
            
            int successCount = 0;
            int failureCount = 0;
            
            for (Long imageId : request.getImageIds()) {
                try {
                    Image image = imageService.getImageById(imageId);
                    if (image != null) {
                        mcpServer.generateAiMetadata(image);
                        successCount++;
                    } else {
                        failureCount++;
                    }
                    
                } catch (Exception e) {
                    log.warn("Failed to generate AI description for image {}: {}", imageId, e.getMessage());
                    failureCount++;
                }
            }
            
            result.setSuccessCount(successCount);
            result.setFailureCount(failureCount);
            
            log.info("Batch AI generation completed: {} success, {} failures", successCount, failureCount);
            return ApiResponse.success("Batch AI generation completed", result);
            
        } catch (Exception e) {
            log.error("Batch AI generation failed: {}", e.getMessage(), e);
            return ApiResponse.error("Batch AI generation failed: " + e.getMessage());
        }
    }


    // DTO Classes
    public static class DescriptionUpdateRequest {
        private String description;
        private String tags;
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public String getTags() { return tags; }
        public void setTags(String tags) { this.tags = tags; }
    }

    public static class AiDescriptionResponse {
        private Long imageId;
        private String aiDescription;
        private String aiTags;
        private String sceneClassification;
        private String objectsDetected;
        private String colorPalette;
        private String textContent;
        private double confidenceScore;
        private java.time.LocalDateTime processedAt;
        
        // Getters and setters
        public Long getImageId() { return imageId; }
        public void setImageId(Long imageId) { this.imageId = imageId; }
        
        public String getAiDescription() { return aiDescription; }
        public void setAiDescription(String aiDescription) { this.aiDescription = aiDescription; }
        
        public String getAiTags() { return aiTags; }
        public void setAiTags(String aiTags) { this.aiTags = aiTags; }
        
        public String getSceneClassification() { return sceneClassification; }
        public void setSceneClassification(String sceneClassification) { this.sceneClassification = sceneClassification; }
        
        public String getObjectsDetected() { return objectsDetected; }
        public void setObjectsDetected(String objectsDetected) { this.objectsDetected = objectsDetected; }
        
        public String getColorPalette() { return colorPalette; }
        public void setColorPalette(String colorPalette) { this.colorPalette = colorPalette; }
        
        public String getTextContent() { return textContent; }
        public void setTextContent(String textContent) { this.textContent = textContent; }
        
        public double getConfidenceScore() { return confidenceScore; }
        public void setConfidenceScore(double confidenceScore) { this.confidenceScore = confidenceScore; }
        
        public java.time.LocalDateTime getProcessedAt() { return processedAt; }
        public void setProcessedAt(java.time.LocalDateTime processedAt) { this.processedAt = processedAt; }
    }

    public static class DescriptionInfo {
        private Long imageId;
        private String manualDescription;
        private String tags;
        private String aiDescription;
        private String aiTags;
        private java.time.LocalDateTime lastUpdated;
        
        // Getters and setters
        public Long getImageId() { return imageId; }
        public void setImageId(Long imageId) { this.imageId = imageId; }
        
        public String getManualDescription() { return manualDescription; }
        public void setManualDescription(String manualDescription) { this.manualDescription = manualDescription; }
        
        public String getTags() { return tags; }
        public void setTags(String tags) { this.tags = tags; }
        
        public String getAiDescription() { return aiDescription; }
        public void setAiDescription(String aiDescription) { this.aiDescription = aiDescription; }
        
        public String getAiTags() { return aiTags; }
        public void setAiTags(String aiTags) { this.aiTags = aiTags; }
        
        public java.time.LocalDateTime getLastUpdated() { return lastUpdated; }
        public void setLastUpdated(java.time.LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }
    }

    public static class BatchDescriptionUpdateRequest {
        private java.util.List<DescriptionUpdate> updates;
        
        public java.util.List<DescriptionUpdate> getUpdates() { return updates; }
        public void setUpdates(java.util.List<DescriptionUpdate> updates) { this.updates = updates; }
    }

    public static class DescriptionUpdate {
        private Long imageId;
        private String description;
        private String tags;
        
        public Long getImageId() { return imageId; }
        public void setImageId(Long imageId) { this.imageId = imageId; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public String getTags() { return tags; }
        public void setTags(String tags) { this.tags = tags; }
    }

    public static class BatchUpdateResult {
        private int totalRequested;
        private int successCount;
        private int failureCount;
        
        public int getTotalRequested() { return totalRequested; }
        public void setTotalRequested(int totalRequested) { this.totalRequested = totalRequested; }
        
        public int getSuccessCount() { return successCount; }
        public void setSuccessCount(int successCount) { this.successCount = successCount; }
        
        public int getFailureCount() { return failureCount; }
        public void setFailureCount(int failureCount) { this.failureCount = failureCount; }
    }

    public static class BatchAiGenerationRequest {
        private java.util.List<Long> imageIds;
        private boolean regenerate = false;
        
        public java.util.List<Long> getImageIds() { return imageIds; }
        public void setImageIds(java.util.List<Long> imageIds) { this.imageIds = imageIds; }
        
        public boolean isRegenerate() { return regenerate; }
        public void setRegenerate(boolean regenerate) { this.regenerate = regenerate; }
    }

    public static class BatchAiGenerationResult {
        private int totalRequested;
        private int successCount;
        private int failureCount;
        
        public int getTotalRequested() { return totalRequested; }
        public void setTotalRequested(int totalRequested) { this.totalRequested = totalRequested; }
        
        public int getSuccessCount() { return successCount; }
        public void setSuccessCount(int successCount) { this.successCount = successCount; }
        
        public int getFailureCount() { return failureCount; }
        public void setFailureCount(int failureCount) { this.failureCount = failureCount; }
    }

}