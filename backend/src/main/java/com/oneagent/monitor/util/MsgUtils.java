/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.oneagent.monitor.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.agentscope.core.message.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

/**
 * Utility methods for working with Msg in examples. These are convenience
 * methods for common
 * operations.
 */
@Slf4j
public class MsgUtils {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Extract text content from a message. Concatenates text from all
     * text-containing blocks
     * (TextBlock and ThinkingBlock).
     *
     * @param msg The message to extract text from
     * @return Concatenated text content or empty string if not available
     */
    public static String getTextContent(Msg msg) {
        String thinking =
                msg.getContent().stream()
                        .filter(block -> block instanceof ThinkingBlock)
                        .map(block -> ((ThinkingBlock) block).getThinking())
                        .collect(Collectors.joining("\n"));

        String text =
                msg.getContent().stream()
                        .filter(block -> block instanceof TextBlock)
                        .map(block -> ((TextBlock) block).getText())
                        .collect(Collectors.joining("\n"));

        if (!thinking.isEmpty() && !text.isEmpty()) {
            return thinking + "\n\n" + text;
        } else if (!thinking.isEmpty()) {
            return thinking;
        } else if (!text.isEmpty()) {
            return text;
        } else {
            return "[No response]";
        }
    }

    public static String getToolsContent(Msg msg) {
        Map<String, Object> result = new HashMap<>();

        msg.getContent().stream()
                .filter(block -> block instanceof ToolResultBlock)
                .map(block -> (ToolResultBlock) block)
                .forEach(block -> {
                    // 获取工具名
                    String toolName = block.getName();
                    result.put("toolName", toolName);

                    // 处理工具输出
                    List<ContentBlock> outputs = block.getOutput();
                    if (outputs == null || outputs.isEmpty()) {
                        result.put("content", "[No response]");
                    } else {
                        // 将所有输出转换为字符串
                        List<String> outputStrings = new ArrayList<>();
                        for (ContentBlock output : outputs) {
                            if (output instanceof TextBlock textBlock) {
                                outputStrings.add(textBlock.getText());
                            } else if (output instanceof ImageBlock imageBlock) {
                                outputStrings.add("[Image: " + imageBlock.getSource() + "]");
                            } else if (output instanceof AudioBlock audioBlock) {
                                outputStrings.add("[Audio: " + audioBlock.getSource() + "]");
                            } else {
                                outputStrings.add(output.toString());
                            }
                        }

                        // 检查输出是否包含 JSON 格式的工具结果
                        String combinedOutput = String.join("\n", outputStrings);
                        
                        // 尝试解析 JSON 格式的工具结果
                        try {
                            // 检查是否是 JSON 格式
                            if (combinedOutput.trim().startsWith("{") || combinedOutput.trim().startsWith("[")) {
                                Map<String, Object> toolResult = objectMapper.readValue(combinedOutput, Map.class);
                                
                                // 如果包含 tool_name 和 result 字段，提取它们
                                if (toolResult.containsKey("tool_name") && toolResult.containsKey("result")) {
                                    String actualToolName = (String) toolResult.get("tool_name");
                                    Object actualResult = toolResult.get("result");
                                    
                                    // 使用工具返回的名称
                                    result.put("toolName", actualToolName);
                                    
                                    // 格式化结果
                                    if (actualResult != null) {
                                        String formattedResult = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(actualResult);
                                        result.put("content", formattedResult);
                                    } else {
                                        result.put("content", "[No result]");
                                    }
                                } else {
                                    // 不是标准的工具返回格式，直接使用原始输出
                                    result.put("content", combinedOutput);
                                }
                            } else {
                                // 不是 JSON 格式，直接使用原始输出
                                result.put("content", combinedOutput);
                            }
                        } catch (Exception e) {
                            // 解析失败，直接使用原始输出
                            result.put("content", combinedOutput);
                        }
                    }
                });

        // 转换为 JSON 字符串
        try {
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            log.error("Failed to serialize tool result to JSON", e);
            return "{\"toolName\":\"unknown\",\"content\":\"[Error serializing result]\"}";
        }
    }

    /**
     * Check if a message has text content.
     *
     * @param msg The message to check
     * @return true if the message contains text content
     */
    public static boolean hasTextContent(Msg msg) {
        return msg.getContent().stream()
                .anyMatch(block -> block instanceof TextBlock || block instanceof ThinkingBlock);
    }

    /**
     * Check if a message has media content.
     *
     * @param msg The message to check
     * @return true if the message contains media content
     */
    public static boolean hasMediaContent(Msg msg) {
        return msg.getContent().stream()
                .anyMatch(
                        block ->
                                block instanceof ImageBlock
                                        || block instanceof AudioBlock
                                        || block instanceof VideoBlock);
    }

    /**
     * Create a message with text content (convenience method).
     *
     * @param name Sender name
     * @param role Message role
     * @param text Text content
     * @return Message with text content
     */
    public static Msg textMsg(String name, MsgRole role, String text) {
        return Msg.builder()
                .name(name)
                .role(role)
                .content(TextBlock.builder().text(text).build())
                .build();
    }

    /**
     * Create a message with image content (convenience method).
     *
     * @param name   Sender name
     * @param role   Message role
     * @param source Image source
     * @return Message with image content
     */
    public static Msg imageMsg(String name, MsgRole role, Source source) {
        return Msg.builder()
                .name(name)
                .role(role)
                .content(ImageBlock.builder().source(source).build())
                .build();
    }

    /**
     * Create a message with audio content (convenience method).
     *
     * @param name   Sender name
     * @param role   Message role
     * @param source Audio source
     * @return Message with audio content
     */
    public static Msg audioMsg(String name, MsgRole role, Source source) {
        return Msg.builder()
                .name(name)
                .role(role)
                .content(AudioBlock.builder().source(source).build())
                .build();
    }

    /**
     * Create a message with video content (convenience method).
     *
     * @param name   Sender name
     * @param role   Message role
     * @param source Video source
     * @return Message with video content
     */
    public static Msg videoMsg(String name, MsgRole role, Source source) {
        return Msg.builder()
                .name(name)
                .role(role)
                .content(VideoBlock.builder().source(source).build())
                .build();
    }

    /**
     * Create a message with thinking content (convenience method).
     *
     * @param name     Sender name
     * @param role     Message role
     * @param thinking Thinking content
     * @return Message with thinking content
     */
    public static Msg thinkingMsg(String name, MsgRole role, String thinking) {
        return Msg.builder()
                .name(name)
                .role(role)
                .content(ThinkingBlock.builder().thinking(thinking).build())
                .build();
    }

    private MsgUtils() {
        // Utility class
    }

}
