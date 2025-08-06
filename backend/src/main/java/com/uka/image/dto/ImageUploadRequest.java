package com.uka.image.dto;

import lombok.Data;

/**
 * Image upload request DTO
 * 
 * @author Uka Team
 */
@Data
public class ImageUploadRequest {
    
    private String description;
    private String tags;
    
    // For batch upload
    private String[] descriptions;
    private String[] tagsList;
}
