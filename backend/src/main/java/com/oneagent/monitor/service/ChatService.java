package com.oneagent.monitor.service;

import com.oneagent.monitor.session.SessionManager;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import com.oneagent.monitor.model.dto.ActionTriggered;
import com.oneagent.monitor.model.dto.AlertLevel;
import com.oneagent.monitor.model.dto.InputCase;
import com.oneagent.monitor.model.dto.MonitorLog;
import com.oneagent.monitor.model.dto.ResultCase;
import com.oneagent.monitor.tool.ApifoxApiTool;
import com.oneagent.monitor.tool.FeishuWebhookTool;
// import lombok.RequiredArgsConstructor; // Removed to manually define constructor
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * 处理聊天交互的服务类
 */
@Slf4j
@Service
public class ChatService {

    private final MonitorService monitorService;
    private final ObjectProvider<ReActAgent> customerServiceAgentProvider;
    private final SessionManager sessionManager;
    private final FeishuWebhookTool feishuWebhookTool;
    private final ApifoxApiTool apifoxApiTool;

    public ChatService(MonitorService monitorService,
                      ObjectProvider<ReActAgent> customerServiceAgentProvider,
                      SessionManager sessionManager,
                      FeishuWebhookTool feishuWebhookTool,
                      ApifoxApiTool apifoxApiTool) {
        this.monitorService = monitorService;
        this.customerServiceAgentProvider = customerServiceAgentProvider;
        this.sessionManager = sessionManager;
        this.feishuWebhookTool = feishuWebhookTool;
        this.apifoxApiTool = apifoxApiTool;
    }

    /**
     * 处理单个查询用例
     */
    public ResultCase processQuery(InputCase inputCase) {
        log.info("处理用例 {}: query={}, apiStatus={}",
                inputCase.getCaseId(), inputCase.getUserQuery(), inputCase.getApiStatus());

        // 获取或创建Agent实例，并加载会话
        ReActAgent agent = sessionManager.getOrCreateAgent(
                inputCase.getCaseId(),
                customerServiceAgentProvider.getObject()
        );

        // 构建带有上下文的 Agent 消息
        // Agent 可以根据上下文自主决定是否需要调用监控工具
        String contextualQuery = buildContextualQuery(inputCase);

        // 调用 Agent 获取回复
        String reply = callAgent(agent, contextualQuery);

        // 保存会话
        sessionManager.saveSession(inputCase.getCaseId());

        log.info("用例 {} 处理完成", inputCase.getCaseId());

        return ResultCase.withReply(inputCase.getCaseId(), reply);
    }

    /**
     * 构建带有上下文的查询供 Agent 使用
     */
    public String buildContextualQuery(InputCase inputCase) {
        StringBuilder context = new StringBuilder();

        // 添加监控上下文信息（仅供参考，不自动更新状态）
        if (inputCase.getApiStatus() != null && !"200 OK".equalsIgnoreCase(inputCase.getApiStatus())) {
            context.append(String.format(
                    "[系统状态提醒: 当前API状态异常 - %s, 响应时间: %s]\n",
                    inputCase.getApiStatus(),
                    inputCase.getApiResponseTime()
            ));
            
            // 如果有监控日志，也添加到上下文中
            if (inputCase.getMonitorLog() != null && !inputCase.getMonitorLog().isEmpty()) {
                context.append("[监控日志:\n");
                for (MonitorLog log : inputCase.getMonitorLog()) {
                    context.append(String.format("  - %s: %s - %s\n",
                            log.getTimestamp(), log.getStatus(), log.getMsg()));
                }
                context.append("]\n");
            }
            
            // 提示 Agent 可以使用监控工具
            context.append("提示：可以使用 check_monitor_status、get_monitor_logs 或 send_uptime_kuma_alert 工具获取更多信息或发送告警。\n\n");
        }

        context.append("用户问题: ").append(inputCase.getUserQuery());

        return context.toString();
    }

    /**
     * 通过发送通知处理 API 告警
     */
    public ActionTriggered handleApiAlert(InputCase inputCase) {
        log.warn("API 告警触发，用例 {}: status={}, time={}",
                inputCase.getCaseId(), inputCase.getApiStatus() ,inputCase.getApiResponseTime());

        ActionTriggered.ActionTriggeredBuilder actions = ActionTriggered.builder();

        // 从监控日志获取最新的错误信息
        String errorMsg = "N/A";
        String errorTime = inputCase.getApiResponseTime();
        if (inputCase.getMonitorLog() != null && !inputCase.getMonitorLog().isEmpty()) {
            MonitorLog latest = inputCase.getMonitorLog().get(0);
            errorMsg = latest.getMsg();
            errorTime = latest.getTimestamp();
        }

        // 发送飞书告警
        String feishuResult = feishuWebhookTool.sendFeishuAlert(
                errorTime,
                inputCase.getApiStatus(),
                inputCase.getApiResponseTime()
        );
        actions.feishuWebhook(feishuResult);

        // 创建 Apifox 文档
        String docId = apifoxApiTool.createApifoxDocument(
                errorTime,
                inputCase.getApiStatus(),
                errorMsg,
                inputCase.getApiResponseTime()
        );
        actions.apifoxDocId(docId);

        log.info("告警动作完成: feishu={}, docId={}", feishuResult, docId);

        return actions.build();
    }

