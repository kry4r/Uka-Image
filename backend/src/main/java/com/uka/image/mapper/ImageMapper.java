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

/**
 * Image mapper interface
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
}