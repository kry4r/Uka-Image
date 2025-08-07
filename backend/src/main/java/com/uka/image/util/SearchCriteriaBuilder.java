package com.uka.image.util;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Search Criteria Builder for constructing comprehensive search parameters
 * Analyzes user queries and builds multi-dimensional search criteria
 * 
 * @author Uka Team
 */
@Slf4j
@Component
public class SearchCriteriaBuilder {

    // Color keywords mapping
    private static final Map<String, Set<String>> COLOR_KEYWORDS = new HashMap<>();
    
    // Technical keywords mapping
    private static final Set<String> TECHNICAL_KEYWORDS = new HashSet<>();
    
    // Visual keywords mapping
    private static final Set<String> VISUAL_KEYWORDS = new HashSet<>();
    
    static {
        // Initialize color keywords
        COLOR_KEYWORDS.put("red", Set.of("red", "crimson", "scarlet", "burgundy"));
        COLOR_KEYWORDS.put("blue", Set.of("blue", "navy", "azure", "cyan"));
        COLOR_KEYWORDS.put("green", Set.of("green", "emerald", "lime", "forest"));
        COLOR_KEYWORDS.put("yellow", Set.of("yellow", "gold", "amber", "lemon"));
        COLOR_KEYWORDS.put("purple", Set.of("purple", "violet", "magenta", "lavender"));
        COLOR_KEYWORDS.put("orange", Set.of("orange", "coral", "peach", "tangerine"));
        COLOR_KEYWORDS.put("black", Set.of("black", "dark", "ebony", "charcoal"));
        COLOR_KEYWORDS.put("white", Set.of("white", "ivory", "pearl", "snow"));
        
        // Initialize technical keywords
        TECHNICAL_KEYWORDS.addAll(Arrays.asList(
            "jpg", "jpeg", "png", "gif", "webp", "bmp", "tiff",
            "high", "low", "resolution", "quality", "size", "format",
            "animated", "transparent", "compression"
        ));
        
        // Initialize visual keywords
        VISUAL_KEYWORDS.addAll(Arrays.asList(
            "landscape", "portrait", "square", "panoramic",
            "bright", "dark", "colorful", "monochrome", "vibrant",
            "contrast", "saturation", "exposure"
        ));
    }

    /**
     * Build comprehensive search criteria from user query
     * 
     * @param query User search query
     * @return SearchCriteria object with analyzed parameters
     */
    public SearchCriteria buildCriteria(String query) {
        log.debug("Building search criteria for query: {}", query);
        
        SearchCriteria criteria = new SearchCriteria();
        criteria.setOriginalQuery(query);
        criteria.setNormalizedQuery(normalizeQuery(query));
        
        // Analyze query type and complexity
        analyzeQueryType(criteria);
        analyzeQueryComplexity(criteria);
        
        // Extract search terms and keywords
        extractSearchTerms(criteria);
        
        // Build filters based on query analysis
        buildTechnicalFilters(criteria);
        buildVisualFilters(criteria);
        buildContentFilters(criteria);
        
        // Configure AI search parameters (removed manual weights)
        configureAISearchParameters(criteria);
        
        log.debug("Built search criteria: type={}, complexity={}, terms={}", 
            criteria.getPrimaryType(), criteria.getComplexity(), criteria.getSearchTerms().size());
        
        return criteria;
    }

    /**
     * Normalize query string for consistent processing
     */
    private String normalizeQuery(String query) {
        if (query == null) return "";
        return query.trim().toLowerCase().replaceAll("\\s+", " ");
    }

    /**
     * Analyze query type based on content
     */
    private void analyzeQueryType(SearchCriteria criteria) {
        String query = criteria.getNormalizedQuery();
        
        // Check for color-based queries
        if (containsColorKeywords(query)) {
            criteria.setPrimaryType(SearchType.COLOR);
        }
        // Check for technical queries
        else if (containsTechnicalKeywords(query)) {
            criteria.setPrimaryType(SearchType.TECHNICAL);
        }
        // Check for visual queries
        else if (containsVisualKeywords(query)) {
            criteria.setPrimaryType(SearchType.VISUAL);
        }
        // Check for content-based queries
        else if (containsContentKeywords(query)) {
            criteria.setPrimaryType(SearchType.CONTENT);
        }
        // Default to semantic search
        else {
            criteria.setPrimaryType(SearchType.SEMANTIC);
        }
    }

    /**
     * Analyze query complexity
     */
    private void analyzeQueryComplexity(SearchCriteria criteria) {
        String query = criteria.getNormalizedQuery();
        String[] terms = query.split("\\s+");
        
        if (terms.length <= 2) {
            criteria.setComplexity(QueryComplexity.SIMPLE);
        } else if (terms.length <= 5) {
            criteria.setComplexity(QueryComplexity.MEDIUM);
        } else {
            criteria.setComplexity(QueryComplexity.COMPLEX);
        }
        
        // Check for advanced query features
        if (query.contains("not ") || query.contains("-")) {
            criteria.setHasNegation(true);
        }
        if (query.contains("larger than") || query.contains("smaller than") || 
            query.contains(">") || query.contains("<")) {
            criteria.setHasComparison(true);
        }
        if (query.contains("recent") || query.contains("old") || query.contains("new")) {
            criteria.setHasTimeReference(true);
        }
    }

