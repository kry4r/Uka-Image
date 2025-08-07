package com.uka.image.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Tag Processing Utility for enhanced tag handling with proper delimiters
 * Supports comma, semicolon, and space-separated tags with normalization
 * 
 * @author Uka Team
 */
@Slf4j
@Component
public class TagProcessor {

    // Regex pattern for splitting tags by various delimiters
    private static final Pattern TAG_DELIMITER_PATTERN = Pattern.compile("[,;\\s]+");
    
    // Pattern for cleaning individual tags
    private static final Pattern TAG_CLEANUP_PATTERN = Pattern.compile("[^\\w\\u4e00-\\u9fff\\-_]");
    
    // Maximum tag length
    private static final int MAX_TAG_LENGTH = 50;
    
    // Maximum number of tags per image
    private static final int MAX_TAGS_COUNT = 20;

    /**
     * Process tags with delimiters for AI analysis
     * Returns formatted string suitable for AI processing
     * 
     * @param rawTags Raw tags string with various delimiters
     * @return Formatted tags string for AI
     */
    public String processTagsWithDelimiters(String rawTags) {
        List<String> processedTags = processTags(rawTags);
        return processedTags.isEmpty() ? "No tags" : String.join(", ", processedTags);
    }

    /**
     * Process raw tags string into clean, normalized tag list
     * 
     * @param rawTags Raw tags string with various delimiters
     * @return List of cleaned and normalized tags
     */
    public List<String> processTags(String rawTags) {
        if (rawTags == null || rawTags.trim().isEmpty()) {
            return new ArrayList<>();
        }

        log.debug("Processing raw tags: {}", rawTags);

        // Split tags by delimiters and clean them
        List<String> processedTags = Arrays.stream(TAG_DELIMITER_PATTERN.split(rawTags))
                .map(this::cleanTag)
                .filter(tag -> !tag.isEmpty())
                .distinct()
                .limit(MAX_TAGS_COUNT)
                .collect(Collectors.toList());

        log.debug("Processed {} tags from raw input", processedTags.size());
        return processedTags;
    }

    /**
     * Format tags list into a standardized string with proper delimiters
     * 
     * @param tags List of tags
     * @param delimiter Delimiter to use (default: comma)
     * @return Formatted tags string
     */
    public String formatTags(List<String> tags, String delimiter) {
        if (tags == null || tags.isEmpty()) {
            return "";
        }

        String actualDelimiter = delimiter != null ? delimiter : ", ";
        return String.join(actualDelimiter, tags);
    }

    /**
     * Format tags list with default comma delimiter
     * 
     * @param tags List of tags
     * @return Formatted tags string with comma delimiter
     */
    public String formatTags(List<String> tags) {
        return formatTags(tags, ", ");
    }

    /**
     * Extract and process tags from raw string for AI analysis
     * Returns tags formatted for optimal AI processing
     * 
     * @param rawTags Raw tags string
     * @return AI-optimized tags string
     */
    public String processTagsForAI(String rawTags) {
        List<String> processedTags = processTags(rawTags);
        
        if (processedTags.isEmpty()) {
            return "No tags";
        }

        // Format for AI with clear delimiters and context
        return processedTags.stream()
                .map(tag -> tag.toLowerCase())
                .collect(Collectors.joining(", "));
    }

    /**
     * Analyze tag relevance for search queries
     * 
     * @param tags List of image tags
     * @param searchQuery User search query
     * @return Relevance score (0.0 to 1.0)
     */
    public double calculateTagRelevance(List<String> tags, String searchQuery) {
        if (tags == null || tags.isEmpty() || searchQuery == null || searchQuery.trim().isEmpty()) {
            return 0.0;
        }

        String normalizedQuery = searchQuery.toLowerCase().trim();
        List<String> queryTerms = Arrays.asList(TAG_DELIMITER_PATTERN.split(normalizedQuery));
        
        double totalRelevance = 0.0;
        int matchCount = 0;

        for (String tag : tags) {
            String normalizedTag = tag.toLowerCase();
            
            // Exact match gets highest score
            if (queryTerms.contains(normalizedTag)) {
                totalRelevance += 1.0;
                matchCount++;
                continue;
            }

            // Partial match gets medium score
            for (String queryTerm : queryTerms) {
                if (normalizedTag.contains(queryTerm) || queryTerm.contains(normalizedTag)) {
                    totalRelevance += 0.6;
                    matchCount++;
                    break;
                }
            }

            // Semantic similarity (basic implementation)
            for (String queryTerm : queryTerms) {
                if (calculateStringSimilarity(normalizedTag, queryTerm) > 0.7) {
                    totalRelevance += 0.4;
                    matchCount++;
                    break;
                }
            }
        }

        // Normalize score based on tag count and query complexity
        double normalizedScore = matchCount > 0 ? totalRelevance / Math.max(tags.size(), queryTerms.size()) : 0.0;
        return Math.min(normalizedScore, 1.0);
    }

    /**
     * Extract semantic keywords from tags for enhanced matching
     * 
     * @param tags List of tags
     * @return Set of semantic keywords
     */
    public Set<String> extractSemanticKeywords(List<String> tags) {
        Set<String> keywords = new HashSet<>();
        
        for (String tag : tags) {
            // Add original tag
            keywords.add(tag.toLowerCase());
            
            // Add variations and related terms
            keywords.addAll(generateTagVariations(tag));
        }
        
        return keywords;
    }

