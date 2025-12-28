package com.oneagent.monitor.studio;

import com.oneagent.monitor.model.config.MonitorProperties;
import io.agentscope.core.studio.StudioManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

/**
 * Studio 初始化组件
 * 在应用启动时初始化 Studio Manager
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StudioInitializer {

    private final MonitorProperties monitorProperties;

    /**
     * 应用启动后初始化 Studio
     */
    @PostConstruct
    public void initialize() {
        if (!monitorProperties.getStudio().isEnabled()) {
            log.info("Studio is disabled, skipping initialization");
            return;
        }

        try {
            log.info("==================================================");
            log.info("Initializing Studio connection...");
            log.info("Studio URL: {}", monitorProperties.getStudio().getStudioUrl());
            log.info("Project: {}", monitorProperties.getStudio().getProjectName());
            log.info("Run: {}", monitorProperties.getStudio().getRunName());
            log.info("==================================================");

            StudioManager.init()
                    .studioUrl(monitorProperties.getStudio().getStudioUrl())
                    .project(monitorProperties.getStudio().getProjectName())
                    .runName(monitorProperties.getStudio().getRunName() + System.currentTimeMillis())
                    .initialize()
                    .block();

            log.info("Studio initialized successfully!");
            log.info("You can view traces at: {}", monitorProperties.getStudio().getStudioUrl());

        } catch (Exception e) {
            log.error("Failed to initialize Studio", e);
            log.warn("Application will continue without Studio integration");
        }
    }

    /**
     * 应用关闭时清理 Studio 资源
     */
    @PreDestroy
    public void shutdown() {
        if (monitorProperties.getStudio().isEnabled()) {
            log.info("Shutting down Studio connection...");
            try {
                StudioManager.shutdown();
                log.info("Studio shutdown completed");
            } catch (Exception e) {
                log.error("Error during Studio shutdown", e);
            }
        }
    }
}
