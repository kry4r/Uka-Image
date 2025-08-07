package com.uka.image.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.uka.image.dto.ApiResponse;
import com.uka.image.dto.DetailedErrorResponse;
import com.uka.image.entity.Image;
import com.uka.image.service.ImageService;
import com.uka.image.service.RealSparkAIService;
import com.uka.image.util.SearchCriteriaBuilder;
import com.uka.image.util.WeightedScoringAlgorithm;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Enhanced AI Search Controller for intelligent image search with multi-dimensional matching
 * Integrates SearchCriteriaBuilder and WeightedScoringAlgorithm for comprehensive search
 * 
 * @author Uka Team
 */
@Slf4j
@RestController
@RequestMapping("/ai-search")
@CrossOrigin(origins = "*")
public class AISearchController {

    @Autowired
    private RealSparkAIService realSparkAIService;

    @Autowired
    private ImageService imageService;

    @Autowired
    private SearchCriteriaBuilder searchCriteriaBuilder;

    @Autowired
    private WeightedScoringAlgorithm scoringAlgorithm;

    /**
     * Perform enhanced AI-powered intelligent search with multi-dimensional matching
     * 
     * @param query Search query describing what user is looking for
     * @param pageNum Page number (default: 1)
     * @param pageSize Page size (default: 20)
     * @param fileFormats Optional file format filters (comma-separated)
     * @param orientation Optional orientation filter
     * @param minScore Optional minimum relevance score threshold
     * @return API response with enhanced AI-ranked search results
     */
    @GetMapping("/search")
    public ApiResponse<EnhancedSearchResponse> enhancedAiSearch(
            @RequestParam("query") String query,
            @RequestParam(value = "pageNum", defaultValue = "1") int pageNum,
            @RequestParam(value = "pageSize", defaultValue = "20") int pageSize,
            @RequestParam(value = "fileFormats", required = false) String fileFormats,
            @RequestParam(value = "orientation", required = false) String orientation,
            @RequestParam(value = "minScore", defaultValue = "0.1") double minScore,
            HttpServletRequest request) {
        
        long searchStartTime = System.currentTimeMillis();
        Map<String, Object> searchContext = new HashMap<>();
        List<String> processingSteps = new ArrayList<>();
        
        try {
            log.info("AI-powered search request: query='{}', page={}, size={}, formats={}, orientation={}", 
                query, pageNum, pageSize, fileFormats, orientation);
            
            processingSteps.add("Request received and validated");
            searchContext.put("searchQuery", query);
            searchContext.put("searchType", "AI_POWERED");
            searchContext.put("parameters", Map.of(
                "pageNum", pageNum,
                "pageSize", pageSize,
                "fileFormats", fileFormats != null ? fileFormats : "none",
                "orientation", orientation != null ? orientation : "none",
                "minScore", minScore
            ));

            // Build comprehensive search criteria
            SearchCriteriaBuilder.SearchCriteria criteria = searchCriteriaBuilder.buildCriteria(query);
            processingSteps.add("Search criteria built");
            
            // Apply additional filters from request parameters
            if (fileFormats != null && !fileFormats.trim().isEmpty()) {
                List<String> formatList = List.of(fileFormats.split(","));
                criteria.getTechnicalFilters().setFileFormats(new HashSet<>(formatList));
                processingSteps.add("File format filters applied");
            }
            
            if (orientation != null && !orientation.trim().isEmpty()) {
                criteria.getVisualFilters().setOrientation(orientation.toUpperCase());
                processingSteps.add("Orientation filter applied");
            }

            // Get all active images for analysis
            IPage<Image> allImagesPage = imageService.getAllActiveImages(1, 1000);
            List<Image> allImages = allImagesPage.getRecords();
            searchContext.put("totalImages", allImages.size());
            processingSteps.add("Retrieved " + allImages.size() + " images for analysis");

            if (allImages.isEmpty()) {
                log.info("No images available for metadata-based search");
                return ApiResponse.success("No images found", createEmptyResponse());
            }

            // Perform AI-powered search with comprehensive error handling
            List<WeightedScoringAlgorithm.ScoredResult> scoredResults = new ArrayList<>();
            boolean aiSearchUsed = false;
            
            try {
                log.info("Attempting iFlytek Spark AI search for query: '{}'", query);
                
                // Check if AI service is available
                if (realSparkAIService.isServiceAvailable()) {
                    List<Long> relevantIds = realSparkAIService.findRelevantImages(query, allImages);
                    processingSteps.add("iFlytek Spark AI search completed");
                    aiSearchUsed = true;
                    
                    if (!relevantIds.isEmpty()) {
                        // Filter images by AI search results and apply scoring
                        List<Image> filteredImages = allImages.stream()
                            .filter(img -> relevantIds.contains(img.getId()))
                            .collect(Collectors.toList());
                        
                        scoredResults = scoringAlgorithm.calculateScores(filteredImages, criteria);
                        processingSteps.add("AI-guided scoring algorithm applied to " + scoredResults.size() + " results");
                        log.info("iFlytek Spark AI search successful, found {} relevant images", scoredResults.size());
                    } else {
                        log.info("iFlytek Spark AI search returned no results, applying fallback scoring");
                        scoredResults = scoringAlgorithm.calculateScores(allImages, criteria);
                        processingSteps.add("AI returned no results, fallback scoring applied");
                    }
                } else {
                    log.warn("iFlytek Spark AI service is not available, using metadata-based fallback");
                    List<Long> relevantIds = realSparkAIService.findRelevantImages(query, allImages);
                    processingSteps.add("Metadata-based fallback search completed");
                    aiSearchUsed = false;
                    
                    if (!relevantIds.isEmpty()) {
                        List<Image> filteredImages = allImages.stream()
                            .filter(img -> relevantIds.contains(img.getId()))
                            .collect(Collectors.toList());
                        
                        scoredResults = scoringAlgorithm.calculateScores(filteredImages, criteria);
                        processingSteps.add("Metadata-based scoring applied to " + scoredResults.size() + " results");
                    } else {
                        scoredResults = scoringAlgorithm.calculateScores(allImages, criteria);
                        processingSteps.add("No metadata matches, comprehensive scoring applied");
                    }
                }
                
            } catch (Exception searchException) {
                searchContext.put("failurePoint", "ai_search");
                searchContext.put("aiSearchAttempted", true);
                searchContext.put("aiSearchUsed", aiSearchUsed);
                searchContext.put("processingSteps", processingSteps);
                searchContext.put("searchDuration", System.currentTimeMillis() - searchStartTime);
                
                log.error("AI search failed, falling back to comprehensive scoring: {}", searchException.getMessage());
                
                // Fallback to comprehensive scoring
                scoredResults = scoringAlgorithm.calculateScores(allImages, criteria);
                aiSearchUsed = false;
                processingSteps.add("AI search failed, comprehensive fallback scoring applied");
            }

            // Filter by minimum score threshold
            List<WeightedScoringAlgorithm.ScoredResult> filteredResults = scoringAlgorithm
                .filterByMinScore(scoredResults, minScore);
            processingSteps.add("Applied minimum score threshold: " + minScore);

            if (filteredResults.isEmpty()) {
                log.info("No images meet the minimum score threshold: {}", minScore);
                return ApiResponse.success("No relevant images found", createEmptyResponse());
            }

            // Apply pagination
            int totalResults = filteredResults.size();
            int startIndex = (pageNum - 1) * pageSize;
            int endIndex = Math.min(startIndex + pageSize, totalResults);
            
            List<WeightedScoringAlgorithm.ScoredResult> paginatedResults = 
                filteredResults.subList(startIndex, endIndex);
            processingSteps.add("Applied pagination: " + paginatedResults.size() + " results for page " + pageNum);

            // Create enhanced response with search information
            EnhancedSearchResponse response = createEnhancedResponse(
                paginatedResults, criteria, totalResults, pageNum, pageSize, aiSearchUsed);
            
            // Add comprehensive search information
            Map<String, Object> searchInfo = new HashMap<>();
            searchInfo.put("searchType", aiSearchUsed ? "AI_POWERED" : "METADATA_BASED");
            searchInfo.put("aiSearchUsed", aiSearchUsed);
            searchInfo.put("totalImagesAnalyzed", allImages.size());
            searchInfo.put("searchDuration", System.currentTimeMillis() - searchStartTime);
            searchInfo.put("processingSteps", processingSteps);
            searchInfo.put("searchStrategy", aiSearchUsed ? 
                "iFlytek Spark AI-powered intelligent search with AI-driven relevance scoring" : 
                "Comprehensive metadata matching with AI-driven relevance scoring");
            response.getSearchInsights().put("searchInfo", searchInfo);

            long searchDuration = System.currentTimeMillis() - searchStartTime;
            log.info("AI-powered search completed: found {} total results, returning {} for page {} in {}ms", 
                totalResults, paginatedResults.size(), pageNum, searchDuration);

            String responseMessage = String.format("Found %d relevant images using AI-powered search", totalResults);
            return ApiResponse.success(responseMessage, response);

        } catch (Exception e) {
            searchContext.put("failurePoint", "general_error");
            searchContext.put("processingSteps", processingSteps);
            searchContext.put("searchDuration", System.currentTimeMillis() - searchStartTime);
            
            // Create detailed error response
            DetailedErrorResponse detailedError = DetailedErrorResponse.fromSearchException(e, query, searchContext);
            detailedError.setUserAgent(request.getHeader("User-Agent"));
            detailedError.setClientIp(getClientIpAddress(request));
            detailedError.setRequestParameters(extractRequestParameters(request));
            
            // Log comprehensive error information
            log.error("AI-powered search encountered unexpected error with full context: {}", detailedError, e);
            
            // Return detailed error response
            ApiResponse<EnhancedSearchResponse> errorResponse = ApiResponse.error("Search failed: " + e.getMessage());
            Map<String, Object> debugInfo = new HashMap<>();
            debugInfo.put("detailedError", detailedError);
            debugInfo.put("searchContext", searchContext);
            debugInfo.put("processingSteps", processingSteps);
            debugInfo.put("errorOccurredAt", System.currentTimeMillis());
            
            return errorResponse;
        }
    }

