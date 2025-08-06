package com.uka.image.service;

import com.uka.image.entity.Image;
import com.uka.image.mapper.ImageMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Batch upload service for handling multiple image uploads efficiently
 * 
 * @author Uka Team
 */
@Slf4j
@Service
public class BatchUploadService {

    @Autowired
    private ImageMapper imageMapper;

    @Autowired
    private LocalStorageService localStorageService;

    @Autowired
    private ImageService imageService;

    private final ExecutorService executorService = Executors.newFixedThreadPool(10);

    /**
     * Batch upload images with parallel processing
     * 
     * @param files Array of image files
     * @param userId User ID
     * @param descriptions Array of descriptions (optional)
     * @param tagsList Array of tags (optional)
     * @param uploadIp Upload IP address
     * @return BatchUploadResult containing success and failed uploads
     */
    @Transactional
    public BatchUploadResult batchUploadImages(
            MultipartFile[] files, 
            Long userId, 
            String[] descriptions, 
            String[] tagsList, 
            String uploadIp) {
        
        BatchUploadResult result = new BatchUploadResult();
        List<CompletableFuture<UploadResult>> futures = new ArrayList<>();

        // Process each file asynchronously
        for (int i = 0; i < files.length; i++) {
            final int index = i;
            final MultipartFile file = files[i];
            final String description = (descriptions != null && i < descriptions.length) ? descriptions[i] : null;
            final String tags = (tagsList != null && i < tagsList.length) ? tagsList[i] : null;

            CompletableFuture<UploadResult> future = CompletableFuture.supplyAsync(() -> {
                try {
                    Image image = uploadSingleImage(file, userId, description, tags, uploadIp);
                    return new UploadResult(true, image, null);
                } catch (Exception e) {
                    log.error("Failed to upload file {}: {}", file.getOriginalFilename(), e.getMessage());
                    return new UploadResult(false, null, e.getMessage());
                }
            }, executorService);

            futures.add(future);
        }

        // Wait for all uploads to complete
        for (CompletableFuture<UploadResult> future : futures) {
            try {
                UploadResult uploadResult = future.get();
                if (uploadResult.isSuccess()) {
                    result.getSuccessfulUploads().add(uploadResult.getImage());
                } else {
                    result.getFailedUploads().add(uploadResult.getErrorMessage());
                }
            } catch (Exception e) {
                log.error("Error waiting for upload completion: {}", e.getMessage());
                result.getFailedUploads().add("Upload interrupted: " + e.getMessage());
            }
        }

        log.info("Batch upload completed: {} successful, {} failed", 
                result.getSuccessfulUploads().size(), 
                result.getFailedUploads().size());

        return result;
    }

    /**
     * Upload single image with metadata extraction
     * 
     * @param file Image file
     * @param userId User ID
     * @param description Image description
     * @param tags Image tags
     * @param uploadIp Upload IP
     * @return Image entity
     */
    private Image uploadSingleImage(MultipartFile file, Long userId, String description, String tags, String uploadIp) {
        try {
            // Validate file
            validateFile(file);

            // Extract image metadata
            ImageMetadata metadata = extractImageMetadata(file);

            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String fileName = UUID.randomUUID().toString() + extension;

            // Upload to local storage
            String fileUrl = localStorageService.uploadFile(file, originalFilename);
            String filePath = fileUrl.replace("http://localhost:8080/api/files/", "");

            // Create thumbnail if needed
            String thumbnailUrl = null;
            if (shouldCreateThumbnail(file.getContentType())) {
                thumbnailUrl = createThumbnail(file, "thumbnails", fileName);
            }

            // Create image entity
            Image image = new Image();
            image.setUserId(userId);
            image.setOriginalName(originalFilename);
            image.setFileName(fileName);
            image.setFilePath(filePath);
            image.setCosUrl(fileUrl);
            image.setThumbnailUrl(thumbnailUrl);
            image.setFileSize(file.getSize());
            image.setFileType(file.getContentType());
            image.setWidth(metadata.getWidth());
            image.setHeight(metadata.getHeight());
            image.setDescription(description);
            image.setTags(tags);
            image.setUploadIp(uploadIp);
            image.setStatus(Image.Status.ACTIVE);

            // Save to database
            imageMapper.insert(image);

            // Generate AI description asynchronously (placeholder for future AI integration)
            generateAiDescriptionAsync(image);

            return image;

        } catch (Exception e) {
            log.error("Failed to upload image: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to upload image: " + e.getMessage());
        }
    }

    /**
     * Extract image metadata (dimensions, etc.)
     * 
     * @param file Image file
     * @return ImageMetadata
     */
    private ImageMetadata extractImageMetadata(MultipartFile file) throws IOException {
        BufferedImage bufferedImage = ImageIO.read(file.getInputStream());
        if (bufferedImage == null) {
            throw new RuntimeException("Invalid image file");
        }

        ImageMetadata metadata = new ImageMetadata();
        metadata.setWidth(bufferedImage.getWidth());
        metadata.setHeight(bufferedImage.getHeight());
        metadata.setColorModel(bufferedImage.getColorModel().toString());
        metadata.setHasAlpha(bufferedImage.getColorModel().hasAlpha());

        return metadata;
    }

