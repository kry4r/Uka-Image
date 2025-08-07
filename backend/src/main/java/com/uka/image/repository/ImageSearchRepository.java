package com.uka.image.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.uka.image.entity.Image;
import com.uka.image.mapper.ImageMapper;
import com.uka.image.util.SearchCriteriaBuilder.SearchCriteria;
import com.uka.image.util.SearchCriteriaBuilder.TechnicalFilters;
import com.uka.image.util.SearchCriteriaBuilder.VisualFilters;
import com.uka.image.util.SearchCriteriaBuilder.ContentFilters;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ImageSearchRepository for optimized metadata-based searching
 * Provides specialized queries for multi-dimensional image search
 * 
 * @author Uka Team
 */
@Slf4j
@Repository
public class ImageSearchRepository {

    @Autowired
    private ImageMapper imageMapper;

    /**
     * Find images by comprehensive search criteria with optimized queries
     * 
     * @param criteria Search criteria with filters and weights
     * @param pageNum Page number
     * @param pageSize Page size
     * @return Page of images matching criteria
     */
    public IPage<Image> findByCriteria(SearchCriteria criteria, int pageNum, int pageSize) {
        log.info("Searching images with criteria type: {}", criteria.getPrimaryType());
        
        // Create base query wrapper
        QueryWrapper<Image> queryWrapper = createBaseQueryWrapper(criteria);
        
        // Apply technical filters
        applyTechnicalFilters(queryWrapper, criteria.getTechnicalFilters());
        
        // Apply visual filters
        applyVisualFilters(queryWrapper, criteria.getVisualFilters());
        
        // Apply content filters
        applyContentFilters(queryWrapper, criteria.getContentFilters());
        
        // Create pagination
        Page<Image> page = new Page<>(pageNum, pageSize);
        
        // Execute query
        return imageMapper.selectPage(page, queryWrapper);
    }
    
    /**
     * Find all active images matching criteria without pagination
     * 
     * @param criteria Search criteria with filters and weights
     * @param limit Maximum number of images to return (for performance)
     * @return List of images matching criteria
     */
    public List<Image> findAllByCriteria(SearchCriteria criteria, int limit) {
        log.info("Finding all images with criteria type: {}, limit: {}", criteria.getPrimaryType(), limit);
        
        // Create base query wrapper
        QueryWrapper<Image> queryWrapper = createBaseQueryWrapper(criteria);
        
        // Apply technical filters
        applyTechnicalFilters(queryWrapper, criteria.getTechnicalFilters());
        
        // Apply visual filters
        applyVisualFilters(queryWrapper, criteria.getVisualFilters());
        
        // Apply content filters
        applyContentFilters(queryWrapper, criteria.getContentFilters());
        
        // Apply limit
        queryWrapper.last("LIMIT " + limit);
        
        // Execute query
        return imageMapper.selectList(queryWrapper);
    }
    
    /**
     * Find images by keyword search with optimized query
     * 
     * @param keyword Search keyword
     * @param pageNum Page number
     * @param pageSize Page size
     * @return Page of images matching keyword
     */
    public IPage<Image> findByKeyword(String keyword, int pageNum, int pageSize) {
        log.info("Searching images by keyword: {}", keyword);
        
        // Create query wrapper
        QueryWrapper<Image> queryWrapper = new QueryWrapper<>();
        
        // Add keyword search conditions
        queryWrapper.eq("status", Image.Status.ACTIVE)
                .and(wrapper -> wrapper
                        .like("file_name", keyword)
                        .or()
                        .like("original_name", keyword)
                        .or()
                        .like("description", keyword)
                        .or()
                        .like("tags", keyword)
                        .or()
                        .like("ai_generated_tags", keyword)
                        .or()
                        .like("semantic_keywords", keyword)
                );
        
        // Create pagination
        Page<Image> page = new Page<>(pageNum, pageSize);
        
        // Execute query
        return imageMapper.selectPage(page, queryWrapper);
    }
    
