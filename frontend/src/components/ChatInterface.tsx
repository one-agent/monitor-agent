/**
 * Main Chat Interface Component
 * Provides multi-turn conversation with message history using Ant Design
 */

import { useState, useEffect, useRef } from 'react';
import { Input, Button, Empty, Card, Spin, Upload } from 'antd';
import {
  SendOutlined,
  DeleteOutlined,
  RobotOutlined,
  UserOutlined,
  CopyOutlined,
  CheckOutlined,
  UploadOutlined,
  FileAddOutlined
} from '@ant-design/icons';
import { processRequestStream, resetSession } from '../services/api';
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
  // 为每个聊天框生成固定的 caseId
  const [caseId, setCaseId] = useState(`C${Date.now()}`);
  const [messages, setMessages] = useState<Message[]>([]);
  const [loading, setLoading] = useState(false);
  const [inputValue, setInputValue] = useState('');
  const [copiedMessageId, setCopiedMessageId] = useState<string | null>(null);
  const [images, setImages] = useState<string[]>([]);
  const [fileList, setFileList] = useState<any[]>([]);
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const messagesContainerRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLTextAreaElement | null>(null);

  // Auto-scroll to bottom when new messages arrive
  useEffect(() => {
    if (messagesEndRef.current) {
      messagesEndRef.current.scrollIntoView({ behavior: 'smooth' });
    }
  }, [messages]);

  const handleSend = async () => {
    const content = inputValue.trim();
    if ((!content && images.length === 0) || loading) return;

    // 保存当前要发送的图片数据
    const currentImages = [...images];

    // Add user message
    const userMsg: Message = {
      id: Date.now().toString(),
      role: 'user',
      content: content,
      timestamp: new Date().toISOString(),
      images: currentImages.length > 0 ? currentImages : undefined
    };

    setMessages(prev => [...prev, userMsg]);
    setInputValue('');
    setImages([]); // 清空图片
    setFileList([]); // 清空文件列表
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
      // 构造监控日志，如果状态不是 200，添加异常日志
      let currentMonitorLogs = [...monitorLogs];
      if (!apiStatus.startsWith('200')) {
        currentMonitorLogs.push({
          timestamp: new Date().toISOString(),
          status: apiStatus,
          msg: `System alert: API responded with status ${apiStatus} - Exception detected in request processing`
        });
      }

      // Use streaming API for real-time typewriter effect
      await processRequestStream(
        {
          case_id: caseId,
          user_query: content,
          api_status: apiStatus,
          api_response_time: apiResponseTime,
          monitor_log: currentMonitorLogs,
          images: currentImages // 添加图片数据
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
        const textarea = messagesContainerRef.current?.querySelector('textarea');
        if (textarea && 'focus' in textarea) {
          (textarea as HTMLTextAreaElement).focus();
        }
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

  const handleClearHistory = async () => {
    // 调用后端 API 重置会话
    try {
      await resetSession(caseId);
      console.log(`会话 ${caseId} 已重置`);
    } catch (error) {
      console.error('重置会话失败:', error);
    }

    // 生成新的 caseId
    setCaseId(`C${Date.now()}`);

    // 清空前端消息
    setMessages([]);
  };

  /**
   * 压缩图片到指定大小以内（1MB）
   * @param file 原始文件
   * @param maxSize 最大文件大小（字节），默认 1MB
   * @returns 压缩后的 base64 字符串
   */
  const compressImage = (file: File, maxSize: number = 1 * 1024 * 1024): Promise<string> => {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.readAsDataURL(file);
      reader.onload = (event) => {
        const img = new Image();
        img.src = event.target?.result as string;
        img.onload = () => {
          const canvas = document.createElement('canvas');
          const ctx = canvas.getContext('2d');

          if (!ctx) {
            reject(new Error('无法获取 canvas context'));
            return;
          }

          // 初始尺寸
          let width = img.width;
          let height = img.height;
          let quality = 0.9;

          // 计算初始缩放比例（如果图片太大）
          const maxDimension = 2048;
          if (width > maxDimension || height > maxDimension) {
            if (width > height) {
              height = (height * maxDimension) / width;
              width = maxDimension;
            } else {
              width = (width * maxDimension) / height;
              height = maxDimension;
            }
          }

          canvas.width = width;
          canvas.height = height;

          // 绘制图片
          ctx.drawImage(img, 0, 0, width, height);

          // 压缩函数
          const compress = (currentQuality: number): void => {
            canvas.toBlob(
              (blob) => {
                if (!blob) {
                  reject(new Error('压缩失败'));
                  return;
                }

                // 如果大小符合要求，或者质量已经很低了，就返回
                if (blob.size <= maxSize || currentQuality <= 0.1) {
                  const reader = new FileReader();
                  reader.readAsDataURL(blob);
                  reader.onload = (e) => {
                    const result = e.target?.result as string;
                    if (blob.size > maxSize) {
                      console.warn(`图片压缩后仍超过 ${maxSize / 1024 / 1024}MB，当前大小: ${(blob.size / 1024 / 1024).toFixed(2)}MB`);
                    } else {
                      console.log(`图片压缩成功: ${(file.size / 1024 / 1024).toFixed(2)}MB -> ${(blob.size / 1024 / 1024).toFixed(2)}MB`);
                    }
                    resolve(result);
                  };
                  reader.onerror = (error) => reject(error);
                } else {
                  // 继续降低质量
                  compress(currentQuality - 0.1);
                }
              },
              'image/jpeg',
              currentQuality
            );
          };

          // 开始压缩
          compress(quality);
        };
        img.onerror = (error) => reject(error);
      };
      reader.onerror = (error) => reject(error);
    });
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
          icon={<FileAddOutlined />}
          onClick={handleClearHistory}
          disabled={!hasMessages}
          className="clear-btn"
        >
          New Session
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
                    {/* 显示用户上传的图片 */}
                    {msg.role === 'user' && msg.images && msg.images.length > 0 && (
                      <div className="message-images">
                        {msg.images.map((image, index) => (
                          <img
                            key={index}
                            src={image}
                            alt={`上传的图片 ${index + 1}`}
                            className="message-image"
                            onClick={() => {
                              // 点击图片可以在新标签页中打开大图
                              const newWindow = window.open('', '_blank');
                              if (newWindow) {
                                newWindow.document.write(`
                                  <html>
                                    <head><title>图片预览</title></head>
                                    <body style="margin:0; display:flex; justify-content:center; align-items:center; min-height:100vh; background:#f0f0f0;">
                                      <img src="${image}" style="max-width:100%; max-height:100vh; object-fit:contain;">
                                    </body>
                                  </html>
                                `);
                              }
                            }}
                          />
                        ))}
                      </div>
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
          <div style={{ display: 'flex', gap: '8px', alignItems: 'flex-end' }}>
            <Upload
              multiple
              accept="image/*"
              fileList={fileList}
              onChange={(info) => {
                setFileList(info.fileList);
              }}
              showUploadList={true}
              beforeUpload={async (file) => {
                try {
                  const maxSize = 1 * 1024 * 1024; // 1MB

                  if (file.size > maxSize) {
                    console.log(`图片大小 ${(file.size / 1024 / 1024).toFixed(2)}MB 超过 1MB，开始压缩...`);
                    const compressedBase64 = await compressImage(file, maxSize);
                    setImages(prev => [...prev, compressedBase64]);
                  } else {
                    // 图片小于 1MB，直接转换
                    const reader = new FileReader();
                    reader.onload = (e) => {
                      const base64String = e.target?.result as string;
                      setImages(prev => [...prev, base64String]);
                    };
                    reader.readAsDataURL(file);
                  }
                } catch (error) {
                  console.error('图片处理失败:', error);
                }
                return false; // 阻止默认上传行为
              }}
              maxCount={5} // 限制最多上传5张图片
            >
              <Button
                type="text"
                icon={<UploadOutlined />}
                disabled={loading}
                className="upload-btn"
                title="Upload Image"
              >
                Upload Image
              </Button>
            </Upload>
            <Button
              type="primary"
              icon={<SendOutlined />}
              onClick={handleSend}
              disabled={(!inputValue.trim() && images.length === 0) || loading}
              loading={loading}
              className="send-btn"
              title="Send"
            >
              Send
            </Button>
          </div>
        </div>
        <div className="input-hint">
          Press Enter to send, Shift+Enter for new line
        </div>
      </div>
    </div>
  );
}
