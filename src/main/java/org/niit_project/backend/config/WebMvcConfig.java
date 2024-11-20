package org.niit_project.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // Match the WebSocket endpoint path
                .allowedOrigins("*")
                .allowedOriginPatterns("*")// Allow frontend (adjust according to your setup)
                .allowedMethods("GET", "POST", "OPTIONS");
    }

    // Configure HTTP message converters (if needed)
}
