package com.uka.image.util;

import com.uka.image.entity.Image;
import com.uka.image.util.WeightedScoringAlgorithm.ScoredResult;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * SearchResultProcessor to format and enrich AI search responses
 * Provides detailed search result information with scoring explanations
 * 
 * @author Uka Team
 */
@Slf4j
@Component
public class SearchResultProcessor {

    /**
     * Process and enrich search results with detailed metadata and scoring information
     * 
     * @param scoredResults List of scored results from WeightedScoringAlgorithm
     * @param query Original search query
     * @return Enriched search response with detailed information
     */
    public EnrichedSearchResponse processResults(List<ScoredResult> scoredResults, String query) {
        EnrichedSearchResponse response = new EnrichedSearchResponse();
        response.setQuery(query);
        response.setTotalResults(scoredResults.size());
        response.setTimestamp(new Date());
        
        // Calculate confidence metrics
        double averageScore = scoredResults.stream()
            .mapToDouble(ScoredResult::getTotalScore)
            .average()
            .orElse(0.0);
        
        double highestScore = scoredResults.isEmpty() ? 0.0 : 
            scoredResults.get(0).getTotalScore();
            
        response.setAverageConfidence(averageScore);
        response.setHighestConfidence(highestScore);
        
        // Determine search quality
        String searchQuality = determineSearchQuality(highestScore, averageScore, scoredResults.size());
        response.setSearchQuality(searchQuality);
        
        // Process individual results
        List<EnrichedResult> enrichedResults = scoredResults.stream()
            .map(this::enrichResult)
            .collect(Collectors.toList());
        
        response.setResults(enrichedResults);
        
        // Generate search insights
        response.setInsights(generateSearchInsights(scoredResults, query));
        
        return response;
    }
    
    /**
     * Enrich individual search result with detailed information
     * 
     * @param scoredResult Scored result from WeightedScoringAlgorithm
     * @return Enriched result with detailed information
     */
    private EnrichedResult enrichResult(ScoredResult scoredResult) {
        EnrichedResult result = new EnrichedResult();
        Image image = scoredResult.getImage();
        
        // Basic image information
        result.setImageId(image.getId());
        result.setFileName(image.getFileName());
        result.setOriginalName(image.getOriginalName());
        result.setFilePath(image.getFilePath());
        result.setCosUrl(image.getCosUrl());
        result.setThumbnailUrl(image.getThumbnailUrl());
        result.setFileSize(image.getFileSize());
        result.setFileType(image.getFileType());
        result.setWidth(image.getWidth());
        result.setHeight(image.getHeight());
        result.setDescription(image.getDescription());
        result.setTags(image.getTags());
        result.setUploaderUsername(image.getUploaderUsername());
        result.setCreatedAt(image.getCreatedAt() != null ? 
            java.sql.Timestamp.valueOf(image.getCreatedAt()) : null);
        
        // Enhanced metadata
        result.setFileFormat(image.getFileFormat());
        result.setResolutionCategory(image.getResolutionCategory());
        result.setOrientation(image.getOrientation());
        result.setContentCategory(image.getContentCategory());
        result.setDominantColors(image.getDominantColors());
        result.setAiGeneratedTags(image.getAiGeneratedTags());
        result.setSemanticKeywords(image.getSemanticKeywords());
        
        // Scoring information
        result.setTotalScore(scoredResult.getTotalScore());
        result.setDescriptionScore(scoredResult.getDescriptionScore());
        result.setTagScore(scoredResult.getTagScore());
        result.setFilenameScore(scoredResult.getFilenameScore());
        result.setMetadataScore(scoredResult.getMetadataScore());
        result.setBonusScore(scoredResult.getBonusScore());
        result.setPenaltyScore(scoredResult.getPenaltyScore());
        result.setExplanation(scoredResult.getExplanation());
        
        // Calculate confidence level
        String confidenceLevel = calculateConfidenceLevel(scoredResult.getTotalScore());
        result.setConfidenceLevel(confidenceLevel);
        
        return result;
    }
    
    /**
     * Calculate confidence level based on total score
     * 
     * @param totalScore Total relevance score
     * @return Confidence level string
     */
    private String calculateConfidenceLevel(double totalScore) {
        if (totalScore >= 0.8) {
            return "VERY_HIGH";
        } else if (totalScore >= 0.6) {
            return "HIGH";
        } else if (totalScore >= 0.4) {
            return "MEDIUM";
        } else if (totalScore >= 0.2) {
            return "LOW";
        } else {
            return "VERY_LOW";
        }
    }
    
