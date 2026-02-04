/**
 * System Status Indicator Component
 * Displays the current monitoring status of the backend service
 */

import { useEffect, useState } from 'react';
import { healthCheck } from '../services/api';
import './StatusIndicator.css';

export default function StatusIndicator() {
  const [isHealthy, setIsHealthy] = useState<boolean | null>(null);
  const [lastUpdate, setLastUpdate] = useState<Date>(new Date());

  const fetchStatus = async () => {
    try {
      const health = await healthCheck();
      setIsHealthy(health.status === 'UP');
      setLastUpdate(new Date());
    } catch (error) {
      console.error('Failed to fetch health status:', error);
      setIsHealthy(false);
    }
  };

  useEffect(() => {
    fetchStatus();
    const interval = setInterval(fetchStatus, 10000); // Update every 10 seconds
    return () => clearInterval(interval);
  }, []);

  if (isHealthy === null) {
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
          {isHealthy ? 'System Healthy' : 'System Unhealthy'}
        </span>
        <span className="status-time">
          Last check: {lastUpdate.toLocaleTimeString()}
        </span>
      </div>
    </div>
  );
}
