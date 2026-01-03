package com.oneagent.monitor.service;

import io.agentscope.core.rag.knowledge.SimpleKnowledge;
import io.agentscope.core.rag.store.InMemoryStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * 知识库管理服务类
 */
@Slf4j
@Service
public class KnowledgeBaseService {

    @Value("${monitor.knowledge.path:src/main/resources/knowledge}")
    private String knowledgePath;

    private SimpleKnowledge knowledge;

    @PostConstruct
    public void init() {
        loadKnowledgeBase();
    }

    /**
     * 从文件加载知识库
     */
    public void loadKnowledgeBase() {
        log.info("Loading knowledge base from: {}", knowledgePath);

        try {
            // 注意：在实际实现中，这会使用 AgentScope 的 Knowledge 类
            // 对于本项目，我们将使用简单的内存方式
            // 因为嵌入模型可能需要外部 API 密钥

            Path kbPath = Paths.get(knowledgePath).toAbsolutePath();
            log.info("Absolute knowledge base path: {}", kbPath);
            log.info("Path exists: {}", Files.exists(kbPath));

            if (!Files.exists(kbPath)) {
                log.warn("Knowledge base path does not exist: {}", kbPath);
                log.warn("Current working directory: {}", System.getProperty("user.dir"));
                return;
            }

            List<String> documents = new ArrayList<>();

            Files.walk(kbPath)
                    .filter(p -> p.toString().endsWith(".md") || p.toString().endsWith(".txt"))
                    .forEach(file -> {
                        try {
                            String content = Files.readString(file);
                            documents.add(content);
                            log.info("Loaded knowledge file: {}", file.getFileName());
                        } catch (IOException e) {
                            log.error("Failed to read file: {}", file, e);
                        }
                    });

            // 注意：SimpleKnowledge 需要配置 embedding model
            // 这里暂时不初始化，使用简单文本搜索作为回退方案
            // 在生产环境中，你需要配置一个嵌入模型
            this.knowledge = null;

            log.info("Knowledge base loaded with {} documents (using simple text search)", documents.size());

        } catch (IOException e) {
            log.error("Failed to load knowledge base", e);
        }
    }

    /**
     * 搜索知识库
     */
    public List<String> search(String query) {
        log.debug("Searching knowledge base for: {}", query);

        List<String> results = new ArrayList<>();

        if (knowledge == null) {
            // 回退到简单文本搜索
            results.addAll(simpleTextSearch(query));
        } else {
            // 在完整实现中会使用 RAG
            // 目前使用简单搜索
            results.addAll(simpleTextSearch(query));
        }

        return results;
    }

    /**
     * 简单基于文本的搜索（当未配置嵌入模型时使用）
     */
    private List<String> simpleTextSearch(String query) {
        List<String> results = new ArrayList<>();

        try {
            Path kbPath = Paths.get(knowledgePath);
            if (!Files.exists(kbPath)) {
                return results;
            }

            Files.walk(kbPath)
                    .filter(p -> p.toString().endsWith(".md") || p.toString().endsWith(".txt"))
                    .forEach(file -> {
                        try {
                            String content = Files.readString(file);

                            // 简单关键词匹配
                            String[] keywords = query.toLowerCase().split("\\s+");
                            for (String keyword : keywords) {
                                if (content.toLowerCase().contains(keyword)) {
                                    results.add(content);
                                    break;
                                }
                            }
                        } catch (IOException e) {
                            log.error("Failed to search in file: {}", file, e);
                        }
                    });

        } catch (IOException e) {
            log.error("Failed to search knowledge base", e);
        }

        return results;
    }

    /**
     * 获取知识库实例
     */
    public SimpleKnowledge getKnowledge() {
        return knowledge;
    }
}
