// src/pages/DashboardPage.tsx
import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import {
  Activity,
  AlertOctagon,
  AlertTriangle,
  ArrowRight,
  CheckCircle2,
  Clock,
  Layers,
  Loader2,
  Plus,
  RefreshCw,
  Server,
  ShieldAlert,
  Table as TableIcon,
  TrendingUp,
} from 'lucide-react';
import { getDashboardApi } from '../api/dashboard';
import { getActivePredictionsApi } from '../api/metrics';
import { PriorityBadge, RiskLevelBadge, SlaPill, StatusChip } from '../components/Badges';
import { useAuth } from '../context/AuthContext';

export const DashboardPage: React.FC = () => {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [showDataTable, setShowDataTable] = useState(false);
  const [hoveredTrendIndex, setHoveredTrendIndex] = useState<number | null>(null);

  const {
    data: dashboard,
    isLoading,
    isRefetching,
    refetch,
    error,
  } = useQuery({
    queryKey: ['dashboard-overview'],
    queryFn: getDashboardApi,
    refetchInterval: 30000, // refresh every 30s
  });

  const { data: activePredictions } = useQuery({
    queryKey: ['dashboard-active-predictions'],
    queryFn: () => getActivePredictionsApi(),
    refetchInterval: 30000,
  });

  const canCreate =
    user && (user.role === 'ADMIN' || user.role === 'ENGINEER' || user.role === 'SUPPORT');

  if (isLoading) {
    return (
      <div className="py-24 flex flex-col items-center justify-center text-muted space-y-3">
        <Loader2 className="w-8 h-8 animate-spin text-primary" />
        <p className="text-xs font-medium">Loading operational intelligence overview...</p>
      </div>
    );
  }

  if (error || !dashboard) {
    return (
      <div className="card-surface p-8 text-center space-y-3 border-red-200">
        <AlertTriangle className="w-8 h-8 text-red-500 mx-auto" />
        <h3 className="text-sm font-bold text-text">Failed to load dashboard overview</h3>
        <p className="text-xs text-muted max-w-sm mx-auto">
          {error instanceof Error ? error.message : 'An error occurred while fetching dashboard metrics.'}
        </p>
        <button
          onClick={() => refetch()}
          className="px-3 py-1.5 rounded bg-primary text-white text-xs font-semibold hover:bg-primary-hover"
        >
          Retry
        </button>
      </div>
    );
  }

  const { kpis, trend30Days, topAffectedServices, recentIncidents, serviceHealth } = dashboard;

  // Format MTTR
  const formatMttr = (minutes: number) => {
    if (!minutes || minutes <= 0) return '—';
    if (minutes < 60) return `${Math.round(minutes)}m`;
    const hrs = Math.floor(minutes / 60);
    const mins = Math.round(minutes % 60);
    return mins > 0 ? `${hrs}h ${mins}m` : `${hrs}h`;
  };

  // SVG Trend Chart Scaling
  const maxTrendVal = Math.max(
    ...trend30Days.map((d) => Math.max(d.createdCount, d.resolvedCount)),
    5
  );
  const chartHeight = 160;
  const chartWidth = 640;
  const paddingX = 30;
  const paddingY = 20;
  const plotWidth = chartWidth - paddingX * 2;
  const plotHeight = chartHeight - paddingY * 2;

  const getX = (index: number) =>
    paddingX + (index / Math.max(trend30Days.length - 1, 1)) * plotWidth;
  const getY = (val: number) =>
    chartHeight - paddingY - (val / maxTrendVal) * plotHeight;

  // Polyline points
  const createdPoints = trend30Days
    .map((d, i) => `${getX(i)},${getY(d.createdCount)}`)
    .join(' ');
  const resolvedPoints = trend30Days
    .map((d, i) => `${getX(i)},${getY(d.resolvedCount)}`)
    .join(' ');

  // Health calculation
  const healthPercent =
    serviceHealth.totalServices > 0
      ? Math.round((serviceHealth.healthyServices / serviceHealth.totalServices) * 100)
      : 100;

  return (
    <div className="space-y-6">
      {/* Header bar */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-xl font-bold tracking-tight text-text flex items-center gap-2">
            Operational Dashboard
            {isRefetching && <Loader2 className="w-3.5 h-3.5 animate-spin text-primary" />}
          </h1>
          <p className="text-xs text-muted mt-0.5">
            Real-time incident volume, SLA posture, and service health across all tiers
          </p>
        </div>

        <div className="flex items-center gap-2.5">
          <button
            onClick={() => refetch()}
            disabled={isRefetching}
            className="p-1.5 rounded-md border border-border text-muted hover:text-text hover:bg-slate-50 transition-colors"
            title="Refresh operational metrics"
          >
            <RefreshCw className={`w-4 h-4 ${isRefetching ? 'animate-spin' : ''}`} />
          </button>

          {canCreate && (
            <Link
              to="/incidents/new"
              className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-md bg-primary hover:bg-primary-hover text-white text-xs font-semibold shadow-sm transition-colors"
            >
              <Plus className="w-4 h-4" />
              New Incident
            </Link>
          )}
        </div>
      </div>

      {/* Row of 5 KPI Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-3.5">
        {/* Card 1: Active Incidents */}
        <div
          onClick={() => navigate('/incidents?status=OPEN')}
          className="card-surface p-4 space-y-2 hover:border-primary/40 cursor-pointer transition-all shadow-sm group"
        >
          <div className="flex items-center justify-between text-muted text-xs font-semibold">
            <span>Active Incidents</span>
            <Activity className="w-4 h-4 text-primary group-hover:scale-110 transition-transform" />
          </div>
          <div className="text-2xl font-bold text-text tabular-nums">{kpis.totalOpen}</div>
          <div className="flex items-center gap-1.5 pt-0.5 flex-wrap">
            <span className="px-1.5 py-0.5 text-[10px] font-bold rounded bg-red-100 text-red-700">
              P1: {kpis.openP1}
            </span>
            <span className="px-1.5 py-0.5 text-[10px] font-bold rounded bg-orange-100 text-orange-700">
              P2: {kpis.openP2}
            </span>
            <span className="px-1.5 py-0.5 text-[10px] font-semibold rounded bg-amber-100 text-amber-700">
              P3: {kpis.openP3}
            </span>
            <span className="px-1.5 py-0.5 text-[10px] font-semibold rounded bg-slate-100 text-slate-700">
              P4: {kpis.openP4}
            </span>
          </div>
        </div>

        {/* Card 2: Unassigned */}
        <div
          onClick={() => navigate('/incidents?status=OPEN')}
          className={`card-surface p-4 space-y-2 cursor-pointer transition-all shadow-sm ${
            kpis.unassigned > 0
              ? 'border-amber-300 bg-amber-50/20 hover:border-amber-400'
              : 'hover:border-slate-300'
          }`}
        >
          <div className="flex items-center justify-between text-muted text-xs font-semibold">
            <span>Unassigned Queue</span>
            <AlertTriangle
              className={`w-4 h-4 ${kpis.unassigned > 0 ? 'text-amber-600' : 'text-muted'}`}
            />
          </div>
          <div
            className={`text-2xl font-bold tabular-nums ${
              kpis.unassigned > 0 ? 'text-amber-700' : 'text-text'
            }`}
          >
            {kpis.unassigned}
          </div>
          <span className="text-[11px] text-muted block">
            {kpis.unassigned > 0 ? 'Awaiting triage / assignment' : 'All active incidents assigned'}
          </span>
        </div>

        {/* Card 3: SLA Posture */}
        <div
          onClick={() => navigate('/incidents')}
          className={`card-surface p-4 space-y-2 cursor-pointer transition-all shadow-sm ${
            kpis.slaBreached > 0
              ? 'border-red-300 bg-red-50/20 hover:border-red-400'
              : 'hover:border-slate-300'
          }`}
        >
          <div className="flex items-center justify-between text-muted text-xs font-semibold">
            <span>SLA Health</span>
            <ShieldAlert
              className={`w-4 h-4 ${kpis.slaBreached > 0 ? 'text-red-600' : 'text-emerald-600'}`}
            />
          </div>
          <div className="flex items-baseline gap-2">
            <span
              className={`text-2xl font-bold tabular-nums ${
                kpis.slaBreached > 0 ? 'text-red-700' : 'text-text'
              }`}
            >
              {kpis.slaBreached}
            </span>
            <span className="text-xs text-muted">breached</span>
          </div>
          <div className="text-[11px] text-muted flex items-center justify-between">
            <span>At risk (&lt; 1 hr):</span>
            <span className="font-semibold text-amber-700 tabular-nums">{kpis.slaAtRisk}</span>
          </div>
        </div>

        {/* Card 4: Resolved (7 Days) */}
        <div className="card-surface p-4 space-y-2 shadow-sm hover:border-slate-300 transition-colors">
          <div className="flex items-center justify-between text-muted text-xs font-semibold">
            <span>Resolved (7 Days)</span>
            <CheckCircle2 className="w-4 h-4 text-emerald-600" />
          </div>
          <div className="text-2xl font-bold text-text tabular-nums">{kpis.resolvedLast7Days}</div>
          <span className="text-[11px] text-emerald-700 font-medium block">
            Successfully remediated
          </span>
        </div>

        {/* Card 5: MTTR (30 Days) */}
        <div className="card-surface p-4 space-y-2 shadow-sm hover:border-slate-300 transition-colors">
          <div className="flex items-center justify-between text-muted text-xs font-semibold">
            <span>MTTR (30 Days)</span>
            <Clock className="w-4 h-4 text-primary" />
          </div>
          <div className="text-2xl font-bold text-text tabular-nums">
            {formatMttr(kpis.mttrMinutes)}
          </div>
          <span className="text-[11px] text-muted block">Mean time to resolution</span>
        </div>
      </div>

      {/* Main Grid: 8 Columns Trend Chart + 4 Columns Top Services & Health */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-5">
        {/* Left Column (8 cols): 30-Day Trend */}
        <div className="lg:col-span-8 card-surface p-5 space-y-4 shadow-sm flex flex-col justify-between">
          <div>
            <div className="flex items-center justify-between flex-wrap gap-2">
              <div>
                <h3 className="text-sm font-bold text-text flex items-center gap-2">
                  <TrendingUp className="w-4 h-4 text-primary" />
                  30-Day Incident Inflow vs Resolution
                </h3>
                <p className="text-[11px] text-muted mt-0.5">
                  Daily volume of newly created incidents compared to resolved incidents
                </p>
              </div>

              <div className="flex items-center gap-4">
                {/* Legend */}
                <div className="flex items-center gap-3 text-xs">
                  <span className="inline-flex items-center gap-1.5 text-text">
                    <span className="w-2.5 h-2.5 rounded-sm bg-[#4F46E5]" />
                    Created
                  </span>
                  <span className="inline-flex items-center gap-1.5 text-text">
                    <span className="w-2.5 h-2.5 rounded-sm bg-[#10B981]" />
                    Resolved
                  </span>
                </div>

                {/* Accessible Data Table Toggle (Section 8 UI-UX Brief) */}
                <button
                  onClick={() => setShowDataTable(!showDataTable)}
                  className="px-2 py-1 text-xs border border-border rounded hover:bg-slate-50 text-muted hover:text-text flex items-center gap-1.5 transition-colors"
                  aria-label="Toggle accessible data table"
                >
                  <TableIcon className="w-3.5 h-3.5" />
                  {showDataTable ? 'Show Chart' : 'Data Table'}
                </button>
              </div>
            </div>
          </div>

          {/* Chart View or Table View */}
          {showDataTable ? (
            <div className="max-h-64 overflow-y-auto border border-border rounded mt-2">
              <table className="w-full text-xs text-left">
                <thead className="bg-slate-50 border-b border-border text-muted font-semibold sticky top-0">
                  <tr>
                    <th className="py-2 px-3">Date</th>
                    <th className="py-2 px-3 text-right">Created</th>
                    <th className="py-2 px-3 text-right">Resolved</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-border">
                  {trend30Days.map((d) => (
                    <tr key={d.date} className="hover:bg-slate-50/60">
                      <td className="py-1.5 px-3 font-mono">{d.date}</td>
                      <td className="py-1.5 px-3 text-right font-semibold text-primary">
                        {d.createdCount}
                      </td>
                      <td className="py-1.5 px-3 text-right font-semibold text-emerald-600">
                        {d.resolvedCount}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : (
            <div className="relative w-full overflow-hidden mt-2">
              <svg
                viewBox={`0 0 ${chartWidth} ${chartHeight}`}
                className="w-full h-48 select-none"
                preserveAspectRatio="none"
              >
                {/* Grid horizontal lines */}
                {[0, 0.25, 0.5, 0.75, 1].map((ratio) => {
                  const y = chartHeight - paddingY - ratio * plotHeight;
                  const labelVal = Math.round(ratio * maxTrendVal);
                  return (
                    <g key={ratio}>
                      <line
                        x1={paddingX}
                        y1={y}
                        x2={chartWidth - paddingX}
                        y2={y}
                        stroke="#E2E8F0"
                        strokeDasharray="3 3"
                        strokeWidth="1"
                      />
                      <text
                        x={paddingX - 6}
                        y={y + 3}
                        fontSize="9"
                        fill="#94A3B8"
                        textAnchor="end"
                      >
                        {labelVal}
                      </text>
                    </g>
                  );
                })}

                {/* Polyline: Created (#4F46E5) */}
                <polyline
                  fill="none"
                  stroke="#4F46E5"
                  strokeWidth="2.5"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  points={createdPoints}
                />

                {/* Polyline: Resolved (#10B981) */}
                <polyline
                  fill="none"
                  stroke="#10B981"
                  strokeWidth="2.5"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  points={resolvedPoints}
                />

                {/* Hover dots & interactive vertical cursor */}
                {trend30Days.map((d, i) => {
                  const cx = getX(i);
                  const cyCreated = getY(d.createdCount);
                  const cyResolved = getY(d.resolvedCount);
                  const isHovered = hoveredTrendIndex === i;

                  return (
                    <g
                      key={d.date}
                      onMouseEnter={() => setHoveredTrendIndex(i)}
                      onMouseLeave={() => setHoveredTrendIndex(null)}
                      className="cursor-pointer"
                    >
                      {/* Invisible hover target bar */}
                      <rect
                        x={cx - 10}
                        y={paddingY}
                        width={20}
                        height={plotHeight}
                        fill="transparent"
                      />

                      {isHovered && (
                        <line
                          x1={cx}
                          y1={paddingY}
                          x2={cx}
                          y2={chartHeight - paddingY}
                          stroke="#94A3B8"
                          strokeWidth="1"
                          strokeDasharray="2 2"
                        />
                      )}

                      <circle
                        cx={cx}
                        cy={cyCreated}
                        r={isHovered ? 4.5 : 2.5}
                        fill="#4F46E5"
                        stroke="#FFFFFF"
                        strokeWidth="1.5"
                      />
                      <circle
                        cx={cx}
                        cy={cyResolved}
                        r={isHovered ? 4.5 : 2.5}
                        fill="#10B981"
                        stroke="#FFFFFF"
                        strokeWidth="1.5"
                      />
                    </g>
                  );
                })}
              </svg>

              {/* Hover Tooltip Overlay */}
              {hoveredTrendIndex !== null && trend30Days[hoveredTrendIndex] && (
                <div
                  className="absolute top-2 bg-slate-900/90 text-white text-[11px] p-2 rounded shadow-lg pointer-events-none transition-all font-mono"
                  style={{
                    left: `${Math.min(
                      Math.max((hoveredTrendIndex / (trend30Days.length - 1)) * 90, 5),
                      80
                    )}%`,
                  }}
                >
                  <div className="font-bold text-slate-300">
                    {trend30Days[hoveredTrendIndex].date}
                  </div>
                  <div className="flex items-center gap-2 mt-1">
                    <span className="text-indigo-400">
                      Created: {trend30Days[hoveredTrendIndex].createdCount}
                    </span>
                    <span className="text-emerald-400">
                      Resolved: {trend30Days[hoveredTrendIndex].resolvedCount}
                    </span>
                  </div>
                </div>
              )}

              {/* X-axis date labels */}
              <div className="flex justify-between text-[10px] text-muted font-mono mt-1 px-4">
                <span>{trend30Days[0]?.date || '30 days ago'}</span>
                <span>{trend30Days[Math.floor(trend30Days.length / 2)]?.date}</span>
                <span>{trend30Days[trend30Days.length - 1]?.date || 'Today'}</span>
              </div>
            </div>
          )}
        </div>

        {/* Right Column (4 cols): Top Affected Services & Service Health */}
        <div className="lg:col-span-4 space-y-4">
          {/* Active Predictive Alerts Panel (Tier 3 F11) */}
          {activePredictions && activePredictions.length > 0 && (
            <div className="card-surface p-4 border-l-4 border-l-red-500 space-y-3 bg-gradient-to-r from-red-50/60 to-white shadow-sm">
              <div className="flex items-center justify-between">
                <h3 className="text-xs font-bold uppercase tracking-wider text-red-900 flex items-center gap-1.5">
                  <ShieldAlert className="w-4 h-4 text-red-600 animate-pulse" />
                  Predictive Anomaly Alerts
                </h3>
                <span className="text-[10px] px-1.5 py-0.5 rounded-full bg-red-100 text-red-800 font-bold font-mono">
                  {activePredictions.length} Active
                </span>
              </div>

              <div className="space-y-2">
                {activePredictions.slice(0, 3).map((pred) => (
                  <div
                    key={pred.id}
                    onClick={() => navigate('/services')}
                    className="p-2.5 rounded bg-white border border-red-200/80 hover:border-red-400 cursor-pointer transition-all shadow-2xs"
                  >
                    <div className="flex items-center justify-between">
                      <span className="font-semibold text-xs text-text">{pred.serviceName}</span>
                      <RiskLevelBadge risk={pred.riskLevel} score={pred.anomalyScore} />
                    </div>
                    <p className="text-[11px] text-muted mt-1">
                      Predicted horizon: <strong>{pred.horizonMinutes}m</strong> • Model: {pred.modelVersion}
                    </p>
                  </div>
                ))}
              </div>

              <Link
                to="/services"
                className="text-[11px] font-semibold text-red-700 hover:text-red-900 flex items-center justify-end gap-1"
              >
                Inspect Telemetry Multiples <ArrowRight className="w-3 h-3" />
              </Link>
            </div>
          )}

          {/* Service Health Card */}
          <div className="card-surface p-4 space-y-3 shadow-sm">
            <div className="flex items-center justify-between">
              <h3 className="text-xs font-bold uppercase tracking-wider text-muted flex items-center gap-1.5">
                <Server className="w-3.5 h-3.5 text-primary" />
                Service Health Overview
              </h3>
              <Link
                to="/services"
                className="text-[11px] font-semibold text-primary hover:underline flex items-center gap-1"
              >
                Catalog <ArrowRight className="w-3 h-3" />
              </Link>
            </div>

            <div className="flex items-baseline justify-between">
              <span className="text-2xl font-bold text-text tabular-nums">{healthPercent}%</span>
              <span className="text-xs font-medium text-muted">
                {serviceHealth.healthyServices} / {serviceHealth.totalServices} operational
              </span>
            </div>

            {/* Progress bar */}
            <div className="w-full h-2 bg-slate-100 rounded-full overflow-hidden flex">
              <div
                style={{ width: `${healthPercent}%` }}
                className="bg-emerald-500 h-full transition-all duration-500"
              />
              {serviceHealth.degradedServices > 0 && (
                <div
                  style={{ width: `${100 - healthPercent}%` }}
                  className="bg-amber-500 h-full transition-all duration-500"
                />
              )}
            </div>

            <div className="flex items-center justify-between text-[11px] text-muted pt-1">
              <span className="flex items-center gap-1 text-emerald-700 font-medium">
                <CheckCircle2 className="w-3 h-3" />
                {serviceHealth.healthyServices} Healthy
              </span>
              {serviceHealth.degradedServices > 0 ? (
                <span className="flex items-center gap-1 text-amber-700 font-semibold">
                  <AlertOctagon className="w-3 h-3" />
                  {serviceHealth.degradedServices} Degraded
                </span>
              ) : (
                <span className="text-slate-400">0 Degraded</span>
              )}
            </div>
          </div>

          {/* Top Affected Services Ranking */}
          <div className="card-surface p-4 space-y-3 shadow-sm">
            <h3 className="text-xs font-bold uppercase tracking-wider text-muted flex items-center gap-1.5">
              <Layers className="w-3.5 h-3.5 text-primary" />
              Top Affected Services
            </h3>

            {topAffectedServices.length === 0 ? (
              <p className="text-xs text-muted py-4 text-center">No active service incidents</p>
            ) : (
              <div className="divide-y divide-border text-xs">
                {topAffectedServices.map((svc) => (
                  <div
                    key={svc.serviceId}
                    onClick={() => navigate(`/incidents?serviceId=${svc.serviceId}`)}
                    className="py-2.5 flex items-center justify-between hover:bg-slate-50/70 cursor-pointer px-1 -mx-1 rounded transition-colors"
                  >
                    <div className="space-y-0.5">
                      <div className="font-semibold text-text hover:text-primary transition-colors">
                        {svc.serviceName}
                      </div>
                      <span className="text-[10px] text-muted uppercase font-mono">
                        {svc.serviceTier}
                      </span>
                    </div>

                    <div className="text-right">
                      <div className="flex items-center gap-1.5 justify-end">
                        <span
                          className={`px-1.5 py-0.5 rounded text-[11px] font-bold tabular-nums ${
                            svc.activeIncidents > 0
                              ? 'bg-amber-100 text-amber-800'
                              : 'bg-slate-100 text-slate-600'
                          }`}
                        >
                          {svc.activeIncidents} active
                        </span>
                      </div>
                      <span className="text-[10px] text-muted block mt-0.5">
                        {svc.totalIncidents} total
                      </span>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Bottom Section: Recent Incidents Queue */}
      <div className="card-surface overflow-hidden shadow-sm space-y-0">
        <div className="p-4 border-b border-border flex items-center justify-between">
          <div>
            <h3 className="text-sm font-bold text-text">Recent Incidents Queue</h3>
            <p className="text-xs text-muted mt-0.5">
              Latest operational events reported across the platform
            </p>
          </div>
          <Link
            to="/incidents"
            className="text-xs font-semibold text-primary hover:text-primary-hover flex items-center gap-1 transition-colors"
          >
            View All Incidents <ArrowRight className="w-3.5 h-3.5" />
          </Link>
        </div>

        {recentIncidents.length === 0 ? (
          <div className="py-12 text-center text-muted space-y-2">
            <CheckCircle2 className="w-8 h-8 text-emerald-500 mx-auto" />
            <p className="text-xs font-medium">No incidents recorded yet.</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-slate-50 border-b border-border text-muted font-semibold uppercase tracking-wider text-[11px]">
                <tr>
                  <th className="py-3 px-4">Number</th>
                  <th className="py-3 px-4">Title</th>
                  <th className="py-3 px-3">Priority</th>
                  <th className="py-3 px-3">Status</th>
                  <th className="py-3 px-4">Service</th>
                  <th className="py-3 px-4">SLA Deadline</th>
                  <th className="py-3 px-4 text-right">Reported</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border">
                {recentIncidents.map((inc) => (
                  <tr
                    key={inc.id}
                    onClick={() => navigate(`/incidents/${inc.id}`)}
                    className="hover:bg-slate-50/80 cursor-pointer transition-colors"
                  >
                    <td className="py-3 px-4 font-mono font-medium text-primary">
                      {inc.formattedNumber}
                    </td>
                    <td className="py-3 px-4 font-medium text-text max-w-sm truncate">
                      <div className="font-semibold truncate">{inc.title}</div>
                      {inc.tags && inc.tags.length > 0 && (
                        <div className="flex gap-1 mt-0.5 flex-wrap">
                          {inc.tags.slice(0, 2).map((t) => (
                            <span
                              key={t}
                              className="inline-block px-1 py-0.1 text-[9px] rounded bg-slate-100 text-slate-600 font-mono"
                            >
                              #{t}
                            </span>
                          ))}
                        </div>
                      )}
                    </td>
                    <td className="py-3 px-3">
                      <PriorityBadge priority={inc.priority} />
                    </td>
                    <td className="py-3 px-3">
                      <StatusChip status={inc.status} />
                    </td>
                    <td className="py-3 px-4 text-text font-medium">{inc.serviceName || '—'}</td>
                    <td className="py-3 px-4">
                      <SlaPill
                        responseDueAt={inc.slaResponseDueAt}
                        resolveDueAt={inc.slaResolveDueAt}
                        breached={inc.slaBreached}
                      />
                    </td>
                    <td className="py-3 px-4 text-right text-muted tabular-nums whitespace-nowrap">
                      {new Date(inc.createdAt).toLocaleString(undefined, {
                        month: 'short',
                        day: 'numeric',
                        hour: '2-digit',
                        minute: '2-digit',
                      })}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
};
