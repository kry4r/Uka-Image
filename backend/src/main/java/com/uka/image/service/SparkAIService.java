package com.uka.image.service;

import com.uka.image.entity.Image;
import com.uka.image.util.TagProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Metadata-based Image Search Service
 * Performs comprehensive metadata matching for image search without AI dependencies
 * 
 * @author Uka Team
 */
@Slf4j
@Service
public class SparkAIService {
    
    @Autowired
    private TagProcessor tagProcessor;

    /**
     * Find relevant images using comprehensive metadata-based matching
     * 
     * @param searchQuery User's search input
     * @param images List of available images with metadata
     * @return List of relevant image IDs sorted by relevance score
     */
    public List<Long> findRelevantImages(String searchQuery, List<Image> images) {
        try {
            log.info("Metadata-based Search - Query: '{}', Total images: {}", searchQuery, images.size());
            
            // Perform comprehensive metadata-based matching
            List<Long> result = performMetadataBasedSearch(searchQuery, images);
            log.info("Metadata-based Search - Found {} relevant images", result.size());
            
            return result;
            
        } catch (Exception e) {
            log.error("Metadata-based search failed for query '{}': {}", searchQuery, e.getMessage(), e);
            throw new RuntimeException("Metadata search failed: " + e.getMessage(), e);
        }
    }

    /**
     * Perform comprehensive metadata-based search using all available image properties
     * 
     * @param searchQuery User's search input
     * @param images List of available images with metadata
     * @return List of relevant image IDs sorted by relevance score
     */
    private List<Long> performMetadataBasedSearch(String searchQuery, List<Image> images) {
        log.info("Starting metadata-based search for query: '{}'", searchQuery);
        
        String normalizedQuery = searchQuery.toLowerCase().trim();
        List<ImageMatch> matches = new ArrayList<>();
        
        for (Image image : images) {
            double relevanceScore = calculateMetadataRelevanceScore(image, normalizedQuery);
            
            if (relevanceScore > 0.0) {
                ImageMatch match = new ImageMatch();
                match.setImageId(image.getId());
                match.setRelevanceScore(relevanceScore);
                match.setMatchDetails(generateMatchDetails(image, normalizedQuery));
                matches.add(match);
            }
        }
        
        // Sort by relevance score (highest first)
        matches.sort((a, b) -> Double.compare(b.getRelevanceScore(), a.getRelevanceScore()));
        
        // Extract image IDs
        List<Long> result = matches.stream()
            .map(ImageMatch::getImageId)
            .collect(Collectors.toList());
        
        log.info("Metadata-based search completed: {} matches found", result.size());
        return result;
    }
    
    /**
     * Calculate comprehensive metadata relevance score for an image
     * 
     * @param image Image to evaluate
     * @param query Normalized search query
     * @return Relevance score (0.0 to 1.0)
     */
    private double calculateMetadataRelevanceScore(Image image, String query) {
        double totalScore = 0.0;
        double maxScore = 0.0;
        
        // Filename matching (weight: 0.25)
        double filenameScore = calculateStringMatch(image.getFileName(), query);
        totalScore += filenameScore * 0.25;
        maxScore += 0.25;
        
        // Original name matching (weight: 0.20)
        double originalNameScore = calculateStringMatch(image.getOriginalName(), query);
        totalScore += originalNameScore * 0.20;
        maxScore += 0.20;
        
        // Description matching (weight: 0.30)
        double descriptionScore = calculateStringMatch(image.getDescription(), query);
        totalScore += descriptionScore * 0.30;
        maxScore += 0.30;
        
        // Tags matching (weight: 0.20)
        double tagScore = calculateTagMatch(image.getTags(), query);
        totalScore += tagScore * 0.20;
        maxScore += 0.20;
        
        // File type matching (weight: 0.05)
        double fileTypeScore = calculateStringMatch(image.getFileType(), query);
        totalScore += fileTypeScore * 0.05;
        maxScore += 0.05;
        
        // Normalize score to 0.0-1.0 range
        return maxScore > 0 ? totalScore / maxScore : 0.0;
    }
    
    /**
     * Calculate string matching score using multiple techniques
     * 
     * @param text Text to search in
     * @param query Search query
     * @return Match score (0.0 to 1.0)
     */
    private double calculateStringMatch(String text, String query) {
        if (text == null || text.trim().isEmpty() || query == null || query.trim().isEmpty()) {
            return 0.0;
        }
        
        String normalizedText = text.toLowerCase().trim();
        String normalizedQuery = query.toLowerCase().trim();
        
        // Exact match
        if (normalizedText.equals(normalizedQuery)) {
            return 1.0;
        }
        
        // Contains match
        if (normalizedText.contains(normalizedQuery)) {
            return 0.8;
        }
        
        // Word-based matching
        String[] textWords = normalizedText.split("\\s+");
        String[] queryWords = normalizedQuery.split("\\s+");
        
        int matchingWords = 0;
        for (String queryWord : queryWords) {
            for (String textWord : textWords) {
                if (textWord.contains(queryWord) || queryWord.contains(textWord)) {
                    matchingWords++;
                    break;
                }
            }
        }
        
        if (queryWords.length > 0) {
            double wordMatchRatio = (double) matchingWords / queryWords.length;
            return wordMatchRatio * 0.6;
        }
        
        return 0.0;
    }
    
    /**
     * Calculate tag matching score with enhanced processing
     * 
     * @param tags Image tags string
     * @param query Search query
     * @return Match score (0.0 to 1.0)
     */
    private double calculateTagMatch(String tags, String query) {
        if (tags == null || tags.trim().isEmpty()) {
            return 0.0;
        }
        
        List<String> processedTags = tagProcessor.processTags(tags);
        return tagProcessor.calculateTagRelevance(processedTags, query);
    }
    
    /**
     * Generate detailed match information for debugging
     * 
     * @param image Matched image
     * @param query Search query
     * @return Match details string
     */
    private String generateMatchDetails(Image image, String query) {
        StringBuilder details = new StringBuilder();
        details.append("Metadata matches for query '").append(query).append("': ");
        
        List<String> matches = new ArrayList<>();
        
        if (image.getFileName() != null && image.getFileName().toLowerCase().contains(query)) {
            matches.add("filename");
        }
        if (image.getOriginalName() != null && image.getOriginalName().toLowerCase().contains(query)) {
            matches.add("original name");
        }
        if (image.getDescription() != null && image.getDescription().toLowerCase().contains(query)) {
            matches.add("description");
        }
        if (image.getTags() != null && image.getTags().toLowerCase().contains(query)) {
            matches.add("tags");
        }
        if (image.getFileType() != null && image.getFileType().toLowerCase().contains(query)) {
            matches.add("file type");
        }
        
        details.append(String.join(", ", matches));
        return details.toString();
    }
    
    
    /**
     * Helper class for storing image match results
     */
    private static class ImageMatch {
        private Long imageId;
        private double relevanceScore;
        private String matchDetails;
        
        public Long getImageId() { return imageId; }
        public void setImageId(Long imageId) { this.imageId = imageId; }
        
        public double getRelevanceScore() { return relevanceScore; }
        public void setRelevanceScore(double relevanceScore) { this.relevanceScore = relevanceScore; }
        
        public String getMatchDetails() { return matchDetails; }
        public void setMatchDetails(String matchDetails) { this.matchDetails = matchDetails; }
    }

    /**
     * Check if metadata-based search service is available
     * Since this is now a local metadata-based service, it's always available
     * 
     * @return true (always available)
     */
    public boolean isServiceAvailable() {
        log.info("Metadata-based search service is always available");
        return true;
    }
}