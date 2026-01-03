package com.oneagent.monitor.service;

import io.agentscope.core.rag.knowledge.SimpleKnowledge;
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
 * 使用 SimpleKnowledge 实现本地 RAG
 */
@Slf4j
@Service
public class KnowledgeBaseService {

    @Value("${monitor.knowledge.path:src/main/resources/knowledge}")
    private String knowledgePath;

    private SimpleKnowledge knowledge;
    private List<String> documents = new ArrayList<>();

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
            Path kbPath = Paths.get(knowledgePath).toAbsolutePath();
            log.info("Absolute knowledge base path: {}", kbPath);
            log.info("Path exists: {}", Files.exists(kbPath));

            if (!Files.exists(kbPath)) {
                log.warn("Knowledge base path does not exist: {}", kbPath);
                return;
            }

            // 收集所有文档内容
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

            if (documents.isEmpty()) {
                log.warn("No documents found in knowledge base path: {}", kbPath);
                return;
            }

            log.info("Found {} documents in knowledge base", documents.size());

            // SimpleKnowledge 将在 AgentConfig 中通过 EmbeddingModel 初始化
            log.info("Knowledge base documents loaded successfully");

        } catch (IOException e) {
            log.error("Failed to load knowledge base", e);
        }
    }

    /**
     * 获取知识库实例
     */
    public SimpleKnowledge getKnowledge() {
        return knowledge;
    }

    /**
     * 设置知识库实例
     */
    public void setKnowledge(SimpleKnowledge knowledge) {
        this.knowledge = knowledge;
    }

    /**
     * 获取文档列表
     */
    public List<String> getDocuments() {
        return documents;
    }
}