package com.oneagent.monitor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;

/**
 * 监控 Agent 主应用程序类
 */
@Slf4j
@SpringBootApplication
public class MonitorAgentApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(MonitorAgentApplication.class, args);

        Environment env = context.getEnvironment();
        log.info("=========================================");
        log.info("监控 Agent 启动成功!");
        log.info("=========================================");
        log.info("应用名称: {}", env.getProperty("spring.application.name"));
        log.info("服务端口: {}", env.getProperty("server.port"));
        log.info("LLM 模型: {}", env.getProperty("agentscope.llm.model-name"));
        log.info("LLM 服务地址: {}", env.getProperty("agentscope.llm.base-url"));
        log.info("=========================================");
        log.info("可用端点:");
        log.info("  - POST /api/process            : 流式处理请求 (SSE)");
        log.info("  - GET  /api/monitor/status     : 获取监控状态");
        log.info("  - GET  /api/health            : 健康检查");
        log.info("  - POST /api/session/reset/{caseId} : 重置指定会话");
        log.info("=========================================");
    }
}
