package com.oneagent.monitor.controller;

import com.oneagent.monitor.model.dto.InputCase;
import com.oneagent.monitor.model.dto.ResultCase;
import com.oneagent.monitor.model.entity.MonitorStatus;
import com.oneagent.monitor.service.ChatService;
import com.oneagent.monitor.service.MonitorService;
import com.oneagent.monitor.service.ResultService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 聊天交互的 REST API 控制器
 */
@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final MonitorService monitorService;
    private final ResultService resultService;

    /**
     * 单对话端点
     */
    @PostMapping("/chat")
    public ResponseEntity<Map<String, String>> chat(@RequestBody Map<String, String> request) {
        log.info("聊天请求: {}", request);

        String userQuery = request.get("query");
        if (userQuery == null || userQuery.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "查询参数 'query' 是必需的"
            ));
        }

        String reply = chatService.simpleChat(userQuery);
        return ResponseEntity.ok(Map.of("reply", reply));
    }

    /**
     * 处理包含完整上下文的单个用例
     */
    @PostMapping("/process")
    public ResponseEntity<ResultCase> processCase(@RequestBody InputCase inputCase) {
        log.info("处理用例: {}", inputCase.getCaseId());

        ResultCase result = chatService.processQuery(inputCase);
        return ResponseEntity.ok(result);
    }

    /**
     * 批量处理端点
     */
    @PostMapping("/process-batch")
    public ResponseEntity<Map<String, Object>> processBatch(
            @RequestParam(required = false) String inputFile,
            @RequestParam(required = false) String outputFile
    ) {
        log.info("批量处理请求: 输入={}, 输出={}", inputFile, outputFile);

        String inputPath = inputFile != null ? inputFile : "inputs/inputs.json";
        String outputPath = outputFile != null ? outputFile : "outputs/results.json";

        resultService.processBatch(inputPath, outputPath);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("inputFile", inputPath);
        response.put("outputFile", outputPath);

        return ResponseEntity.ok(response);
    }

    /**
     * 获取当前监控状态
     */
    @GetMapping("/monitor/status")
    public ResponseEntity<MonitorStatus> getMonitorStatus() {
        MonitorStatus status = monitorService.getCurrentStatus();
        return ResponseEntity.ok(status);
    }

    /**
     * 健康检查端点
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "Monitor Agent"
        ));
    }

    /**
     * 获取 API 信息
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> info() {
        Map<String, Object> info = new HashMap<>();
        info.put("name", "Monitor Agent");
        info.put("version", "1.0.0");
        info.put("description", "Intelligent Customer Service Monitor Agent with AgentScope Java");

        Map<String, String> endpoints = new HashMap<>();
        endpoints.put("chat", "POST /api/chat");
        endpoints.put("process", "POST /api/process");
        endpoints.put("process-batch", "POST /api/process-batch");
        endpoints.put("monitor-status", "GET /api/monitor/status");
        endpoints.put("health", "GET /api/health");
        info.put("endpoints", endpoints);

        return ResponseEntity.ok(info);
    }

    /**
     * 错误处理器
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleError(Exception e) {
        log.error("请求处理错误", e);
        return ResponseEntity.internalServerError().body(Map.of(
                "error", "发生错误: " + e.getMessage()
        ));
    }
}
