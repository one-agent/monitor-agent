/**
 * API service for communicating with the Monitor Agent backend
 */

import type {
  ProcessRequest,
  ProcessResponse,
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
  onReasoning?: (reasoning: string) => void,
  onToolResult?: (toolResult: string) => void
) => {
  console.log('Starting stream request:', data);
  let eventCount = 0;

  try {
    // 始终使用 JSON 请求，将图片作为 base64 字符串包含在数据中
    const response = await fetch(`${API_BASE_URL}/process`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'text/event-stream',
        'Cache-Control': 'no-cache',
      },
      body: JSON.stringify(data), // 直接发送包含图片数据的 JSON
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
            // Fix: Do not trim the end of the line! Trimming removes trailing \n or whitespace-only chunks.
            // Standard SSE usually has a space after colon: "data: content"
            let textData = line.substring(5);
            if (textData.startsWith(' ')) {
              textData = textData.substring(1);
            }
            
            // If textData is empty here, it might be an empty line meant to keep connection alive or truly empty.
            // But if it was `data: \n` (encoded as json string "\n"), textData is now `"\n"`.
            // If it was raw `data: \n`, textData is empty? No, buffer split by \n removes the delimiting newline.
            // If content is encoded as JSON string, it will be surrounded by quotes.
            
            if (!textData && textData !== '') continue;

            // 尝试解析 JSON 数据（支持后端的新 JSON 包装格式，也能处理旧的纯文本格式）
            let parsedData: any = textData;
            let isJson = false;
            try {
              if (textData.startsWith('"') || textData.startsWith('{') || textData.startsWith('[')) {
                parsedData = JSON.parse(textData);
                isJson = true;
              }
            } catch (e) {
              // 解析失败，说明是普通文本
            }

            eventCount++;
            
            if (currentEventType === 'reasoning') {
              // 思考过程 - 实时累积并更新
              if (isJson && typeof parsedData === 'string') {
                // 新格式：JSON 字符串，保留了换行符
                accumulatedReasoning += parsedData;
              } else {
                // 旧格式：每行数据追加换行
                accumulatedReasoning += textData + '\n';
              }
              
              if (onReasoning) {
                onReasoning(accumulatedReasoning); // Remove trim() to allow typing effect if needed, or keep trim for display? specific req says trim
              }
            } else if (currentEventType === 'tool_result') {
              // 工具调用结果 - 解析 JSON 格式
              let toolContent = '';
              try {
                // 如果 parsedData 已经是对象，直接使用；否则尝试解析
                const toolData = (typeof parsedData === 'object' && parsedData !== null) 
                    ? parsedData 
                    : JSON.parse(textData);
                    
                const toolName = toolData.toolName || '未知工具';
                const content = toolData.content;

                if (content === '[No response]' || content === '[No result]') {
                  toolContent = `\n\n> 🔧 ${toolName}\n> 无响应\n\n`;
                } else {
                  // 尝试解析 content，处理可能的嵌套 JSON 字符串
                  try {
                    let parsedContent = JSON.parse(content);
                    
                    // 如果解析后是字符串，尝试再次解析（处理转义的 JSON）
                    if (typeof parsedContent === 'string') {
                      try {
                        parsedContent = JSON.parse(parsedContent);
                      } catch {
                        // 第二次解析失败，保持原样
                      }
                    }
                    
                    // 如果是 JSON，使用代码块显示
                    toolContent = `\n\n> 🔧 ${toolName}\n\`\`\`json\n${JSON.stringify(parsedContent, null, 2)}\n\`\`\`\n\n`;
                  } catch {
                    // 不是 JSON 格式，直接显示
                    toolContent = `\n\n> 🔧 ${toolName}\n> ${content.replace(/\n/g, '\n> ')}\n\n`;
                  }
                }
              } catch (e) {
                // 解析失败，显示原始数据
                toolContent = `\n\n> 🔧 工具执行结果\n> ${textData.replace(/\n/g, '\n> ')}\n\n`;
              }

              // 调用 onToolResult 回调
              if (onToolResult) {
                onToolResult(toolContent);
              }

            } else if (currentEventType === 'content') {
              // 正文内容 - 正常显示
              if (isJson && typeof parsedData === 'string') {
                 fullResponse += parsedData;
                 onChunk(parsedData);
              } else {
                 fullResponse += textData;
                 onChunk(textData);
              }
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
 * Check if the backend service is healthy
 */
export async function healthCheck(): Promise<{ status: string; service: string }> {
  const response = await fetch(`${API_BASE_URL}/health`);

  if (!response.ok) {
    throw new Error(`HTTP error! status: ${response.status}`);
  }

  return response.json();
}

/**
 * Reset a specific session by caseId
 */
export async function resetSession(caseId: string): Promise<{ status: string; message: string }> {
  const response = await fetch(`${API_BASE_URL}/session/reset/${caseId}`, {
    method: 'POST',
  });

  if (!response.ok) {
    throw new Error(`HTTP error! status: ${response.status}`);
  }

  return response.json();
}

/**
 * 从 data URL 提取 MIME 类型和数据部分
 */
function extractMimeTypeAndData(dataUrl: string): [string, string] {
  const match = dataUrl.match(/^data:([^;]+);base64,(.+)$/);
  if (match) {
    return [match[1], match[2]];
  }
  return ['image/png', dataUrl]; // 默认为 PNG
}

/**
 * 从 MIME 类型获取文件扩展名
 */
function getFileType(mimeType: string): string {
  const mimeToExt: Record<string, string> = {
    'image/jpeg': 'jpg',
    'image/jpg': 'jpg',
    'image/png': 'png',
    'image/gif': 'gif',
    'image/webp': 'webp',
    'image/svg+xml': 'svg'
  };
  return mimeToExt[mimeType] || 'png';
}

/**
 * 将 data URL 转换为 Blob 对象
 */
function dataURLtoBlob(dataUrl: string): Blob {
  const [mimeType, base64Data] = extractMimeTypeAndData(dataUrl);
  const byteCharacters = atob(base64Data);
  const byteArrays = [];

  for (let offset = 0; offset < byteCharacters.length; offset += 512) {
    const slice = byteCharacters.slice(offset, offset + 512);
    const byteNumbers = new Array(slice.length);
    for (let i = 0; i < slice.length; i++) {
      byteNumbers[i] = slice.charCodeAt(i);
    }
    const byteArray = new Uint8Array(byteNumbers);
    byteArrays.push(byteArray);
  }
  return new Blob(byteArrays, { type: mimeType });
}
