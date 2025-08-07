package com.uka.image.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.uka.image.entity.Image;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

/**
 * Enhanced Image mapper interface with optimized queries for multi-dimensional search
 * 
 * @author Uka Team
 */
@Mapper
public interface ImageMapper extends BaseMapper<Image> {

    /**
     * Find images by user ID with pagination
     */
    @Select("SELECT i.*, u.username as uploaderUsername FROM images i " +
            "LEFT JOIN users u ON i.user_id = u.id " +
            "WHERE i.user_id = #{userId} AND i.deleted = 0 " +
            "ORDER BY i.created_at DESC")
    IPage<Image> findByUserId(Page<Image> page, @Param("userId") Long userId);

    /**
     * Find all active images with pagination
     */
    @Select("SELECT i.*, u.username as uploaderUsername FROM images i " +
            "LEFT JOIN users u ON i.user_id = u.id " +
            "WHERE i.status = 1 AND i.deleted = 0 " +
            "ORDER BY i.created_at DESC")
    IPage<Image> findAllActive(Page<Image> page);

    /**
     * Search images by description or tags
     */
    @Select("SELECT i.*, u.username as uploaderUsername FROM images i " +
            "LEFT JOIN users u ON i.user_id = u.id " +
            "WHERE (i.description LIKE CONCAT('%', #{keyword}, '%') " +
            "OR i.tags LIKE CONCAT('%', #{keyword}, '%')) " +
            "AND i.status = 1 AND i.deleted = 0 " +
            "ORDER BY i.created_at DESC")
    IPage<Image> searchByKeyword(Page<Image> page, @Param("keyword") String keyword);

    /**
     * Find images by file type
     */
    @Select("SELECT i.*, u.username as uploaderUsername FROM images i " +
            "LEFT JOIN users u ON i.user_id = u.id " +
            "WHERE i.file_type = #{fileType} AND i.status = 1 AND i.deleted = 0 " +
            "ORDER BY i.created_at DESC")
    IPage<Image> findByFileType(Page<Image> page, @Param("fileType") String fileType);

    /**
     * Increment view count
     */
    @Update("UPDATE images SET view_count = view_count + 1 WHERE id = #{id}")
    int incrementViewCount(@Param("id") Long id);

    /**
     * Increment download count
     */
    @Update("UPDATE images SET download_count = download_count + 1 WHERE id = #{id}")
    int incrementDownloadCount(@Param("id") Long id);

    /**
     * Get user's image statistics
     */
    @Select("SELECT COUNT(*) as total, " +
            "SUM(CASE WHEN status = 1 THEN 1 ELSE 0 END) as active, " +
            "SUM(file_size) as totalSize " +
            "FROM images WHERE user_id = #{userId} AND deleted = 0")
    List<Object> getUserImageStats(@Param("userId") Long userId);

    /**
     * Find recent uploaded images
     */
    @Select("SELECT i.*, u.username as uploaderUsername FROM images i " +
            "LEFT JOIN users u ON i.user_id = u.id " +
            "WHERE i.status = 1 AND i.deleted = 0 " +
            "ORDER BY i.created_at DESC LIMIT #{limit}")
    List<Image> findRecentImages(@Param("limit") int limit);
    
    /**
     * Find images by enhanced metadata criteria
     * Uses XML mapper for complex query
     * 
     * @param page Pagination parameters
     * @param params Map of search parameters
     * @return Page of matching images
     */
    IPage<Image> findByMetadata(Page<Image> page, @Param("params") Map<String, Object> params);
    
    /**
     * Find images by file format
     * 
     * @param page Pagination parameters
     * @param fileFormat File format (e.g., JPEG, PNG)
     * @return Page of matching images
     */
    @Select("SELECT i.*, u.username as uploaderUsername FROM images i " +
            "LEFT JOIN users u ON i.user_id = u.id " +
            "WHERE i.file_format = #{fileFormat} AND i.status = 1 AND i.deleted = 0 " +
            "ORDER BY i.created_at DESC")
    IPage<Image> findByFileFormat(Page<Image> page, @Param("fileFormat") String fileFormat);
    
    /**
     * Find images by resolution category
     * 
     * @param page Pagination parameters
     * @param resolutionCategory Resolution category (LOW, MEDIUM, HIGH, ULTRA_HIGH)
     * @return Page of matching images
     */
    @Select("SELECT i.*, u.username as uploaderUsername FROM images i " +
            "LEFT JOIN users u ON i.user_id = u.id " +
            "WHERE i.resolution_category = #{resolutionCategory} AND i.status = 1 AND i.deleted = 0 " +
            "ORDER BY i.created_at DESC")
    IPage<Image> findByResolutionCategory(Page<Image> page, @Param("resolutionCategory") String resolutionCategory);
    
