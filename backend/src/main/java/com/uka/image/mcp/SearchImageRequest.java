package com.uka.image.mcp;

import lombok.Data;

/**
 * Search image request DTO for MCP Server
 * 
 * @author Uka Team
 */
@Data
public class SearchImageRequest {
    
    private String query;           // Text query for semantic search
    private Long imageId;           // Reference image ID for visual similarity search
    private String colorQuery;      // Color-based search query
    private String sceneType;       // Scene classification filter
    private int limit = 10;         // Maximum number of results
    private double minConfidence = 0.5; // Minimum confidence threshold
    private String[] searchTypes;   // Types of search to perform: semantic, visual, color, scene
    
    public SearchImageRequest() {}
    
    public SearchImageRequest(String query) {
        this.query = query;
    }
    
    public SearchImageRequest(String query, int limit) {
        this.query = query;
        this.limit = limit;
    }
}