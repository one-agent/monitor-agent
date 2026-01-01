package com.oneagent.monitor.service;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import com.oneagent.monitor.model.dto.ActionTriggered;
import com.oneagent.monitor.model.dto.InputCase;
import com.oneagent.monitor.model.dto.MonitorLog;
import com.oneagent.monitor.model.dto.ResultCase;
import com.oneagent.monitor.tool.ApifoxApiTool;
import com.oneagent.monitor.tool.FeishuWebhookTool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * 处理聊天交互的服务类
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
     * 处理单个查询用例
     */
    public ResultCase processQuery(InputCase inputCase) {
        log.info("处理用例 {}: query={}, apiStatus={}",
                inputCase.getCaseId(), inputCase.getUserQuery(), inputCase.getApiStatus());

        // 更新监控服务的当前用例数据
        monitorService.updateStatus(
                inputCase.getApiStatus(),
                inputCase.getApiResponseTime(),
                inputCase.getMonitorLog()
        );

        // 检查是否需要告警
        ActionTriggered actions = null;
        if (monitorService.needsAlert(inputCase.getApiStatus())) {
            actions = handleApiAlert(inputCase);
        }

        // 构建带有上下文的 Agent 消息
        String contextualQuery = buildContextualQuery(inputCase);

        // 调用 Agent 获取回复
        String reply = callAgent(contextualQuery);

        log.info("用例 {} 处理完成. 告警触发: {}", inputCase.getCaseId(), actions != null);

        return actions != null
                ? ResultCase.withActions(inputCase.getCaseId(), reply, actions)
                : ResultCase.withReply(inputCase.getCaseId(), reply);
    }

    /**
     * 构建带有上下文的查询供 Agent 使用
     */
    public String buildContextualQuery(InputCase inputCase) {
        StringBuilder context = new StringBuilder();

        // 添加监控上下文
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
     * 调用 Agent 获取回复
     */
    private String callAgent(String query) {
        try {
            Msg message = Msg.builder()
                    .name("user")
                    .role(MsgRole.USER)
                    .textContent(query)
                    .build();

            // 直接使用 Agent 的 block() 方法，让 StudioMessageHook 能正常追踪
            Msg response = customerServiceAgent.call(message).block();

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

            Msg response = Mono.from(customerServiceAgent.call(message))
                    .block();

            return response != null ? response.getTextContent() : "未能获取回复";

        } catch (Exception e) {
            log.error("简单聊天出错", e);
            return "聊天时发生错误：" + e.getMessage();
        }
    }
}
