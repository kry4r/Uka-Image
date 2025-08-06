package com.uka.image.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.uka.image.entity.Image;
import com.uka.image.entity.ImageSearchMetadata;
import com.uka.image.mapper.ImageMapper;
import com.uka.image.mapper.ImageSearchMetadataMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Image service for business logic
 * 
 * @author Uka Team
 */
@Slf4j
@Service
public class ImageService {

    @Autowired
    private ImageMapper imageMapper;

    @Autowired
    private ImageSearchMetadataMapper searchMetadataMapper;

    @Autowired
    private LocalStorageService localStorageService;

    private static final List<String> ALLOWED_TYPES = Arrays.asList(
        "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp", "image/bmp"
    );

    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024; // 50MB

    /**
     * Upload single image
     * 
     * @param file MultipartFile
     * @param userId User ID
     * @param description Image description
     * @param tags Image tags
     * @param uploadIp Upload IP
     * @return Image entity
     */
    @Transactional
    public Image uploadImage(MultipartFile file, Long userId, String description, String tags, String uploadIp) {
        // Validate file
        validateFile(file);

        try {
            // Extract image dimensions
            BufferedImage bufferedImage = ImageIO.read(file.getInputStream());
            int width = bufferedImage.getWidth();
            int height = bufferedImage.getHeight();

            // Upload to local storage
            String originalFilename = file.getOriginalFilename();
            String fileUrl = localStorageService.uploadFile(file, originalFilename);

            // Extract filename from URL for database storage
            String fileName = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
            String filePath = fileUrl.replace("http://localhost:8080/api/files/", "");

            // Create image entity
            Image image = new Image();
            image.setUserId(userId);
            image.setOriginalName(originalFilename);
            image.setFileName(fileName);
            image.setFilePath(filePath);
            image.setCosUrl(fileUrl);
            image.setFileSize(file.getSize());
            image.setFileType(file.getContentType());
            image.setWidth(width);
            image.setHeight(height);
            image.setDescription(description);
            image.setTags(tags);
            image.setUploadIp(uploadIp);
            image.setStatus(Image.Status.ACTIVE);

            // Save to database
            imageMapper.insert(image);
            log.info("Image uploaded successfully: {}", image.getId());

            return image;

        } catch (IOException e) {
            log.error("Failed to process image: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process image: " + e.getMessage());
        }
    }

    /**
     * Get image by ID
     * 
     * @param id Image ID
     * @return Image entity
     */
    public Image getImageById(Long id) {
        Image image = imageMapper.selectById(id);
        if (image != null) {
            // Increment view count
            imageMapper.incrementViewCount(id);
            
            // Load search metadata
            ImageSearchMetadata metadata = searchMetadataMapper.findByImageId(id);
            image.setSearchMetadata(metadata);
        }
        return image;
    }

    /**
     * Get images by user ID with pagination
     * 
     * @param userId User ID
     * @param pageNum Page number
     * @param pageSize Page size
     * @return Page of images
     */
    public IPage<Image> getImagesByUserId(Long userId, int pageNum, int pageSize) {
        Page<Image> page = new Page<>(pageNum, pageSize);
        return imageMapper.findByUserId(page, userId);
    }

    /**
     * Get all active images with pagination
     * 
     * @param pageNum Page number
     * @param pageSize Page size
     * @return Page of images
     */
    public IPage<Image> getAllActiveImages(int pageNum, int pageSize) {
        Page<Image> page = new Page<>(pageNum, pageSize);
        return imageMapper.findAllActive(page);
    }

    /**
     * Search images by keyword
     * 
     * @param keyword Search keyword
     * @param pageNum Page number
     * @param pageSize Page size
     * @return Page of images
     */
    public IPage<Image> searchImages(String keyword, int pageNum, int pageSize) {
        Page<Image> page = new Page<>(pageNum, pageSize);
        return imageMapper.searchByKeyword(page, keyword);
    }

    /**
     * Update image description and tags
     * 
     * @param id Image ID
     * @param description New description
     * @param tags New tags
     * @return Updated image
     */
    @Transactional
    public Image updateImageInfo(Long id, String description, String tags) {
        Image image = imageMapper.selectById(id);
        if (image == null) {
            throw new RuntimeException("Image not found: " + id);
        }

        image.setDescription(description);
        image.setTags(tags);
        imageMapper.updateById(image);

        log.info("Image info updated: {}", id);
        return image;
    }

    /**
     * Update image description (alias method for DescriptionController)
     * 
     * @param id Image ID
     * @param description New description
     * @param tags New tags
     * @return Updated image
     */
    @Transactional
    public Image updateImageDescription(Long id, String description, String tags) {
        return updateImageInfo(id, description, tags);
    }

    /**
     * Delete image
     * 
     * @param id Image ID
     * @param userId User ID (for permission check)
     * @return true if deleted successfully
     */
    @Transactional
    public boolean deleteImage(Long id, Long userId) {
        Image image = imageMapper.selectById(id);
        if (image == null) {
            return false;
        }

        // Check permission
        if (!image.getUserId().equals(userId)) {
            throw new RuntimeException("Permission denied");
        }

        // Delete from local storage
        localStorageService.deleteFile(image.getFilePath());
        if (image.getThumbnailUrl() != null) {
            // Extract file path from thumbnail URL and delete
            String thumbnailPath = image.getThumbnailUrl().replace("http://localhost:8080/api/files/", "");
            localStorageService.deleteFile(thumbnailPath);
        }

        // Delete from database (logical delete)
        imageMapper.deleteById(id);

        // Delete search metadata
        QueryWrapper<ImageSearchMetadata> wrapper = new QueryWrapper<>();
        wrapper.eq("image_id", id);
        searchMetadataMapper.delete(wrapper);

        log.info("Image deleted: {}", id);
        return true;
    }

    /**
     * Increment download count
     * 
     * @param id Image ID
     */
    public void incrementDownloadCount(Long id) {
        imageMapper.incrementDownloadCount(id);
    }

    /**
     * Get recent images
     * 
     * @param limit Limit number
     * @return List of recent images
     */
    public List<Image> getRecentImages(int limit) {
        return imageMapper.findRecentImages(limit);
    }

    /**
     * Validate uploaded file
     * 
     * @param file MultipartFile to validate
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("File is empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new RuntimeException("File size exceeds limit: " + MAX_FILE_SIZE + " bytes");
        }

        String contentType = file.getContentType();
        if (!ALLOWED_TYPES.contains(contentType)) {
            throw new RuntimeException("File type not allowed: " + contentType);
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.contains(".")) {
            throw new RuntimeException("Invalid file name");
        }
    }
}