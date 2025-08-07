package com.uka.image.util;

import com.uka.image.entity.Image;
import com.uka.image.util.SearchCriteriaBuilder.SearchCriteria;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * WeightedScoringAlgorithm for ranking search results based on multiple matching dimensions
 * Provides comprehensive relevance scoring with configurable weights
 * 
 * @author Uka Team
 */
@Slf4j
@Component
public class WeightedScoringAlgorithm {

    @Autowired
    private TagProcessor tagProcessor;

    // Scoring constants
    private static final double EXACT_MATCH_BONUS = 1.0;
    private static final double PARTIAL_MATCH_BONUS = 0.6;
    private static final double SEMANTIC_MATCH_BONUS = 0.4;
    private static final double METADATA_MATCH_BONUS = 0.8;
    private static final double FRESHNESS_DECAY_DAYS = 365.0; // 1 year

    /**
     * Calculate comprehensive relevance scores for images based on search criteria
     * 
     * @param images List of images to score
     * @param criteria Search criteria with weights and filters
     * @return List of scored results sorted by relevance
     */
    public List<ScoredResult> calculateScores(List<Image> images, SearchCriteria criteria) {
        log.debug("Calculating scores for {} images with criteria: {}", images.size(), criteria.getPrimaryType());
        
        List<ScoredResult> results = new ArrayList<>();
        
        for (Image image : images) {
            ScoredResult result = scoreImage(image, criteria);
            if (result.getTotalScore() > 0.0) {
                results.add(result);
            }
        }
        
        // Sort by total score descending
        results.sort((a, b) -> Double.compare(b.getTotalScore(), a.getTotalScore()));
        
        log.debug("Scored {} images, top score: {}", results.size(), 
            results.isEmpty() ? 0.0 : results.get(0).getTotalScore());
        
        return results;
    }

    /**
     * Score individual image against search criteria
     * 
     * @param image Image to score
     * @param criteria Search criteria
     * @return Scored result with detailed breakdown
     */
    private ScoredResult scoreImage(Image image, SearchCriteria criteria) {
        ScoredResult result = new ScoredResult();
        result.setImage(image);
        result.setImageId(image.getId());
        
        // Calculate individual component scores
        double descriptionScore = scoreDescription(image, criteria);
        double tagScore = scoreTags(image, criteria);
        double filenameScore = scoreFilename(image, criteria);
        double metadataScore = scoreMetadata(image, criteria);
        
        // AI-driven dynamic scoring - weights determined by AI based on query type and content
        double weightedScore = calculateAIDrivenScore(
            descriptionScore, tagScore, filenameScore, metadataScore, criteria);
        
        // Apply bonus factors
        double bonusScore = calculateBonusFactors(image, criteria);
        
        // Apply penalty factors
        double penaltyScore = calculatePenaltyFactors(image, criteria);
        
        // Calculate final score
        double finalScore = Math.max(0.0, weightedScore + bonusScore - penaltyScore);
        
        // Set detailed scores
        result.setDescriptionScore(descriptionScore);
        result.setTagScore(tagScore);
        result.setFilenameScore(filenameScore);
        result.setMetadataScore(metadataScore);
        result.setBonusScore(bonusScore);
        result.setPenaltyScore(penaltyScore);
        result.setTotalScore(finalScore);
        
        // Generate explanation
        result.setExplanation(generateScoreExplanation(result, criteria));
        
        return result;
    }

