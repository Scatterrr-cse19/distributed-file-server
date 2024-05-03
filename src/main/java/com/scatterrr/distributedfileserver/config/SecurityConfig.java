package com.scatterrr.distributedfileserver.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SecurityConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(org.springframework.web.servlet.config.annotation.CorsRegistry registry) {
                registry.addMapping("/api/server/**")
                        .allowedOrigins("http://localhost:3000")
                        .allowedMethods("GET", "POST", "DELETE")
                        .allowCredentials(false);
            }
        };
    }
}