/**
 * System Status Indicator Component
 * Displays the current monitoring status of the backend service
 */

import { useEffect, useState } from 'react';
import { getMonitorStatus, healthCheck } from '../services/api';
import type { MonitorStatus } from '../types';
import './StatusIndicator.css';

export default function StatusIndicator() {
  const [status, setStatus] = useState<MonitorStatus | null>(null);
  const [isHealthy, setIsHealthy] = useState<boolean | null>(null);
  const [lastUpdate, setLastUpdate] = useState<Date>(new Date());

  const fetchStatus = async () => {
    try {
      const [monitorStatus, health] = await Promise.all([
        getMonitorStatus(),
        healthCheck()
      ]);

      setStatus(monitorStatus);
      setIsHealthy(health.status === 'UP');
      setLastUpdate(new Date());
    } catch (error) {
      console.error('Failed to fetch status:', error);
      setIsHealthy(false);
    }
  };

  useEffect(() => {
    fetchStatus();
    const interval = setInterval(fetchStatus, 10000); // Update every 10 seconds
    return () => clearInterval(interval);
  }, []);

  if (!status) {
    return (
      <div className="status-indicator loading">
        <span className="status-dot">•</span>
        <span>Loading status...</span>
      </div>
    );
  }

  return (
    <div className={`status-indicator ${isHealthy ? 'healthy' : 'unhealthy'}`}>
      <span className={`status-dot ${isHealthy ? 'healthy' : 'unhealthy'}`}>
        {isHealthy ? '●' : '●'}
      </span>
      <div className="status-info">
        <span className="status-text">
          {status.status || (isHealthy ? 'System Healthy' : 'System Unhealthy')}
        </span>
        {status.responseTime && (
          <span className="status-detail">
            | Latency: {status.responseTime}
          </span>
        )}
        {!status.healthy && status.errorCount > 0 && (
          <span className="status-error">
            | Errors: {status.errorCount}
          </span>
        )}
        <span className="status-time">
          Last check: {lastUpdate.toLocaleTimeString()}
        </span>
      </div>
    </div>
  );
}