    /**
     * Validate tags format and content
     * 
     * @param rawTags Raw tags string
     * @return Validation result with issues
     */
    public TagValidationResult validateTags(String rawTags) {
        TagValidationResult result = new TagValidationResult();
        
        if (rawTags == null || rawTags.trim().isEmpty()) {
            result.setValid(true);
            result.addMessage("No tags provided");
            return result;
        }

        List<String> processedTags = processTags(rawTags);
        
        // Check tag count
        if (processedTags.size() > MAX_TAGS_COUNT) {
            result.setValid(false);
            result.addMessage("Too many tags. Maximum allowed: " + MAX_TAGS_COUNT);
        }

        // Check individual tag lengths
        for (String tag : processedTags) {
            if (tag.length() > MAX_TAG_LENGTH) {
                result.setValid(false);
                result.addMessage("Tag too long: '" + tag + "'. Maximum length: " + MAX_TAG_LENGTH);
            }
        }

        if (result.isValid()) {
            result.addMessage("Tags validation passed");
        }

        result.setProcessedTags(processedTags);
        return result;
    }

    /**
     * Generate search suggestions based on existing tags
     * 
     * @param existingTags Collection of existing tags from database
     * @param query Partial query for suggestions
     * @param limit Maximum number of suggestions
     * @return List of tag suggestions
     */
    public List<String> generateTagSuggestions(Collection<String> existingTags, String query, int limit) {
        if (query == null || query.trim().isEmpty()) {
            return existingTags.stream()
                    .limit(limit)
                    .collect(Collectors.toList());
        }

        String normalizedQuery = query.toLowerCase().trim();
        
        return existingTags.stream()
                .filter(tag -> tag.toLowerCase().contains(normalizedQuery))
                .sorted((a, b) -> {
                    // Prioritize exact matches and shorter tags
                    boolean aExact = a.toLowerCase().equals(normalizedQuery);
                    boolean bExact = b.toLowerCase().equals(normalizedQuery);
                    
                    if (aExact && !bExact) return -1;
                    if (!aExact && bExact) return 1;
                    
                    return Integer.compare(a.length(), b.length());
                })
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Clean individual tag by removing invalid characters and normalizing
     * 
     * @param tag Raw tag string
     * @return Cleaned tag string
     */
    private String cleanTag(String tag) {
        if (tag == null) {
            return "";
        }

        // Remove leading/trailing whitespace
        String cleaned = tag.trim();
        
        // Remove invalid characters but keep Chinese characters, letters, numbers, hyphens, underscores
        cleaned = TAG_CLEANUP_PATTERN.matcher(cleaned).replaceAll("");
        
        // Limit length
        if (cleaned.length() > MAX_TAG_LENGTH) {
            cleaned = cleaned.substring(0, MAX_TAG_LENGTH);
        }

        return cleaned;
    }

    /**
     * Calculate string similarity using Levenshtein distance
     * 
     * @param str1 First string
     * @param str2 Second string
     * @return Similarity score (0.0 to 1.0)
     */
    private double calculateStringSimilarity(String str1, String str2) {
        if (str1 == null || str2 == null) {
            return 0.0;
        }

        int maxLength = Math.max(str1.length(), str2.length());
        if (maxLength == 0) {
            return 1.0;
        }

        int distance = calculateLevenshteinDistance(str1, str2);
        return 1.0 - (double) distance / maxLength;
    }

    /**
     * Calculate Levenshtein distance between two strings
     */
    private int calculateLevenshteinDistance(String str1, String str2) {
        int[][] dp = new int[str1.length() + 1][str2.length() + 1];

        for (int i = 0; i <= str1.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= str2.length(); j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= str1.length(); i++) {
            for (int j = 1; j <= str2.length(); j++) {
                int cost = str1.charAt(i - 1) == str2.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1), dp[i - 1][j - 1] + cost);
            }
        }

        return dp[str1.length()][str2.length()];
    }

    /**
     * Generate variations of a tag for enhanced matching
     */
    private Set<String> generateTagVariations(String tag) {
        Set<String> variations = new HashSet<>();
        String lowerTag = tag.toLowerCase();
        
        // Add plural/singular variations (basic implementation)
        if (lowerTag.endsWith("s") && lowerTag.length() > 2) {
            variations.add(lowerTag.substring(0, lowerTag.length() - 1));
        } else {
            variations.add(lowerTag + "s");
        }
        
        // Add common variations
        variations.add(lowerTag.replace("-", ""));
        variations.add(lowerTag.replace("_", ""));
        variations.add(lowerTag.replace(" ", ""));
        
        return variations;
    }

    /**
     * Tag validation result class
     */
    public static class TagValidationResult {
        private boolean valid = true;
        private List<String> messages = new ArrayList<>();
        private List<String> processedTags = new ArrayList<>();

        public boolean isValid() { return valid; }
        public void setValid(boolean valid) { this.valid = valid; }
        
        public List<String> getMessages() { return messages; }
        public void addMessage(String message) { this.messages.add(message); }
        
        public List<String> getProcessedTags() { return processedTags; }
        public void setProcessedTags(List<String> processedTags) { this.processedTags = processedTags; }
    }
}