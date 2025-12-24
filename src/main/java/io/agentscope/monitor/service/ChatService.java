package io.agentscope.monitor.service;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.monitor.model.dto.ActionTriggered;
import io.agentscope.monitor.model.dto.InputCase;
import io.agentscope.monitor.model.dto.MonitorLog;
import io.agentscope.monitor.model.dto.ResultCase;
import io.agentscope.monitor.tool.ApifoxApiTool;
import io.agentscope.monitor.tool.FeishuWebhookTool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Service for handling chat interactions
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final MonitorService monitorService;
    private final ReActAgent customerServiceAgent;
    private final FeishuWebhookTool feishuWebhookTool;
    private final ApifoxApiTool apifoxApiTool;

    /**
     * Process a single query case
     */
    public ResultCase processQuery(InputCase inputCase) {
        log.info("Processing case {}: query={}, apiStatus={}",
                inputCase.getCaseId(), inputCase.getUserQuery(), inputCase.getApiStatus());

        // Update monitor service with current case data
        monitorService.updateStatus(
                inputCase.getApiStatus(),
                inputCase.getApiResponseTime(),
                inputCase.getMonitorLog()
        );

        // Check if alert is needed
        ActionTriggered actions = null;
        if (monitorService.needsAlert(inputCase.getApiStatus())) {
            actions = handleApiAlert(inputCase);
        }

        // Build agent message with context
        String contextualQuery = buildContextualQuery(inputCase);

        // Call agent to get reply
        String reply = callAgent(contextualQuery);

        log.info("Case {} processed. Alert triggered: {}", inputCase.getCaseId(), actions != null);

        return actions != null
                ? ResultCase.withActions(inputCase.getCaseId(), reply, actions)
                : ResultCase.withReply(inputCase.getCaseId(), reply);
    }

    /**
     * Build query with context for the agent
     */
    private String buildContextualQuery(InputCase inputCase) {
        StringBuilder context = new StringBuilder();

        // Add monitoring context
        if (!"200 OK".equalsIgnoreCase(inputCase.getApiStatus())) {
            context.append(String.format(
                    "[系统状态提醒: 当前API状态异常 - %s, 响应时间: %s]\n\n",
                    inputCase.getApiStatus(),
                    inputCase.getApiResponseTime()
            ));
        }

        if (inputCase.getMonitorLog() != null && !inputCase.getMonitorLog().isEmpty()) {
            context.append("[最近的监控日志:\n");
            for (MonitorLog log : inputCase.getMonitorLog()) {
                context.append(String.format("  - %s: %s (%s)\n",
                        log.getTimestamp(), log.getStatus(), log.getMsg()));
            }
            context.append("]\n\n");
        }

        context.append("用户问题: ").append(inputCase.getUserQuery());

        return context.toString();
    }

    /**
     * Handle API alert by sending notifications
     */
    private ActionTriggered handleApiAlert(InputCase inputCase) {
        log.warn("API alert triggered for case {}: status={}, time={}",
                inputCase.getCaseId(), inputCase.getApiResponseTime());

        ActionTriggered.ActionTriggeredBuilder actions = ActionTriggered.builder();

        // Get latest error info from monitor log
        String errorMsg = "N/A";
        String errorTime = inputCase.getApiResponseTime();
        if (inputCase.getMonitorLog() != null && !inputCase.getMonitorLog().isEmpty()) {
            MonitorLog latest = inputCase.getMonitorLog().get(0);
            errorMsg = latest.getMsg();
            errorTime = latest.getTimestamp();
        }

        // Send Feishu alert
        String feishuResult = feishuWebhookTool.sendFeishuAlert(
                errorTime,
                inputCase.getApiStatus(),
                inputCase.getApiResponseTime()
        );
        actions.feishuWebhook(feishuResult);

        // Create Apifox document
        String docId = apifoxApiTool.createApifoxDocument(
                errorTime,
                inputCase.getApiStatus(),
                errorMsg,
                inputCase.getApiResponseTime()
        );
        actions.apifoxDocId(docId);

        log.info("Alert actions completed: feishu={}, docId={}", feishuResult, docId);

        return actions.build();
    }

    /**
     * Call the agent to get a reply
     */
    private String callAgent(String query) {
        try {
            Msg message = Msg.builder()
                    .name("user")
                    .role(MsgRole.USER)
                    .textContent(query)
                    .build();

            // Use Reactor's Mono properly
            Msg response = Mono.from(customerServiceAgent.call(message))
                    .block();

            if (response != null) {
                String reply = response.getTextContent();
                log.debug("Agent reply: {}", reply);
                return reply;
            }

            return "抱歉，我暂时无法回答这个问题。请稍后再试。";

        } catch (Exception e) {
            log.error("Error calling agent", e);
            return "抱歉，处理您的请求时发生了错误：" + e.getMessage();
        }
    }

    /**
     * Simple chat without case context (for direct API calls)
     */
    public String simpleChat(String userQuery) {
        log.info("Simple chat request: {}", userQuery);

        try {
            Msg message = Msg.builder()
                    .name("user")
                    .role(MsgRole.USER)
                    .textContent(userQuery)
                    .build();

            Msg response = Mono.from(customerServiceAgent.call(message))
                    .block();

            return response != null ? response.getTextContent() : "未能获取回复";

        } catch (Exception e) {
            log.error("Error in simple chat", e);
            return "聊天时发生错误：" + e.getMessage();
        }
    }
}
