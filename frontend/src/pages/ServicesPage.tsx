// frontend/src/pages/ServicesPage.tsx
import React, { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import {
  Activity,
  AlertOctagon,
  AlertTriangle,
  CheckCircle2,
  Clock,
  Cpu,
  ExternalLink,
  Flame,
  GitBranch,
  HardDrive,
  Layers,
  Loader2,
  Plus,
  RefreshCw,
  Search,
  Server,
  ShieldAlert,
  Table as TableIcon,
  X,
  Zap,
} from 'lucide-react';
import {
  createIncidentFromPredictionApi,
  dismissPredictionApi,
  getServiceHealthApi,
  getServicesHealthApi,
} from '../api/metrics';
import {
  createServiceApi,
  deactivateServiceApi,
  recordDeploymentApi,
} from '../api/services';
import { HealthStatusBadge, PriorityBadge, RiskLevelBadge } from '../components/Badges';
import { useAuth } from '../context/AuthContext';
import { ServiceCriticality, ServiceEnvironment } from '../types';

export const ServicesPage: React.FC = () => {
  const queryClient = useQueryClient();
  const navigate = useNavigate();
  const { user } = useAuth();

  const [selectedServiceId, setSelectedServiceId] = useState<string | null>(null);
  const [searchTerm, setSearchTerm] = useState('');
  const [filterHealth, setFilterHealth] = useState<'ALL' | 'CRITICAL' | 'DEGRADED' | 'HEALTHY'>('ALL');

  // Modals & States
  const [isAddServiceOpen, setIsAddServiceOpen] = useState(false);
  const [isRecordDepOpen, setIsRecordDepOpen] = useState(false);
  const [actionError, setActionError] = useState<string | null>(null);
  const [activeTableViews, setActiveTableViews] = useState<Record<string, boolean>>({});

  // New Service Form State
  const [newServiceName, setNewServiceName] = useState('');
  const [newServiceEnv, setNewServiceEnv] = useState<ServiceEnvironment>('PROD');
  const [newServiceOwner, setNewServiceOwner] = useState('');
  const [newServiceCrit, setNewServiceCrit] = useState<ServiceCriticality>('HIGH');
  const [newServiceDesc, setNewServiceDesc] = useState('');

  // New Deployment Form State
  const [depVersion, setDepVersion] = useState('');
  const [depBy, setDepBy] = useState('');
  const [depNotes, setDepNotes] = useState('');

  // 1. Fetch All Services Health Summaries
  const {
    data: servicesHealth,
    isLoading: healthLoading,
    isRefetching: healthRefetching,
    refetch: refetchHealth,
  } = useQuery({
    queryKey: ['services-health'],
    queryFn: getServicesHealthApi,
    refetchInterval: 30000,
  });

  // Set default selected service once loaded
  const servicesList = servicesHealth || [];
  const effectiveSelectedId = selectedServiceId || (servicesList.length > 0 ? servicesList[0].serviceId : null);

  // 2. Fetch Selected Service Detailed Health
  const {
    data: serviceDetail,
    isLoading: detailLoading,
    refetch: refetchDetail,
  } = useQuery({
    queryKey: ['service-health-detail', effectiveSelectedId],
    queryFn: () => getServiceHealthApi(effectiveSelectedId!),
    enabled: !!effectiveSelectedId,
    refetchInterval: 30000,
  });

  // Mutations
  const createServiceMutation = useMutation({
    mutationFn: createServiceApi,
    onSuccess: () => {
      setIsAddServiceOpen(false);
      resetServiceForm();
      setActionError(null);
      queryClient.invalidateQueries({ queryKey: ['services-health'] });
    },
    onError: (err: Error) => setActionError(err.message),
  });

  const deactivateMutation = useMutation({
    mutationFn: deactivateServiceApi,
    onSuccess: () => {
      setActionError(null);
      queryClient.invalidateQueries({ queryKey: ['services-health'] });
    },
    onError: (err: Error) => setActionError(err.message),
  });

  const recordDepMutation = useMutation({
    mutationFn: () =>
      recordDeploymentApi(effectiveSelectedId!, {
        version: depVersion.trim(),
        deployedBy: depBy.trim() || undefined,
        notes: depNotes.trim() || undefined,
      }),
    onSuccess: () => {
      setIsRecordDepOpen(false);
      setDepVersion('');
      setDepBy('');
      setDepNotes('');
      setActionError(null);
      queryClient.invalidateQueries({ queryKey: ['service-health-detail', effectiveSelectedId] });
    },
    onError: (err: Error) => setActionError(err.message),
  });

  const dismissPredictionMutation = useMutation({
    mutationFn: (predId: string) => dismissPredictionApi(predId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['service-health-detail', effectiveSelectedId] });
      queryClient.invalidateQueries({ queryKey: ['services-health'] });
    },
    onError: (err: Error) => setActionError(err.message),
  });

  const createIncidentMutation = useMutation({
    mutationFn: (predId: string) => createIncidentFromPredictionApi(predId),
    onSuccess: (incident) => {
      queryClient.invalidateQueries({ queryKey: ['service-health-detail', effectiveSelectedId] });
      queryClient.invalidateQueries({ queryKey: ['services-health'] });
      navigate(`/incidents/${incident.id}`);
    },
    onError: (err: Error) => setActionError(err.message),
  });

  const resetServiceForm = () => {
    setNewServiceName('');
    setNewServiceOwner('');
    setNewServiceDesc('');
    setNewServiceEnv('PROD');
    setNewServiceCrit('HIGH');
  };

  const toggleTableView = (metricKey: string) => {
    setActiveTableViews((prev) => ({
      ...prev,
      [metricKey]: !prev[metricKey],
    }));
  };

  // Filtered Services List
  const filteredServices = servicesList.filter((s) => {
    const matchesSearch =
      s.serviceName.toLowerCase().includes(searchTerm.toLowerCase()) ||
      s.ownerTeam.toLowerCase().includes(searchTerm.toLowerCase());
    const matchesHealth = filterHealth === 'ALL' || s.healthStatus === filterHealth;
    return matchesSearch && matchesHealth;
  });

  // Health summary metrics
  const totalCount = servicesList.length;
  const criticalCount = servicesList.filter((s) => s.healthStatus === 'CRITICAL').length;
  const degradedCount = servicesList.filter((s) => s.healthStatus === 'DEGRADED').length;
  const healthyCount = servicesList.filter((s) => s.healthStatus === 'HEALTHY').length;
  const totalActivePredictions = servicesList.reduce((acc, s) => acc + s.activePredictionCount, 0);

  const canEdit = user && (user.role === 'ADMIN' || user.role === 'ENGINEER');
  const isAdmin = user && user.role === 'ADMIN';

  return (
    <div className="space-y-6">
      {/* Top Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <h1 className="text-2xl font-bold tracking-tight text-text">Service Health & Anomaly Intelligence</h1>
            <span className="text-[10px] font-semibold tracking-wide uppercase px-2 py-0.5 rounded bg-primary/10 text-primary border border-primary/20">
              Tier 3 AIOps
            </span>
          </div>
          <p className="text-xs text-muted mt-1">
            Real-time telemetry ingestion, statistical baseline Z-scores, and predictive early-warning advisories.
          </p>
        </div>

        <div className="flex items-center gap-3">
          <button
            onClick={() => {
              refetchHealth();
              if (effectiveSelectedId) refetchDetail();
            }}
            disabled={healthRefetching}
            className="btn-secondary h-9 px-3 gap-1.5 text-xs"
            title="Refresh Service Telemetry"
          >
            <RefreshCw className={`w-3.5 h-3.5 ${healthRefetching ? 'animate-spin' : ''}`} />
            Refresh
          </button>

          {isAdmin && (
            <button
              onClick={() => setIsAddServiceOpen(true)}
              className="btn-primary h-9 px-3.5 gap-1.5 text-xs shadow-sm shadow-primary/20"
            >
              <Plus className="w-4 h-4" />
              Register Service
            </button>
          )}
        </div>
      </div>

      {actionError && (
        <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg text-xs flex items-center justify-between">
          <div className="flex items-center gap-2">
            <AlertTriangle className="w-4 h-4 text-red-500 shrink-0" />
            <span>{actionError}</span>
          </div>
          <button onClick={() => setActionError(null)} className="text-red-500 hover:text-red-700">
            <X className="w-4 h-4" />
          </button>
        </div>
      )}

      {/* 4 Overview Metric Cards */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        <div className="card-surface p-4 flex items-center justify-between border-slate-200/80">
          <div>
            <p className="text-[11px] font-medium text-muted uppercase tracking-wider">Registered Services</p>
            <p className="text-2xl font-bold text-text mt-1">{totalCount}</p>
            <p className="text-[11px] text-muted mt-0.5">{healthyCount} fully operational</p>
          </div>
          <div className="w-10 h-10 rounded-lg bg-indigo-50 border border-indigo-100 flex items-center justify-center text-primary">
            <Server className="w-5 h-5" />
          </div>
        </div>

        <div className="card-surface p-4 flex items-center justify-between border-slate-200/80">
          <div>
            <p className="text-[11px] font-medium text-muted uppercase tracking-wider">Healthy Status</p>
            <p className="text-2xl font-bold text-emerald-600 mt-1">
              {totalCount > 0 ? Math.round((healthyCount / totalCount) * 100) : 100}%
            </p>
            <p className="text-[11px] text-emerald-600/80 mt-0.5">Normal baseline telemetry</p>
          </div>
          <div className="w-10 h-10 rounded-lg bg-emerald-50 border border-emerald-100 flex items-center justify-center text-emerald-600">
            <CheckCircle2 className="w-5 h-5" />
          </div>
        </div>

        <div className="card-surface p-4 flex items-center justify-between border-slate-200/80">
          <div>
            <p className="text-[11px] font-medium text-muted uppercase tracking-wider">Degraded / Critical</p>
            <p className="text-2xl font-bold text-amber-600 mt-1">{criticalCount + degradedCount}</p>
            <p className="text-[11px] text-amber-700/80 mt-0.5">
              {criticalCount} critical • {degradedCount} degraded
            </p>
          </div>
          <div className="w-10 h-10 rounded-lg bg-amber-50 border border-amber-100 flex items-center justify-center text-amber-600">
            <AlertTriangle className="w-5 h-5" />
          </div>
        </div>

        <div className="card-surface p-4 flex items-center justify-between border-slate-200/80">
          <div>
            <p className="text-[11px] font-medium text-muted uppercase tracking-wider">Predictive Alerts</p>
            <p className="text-2xl font-bold text-purple-700 mt-1">{totalActivePredictions}</p>
            <p className="text-[11px] text-purple-700/80 mt-0.5">Anomaly score &ge; 0.50</p>
          </div>
          <div className="w-10 h-10 rounded-lg bg-purple-50 border border-purple-100 flex items-center justify-center text-purple-700">
            <ShieldAlert className="w-5 h-5" />
          </div>
        </div>
      </div>

      {/* Main Two-Column Layout */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
        {/* Left Column: Services Selector & Health Status (4 columns) */}
        <div className="lg:col-span-4 space-y-4">
          <div className="card-surface p-3 space-y-3">
            {/* Filter Bar */}
            <div className="relative">
              <Search className="w-3.5 h-3.5 text-muted absolute left-3 top-1/2 -translate-y-1/2" />
              <input
                type="text"
                placeholder="Search services or owner team..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className="input-field pl-8 h-8 text-xs w-full"
              />
            </div>

            {/* Quick Status Pill Tabs */}
            <div className="flex items-center gap-1 p-0.5 bg-slate-100/80 rounded-md text-[11px]">
              {(['ALL', 'CRITICAL', 'DEGRADED', 'HEALTHY'] as const).map((tab) => (
                <button
                  key={tab}
                  onClick={() => setFilterHealth(tab)}
                  className={`flex-1 py-1 rounded font-medium transition-colors ${
                    filterHealth === tab
                      ? 'bg-white text-text shadow-sm'
                      : 'text-muted hover:text-text'
                  }`}
                >
                  {tab}
                </button>
              ))}
            </div>

            {/* Services List */}
            {healthLoading ? (
              <div className="py-12 flex flex-col items-center justify-center text-muted space-y-2">
                <Loader2 className="w-6 h-6 animate-spin text-primary" />
                <span className="text-xs">Loading services telemetry...</span>
              </div>
            ) : filteredServices.length === 0 ? (
              <div className="py-10 text-center text-muted text-xs">
                No services found matching criteria.
              </div>
            ) : (
              <div className="divide-y divide-slate-100 max-h-[640px] overflow-y-auto pr-1">
                {filteredServices.map((service) => {
                  const isSelected = service.serviceId === effectiveSelectedId;
                  const cpuVal = service.latestMetrics['CPU_PERCENT'];
                  const latVal = service.latestMetrics['LATENCY_P95_MS'];

                  return (
                    <div
                      key={service.serviceId}
                      onClick={() => setSelectedServiceId(service.serviceId)}
                      className={`p-3 rounded-lg cursor-pointer transition-all mt-1 first:mt-0 ${
                        isSelected
                          ? 'bg-primary/5 border border-primary/20 shadow-sm'
                          : 'hover:bg-slate-50 border border-transparent'
                      }`}
                    >
                      <div className="flex items-start justify-between gap-2">
                        <div>
                          <div className="flex items-center gap-1.5">
                            <span className="font-semibold text-xs text-text">{service.serviceName}</span>
                            <span className="text-[10px] px-1 py-0.2 bg-slate-100 text-slate-600 rounded">
                              {service.environment}
                            </span>
                          </div>
                          <p className="text-[11px] text-muted mt-0.5">{service.ownerTeam}</p>
                        </div>
                        <HealthStatusBadge status={service.healthStatus} />
                      </div>

                      <div className="flex items-center justify-between mt-2 pt-2 border-t border-slate-100/60 text-[11px]">
                        <div className="flex items-center gap-3">
                          {cpuVal !== undefined && (
                            <span className="text-muted">
                              CPU: <strong className="text-text font-mono">{cpuVal.toFixed(1)}%</strong>
                            </span>
                          )}
                          {latVal !== undefined && (
                            <span className="text-muted">
                              p95: <strong className="text-text font-mono">{Math.round(latVal)}ms</strong>
                            </span>
                          )}
                        </div>

                        <div className="flex items-center gap-1.5">
                          {service.activePredictionCount > 0 && (
                            <RiskLevelBadge risk={service.riskLevel} />
                          )}
                          {service.openIncidentCount > 0 && (
                            <span className="px-1.5 py-0.5 rounded bg-red-50 text-red-700 text-[10px] font-bold border border-red-200">
                              {service.openIncidentCount} inc
                            </span>
                          )}
                        </div>
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        </div>

        {/* Right Column: Selected Service Detailed Screen S8 (8 columns) */}
        <div className="lg:col-span-8 space-y-6">
          {detailLoading || !serviceDetail ? (
            <div className="card-surface p-16 flex flex-col items-center justify-center text-muted space-y-3">
              <Loader2 className="w-8 h-8 animate-spin text-primary" />
              <p className="text-xs font-medium">Loading telemetry small multiples & predictive models...</p>
            </div>
          ) : (
            <>
              {/* Service Header Info Card */}
              <div className="card-surface p-5 space-y-4">
                <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
                  <div>
                    <div className="flex items-center gap-2">
                      <h2 className="text-lg font-bold text-text">{serviceDetail.serviceName}</h2>
                      <HealthStatusBadge status={serviceDetail.healthStatus} />
                      {serviceDetail.activePredictions.length > 0 && (
                        <RiskLevelBadge
                          risk={serviceDetail.riskLevel}
                          score={serviceDetail.activePredictions[0].anomalyScore}
                        />
                      )}
                    </div>
                    <div className="flex items-center gap-4 text-xs text-muted mt-1.5">
                      <span>
                        Environment: <strong className="text-text font-medium">{serviceDetail.environment}</strong>
                      </span>
                      <span>•</span>
                      <span>
                        Team: <strong className="text-text font-medium">{serviceDetail.ownerTeam}</strong>
                      </span>
                      <span>•</span>
                      <span>
                        Criticality: <strong className="text-text font-medium">{serviceDetail.criticality}</strong>
                      </span>
                    </div>
                  </div>

                  <div className="flex items-center gap-2">
                    {canEdit && (
                      <button
                        onClick={() => setIsRecordDepOpen(true)}
                        className="btn-secondary h-8 px-3 text-xs gap-1.5"
                      >
                        <GitBranch className="w-3.5 h-3.5" />
                        Record Deployment
                      </button>
                    )}
                    {isAdmin && (
                      <button
                        onClick={() => {
                          if (confirm(`Deactivate service ${serviceDetail.serviceName}?`)) {
                            deactivateMutation.mutate(serviceDetail.serviceId);
                          }
                        }}
                        className="btn-secondary h-8 px-2.5 text-xs text-red-600 hover:bg-red-50"
                        title="Deactivate Service"
                      >
                        Deactivate
                      </button>
                    )}
                  </div>
                </div>
              </div>

              {/* Active Predictive Anomalies Alert (Feature F11 / Tier 3 Expert) */}
              {serviceDetail.activePredictions.length > 0 && (
                <div className="p-4 rounded-xl border border-red-200 bg-gradient-to-r from-red-50/80 via-white to-amber-50/80 space-y-3 shadow-sm">
                  <div className="flex items-start justify-between gap-3">
                    <div className="flex items-start gap-2.5">
                      <div className="w-8 h-8 rounded-lg bg-red-100 text-red-700 flex items-center justify-center shrink-0 mt-0.5">
                        <Flame className="w-4 h-4 animate-bounce" />
                      </div>
                      <div>
                        <div className="flex items-center gap-2">
                          <h3 className="text-sm font-bold text-red-900">
                            Predictive Failure Anomaly Detected
                          </h3>
                          <span className="text-[10px] font-mono px-2 py-0.5 bg-red-100 text-red-800 rounded-full font-bold">
                            Score: {Math.round(serviceDetail.activePredictions[0].anomalyScore * 100)}%
                          </span>
                        </div>
                        <p className="text-xs text-red-800/90 mt-0.5">
                          Statistical detector identified telemetry deviation exceeding 3&sigma; baseline threshold.
                          Predicted horizon: <strong>{serviceDetail.activePredictions[0].horizonMinutes} minutes</strong>.
                        </p>
                      </div>
                    </div>

                    <div className="flex items-center gap-2 shrink-0">
                      {canEdit && (
                        <>
                          <button
                            onClick={() => createIncidentMutation.mutate(serviceDetail.activePredictions[0].id)}
                            disabled={createIncidentMutation.isPending}
                            className="btn-primary h-8 px-3 text-xs gap-1.5 bg-red-600 hover:bg-red-700 border-red-700 shadow-sm"
                          >
                            <AlertTriangle className="w-3.5 h-3.5" />
                            {createIncidentMutation.isPending ? 'Drafting...' : '1-Click Create Incident'}
                          </button>
                          <button
                            onClick={() => dismissPredictionMutation.mutate(serviceDetail.activePredictions[0].id)}
                            disabled={dismissPredictionMutation.isPending}
                            className="btn-secondary h-8 px-2.5 text-xs text-slate-600 hover:bg-slate-100"
                            title="Dismiss false positive"
                          >
                            Dismiss
                          </button>
                        </>
                      )}
                    </div>
                  </div>

                  {/* Contributing telemetry factors */}
                  {serviceDetail.activePredictions[0].contributingMetrics && (
                    <div className="pt-2 border-t border-red-100 flex flex-wrap gap-2 items-center text-[11px]">
                      <span className="text-red-900/80 font-medium">Anomalous Drivers:</span>
                      {Object.entries(serviceDetail.activePredictions[0].contributingMetrics).map(
                        ([metric, info]: [string, any]) => (
                          <span
                            key={metric}
                            className={`px-2 py-0.5 rounded font-mono border ${
                              info.status === 'CRITICAL'
                                ? 'bg-red-100 text-red-800 border-red-300 font-bold'
                                : 'bg-amber-100 text-amber-800 border-amber-200'
                            }`}
                          >
                            {metric}: {info.value} ({info.status}, z: {info.zScore})
                          </span>
                        )
                      )}
                    </div>
                  )}
                </div>
              )}

              {/* 2 by 3 Telemetry Small Multiples (Screen S8 Requirement) */}
              <div className="space-y-3">
                <div className="flex items-center justify-between">
                  <div>
                    <h3 className="text-sm font-bold text-text">Telemetry Small Multiples (24h Trend)</h3>
                    <p className="text-[11px] text-muted">
                      Continuous operational metrics with baseline z-score monitoring.
                    </p>
                  </div>
                  <span className="text-[11px] text-muted font-medium">6 Small Multiples Grid</span>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  {serviceDetail.metricSeries.map((series) => {
                    const isTableView = activeTableViews[series.metricName] || false;
                    const hasData = series.points.length > 0;
                    const icon = getMetricIcon(series.metricName);

                    return (
                      <div
                        key={series.metricName}
                        className="card-surface p-4 space-y-3 border-slate-200/90 relative"
                      >
                        {/* Metric Header */}
                        <div className="flex items-center justify-between">
                          <div className="flex items-center gap-2">
                            <div className="w-7 h-7 rounded-md bg-slate-100 text-slate-700 flex items-center justify-center">
                              {icon}
                            </div>
                            <div>
                              <h4 className="text-xs font-bold text-text">{series.displayName}</h4>
                              <p className="text-[10px] text-muted font-mono">{series.metricName}</p>
                            </div>
                          </div>

                          <div className="flex items-center gap-2">
                            {/* Accessible Table View Toggle (WCAG AA requirement) */}
                            <button
                              onClick={() => toggleTableView(series.metricName)}
                              className="btn-secondary h-6 px-2 text-[10px] gap-1"
                              aria-label={`Toggle accessible table view for ${series.displayName}`}
                            >
                              <TableIcon className="w-3 h-3" />
                              {isTableView ? 'Chart' : 'Table'}
                            </button>

                            <div className="text-right">
                              <span className="text-base font-bold text-text tabular-nums font-mono">
                                {series.current !== null ? series.current.toFixed(1) : '--'}
                              </span>
                              <span className="text-xs text-muted ml-0.5">{series.unit}</span>
                            </div>
                          </div>
                        </div>

                        {/* Metric Stats Banner */}
                        <div className="flex items-center justify-between px-2.5 py-1.5 bg-slate-50/80 rounded border border-slate-100 text-[11px] text-muted">
                          <span>
                            Min: <strong className="text-text font-mono">{series.min ?? '--'}</strong>
                          </span>
                          <span>
                            Avg: <strong className="text-text font-mono">{series.avg ?? '--'}</strong>
                          </span>
                          <span>
                            Max: <strong className="text-text font-mono">{series.max ?? '--'}</strong>
                          </span>
                        </div>

                        {/* Chart or Accessible Data Table */}
                        {isTableView ? (
                          <div className="max-h-36 overflow-y-auto border border-slate-200 rounded text-[11px]">
                            <table className="w-full text-left divide-y divide-slate-100">
                              <caption className="sr-only">{series.displayName} Telemetry Data Table</caption>
                              <thead className="bg-slate-50 font-semibold text-muted sticky top-0">
                                <tr>
                                  <th className="p-1.5">Timestamp</th>
                                  <th className="p-1.5 text-right">Value ({series.unit})</th>
                                </tr>
                              </thead>
                              <tbody className="divide-y divide-slate-100 font-mono">
                                {series.points.length === 0 ? (
                                  <tr>
                                    <td colSpan={2} className="p-3 text-center text-muted">
                                      No data points recorded.
                                    </td>
                                  </tr>
                                ) : (
                                  series.points.map((pt, idx) => (
                                    <tr key={idx} className="hover:bg-slate-50">
                                      <td className="p-1.5 text-slate-600">
                                        {new Date(pt.timestamp).toLocaleTimeString([], {
                                          hour: '2-digit',
                                          minute: '2-digit',
                                        })}
                                      </td>
                                      <td className="p-1.5 text-right font-bold text-text">
                                        {pt.value.toFixed(2)}
                                      </td>
                                    </tr>
                                  ))
                                )}
                              </tbody>
                            </table>
                          </div>
                        ) : !hasData ? (
                          <div className="h-28 flex items-center justify-center text-muted text-xs border border-dashed border-slate-200 rounded-lg">
                            No telemetry ingested for {series.displayName}
                          </div>
                        ) : (
                          <div className="h-28 w-full pt-1">
                            <MiniSparkline
                              points={series.points.map((p) => p.value)}
                              metricName={series.metricName}
                            />
                          </div>
                        )}
                      </div>
                    );
                  })}
                </div>
              </div>

              {/* Deployment & Incident Correlation Section */}
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {/* Recent Deployments */}
                <div className="card-surface p-4 space-y-3">
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-1.5">
                      <GitBranch className="w-4 h-4 text-primary" />
                      <h4 className="text-xs font-bold text-text">Recent Deployments</h4>
                    </div>
                    <span className="text-[10px] text-muted">
                      {serviceDetail.recentDeployments.length} logged
                    </span>
                  </div>

                  {serviceDetail.recentDeployments.length === 0 ? (
                    <p className="text-xs text-muted py-4 text-center">No deployments recorded yet.</p>
                  ) : (
                    <div className="space-y-2">
                      {serviceDetail.recentDeployments.map((dep) => (
                        <div
                          key={dep.id}
                          className="p-2.5 rounded bg-slate-50 border border-slate-100 flex items-center justify-between text-xs"
                        >
                          <div>
                            <span className="font-mono font-bold text-text">{dep.version}</span>
                            {dep.notes && <p className="text-[11px] text-muted mt-0.5">{dep.notes}</p>}
                          </div>
                          <span className="text-[10px] text-muted font-mono">
                            {new Date(dep.deployedAt).toLocaleDateString()}
                          </span>
                        </div>
                      ))}
                    </div>
                  )}
                </div>

                {/* Open Incidents for this service */}
                <div className="card-surface p-4 space-y-3">
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-1.5">
                      <AlertOctagon className="w-4 h-4 text-red-500" />
                      <h4 className="text-xs font-bold text-text">Active Service Incidents</h4>
                    </div>
                    <span className="text-[10px] text-muted">
                      {serviceDetail.openIncidents.length} active
                    </span>
                  </div>

                  {serviceDetail.openIncidents.length === 0 ? (
                    <p className="text-xs text-emerald-600 font-medium py-4 text-center">
                      No active incidents currently affecting this service.
                    </p>
                  ) : (
                    <div className="space-y-2">
                      {serviceDetail.openIncidents.map((inc) => (
                        <div
                          key={inc.id}
                          onClick={() => navigate(`/incidents/${inc.id}`)}
                          className="p-2.5 rounded bg-red-50/50 border border-red-100 hover:bg-red-50 cursor-pointer transition-colors flex items-center justify-between text-xs"
                        >
                          <div className="flex items-center gap-2">
                            <PriorityBadge priority={inc.priority as any} size="sm" />
                            <span className="font-medium text-text truncate max-w-[200px]">{inc.title}</span>
                          </div>
                          <ExternalLink className="w-3.5 h-3.5 text-muted hover:text-text" />
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              </div>
            </>
          )}
        </div>
      </div>

      {/* Register Service Modal */}
      {isAddServiceOpen && (
        <div className="fixed inset-0 z-50 bg-slate-900/40 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="card-surface max-w-md w-full p-6 space-y-4 shadow-xl">
            <div className="flex items-center justify-between">
              <h3 className="text-base font-bold text-text">Register New Service</h3>
              <button onClick={() => setIsAddServiceOpen(false)} className="text-muted hover:text-text">
                <X className="w-4 h-4" />
              </button>
            </div>

            <div className="space-y-3 text-xs">
              <div>
                <label className="block text-muted font-medium mb-1">Service Name *</label>
                <input
                  type="text"
                  placeholder="e.g. checkout-api"
                  value={newServiceName}
                  onChange={(e) => setNewServiceName(e.target.value)}
                  className="input-field w-full text-xs"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-muted font-medium mb-1">Environment</label>
                  <select
                    value={newServiceEnv}
                    onChange={(e) => setNewServiceEnv(e.target.value as ServiceEnvironment)}
                    className="input-field w-full text-xs"
                  >
                    <option value="PROD">PROD</option>
                    <option value="STAGING">STAGING</option>
                    <option value="DEV">DEV</option>
                  </select>
                </div>

                <div>
                  <label className="block text-muted font-medium mb-1">Criticality</label>
                  <select
                    value={newServiceCrit}
                    onChange={(e) => setNewServiceCrit(e.target.value as ServiceCriticality)}
                    className="input-field w-full text-xs"
                  >
                    <option value="CRITICAL">CRITICAL</option>
                    <option value="HIGH">HIGH</option>
                    <option value="MEDIUM">MEDIUM</option>
                    <option value="LOW">LOW</option>
                  </select>
                </div>
              </div>

              <div>
                <label className="block text-muted font-medium mb-1">Owner Team *</label>
                <input
                  type="text"
                  placeholder="e.g. Checkout SRE"
                  value={newServiceOwner}
                  onChange={(e) => setNewServiceOwner(e.target.value)}
                  className="input-field w-full text-xs"
                />
              </div>

              <div>
                <label className="block text-muted font-medium mb-1">Description</label>
                <textarea
                  rows={2}
                  placeholder="Service architectural purpose..."
                  value={newServiceDesc}
                  onChange={(e) => setNewServiceDesc(e.target.value)}
                  className="input-field w-full text-xs"
                />
              </div>
            </div>

            <div className="flex justify-end gap-2 pt-2">
              <button onClick={() => setIsAddServiceOpen(false)} className="btn-secondary h-8 px-3 text-xs">
                Cancel
              </button>
              <button
                onClick={() =>
                  createServiceMutation.mutate({
                    name: newServiceName.trim(),
                    environment: newServiceEnv,
                    ownerTeam: newServiceOwner.trim(),
                    criticality: newServiceCrit,
                    description: newServiceDesc.trim() || undefined,
                  })
                }
                disabled={!newServiceName.trim() || !newServiceOwner.trim() || createServiceMutation.isPending}
                className="btn-primary h-8 px-4 text-xs"
              >
                {createServiceMutation.isPending ? 'Registering...' : 'Register Service'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Record Deployment Modal */}
      {isRecordDepOpen && (
        <div className="fixed inset-0 z-50 bg-slate-900/40 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="card-surface max-w-md w-full p-6 space-y-4 shadow-xl">
            <div className="flex items-center justify-between">
              <h3 className="text-base font-bold text-text">Record Service Deployment</h3>
              <button onClick={() => setIsRecordDepOpen(false)} className="text-muted hover:text-text">
                <X className="w-4 h-4" />
              </button>
            </div>

            <div className="space-y-3 text-xs">
              <div>
                <label className="block text-muted font-medium mb-1">Release Version *</label>
                <input
                  type="text"
                  placeholder="e.g. v2.4.1 or sha-8fd2"
                  value={depVersion}
                  onChange={(e) => setDepVersion(e.target.value)}
                  className="input-field w-full text-xs"
                />
              </div>

              <div>
                <label className="block text-muted font-medium mb-1">Deployed By</label>
                <input
                  type="text"
                  placeholder="e.g. GitHub Actions / Alice"
                  value={depBy}
                  onChange={(e) => setDepBy(e.target.value)}
                  className="input-field w-full text-xs"
                />
              </div>

              <div>
                <label className="block text-muted font-medium mb-1">Release Notes</label>
                <textarea
                  rows={2}
                  placeholder="Changelog, PR links, configuration changes..."
                  value={depNotes}
                  onChange={(e) => setDepNotes(e.target.value)}
                  className="input-field w-full text-xs"
                />
              </div>
            </div>

            <div className="flex justify-end gap-2 pt-2">
              <button onClick={() => setIsRecordDepOpen(false)} className="btn-secondary h-8 px-3 text-xs">
                Cancel
              </button>
              <button
                onClick={() => recordDepMutation.mutate()}
                disabled={!depVersion.trim() || recordDepMutation.isPending}
                className="btn-primary h-8 px-4 text-xs"
              >
                {recordDepMutation.isPending ? 'Recording...' : 'Record Deployment'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

// Helper: SVG Mini Sparkline Component
const MiniSparkline: React.FC<{
  points: number[];
  metricName: string;
}> = ({ points, metricName }) => {
  if (points.length === 0) return null;

  const min = Math.min(...points);
  const max = Math.max(...points);
  const range = max - min || 1;
  const width = 300;
  const height = 90;
  const padding = 8;

  const getX = (index: number) => padding + (index / (points.length - 1 || 1)) * (width - 2 * padding);
  const getY = (val: number) => height - padding - ((val - min) / range) * (height - 2 * padding);

  const pathCoords = points.map((val, idx) => `${getX(idx)},${getY(val)}`).join(' L ');
  const linePath = `M ${pathCoords}`;
  const areaPath = `M ${getX(0)},${height} L ${pathCoords} L ${getX(points.length - 1)},${height} Z`;

  // Color tokens based on metric type
  const isErrorOrSaturation =
    metricName === 'ERROR_RATE_PERCENT' ||
    (metricName === 'CPU_PERCENT' && max >= 85) ||
    (metricName === 'MEMORY_PERCENT' && max >= 85);

  const strokeColor = isErrorOrSaturation ? '#ef4444' : '#4f46e5';

  return (
    <svg viewBox={`0 0 ${width} ${height}`} className="w-full h-full overflow-visible">
      <defs>
        <linearGradient id={`grad-${metricName}`} x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stopColor={strokeColor} stopOpacity="0.25" />
          <stop offset="100%" stopColor={strokeColor} stopOpacity="0.0" />
        </linearGradient>
      </defs>

      {/* Grid lines */}
      <line x1={0} y1={height - padding} x2={width} y2={height - padding} stroke="#f1f5f9" strokeWidth="1" />
      <line x1={0} y1={padding} x2={width} y2={padding} stroke="#f1f5f9" strokeWidth="1" strokeDasharray="3 3" />

      {/* Area fill */}
      <path d={areaPath} fill={`url(#grad-${metricName})`} />

      {/* Trend line */}
      <path d={linePath} fill="none" stroke={strokeColor} strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />

      {/* Last value dot */}
      {points.length > 0 && (
        <circle
          cx={getX(points.length - 1)}
          cy={getY(points[points.length - 1])}
          r="3.5"
          fill={strokeColor}
          stroke="#ffffff"
          strokeWidth="1.5"
        />
      )}
    </svg>
  );
};

// Helper: Metric Icon picker
function getMetricIcon(type: string) {
  switch (type) {
    case 'CPU_PERCENT':
      return <Cpu className="w-4 h-4 text-indigo-600" />;
    case 'MEMORY_PERCENT':
      return <Layers className="w-4 h-4 text-purple-600" />;
    case 'DISK_PERCENT':
      return <HardDrive className="w-4 h-4 text-sky-600" />;
    case 'LATENCY_P95_MS':
      return <Clock className="w-4 h-4 text-amber-600" />;
    case 'THROUGHPUT_RPS':
      return <Zap className="w-4 h-4 text-emerald-600" />;
    case 'ERROR_RATE_PERCENT':
      return <AlertTriangle className="w-4 h-4 text-red-600" />;
    default:
      return <Activity className="w-4 h-4 text-slate-600" />;
  }
}
