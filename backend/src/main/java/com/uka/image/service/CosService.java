package com.uka.image.service;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.exception.CosClientException;
import com.qcloud.cos.exception.CosServiceException;
import com.qcloud.cos.model.*;
import com.qcloud.cos.http.HttpMethodName;
import com.uka.image.config.CosConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Date;
import java.util.UUID;

/**
 * Tencent Cloud COS service for file operations
 * 
 * @author Uka Team
 */
@Slf4j
@Service
public class CosService {

    @Autowired
    private COSClient cosClient;

    @Autowired
    private CosConfig cosConfig;

    /**
     * Upload file to COS
     * 
     * @param file MultipartFile to upload
     * @param folder Folder path in COS bucket
     * @return COS file URL
     */
    public String uploadFile(MultipartFile file, String folder) {
        try {
            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String fileName = UUID.randomUUID().toString() + extension;
            String key = folder + "/" + fileName;

            // Create object metadata
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            metadata.setContentType(file.getContentType());
            metadata.setCacheControl("max-age=31536000"); // 1 year cache

            // Upload file
            PutObjectRequest putObjectRequest = new PutObjectRequest(
                cosConfig.getBucketName(), 
                key, 
                file.getInputStream(), 
                metadata
            );
            
            PutObjectResult putObjectResult = cosClient.putObject(putObjectRequest);
            log.info("File uploaded successfully: {}, ETag: {}", key, putObjectResult.getETag());

            // Return public URL
            return cosConfig.getBaseUrl() + "/" + key;

        } catch (CosServiceException e) {
            log.error("COS service error: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to upload file to COS: " + e.getMessage());
        } catch (CosClientException e) {
            log.error("COS client error: {}", e.getMessage(), e);
            throw new RuntimeException("COS client error: " + e.getMessage());
        } catch (IOException e) {
            log.error("IO error during file upload: {}", e.getMessage(), e);
            throw new RuntimeException("IO error during file upload: " + e.getMessage());
        }
    }

    /**
     * Upload byte array to COS
     * 
     * @param bytes Byte array to upload
     * @param fileName File name
     * @param contentType Content type
     * @param folder Folder path
     * @return COS file URL
     */
    public String uploadBytes(byte[] bytes, String fileName, String contentType, String folder) {
        try {
            String key = folder + "/" + fileName;

            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(bytes.length);
            metadata.setContentType(contentType);
            metadata.setCacheControl("max-age=31536000");

            InputStream inputStream = new ByteArrayInputStream(bytes);
            PutObjectRequest putObjectRequest = new PutObjectRequest(
                cosConfig.getBucketName(), 
                key, 
                inputStream, 
                metadata
            );

            PutObjectResult putObjectResult = cosClient.putObject(putObjectRequest);
            log.info("Bytes uploaded successfully: {}, ETag: {}", key, putObjectResult.getETag());

            return cosConfig.getBaseUrl() + "/" + key;

        } catch (CosServiceException e) {
            log.error("Failed to upload bytes to COS: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to upload bytes to COS: " + e.getMessage());
        } catch (CosClientException e) {
            log.error("Failed to upload bytes to COS: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to upload bytes to COS: " + e.getMessage());
        }
    }

    /**
     * Delete file from COS
     * 
     * @param fileUrl COS file URL
     * @return true if deleted successfully
     */
    public boolean deleteFile(String fileUrl) {
        try {
            // Extract key from URL
            String key = extractKeyFromUrl(fileUrl);
            if (key == null) {
                log.warn("Invalid COS URL: {}", fileUrl);
                return false;
            }

            cosClient.deleteObject(cosConfig.getBucketName(), key);
            log.info("File deleted successfully: {}", key);
            return true;

        } catch (CosServiceException e) {
            log.error("Failed to delete file from COS: {}", e.getMessage(), e);
            return false;
        } catch (CosClientException e) {
            log.error("Failed to delete file from COS: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Check if file exists in COS
     * 
     * @param fileUrl COS file URL
     * @return true if file exists
     */
    public boolean fileExists(String fileUrl) {
        try {
            String key = extractKeyFromUrl(fileUrl);
            if (key == null) {
                return false;
            }

            cosClient.getObjectMetadata(cosConfig.getBucketName(), key);
            return true;

        } catch (CosServiceException e) {
            if (e.getStatusCode() == 404) {
                return false;
            }
            log.error("Error checking file existence: {}", e.getMessage(), e);
            return false;
        } catch (CosClientException e) {
            log.error("COS client error: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Get file metadata
     * 
     * @param fileUrl COS file URL
     * @return ObjectMetadata
     */
    public ObjectMetadata getFileMetadata(String fileUrl) {
        try {
            String key = extractKeyFromUrl(fileUrl);
            if (key == null) {
                return null;
            }

            return cosClient.getObjectMetadata(cosConfig.getBucketName(), key);

        } catch (CosServiceException e) {
            log.error("Failed to get file metadata: {}", e.getMessage(), e);
            return null;
        } catch (CosClientException e) {
            log.error("Failed to get file metadata: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Generate presigned URL for temporary access
     * 
     * @param fileUrl COS file URL
     * @param expiration Expiration time
     * @return Presigned URL
     */
    public String generatePresignedUrl(String fileUrl, Date expiration) {
        try {
            String key = extractKeyFromUrl(fileUrl);
            if (key == null) {
                return null;
            }

            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(
                cosConfig.getBucketName(), 
                key, 
                HttpMethodName.GET
            );
            request.setExpiration(expiration);

            URL url = cosClient.generatePresignedUrl(request);
            return url.toString();

        } catch (CosServiceException e) {
            log.error("Failed to generate presigned URL: {}", e.getMessage(), e);
            return null;
        } catch (CosClientException e) {
            log.error("Failed to generate presigned URL: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Copy file within COS
     * 
     * @param sourceUrl Source file URL
     * @param destinationFolder Destination folder
     * @param newFileName New file name
     * @return New file URL
     */
    public String copyFile(String sourceUrl, String destinationFolder, String newFileName) {
        try {
            String sourceKey = extractKeyFromUrl(sourceUrl);
            if (sourceKey == null) {
                return null;
            }

            String destinationKey = destinationFolder + "/" + newFileName;
            
            CopyObjectRequest copyRequest = new CopyObjectRequest(
                cosConfig.getBucketName(), 
                sourceKey,
                cosConfig.getBucketName(), 
                destinationKey
            );

            cosClient.copyObject(copyRequest);
            log.info("File copied successfully: {} -> {}", sourceKey, destinationKey);

            return cosConfig.getBaseUrl() + "/" + destinationKey;

        } catch (CosServiceException e) {
            log.error("Failed to copy file: {}", e.getMessage(), e);
            return null;
        } catch (CosClientException e) {
            log.error("Failed to copy file: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Extract key from COS URL
     * 
     * @param fileUrl COS file URL
     * @return File key
     */
    private String extractKeyFromUrl(String fileUrl) {
        if (fileUrl == null || !fileUrl.startsWith(cosConfig.getBaseUrl())) {
            return null;
        }
        
        return fileUrl.substring(cosConfig.getBaseUrl().length() + 1);
    }

    /**
     * Generate folder path based on date
     * 
     * @return Folder path like "images/2024/01/15"
     */
    public String generateDateFolder() {
        Date now = new Date();
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy/MM/dd");
        return "images/" + sdf.format(now);
    }

    /**
     * Generate thumbnail folder path
     * 
     * @return Thumbnail folder path
     */
    public String generateThumbnailFolder() {
        Date now = new Date();
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy/MM/dd");
        return "thumbnails/" + sdf.format(now);
    }
}