    /**
     * 处理 Uptime Kuma 告警
     * 
     * @param inputCase 输入用例
     * @param webhookData Uptime Kuma Webhook 数据
     * @return 触发的动作
     */
    public ActionTriggered handleUptimeKumaAlert(
            InputCase inputCase,
            com.oneagent.monitor.model.dto.UptimeKumaWebhookDTO webhookData) {

        AlertLevel alertLevel = webhookData.getAlertLevel();
        log.warn("Uptime Kuma 告警触发: monitorId={}, monitorName={}, level={}, status={}",
                webhookData.getMonitorIdStr(),
                webhookData.getMonitorName(),
                alertLevel,
                webhookData.getHeartbeat() != null ? webhookData.getHeartbeat().getStatus() : "unknown");

        ActionTriggered.ActionTriggeredBuilder actions = ActionTriggered.builder();

        // 获取错误信息
        String errorMsg = webhookData.getErrorMessage();
        String errorTime = webhookData.getHeartbeatTime();
        String monitorName = webhookData.getMonitorName();
        String monitorUrl = webhookData.getMonitorUrl();
        String monitorType = webhookData.getMonitorType();

        // 发送飞书告警（带告警级别）
        String feishuResult = feishuWebhookTool.sendFeishuAlertWithLevel(
                errorTime,
                inputCase.getApiStatus(),
                inputCase.getApiResponseTime(),
                alertLevel,
                monitorName,
                monitorUrl,
                monitorType,
                errorMsg
        );
        actions.feishuWebhook(feishuResult);

        // 创建 Apifox 文档
        String docId = apifoxApiTool.createApifoxDocumentWithDetails(
                errorTime,
                inputCase.getApiStatus(),
                errorMsg,
                inputCase.getApiResponseTime(),
                alertLevel,
                monitorName,
                monitorUrl,
                monitorType
        );
        actions.apifoxDocId(docId);

        log.info("Uptime Kuma 告警动作完成: feishu={}, docId={}, level={}",
                feishuResult, docId, alertLevel);

        return actions.build();
    }

    /**
     * 处理 Uptime Kuma 恢复通知
     * 
     * @param inputCase 输入用例
     * @param webhookData Uptime Kuma Webhook 数据
     * @return 触发的动作
     */
    public ActionTriggered handleUptimeKumaRecovery(
            InputCase inputCase,
            com.oneagent.monitor.model.dto.UptimeKumaWebhookDTO webhookData) {

        log.info("Uptime Kuma 恢复通知: monitorId={}, monitorName={}",
                webhookData.getMonitorIdStr(),
                webhookData.getMonitorName());

        ActionTriggered.ActionTriggeredBuilder actions = ActionTriggered.builder();

        // 获取恢复信息
        String recoveryTime = webhookData.getHeartbeatTime();
        String monitorName = webhookData.getMonitorName();
        String monitorUrl = webhookData.getMonitorUrl();
        String monitorType = webhookData.getMonitorType();
        String responseTime = webhookData.getResponseTime() > 0
                ? webhookData.getResponseTime() + "ms"
                : "Unknown";

        // 发送飞书恢复通知
        String feishuResult = feishuWebhookTool.sendFeishuRecovery(
                recoveryTime,
                responseTime,
                monitorName,
                monitorUrl,
                monitorType
        );
        actions.feishuWebhook(feishuResult);

        // 创建 Apifox 恢复文档
        String docId = apifoxApiTool.createApifoxRecoveryDocument(
                recoveryTime,
                responseTime,
                monitorName,
                monitorUrl,
                monitorType
        );
        actions.apifoxDocId(docId);

        log.info("Uptime Kuma 恢复通知完成: feishu={}, docId={}", feishuResult, docId);

        return actions.build();
    }

    /**
     * 调用 Agent 获取回复
     */
    private String callAgent(ReActAgent agent, String query) {
        try {
            Msg message = Msg.builder()
                    .name("user")
                    .role(MsgRole.USER)
                    .textContent(query)
                    .build();

            // 直接使用 Agent 的 block() 方法，让 StudioMessageHook 能正常追踪
            Msg response = agent.call(message).block();

            if (response != null) {
                String reply = response.getTextContent();
                log.debug("Agent 回复: {}", reply);
                return reply;
            }

            return "抱歉，我暂时无法回答这个问题。请稍后再试。";

        } catch (Exception e) {
            log.error("调用 Agent 出错", e);
            return "抱歉，处理您的请求时发生了错误：" + e.getMessage();
        }
    }

    /**
     * 简单聊天，不包含用例上下文（用于直接 API 调用）
     */
    public String simpleChat(String userQuery) {
        log.info("简单聊天请求: {}", userQuery);

        try {
            Msg message = Msg.builder()
                    .name("user")
                    .role(MsgRole.USER)
                    .textContent(userQuery)
                    .build();

            ReActAgent agent = sessionManager.getOrCreateAgent(
                    "default_session",
                    customerServiceAgentProvider.getObject()
            );
            Msg response = Mono.from(agent.call(message))
                    .block();
            // 保存会话
            sessionManager.saveSession("default_session");

            return response != null ? response.getTextContent() : "未能获取回复";

        } catch (Exception e) {
            log.error("简单聊天出错", e);
            return "聊天时发生错误：" + e.getMessage();
        }
    }
}
