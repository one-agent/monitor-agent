package com.oneagent.monitor.service;

import io.agentscope.core.rag.knowledge.SimpleKnowledge;
import io.agentscope.core.rag.store.InMemoryStore;
import lombok.extern.slf4j.Slf4j;
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

    private final String knowledgePath;
    private SimpleKnowledge knowledge;

    public KnowledgeBaseService() {
        this("knowledge");
    }
    public KnowledgeBaseService(String knowledgePath) {
        this.knowledgePath = knowledgePath;
    }

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

            Path kbPath = Paths.get(knowledgePath);
            if (!Files.exists(kbPath)) {
                log.warn("Knowledge base path does not exist: {}", kbPath);
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

            // 使用内存存储初始化 SimpleKnowledge
            // 注意：这是一个没有实际嵌入的简化方法
            // 在生产环境中，你需要配置一个嵌入模型
            this.knowledge = SimpleKnowledge.builder()
                    .embeddingStore(InMemoryStore.builder().build())
                    .build();

            log.info("Knowledge base loaded with {} documents", documents.size());

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