    /**
     * Legacy AI search method for backward compatibility
     */
    @GetMapping("/search/legacy")
    public ApiResponse<List<Image>> legacyAiSearch(
            @RequestParam("query") String query,
            @RequestParam(value = "pageNum", defaultValue = "1") int pageNum,
            @RequestParam(value = "pageSize", defaultValue = "20") int pageSize,
            HttpServletRequest request) {
        
        try {
            // Use enhanced search and convert to legacy format
            ApiResponse<EnhancedSearchResponse> enhancedResponse = enhancedAiSearch(
                query, pageNum, pageSize, null, null, 0.1, request);
            
            if (enhancedResponse.getData() == null) {
                return ApiResponse.error("Enhanced search failed");
            }
            
            List<Image> legacyResults = enhancedResponse.getData().getResults().stream()
                .map(result -> result.getImage())
                .collect(Collectors.toList());
            
            return ApiResponse.success(enhancedResponse.getMessage(), legacyResults);
            
        } catch (Exception e) {
            log.error("Legacy AI search failed for query '{}': {}", query, e.getMessage(), e);
            return ApiResponse.error("Legacy AI search failed: " + e.getMessage());
        }
    }

    /**
     * Check iFlytek Spark AI service health with detailed diagnostics
     * 
     * @return API response with detailed service status
     */
    @GetMapping("/health")
    public ApiResponse<Map<String, Object>> checkHealth() {
        Map<String, Object> healthInfo = new HashMap<>();
        
        try {
            log.info("Performing iFlytek Spark AI service health check");
            
            long startTime = System.currentTimeMillis();
            boolean isAvailable = realSparkAIService.isServiceAvailable();
            long responseTime = System.currentTimeMillis() - startTime;
            
            healthInfo.put("service", "iFlytek Spark AI");
            healthInfo.put("status", isAvailable ? "UP" : "DOWN");
            healthInfo.put("responseTime", responseTime + "ms");
            healthInfo.put("endpoint", "https://spark-api-open.xf-yun.com/v1/chat/completions");
            healthInfo.put("timestamp", System.currentTimeMillis());
            
            if (isAvailable) {
                healthInfo.put("message", "iFlytek Spark AI service is available and responding");
                log.info("iFlytek Spark AI health check: UP ({}ms)", responseTime);
                return ApiResponse.success("iFlytek Spark AI service is healthy", healthInfo);
            } else {
                healthInfo.put("message", "iFlytek Spark AI service is not responding properly");
                healthInfo.put("troubleshooting", List.of(
                    "Check API credentials (APIPassword) in application.yml",
                    "Verify network connectivity to iFlytek Spark API",
                    "Check if API quota/rate limits are exceeded",
                    "Ensure the selected model is available"
                ));
                log.warn("iFlytek Spark AI health check: DOWN ({}ms)", responseTime);
                ApiResponse<Map<String, Object>> response = ApiResponse.error("iFlytek Spark AI service is not available");
                response.setData(healthInfo);
                return response;
            }

        } catch (Exception e) {
            healthInfo.put("service", "iFlytek Spark AI");
            healthInfo.put("status", "ERROR");
            healthInfo.put("error", e.getMessage());
            healthInfo.put("timestamp", System.currentTimeMillis());
            healthInfo.put("troubleshooting", List.of(
                "Check application logs for detailed error information",
                "Verify iFlytek Spark API credentials",
                "Check network connectivity",
                "Ensure proper configuration in application.yml"
            ));
            
            log.error("iFlytek Spark AI service health check failed: {}", e.getMessage(), e);
            ApiResponse<Map<String, Object>> response = ApiResponse.error("iFlytek Spark AI health check failed: " + e.getMessage());
            response.setData(healthInfo);
            return response;
        }
    }