    /**
     * Find images by technical specifications
     * 
     * @param fileFormat File format (e.g., JPEG, PNG)
     * @param minWidth Minimum width
     * @param minHeight Minimum height
     * @param hasTransparency Whether image has transparency
     * @param pageNum Page number
     * @param pageSize Page size
     * @return Page of images matching technical specifications
     */
    public IPage<Image> findByTechnicalSpecs(
            String fileFormat, 
            Integer minWidth, 
            Integer minHeight, 
            Boolean hasTransparency,
            int pageNum, 
            int pageSize) {
        
        log.info("Searching images by technical specs: format={}, minWidth={}, minHeight={}, hasTransparency={}", 
                fileFormat, minWidth, minHeight, hasTransparency);
        
        // Create query wrapper
        QueryWrapper<Image> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", Image.Status.ACTIVE);
        
        // Apply technical filters
        if (fileFormat != null && !fileFormat.trim().isEmpty()) {
            queryWrapper.eq("file_format", fileFormat.trim().toUpperCase());
        }
        
        if (minWidth != null && minWidth > 0) {
            queryWrapper.ge("width", minWidth);
        }
        
        if (minHeight != null && minHeight > 0) {
            queryWrapper.ge("height", minHeight);
        }
        
        if (hasTransparency != null) {
            queryWrapper.eq("has_transparency", hasTransparency);
        }
        
        // Create pagination
        Page<Image> page = new Page<>(pageNum, pageSize);
        
        // Execute query
        return imageMapper.selectPage(page, queryWrapper);
    }
    
    /**
     * Find images by visual characteristics
     * 
     * @param orientation Image orientation (LANDSCAPE, PORTRAIT, SQUARE, PANORAMIC)
     * @param dominantColors Comma-separated list of dominant colors
     * @param minBrightness Minimum brightness level (0-100)
     * @param maxBrightness Maximum brightness level (0-100)
     * @param pageNum Page number
     * @param pageSize Page size
     * @return Page of images matching visual characteristics
     */
    public IPage<Image> findByVisualCharacteristics(
            String orientation,
            String dominantColors,
            Integer minBrightness,
            Integer maxBrightness,
            int pageNum,
            int pageSize) {
        
        log.info("Searching images by visual characteristics: orientation={}, dominantColors={}, brightness={}-{}", 
                orientation, dominantColors, minBrightness, maxBrightness);
        
        // Create query wrapper
        QueryWrapper<Image> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", Image.Status.ACTIVE);
        
        // Apply visual filters
        if (orientation != null && !orientation.trim().isEmpty()) {
            queryWrapper.eq("orientation", orientation.trim().toUpperCase());
        }
        
        if (dominantColors != null && !dominantColors.trim().isEmpty()) {
            String[] colors = dominantColors.split(",");
            queryWrapper.and(wrapper -> {
                for (String color : colors) {
                    wrapper.or().like("dominant_colors", "%" + color.trim() + "%");
                }
            });
        }
        
        if (minBrightness != null) {
            queryWrapper.ge("brightness_level", minBrightness);
        }
        
        if (maxBrightness != null) {
            queryWrapper.le("brightness_level", maxBrightness);
        }
        
        // Create pagination
        Page<Image> page = new Page<>(pageNum, pageSize);
        
        // Execute query
        return imageMapper.selectPage(page, queryWrapper);
    }
    
    /**
     * Find images by content category
     * 
     * @param contentCategory Content category (PHOTOGRAPHY, ARTWORK, SCREENSHOT, etc.)
     * @param pageNum Page number
     * @param pageSize Page size
     * @return Page of images matching content category
     */
    public IPage<Image> findByContentCategory(String contentCategory, int pageNum, int pageSize) {
        log.info("Searching images by content category: {}", contentCategory);
        
        // Create query wrapper
        QueryWrapper<Image> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", Image.Status.ACTIVE)
                .eq("content_category", contentCategory.trim().toUpperCase());
        
        // Create pagination
        Page<Image> page = new Page<>(pageNum, pageSize);
        
        // Execute query
        return imageMapper.selectPage(page, queryWrapper);
    }
    
