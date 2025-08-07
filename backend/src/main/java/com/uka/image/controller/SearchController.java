package com.uka.image.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.uka.image.dto.ApiResponse;
import com.uka.image.entity.Image;
import com.uka.image.mcp.McpServer;
import com.uka.image.mcp.SearchImageRequest;
import com.uka.image.mcp.SearchImageResponse;
import com.uka.image.service.ImageService;
import com.uka.image.service.SparkAIService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Search REST API Controller integrating with MCPServer
 * 
 * @author Uka Team
 */
@Slf4j
@RestController
@RequestMapping("/search")
@CrossOrigin(origins = "*")
public class SearchController {

    @Autowired
    private McpServer mcpServer;

    @Autowired
    private ImageService imageService;

    @Autowired
    private SparkAIService sparkAIService;

    /**
     * AI-powered image search using search_image interface
     * 
     * @param query Search query text
     * @param imageId Reference image ID for visual similarity search (optional)
     * @param colorQuery Color-based search query (optional)
     * @param sceneType Scene classification filter (optional)
     * @param limit Maximum number of results (default: 10)
     * @param minConfidence Minimum confidence threshold (default: 0.5)
     * @return API response with search results
     */
    @GetMapping("/images")
    public ApiResponse<SearchImageResponse> searchImages(
            @RequestParam(value = "query", required = false) String query,
            @RequestParam(value = "imageId", required = false) Long imageId,
            @RequestParam(value = "colorQuery", required = false) String colorQuery,
            @RequestParam(value = "sceneType", required = false) String sceneType,
            @RequestParam(value = "limit", defaultValue = "10") int limit,
            @RequestParam(value = "minConfidence", defaultValue = "0.5") double minConfidence) {
        
        try {
            // Validate input parameters
            if (query == null && imageId == null && colorQuery == null && sceneType == null) {
                return ApiResponse.badRequest("At least one search parameter is required");
            }
            
            if (limit <= 0 || limit > 100) {
                return ApiResponse.badRequest("Limit must be between 1 and 100");
            }
            
            if (minConfidence < 0.0 || minConfidence > 1.0) {
                return ApiResponse.badRequest("Minimum confidence must be between 0.0 and 1.0");
            }
            
            // Create search request
            SearchImageRequest request = new SearchImageRequest();
            request.setQuery(query);
            request.setImageId(imageId);
            request.setColorQuery(colorQuery);
            request.setSceneType(sceneType);
            request.setLimit(limit);
            request.setMinConfidence(minConfidence);
            
            // Perform search using MCP Server
            SearchImageResponse response = mcpServer.searchImage(request);
            
            log.info("Search completed: query='{}', results={}, processingTime={}ms", 
                    query, response.getTotalCount(), response.getProcessingTimeMs());
            
            return ApiResponse.success("Search completed successfully", response);
            
        } catch (Exception e) {
            log.error("Search failed: {}", e.getMessage(), e);
            return ApiResponse.error("Search failed: " + e.getMessage());
        }
    }

    /**
     * Advanced search with multiple parameters
     * 
     * @param request Search request object
     * @return API response with search results
     */
    @PostMapping("/images/advanced")
    public ApiResponse<SearchImageResponse> advancedSearch(@RequestBody SearchImageRequest request) {
        try {
            // Validate request
            if (request == null) {
                return ApiResponse.badRequest("Search request is required");
            }
            
            if (request.getQuery() == null && request.getImageId() == null && 
                request.getColorQuery() == null && request.getSceneType() == null) {
                return ApiResponse.badRequest("At least one search parameter is required");
            }
            
            // Set default values
            if (request.getLimit() <= 0) {
                request.setLimit(10);
            }
            if (request.getMinConfidence() < 0.0) {
                request.setMinConfidence(0.5);
            }
            
            // Perform search
            SearchImageResponse response = mcpServer.searchImage(request);
            
            log.info("Advanced search completed: results={}, processingTime={}ms", 
                    response.getTotalCount(), response.getProcessingTimeMs());
            
            return ApiResponse.success("Advanced search completed successfully", response);
            
        } catch (Exception e) {
            log.error("Advanced search failed: {}", e.getMessage(), e);
            return ApiResponse.error("Advanced search failed: " + e.getMessage());
        }
    }