    /**
     * Get search analytics and insights
     * 
     * @return API response with search analytics data
     */
    @GetMapping("/analytics")
    public ApiResponse<SearchAnalyticsResponse> getSearchAnalytics() {
        try {
            SearchAnalyticsResponse analytics = new SearchAnalyticsResponse();
            
            // Get total images count
            IPage<Image> allImages = imageService.getAllActiveImages(1, 1);
            analytics.setTotalImages(Math.toIntExact(allImages.getTotal()));
            
            // Mock analytics data (in real implementation, this would come from search logs)
            analytics.setTotalSearches(1250);
            analytics.setSuccessfulSearches(1100);
            analytics.setAverageResultsPerSearch(8.5);
            analytics.setAverageSearchTime(0.85);
            
            Map<String, Integer> popularSearchTerms = new HashMap<>();
            popularSearchTerms.put("nature", 145);
            popularSearchTerms.put("portrait", 120);
            popularSearchTerms.put("landscape", 98);
            popularSearchTerms.put("abstract", 87);
            popularSearchTerms.put("architecture", 76);
            
            analytics.setPopularSearchTerms(popularSearchTerms);
            
            return ApiResponse.success("Search analytics retrieved", analytics);
            
        } catch (Exception e) {
            log.error("Failed to get search analytics: {}", e.getMessage(), e);
            return ApiResponse.error("Failed to get analytics: " + e.getMessage());
        }
    }

