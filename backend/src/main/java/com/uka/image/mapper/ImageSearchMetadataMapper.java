package com.uka.image.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.uka.image.entity.ImageSearchMetadata;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * Image search metadata mapper interface
 * 
 * @author Uka Team
 */
@Mapper
public interface ImageSearchMetadataMapper extends BaseMapper<ImageSearchMetadata> {

    /**
     * Find metadata by image ID
     */
    @Select("SELECT * FROM image_search_metadata WHERE image_id = #{imageId}")
    ImageSearchMetadata findByImageId(@Param("imageId") Long imageId);

    /**
     * Search by AI description
     */
    @Select("SELECT * FROM image_search_metadata " +
            "WHERE MATCH(ai_description) AGAINST(#{keyword} IN NATURAL LANGUAGE MODE) " +
            "ORDER BY confidence_score DESC")
    List<ImageSearchMetadata> searchByAiDescription(@Param("keyword") String keyword);

    /**
     * Search by scene classification
     */
    @Select("SELECT * FROM image_search_metadata " +
            "WHERE scene_classification = #{scene} " +
            "ORDER BY confidence_score DESC")
    List<ImageSearchMetadata> findBySceneClassification(@Param("scene") String scene);

    /**
     * Search by text content (OCR)
     */
    @Select("SELECT * FROM image_search_metadata " +
            "WHERE MATCH(text_content) AGAINST(#{keyword} IN NATURAL LANGUAGE MODE) " +
            "ORDER BY confidence_score DESC")
    List<ImageSearchMetadata> searchByTextContent(@Param("keyword") String keyword);

    /**
     * Find images with high confidence AI analysis
     */
    @Select("SELECT * FROM image_search_metadata " +
            "WHERE confidence_score >= #{minScore} " +
            "ORDER BY confidence_score DESC")
    List<ImageSearchMetadata> findHighConfidenceResults(@Param("minScore") Double minScore);

    /**
     * Delete metadata by image ID
     */
    @Delete("DELETE FROM image_search_metadata WHERE image_id = #{imageId}")
    int deleteByImageId(@Param("imageId") Long imageId);
}