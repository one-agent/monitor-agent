package com.oneagent.monitor.session;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.session.JsonSession;
import io.agentscope.core.session.Session;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 统一的Session管理器，用于管理AgentScope中的会话
 */
@Slf4j
@Component
public class SessionManager {

    private final Session session;
    private final ConcurrentHashMap<String, ReActAgent> agentSessions = new ConcurrentHashMap<>();

    public SessionManager() {
        // 设置会话存储路径
        Path sessionPath = Path.of(System.getProperty("user.home"), ".agentscope", "sessions", "monitor");
        this.session = new JsonSession(sessionPath);
        log.info("SessionManager initialized with path: {}", sessionPath);
    }

    /**
     * 获取或创建Agent实例，并加载其会话
     */
    public ReActAgent getOrCreateAgent(String sessionId, ReActAgent defaultAgent) {
        return agentSessions.computeIfAbsent(sessionId, id -> {
            log.info("Creating new agent for session: {}", id);
            ReActAgent agent = defaultAgent;
            // 尝试加载已存在会话
            if (loadIfExists(id)) {
                log.info("Loaded existing session: {}", id);
            } else {
                log.info("No existing session found for: {}", id);
            }
            return agent;
        });
    }

    /**
     * 为指定会话ID加载会话
     */
    public boolean loadIfExists(String sessionId) {
        ReActAgent agent = agentSessions.get(sessionId);
        if (agent != null) {
            return agent.loadIfExists(session, sessionId);
        }
        return false;
    }

    /**
     * 为指定会话ID保存会话
     */
    public void saveSession(String sessionId) {
        ReActAgent agent = agentSessions.get(sessionId);
        if (agent != null) {
            agent.saveTo(session, sessionId);
            log.info("Session saved for: {}", sessionId);
        } else {
            log.warn("No agent found for session: {}", sessionId);
        }
    }

    /**
     * 检查指定会话是否存在
     */
    public boolean exists(String sessionId) {
        return session.exists(io.agentscope.core.state.SimpleSessionKey.of(sessionId));
    }

    /**
     * 删除指定会话
     */
    public void deleteSession(String sessionId) {
        agentSessions.remove(sessionId);
        session.delete(io.agentscope.core.state.SimpleSessionKey.of(sessionId));
        log.info("Session deleted: {}", sessionId);
    }

    /**
     * 清除所有内存中的会话
     */
    public void clear() {
        agentSessions.clear();
        log.info("All session references cleared from memory");
    }

    /**
     * 获取当前已加载的会话ID列表
     */
    public ConcurrentHashMap<String, ReActAgent> getAgentSessions() {
        return new ConcurrentHashMap<>(agentSessions);
    }
}