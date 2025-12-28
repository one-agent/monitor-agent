package com.oneagent.monitor.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oneagent.monitor.model.dto.InputCase;
import com.oneagent.monitor.model.dto.ResultCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

/**
 * 处理批量结果的服务类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResultService {

    private final ChatService chatService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Process batch of input cases and generate output
     */
    public void processBatch(String inputPath, String outputPath) {
        log.info("Processing batch: input={}, output={}", inputPath, outputPath);

        try {
            // 读取输入用例
            List<InputCase> inputCases = readInputFile(inputPath);
            log.info("已加载 {} 个用例，来自输入文件", inputCases.size());

            // 处理每个用例
            List<ResultCase> results = new ArrayList<>();
            for (InputCase inputCase : inputCases) {
                ResultCase result = chatService.processQuery(inputCase);
                results.add(result);
            }

            // 写入结果
            writeOutputFile(outputPath, results);
            log.info("批量处理完成。结果已写入 {}", outputPath);

        } catch (Exception e) {
            log.error("处理批量出错", e);
            throw new RuntimeException("批量处理失败: " + e.getMessage(), e);
        }
    }

    /**
     * Read input cases from JSON file
     */
    private List<InputCase> readInputFile(String inputPath) throws IOException {
        Path path = Paths.get(inputPath);

        if (!Files.exists(path)) {
            log.warn("Input file does not exist: {}. Creating empty list.", inputPath);
            return new ArrayList<>();
        }

        String content = Files.readString(path);
        return objectMapper.readValue(content, new TypeReference<List<InputCase>>() {});
    }

    /**
     * Write results to JSON file
     */
    private void writeOutputFile(String outputPath, List<ResultCase> results) throws IOException {
        Path path = Paths.get(outputPath);

        // Create parent directories if they don't exist
        Files.createDirectories(path.getParent());

        // Write with pretty formatting
        String json = objectMapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(results);

        Files.writeString(path, json, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        log.debug("Results written:\n{}", json);
    }

    /**
     * Get processing statistics
     */
    public BatchStats getBatchStats(List<ResultCase> results) {
        int alertCount = 0;
        int successCount = 0;

        for (ResultCase result : results) {
            if (result.getActionTriggered() != null) {
                alertCount++;
            }
            if (result.getReply() != null && !result.getReply().isEmpty()) {
                successCount++;
            }
        }

        return new BatchStats(
                results.size(),
                successCount,
                alertCount
        );
    }

    /**
     * Batch statistics record
     */
    public record BatchStats(
            int totalCases,
            int successfulReplies,
            int alertsTriggered
    ) {
        @Override
        public String toString() {
            return String.format(
                    "Total: %d, Success: %d, Alerts: %d",
                    totalCases, successfulReplies, alertsTriggered
            );
        }
    }
}
