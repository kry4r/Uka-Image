package com.uka.image.mcp;

import lombok.Data;
import java.util.List;

/**
 * Search image response DTO for MCP Server
 * 
 * @author Uka Team
 */
@Data
public class SearchImageResponse {
    
    private String query;                    // Original search query
    private List<SearchResult> results;      // Search results
    private int totalCount;                  // Total number of results
    private long timestamp;                  // Search timestamp
    private long processingTimeMs;           // Processing time in milliseconds
    private String searchStrategy;           // Search strategy used
    private boolean hasMore;                 // Whether there are more results available
    
    public SearchImageResponse() {}
    
    public SearchImageResponse(String query, List<SearchResult> results) {
        this.query = query;
        this.results = results;
        this.totalCount = results != null ? results.size() : 0;
        this.timestamp = System.currentTimeMillis();
    }
}