    /**
     * Search similar images based on a reference image
     * 
     * @param imageId Reference image ID
     * @param limit Maximum number of results (default: 10)
     * @return API response with similar images
     */
    @GetMapping("/images/{imageId}/similar")
    public ApiResponse<SearchImageResponse> findSimilarImages(
            @PathVariable Long imageId,
            @RequestParam(value = "limit", defaultValue = "10") int limit) {
        
        try {
            if (imageId == null || imageId <= 0) {
                return ApiResponse.badRequest("Valid image ID is required");
            }
            
            SearchImageRequest request = new SearchImageRequest();
            request.setImageId(imageId);
            request.setLimit(limit);
            request.setMinConfidence(0.3); // Lower threshold for similarity search
            
            SearchImageResponse response = mcpServer.searchImage(request);
            
            log.info("Similar images search completed for image {}: {} results found", 
                    imageId, response.getTotalCount());
            
            return ApiResponse.success("Similar images found", response);
            
        } catch (Exception e) {
            log.error("Similar images search failed: {}", e.getMessage(), e);
            return ApiResponse.error("Similar images search failed: " + e.getMessage());
        }
    }

    /**
     * Search images by scene classification
     * 
     * @param scene Scene type (landscape, portrait, architecture, etc.)
     * @param limit Maximum number of results (default: 20)
     * @return API response with images of the specified scene
     */
    @GetMapping("/images/scene/{scene}")
    public ApiResponse<SearchImageResponse> searchByScene(
            @PathVariable String scene,
            @RequestParam(value = "limit", defaultValue = "20") int limit) {
        
        try {
            if (scene == null || scene.trim().isEmpty()) {
                return ApiResponse.badRequest("Scene type is required");
            }
            
            SearchImageRequest request = new SearchImageRequest();
            request.setSceneType(scene.toLowerCase());
            request.setLimit(limit);
            request.setMinConfidence(0.6);
            
            SearchImageResponse response = mcpServer.searchImage(request);
            
            log.info("Scene search completed for '{}': {} results found", 
                    scene, response.getTotalCount());
            
            return ApiResponse.success("Scene search completed", response);
            
        } catch (Exception e) {
            log.error("Scene search failed: {}", e.getMessage(), e);
            return ApiResponse.error("Scene search failed: " + e.getMessage());
        }
    }

    /**
     * Search images by dominant color
     * 
     * @param color Color query (red, blue, warm, cool, etc.)
     * @param limit Maximum number of results (default: 15)
     * @return API response with images matching the color query
     */
    @GetMapping("/images/color/{color}")
    public ApiResponse<SearchImageResponse> searchByColor(
            @PathVariable String color,
            @RequestParam(value = "limit", defaultValue = "15") int limit) {
        
        try {
            if (color == null || color.trim().isEmpty()) {
                return ApiResponse.badRequest("Color query is required");
            }
            
            SearchImageRequest request = new SearchImageRequest();
            request.setColorQuery(color.toLowerCase());
            request.setLimit(limit);
            request.setMinConfidence(0.5);
            
            SearchImageResponse response = mcpServer.searchImage(request);
            
            log.info("Color search completed for '{}': {} results found", 
                    color, response.getTotalCount());
            
            return ApiResponse.success("Color search completed", response);
            
        } catch (Exception e) {
            log.error("Color search failed: {}", e.getMessage(), e);
            return ApiResponse.error("Color search failed: " + e.getMessage());
        }
    }

    /**
     * Get available search categories and options
     * 
     * @return API response with search options
     */
    @GetMapping("/options")
    public ApiResponse<SearchOptions> getSearchOptions() {
        try {
            SearchOptions options = new SearchOptions();
            
            // Scene classifications
            options.setSceneTypes(java.util.Arrays.asList(
                "landscape", "portrait", "architecture", "nature", "urban", "indoor", "outdoor",
                "food", "animal", "vehicle", "technology", "art", "sports", "travel"
            ));
            
            // Color categories
            options.setColorTypes(java.util.Arrays.asList(
                "red", "blue", "green", "yellow", "orange", "purple", "pink", "brown",
                "black", "white", "gray", "warm", "cool", "neutral", "vibrant"
            ));
            
            // Search types
            options.setSearchTypes(java.util.Arrays.asList(
                "semantic", "visual_similarity", "color", "scene", "text"
            ));
            
            return ApiResponse.success("Search options retrieved", options);
            
        } catch (Exception e) {
            log.error("Failed to get search options: {}", e.getMessage(), e);
            return ApiResponse.error("Failed to get search options: " + e.getMessage());
        }
    }

    /**
     * Search options DTO
     */
    public static class SearchOptions {
        private java.util.List<String> sceneTypes;
        private java.util.List<String> colorTypes;
        private java.util.List<String> searchTypes;
        
        public java.util.List<String> getSceneTypes() { return sceneTypes; }
        public void setSceneTypes(java.util.List<String> sceneTypes) { this.sceneTypes = sceneTypes; }
        
        public java.util.List<String> getColorTypes() { return colorTypes; }
        public void setColorTypes(java.util.List<String> colorTypes) { this.colorTypes = colorTypes; }
        
        public java.util.List<String> getSearchTypes() { return searchTypes; }
        public void setSearchTypes(java.util.List<String> searchTypes) { this.searchTypes = searchTypes; }
    }
}