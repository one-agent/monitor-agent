/**
 * Main App Component
 * Root component for the Monitor Agent Chat Interface
 */

import { useState } from 'react';
import ChatInterface from './components/ChatInterface';
import StatusIndicator from './components/StatusIndicator';
import type { MonitorLog } from './types';
import './App.css';

export default function App() {
  const [monitorLogs] = useState<MonitorLog[]>([]);
  const [apiStatus] = useState<string>('200 OK');
  const [apiResponseTime] = useState<string>('Unknown');

  return (
    <div className="app">
      <header>
        <div className="header-content">
          <h1>Monitor Agent</h1>
          <div className="header-status">
            <StatusIndicator />
          </div>
        </div>
      </header>

      <main className="main-content">
        <ChatInterface
          monitorLogs={monitorLogs}
          apiStatus={apiStatus}
          apiResponseTime={apiResponseTime}
        />
      </main>

      <footer>
        <p>Monitor Agent v1.0.0 | Powered by AgentScope & Spark Design Chat</p>
      </footer>
    </div>
  );
}
