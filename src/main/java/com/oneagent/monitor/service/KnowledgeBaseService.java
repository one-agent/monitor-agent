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
 * Service for knowledge base management
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
     * Load knowledge base from files
     */
    public void loadKnowledgeBase() {
        log.info("Loading knowledge base from: {}", knowledgePath);

        try {
            // Note: In actual implementation, this would use AgentScope's Knowledge classes
            // For this project, we'll use a simple in-memory approach
            // since embedding models might need external API keys

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

            // Initialize SimpleKnowledge with in-memory store
            // Note: This is a simplified approach without actual embeddings
            // In production, you would configure an embedding model
            this.knowledge = SimpleKnowledge.builder()
                    .embeddingStore(InMemoryStore.builder().build())
                    .build();

            log.info("Knowledge base loaded with {} documents", documents.size());

        } catch (IOException e) {
            log.error("Failed to load knowledge base", e);
        }
    }

    /**
     * Search knowledge base
     */
    public List<String> search(String query) {
        log.debug("Searching knowledge base for: {}", query);

        List<String> results = new ArrayList<>();

        if (knowledge == null) {
            // Fallback to simple text search
            results.addAll(simpleTextSearch(query));
        } else {
            // Would use RAG here in full implementation
            // For now, use simple search
            results.addAll(simpleTextSearch(query));
        }

        return results;
    }

    /**
     * Simple text-based search (fallback when embeddings are not configured)
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

                            // Simple keyword matching
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
     * Get knowledge base instance
     */
    public SimpleKnowledge getKnowledge() {
        return knowledge;
    }
}