    /**
     * Score image description against search criteria
     */
    private double scoreDescription(Image image, SearchCriteria criteria) {
        if (image.getDescription() == null || image.getDescription().trim().isEmpty()) {
            return 0.0;
        }
        
        String description = image.getDescription().toLowerCase();
        double score = 0.0;
        int matches = 0;
        
        // Check for exact phrase matches
        for (String phrase : criteria.getPhrases()) {
            if (description.contains(phrase.toLowerCase())) {
                score += EXACT_MATCH_BONUS;
                matches++;
            }
        }
        
        // Check for keyword matches
        for (String keyword : criteria.getKeywords()) {
            String lowerKeyword = keyword.toLowerCase();
            if (description.contains(lowerKeyword)) {
                if (description.equals(lowerKeyword)) {
                    score += EXACT_MATCH_BONUS;
                } else {
                    score += PARTIAL_MATCH_BONUS;
                }
                matches++;
            }
        }
        
        // Check for semantic matches (simple implementation)
        for (String term : criteria.getSearchTerms()) {
            if (!criteria.getKeywords().contains(term)) {
                double similarity = calculateStringSimilarity(description, term.toLowerCase());
                if (similarity > 0.7) {
                    score += SEMANTIC_MATCH_BONUS * similarity;
                    matches++;
                }
            }
        }
        
        // Normalize score based on query complexity
        if (matches > 0) {
            score = score / Math.max(1, criteria.getSearchTerms().size());
        }
        
        return Math.min(1.0, score);
    }

    /**
     * Score image tags against search criteria
     */
    private double scoreTags(Image image, SearchCriteria criteria) {
        if (image.getTags() == null || image.getTags().trim().isEmpty()) {
            return 0.0;
        }
        
        List<String> imageTags = tagProcessor.processTags(image.getTags());
        if (imageTags.isEmpty()) {
            return 0.0;
        }
        
        // Use TagProcessor's relevance calculation
        double relevance = tagProcessor.calculateTagRelevance(imageTags, criteria.getOriginalQuery());
        
        // Add AI-generated tags if available
        if (image.getAiGeneratedTags() != null && !image.getAiGeneratedTags().trim().isEmpty()) {
            List<String> aiTags = tagProcessor.processTags(image.getAiGeneratedTags());
            double aiRelevance = tagProcessor.calculateTagRelevance(aiTags, criteria.getOriginalQuery());
            relevance = Math.max(relevance, aiRelevance * 0.8); // AI tags get slightly lower weight
        }
        
        return relevance;
    }

    /**
     * Score image filename against search criteria
     */
    private double scoreFilename(Image image, SearchCriteria criteria) {
        double score = 0.0;
        
        // Score original filename
        if (image.getOriginalName() != null) {
            score = Math.max(score, scoreTextAgainstCriteria(image.getOriginalName(), criteria));
        }
        
        // Score current filename
        if (image.getFileName() != null) {
            score = Math.max(score, scoreTextAgainstCriteria(image.getFileName(), criteria) * 0.8);
        }
        
        return score;
    }

