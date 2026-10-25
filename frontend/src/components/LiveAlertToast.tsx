// src/components/LiveAlertToast.tsx
import React from 'react';
import { AlertCircle, AlertTriangle, Info, X } from 'lucide-react';
import { useWebSocket } from '../context/WebSocketContext';

export const LiveAlertToast: React.FC = () => {
  const { recentLiveAlerts, dismissAlert } = useWebSocket();

  if (recentLiveAlerts.length === 0) return null;

  return (
    <div className="fixed bottom-5 right-5 z-50 flex flex-col gap-2 max-w-sm w-full pointer-events-none">
      {recentLiveAlerts.map((alert) => {
        const isCritical = alert.type === 'CRITICAL' || alert.type === 'PREDICTION_ALERT';
        const isWarning = alert.type === 'WARNING' || alert.type === 'INCIDENT_ESCALATED';

        return (
          <div
            key={alert.id}
            className={`pointer-events-auto p-3.5 rounded-xl border shadow-lg flex items-start gap-3 bg-surface transition-all animate-in slide-in-from-bottom-3 duration-200 ${
              isCritical
                ? 'border-red-200 bg-red-50/90 text-red-950'
                : isWarning
                ? 'border-amber-200 bg-amber-50/90 text-amber-950'
                : 'border-border bg-white text-text'
            }`}
          >
            <div className="mt-0.5 shrink-0">
              {isCritical ? (
                <AlertCircle className="w-5 h-5 text-red-600" />
              ) : isWarning ? (
                <AlertTriangle className="w-5 h-5 text-amber-600" />
              ) : (
                <Info className="w-5 h-5 text-primary" />
              )}
            </div>

            <div className="flex-1 min-w-0">
              <div className="flex items-center justify-between gap-1">
                <span className="text-xs font-bold truncate">{alert.title}</span>
                <span className="text-[10px] text-muted shrink-0 font-medium">Just now</span>
              </div>
              <p className="text-xs text-muted mt-0.5 leading-snug break-words">
                {alert.message}
              </p>
            </div>

            <button
              type="button"
              onClick={() => dismissAlert(alert.id)}
              className="text-slate-400 hover:text-slate-600 p-0.5 rounded transition-colors shrink-0"
              title="Dismiss"
            >
              <X className="w-4 h-4" />
            </button>
          </div>
        );
      })}
    </div>
  );
};