    /**
     * Determine overall search quality based on scores and result count
     * 
     * @param highestScore Highest relevance score
     * @param averageScore Average relevance score
     * @param resultCount Number of results
     * @return Search quality string
     */
    private String determineSearchQuality(double highestScore, double averageScore, int resultCount) {
        if (resultCount == 0) {
            return "NO_RESULTS";
        }
        
        if (highestScore >= 0.8 && averageScore >= 0.6 && resultCount >= 5) {
            return "EXCELLENT";
        } else if (highestScore >= 0.7 && averageScore >= 0.5 && resultCount >= 3) {
            return "GOOD";
        } else if (highestScore >= 0.5 && averageScore >= 0.3) {
            return "FAIR";
        } else if (highestScore >= 0.3) {
            return "POOR";
        } else {
            return "VERY_POOR";
        }
    }
    
    /**
     * Generate search insights based on scored results
     * 
     * @param scoredResults List of scored results
     * @param query Original search query
     * @return Map of search insights
     */
    private Map<String, Object> generateSearchInsights(List<ScoredResult> scoredResults, String query) {
        Map<String, Object> insights = new HashMap<>();
        
        if (scoredResults.isEmpty()) {
            insights.put("message", "No matching results found for your query");
            insights.put("suggestions", Arrays.asList(
                "Try using more general terms",
                "Check for spelling mistakes",
                "Try searching by file type or image properties"
            ));
            return insights;
        }
        
        // Calculate match distribution
        Map<String, Double> matchDistribution = new HashMap<>();
        double totalDescriptionScore = 0.0;
        double totalTagScore = 0.0;
        double totalFilenameScore = 0.0;
        double totalMetadataScore = 0.0;
        
        for (ScoredResult result : scoredResults) {
            totalDescriptionScore += result.getDescriptionScore();
            totalTagScore += result.getTagScore();
            totalFilenameScore += result.getFilenameScore();
            totalMetadataScore += result.getMetadataScore();
        }
        
        double total = totalDescriptionScore + totalTagScore + totalFilenameScore + totalMetadataScore;
        if (total > 0) {
            matchDistribution.put("description", totalDescriptionScore / total);
            matchDistribution.put("tags", totalTagScore / total);
            matchDistribution.put("filename", totalFilenameScore / total);
            matchDistribution.put("metadata", totalMetadataScore / total);
        }
        
        insights.put("matchDistribution", matchDistribution);
        
        // Extract common metadata patterns
        Map<String, List<String>> commonPatterns = new HashMap<>();
        List<String> commonFormats = new ArrayList<>();
        List<String> commonResolutions = new ArrayList<>();
        List<String> commonOrientations = new ArrayList<>();
        List<String> commonContentCategories = new ArrayList<>();
        
        for (ScoredResult result : scoredResults) {
            Image image = result.getImage();
            
            if (image.getFileFormat() != null && !commonFormats.contains(image.getFileFormat())) {
                commonFormats.add(image.getFileFormat());
            }
            
            if (image.getResolutionCategory() != null && !commonResolutions.contains(image.getResolutionCategory())) {
                commonResolutions.add(image.getResolutionCategory());
            }
            
            if (image.getOrientation() != null && !commonOrientations.contains(image.getOrientation())) {
                commonOrientations.add(image.getOrientation());
            }
            
            if (image.getContentCategory() != null && !commonContentCategories.contains(image.getContentCategory())) {
                commonContentCategories.add(image.getContentCategory());
            }
        }
        
        commonPatterns.put("formats", commonFormats);
        commonPatterns.put("resolutions", commonResolutions);
        commonPatterns.put("orientations", commonOrientations);
        commonPatterns.put("contentCategories", commonContentCategories);
        
        insights.put("commonPatterns", commonPatterns);
        
        
        return insights;
    }
    
    
    /**
     * Enriched search response with detailed information
     */
    @Data
    public static class EnrichedSearchResponse {
        private String query;
        private int totalResults;
        private Date timestamp;
        private double averageConfidence;
        private double highestConfidence;
        private String searchQuality;
        private List<EnrichedResult> results;
        private Map<String, Object> insights;
    }
    
    /**
     * Enriched search result with detailed information
     */
    @Data
    public static class EnrichedResult {
        // Basic image information
        private Long imageId;
        private String fileName;
        private String originalName;
        private String filePath;
        private String cosUrl;
        private String thumbnailUrl;
        private Long fileSize;
        private String fileType;
        private Integer width;
        private Integer height;
        private String description;
        private String tags;
        private String uploaderUsername;
        private Date createdAt;
        
        // Enhanced metadata
        private String fileFormat;
        private String resolutionCategory;
        private String orientation;
        private String contentCategory;
        private String dominantColors;
        private String aiGeneratedTags;
        private String semanticKeywords;
        
        // Scoring information
        private double totalScore;
        private double descriptionScore;
        private double tagScore;
        private double filenameScore;
        private double metadataScore;
        private double bonusScore;
        private double penaltyScore;
        private String explanation;
        private String confidenceLevel;
    }
}