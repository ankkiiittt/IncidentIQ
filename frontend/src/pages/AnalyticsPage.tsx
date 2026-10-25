// src/pages/AnalyticsPage.tsx
import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  AlertTriangle,
  BarChart3,
  Brain,
  CheckCircle2,
  Clock,
  ExternalLink,
  Layers,
  RefreshCw,
  Table as TableIcon,
  ThumbsUp,
  TrendingUp,
} from 'lucide-react';
import {
  AiFeedbackMetrics,
  AnalyticsCauses,
  AnalyticsOverview,
  AnalyticsSla,
  AnalyticsVolume,
  getAnalyticsAiFeedback,
  getAnalyticsCauses,
  getAnalyticsOverview,
  getAnalyticsSla,
  getAnalyticsVolume,
} from '../api/analytics';
import { CategoryBadge, PriorityBadge } from '../components/Badges';

function formatMinutes(minutes: number): string {
  if (minutes < 60) {
    return `${Math.round(minutes * 10) / 10}m`;
  }
  const hours = Math.round((minutes / 60) * 10) / 10;
  return `${hours}h`;
}

function formatDate(iso: string): string {
  try {
    const d = new Date(iso);
    return d.toLocaleDateString(undefined, { month: 'short', day: 'numeric', year: 'numeric' });
  } catch {
    return iso;
  }
}