    /**
     * Find images by multiple IDs
     * 
     * @param imageIds List of image IDs
     * @return List of images with the specified IDs
     */
    public List<Image> findByIds(List<Long> imageIds) {
        if (imageIds == null || imageIds.isEmpty()) {
            return new ArrayList<>();
        }
        
        log.info("Finding images by IDs: {}", imageIds);
        
        // Create query wrapper
        LambdaQueryWrapper<Image> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(Image::getId, imageIds)
                .eq(Image::getStatus, Image.Status.ACTIVE);
        
        // Execute query
        return imageMapper.selectList(queryWrapper);
    }
    
    /**
     * Find recent images with optimized query
     * 
     * @param limit Maximum number of images to return
     * @return List of recent images
     */
    public List<Image> findRecentImages(int limit) {
        log.info("Finding {} recent images", limit);
        
        // Create query wrapper
        QueryWrapper<Image> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", Image.Status.ACTIVE)
                .orderByDesc("created_at")
                .last("LIMIT " + limit);
        
        // Execute query
        return imageMapper.selectList(queryWrapper);
    }
    
    /**
     * Find popular images based on view count
     * 
     * @param limit Maximum number of images to return
     * @return List of popular images
     */
    public List<Image> findPopularImages(int limit) {
        log.info("Finding {} popular images", limit);
        
        // Create query wrapper
        QueryWrapper<Image> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", Image.Status.ACTIVE)
                .orderByDesc("view_count")
                .last("LIMIT " + limit);
        
        // Execute query
        return imageMapper.selectList(queryWrapper);
    }
    
    /**
     * Create base query wrapper with common conditions
     * 
     * @param criteria Search criteria
     * @return QueryWrapper with base conditions
     */
    private QueryWrapper<Image> createBaseQueryWrapper(SearchCriteria criteria) {
        QueryWrapper<Image> queryWrapper = new QueryWrapper<>();
        
        // Only active images
        queryWrapper.eq("status", Image.Status.ACTIVE);
        
        // Add keyword search if available
        List<String> keywords = criteria.getKeywords();
        if (!keywords.isEmpty()) {
            queryWrapper.and(wrapper -> {
                for (String keyword : keywords) {
                    wrapper.or().like("file_name", "%" + keyword + "%")
                            .or().like("original_name", "%" + keyword + "%")
                            .or().like("description", "%" + keyword + "%")
                            .or().like("tags", "%" + keyword + "%")
                            .or().like("ai_generated_tags", "%" + keyword + "%")
                            .or().like("semantic_keywords", "%" + keyword + "%");
                }
            });
        }
        
        // Add phrase search if available
        List<String> phrases = criteria.getPhrases();
        if (!phrases.isEmpty()) {
            queryWrapper.and(wrapper -> {
                for (String phrase : phrases) {
                    wrapper.or().like("description", "%" + phrase + "%")
                            .or().like("tags", "%" + phrase + "%")
                            .or().like("ai_generated_tags", "%" + phrase + "%")
                            .or().like("semantic_keywords", "%" + phrase + "%");
                }
            });
        }
        
        return queryWrapper;
    }
    
