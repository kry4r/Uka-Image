package com.uka.image.service;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.exif.GpsDirectory;
import com.drew.metadata.file.FileSystemDirectory;
import com.drew.metadata.jpeg.JpegDirectory;
import com.drew.metadata.png.PngDirectory;
import com.uka.image.entity.Image;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.*;

/**
 * MetadataExtractor Service for analyzing and extracting comprehensive image metadata
 * Supports EXIF data, color analysis, visual characteristics, and technical specifications
 * 
 * @author Uka Team
 */
@Slf4j
@Service
public class MetadataExtractor {

    // Color analysis constants
    private static final int COLOR_SAMPLE_SIZE = 100;
    private static final int DOMINANT_COLORS_COUNT = 5;
    
    // Visual complexity thresholds
    private static final double HIGH_COMPLEXITY_THRESHOLD = 0.7;
    private static final double MEDIUM_COMPLEXITY_THRESHOLD = 0.4;

    /**
     * Extract comprehensive metadata from image file
     * 
     * @param imageFile Image file to analyze
     * @param image Image entity to populate with metadata
     * @return Updated image entity with extracted metadata
     */
    public Image extractMetadata(File imageFile, Image image) {
        try {
            log.info("Extracting metadata for image: {}", imageFile.getName());
            
            // Extract basic file information
            extractBasicFileInfo(imageFile, image);
            
            // Extract EXIF metadata
            extractExifMetadata(imageFile, image);
            
            // Analyze image content
            analyzeImageContent(imageFile, image);
            
            // Calculate derived metadata
            calculateDerivedMetadata(image);
            
            log.info("Metadata extraction completed for image ID: {}", image.getId());
            return image;
            
        } catch (Exception e) {
            log.error("Failed to extract metadata for image {}: {}", imageFile.getName(), e.getMessage(), e);
            // Set default values for failed extraction
            setDefaultMetadata(image);
            return image;
        }
    }