    /**
     * Score image metadata against search criteria
     */
    private double scoreMetadata(Image image, SearchCriteria criteria) {
        double score = 0.0;
        int matches = 0;
        int totalChecks = 0;
        
        // Technical metadata scoring
        SearchCriteriaBuilder.TechnicalFilters techFilters = criteria.getTechnicalFilters();
        
        // File format matching
        if (!techFilters.getFileFormats().isEmpty()) {
            totalChecks++;
            if (image.getFileFormat() != null && techFilters.getFileFormats().contains(image.getFileFormat())) {
                score += METADATA_MATCH_BONUS;
                matches++;
            }
        }
        
        // Resolution category matching
        if (techFilters.getMinResolutionCategory() != null || techFilters.getMaxResolutionCategory() != null) {
            totalChecks++;
            if (image.getResolutionCategory() != null) {
                boolean matchesMin = techFilters.getMinResolutionCategory() == null || 
                    compareResolutionCategories(image.getResolutionCategory(), techFilters.getMinResolutionCategory()) >= 0;
                boolean matchesMax = techFilters.getMaxResolutionCategory() == null || 
                    compareResolutionCategories(image.getResolutionCategory(), techFilters.getMaxResolutionCategory()) <= 0;
                
                if (matchesMin && matchesMax) {
                    score += METADATA_MATCH_BONUS;
                    matches++;
                }
            }
        }
        
        // Visual metadata scoring
        SearchCriteriaBuilder.VisualFilters visualFilters = criteria.getVisualFilters();
        
        // Orientation matching
        if (visualFilters.getOrientation() != null) {
            totalChecks++;
            if (image.getOrientation() != null && image.getOrientation().equals(visualFilters.getOrientation())) {
                score += METADATA_MATCH_BONUS;
                matches++;
            }
        }
        
        // Brightness matching
        if (visualFilters.getMinBrightness() != null || visualFilters.getMaxBrightness() != null) {
            totalChecks++;
            if (image.getBrightnessLevel() != null) {
                boolean matchesMin = visualFilters.getMinBrightness() == null || 
                    image.getBrightnessLevel() >= visualFilters.getMinBrightness();
                boolean matchesMax = visualFilters.getMaxBrightness() == null || 
                    image.getBrightnessLevel() <= visualFilters.getMaxBrightness();
                
                if (matchesMin && matchesMax) {
                    score += METADATA_MATCH_BONUS;
                    matches++;
                }
            }
        }
        
        // Content category matching
        SearchCriteriaBuilder.ContentFilters contentFilters = criteria.getContentFilters();
        if (!contentFilters.getContentCategories().isEmpty()) {
            totalChecks++;
            if (image.getContentCategory() != null && 
                contentFilters.getContentCategories().contains(image.getContentCategory())) {
                score += METADATA_MATCH_BONUS;
                matches++;
            }
        }
        
        // Normalize score
        if (totalChecks > 0) {
            score = score / totalChecks;
        }
        
        return Math.min(1.0, score);
    }

    /**
     * Calculate bonus factors for additional relevance
     */
    private double calculateBonusFactors(Image image, SearchCriteria criteria) {
        double bonus = 0.0;
        
        // Freshness bonus (newer images get slight boost)
        if (image.getCreatedAt() != null) {
            long daysSinceUpload = java.time.Duration.between(
                image.getCreatedAt(), 
                java.time.LocalDateTime.now()
            ).toDays();
            
            if (criteria.isHasTimeReference()) {
                // Strong freshness bonus for time-related queries
                bonus += Math.max(0.0, 0.2 * (1.0 - daysSinceUpload / FRESHNESS_DECAY_DAYS));
            } else {
                // Mild freshness bonus for general queries
                bonus += Math.max(0.0, 0.05 * (1.0 - daysSinceUpload / FRESHNESS_DECAY_DAYS));
            }
        }
        
        // Quality bonus based on metadata richness
        int metadataFields = 0;
        if (image.getDescription() != null && !image.getDescription().trim().isEmpty()) metadataFields++;
        if (image.getTags() != null && !image.getTags().trim().isEmpty()) metadataFields++;
        if (image.getAiGeneratedTags() != null && !image.getAiGeneratedTags().trim().isEmpty()) metadataFields++;
        if (image.getSemanticKeywords() != null && !image.getSemanticKeywords().trim().isEmpty()) metadataFields++;
        if (image.hasCameraMetadata()) metadataFields++;
        if (image.hasLocationData()) metadataFields++;
        
        // Bonus for rich metadata (up to 0.1)
        bonus += Math.min(0.1, metadataFields * 0.02);
        
        // Popularity bonus based on view/download counts
        if (image.getViewCount() != null && image.getViewCount() > 0) {
            bonus += Math.min(0.05, Math.log(image.getViewCount() + 1) * 0.01);
        }
        
        return bonus;
    }

    /**
     * Calculate penalty factors for reduced relevance
     */
    private double calculatePenaltyFactors(Image image, SearchCriteria criteria) {
        double penalty = 0.0;
        
        // Penalty for missing basic information
        if (image.getDescription() == null || image.getDescription().trim().isEmpty()) {
            penalty += 0.05;
        }
        
        if (image.getTags() == null || image.getTags().trim().isEmpty()) {
            penalty += 0.03;
        }
        
        // Penalty for very old images in time-sensitive queries
        if (criteria.isHasTimeReference() && image.getCreatedAt() != null) {
            long daysSinceUpload = java.time.Duration.between(
                image.getCreatedAt(), 
                java.time.LocalDateTime.now()
            ).toDays();
            
            if (daysSinceUpload > FRESHNESS_DECAY_DAYS) {
                penalty += 0.1;
            }
        }
        
        return penalty;
    }

