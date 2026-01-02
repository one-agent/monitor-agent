/**
 * Main Chat Interface Component
 * Provides multi-turn conversation with message history using Ant Design
 */

import { useState, useEffect, useRef } from 'react';
import { Input, Button, Empty, Card, Spin } from 'antd';
import {
  SendOutlined,
  DeleteOutlined,
  RobotOutlined,
  UserOutlined,
  CopyOutlined,
  CheckOutlined
} from '@ant-design/icons';
import { processRequestStream } from '../services/api';
import type { Message, MonitorLog } from '../types';
import MarkdownText from './MarkdownText';
import './ChatInterface.css';

const { TextArea } = Input;

// 可折叠的思考过程组件
const ThinkingPanel = ({ reasoning, isThinkingDone }: { reasoning: string; isThinkingDone?: boolean }) => {
  const [isExpanded, setIsExpanded] = useState(false);
  const isExpandedRef = useRef(isExpanded);

  // 同步 ref 和 state
  useEffect(() => {
    isExpandedRef.current = isExpanded;
  }, [isExpanded]);

  // 当有内容时，默认展开（只在思考未完成时）
  useEffect(() => {
    if (!isExpandedRef.current && !isThinkingDone) {
      setIsExpanded(true);
    }
  }, [isThinkingDone]);

  // 当思考完成时，自动收起
  useEffect(() => {
    if (isThinkingDone && isExpandedRef.current) {
      setIsExpanded(false);
    }
  }, [isThinkingDone]);

  return (
    <div className="thinking-panel">
      <div 
        className="thinking-header"
        onClick={() => setIsExpanded(!isExpanded)}
      >
        <span className="thinking-icon">💭</span>
        <span className="thinking-title">
          {isThinkingDone ? '思考过程' : '思考中...'}
        </span>
        <span className="thinking-toggle">
          {isExpanded ? '▼' : '▶'}
        </span>
      </div>
      {isExpanded && (
        <div className="thinking-content">
          <MarkdownText>{reasoning}</MarkdownText>
        </div>
      )}
    </div>
  );
};