    // Helper methods
    private EnhancedSearchResponse createEmptyResponse() {
        EnhancedSearchResponse response = new EnhancedSearchResponse();
        response.setResults(new ArrayList<>());
        response.setTotalResults(0);
        response.setCurrentPage(1);
        response.setPageSize(20);
        response.setTotalPages(0);
        response.setSearchStrategy("No results found");
        response.setAiSearchUsed(false);
        response.setSearchCriteria(null);
        
        // Initialize searchInsights to prevent null pointer exceptions
        Map<String, Object> insights = new HashMap<>();
        insights.put("queryType", "NONE");
        insights.put("searchComplexity", 0);
        insights.put("hasFilters", false);
        insights.put("averageScore", 0.0);
        response.setSearchInsights(insights);
        
        return response;
    }

    private EnhancedSearchResponse createEnhancedResponse(
            List<WeightedScoringAlgorithm.ScoredResult> results,
            SearchCriteriaBuilder.SearchCriteria criteria,
            int totalResults,
            int pageNum,
            int pageSize,
            boolean aiSearchUsed) {
        
        EnhancedSearchResponse response = new EnhancedSearchResponse();
        response.setResults(results);
        response.setTotalResults(totalResults);
        response.setCurrentPage(pageNum);
        response.setPageSize(pageSize);
        response.setTotalPages((int) Math.ceil((double) totalResults / pageSize));
        response.setSearchCriteria(criteria);
        response.setSearchStrategy(generateSearchStrategy(criteria, aiSearchUsed));
        response.setAiSearchUsed(aiSearchUsed);
        
        // Add search insights
        Map<String, Object> insights = new HashMap<>();
        insights.put("queryType", criteria.getPrimaryType());
        insights.put("searchComplexity", criteria.getSearchTerms().size());
        insights.put("hasFilters", !criteria.getTechnicalFilters().getFileFormats().isEmpty() || 
                                  criteria.getVisualFilters().getOrientation() != null);
        insights.put("averageScore", results.stream()
                .mapToDouble(WeightedScoringAlgorithm.ScoredResult::getTotalScore)
                .average().orElse(0.0));
        
        response.setSearchInsights(insights);
        
        return response;
    }

