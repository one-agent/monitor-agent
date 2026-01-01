/**
 * TypeScript type definitions for Monitor Agent Chat Interface
 */

export interface Message {
  id: string;
  role: 'user' | 'assistant';
  content: string;
  timestamp: string;
  reasoning?: string; // 思考过程内容
  isThinkingDone?: boolean; // 思考是否完成
}

export interface MonitorLog {
  timestamp: string;
  status: string;
  msg: string;
}

export interface ChatRequest {
  query: string;
}

export interface ChatResponse {
  reply: string;
}

export interface ProcessRequest {
  case_id: string;
  user_query: string;
  api_status: string;
  api_response_time: string;
  monitor_log: MonitorLog[];
}

export interface ProcessResponse {
  case_id: string;
  reply: string;
  action_triggered: ActionTriggered | null;
}

export interface ActionTriggered {
  feishu_webhook?: string;
  apifox_doc_id?: string;
}

export interface MonitorStatus {
  status: string;
  responseTime: string;
  healthy: boolean;
  errorCount: number;
  lastCheckTime: string;
}

/**
 * SSE 事件类型
 */
export type SseEventType = 'reasoning' | 'tool_result' | 'hint' | 'complete' | 'error' | 'agent_result' | 'unknown';

/**
 * SSE 事件数据
 */
export interface SseEvent {
  event: SseEventType;
  data: string;
  caseId?: string;
  toolName?: string;
  actionTriggered?: ActionTriggered;
}

/**
 * 流式请求处理函数类型
 */
export type StreamRequestHandler = (
  data: ProcessRequest,
  onChunk: (chunk: string) => void,
  onComplete: (result?: ProcessResponse) => void,
  onError: (error: Error) => void,
  onReasoning?: (reasoning: string) => void
) => void;
