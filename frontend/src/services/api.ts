/**
 * API service for communicating with the Monitor Agent backend
 */

import type {
  ProcessRequest,
  ProcessResponse,
  MonitorStatus,
  StreamRequestHandler
} from '../types';

// @ts-ignore
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8081/api';

/**
 * Process a request with AgentScope streaming support
 */
export const processRequestStream: StreamRequestHandler = async (
    data: ProcessRequest,
    onChunk: (chunk: string) => void,
    onComplete: (result?: ProcessResponse) => void,
    onError: (error: Error) => void,
    onReasoning?: (reasoning: string) => void
) => {
  console.log('Starting stream request:', data);
  let eventCount = 0;
  try {
    // 使用 WebFlux 端点进行流式响应
    const response = await fetch(`${API_BASE_URL}/process`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'text/event-stream',
        'Cache-Control': 'no-cache',
      },
      body: JSON.stringify(data),
    });

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }

    const reader = response.body?.getReader();
    if (!reader) {
      throw new Error('Failed to get response body reader');
    }

    const decoder = new TextDecoder();
    let buffer = '';
    let fullResponse = '';
    let accumulatedReasoning = '';
    let caseId = '';

    while (true) {
      const {done, value} = await reader.read();

      if (done) {
        // 如果有思考内容，通过回调传递
        if (accumulatedReasoning.trim() && onReasoning) {
          onReasoning(accumulatedReasoning.trim());
        }
        onComplete({
          case_id: caseId,
          reply: fullResponse,
          action_triggered: null,
        });
        break;
      }

      buffer += decoder.decode(value, {stream: true});

      const lines = buffer.split('\n');
      buffer = lines.pop() || '';

      let currentEventType: string | null = null;

      for (const line of lines) {
        if (line.startsWith('event:')) {
          // 事件类型行
          currentEventType = line.substring(6).trim();
        } else if (line.startsWith('data:')) {
          try {
            const textData = line.substring(5).trim();
            if (!textData) continue;

            eventCount++;
            console.log(`Event #${eventCount}: type=${currentEventType}, data=${textData.substring(0, 50)}...`);

            if (currentEventType === 'reasoning') {
              // 思考过程 - 实时累积并更新
              accumulatedReasoning += textData + '\n';
              if (onReasoning) {
                onReasoning(accumulatedReasoning.trim());
              }
            } else if (currentEventType === 'tool_result') {
              // 工具调用结果 - 以引用块显示
              let toolContent = '';
              if (textData === '[No response]') {
                toolContent = `\n\n> 🔧 工具执行：无响应\n\n`;
              } else if (textData.trim().startsWith('{') || textData.trim().startsWith('[')) {
                // JSON 格式
                toolContent = `\n\n> 🔧 工具执行结果\n\`\`\`json\n${textData}\n\`\`\`\n\n`;
              } else {
                // 其他工具结果
                toolContent = `\n\n> 🔧 工具执行结果\n> ${textData.replace(/\n/g, '\n> ')}\n\n`;
              }

              fullResponse += toolContent;
              onChunk(toolContent);
            } else if (currentEventType === 'content') {
              // 正文内容 - 正常显示
              fullResponse += textData;
              onChunk(textData);
            }
          } catch (e) {
            console.error('Failed to parse SSE chunk:', line, e);
          }
        }
      }
    }
  } catch (error) {
    onError(error instanceof Error ? error : new Error('Unknown error'));
  }
};

/**
 * Send a process request with monitoring context (non-streaming fallback)
 * @deprecated Use processRequestStream instead
 */
// Removed unused processRequest function

/**
 * Get the current system monitoring status
 */
export async function getMonitorStatus(): Promise<MonitorStatus> {
  const response = await fetch(`${API_BASE_URL}/monitor/status`);

  if (!response.ok) {
    throw new Error(`HTTP error! status: ${response.status}`);
  }

  return response.json();
}

/**
 * Check if the backend service is healthy
 */
export async function healthCheck(): Promise<{ status: string; service: string }> {
  const response = await fetch(`${API_BASE_URL}/health`);

  if (!response.ok) {
    throw new Error(`HTTP error! status: ${response.status}`);
  }

  return response.json();
}
