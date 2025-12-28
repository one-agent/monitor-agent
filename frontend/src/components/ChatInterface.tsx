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
import { sendChatMessage, processRequest } from '../services/api';
import type { Message, MonitorLog } from '../types';
import './ChatInterface.css';

const { TextArea } = Input;

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
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const messagesContainerRef = useRef<HTMLDivElement>(null);

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

    try {
      // Use /api/process for richer context with monitoring data
      const response = await processRequest({
        case_id: caseId,
        user_query: content,
        api_status: apiStatus,
        api_response_time: apiResponseTime,
        monitor_log: monitorLogs
      });

      // Add assistant message
      const assistantMsg: Message = {
        id: (Date.now() + 1).toString(),
        role: 'assistant',
        content: response.reply,
        timestamp: new Date().toISOString()
      };

      setMessages(prev => [...prev, assistantMsg]);
    } catch (error) {
      console.error('Error sending message:', error);

      // Add error message
      const errorMsg: Message = {
        id: (Date.now() + 1).toString(),
        role: 'assistant',
        content: 'Sorry, there was an error processing your request. Please try again.',
        timestamp: new Date().toISOString()
      };

      setMessages(prev => [...prev, errorMsg]);
    } finally {
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
                    <div className="message-text">
                      {msg.content}
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
            {loading && (
              <div className="message-wrapper assistant">
                <Card className="message-bubble assistant loading" bordered={false}>
                  <div className="message-content">
                    <div className="message-header">
                      <span className="message-author assistant">
                        <RobotOutlined /> Assistant
                      </span>
                    </div>
                    <div className="message-text">
                      <Spin size="small" /> Thinking...
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