    /**
     * Find images by orientation
     * 
     * @param page Pagination parameters
     * @param orientation Orientation (LANDSCAPE, PORTRAIT, SQUARE, PANORAMIC)
     * @return Page of matching images
     */
    @Select("SELECT i.*, u.username as uploaderUsername FROM images i " +
            "LEFT JOIN users u ON i.user_id = u.id " +
            "WHERE i.orientation = #{orientation} AND i.status = 1 AND i.deleted = 0 " +
            "ORDER BY i.created_at DESC")
    IPage<Image> findByOrientation(Page<Image> page, @Param("orientation") String orientation);
    
    /**
     * Find images by content category
     * 
     * @param page Pagination parameters
     * @param contentCategory Content category
     * @return Page of matching images
     */
    @Select("SELECT i.*, u.username as uploaderUsername FROM images i " +
            "LEFT JOIN users u ON i.user_id = u.id " +
            "WHERE i.content_category = #{contentCategory} AND i.status = 1 AND i.deleted = 0 " +
            "ORDER BY i.created_at DESC")
    IPage<Image> findByContentCategory(Page<Image> page, @Param("contentCategory") String contentCategory);
    
    /**
     * Find images by dominant color (using LIKE for partial hex match)
     * 
     * @param page Pagination parameters
     * @param colorHex Color hex code (partial match)
     * @return Page of matching images
     */
    @Select("SELECT i.*, u.username as uploaderUsername FROM images i " +
            "LEFT JOIN users u ON i.user_id = u.id " +
            "WHERE i.dominant_colors LIKE CONCAT('%', #{colorHex}, '%') AND i.status = 1 AND i.deleted = 0 " +
            "ORDER BY i.created_at DESC")
    IPage<Image> findByDominantColor(Page<Image> page, @Param("colorHex") String colorHex);
    
    /**
     * Find images by AI-generated tags
     * 
     * @param page Pagination parameters
     * @param aiTag AI-generated tag
     * @return Page of matching images
     */
    @Select("SELECT i.*, u.username as uploaderUsername FROM images i " +
            "LEFT JOIN users u ON i.user_id = u.id " +
            "WHERE i.ai_generated_tags LIKE CONCAT('%', #{aiTag}, '%') AND i.status = 1 AND i.deleted = 0 " +
            "ORDER BY i.created_at DESC")
    IPage<Image> findByAIGeneratedTag(Page<Image> page, @Param("aiTag") String aiTag);
    
    /**
     * Find images by semantic keywords
     * 
     * @param page Pagination parameters
     * @param keyword Semantic keyword
     * @return Page of matching images
     */
    @Select("SELECT i.*, u.username as uploaderUsername FROM images i " +
            "LEFT JOIN users u ON i.user_id = u.id " +
            "WHERE i.semantic_keywords LIKE CONCAT('%', #{keyword}, '%') AND i.status = 1 AND i.deleted = 0 " +
            "ORDER BY i.created_at DESC")
    IPage<Image> findBySemanticKeyword(Page<Image> page, @Param("keyword") String keyword);
    
    /**
     * Find images with transparency
     * 
     * @param page Pagination parameters
     * @return Page of matching images
     */
    @Select("SELECT i.*, u.username as uploaderUsername FROM images i " +
            "LEFT JOIN users u ON i.user_id = u.id " +
            "WHERE i.has_transparency = true AND i.status = 1 AND i.deleted = 0 " +
            "ORDER BY i.created_at DESC")
    IPage<Image> findWithTransparency(Page<Image> page);
    
    /**
     * Find animated images
     * 
     * @param page Pagination parameters
     * @return Page of matching images
     */
    @Select("SELECT i.*, u.username as uploaderUsername FROM images i " +
            "LEFT JOIN users u ON i.user_id = u.id " +
            "WHERE i.is_animated = true AND i.status = 1 AND i.deleted = 0 " +
            "ORDER BY i.created_at DESC")
    IPage<Image> findAnimatedImages(Page<Image> page);
    
    /**
     * Find images by brightness level range
     * 
     * @param page Pagination parameters
     * @param minBrightness Minimum brightness level
     * @param maxBrightness Maximum brightness level
     * @return Page of matching images
     */
    @Select("SELECT i.*, u.username as uploaderUsername FROM images i " +
            "LEFT JOIN users u ON i.user_id = u.id " +
            "WHERE i.brightness_level BETWEEN #{minBrightness} AND #{maxBrightness} " +
            "AND i.status = 1 AND i.deleted = 0 " +
            "ORDER BY i.created_at DESC")
    IPage<Image> findByBrightnessRange(Page<Image> page, 
                                      @Param("minBrightness") Integer minBrightness, 
                                      @Param("maxBrightness") Integer maxBrightness);
}