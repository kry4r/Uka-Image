package com.uka.image.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * File serving controller for local storage
 * 
 * @author Uka Team
 */
@Slf4j
@RestController
@RequestMapping("/files")
@CrossOrigin(origins = "*")
public class FileController {

    @Value("${app.upload.path}")
    private String uploadPath;

    /**
     * Serve uploaded files
     * 
     * @param request HttpServletRequest to extract path
     * @return File resource
     */
    @GetMapping("/**")
    public ResponseEntity<Resource> serveFile(HttpServletRequest request) {
        try {
            // Extract file path from request URI
            String requestURI = request.getRequestURI();
            String filePath = requestURI.replace("/api/files/", "");
            
            // URL decode to handle Chinese characters and spaces
            filePath = URLDecoder.decode(filePath, StandardCharsets.UTF_8);
            
            log.info("Serving file: {}", filePath);
            
            Path file = Paths.get(uploadPath, filePath);
            Resource resource = new FileSystemResource(file);

            if (!resource.exists() || !resource.isReadable()) {
                log.warn("File not found or not readable: {}", file.toAbsolutePath());
                return ResponseEntity.notFound().build();
            }

            // Determine content type
            String contentType = determineContentType(file.toString());

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                    .body(resource);

        } catch (Exception e) {
            log.error("Error serving file from request: {}", request.getRequestURI(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Determine content type based on file extension
     * 
     * @param filename Filename
     * @return Content type
     */
    private String determineContentType(String filename) {
        String extension = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
        
        switch (extension) {
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "png":
                return "image/png";
            case "gif":
                return "image/gif";
            case "webp":
                return "image/webp";
            case "bmp":
                return "image/bmp";
            default:
                return "application/octet-stream";
        }
    }
}