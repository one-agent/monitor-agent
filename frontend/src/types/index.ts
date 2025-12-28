/**
 * TypeScript type definitions for Monitor Agent Chat Interface
 */

export interface Message {
  id: string;
  role: 'user' | 'assistant';
  content: string;
  timestamp: string;
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