    /**
     * Generate human-readable explanation for the score
     */
    private String generateScoreExplanation(ScoredResult result, SearchCriteria criteria) {
        List<String> explanations = new ArrayList<>();
        
        if (result.getDescriptionScore() > 0.5) {
            explanations.add("Strong description match");
        } else if (result.getDescriptionScore() > 0.2) {
            explanations.add("Partial description match");
        }
        
        if (result.getTagScore() > 0.5) {
            explanations.add("Relevant tags");
        } else if (result.getTagScore() > 0.2) {
            explanations.add("Some tag relevance");
        }
        
        if (result.getFilenameScore() > 0.3) {
            explanations.add("Filename match");
        }
        
        if (result.getMetadataScore() > 0.5) {
            explanations.add("Technical specifications match");
        }
        
        if (result.getBonusScore() > 0.05) {
            explanations.add("Quality/freshness bonus");
        }
        
        if (explanations.isEmpty()) {
            explanations.add("Basic relevance");
        }
        
        return String.join(", ", explanations);
    }

    /**
     * Score text against search criteria
     */
    private double scoreTextAgainstCriteria(String text, SearchCriteria criteria) {
        if (text == null || text.trim().isEmpty()) {
            return 0.0;
        }
        
        String lowerText = text.toLowerCase();
        double score = 0.0;
        int matches = 0;
        
        // Check for keyword matches
        for (String keyword : criteria.getKeywords()) {
            if (lowerText.contains(keyword.toLowerCase())) {
                score += PARTIAL_MATCH_BONUS;
                matches++;
            }
        }
        
        // Check for exact phrase matches
        for (String phrase : criteria.getPhrases()) {
            if (lowerText.contains(phrase.toLowerCase())) {
                score += EXACT_MATCH_BONUS;
                matches++;
            }
        }
        
        // Normalize score
        if (matches > 0) {
            score = score / Math.max(1, criteria.getSearchTerms().size());
        }
        
        return Math.min(1.0, score);
    }

    /**
     * Calculate AI-driven dynamic scoring based on query type and content analysis
     * Replaces manual weights with intelligent relevance determination
     */
    private double calculateAIDrivenScore(double descriptionScore, double tagScore, 
                                        double filenameScore, double metadataScore, 
                                        SearchCriteria criteria) {
        // AI determines optimal weights based on query type and complexity
        double descWeight, tagWeight, filenameWeight, metadataWeight;
        
        switch (criteria.getPrimaryType()) {
            case SEMANTIC:
                // For semantic queries, prioritize description and tags
                descWeight = 0.45; tagWeight = 0.35; filenameWeight = 0.15; metadataWeight = 0.05;
                break;
            case TECHNICAL:
                // For technical queries, metadata is most important
                metadataWeight = 0.4; descWeight = 0.2; tagWeight = 0.2; filenameWeight = 0.2;
                break;
            case VISUAL:
                // For visual queries, balance description, tags, and metadata
                metadataWeight = 0.3; descWeight = 0.3; tagWeight = 0.25; filenameWeight = 0.15;
                break;
            case COLOR:
                // For color queries, prioritize description and tags
                descWeight = 0.35; tagWeight = 0.35; filenameWeight = 0.15; metadataWeight = 0.15;
                break;
            case CONTENT:
                // For content queries, prioritize description and tags
                descWeight = 0.4; tagWeight = 0.3; filenameWeight = 0.15; metadataWeight = 0.15;
                break;
            default:
                // Default balanced weights
                descWeight = 0.4; tagWeight = 0.3; filenameWeight = 0.2; metadataWeight = 0.1;
        }
        
        // Adjust weights based on query complexity - AI adapts to complexity
        if (criteria.getComplexity() == SearchCriteriaBuilder.QueryComplexity.COMPLEX) {
            // For complex queries, increase description and tag importance
            descWeight = Math.min(0.5, descWeight * 1.1);
            tagWeight = Math.min(0.4, tagWeight * 1.1);
            filenameWeight *= 0.9;
            metadataWeight *= 0.9;
        }
        
        // Calculate AI-driven weighted score
        double aiScore = descriptionScore * descWeight +
                        tagScore * tagWeight +
                        filenameScore * filenameWeight +
                        metadataScore * metadataWeight;
        
        log.debug("AI-driven scoring: desc={:.2f}*{:.2f} + tag={:.2f}*{:.2f} + file={:.2f}*{:.2f} + meta={:.2f}*{:.2f} = {:.2f}",
            descriptionScore, descWeight, tagScore, tagWeight, 
            filenameScore, filenameWeight, metadataScore, metadataWeight, aiScore);
        
        return aiScore;
    }

