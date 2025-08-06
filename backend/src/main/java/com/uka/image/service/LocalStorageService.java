package com.uka.image.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Local file storage service
 * 
 * @author Uka Team
 */
@Slf4j
@Service
public class LocalStorageService {

    @Value("${app.upload.path}")
    private String uploadPath;

    @Value("${app.upload.base-url}")
    private String baseUrl;

    /**
     * Upload file to local storage
     * 
     * @param file Multipart file
     * @param originalName Original filename
     * @return File access URL
     * @throws IOException If file upload fails
     */
    public String uploadFile(MultipartFile file, String originalName) throws IOException {
        // Create upload directory if not exists
        File uploadDir = new File(uploadPath);
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }

        // Generate unique filename
        String fileName = generateUniqueFileName(originalName, file.getBytes());
        
        // Create date-based subdirectory
        String dateDir = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        File targetDir = new File(uploadDir, dateDir);
        if (!targetDir.exists()) {
            targetDir.mkdirs();
        }

        // Save file
        Path targetPath = Paths.get(targetDir.getAbsolutePath(), fileName);
        Files.write(targetPath, file.getBytes());

        // Return access URL
        String relativePath = dateDir + "/" + fileName;
        String accessUrl = baseUrl + "/" + relativePath.replace("\\", "/");
        
        log.info("File uploaded successfully: {}", accessUrl);
        return accessUrl;
    }

    /**
     * Generate unique filename with hash to avoid duplicates
     * 
     * @param originalName Original filename
     * @param fileBytes File content bytes
     * @return Unique filename
     */
    private String generateUniqueFileName(String originalName, byte[] fileBytes) {
        try {
            // Get file extension
            String extension = "";
            int lastDotIndex = originalName.lastIndexOf('.');
            if (lastDotIndex > 0) {
                extension = originalName.substring(lastDotIndex);
            }

            // Generate hash from file content
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hashBytes = md.digest(fileBytes);
            StringBuilder hashString = new StringBuilder();
            for (byte b : hashBytes) {
                hashString.append(String.format("%02x", b));
            }

            // Use original name if possible, otherwise use hash
            String baseName = originalName.substring(0, lastDotIndex > 0 ? lastDotIndex : originalName.length());
            String fileName = baseName + "_" + hashString.substring(0, 8) + extension;

            return fileName;
        } catch (NoSuchAlgorithmException e) {
            // Fallback to UUID if hash generation fails
            log.warn("Failed to generate hash, using UUID instead", e);
            return UUID.randomUUID().toString() + getFileExtension(originalName);
        }
    }

    /**
     * Get file extension from filename
     * 
     * @param filename Filename
     * @return File extension with dot
     */
    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        return lastDotIndex > 0 ? filename.substring(lastDotIndex) : "";
    }

    /**
     * Delete file from local storage
     * 
     * @param filePath File path relative to upload directory
     * @return true if deleted successfully
     */
    public boolean deleteFile(String filePath) {
        try {
            Path targetPath = Paths.get(uploadPath, filePath);
            return Files.deleteIfExists(targetPath);
        } catch (IOException e) {
            log.error("Failed to delete file: {}", filePath, e);
            return false;
        }
    }
}