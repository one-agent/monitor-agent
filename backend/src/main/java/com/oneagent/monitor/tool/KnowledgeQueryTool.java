package com.oneagent.monitor.tool;

import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolExecutionContext;
import com.oneagent.monitor.service.KnowledgeBaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 查询知识库的工具类
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KnowledgeQueryTool {

    private final KnowledgeBaseService knowledgeBaseService;

    /**
     * 查询知识库获取业务信息
     */
    @Tool(description = "查询胜算云知识库获取业务信息。用于回答用户关于平台功能、计费模式、服务条款等问题。返回相关的知识库内容片段。")
    public String queryKnowledge(
            ToolExecutionContext context,
            String query
    ) {
        log.info("Querying knowledge base: {}", query);

        try {
            List<String> results = knowledgeBaseService.search(query);

            if (results.isEmpty()) {
                log.info("No results found for query: {}", query);
                return "知识库中未找到相关信息";
            }

            String answer = results.stream()
                    .collect(Collectors.joining("\n\n"));
            log.info("Knowledge query returned {} results", results.size());
            return answer;
        } catch (Exception e) {
            log.error("Error querying knowledge base", e);
            return "查询知识库时发生错误：" + e.getMessage();
        }
    }

    /**
     * 在知识库中搜索关键词
     */
    @Tool(description = "在知识库中搜索关键词。返回包含该关键词的所有知识条目。")
    public String searchByKeyword(String keyword) {
        log.info("Searching keyword: {}", keyword);

        try {
            List<String> results = knowledgeBaseService.search(keyword);

            if (results.isEmpty()) {
                return "未找到与关键词 \"" + keyword + "\" 相关的信息";
            }

            return results.stream()
                    .collect(Collectors.joining("\n\n"));
        } catch (Exception e) {
            log.error("Error searching by keyword", e);
            return "搜索时发生错误：" + e.getMessage();
        }
    }
}
