package com.oneagent.monitor.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oneagent.monitor.MonitorAgentApplication;
import com.oneagent.monitor.model.dto.ActionTriggered;
import com.oneagent.monitor.model.dto.InputCase;
import com.oneagent.monitor.model.dto.MonitorLog;
import com.oneagent.monitor.model.dto.ResultCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ResultService 集成测试
 * 使用真实的 Spring Boot 环境，不使用 mock
 * 测试 ResultService 中的 processBatch 方法
 */
@SpringBootTest(classes = MonitorAgentApplication.class)
@ActiveProfiles("test")
class ResultServiceTest {

    @Autowired
    private ResultService resultService;

    private ObjectMapper objectMapper;

    private Path inputFilePath;
    private Path outputFilePath;

    @BeforeEach
    void setUp() throws IOException {
        objectMapper = new ObjectMapper();

        // 使用项目中的真实目录
        inputFilePath = java.nio.file.Paths.get("inputs", "inputs.json");
        outputFilePath = java.nio.file.Paths.get("outputs", "results.json");

        // 确保目录存在
        if (!Files.exists(inputFilePath.getParent())) {
            Files.createDirectories(inputFilePath.getParent());
        }
        if (!Files.exists(outputFilePath.getParent())) {
            Files.createDirectories(outputFilePath.getParent());
        }
    }

    @Test
    void testProcessBatch_WithMultipleCases_ShouldProcessAll() throws IOException {
        // 准备多个测试用例
        int caseCount = 5;
        List<InputCase> inputCases = createRealInputCases(caseCount);
        String inputJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(inputCases);
        Files.writeString(inputFilePath, inputJson);

        // 执行测试
        System.out.println("测试批量处理 " + caseCount + " 个用例...");
        long startTime = System.currentTimeMillis();
        resultService.processBatch(inputFilePath.toString(), outputFilePath.toString());
        long duration = System.currentTimeMillis() - startTime;

        // 验证
        String outputJson = Files.readString(outputFilePath);
        List<ResultCase> results = objectMapper.readValue(outputJson,
                new TypeReference<List<ResultCase>>() {});

        assertEquals(caseCount, results.size(), "应该生成 " + caseCount + " 个结果");

        System.out.println("测试通过: 成功处理 " + results.size() + " 个用例，耗时 " + duration + "ms");
    }

    @Test
    void testProcessBatch_RealWorldScenario() throws IOException {
        // 真实场景测试：模拟监控告警查询
        List<InputCase> inputCases = new ArrayList<>();

        // 用例 1: 正常状态查询
        InputCase case1 = new InputCase();
        case1.setCaseId("case-001");
        case1.setUserQuery("系统当前状态如何？");
        case1.setApiStatus("200 OK");
        case1.setApiResponseTime("120ms");
        inputCases.add(case1);

        // 用例 2: 慢响应查询
        InputCase case2 = new InputCase();
        case2.setCaseId("case-002");
        case2.setUserQuery("API 响应时间超过 2 秒，是否正常？");
        case2.setApiStatus("200 OK");
        case2.setApiResponseTime("2500ms");

        MonitorLog log2 = new MonitorLog();
        log2.setTimestamp("2024-01-06 11:15:30");
        log2.setStatus("slow");
        log2.setMsg("Slow response detected");
        case2.setMonitorLog(List.of(log2));
        inputCases.add(case2);

        // 用例 3: 错误查询
        InputCase case3 = new InputCase();
        case3.setCaseId("case-003");
        case3.setUserQuery("遇到 500 错误，应该如何排查？");
        case3.setApiStatus("500 Internal Server Error");
        case3.setApiResponseTime("500ms");

        MonitorLog log3 = new MonitorLog();
        log3.setTimestamp("2024-01-06 11:20:45");
        log3.setStatus("error");
        log3.setMsg("Internal server error in user service");
        case3.setMonitorLog(List.of(log3));
        inputCases.add(case3);

        String inputJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(inputCases);
        Files.writeString(inputFilePath, inputJson);

        // 执行测试
        System.out.println("\n===== 真实场景测试开始 =====");
        System.out.println("包含 3 个不同场景的用例:");
        System.out.println("  1. 正常状态查询");
        System.out.println("  2. 慢响应查询");
        System.out.println("  3. 错误查询");

        long startTime = System.currentTimeMillis();
        resultService.processBatch(inputFilePath.toString(), outputFilePath.toString());
        long duration = System.currentTimeMillis() - startTime;

        // 验证结果
        String outputJson = Files.readString(outputFilePath);
        List<ResultCase> results = objectMapper.readValue(outputJson,
                new TypeReference<List<ResultCase>>() {});

        assertEquals(3, results.size(), "应该生成 3 个结果");

        // 打印详细结果
        System.out.println("\n===== 处理结果 =====");
        System.out.println("总耗时: " + duration + "ms");
        for (int i = 0; i < results.size(); i++) {
            ResultCase result = results.get(i);
            System.out.println("\n用例 " + (i + 1) + ":");
            System.out.println("  ID: " + result.getCaseId());
            System.out.println("  回复: " +
                    (result.getReply() != null
                            ? (result.getReply().length() > 100
                                ? result.getReply().substring(0, 100) + "..."
                                : result.getReply())
                            : "null"));

            ActionTriggered action = result.getActionTriggered();
            if (action != null) {
                System.out.println("  告警触发: 是");
                System.out.println("    飞书: " + action.getFeishuWebhook());
                System.out.println("    Apifox: " + action.getApifoxDocId());
            } else {
                System.out.println("  告警触发: 否");
            }
        }

        System.out.println("\n===== 真实场景测试通过 =====");
    }

