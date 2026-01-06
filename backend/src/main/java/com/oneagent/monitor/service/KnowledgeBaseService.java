package com.oneagent.monitor.service;

import io.agentscope.core.rag.knowledge.SimpleKnowledge;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 知识库管理服务类
 * 使用 SimpleKnowledge 实现本地 RAG
 */
@Slf4j
@Service
public class KnowledgeBaseService {

    /**
     * 知识库路径 pattern，支持动态读取 classpath 中的文件
     */
    private static final String KNOWLEDGE_PATH_PATTERN = "classpath*:knowledge/**/*.md";

    private final ResourceLoader resourceLoader;

    public KnowledgeBaseService(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    /**
     * -- GETTER --
     *  获取知识库实例
     * -- SETTER --
     *  设置知识库实例

     */
    @Setter
    @Getter
    private SimpleKnowledge knowledge;
    /**
     * -- GETTER --
     *  获取文档列表
     */
    @Getter
    private List<String> documents = new ArrayList<>();

    @PostConstruct
    public void init() {
        loadKnowledgeBase();
    }

    /**
     * 从 classpath 动态加载知识库
     */
    public void loadKnowledgeBase() {
        log.info("Loading knowledge base from classpath: {}", KNOWLEDGE_PATH_PATTERN);

        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(resourceLoader);
            Resource[] resources = resolver.getResources(KNOWLEDGE_PATH_PATTERN);

            log.info("Found {} knowledge files in classpath", resources.length);

            for (Resource resource : resources) {
                try {
                    String content = readResourceContent(resource);
                    documents.add(content);
                    log.info("Loaded knowledge file: {}", resource.getFilename());
                } catch (IOException e) {
                    log.error("Failed to read resource: {}", resource.getFilename(), e);
                }
            }

            if (documents.isEmpty()) {
                log.warn("No documents found in knowledge base path: {}", KNOWLEDGE_PATH_PATTERN);
                return;
            }

            log.info("Successfully loaded {} documents from knowledge base", documents.size());

        } catch (IOException e) {
            log.error("Failed to load knowledge base from classpath", e);
        }
    }

    /**
     * 读取 Resource 内容为字符串
     */
    private String readResourceContent(Resource resource) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            return content.toString();
        }
    }

}