    /**
     * Extract search terms and keywords
     */
    private void extractSearchTerms(SearchCriteria criteria) {
        String query = criteria.getNormalizedQuery();
        
        // Split into individual terms
        List<String> terms = Arrays.asList(query.split("\\s+"));
        criteria.setSearchTerms(terms);
        
        // Extract keywords (filter out common words)
        Set<String> stopWords = Set.of("the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for", "of", "with", "by");
        List<String> keywords = terms.stream()
            .filter(term -> !stopWords.contains(term) && term.length() > 2)
            .distinct()
            .toList();
        criteria.setKeywords(keywords);
        
        // Extract phrases (quoted strings)
        List<String> phrases = extractQuotedPhrases(criteria.getOriginalQuery());
        criteria.setPhrases(phrases);
    }

    /**
     * Extract quoted phrases from query
     */
    private List<String> extractQuotedPhrases(String query) {
        List<String> phrases = new ArrayList<>();
        Pattern pattern = Pattern.compile("\"([^\"]+)\"");
        var matcher = pattern.matcher(query);
        while (matcher.find()) {
            phrases.add(matcher.group(1));
        }
        return phrases;
    }

    /**
     * Build technical filters based on query analysis
     */
    private void buildTechnicalFilters(SearchCriteria criteria) {
        TechnicalFilters filters = criteria.getTechnicalFilters();
        String query = criteria.getNormalizedQuery();
        
        // File format detection
        if (query.contains("jpg") || query.contains("jpeg")) {
            filters.getFileFormats().add("JPEG");
        }
        if (query.contains("png")) {
            filters.getFileFormats().add("PNG");
        }
        if (query.contains("gif")) {
            filters.getFileFormats().add("GIF");
        }
        if (query.contains("webp")) {
            filters.getFileFormats().add("WEBP");
        }
        
        // Resolution detection
        if (query.contains("high resolution") || query.contains("hd") || query.contains("high quality")) {
            filters.setMinResolutionCategory("HIGH");
        }
        if (query.contains("low resolution") || query.contains("small")) {
            filters.setMaxResolutionCategory("LOW");
        }
        
        // File size detection
        if (query.contains("large file") || query.contains("big")) {
            filters.setMinFileSize(1024L * 1024L); // 1MB
        }
        if (query.contains("small file") || query.contains("tiny")) {
            filters.setMaxFileSize(100L * 1024L); // 100KB
        }
        
        // Special properties
        if (query.contains("transparent") || query.contains("transparency")) {
            filters.setHasTransparency(true);
        }
        if (query.contains("animated") || query.contains("animation")) {
            filters.setIsAnimated(true);
        }
        
        criteria.setTechnicalFilters(filters);
    }

    /**
     * Build visual filters based on query analysis
     */
    private void buildVisualFilters(SearchCriteria criteria) {
        VisualFilters filters = criteria.getVisualFilters();
        String query = criteria.getNormalizedQuery();
        
        // Color analysis
        Set<String> colorKeywords = new HashSet<>();
        for (Map.Entry<String, Set<String>> entry : COLOR_KEYWORDS.entrySet()) {
            for (String colorTerm : entry.getValue()) {
                if (query.contains(colorTerm)) {
                    colorKeywords.add(entry.getKey());
                    break;
                }
            }
        }
        filters.setColorKeywords(colorKeywords);
        
        // Brightness detection
        if (query.contains("bright") || query.contains("light")) {
            filters.setMinBrightness(0.7);
        }
        if (query.contains("dark") || query.contains("dim")) {
            filters.setMaxBrightness(0.3);
        }
        
        // Saturation detection
        if (query.contains("colorful") || query.contains("vibrant")) {
            filters.setMinSaturation(0.6);
        }
        if (query.contains("muted") || query.contains("desaturated")) {
            filters.setMaxSaturation(0.4);
        }
        
        // Contrast detection
        if (query.contains("high contrast")) {
            filters.setMinContrast(0.7);
        }
        if (query.contains("low contrast") || query.contains("soft")) {
            filters.setMaxContrast(0.3);
        }
        
        // Orientation detection
        if (query.contains("landscape") || query.contains("wide")) {
            filters.setOrientation("LANDSCAPE");
        } else if (query.contains("portrait") || query.contains("tall")) {
            filters.setOrientation("PORTRAIT");
        } else if (query.contains("square")) {
            filters.setOrientation("SQUARE");
        } else if (query.contains("panoramic") || query.contains("panorama")) {
            filters.setOrientation("PANORAMIC");
        }
        
        criteria.setVisualFilters(filters);
    }

