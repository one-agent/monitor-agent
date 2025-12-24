package io.agentscope.monitor.controller;

import io.agentscope.monitor.model.dto.InputCase;
import io.agentscope.monitor.model.dto.ResultCase;
import io.agentscope.monitor.model.entity.MonitorStatus;
import io.agentscope.monitor.service.ChatService;
import io.agentscope.monitor.service.MonitorService;
import io.agentscope.monitor.service.ResultService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST API controller for chat interactions
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
     * Single chat endpoint
     */
    @PostMapping("/chat")
    public ResponseEntity<Map<String, String>> chat(@RequestBody Map<String, String> request) {
        log.info("Chat request: {}", request);

        String userQuery = request.get("query");
        if (userQuery == null || userQuery.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Query parameter 'query' is required"
            ));
        }

        String reply = chatService.simpleChat(userQuery);
        return ResponseEntity.ok(Map.of("reply", reply));
    }

    /**
     * Process a single case with full context
     */
    @PostMapping("/process")
    public ResponseEntity<ResultCase> processCase(@RequestBody InputCase inputCase) {
        log.info("Processing case: {}", inputCase.getCaseId());

        ResultCase result = chatService.processQuery(inputCase);
        return ResponseEntity.ok(result);
    }

    /**
     * Batch process endpoint
     */
    @PostMapping("/process-batch")
    public ResponseEntity<Map<String, Object>> processBatch(
            @RequestParam(required = false) String inputFile,
            @RequestParam(required = false) String outputFile
    ) {
        log.info("Batch processing request: input={}, output={}", inputFile, outputFile);

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
     * Get current monitor status
     */
    @GetMapping("/monitor/status")
    public ResponseEntity<MonitorStatus> getMonitorStatus() {
        MonitorStatus status = monitorService.getCurrentStatus();
        return ResponseEntity.ok(status);
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "Monitor Agent"
        ));
    }

    /**
     * Get API info
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
     * Error handler
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleError(Exception e) {
        log.error("Request processing error", e);
        return ResponseEntity.internalServerError().body(Map.of(
                "error", "An error occurred: " + e.getMessage()
        ));
    }
}
