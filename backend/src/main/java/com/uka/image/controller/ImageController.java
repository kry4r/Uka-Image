package com.uka.image.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.uka.image.dto.ApiResponse;
import com.uka.image.dto.ImageUploadRequest;
import com.uka.image.entity.Image;
import com.uka.image.service.ImageService;
import com.uka.image.service.BatchUploadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

/**
 * Image REST API Controller
 * 
 * @author Uka Team
 */
@Slf4j
@RestController
@RequestMapping("/images")
@CrossOrigin(origins = "*")
public class ImageController {

    @Autowired
    private ImageService imageService;

    @Autowired
    private BatchUploadService batchUploadService;

    /**
     * Upload single image
     * 
     * @param file Image file
     * @param description Image description
     * @param tags Image tags
     * @param customName Custom image name
     * @param hashId Unique hash ID
     * @param httpRequest HTTP request
     * @return API response with uploaded image
     */
    @PostMapping("/upload")
    public ApiResponse<Image> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "tags", required = false) String tags,
            @RequestParam(value = "customName", required = false) String customName,
            @RequestParam(value = "hashId", required = false) String hashId,
            HttpServletRequest httpRequest) {
        
        try {
            // Get client IP
            String uploadIp = getClientIpAddress(httpRequest);
            
            // For demo purposes, use default user ID = 1
            // In real application, get user ID from authentication context
            Long userId = 1L;
            
            Image image = imageService.uploadImage(file, userId, description, tags, uploadIp, customName, hashId);
            
            log.info("Image uploaded successfully: {}", image.getId());
            return ApiResponse.success("Image uploaded successfully", image);
            
        } catch (Exception e) {
            log.error("Failed to upload image: {}", e.getMessage(), e);
            return ApiResponse.error("Failed to upload image: " + e.getMessage());
        }
    }

    /**
     * Batch upload images with enhanced processing
     * 
     * @param files Image files
     * @param descriptions Image descriptions (optional)
     * @param tagsList Image tags list (optional)
     * @param httpRequest HTTP request
     * @return API response with batch upload results
     */
    @PostMapping("/batch-upload")
    public ApiResponse<BatchUploadService.BatchUploadResult> batchUploadImages(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam(value = "descriptions", required = false) String[] descriptions,
            @RequestParam(value = "tagsList", required = false) String[] tagsList,
            HttpServletRequest httpRequest) {
        
        try {
            if (files == null || files.length == 0) {
                return ApiResponse.badRequest("No files provided");
            }
            
            if (files.length > 20) { // Limit batch size
                return ApiResponse.badRequest("Batch upload limit exceeded (max 20 files)");
            }
            
            String uploadIp = getClientIpAddress(httpRequest);
            Long userId = 1L; // Demo user ID
            
            BatchUploadService.BatchUploadResult result = batchUploadService.batchUploadImages(
                files, userId, descriptions, tagsList, uploadIp);
            
            String message = String.format("Batch upload completed: %d successful, %d failed", 
                result.getSuccessCount(), result.getFailureCount());
            
            log.info(message);
            return ApiResponse.success(message, result);
            
        } catch (Exception e) {
            log.error("Failed to batch upload images: {}", e.getMessage(), e);
            return ApiResponse.error("Failed to batch upload images: " + e.getMessage());
        }
    }

    /**
     * Get image by ID
     * 
     * @param id Image ID
     * @return API response with image details
     */
    @GetMapping("/{id}")
    public ApiResponse<Image> getImage(@PathVariable Long id) {
        try {
            Image image = imageService.getImageById(id);
            if (image == null) {
                return ApiResponse.notFound("Image not found");
            }
            
            return ApiResponse.success(image);
            
        } catch (Exception e) {
            log.error("Failed to get image: {}", e.getMessage(), e);
            return ApiResponse.error("Failed to get image: " + e.getMessage());
        }
    }

    /**
     * Get images with pagination
     * 
     * @param pageNum Page number (default: 1)
     * @param pageSize Page size (default: 20)
     * @param userId User ID (optional)
     * @return API response with paginated images
     */
    @GetMapping("/list")
    public ApiResponse<IPage<Image>> getImages(
            @RequestParam(value = "pageNum", defaultValue = "1") int pageNum,
            @RequestParam(value = "pageSize", defaultValue = "20") int pageSize,
            @RequestParam(value = "userId", required = false) Long userId) {
        
        try {
            IPage<Image> images;
            
            if (userId != null) {
                images = imageService.getImagesByUserId(userId, pageNum, pageSize);
            } else {
                images = imageService.getAllActiveImages(pageNum, pageSize);
            }
            
            return ApiResponse.success(images);
            
        } catch (Exception e) {
            log.error("Failed to get images: {}", e.getMessage(), e);
            return ApiResponse.error("Failed to get images: " + e.getMessage());
        }
    }

    /**
     * Search images by keyword
     * 
     * @param keyword Search keyword
     * @param pageNum Page number (default: 1)
     * @param pageSize Page size (default: 20)
     * @return API response with search results
     */
    @GetMapping("/search")
    public ApiResponse<IPage<Image>> searchImages(
            @RequestParam("keyword") String keyword,
            @RequestParam(value = "pageNum", defaultValue = "1") int pageNum,
            @RequestParam(value = "pageSize", defaultValue = "20") int pageSize) {
        
        try {
            IPage<Image> images = imageService.searchImages(keyword, pageNum, pageSize);
            return ApiResponse.success(images);
            
        } catch (Exception e) {
            log.error("Failed to search images: {}", e.getMessage(), e);
            return ApiResponse.error("Failed to search images: " + e.getMessage());
        }
    }

    /**
     * Update image information
     * 
     * @param id Image ID
     * @param description New description
     * @param tags New tags
     * @return API response with updated image
     */
    @PutMapping("/{id}")
    public ApiResponse<Image> updateImage(
            @PathVariable Long id,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "tags", required = false) String tags) {
        
        try {
            Image image = imageService.updateImageInfo(id, description, tags);
            return ApiResponse.success("Image updated successfully", image);
            
        } catch (Exception e) {
            log.error("Failed to update image: {}", e.getMessage(), e);
            return ApiResponse.error("Failed to update image: " + e.getMessage());
        }
    }

    /**
     * Delete image
     * 
     * @param id Image ID
     * @return API response
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteImage(@PathVariable Long id) {
        try {
            // For demo purposes, use default user ID = 1
            // In real application, get user ID from authentication context
            Long userId = 1L;
            
            boolean deleted = imageService.deleteImage(id, userId);
            if (deleted) {
                return ApiResponse.success("Image deleted successfully", null);
            } else {
                return ApiResponse.notFound("Image not found");
            }
            
        } catch (Exception e) {
            log.error("Failed to delete image: {}", e.getMessage(), e);
            return ApiResponse.error("Failed to delete image: " + e.getMessage());
        }
    }

    /**
     * Download image (increment download count)
     * 
     * @param id Image ID
     * @return API response with download URL
     */
    @PostMapping("/{id}/download")
    public ApiResponse<String> downloadImage(@PathVariable Long id) {
        try {
            Image image = imageService.getImageById(id);
            if (image == null) {
                return ApiResponse.notFound("Image not found");
            }
            
            // Increment download count
            imageService.incrementDownloadCount(id);
            
            return ApiResponse.success("Download URL retrieved", image.getCosUrl());
            
        } catch (Exception e) {
            log.error("Failed to get download URL: {}", e.getMessage(), e);
            return ApiResponse.error("Failed to get download URL: " + e.getMessage());
        }
    }

    /**
     * Get recent images
     * 
     * @param limit Limit number (default: 10)
     * @return API response with recent images
     */
    @GetMapping("/recent")
    public ApiResponse<List<Image>> getRecentImages(
            @RequestParam(value = "limit", defaultValue = "10") int limit) {
        
        try {
            List<Image> images = imageService.getRecentImages(limit);
            return ApiResponse.success(images);
            
        } catch (Exception e) {
            log.error("Failed to get recent images: {}", e.getMessage(), e);
            return ApiResponse.error("Failed to get recent images: " + e.getMessage());
        }
    }

    /**
     * Get client IP address from HTTP request
     * 
     * @param request HTTP request
     * @return Client IP address
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0];
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}