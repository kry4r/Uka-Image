package com.uka.image;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main application class for Uka Image Hosting System
 * 
 * @author Uka Team
 * @version 1.0.0
 */
@SpringBootApplication
@MapperScan("com.uka.image.mapper")
public class ImageHostingApplication {

    public static void main(String[] args) {
        SpringApplication.run(ImageHostingApplication.class, args);
        System.out.println("=================================");
        System.out.println("Uka Image Hosting System Started");
        System.out.println("=================================");
    }
}