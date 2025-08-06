package com.uka.image.service;

import com.uka.image.entity.Image;
import com.uka.image.entity.ImageSearchMetadata;
import com.uka.image.mapper.ImageSearchMetadataMapper;
import com.uka.image.mapper.ImageMapper;
import com.uka.image.mcp.McpServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * AI Analysis Service
 * Handles automatic AI analysis of uploaded images
 */
@Slf4j
@Service
public class AIAnalysisService {
    
    @Autowired
    private McpServer mcpServer;
    
    @Autowired
    private ImageSearchMetadataMapper searchMetadataMapper;
    
    @Autowired
    private ImageMapper imageMapper;
    
    /**
     * Analyze image asynchronously after upload
     * @param image The uploaded image
     * @return CompletableFuture with analysis result
     */
    @Async("aiAnalysisExecutor")
    public CompletableFuture<ImageSearchMetadata> analyzeImageAsync(Image image) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Starting AI analysis for image: {}", image.getId());
                
                // Check if analysis already exists
                ImageSearchMetadata existing = searchMetadataMapper.findByImageId(image.getId());
                if (existing != null) {
                    log.info("AI analysis already exists for image: {}", image.getId());
                    return existing;
                }
                
                // Generate AI metadata using MCP server
                ImageSearchMetadata metadata = mcpServer.generateAiMetadata(image);
                
                log.info("AI analysis completed for image: {}", image.getId());
                return metadata;
                
            } catch (Exception e) {
                log.error("AI analysis failed for image: {}", image.getId(), e);
                throw new RuntimeException("AI analysis failed", e);
            }
        });
    }
    
    /**
     * Batch analyze multiple images
     * @param images List of images to analyze
     * @return CompletableFuture with analysis results
     */
    @Async("aiAnalysisExecutor")
    public CompletableFuture<Void> batchAnalyzeAsync(java.util.List<Image> images) {
        return CompletableFuture.runAsync(() -> {
            log.info("Starting batch AI analysis for {} images", images.size());
            
            images.parallelStream().forEach(image -> {
                try {
                    analyzeImageAsync(image).get();
                } catch (Exception e) {
                    log.error("Failed to analyze image in batch: {}", image.getId(), e);
                }
            });
            
            log.info("Batch AI analysis completed for {} images", images.size());
        });
    }
    
    /**
     * Re-analyze image with updated AI models
     * @param imageId Image ID to re-analyze
     * @return Updated analysis result
     */
    public ImageSearchMetadata reanalyzeImage(Long imageId) {
        try {
            // Delete existing analysis
            searchMetadataMapper.deleteByImageId(imageId);
            
            // Get image
            Image image = imageMapper.selectById(imageId);
            if (image == null) {
                throw new IllegalArgumentException("Image not found: " + imageId);
            }
            
            // Generate new analysis
            return mcpServer.generateAiMetadata(image);
            
        } catch (Exception e) {
            log.error("Failed to re-analyze image: {}", imageId, e);
            throw new RuntimeException("Re-analysis failed", e);
        }
    }
}