    // ========== 辅助方法 ==========

    /**
     * 创建真实的输入用例列表
     */
    private List<InputCase> createRealInputCases(int count) {
        List<InputCase> allCases = new ArrayList<>();

        // C001
        InputCase c1 = new InputCase();
        c1.setCaseId("C001");
        c1.setUserQuery("你们平台的计费模式是怎样的？");
        c1.setApiStatus("200 OK");
        c1.setApiResponseTime("120ms");
        c1.setMonitorLog(new ArrayList<>());
        allCases.add(c1);

        // C002
        InputCase c2 = new InputCase();
        c2.setCaseId("C002");
        c2.setUserQuery("基础版详细介绍？");
        c2.setApiStatus("500 Internal Server Error");
        c2.setApiResponseTime("Timeout");
        MonitorLog log2 = new MonitorLog();
        log2.setTimestamp("10:00:01");
        log2.setStatus("Error");
        log2.setMsg("Connection Refused");
        c2.setMonitorLog(List.of(log2));
        allCases.add(c2);

        // C005
        InputCase c5 = new InputCase();
        c5.setCaseId("C005");
        c5.setUserQuery("刚才出现了500错误，现在恢复了吗？");
        c5.setApiStatus("503 Service Unavailable");
        c5.setApiResponseTime("Timeout");
        List<MonitorLog> logs5 = new ArrayList<>();
        MonitorLog log5_1 = new MonitorLog();
        log5_1.setTimestamp("14:30:25");
        log5_1.setStatus("Error");
        log5_1.setMsg("Service Unavailable");
        logs5.add(log5_1);
        MonitorLog log5_2 = new MonitorLog();
        log5_2.setTimestamp("15:31:10");
        log5_2.setStatus("Error");
        log5_2.setMsg("Gateway Timeout");
        logs5.add(log5_2);
        c5.setMonitorLog(logs5);
        allCases.add(c5);

        // C003
        InputCase c3 = new InputCase();
        c3.setCaseId("C003");
        c3.setUserQuery("现在恢复了吗？");
        c3.setApiStatus("200 OK");
        c3.setApiResponseTime("95ms");
        c3.setMonitorLog(new ArrayList<>());
        allCases.add(c3);

        // C004
        InputCase c4 = new InputCase();
        c4.setCaseId("C004");
        c4.setUserQuery("你们的客服电话是多少？");
        c4.setApiStatus("200 OK");
        c4.setApiResponseTime("88ms");
        c4.setMonitorLog(new ArrayList<>());
        allCases.add(c4);


        // 如果请求的数量超过预定义数量，循环重复
        List<InputCase> result = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            result.add(allCases.get(i % allCases.size()));
        }

        return result;
    }
}
