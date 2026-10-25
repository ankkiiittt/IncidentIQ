// src/components/Badges.tsx
import React from 'react';
import { AlertCircle, Clock, ShieldAlert } from 'lucide-react';
import { IncidentPriority, IncidentSeverity, IncidentStatus, UserRole } from '../types';

export const PriorityBadge: React.FC<{ priority: IncidentPriority; size?: 'sm' | 'md' }> = ({
  priority,
  size = 'md',
}) => {
  const styles = {
    P1: 'bg-p1-bg text-p1-text border-red-200',
    P2: 'bg-p2-bg text-p2-text border-orange-200',
    P3: 'bg-p3-bg text-p3-text border-yellow-200',
    P4: 'bg-p4-bg text-p4-text border-slate-200',
  }[priority] || 'bg-slate-100 text-slate-700 border-slate-200';

  const sizeClass = size === 'sm' ? 'px-1.5 py-0.5 text-[11px]' : 'px-2 py-0.5 text-xs';

  return (
    <span className={`inline-flex items-center font-semibold rounded border tabular-nums ${styles} ${sizeClass}`}>
      {priority}
    </span>
  );
};

export const SeverityBadge: React.FC<{ severity: IncidentSeverity }> = ({
  severity,
}) => {
  const styles = {
    CRITICAL: 'bg-red-50 text-red-700 border-red-200',
    HIGH: 'bg-orange-50 text-orange-700 border-orange-200',
    MEDIUM: 'bg-yellow-50 text-yellow-800 border-yellow-200',
    LOW: 'bg-green-50 text-green-700 border-green-200',
  }[severity] || 'bg-slate-100 text-slate-700 border-slate-200';

  return (
    <span className={`inline-flex items-center px-2 py-0.5 text-xs font-medium rounded border ${styles}`}>
      {severity}
    </span>
  );
};

export const StatusChip: React.FC<{ status: IncidentStatus }> = ({ status }) => {
  const styles = {
    OPEN: 'bg-slate-50 text-slate-700 border-slate-300',
    ACKNOWLEDGED: 'bg-sky-50 text-sky-800 border-sky-300',
    IN_PROGRESS: 'bg-indigo-50 text-indigo-700 border-indigo-300',
    RESOLVED: 'bg-emerald-50 text-emerald-800 border-emerald-300',
    CLOSED: 'bg-slate-100 text-slate-500 border-slate-200',
  }[status] || 'bg-slate-100 text-slate-700 border-slate-200';

  return (
    <span className={`inline-flex items-center px-2.5 py-0.5 text-xs font-medium rounded-full border ${styles}`}>
      {status.replace('_', ' ')}
    </span>
  );
};

export const RoleBadge: React.FC<{ role: UserRole }> = ({ role }) => {
  const styles = {
    ADMIN: 'bg-purple-100 text-purple-800 border-purple-200',
    ENGINEER: 'bg-blue-100 text-blue-800 border-blue-200',
    SUPPORT: 'bg-emerald-100 text-emerald-800 border-emerald-200',
    VIEWER: 'bg-slate-100 text-slate-700 border-slate-200',
  }[role] || 'bg-slate-100 text-slate-700 border-slate-200';

  return (
    <span className={`inline-flex items-center px-2 py-0.5 text-[11px] font-semibold rounded border ${styles}`}>
      {role}
    </span>
  );
};