    /**
     * Extract basic file information
     */
    private void extractBasicFileInfo(File imageFile, Image image) {
        try {
            // File format extraction
            String fileName = imageFile.getName().toLowerCase();
            if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
                image.setFileFormat("JPEG");
                image.setHasTransparency(false);
            } else if (fileName.endsWith(".png")) {
                image.setFileFormat("PNG");
                image.setHasTransparency(true);
            } else if (fileName.endsWith(".gif")) {
                image.setFileFormat("GIF");
                image.setHasTransparency(true);
                image.setIsAnimated(checkIfAnimated(imageFile));
            } else if (fileName.endsWith(".webp")) {
                image.setFileFormat("WEBP");
                image.setHasTransparency(true);
            } else if (fileName.endsWith(".bmp")) {
                image.setFileFormat("BMP");
                image.setHasTransparency(false);
            } else if (fileName.endsWith(".tiff") || fileName.endsWith(".tif")) {
                image.setFileFormat("TIFF");
                image.setHasTransparency(false);
            } else {
                image.setFileFormat("OTHER");
                image.setHasTransparency(false);
            }
            
            // Set animation flag for non-GIF formats
            if (image.getIsAnimated() == null) {
                image.setIsAnimated(false);
            }
            
        } catch (Exception e) {
            log.warn("Failed to extract basic file info: {}", e.getMessage());
        }
    }

    /**
     * Extract EXIF metadata from image
     */
    private void extractExifMetadata(File imageFile, Image image) {
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(imageFile);
            
            // Extract camera information
            extractCameraInfo(metadata, image);
            
            // Extract GPS information
            extractGpsInfo(metadata, image);
            
            // Extract technical specifications
            extractTechnicalSpecs(metadata, image);
            
        } catch (ImageProcessingException | IOException e) {
            log.warn("Failed to extract EXIF metadata: {}", e.getMessage());
        }
    }

    /**
     * Extract camera information from EXIF data
     */
    private void extractCameraInfo(Metadata metadata, Image image) {
        try {
            ExifIFD0Directory exifIFD0 = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
            if (exifIFD0 != null) {
                // Camera make and model
                if (exifIFD0.hasTagName(ExifIFD0Directory.TAG_MAKE)) {
                    image.setCameraMake(exifIFD0.getString(ExifIFD0Directory.TAG_MAKE));
                }
                if (exifIFD0.hasTagName(ExifIFD0Directory.TAG_MODEL)) {
                    image.setCameraModel(exifIFD0.getString(ExifIFD0Directory.TAG_MODEL));
                }
            }
            
            ExifSubIFDDirectory exifSubIFD = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
            if (exifSubIFD != null) {
                // Focal length
                if (exifSubIFD.hasTagName(ExifSubIFDDirectory.TAG_FOCAL_LENGTH)) {
                    try {
                        image.setFocalLength(exifSubIFD.getDouble(ExifSubIFDDirectory.TAG_FOCAL_LENGTH));
                    } catch (Exception e) {
                        log.debug("Failed to parse focal length: {}", e.getMessage());
                    }
                }
                
                // Aperture
                if (exifSubIFD.hasTagName(ExifSubIFDDirectory.TAG_FNUMBER)) {
                    try {
                        double fNumber = exifSubIFD.getDouble(ExifSubIFDDirectory.TAG_FNUMBER);
                        image.setAperture(String.format("f/%.1f", fNumber));
                    } catch (Exception e) {
                        log.debug("Failed to parse aperture: {}", e.getMessage());
                    }
                }
                
                // ISO speed
                if (exifSubIFD.hasTagName(ExifSubIFDDirectory.TAG_ISO_EQUIVALENT)) {
                    try {
                        image.setIsoSpeed(exifSubIFD.getInt(ExifSubIFDDirectory.TAG_ISO_EQUIVALENT));
                    } catch (Exception e) {
                        log.debug("Failed to parse ISO speed: {}", e.getMessage());
                    }
                }
                
                // Exposure time
                if (exifSubIFD.hasTagName(ExifSubIFDDirectory.TAG_EXPOSURE_TIME)) {
                    try {
                        image.setExposureTime(exifSubIFD.getString(ExifSubIFDDirectory.TAG_EXPOSURE_TIME));
                    } catch (Exception e) {
                        log.debug("Failed to parse exposure time: {}", e.getMessage());
                    }
                }
            }
            
        } catch (Exception e) {
            log.warn("Failed to extract camera info: {}", e.getMessage());
        }
    }

    /**
     * Extract GPS information from EXIF data
     */
    private void extractGpsInfo(Metadata metadata, Image image) {
        try {
            GpsDirectory gpsDirectory = metadata.getFirstDirectoryOfType(GpsDirectory.class);
            if (gpsDirectory != null) {
                if (gpsDirectory.hasTagName(GpsDirectory.TAG_LATITUDE) && 
                    gpsDirectory.hasTagName(GpsDirectory.TAG_LONGITUDE)) {
                    
                    try {
                        double latitude = gpsDirectory.getGeoLocation().getLatitude();
                        double longitude = gpsDirectory.getGeoLocation().getLongitude();
                        image.setGpsLatitude(latitude);
                        image.setGpsLongitude(longitude);
                    } catch (Exception e) {
                        log.debug("Failed to parse GPS coordinates: {}", e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to extract GPS info: {}", e.getMessage());
        }
    }

    /**
     * Extract technical specifications
     */
    private void extractTechnicalSpecs(Metadata metadata, Image image) {
        try {
            // Color profile information
            for (Directory directory : metadata.getDirectories()) {
                if (directory.hasTagName(0x0112)) { // Orientation tag
                    try {
                        int orientation = directory.getInt(0x0112);
                        // Process orientation if needed
                    } catch (Exception e) {
                        log.debug("Failed to parse orientation: {}", e.getMessage());
                    }
                }
            }
            
            // Set default color profile
            if (image.getColorProfile() == null) {
                image.setColorProfile("sRGB");
            }
            
        } catch (Exception e) {
            log.warn("Failed to extract technical specs: {}", e.getMessage());
        }
    }

    /**
     * Analyze image content for visual characteristics
     */
    private void analyzeImageContent(File imageFile, Image image) {
        try {
            BufferedImage bufferedImage = ImageIO.read(imageFile);
            if (bufferedImage == null) {
                log.warn("Could not read image for content analysis: {}", imageFile.getName());
                return;
            }
            
            // Analyze colors
            analyzeColors(bufferedImage, image);
            
            // Analyze visual complexity
            analyzeVisualComplexity(bufferedImage, image);
            
            // Analyze brightness, contrast, saturation
            analyzeVisualCharacteristics(bufferedImage, image);
            
        } catch (IOException e) {
            log.warn("Failed to analyze image content: {}", e.getMessage());
        }
    }

    /**
     * Analyze dominant colors in the image
     */
    private void analyzeColors(BufferedImage image, Image imageEntity) {
        try {
            Map<Integer, Integer> colorFrequency = new HashMap<>();
            int width = image.getWidth();
            int height = image.getHeight();
            
            // Sample colors from the image
            int stepX = Math.max(1, width / COLOR_SAMPLE_SIZE);
            int stepY = Math.max(1, height / COLOR_SAMPLE_SIZE);
            
            for (int x = 0; x < width; x += stepX) {
                for (int y = 0; y < height; y += stepY) {
                    int rgb = image.getRGB(x, y);
                    // Quantize color to reduce noise
                    int quantizedColor = quantizeColor(rgb);
                    colorFrequency.put(quantizedColor, colorFrequency.getOrDefault(quantizedColor, 0) + 1);
                }
            }
            
            // Find dominant colors
            List<Map.Entry<Integer, Integer>> sortedColors = colorFrequency.entrySet()
                .stream()
                .sorted(Map.Entry.<Integer, Integer>comparingByValue().reversed())
                .limit(DOMINANT_COLORS_COUNT)
                .toList();
            
            // Convert to hex strings
            List<String> dominantColors = new ArrayList<>();
            for (Map.Entry<Integer, Integer> entry : sortedColors) {
                String hexColor = String.format("#%06X", entry.getKey() & 0xFFFFFF);
                dominantColors.add(hexColor);
            }
            
            imageEntity.setDominantColors(String.join(",", dominantColors));
            
        } catch (Exception e) {
            log.warn("Failed to analyze colors: {}", e.getMessage());
        }
    }

    /**
     * Quantize color to reduce noise in color analysis
     */
    private int quantizeColor(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        
        // Quantize to 32 levels per channel
        r = (r / 8) * 8;
        g = (g / 8) * 8;
        b = (b / 8) * 8;
        
        return (r << 16) | (g << 8) | b;
    }

    /**
     * Analyze visual complexity of the image
     */
    private void analyzeVisualComplexity(BufferedImage image, Image imageEntity) {
        try {
            int width = image.getWidth();
            int height = image.getHeight();
            
            // Calculate edge density as a measure of complexity
            int edgeCount = 0;
            int totalPixels = 0;
            
            for (int x = 1; x < width - 1; x++) {
                for (int y = 1; y < height - 1; y++) {
                    int center = image.getRGB(x, y);
                    int right = image.getRGB(x + 1, y);
                    int bottom = image.getRGB(x, y + 1);
                    
                    // Simple edge detection
                    if (colorDifference(center, right) > 30 || colorDifference(center, bottom) > 30) {
                        edgeCount++;
                    }
                    totalPixels++;
                }
            }
            
            double complexity = totalPixels > 0 ? (double) edgeCount / totalPixels : 0.0;
            imageEntity.setVisualComplexityScore(Math.min(complexity * 10, 1.0)); // Normalize to 0-1
            
        } catch (Exception e) {
            log.warn("Failed to analyze visual complexity: {}", e.getMessage());
        }
    }

    /**
     * Calculate color difference between two RGB values
     */
    private int colorDifference(int rgb1, int rgb2) {
        int r1 = (rgb1 >> 16) & 0xFF;
        int g1 = (rgb1 >> 8) & 0xFF;
        int b1 = rgb1 & 0xFF;
        
        int r2 = (rgb2 >> 16) & 0xFF;
        int g2 = (rgb2 >> 8) & 0xFF;
        int b2 = rgb2 & 0xFF;
        
        return Math.abs(r1 - r2) + Math.abs(g1 - g2) + Math.abs(b1 - b2);
    }

    /**
     * Analyze brightness, contrast, and saturation
     */
    private void analyzeVisualCharacteristics(BufferedImage image, Image imageEntity) {
        try {
            int width = image.getWidth();
            int height = image.getHeight();
            
            long totalBrightness = 0;
            long totalSaturation = 0;
            int minBrightness = 255;
            int maxBrightness = 0;
            int sampleCount = 0;
            
            // Sample pixels for analysis
            int stepX = Math.max(1, width / 50);
            int stepY = Math.max(1, height / 50);
            
            for (int x = 0; x < width; x += stepX) {
                for (int y = 0; y < height; y += stepY) {
                    int rgb = image.getRGB(x, y);
                    
                    int r = (rgb >> 16) & 0xFF;
                    int g = (rgb >> 8) & 0xFF;
                    int b = rgb & 0xFF;
                    
                    // Calculate brightness (luminance)
                    int brightness = (int) (0.299 * r + 0.587 * g + 0.114 * b);
                    totalBrightness += brightness;
                    minBrightness = Math.min(minBrightness, brightness);
                    maxBrightness = Math.max(maxBrightness, brightness);
                    
                    // Calculate saturation
                    int max = Math.max(r, Math.max(g, b));
                    int min = Math.min(r, Math.min(g, b));
                    int saturation = max > 0 ? ((max - min) * 255) / max : 0;
                    totalSaturation += saturation;
                    
                    sampleCount++;
                }
            }
            
            if (sampleCount > 0) {
                // Average brightness (0-1)
                double avgBrightness = (double) totalBrightness / (sampleCount * 255);
                imageEntity.setBrightnessLevel(avgBrightness);
                
                // Contrast (0-1)
                double contrast = (double) (maxBrightness - minBrightness) / 255;
                imageEntity.setContrastLevel(contrast);
                
                // Average saturation (0-1)
                double avgSaturation = (double) totalSaturation / (sampleCount * 255);
                imageEntity.setSaturationLevel(avgSaturation);
                
                // Estimate color temperature based on color balance
                estimateColorTemperature(image, imageEntity);
            }
            
        } catch (Exception e) {
            log.warn("Failed to analyze visual characteristics: {}", e.getMessage());
        }
    }

    /**
     * Estimate color temperature based on color balance
     */
    private void estimateColorTemperature(BufferedImage image, Image imageEntity) {
        try {
            int width = image.getWidth();
            int height = image.getHeight();
            
            long totalR = 0, totalG = 0, totalB = 0;
            int sampleCount = 0;
            
            // Sample pixels
            int stepX = Math.max(1, width / 20);
            int stepY = Math.max(1, height / 20);
            
            for (int x = 0; x < width; x += stepX) {
                for (int y = 0; y < height; y += stepY) {
                    int rgb = image.getRGB(x, y);
                    
                    totalR += (rgb >> 16) & 0xFF;
                    totalG += (rgb >> 8) & 0xFF;
                    totalB += rgb & 0xFF;
                    sampleCount++;
                }
            }
            
            if (sampleCount > 0) {
                double avgR = (double) totalR / sampleCount;
                double avgG = (double) totalG / sampleCount;
                double avgB = (double) totalB / sampleCount;
                
                // Simple color temperature estimation
                double colorTemp;
                if (avgR > avgB) {
                    // Warmer image
                    colorTemp = 3000 + (avgR - avgB) * 20;
                } else {
                    // Cooler image
                    colorTemp = 6500 + (avgB - avgR) * 15;
                }
                
                imageEntity.setColorTemperature((int) Math.max(2000, Math.min(10000, colorTemp)));
            }
            
        } catch (Exception e) {
            log.warn("Failed to estimate color temperature: {}", e.getMessage());
        }
    }

    /**
     * Calculate derived metadata based on extracted information
     */
    private void calculateDerivedMetadata(Image image) {
        // Calculate aspect ratio
        image.calculateAspectRatio();
        
        // Determine resolution category
        image.calculateResolutionCategory();
        
        // Determine orientation
        image.calculateOrientation();
        
        // Extract file format if not already set
        if (image.getFileFormat() == null) {
            image.extractFileFormat();
        }
        
        // Determine content category based on available information
        determineContentCategory(image);
    }

    /**
     * Determine content category based on image characteristics
     */
    private void determineContentCategory(Image image) {
        try {
            String fileName = image.getFileName() != null ? image.getFileName().toLowerCase() : "";
            String description = image.getDescription() != null ? image.getDescription().toLowerCase() : "";
            String tags = image.getTags() != null ? image.getTags().toLowerCase() : "";
            
            // Simple rule-based categorization
            if (fileName.contains("logo") || description.contains("logo") || tags.contains("logo")) {
                image.setContentCategory(Image.ContentCategory.LOGO);
            } else if (fileName.contains("icon") || description.contains("icon") || tags.contains("icon")) {
                image.setContentCategory(Image.ContentCategory.ICON);
            } else if (fileName.contains("screenshot") || description.contains("screenshot")) {
                image.setContentCategory(Image.ContentCategory.SCREENSHOT);
            } else if (image.hasCameraMetadata()) {
                image.setContentCategory(Image.ContentCategory.PHOTOGRAPHY);
            } else if (tags.contains("art") || tags.contains("design")) {
                image.setContentCategory(Image.ContentCategory.ARTWORK);
            } else if (tags.contains("texture") || tags.contains("pattern")) {
                image.setContentCategory(Image.ContentCategory.TEXTURE);
            } else {
                image.setContentCategory(Image.ContentCategory.PHOTOGRAPHY); // Default
            }
            
        } catch (Exception e) {
            log.warn("Failed to determine content category: {}", e.getMessage());
            image.setContentCategory(Image.ContentCategory.PHOTOGRAPHY);
        }
    }

    /**
     * Check if GIF image is animated
     */
    private boolean checkIfAnimated(File imageFile) {
        try (ImageInputStream iis = ImageIO.createImageInputStream(imageFile)) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            if (readers.hasNext()) {
                ImageReader reader = readers.next();
                reader.setInput(iis);
                return reader.getNumImages(true) > 1;
            }
        } catch (IOException e) {
            log.debug("Failed to check if GIF is animated: {}", e.getMessage());
        }
        return false;
    }

    /**
     * Set default metadata values for failed extractions
     */
    private void setDefaultMetadata(Image image) {
        if (image.getFileFormat() == null) {
            image.setFileFormat("UNKNOWN");
        }
        if (image.getHasTransparency() == null) {
            image.setHasTransparency(false);
        }
        if (image.getIsAnimated() == null) {
            image.setIsAnimated(false);
        }
        if (image.getColorProfile() == null) {
            image.setColorProfile("sRGB");
        }
        if (image.getContentCategory() == null) {
            image.setContentCategory(Image.ContentCategory.PHOTOGRAPHY);
        }
        
        // Calculate basic derived metadata
        image.calculateAspectRatio();
        image.calculateResolutionCategory();
        image.calculateOrientation();
    }

    /**
     * Generate AI tags based on extracted metadata and visual analysis
     * 
     * @param image Image entity with extracted metadata
     * @return Generated AI tags string
     */
    public String generateAITags(Image image) {
        List<String> aiTags = new ArrayList<>();
        
        try {
            // Add technical tags
            if (image.getFileFormat() != null) {
                aiTags.add(image.getFileFormat().toLowerCase());
            }
            
            if (image.getOrientation() != null) {
                aiTags.add(image.getOrientation().toLowerCase());
            }
            
            if (image.getResolutionCategory() != null) {
                aiTags.add(image.getResolutionCategory().toLowerCase() + "_resolution");
            }
            
            // Add visual characteristic tags
            if (image.getBrightnessLevel() != null) {
                if (image.getBrightnessLevel() > 0.7) {
                    aiTags.add("bright");
                } else if (image.getBrightnessLevel() < 0.3) {
                    aiTags.add("dark");
                }
            }
            
            if (image.getContrastLevel() != null) {
                if (image.getContrastLevel() > 0.7) {
                    aiTags.add("high_contrast");
                } else if (image.getContrastLevel() < 0.3) {
                    aiTags.add("low_contrast");
                }
            }
            
            if (image.getSaturationLevel() != null) {
                if (image.getSaturationLevel() > 0.7) {
                    aiTags.add("vibrant");
                } else if (image.getSaturationLevel() < 0.3) {
                    aiTags.add("muted");
                }
            }
            
            // Add complexity tags
            if (image.getVisualComplexityScore() != null) {
                if (image.getVisualComplexityScore() > HIGH_COMPLEXITY_THRESHOLD) {
                    aiTags.add("complex");
                } else if (image.getVisualComplexityScore() < MEDIUM_COMPLEXITY_THRESHOLD) {
                    aiTags.add("simple");
                }
            }
            
            // Add special property tags
            if (image.getHasTransparency() != null && image.getHasTransparency()) {
                aiTags.add("transparent");
            }
            
            if (image.getIsAnimated() != null && image.getIsAnimated()) {
                aiTags.add("animated");
            }
            
            // Add camera-related tags
            if (image.hasCameraMetadata()) {
                aiTags.add("photography");
                if (image.getCameraMake() != null) {
                    aiTags.add(image.getCameraMake().toLowerCase().replaceAll("\\s+", "_"));
                }
            }
            
            // Add location tags
            if (image.hasLocationData()) {
                aiTags.add("geotagged");
            }
            
        } catch (Exception e) {
            log.warn("Failed to generate AI tags: {}", e.getMessage());
        }
        
        return String.join(", ", aiTags);
    }
}