    /**
     * Build content filters based on query analysis
     */
    private void buildContentFilters(SearchCriteria criteria) {
        ContentFilters filters = new ContentFilters();
        String query = criteria.getNormalizedQuery();
        
        // Content category detection
        if (query.contains("nature") || query.contains("landscape") || query.contains("outdoor")) {
            filters.getContentCategories().add("NATURE");
        }
        if (query.contains("people") || query.contains("person") || query.contains("portrait")) {
            filters.getContentCategories().add("PEOPLE");
        }
        if (query.contains("architecture") || query.contains("building") || query.contains("structure")) {
            filters.getContentCategories().add("ARCHITECTURE");
        }
        if (query.contains("art") || query.contains("artistic") || query.contains("creative")) {
            filters.getContentCategories().add("ART");
        }
        if (query.contains("technology") || query.contains("tech") || query.contains("digital")) {
            filters.getContentCategories().add("TECHNOLOGY");
        }
        
        // Complexity detection
        if (query.contains("simple") || query.contains("minimal")) {
            filters.setMinComplexity(0.0);
            filters.setMaxComplexity(0.3);
        }
        if (query.contains("complex") || query.contains("detailed")) {
            filters.setMinComplexity(0.7);
            filters.setMaxComplexity(1.0);
        }
        
        // Content features
        if (query.contains("text") || query.contains("writing") || query.contains("words")) {
            filters.setHasText(true);
        }
        if (query.contains("face") || query.contains("faces") || query.contains("people")) {
            filters.setHasFaces(true);
        }
        if (query.contains("object") || query.contains("objects") || query.contains("things")) {
            filters.setHasObjects(true);
        }
        
        criteria.setContentFilters(filters);
    }

    /**
     * Configure AI-driven search parameters based on query type and complexity
     * Removed manual weights - AI now handles all relevance determination
     */
    private void configureAISearchParameters(SearchCriteria criteria) {
        // Store search type and complexity for AI to use in relevance determination
        // AI will now handle all weight calculations dynamically based on content analysis
        log.debug("Configured AI search parameters for {} query with {} complexity", 
            criteria.getPrimaryType(), criteria.getComplexity());
    }

    // Helper methods for keyword detection
    private boolean containsColorKeywords(String query) {
        return COLOR_KEYWORDS.values().stream()
            .flatMap(Set::stream)
            .anyMatch(query::contains);
    }

    private boolean containsTechnicalKeywords(String query) {
        return TECHNICAL_KEYWORDS.stream().anyMatch(query::contains);
    }

    private boolean containsVisualKeywords(String query) {
        return VISUAL_KEYWORDS.stream().anyMatch(query::contains);
    }

    private boolean containsContentKeywords(String query) {
        return query.contains("nature") || query.contains("people") || 
               query.contains("architecture") || query.contains("art") || 
               query.contains("technology");
    }

    // Enum definitions
    public enum SearchType {
        SEMANTIC, TECHNICAL, VISUAL, COLOR, CONTENT
    }

    public enum QueryComplexity {
        SIMPLE, MEDIUM, COMPLEX
    }

    // Data classes
    @Data
    public static class SearchCriteria {
        private String originalQuery;
        private String normalizedQuery;
        private SearchType primaryType = SearchType.SEMANTIC;
        private QueryComplexity complexity = QueryComplexity.SIMPLE;
        private List<String> searchTerms = new ArrayList<>();
        private List<String> keywords = new ArrayList<>();
        private List<String> phrases = new ArrayList<>();
        private boolean hasNegation = false;
        private boolean hasComparison = false;
        private boolean hasTimeReference = false;
        private TechnicalFilters technicalFilters = new TechnicalFilters();
        private VisualFilters visualFilters = new VisualFilters();
        private ContentFilters contentFilters = new ContentFilters();
    }

    @Data
    public static class TechnicalFilters {
        private Set<String> fileFormats = new HashSet<>();
        private String minResolutionCategory;
        private String maxResolutionCategory;
        private Long minFileSize;
        private Long maxFileSize;
        private Boolean hasTransparency;
        private Boolean isAnimated;
    }

    @Data
    public static class VisualFilters {
        private String orientation; // LANDSCAPE, PORTRAIT, SQUARE
        private String resolutionCategory; // LOW, MEDIUM, HIGH, ULTRA_HIGH
        private Integer minWidth;
        private Integer maxWidth;
        private Integer minHeight;
        private Integer maxHeight;
        private Double aspectRatio;
        private Double aspectRatioTolerance = 0.1;
        private Set<String> dominantColors = new HashSet<>();
        private Set<String> colorKeywords = new HashSet<>();
        private Double minBrightness;
        private Double maxBrightness;
        private Double minSaturation;
        private Double maxSaturation;
        private Double minContrast;
        private Double maxContrast;
    }

    @Data
    public static class ContentFilters {
        private Set<String> contentCategories = new HashSet<>();
        private Double minComplexity;
        private Double maxComplexity;
        private Double minVisualComplexity;
        private Double maxVisualComplexity;
        private boolean hasText = false;
        private boolean hasFaces = false;
        private boolean hasObjects = false;
    }

}
