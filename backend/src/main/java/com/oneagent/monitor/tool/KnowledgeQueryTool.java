package com.oneagent.monitor.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.agentscope.core.rag.model.Document;
import io.agentscope.core.rag.model.RetrieveConfig;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolExecutionContext;
import io.agentscope.core.tool.ToolParam;
import com.oneagent.monitor.service.KnowledgeBaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 查询知识库的工具类
 * 注意：在 Agentic RAG 模式下，Agent 会自动使用 retrieve_knowledge 工具
 * 此工具保留用于向后兼容或特殊场景
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KnowledgeQueryTool {

    private final KnowledgeBaseService knowledgeBaseService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 查询知识库获取业务信息
     * 注意：在 Agentic RAG 模式下，建议使用 Agent 自动提供的 retrieve_knowledge 工具
     */
    @Tool(name = "query_knowledge", description = "查询胜算云知识库获取业务信息。用于回答用户关于平台功能、计费模式、服务条款等问题。返回相关的知识库内容片段。注意：在 Agentic RAG 模式下，建议使用 retrieve_knowledge 工具。")
    public String queryKnowledge(
            ToolExecutionContext context,
            @ToolParam(name = "query", description = "查询知识库的具体问题或关键词") String query
    ) {
        log.info("Querying knowledge base: {}", query);

        try {
            // 使用 SimpleKnowledge 的检索功能
            if (knowledgeBaseService.getKnowledge() != null) {
                RetrieveConfig config = RetrieveConfig.builder()
                        .limit(3)
                        .scoreThreshold(0.3)
                        .build();

                // retrieve 返回 Mono<List<Document>>
                Mono<List<Document>> monoResults =
                        knowledgeBaseService.getKnowledge().retrieve(query, config);

                // 阻塞获取结果
                List<Document> documents = monoResults.block();

                String answer;
                if (documents == null || documents.isEmpty()) {
                    log.info("No results found for query: {}", query);
                    answer = "知识库中未找到相关信息";
                } else {
                    // 提取文档内容 - 使用 toString() 方法
                    StringBuilder sb = new StringBuilder();
                    for (Document doc : documents) {
                        sb.append(doc.toString()).append("\n\n");
                    }
                    answer = sb.toString().trim();
                    log.info("Knowledge query returned {} results", documents.size());
                }
                return objectMapper.writeValueAsString(answer);
            } else {
                return "知识库未初始化，请检查 Embedding 模型配置";
            }
        } catch (Exception e) {
            log.error("Error querying knowledge base", e);
            return "查询知识库时发生错误：" + e.getMessage();
        }
    }
}
