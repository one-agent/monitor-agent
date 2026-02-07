package com.oneagent.monitor.config;

import com.oneagent.monitor.model.config.MonitorProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

/**
 * CORS Configuration for WebFlux
 * Allows frontend to access backend API
 */
@Slf4j
@Configuration
public class CorsConfig {

    private final MonitorProperties monitorProperties;

    public CorsConfig(MonitorProperties monitorProperties) {
        this.monitorProperties = monitorProperties;
    }

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration config = new CorsConfiguration();

        // Allow all headers
        config.addAllowedHeader("*");

        // Allow all HTTP methods
        config.addAllowedMethod("*");

        // Get allowed origins from configuration
        String allowedOrigins = monitorProperties.getCors().getAllowedOrigins();
        log.info("CORS allowed origins: {}", allowedOrigins);

        if (allowedOrigins == null || allowedOrigins.trim().isEmpty() || "*".equals(allowedOrigins.trim())) {
            // Allow all origins
            config.addAllowedOriginPattern("*");
            log.info("CORS configured to allow all origins");
        } else {
            // Parse comma-separated origins
            String[] origins = allowedOrigins.split(",");
            for (String origin : origins) {
                String trimmedOrigin = origin.trim();
                if (!trimmedOrigin.isEmpty()) {
                    if (trimmedOrigin.contains("*")) {
                        // Use pattern for wildcard origins
                        config.addAllowedOriginPattern(trimmedOrigin);
                    } else {
                        // Use exact match for specific origins
                        config.addAllowedOrigin(trimmedOrigin);
                    }
                    log.info("CORS allowed origin added: {}", trimmedOrigin);
                }
            }
        }

        // Don't allow credentials for wildcard origins
        config.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsWebFilter(source);
    }
}