// 可折叠的工具结果组件
const ToolResultsPanel = ({ toolResults, isToolResultsDone }: { toolResults: string[]; isToolResultsDone?: boolean }) => {
  const [isExpanded, setIsExpanded] = useState(false);
  const isExpandedRef = useRef(isExpanded);

  // 同步 ref 和 state
  useEffect(() => {
    isExpandedRef.current = isExpanded;
  }, [isExpanded]);

  // 当有内容时，默认展开（只在工具结果未完成时）
  useEffect(() => {
    if (!isExpandedRef.current && !isToolResultsDone) {
      setIsExpanded(true);
    }
  }, [isToolResultsDone]);

  // 当工具结果完成时，自动收起
  useEffect(() => {
    if (isToolResultsDone && isExpandedRef.current) {
      setIsExpanded(false);
    }
  }, [isToolResultsDone]);

  return (
    <div className="tool-results-panel">
      <div 
        className="tool-results-header"
        onClick={() => setIsExpanded(!isExpanded)}
      >
        <span className="tool-results-icon">🔧</span>
        <span className="tool-results-title">
          {isToolResultsDone ? '工具执行结果' : '工具执行中...'}
        </span>
        <span className="tool-results-toggle">
          {isExpanded ? '▼' : '▶'}
        </span>
      </div>
      {isExpanded && (
        <div className="tool-results-content">
          {toolResults.map((result, index) => (
            <div key={index} className="tool-result-item">
              <MarkdownText>{result}</MarkdownText>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

interface ChatInterfaceProps {
  /**
   * Optional: Provide monitor logs context for richer responses
   */
  monitorLogs?: MonitorLog[];
  /**
   * Optional: API status context
   */
  apiStatus?: string;
  /**
   * Optional: API response time context
   */
  apiResponseTime?: string;
}

export default function ChatInterface({
  monitorLogs = [],
  apiStatus = '200 OK',
  apiResponseTime = 'Unknown'
}: ChatInterfaceProps) {
  const [messages, setMessages] = useState<Message[]>([]);
  const [loading, setLoading] = useState(false);
  const [inputValue, setInputValue] = useState('');
  const [copiedMessageId, setCopiedMessageId] = useState<string | null>(null);
  const [customApiStatus, setCustomApiStatus] = useState(apiStatus);
  const [customApiResponseTime, setCustomApiResponseTime] = useState(apiResponseTime);
  const [showSettings, setShowSettings] = useState(false);
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const messagesContainerRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<any>(null);

  // Load saved messages from localStorage on mount
  useEffect(() => {
    const saved = localStorage.getItem('chat-messages');
    if (saved) {
      try {
        setMessages(JSON.parse(saved));
      } catch (e) {
        console.error('Failed to load saved messages:', e);
      }
    }
  }, []);

  // Save messages to localStorage when they change
  useEffect(() => {
    localStorage.setItem('chat-messages', JSON.stringify(messages));
  }, [messages]);

  // Auto-scroll to bottom when new messages arrive
  useEffect(() => {
    if (messagesEndRef.current) {
      messagesEndRef.current.scrollIntoView({ behavior: 'smooth' });
    }
  }, [messages]);

  const handleSend = async () => {
    const content = inputValue.trim();
    if (!content || loading) return;

    // Generate unique case ID
    const caseId = `C${Date.now()}`;

    // Add user message
    const userMsg: Message = {
      id: Date.now().toString(),
      role: 'user',
      content,
      timestamp: new Date().toISOString()
    };

    setMessages(prev => [...prev, userMsg]);
    setInputValue('');
    setLoading(true);

    // 立即聚焦到输入框
    setTimeout(() => {
      inputRef.current?.focus();
    }, 0);

    // Create an empty assistant message upfront for streaming
    const assistantMsgId = (Date.now() + 1).toString();
    const assistantMsg: Message = {
      id: assistantMsgId,
      role: 'assistant',
      content: '',
      timestamp: new Date().toISOString()
    };

    setMessages(prev => [...prev, assistantMsg]);

    try {
      // Use streaming API for real-time typewriter effect
      await processRequestStream(
        {
          case_id: caseId,
          user_query: content,
          api_status: customApiStatus,
          api_response_time: customApiResponseTime,
          monitor_log: monitorLogs
        },
        // onChunk - update message content in real-time
        (chunk: string) => {
          // 使用函数式更新确保正确累积内容
          setMessages(prev => {
            const msgIndex = prev.findIndex(m => m.id === assistantMsgId);
            if (msgIndex !== -1) {
              const currentContent = prev[msgIndex].content;
              const newContent = currentContent + chunk;

              const newMessages = [...prev];
              newMessages[msgIndex] = { ...prev[msgIndex], content: newContent };
              
              // 如果接收到非工具结果的内容，且之前还没有标记为思考完成，则标记为完成
              if (!chunk.startsWith('\n\n> 🔧') && !prev[msgIndex].isThinkingDone) {
                newMessages[msgIndex] = { ...newMessages[msgIndex], isThinkingDone: true };
              }
              
              // 如果接收到非工具结果的内容，且之前还没有标记为工具结果完成，则标记为完成
              if (!chunk.startsWith('\n\n> 🔧') && !prev[msgIndex].isToolResultsDone) {
                newMessages[msgIndex] = { ...newMessages[msgIndex], isToolResultsDone: true };
              }
              
              return newMessages;
            }
            return prev;
          });
        },
        // onComplete - streaming finished
        () => {
          // Result is already handled through chunks, just ensure loading state is updated
          setLoading(false);
          // Set tool results as done
          setMessages(prev => {
            const msgIndex = prev.findIndex(m => m.id === assistantMsgId);
            if (msgIndex !== -1) {
              const newMessages = [...prev];
              newMessages[msgIndex] = { ...prev[msgIndex], isToolResultsDone: true };
              return newMessages;
            }
            return prev;
          });
          // Focus back on input
          setTimeout(() => {
            inputRef.current?.focus();
          }, 0);
        },
        // onError - handle errors
        (error: Error) => {
          console.error('Error sending message:', error);
          setMessages(prev =>
            prev.map(msg =>
              msg.id === assistantMsgId
                ? {
                    ...msg,
                    content: 'Sorry, there was an error processing your request. Please try again.'
                  }
                : msg
            )
          );
          setLoading(false);
          // Focus back on input
          setTimeout(() => {
            inputRef.current?.focus();
          }, 0);
        },
        // onReasoning - handle reasoning content
        (reasoning: string) => {
          if (reasoning && reasoning.trim()) {
            setMessages(prev => {
              const msgIndex = prev.findIndex(m => m.id === assistantMsgId);
              if (msgIndex !== -1) {
                const newMessages = [...prev];
                newMessages[msgIndex] = { ...prev[msgIndex], reasoning };
                return newMessages;
              }
              return prev;
            });
          }
        },
        // onToolResult - handle tool result content
        (toolResult: string) => {
          setMessages(prev => {
            const msgIndex = prev.findIndex(m => m.id === assistantMsgId);
            if (msgIndex !== -1) {
              const currentToolResults = prev[msgIndex].toolResults || [];
              const newMessages = [...prev];
              newMessages[msgIndex] = { 
                ...prev[msgIndex], 
                toolResults: [...currentToolResults, toolResult]
              };
              return newMessages;
            }
            return prev;
          });
        }
      );
    } catch (error) {
      console.error('Error sending message:', error);

      // Update the assistant message with error
      setMessages(prev =>
        prev.map(msg =>
          msg.id === assistantMsgId
            ? {
                ...msg,
                content: 'Sorry, there was an error processing your request. Please try again.'
              }
            : msg
        )
      );
      setLoading(false);
      // Focus back on input
      setTimeout(() => {
        messagesContainerRef.current?.querySelector('textarea')?.focus();
      }, 100);
    }
  };

  const handleKeyPress = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  const handleCopy = (content: string, messageId: string) => {
    navigator.clipboard.writeText(content);
    setCopiedMessageId(messageId);
    setTimeout(() => setCopiedMessageId(null), 2000);
  };

  const handleClearHistory = () => {
    setMessages([]);
    localStorage.removeItem('chat-messages');
  };

  const hasMessages = messages.length > 0;

  return (
    <div className="chat-interface">
      <div className="chat-header">
        <h2>
          <RobotOutlined /> Monitor Agent Chat
        </h2>
        <Button
          type="text"
          icon={<DeleteOutlined />}
          onClick={handleClearHistory}
          disabled={!hasMessages}
          className="clear-btn"
        >
          Clear History
        </Button>
      </div>

      <div className="messages-container" ref={messagesContainerRef}>
        {hasMessages ? (
          <div className="messages">
            {messages.map((msg) => (
              <div
                key={msg.id}
                className={`message-wrapper ${msg.role}`}
              >
                <Card
                  className={`message-bubble ${msg.role}`}
                  bordered={false}
                >
                  <div className="message-content">
                    <div className="message-header">
                      {msg.role === 'user' ? (
                        <span className="message-author">
                          <UserOutlined /> You
                        </span>
                      ) : (
                        <span className="message-author assistant">
                          <RobotOutlined /> Assistant
                        </span>
                      )}
                    </div>
                    {msg.role === 'assistant' && msg.reasoning && msg.reasoning.trim() && (
                      <ThinkingPanel 
                        reasoning={msg.reasoning} 
                        isThinkingDone={msg.isThinkingDone} 
                      />
                    )}
                    {msg.role === 'assistant' && msg.toolResults && msg.toolResults.length > 0 && (
                      <ToolResultsPanel 
                        toolResults={msg.toolResults} 
                        isToolResultsDone={msg.isToolResultsDone} 
                      />
                    )}
                    <div className="message-text">
                      <MarkdownText>
                        {msg.content}
                      </MarkdownText>
                    </div>
                    <div className="message-footer">
                      <span className="message-time">
                        {new Date(msg.timestamp).toLocaleTimeString()}
                      </span>
                      <Button
                        type="text"
                        size="small"
                        icon={copiedMessageId === msg.id ? <CheckOutlined /> : <CopyOutlined />}
                        onClick={() => handleCopy(msg.content, msg.id)}
                        className="copy-btn"
                      />
                    </div>
                  </div>
                </Card>
              </div>
            ))}
            {loading && messages[messages.length - 1]?.role === 'assistant' && messages[messages.length - 1]?.content === '' && (
              <div className="message-wrapper assistant">
                <Card className="message-bubble assistant loading" bordered={false}>
                  <div className="message-content">
                    <div className="message-header">
                      <span className="message-author assistant">
                        <RobotOutlined /> Assistant
                      </span>
                    </div>
                    <div className="message-text">
                      <Spin size="small" /> <span className="typing-cursor">Typing</span>
                    </div>
                  </div>
                </Card>
              </div>
            )}
            <div ref={messagesEndRef} />
          </div>
        ) : (
          <div className="welcome-container">
            <Empty
              image={Empty.PRESENTED_IMAGE_SIMPLE}
              description={
                <div className="welcome-description">
                  <h3>Welcome to Monitor Agent</h3>
                  <p>I'm your intelligent customer service monitoring assistant.</p>
                  <ul className="welcome-features">
                    <li>Answering questions about Shengsuan Cloud platform</li>
                    <li>Checking system monitoring status</li>
                    <li>Responding to stability inquiries with real data</li>
                  </ul>
                  <p className="welcome-hint">Type your question below to get started!</p>
                </div>
              }
            />
          </div>
        )}
      </div>

      <div className="input-container">
        {/* 设置面板 */}
        <div className="settings-panel">
          <Button
            type="text"
            size="small"
            onClick={() => setShowSettings(!showSettings)}
            className="settings-toggle"
          >
            {showSettings ? '⬆️ 收起设置' : '⚙️ 测试设置'}
          </Button>
          
          {showSettings && (
            <div className="settings-content">
              <div className="setting-item">
                <label>API Status:</label>
                <Input
                  value={customApiStatus}
                  onChange={(e) => setCustomApiStatus(e.target.value)}
                  placeholder="例如: 500 Internal Server Error"
                  size="small"
                  className="setting-input"
                />
              </div>
              <div className="setting-item">
                <label>Response Time:</label>
                <Input
                  value={customApiResponseTime}
                  onChange={(e) => setCustomApiResponseTime(e.target.value)}
                  placeholder="例如: 5000ms"
                  size="small"
                  className="setting-input"
                />
              </div>
              <div className="setting-presets">
                <Button size="small" onClick={() => {
                  setCustomApiStatus('200 OK');
                  setCustomApiResponseTime('100ms');
                }}>
                  正常状态
                </Button>
                <Button size="small" onClick={() => {
                  setCustomApiStatus('500 Internal Server Error');
                  setCustomApiResponseTime('5000ms');
                }}>
                  服务器错误
                </Button>
                <Button size="small" onClick={() => {
                  setCustomApiStatus('503 Service Unavailable');
                  setCustomApiResponseTime('10000ms');
                }}>
                  服务不可用
                </Button>
              </div>
            </div>
          )}
        </div>

        <div className="input-wrapper">
          <TextArea
            ref={inputRef}
            value={inputValue}
            onChange={(e) => setInputValue(e.target.value)}
            onKeyDown={handleKeyPress}
            placeholder="Type your question here... (Enter to send, Shift+Enter for new line)"
            autoSize={{ minRows: 1, maxRows: 6 }}
            disabled={loading}
            className="message-input"
          />
          <Button
            type="primary"
            icon={<SendOutlined />}
            onClick={handleSend}
            disabled={!inputValue.trim() || loading}
            loading={loading}
            className="send-btn"
          >
            Send
          </Button>
        </div>
        <div className="input-hint">
          Press Enter to send, Shift+Enter for new line
        </div>
      </div>
    </div>
  );
}
