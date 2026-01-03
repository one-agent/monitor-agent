package com.oneagent.monitor.agent;

import com.oneagent.monitor.config.AgentScopeProperties;
import com.oneagent.monitor.hook.ToolMonitorHook;
import com.oneagent.monitor.model.config.MonitorProperties;
import com.oneagent.monitor.service.KnowledgeBaseService;
import com.oneagent.monitor.tool.ApifoxApiTool;
import com.oneagent.monitor.tool.FeishuWebhookTool;
import com.oneagent.monitor.tool.MonitorCheckTool;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.embedding.EmbeddingModel;
import io.agentscope.core.embedding.openai.OpenAITextEmbedding;
import io.agentscope.core.formatter.openai.OpenAIChatFormatter;
import io.agentscope.core.hook.Hook;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.OpenAIChatModel;
import io.agentscope.core.rag.RAGMode;
import io.agentscope.core.rag.knowledge.SimpleKnowledge;
import io.agentscope.core.rag.model.Document;
import io.agentscope.core.rag.model.RetrieveConfig;
import io.agentscope.core.rag.reader.ReaderInput;
import io.agentscope.core.rag.reader.SplitStrategy;
import io.agentscope.core.rag.reader.TextReader;
import io.agentscope.core.rag.store.InMemoryStore;
import io.agentscope.core.studio.StudioManager;
import io.agentscope.core.studio.StudioMessageHook;
import io.agentscope.core.tool.Toolkit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Scope;

import java.util.List;

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
    private final KnowledgeBaseService knowledgeBaseService;

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
     * 创建 OpenAI TextEmbedding 模型
     */
    @Bean
    public EmbeddingModel textEmbeddingModel() {
        AgentScopeProperties.EmbeddingConfig embeddingConfig = agentScopeProperties.getEmbedding();

        log.info("Creating text embedding model: url={}, model={}, enabled={}",
                embeddingConfig.getBaseUrl(),
                embeddingConfig.getModelName(),
                embeddingConfig.getEnabled());

        if (!embeddingConfig.getEnabled()) {
            log.warn("Embedding model is disabled in configuration");
            return null;
        }

        return OpenAITextEmbedding.builder()
                .baseUrl(embeddingConfig.getBaseUrl())
                .apiKey(embeddingConfig.getApiKey())
                .modelName(embeddingConfig.getModelName())
                .build();
    }

    /**
     * 创建包含所有注册工具的工具包
     */
    @Bean
    public Toolkit toolkit(OpenAIChatModel chatModel) {
        log.info("Creating toolkit and registering tools");

        Toolkit toolkit = new Toolkit();

        // 注册工具实例
        toolkit.registerTool(feishuWebhookTool);
        toolkit.registerTool(apifoxApiTool);
        toolkit.registerTool(monitorCheckTool);

        log.debug("Registered tools: {}", toolkit.getToolNames());

        return toolkit;
    }

    /**
     * 创建 hook
     */
    public List<Hook> hookList() {
        ToolMonitorHook hook = new ToolMonitorHook();
        return List.of(hook);
    }

    /**
     * 创建包含所有配置的 ReActAgent
     * 使用原型作用域，确保每个请求都有独立的 Agent 实例
     * 集成 RAG 功能（Agentic 模式）
     */
    @Bean
    @Scope("prototype")
    public ReActAgent customerServiceAgent(OpenAIChatModel chatModel, Toolkit toolkit, EmbeddingModel textEmbeddingModel) {
        log.info("Creating CustomerServiceAgent with RAG support");

        String systemPrompt = buildSystemPrompt();

        ReActAgent.Builder builder = ReActAgent.builder()
                .name("CustomerServiceAgent")
                .model(chatModel)
                .sysPrompt(systemPrompt)
                .memory(new InMemoryMemory())
                .toolkit(toolkit)
                .hooks(hookList())
                .maxIters(10);

        // 集成 RAG 功能（Agentic 模式）
        builderRAG(textEmbeddingModel, builder);

        // 添加 Studio 集成 Hook
        if (monitorProperties.getStudio().isEnabled()) {
            log.info("Studio is enabled, adding StudioMessageHook to agent");
            try {
                builder.hook(new StudioMessageHook(StudioManager.getClient()));
            } catch (Exception e) {
                log.warn("Failed to add StudioMessageHook (Studio may not be initialized): {}", e.getMessage());
            }
        }

        return builder.build();
    }

    private void builderRAG(EmbeddingModel textEmbeddingModel, ReActAgent.Builder builder) {
        if (textEmbeddingModel != null) {
            log.info("Enabling RAG in Agentic mode");

            try {
                // 创建向量存储 - 使用 InMemoryStore.builder()
                InMemoryStore vectorStore =
                        InMemoryStore.builder()
                                .dimensions(1536)  // text-embedding-3-small 的维度
                                .build();

                // 创建知识库 - 使用 SimpleKnowledge.builder()
                SimpleKnowledge knowledge =
                        SimpleKnowledge.builder()
                                .embeddingModel(textEmbeddingModel)
                                .embeddingStore(vectorStore)
                                .build();

                // 加载文档到知识库
                // 使用 TextReader 进行文档分块
                TextReader reader =
                        new TextReader(
                                512,  // chunk size
                                SplitStrategy.PARAGRAPH,
                                50    // chunk overlap
                        );

                for (String doc : knowledgeBaseService.getDocuments()) {
                    try {
                        ReaderInput input =
                                ReaderInput.fromString(doc);
                        List<Document> docs =
                                reader.read(input).block();
                        if (docs != null && !docs.isEmpty()) {
                            knowledge.addDocuments(docs).block();
                        }
                    } catch (Exception e) {
                        log.error("Error adding document to knowledge base: {}", e.getMessage(), e);
                    }
                }

                // 将知识库设置到服务中
                knowledgeBaseService.setKnowledge(knowledge);

                // 配置 RAG 为 Agentic 模式
                builder.knowledge(knowledge)
                        .ragMode(RAGMode.AGENTIC)
                        .retrieveConfig(
                                RetrieveConfig.builder()
                                        .limit(3)
                                        .scoreThreshold(0.3)
                                        .build()
                        );

                log.info("RAG enabled successfully with {} documents", knowledgeBaseService.getDocuments().size());
            } catch (Exception e) {
                log.error("Failed to initialize RAG: {}", e.getMessage(), e);
            }
        } else {
            log.warn("RAG not enabled: embeddingModel is null");
        }
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
                - retrieve_knowledge: 从知识库检索相关信息（RAG 模式自动提供）
                - check_monitor_status: 检查当前系统监控状态
                - get_monitor_logs: 获取最近的监控日志记录
                - is_api_healthy: 检查 API 是否健康
                - send_feishu_alert: 发送飞书告警（系统会自动调用，无需你主动发起）
                - create_apifox_document: 创建 Apifox 故障记录文档（系统会自动调用，无需你主动发起）

                【工作流程】
                1. 首先判断用户问题类型（业务咨询 vs 稳定性询问）
                2. 如果是业务咨询，使用 retrieve_knowledge 工具从知识库检索相关信息
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