    private String generateSearchStrategy(SearchCriteriaBuilder.SearchCriteria criteria, boolean metadataSearchUsed) {
        StringBuilder strategy = new StringBuilder();
        
        if (metadataSearchUsed) {
            strategy.append("Metadata-based matching with ");
        } else {
            strategy.append("Weighted scoring algorithm with ");
        }
        
        strategy.append(criteria.getPrimaryType().toString().toLowerCase()).append(" focus");
        
        if (!criteria.getTechnicalFilters().getFileFormats().isEmpty()) {
            strategy.append(", filtered by file formats");
        }
        
        if (criteria.getVisualFilters().getOrientation() != null) {
            strategy.append(", filtered by orientation");
        }
        
        return strategy.toString();
    }
    
    /**
     * Extract client IP address from request
     * 
     * @param request HTTP request
     * @return Client IP address
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0];
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
    
    /**
     * Extract request parameters for debugging
     * 
     * @param request HTTP request
     * @return Map of request parameters
     */
    private Map<String, Object> extractRequestParameters(HttpServletRequest request) {
        Map<String, Object> parameters = new HashMap<>();
        
        request.getParameterMap().forEach((key, values) -> {
            if (values.length == 1) {
                parameters.put(key, values[0]);
            } else {
                parameters.put(key, Arrays.asList(values));
            }
        });
        
        return parameters;
    }

    // Response classes
    @Data
    public static class EnhancedSearchResponse {
        private List<WeightedScoringAlgorithm.ScoredResult> results;
        private int totalResults;
        private int currentPage;
        private int pageSize;
        private int totalPages;
        private SearchCriteriaBuilder.SearchCriteria searchCriteria;
        private String searchStrategy;
        private boolean aiSearchUsed;
        private Map<String, Object> searchInsights;
    }


    @Data
    public static class SearchAnalyticsResponse {
        private int totalImages;
        private int totalSearches;
        private int successfulSearches;
        private double averageResultsPerSearch;
        private double averageSearchTime;
        private Map<String, Integer> popularSearchTerms;
    }
}
