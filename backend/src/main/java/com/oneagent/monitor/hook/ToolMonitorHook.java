package com.oneagent.monitor.hook;

import io.agentscope.core.hook.Hook;
import io.agentscope.core.hook.HookEvent;
import io.agentscope.core.hook.PostActingEvent;
import io.agentscope.core.hook.PreActingEvent;
import io.agentscope.core.message.TextBlock;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
public class ToolMonitorHook implements Hook {

    @Override
    public <T extends HookEvent> Mono<T> onEvent(T event) {
        
        if (event instanceof PreActingEvent e) {
            log.info("调用工具:{} 参数: {}" , e.getToolUse().getName(), e.getToolUse().getInput());
            return Mono.just(event);
        }

        if (event instanceof PostActingEvent e) {
            String resultText = e.getToolResult().getOutput().stream()
                    .filter(block -> block instanceof TextBlock)
                    .map(block -> ((TextBlock) block).getText())
                    .findFirst()
                    .orElse("");
            log.info("工具名:{} 工具返回结果: {}" , e.getToolResult().getName(), resultText);
            return Mono.just(event);
        }
        return Mono.just(event);
    }
}