    /**
     * Apply technical filters to query wrapper
     * 
     * @param queryWrapper Query wrapper
     * @param filters Technical filters
     */
    private void applyTechnicalFilters(QueryWrapper<Image> queryWrapper, TechnicalFilters filters) {
        // File format filter
        if (!filters.getFileFormats().isEmpty()) {
            queryWrapper.and(wrapper -> {
                for (String format : filters.getFileFormats()) {
                    wrapper.or().eq("file_format", format.toUpperCase());
                }
            });
        }
        
        // Resolution category filter
        if (filters.getMinResolutionCategory() != null) {
            List<String> validCategories = getResolutionCategoriesAbove(filters.getMinResolutionCategory());
            if (!validCategories.isEmpty()) {
                queryWrapper.and(wrapper -> {
                    for (String category : validCategories) {
                        wrapper.or().eq("resolution_category", category);
                    }
                });
            }
        }
        
        // File size filter
        if (filters.getMinFileSize() != null) {
            queryWrapper.ge("file_size", filters.getMinFileSize());
        }
        
        if (filters.getMaxFileSize() != null) {
            queryWrapper.le("file_size", filters.getMaxFileSize());
        }
        
        // Transparency filter
        if (filters.getHasTransparency() != null) {
            queryWrapper.eq("has_transparency", filters.getHasTransparency());
        }
        
        // Animation filter
        if (filters.getIsAnimated() != null) {
            queryWrapper.eq("is_animated", filters.getIsAnimated());
        }
    }
    
    /**
     * Apply visual filters to query wrapper
     * 
     * @param queryWrapper Query wrapper
     * @param filters Visual filters
     */
    private void applyVisualFilters(QueryWrapper<Image> queryWrapper, VisualFilters filters) {
        // Orientation filter
        if (filters.getOrientation() != null) {
            queryWrapper.eq("orientation", filters.getOrientation());
        }
        
        // Brightness filter
        if (filters.getMinBrightness() != null) {
            queryWrapper.ge("brightness_level", filters.getMinBrightness());
        }
        
        if (filters.getMaxBrightness() != null) {
            queryWrapper.le("brightness_level", filters.getMaxBrightness());
        }
        
        // Contrast filter
        if (filters.getMinContrast() != null) {
            queryWrapper.ge("contrast_level", filters.getMinContrast());
        }
        
        if (filters.getMaxContrast() != null) {
            queryWrapper.le("contrast_level", filters.getMaxContrast());
        }
        
        // Saturation filter
        if (filters.getMinSaturation() != null) {
            queryWrapper.ge("saturation_level", filters.getMinSaturation());
        }
        
        if (filters.getMaxSaturation() != null) {
            queryWrapper.le("saturation_level", filters.getMaxSaturation());
        }
        
        // Dominant colors filter
        if (!filters.getDominantColors().isEmpty()) {
            queryWrapper.and(wrapper -> {
                for (String color : filters.getDominantColors()) {
                    wrapper.or().like("dominant_colors", "%" + color + "%");
                }
            });
        }
    }
    
    /**
     * Apply content filters to query wrapper
     * 
     * @param queryWrapper Query wrapper
     * @param filters Content filters
     */
    private void applyContentFilters(QueryWrapper<Image> queryWrapper, ContentFilters filters) {
        // Content category filter
        if (!filters.getContentCategories().isEmpty()) {
            queryWrapper.and(wrapper -> {
                for (String category : filters.getContentCategories()) {
                    wrapper.or().eq("content_category", category);
                }
            });
        }
        
        // Visual complexity filter
        if (filters.getMinVisualComplexity() != null) {
            queryWrapper.ge("visual_complexity_score", filters.getMinVisualComplexity());
        }
        
        if (filters.getMaxVisualComplexity() != null) {
            queryWrapper.le("visual_complexity_score", filters.getMaxVisualComplexity());
        }
    }
    
    /**
     * Get resolution categories above the specified category
     * 
     * @param minCategory Minimum resolution category
     * @return List of valid resolution categories
     */
    private List<String> getResolutionCategoriesAbove(String minCategory) {
        List<String> allCategories = List.of(
                Image.ResolutionCategory.LOW,
                Image.ResolutionCategory.MEDIUM,
                Image.ResolutionCategory.HIGH,
                Image.ResolutionCategory.ULTRA_HIGH
        );
        
        int minIndex = allCategories.indexOf(minCategory);
        if (minIndex == -1) {
            return allCategories;
        }
        
        return allCategories.subList(minIndex, allCategories.size());
    }
}