export const AnalyticsPage: React.FC = () => {
  const [days, setDays] = useState<number>(30);
  const [groupBy, setGroupBy] = useState<'day' | 'week'>('day');
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  // Analytics data state
  const [overview, setOverview] = useState<AnalyticsOverview | null>(null);
  const [volume, setVolume] = useState<AnalyticsVolume | null>(null);
  const [sla, setSla] = useState<AnalyticsSla | null>(null);
  const [causes, setCauses] = useState<AnalyticsCauses | null>(null);
  const [aiFeedback, setAiFeedback] = useState<AiFeedbackMetrics | null>(null);

  // Table view toggles for accessibility
  const [showVolumeTable, setShowVolumeTable] = useState<boolean>(false);
  const [showSlaTable, setShowSlaTable] = useState<boolean>(false);
  const [showCausesTable, setShowCausesTable] = useState<boolean>(false);
  const [showAiTable, setShowAiTable] = useState<boolean>(false);

  const fetchAnalytics = async () => {
    setIsLoading(true);
    setError(null);
    try {
      const [ov, vol, slaRes, cau, ai] = await Promise.all([
        getAnalyticsOverview(days),
        getAnalyticsVolume(days, groupBy),
        getAnalyticsSla(days),
        getAnalyticsCauses(days),
        getAnalyticsAiFeedback(days),
      ]);
      setOverview(ov);
      setVolume(vol);
      setSla(slaRes);
      setCauses(cau);
      setAiFeedback(ai);
    } catch (err: unknown) {
      console.error('Failed to load analytics:', err);
      setError('Unable to load analytics metrics. Please retry.');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchAnalytics();
  }, [days, groupBy]);

  return (
    <div className="space-y-6">
      {/* Page Header with Range Selectors */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 pb-4 border-b border-border">
        <div>
          <h1 className="text-xl font-bold text-text flex items-center gap-2.5">
            <BarChart3 className="w-5 h-5 text-primary" />
            Reliability & Incident Analytics
          </h1>
          <p className="text-xs text-muted mt-0.5">
            Comprehensive MTTR, MTTD, SLA compliance, recurrence detection, and AI triage effectiveness.
          </p>
        </div>

        {/* Range and Refresh Controls */}
        <div className="flex items-center gap-2.5 flex-wrap">
          <div className="inline-flex rounded-lg border border-border bg-surface p-0.5 text-xs font-medium">
            <button
              type="button"
              onClick={() => setDays(7)}
              className={`px-3 py-1.5 rounded-md transition-colors ${
                days === 7 ? 'bg-primary text-white font-semibold' : 'text-muted hover:text-text'
              }`}
            >
              7 Days
            </button>
            <button
              type="button"
              onClick={() => setDays(30)}
              className={`px-3 py-1.5 rounded-md transition-colors ${
                days === 30 ? 'bg-primary text-white font-semibold' : 'text-muted hover:text-text'
              }`}
            >
              30 Days
            </button>
            <button
              type="button"
              onClick={() => setDays(90)}
              className={`px-3 py-1.5 rounded-md transition-colors ${
                days === 90 ? 'bg-primary text-white font-semibold' : 'text-muted hover:text-text'
              }`}
            >
              90 Days
            </button>
          </div>

          <button
            type="button"
            onClick={fetchAnalytics}
            disabled={isLoading}
            className="p-1.5 rounded-md border border-border hover:bg-slate-50 text-muted hover:text-text transition-colors disabled:opacity-50"
            title="Refresh analytics data"
          >
            <RefreshCw className={`w-4 h-4 ${isLoading ? 'animate-spin' : ''}`} />
          </button>
        </div>
      </div>

      {error && (
        <div className="p-3.5 bg-red-50 border border-red-200 text-red-700 text-xs rounded-md flex items-center justify-between">
          <span>{error}</span>
          <button onClick={fetchAnalytics} className="underline font-semibold hover:text-red-900">
            Retry
          </button>
        </div>
      )}

      {/* Row 1: KPI Summary Cards (4 Cards) */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {/* MTTR */}
        <div className="bg-surface border border-border rounded-lg p-4 shadow-sm">
          <div className="flex items-center justify-between text-muted text-xs font-medium mb-1">
            <span>Mean Time to Resolution (MTTR)</span>
            <Clock className="w-4 h-4 text-blue-500" />
          </div>
          <div className="text-2xl font-bold font-mono text-text">
            {isLoading ? (
              <div className="h-8 w-20 bg-slate-100 rounded animate-pulse" />
            ) : (
              formatMinutes(overview?.mttrMinutes || 0)
            )}
          </div>
          <p className="text-[11px] text-muted mt-1">Average resolved duration across {days}-day window</p>
        </div>

        {/* MTTD */}
        <div className="bg-surface border border-border rounded-lg p-4 shadow-sm">
          <div className="flex items-center justify-between text-muted text-xs font-medium mb-1">
            <span>Mean Time to Detect (MTTD)</span>
            <TrendingUp className="w-4 h-4 text-emerald-500" />
          </div>
          <div className="text-2xl font-bold font-mono text-text">
            {isLoading ? (
              <div className="h-8 w-20 bg-slate-100 rounded animate-pulse" />
            ) : (
              formatMinutes(overview?.mttdMinutes || 0)
            )}
          </div>
          <p className="text-[11px] text-muted mt-1">Average detection lag from symptom onset</p>
        </div>

        {/* SLA Compliance */}
        <div className="bg-surface border border-border rounded-lg p-4 shadow-sm">
          <div className="flex items-center justify-between text-muted text-xs font-medium mb-1">
            <span>SLA Compliance Rate</span>
            <CheckCircle2
              className={`w-4 h-4 ${
                (overview?.slaComplianceRate || 0) >= 95
                  ? 'text-emerald-500'
                  : (overview?.slaComplianceRate || 0) >= 90
                  ? 'text-amber-500'
                  : 'text-red-500'
              }`}
            />
          </div>
          <div className="text-2xl font-bold font-mono text-text">
            {isLoading ? (
              <div className="h-8 w-20 bg-slate-100 rounded animate-pulse" />
            ) : (
              `${overview?.slaComplianceRate || 100}%`
            )}
          </div>
          <div className="flex items-center gap-1.5 mt-1">
            <span
              className={`inline-block w-2 h-2 rounded-full ${
                (overview?.slaComplianceRate || 0) >= 95
                  ? 'bg-emerald-500'
                  : (overview?.slaComplianceRate || 0) >= 90
                  ? 'bg-amber-500'
                  : 'bg-red-500'
              }`}
            />
            <span className="text-[11px] text-muted">Target: 95.0% adherence</span>
          </div>
        </div>

        {/* Incident Volume */}
        <div className="bg-surface border border-border rounded-lg p-4 shadow-sm">
          <div className="flex items-center justify-between text-muted text-xs font-medium mb-1">
            <span>Window Volume Activity</span>
            <Layers className="w-4 h-4 text-purple-500" />
          </div>
          <div className="text-2xl font-bold font-mono text-text">
            {isLoading ? (
              <div className="h-8 w-20 bg-slate-100 rounded animate-pulse" />
            ) : (
              overview?.totalCreated || 0
            )}
          </div>
          <p className="text-[11px] text-muted mt-1">
            <span className="font-semibold text-emerald-600">{overview?.totalResolved || 0} resolved</span>
            {' • '}
            <span className="font-semibold text-amber-600">{overview?.activeCount || 0} active</span>
          </p>
        </div>
      </div>

      {/* Row 2: 2x2 Interactive Analytics Charts Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Chart 1: Volume Over Time */}
        <div className="bg-surface border border-border rounded-lg p-5 shadow-sm flex flex-col justify-between">
          <div>
            <div className="flex items-center justify-between mb-2">
              <div>
                <h2 className="text-sm font-semibold text-text">Incident Volume Over Time</h2>
                <p className="text-xs text-muted">Daily distribution split across severity priorities (P1-P4)</p>
              </div>
              <div className="flex items-center gap-2">
                <div className="inline-flex rounded border border-border text-[11px]">
                  <button
                    type="button"
                    onClick={() => setGroupBy('day')}
                    className={`px-2 py-0.5 rounded-l ${groupBy === 'day' ? 'bg-primary text-white font-medium' : 'text-muted'}`}
                  >
                    Day
                  </button>
                  <button
                    type="button"
                    onClick={() => setGroupBy('week')}
                    className={`px-2 py-0.5 rounded-r ${groupBy === 'week' ? 'bg-primary text-white font-medium' : 'text-muted'}`}
                  >
                    Week
                  </button>
                </div>
                <button
                  type="button"
                  onClick={() => setShowVolumeTable(!showVolumeTable)}
                  className="p-1 rounded text-muted hover:text-text border border-border hover:bg-slate-50 text-xs flex items-center gap-1"
                  title="Toggle accessible data table"
                >
                  <TableIcon className="w-3.5 h-3.5" />
                  <span className="text-[10px] hidden sm:inline">{showVolumeTable ? 'Chart' : 'Table'}</span>
                </button>
              </div>
            </div>

            {showVolumeTable ? (
              <div className="mt-4 max-h-60 overflow-y-auto border border-border rounded">
                <table className="w-full text-left text-xs">
                  <thead className="bg-slate-50 border-b border-border text-muted font-medium">
                    <tr>
                      <th className="py-2 px-3">Date</th>
                      <th className="py-2 px-2 text-red-600">P1</th>
                      <th className="py-2 px-2 text-amber-600">P2</th>
                      <th className="py-2 px-2 text-blue-600">P3</th>
                      <th className="py-2 px-2 text-slate-600">P4</th>
                      <th className="py-2 px-3 text-right">Total</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-border">
                    {volume?.series.map((pt) => (
                      <tr key={pt.timestamp} className="hover:bg-slate-50">
                        <td className="py-1.5 px-3 font-mono text-[11px]">{pt.timestamp}</td>
                        <td className="py-1.5 px-2">{pt.p1}</td>
                        <td className="py-1.5 px-2">{pt.p2}</td>
                        <td className="py-1.5 px-2">{pt.p3}</td>
                        <td className="py-1.5 px-2">{pt.p4}</td>
                        <td className="py-1.5 px-3 text-right font-semibold">{pt.total}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            ) : (
              <div className="mt-4">
                {/* Visual Chart Bars */}
                <div className="h-48 flex items-end gap-1.5 pt-4 pb-2 border-b border-border overflow-x-auto">
                  {volume?.series.map((pt) => {
                    const maxVal = Math.max(1, ...volume.series.map((s) => s.total));
                    const heightPct = Math.max(8, (pt.total / maxVal) * 100);
                    return (
                      <div
                        key={pt.timestamp}
                        className="flex-1 min-w-[20px] flex flex-col items-center group relative h-full justify-end"
                      >
                        {/* Tooltip */}
                        <div className="absolute -top-10 hidden group-hover:flex flex-col items-center bg-slate-900 text-white text-[10px] py-1 px-2 rounded shadow-lg whitespace-nowrap z-20 pointer-events-none">
                          <span>{pt.timestamp}</span>
                          <span>
                            Total: {pt.total} (P1:{pt.p1} P2:{pt.p2})
                          </span>
                        </div>

                        {/* Stacked bar */}
                        <div
                          style={{ height: `${heightPct}%` }}
                          className="w-full rounded-t flex flex-col justify-end overflow-hidden transition-all duration-200 group-hover:brightness-95"
                        >
                          {pt.p1 > 0 && <div style={{ flex: pt.p1 }} className="bg-red-500 w-full" />}
                          {pt.p2 > 0 && <div style={{ flex: pt.p2 }} className="bg-amber-500 w-full" />}
                          {pt.p3 > 0 && <div style={{ flex: pt.p3 }} className="bg-blue-500 w-full" />}
                          {pt.p4 > 0 && <div style={{ flex: pt.p4 }} className="bg-slate-400 w-full" />}
                          {pt.total === 0 && <div className="bg-slate-100 w-full h-full" />}
                        </div>
                      </div>
                    );
                  })}
                </div>

                {/* Legend */}
                <div className="flex items-center justify-between text-xs text-muted mt-3">
                  <div className="flex items-center gap-3">
                    <span className="flex items-center gap-1.5">
                      <span className="w-2.5 h-2.5 rounded-sm bg-red-500 inline-block" /> P1 Critical
                    </span>
                    <span className="flex items-center gap-1.5">
                      <span className="w-2.5 h-2.5 rounded-sm bg-amber-500 inline-block" /> P2 High
                    </span>
                    <span className="flex items-center gap-1.5">
                      <span className="w-2.5 h-2.5 rounded-sm bg-blue-500 inline-block" /> P3 Medium
                    </span>
                    <span className="flex items-center gap-1.5">
                      <span className="w-2.5 h-2.5 rounded-sm bg-slate-400 inline-block" /> P4 Low
                    </span>
                  </div>
                  <span className="text-[11px] font-mono text-muted">
                    Total: {volume?.series.reduce((sum, s) => sum + s.total, 0) || 0}
                  </span>
                </div>
              </div>
            )}
          </div>
        </div>

        {/* Chart 2: SLA Performance Breakdown */}
        <div className="bg-surface border border-border rounded-lg p-5 shadow-sm flex flex-col justify-between">
          <div>
            <div className="flex items-center justify-between mb-2">
              <div>
                <h2 className="text-sm font-semibold text-text">SLA Compliance by Priority</h2>
                <p className="text-xs text-muted">Resolution time target adherence and breach rates</p>
              </div>
              <button
                type="button"
                onClick={() => setShowSlaTable(!showSlaTable)}
                className="p-1 rounded text-muted hover:text-text border border-border hover:bg-slate-50 text-xs flex items-center gap-1"
                title="Toggle accessible data table"
              >
                <TableIcon className="w-3.5 h-3.5" />
                <span className="text-[10px] hidden sm:inline">{showSlaTable ? 'Chart' : 'Table'}</span>
              </button>
            </div>

            {showSlaTable ? (
              <div className="mt-4 border border-border rounded">
                <table className="w-full text-left text-xs">
                  <thead className="bg-slate-50 border-b border-border text-muted font-medium">
                    <tr>
                      <th className="py-2 px-3">Priority</th>
                      <th className="py-2 px-3">Target</th>
                      <th className="py-2 px-3 text-emerald-600">Met</th>
                      <th className="py-2 px-3 text-red-600">Breached</th>
                      <th className="py-2 px-3 text-right">Compliance</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-border">
                    {sla?.byPriority.map((row) => (
                      <tr key={row.priority} className="hover:bg-slate-50">
                        <td className="py-2 px-3 font-semibold">
                          <PriorityBadge priority={row.priority} />
                        </td>
                        <td className="py-2 px-3 text-muted">{formatMinutes(row.targetMinutes)}</td>
                        <td className="py-2 px-3 font-semibold text-emerald-700">{row.metCount}</td>
                        <td className="py-2 px-3 font-semibold text-red-700">{row.breachedCount}</td>
                        <td className="py-2 px-3 text-right font-mono font-semibold">{row.complianceRate}%</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            ) : (
              <div className="mt-4 space-y-3.5">
                {sla?.byPriority.map((row) => (
                  <div key={row.priority} className="space-y-1">
                    <div className="flex items-center justify-between text-xs">
                      <div className="flex items-center gap-2">
                        <PriorityBadge priority={row.priority} />
                        <span className="text-muted text-[11px]">Target: {formatMinutes(row.targetMinutes)}</span>
                      </div>
                      <div className="flex items-center gap-3">
                        <span className="text-[11px] text-muted">
                          <span className="text-emerald-600 font-semibold">{row.metCount} Met</span>
                          {' / '}
                          <span className="text-red-600 font-semibold">{row.breachedCount} Breached</span>
                        </span>
                        <span className="font-mono font-bold text-xs text-text">{row.complianceRate}%</span>
                      </div>
                    </div>
                    {/* Progress Bar */}
                    <div className="w-full bg-slate-100 rounded-full h-2 overflow-hidden flex">
                      <div
                        style={{ width: `${row.complianceRate}%` }}
                        className={`h-full transition-all duration-300 ${
                          row.complianceRate >= 95
                            ? 'bg-emerald-500'
                            : row.complianceRate >= 80
                            ? 'bg-amber-500'
                            : 'bg-red-500'
                        }`}
                      />
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>

          <div className="pt-4 border-t border-border mt-4 flex items-center justify-between text-xs text-muted">
            <span>Overall SLA Compliance</span>
            <span className="font-mono font-bold text-sm text-text">{sla?.overallComplianceRate || 100}%</span>
          </div>
        </div>

        {/* Chart 3: Incident Category Distribution */}
        <div className="bg-surface border border-border rounded-lg p-5 shadow-sm flex flex-col justify-between">
          <div>
            <div className="flex items-center justify-between mb-2">
              <div>
                <h2 className="text-sm font-semibold text-text">Failure Category Distribution</h2>
                <p className="text-xs text-muted">Root-domain classification of incidents in window</p>
              </div>
              <button
                type="button"
                onClick={() => setShowCausesTable(!showCausesTable)}
                className="p-1 rounded text-muted hover:text-text border border-border hover:bg-slate-50 text-xs flex items-center gap-1"
                title="Toggle accessible data table"
              >
                <TableIcon className="w-3.5 h-3.5" />
                <span className="text-[10px] hidden sm:inline">{showCausesTable ? 'Chart' : 'Table'}</span>
              </button>
            </div>

            {showCausesTable ? (
              <div className="mt-4 border border-border rounded">
                <table className="w-full text-left text-xs">
                  <thead className="bg-slate-50 border-b border-border text-muted font-medium">
                    <tr>
                      <th className="py-2 px-3">Category</th>
                      <th className="py-2 px-3">Incidents</th>
                      <th className="py-2 px-3 text-right">Share</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-border">
                    {causes?.categoryDistribution.map((c) => (
                      <tr key={c.category} className="hover:bg-slate-50">
                        <td className="py-2 px-3">
                          <CategoryBadge category={c.category} />
                        </td>
                        <td className="py-2 px-3 font-semibold">{c.count}</td>
                        <td className="py-2 px-3 text-right font-mono">{c.percentage}%</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            ) : (
              <div className="mt-4 space-y-3">
                {causes?.categoryDistribution.length === 0 ? (
                  <div className="p-6 text-center text-xs text-muted">No categorized incidents in selected window.</div>
                ) : (
                  causes?.categoryDistribution.map((cat) => (
                    <div key={cat.category} className="space-y-1">
                      <div className="flex items-center justify-between text-xs">
                        <CategoryBadge category={cat.category} />
                        <div className="flex items-center gap-2">
                          <span className="text-muted text-[11px]">{cat.count} incidents</span>
                          <span className="font-mono font-semibold text-text">{cat.percentage}%</span>
                        </div>
                      </div>
                      <div className="w-full bg-slate-100 rounded-full h-2 overflow-hidden">
                        <div
                          style={{ width: `${cat.percentage}%` }}
                          className="h-full bg-primary rounded-full transition-all duration-300"
                        />
                      </div>
                    </div>
                  ))
                )}
              </div>
            )}
          </div>

          <div className="pt-4 border-t border-border mt-4 flex items-center justify-between text-xs text-muted">
            <span>Domain Categorization</span>
            <Link to="/incidents" className="text-primary hover:underline text-xs flex items-center gap-1 font-medium">
              Explore incident list
              <ExternalLink className="w-3 h-3" />
            </Link>
          </div>
        </div>

        {/* Chart 4: AI Intelligence & Feedback Panel */}
        <div className="bg-surface border border-border rounded-lg p-5 shadow-sm flex flex-col justify-between">
          <div>
            <div className="flex items-center justify-between mb-2">
              <div>
                <h2 className="text-sm font-semibold text-text flex items-center gap-2">
                  <Brain className="w-4 h-4 text-purple-600" />
                  AI Triage & Advisory Effectiveness
                </h2>
                <p className="text-xs text-muted">Grounded RAG accuracy, engineer ratings, and adoption metrics</p>
              </div>
              <button
                type="button"
                onClick={() => setShowAiTable(!showAiTable)}
                className="p-1 rounded text-muted hover:text-text border border-border hover:bg-slate-50 text-xs flex items-center gap-1"
                title="Toggle accessible data table"
              >
                <TableIcon className="w-3.5 h-3.5" />
                <span className="text-[10px] hidden sm:inline">{showAiTable ? 'Gauges' : 'Table'}</span>
              </button>
            </div>

            {showAiTable ? (
              <div className="mt-4 border border-border rounded">
                <table className="w-full text-left text-xs">
                  <thead className="bg-slate-50 border-b border-border text-muted font-medium">
                    <tr>
                      <th className="py-2 px-3">Metric</th>
                      <th className="py-2 px-3 text-right">Value</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-border">
                    <tr>
                      <td className="py-2 px-3 text-muted">Total Diagnostics Executed</td>
                      <td className="py-2 px-3 text-right font-mono font-semibold">{aiFeedback?.totalAnalyses || 0}</td>
                    </tr>
                    <tr>
                      <td className="py-2 px-3 text-muted">Helpful Ratings Rate</td>
                      <td className="py-2 px-3 text-right font-mono font-semibold text-emerald-600">
                        {aiFeedback?.helpfulRate || 0}% ({aiFeedback?.helpfulCount || 0} /{' '}
                        {(aiFeedback?.helpfulCount || 0) + (aiFeedback?.notHelpfulCount || 0)})
                      </td>
                    </tr>
                    <tr>
                      <td className="py-2 px-3 text-muted">Recommendation Acceptance Rate</td>
                      <td className="py-2 px-3 text-right font-mono font-semibold text-primary">
                        {aiFeedback?.acceptanceRate || 0}% ({aiFeedback?.acceptedCount || 0} /{' '}
                        {(aiFeedback?.acceptedCount || 0) + (aiFeedback?.rejectedCount || 0)})
                      </td>
                    </tr>
                    <tr>
                      <td className="py-2 px-3 text-muted">Average Grounding Confidence</td>
                      <td className="py-2 px-3 text-right font-mono font-semibold">
                        {Math.round((aiFeedback?.avgConfidence || 0) * 100)}%
                      </td>
                    </tr>
                    <tr>
                      <td className="py-2 px-3 text-muted">Average Pipeline Latency</td>
                      <td className="py-2 px-3 text-right font-mono font-semibold">{aiFeedback?.avgLatencyMs || 0} ms</td>
                    </tr>
                  </tbody>
                </table>
              </div>
            ) : (
              <div className="mt-4 grid grid-cols-2 gap-4">
                {/* Helpful Rating Metric */}
                <div className="p-3 bg-slate-50 rounded-lg border border-border">
                  <div className="flex items-center justify-between text-xs text-muted mb-1">
                    <span className="font-medium">Engineer Feedback</span>
                    <ThumbsUp className="w-3.5 h-3.5 text-emerald-600" />
                  </div>
                  <div className="text-xl font-bold font-mono text-emerald-700">{aiFeedback?.helpfulRate || 0}%</div>
                  <div className="flex items-center gap-1.5 text-[11px] text-muted mt-1">
                    <span>
                      {aiFeedback?.helpfulCount || 0} Helpful • {aiFeedback?.notHelpfulCount || 0} Negative
                    </span>
                  </div>
                </div>

                {/* Recommendation Acceptance Rate */}
                <div className="p-3 bg-slate-50 rounded-lg border border-border">
                  <div className="flex items-center justify-between text-xs text-muted mb-1">
                    <span className="font-medium">Remediation Adoption</span>
                    <CheckCircle2 className="w-3.5 h-3.5 text-primary" />
                  </div>
                  <div className="text-xl font-bold font-mono text-primary">{aiFeedback?.acceptanceRate || 0}%</div>
                  <div className="flex items-center gap-1.5 text-[11px] text-muted mt-1">
                    <span>
                      {aiFeedback?.acceptedCount || 0} Accepted • {aiFeedback?.rejectedCount || 0} Rejected
                    </span>
                  </div>
                </div>

                {/* Grounding Confidence */}
                <div className="p-3 bg-slate-50 rounded-lg border border-border">
                  <div className="text-xs text-muted font-medium mb-1">Mean Vector Confidence</div>
                  <div className="text-xl font-bold font-mono text-text">
                    {Math.round((aiFeedback?.avgConfidence || 0) * 100)}%
                  </div>
                  <p className="text-[11px] text-muted mt-1">Grounded chunk similarity score</p>
                </div>

                {/* Pipeline Latency */}
                <div className="p-3 bg-slate-50 rounded-lg border border-border">
                  <div className="text-xs text-muted font-medium mb-1">Pipeline Latency</div>
                  <div className="text-xl font-bold font-mono text-text">{aiFeedback?.avgLatencyMs || 0} ms</div>
                  <p className="text-[11px] text-muted mt-1">RAG retrieval + LLM synthesis</p>
                </div>
              </div>
            )}
          </div>

          <div className="pt-4 border-t border-border mt-4 text-[11px] text-muted">
            <span className="font-semibold text-amber-800">Advisory Safeguard: </span>
            Recommendations are suggestions; human operators maintain strict decision authority.
          </div>
        </div>
      </div>

      {/* Row 3: Recurring Causes & Failure Patterns Table */}
      <div className="bg-surface border border-border rounded-lg shadow-sm">
        <div className="p-4 border-b border-border flex items-center justify-between">
          <div>
            <h2 className="text-sm font-semibold text-text flex items-center gap-2">
              <AlertTriangle className="w-4 h-4 text-amber-600" />
              Recurring Failure Patterns & Hotspots
            </h2>
            <p className="text-xs text-muted mt-0.5">
              Services with 2 or more incidents in the same category within a 30-day window (SRE Recurrence Standard).
            </p>
          </div>
          <span className="text-xs font-mono font-medium text-muted">
            {causes?.recurringCauses.filter((rc) => rc.isRecurring).length || 0} Recurring Groups
          </span>
        </div>

        {causes?.recurringCauses.length === 0 ? (
          <div className="p-8 text-center text-xs text-muted">
            No recurring incident patterns identified in the selected {days}-day window.
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-slate-50 border-b border-border text-muted font-medium">
                <tr>
                  <th className="py-2.5 px-4">Service</th>
                  <th className="py-2.5 px-3">Failure Category</th>
                  <th className="py-2.5 px-3">Incident Count</th>
                  <th className="py-2.5 px-3">Pattern Status</th>
                  <th className="py-2.5 px-3">Latest Incident</th>
                  <th className="py-2.5 px-3">Last Occurred</th>
                  <th className="py-2.5 px-4 text-right">Action</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border">
                {causes?.recurringCauses.map((item) => (
                  <tr key={`${item.serviceId}-${item.category}`} className="hover:bg-slate-50 transition-colors">
                    <td className="py-3 px-4 font-semibold text-text">
                      <Link to={`/services`} className="hover:text-primary hover:underline">
                        {item.serviceName}
                      </Link>
                    </td>
                    <td className="py-3 px-3">
                      <CategoryBadge category={item.category} />
                    </td>
                    <td className="py-3 px-3 font-mono font-semibold text-text">
                      <span className="inline-flex items-center px-2 py-0.5 rounded-full bg-slate-100 text-slate-800 text-xs">
                        {item.incidentCount} incidents
                      </span>
                    </td>
                    <td className="py-3 px-3">
                      {item.isRecurring ? (
                        <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-semibold bg-amber-50 text-amber-800 border border-amber-200">
                          <AlertTriangle className="w-3 h-3 text-amber-600" />
                          Recurring Pattern
                        </span>
                      ) : (
                        <span className="inline-flex items-center px-2 py-0.5 rounded text-xs text-muted">
                          Isolated Event
                        </span>
                      )}
                    </td>
                    <td className="py-3 px-3 font-mono text-primary font-medium">
                      {item.lastIncidentNumber ? `#INC-${String(item.lastIncidentNumber).padStart(4, '0')}` : '—'}
                    </td>
                    <td className="py-3 px-3 text-muted text-[11px]">{formatDate(item.lastOccurredAt)}</td>
                    <td className="py-3 px-4 text-right">
                      <Link
                        to={`/incidents?q=${encodeURIComponent(item.serviceName)}`}
                        className="inline-flex items-center gap-1 text-xs font-medium text-primary hover:underline"
                      >
                        Inspect Incidents
                        <ExternalLink className="w-3 h-3" />
                      </Link>
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
