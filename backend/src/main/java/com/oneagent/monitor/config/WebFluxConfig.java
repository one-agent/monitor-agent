package com.oneagent.monitor.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.web.reactive.config.WebFluxConfigurer;

/**
 * WebFlux Configuration
 * Configures WebFlux settings including request body size limits
 */
@Configuration
public class WebFluxConfig implements WebFluxConfigurer {

    /**
     * Configure the maximum size for HTTP request bodies
     * This allows larger JSON payloads containing base64-encoded images
     */
    @Override
    public void configureHttpMessageCodecs(ServerCodecConfigurer configurer) {
        configurer.defaultCodecs().maxInMemorySize(110 * 1024 * 1024); // 100MB
    }
}