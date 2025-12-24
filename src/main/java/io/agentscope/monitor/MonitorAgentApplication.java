package io.agentscope.monitor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;

/**
 * Main application class for Monitor Agent
 */
@Slf4j
@SpringBootApplication
public class MonitorAgentApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(MonitorAgentApplication.class, args);

        Environment env = context.getEnvironment();
        log.info("=========================================");
        log.info("Monitor Agent Started Successfully!");
        log.info("=========================================");
        log.info("Application Name: {}", env.getProperty("spring.application.name"));
        log.info("Server Port: {}", env.getProperty("server.port"));
        log.info("LLM Model: {}", env.getProperty("llm.model-name"));
        log.info("LLM Base URL: {}", env.getProperty("llm.base-url"));
        log.info("=========================================");
        log.info("Available Endpoints:");
        log.info("  - POST /api/chat              : Simple chat");
        log.info("  - POST /api/process            : Process single case");
        log.info("  - POST /api/process-batch       : Batch processing");
        log.info("  - GET  /api/monitor/status     : Monitor status");
        log.info("  - GET  /api/health              : Health check");
        log.info("  - GET  /api/info                : API info");
        log.info("=========================================");
    }
}