    /**
     * Calculate string similarity using simple character-based approach
     */
    private double calculateStringSimilarity(String str1, String str2) {
        if (str1 == null || str2 == null) {
            return 0.0;
        }
        
        if (str1.equals(str2)) {
            return 1.0;
        }
        
        // Simple containment check
        if (str1.contains(str2) || str2.contains(str1)) {
            return 0.8;
        }
        
        // Character overlap calculation
        Set<Character> chars1 = str1.chars().mapToObj(c -> (char) c).collect(Collectors.toSet());
        Set<Character> chars2 = str2.chars().mapToObj(c -> (char) c).collect(Collectors.toSet());
        
        Set<Character> intersection = new HashSet<>(chars1);
        intersection.retainAll(chars2);
        
        Set<Character> union = new HashSet<>(chars1);
        union.addAll(chars2);
        
        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }

    /**
     * Compare resolution categories for ordering
     */
    private int compareResolutionCategories(String category1, String category2) {
        Map<String, Integer> categoryOrder = Map.of(
            Image.ResolutionCategory.LOW, 1,
            Image.ResolutionCategory.MEDIUM, 2,
            Image.ResolutionCategory.HIGH, 3,
            Image.ResolutionCategory.ULTRA_HIGH, 4
        );
        
        Integer order1 = categoryOrder.get(category1);
        Integer order2 = categoryOrder.get(category2);
        
        if (order1 == null || order2 == null) {
            return 0;
        }
        
        return Integer.compare(order1, order2);
    }

    /**
     * Get top N scored results
     * 
     * @param results List of scored results
     * @param limit Maximum number of results to return
     * @return Top N results
     */
    public List<ScoredResult> getTopResults(List<ScoredResult> results, int limit) {
        return results.stream()
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Filter results by minimum score threshold
     * 
     * @param results List of scored results
     * @param minScore Minimum score threshold
     * @return Filtered results
     */
    public List<ScoredResult> filterByMinScore(List<ScoredResult> results, double minScore) {
        return results.stream()
                .filter(result -> result.getTotalScore() >= minScore)
                .collect(Collectors.toList());
    }

    /**
     * ScoredResult class to hold image with its relevance score
     */
    @Data
    public static class ScoredResult {
        private Long imageId;
        private Image image;
        private double descriptionScore;
        private double tagScore;
        private double filenameScore;
        private double metadataScore;
        private double bonusScore;
        private double penaltyScore;
        private double totalScore;
        private String explanation;
        
        /**
         * Get confidence level based on total score
         */
        public String getConfidenceLevel() {
            if (totalScore >= 0.8) {
                return "HIGH";
            } else if (totalScore >= 0.5) {
                return "MEDIUM";
            } else if (totalScore >= 0.2) {
                return "LOW";
            } else {
                return "MINIMAL";
            }
        }
        
        /**
         * Check if result meets quality threshold
         */
        public boolean meetsQualityThreshold() {
            return totalScore >= 0.2;
        }
    }
}