    /**
     * Create thumbnail for image
     * 
     * @param file Original image file
     * @param folder Storage folder
     * @param fileName Original filename
     * @return Thumbnail URL
     */
    private String createThumbnail(MultipartFile file, String folder, String fileName) {
        try {
            BufferedImage originalImage = ImageIO.read(file.getInputStream());
            BufferedImage thumbnail = createThumbnailImage(originalImage, 300, 300);

            // Convert thumbnail to byte array
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            String format = fileName.substring(fileName.lastIndexOf(".") + 1);
            ImageIO.write(thumbnail, format, baos);
            byte[] thumbnailBytes = baos.toByteArray();

            // For now, skip thumbnail upload to local storage
            // This can be implemented later with proper local storage support
            log.info("Thumbnail creation completed for: {}", fileName);
            return null;

        } catch (Exception e) {
            log.warn("Failed to create thumbnail for {}: {}", fileName, e.getMessage());
            return null;
        }
    }

    /**
     * Create thumbnail image with specified dimensions
     * 
     * @param originalImage Original image
     * @param maxWidth Maximum width
     * @param maxHeight Maximum height
     * @return Thumbnail image
     */
    private BufferedImage createThumbnailImage(BufferedImage originalImage, int maxWidth, int maxHeight) {
        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();

        // Calculate new dimensions maintaining aspect ratio
        double aspectRatio = (double) originalWidth / originalHeight;
        int newWidth, newHeight;

        if (originalWidth > originalHeight) {
            newWidth = Math.min(maxWidth, originalWidth);
            newHeight = (int) (newWidth / aspectRatio);
        } else {
            newHeight = Math.min(maxHeight, originalHeight);
            newWidth = (int) (newHeight * aspectRatio);
        }

        // Create thumbnail
        BufferedImage thumbnail = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g2d = thumbnail.createGraphics();
        g2d.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(originalImage, 0, 0, newWidth, newHeight, null);
        g2d.dispose();

        return thumbnail;
    }

    /**
     * Check if thumbnail should be created for file type
     * 
     * @param contentType File content type
     * @return true if thumbnail should be created
     */
    private boolean shouldCreateThumbnail(String contentType) {
        return contentType != null && (
            contentType.equals("image/jpeg") ||
            contentType.equals("image/jpg") ||
            contentType.equals("image/png") ||
            contentType.equals("image/webp")
        );
    }

    /**
     * Generate AI description asynchronously (placeholder)
     * 
     * @param image Image entity
     */
    private void generateAiDescriptionAsync(Image image) {
        CompletableFuture.runAsync(() -> {
            try {
                // Placeholder for AI description generation
                // This would integrate with actual AI service in production
                Thread.sleep(1000); // Simulate AI processing time
                log.info("AI description generated for image: {}", image.getId());
            } catch (Exception e) {
                log.warn("Failed to generate AI description for image {}: {}", image.getId(), e.getMessage());
            }
        }, executorService);
    }

    /**
     * Validate uploaded file
     * 
     * @param file File to validate
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("File is empty");
        }

        if (file.getSize() > 50 * 1024 * 1024) { // 50MB
            throw new RuntimeException("File size exceeds limit");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new RuntimeException("Invalid file type");
        }
    }

    /**
     * Batch upload result container
     */
    public static class BatchUploadResult {
        private List<Image> successfulUploads = new ArrayList<>();
        private List<String> failedUploads = new ArrayList<>();

        public List<Image> getSuccessfulUploads() { return successfulUploads; }
        public List<String> getFailedUploads() { return failedUploads; }
        
        public int getSuccessCount() { return successfulUploads.size(); }
        public int getFailureCount() { return failedUploads.size(); }
        public int getTotalCount() { return successfulUploads.size() + failedUploads.size(); }
    }

    /**
     * Single upload result
     */
    private static class UploadResult {
        private boolean success;
        private Image image;
        private String errorMessage;

        public UploadResult(boolean success, Image image, String errorMessage) {
            this.success = success;
            this.image = image;
            this.errorMessage = errorMessage;
        }

        public boolean isSuccess() { return success; }
        public Image getImage() { return image; }
        public String getErrorMessage() { return errorMessage; }
    }

    /**
     * Image metadata container
     */
    private static class ImageMetadata {
        private int width;
        private int height;
        private String colorModel;
        private boolean hasAlpha;

        public int getWidth() { return width; }
        public void setWidth(int width) { this.width = width; }
        public int getHeight() { return height; }
        public void setHeight(int height) { this.height = height; }
        public String getColorModel() { return colorModel; }
        public void setColorModel(String colorModel) { this.colorModel = colorModel; }
        public boolean isHasAlpha() { return hasAlpha; }
        public void setHasAlpha(boolean hasAlpha) { this.hasAlpha = hasAlpha; }
    }
}