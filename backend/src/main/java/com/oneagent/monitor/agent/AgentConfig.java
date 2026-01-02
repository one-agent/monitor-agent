package com.oneagent.monitor.agent;

import com.oneagent.monitor.config.AgentScopeProperties;
import com.oneagent.monitor.model.config.MonitorProperties;
import com.oneagent.monitor.tool.ApifoxApiTool;
import com.oneagent.monitor.tool.FeishuWebhookTool;
import com.oneagent.monitor.tool.KnowledgeQueryTool;
import com.oneagent.monitor.tool.MonitorCheckTool;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.formatter.openai.OpenAIChatFormatter;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.OpenAIChatModel;
import io.agentscope.core.studio.StudioManager;
import io.agentscope.core.studio.StudioMessageHook;
import io.agentscope.core.tool.Toolkit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

/**
 * Agent 配置类
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(AgentScopeProperties.class)
@DependsOn("studioInitializer")  // 确保 Studio 先初始化
public class AgentConfig {

    private final AgentScopeProperties agentScopeProperties;
    private final MonitorProperties monitorProperties;
    private final FeishuWebhookTool feishuWebhookTool;
    private final ApifoxApiTool apifoxApiTool;
    private final MonitorCheckTool monitorCheckTool;
    private final KnowledgeQueryTool knowledgeQueryTool;

    /**
     * 创建兼容 OpenAI 的聊天模型
     */
    @Bean
    public OpenAIChatModel chatModel() {
        AgentScopeProperties.LlmConfig llmConfig = agentScopeProperties.getLlm();

        log.info("Creating chat model: url={}, model={}",
                llmConfig.getBaseUrl(),
                llmConfig.getModelName());

        return OpenAIChatModel.builder()
                .baseUrl(llmConfig.getBaseUrl())
                .apiKey(llmConfig.getApiKey())
                .modelName(llmConfig.getModelName())
                .stream(llmConfig.getStream())
                .formatter(new OpenAIChatFormatter())
                .defaultOptions(GenerateOptions.builder()
                        .temperature(llmConfig.getTemperature())
                        .maxTokens(llmConfig.getMaxTokens())
                        .build())
                .build();
    }

    /**
     * 创建包含所有注册工具的工具包
     */
    @Bean
    public Toolkit toolkit() {
        log.info("Creating toolkit and registering tools");

        Toolkit toolkit = new Toolkit();

        // 注册工具实例
        toolkit.registerTool(feishuWebhookTool);
        toolkit.registerTool(apifoxApiTool);
        toolkit.registerTool(monitorCheckTool);
        toolkit.registerTool(knowledgeQueryTool);

        log.debug("Registered tools: {}", toolkit.getToolNames());

        return toolkit;
    }

    /**
     * 创建包含所有配置的 ReActAgent
     */
    @Bean
    public ReActAgent customerServiceAgent(OpenAIChatModel chatModel, Toolkit toolkit) {
        log.info("Creating CustomerServiceAgent");

        String systemPrompt = buildSystemPrompt();

        ReActAgent.Builder builder = ReActAgent.builder()
                .name("CustomerServiceAgent")
                .model(chatModel)
                .sysPrompt(systemPrompt)
                .memory(new InMemoryMemory())
                .toolkit(toolkit)
                .maxIters(10);

        // 添加 Studio 集成 Hook
        if (monitorProperties.getStudio().isEnabled()) {
            log.info("Studio is enabled, adding StudioMessageHook to agent");
            try {
                // StudioMessageHook - 用于记录消息到 Studio UI
                builder.hook(new StudioMessageHook(StudioManager.getClient()));
            } catch (Exception e) {
                log.warn("Failed to add StudioMessageHook (Studio may not be initialized): {}", e.getMessage());
            }
        }

        return builder.build();
    }

    /**
     * 构建 Agent 的系统提示词
     */
    private String buildSystemPrompt() {
        return """
                你是胜算云平台的智能客服监控 Agent。

                【核心职责】
                1. 基于知识库回答业务问题，严禁产生幻觉
                2. 监听系统状态，及时感知 API 异常
                3. 当系统异常时，触发飞书告警并记录故障文档
                4. 回答稳定性问题时，必须引用真实的监控日志数据

                【内容规范】
                - 智能问答：必须基于提供的"胜算云知识库"回答业务问题
                - 如果知识库中没有答案，回答"知识库中未找到相关信息"
                - 当被问及稳定性时，严禁直接回答"很稳定"，必须读取监控日志
                - 如果有报错记录，需诚实告知用户最近的异常情况
                - 回答时保持礼貌、专业、简洁

                【可用工具】
                - check_monitor_status: 检查当前系统监控状态
                - get_monitor_logs: 获取最近的监控日志记录
                - is_api_healthy: 检查 API 是否健康
                - query_knowledge: 查询胜算云知识库获取业务信息
                - search_by_keyword: 在知识库中搜索关键词
                - send_feishu_alert: 发送飞书告警（系统会自动调用，无需你主动发起）
                - create_apifox_document: 创建 Apifox 故障记录文档（系统会自动调用，无需你主动发起）

                【工作流程】
                1. 首先判断用户问题类型（业务咨询 vs 稳定性询问）
                2. 如果是业务咨询，使用 query_knowledge 工具查询知识库
                3. 如果是稳定性询问，使用 get_monitor_logs 获取真实数据
                4. 根据查询结果，组织准确的回答
                5. 注意：系统会自动处理 API 告警，你不需要主动调用告警工具

                【回答风格】
                - 使用友好、专业的语气
                - 回答简洁明了，避免冗长
                - 如果不确定，诚实地告知
                - 严禁编造信息
                """;
    }
}