export const SlaPill: React.FC<{
  responseDueAt?: string;
  resolveDueAt?: string;
  breached: boolean;
  acknowledgedAt?: string;
  resolvedAt?: string;
}> = ({ responseDueAt, resolveDueAt, breached, acknowledgedAt, resolvedAt }) => {
  if (breached) {
    return (
      <span className="inline-flex items-center gap-1 px-2 py-0.5 text-xs font-semibold rounded bg-red-100 text-red-800 border border-red-300">
        <ShieldAlert className="w-3.5 h-3.5 text-red-700" />
        SLA BREACHED
      </span>
    );
  }

  // If resolved, SLA is satisfied
  if (resolvedAt) {
    return (
      <span className="inline-flex items-center gap-1 px-2 py-0.5 text-xs font-medium rounded bg-emerald-50 text-emerald-700 border border-emerald-200">
        SLA Met
      </span>
    );
  }

  // Calculate remaining time
  const targetDateStr = !acknowledgedAt && responseDueAt ? responseDueAt : resolveDueAt;
  if (!targetDateStr) {
    return (
      <span className="inline-flex items-center gap-1 px-2 py-0.5 text-xs text-muted">
        No SLA
      </span>
    );
  }

  const diffMs = new Date(targetDateStr).getTime() - Date.now();
  if (diffMs <= 0) {
    return (
      <span className="inline-flex items-center gap-1 px-2 py-0.5 text-xs font-semibold rounded bg-red-100 text-red-800 border border-red-300">
        <AlertCircle className="w-3.5 h-3.5 text-red-700" />
        Overdue
      </span>
    );
  }

  const diffMins = Math.floor(diffMs / 60000);
  const diffHours = Math.floor(diffMins / 60);
  const diffDays = Math.floor(diffHours / 24);

  let label = '';
  if (diffDays > 0) {
    label = `${diffDays}d ${diffHours % 24}h left`;
  } else if (diffHours > 0) {
    label = `${diffHours}h ${diffMins % 60}m left`;
  } else {
    label = `${diffMins}m left`;
  }

  const isWarning = diffMins < 30;

  return (
    <span
      className={`inline-flex items-center gap-1 px-2 py-0.5 text-xs font-medium rounded border tabular-nums ${
        isWarning
          ? 'bg-amber-50 text-amber-800 border-amber-300'
          : 'bg-slate-50 text-slate-700 border-slate-200'
      }`}
    >
      <Clock className="w-3 h-3 text-muted" />
      {label}
    </span>
  );
};

export const CategoryBadge: React.FC<{ category: string }> = ({ category }) => {
  const styles: Record<string, string> = {
    INFRASTRUCTURE: 'bg-purple-50 text-purple-700 border-purple-200',
    APPLICATION: 'bg-blue-50 text-blue-700 border-blue-200',
    DATABASE: 'bg-amber-50 text-amber-700 border-amber-200',
    SECURITY: 'bg-red-50 text-red-700 border-red-200',
    THIRD_PARTY: 'bg-emerald-50 text-emerald-700 border-emerald-200',
    NETWORK: 'bg-cyan-50 text-cyan-700 border-cyan-200',
  };

  const style = styles[category] || 'bg-slate-100 text-slate-700 border-slate-200';

  return (
    <span className={`inline-flex items-center px-2 py-0.5 text-xs font-medium rounded border ${style}`}>
      {category.replace('_', ' ')}
    </span>
  );
};

export const HealthStatusBadge: React.FC<{ status: 'HEALTHY' | 'DEGRADED' | 'CRITICAL' | string }> = ({ status }) => {
  const styles: Record<string, { bg: string; dot: string; label: string }> = {
    HEALTHY: { bg: 'bg-emerald-50 text-emerald-700 border-emerald-200', dot: 'bg-emerald-500', label: 'Healthy' },
    DEGRADED: { bg: 'bg-amber-50 text-amber-800 border-amber-300', dot: 'bg-amber-500', label: 'Degraded' },
    CRITICAL: { bg: 'bg-red-50 text-red-700 border-red-300', dot: 'bg-red-500 animate-ping', label: 'Critical' },
  };

  const config = styles[status] || { bg: 'bg-slate-50 text-slate-700 border-slate-200', dot: 'bg-slate-400', label: status };

  return (
    <span className={`inline-flex items-center gap-1.5 px-2.5 py-0.5 text-xs font-semibold rounded-full border ${config.bg}`}>
      <span className={`w-1.5 h-1.5 rounded-full ${config.dot}`} />
      {config.label}
    </span>
  );
};

export const RiskLevelBadge: React.FC<{ risk: 'LOW' | 'MEDIUM' | 'HIGH' | string; score?: number }> = ({ risk, score }) => {
  const styles: Record<string, string> = {
    HIGH: 'bg-red-100 text-red-800 border-red-300',
    MEDIUM: 'bg-amber-100 text-amber-800 border-amber-300',
    LOW: 'bg-emerald-100 text-emerald-800 border-emerald-300',
  };

  const style = styles[risk] || 'bg-slate-100 text-slate-700 border-slate-200';

  return (
    <span className={`inline-flex items-center gap-1 px-2 py-0.5 text-xs font-semibold rounded border ${style}`}>
      <span className="uppercase">{risk} Risk</span>
      {score !== undefined && (
        <span className="text-[11px] opacity-75 font-mono">({Math.round(score * 100)}%)</span>
      )}
    </span>
  );
};

