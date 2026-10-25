// src/pages/IncidentListPage.tsx
import React, { useEffect, useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import {
  AlertTriangle,
  ChevronLeft,
  ChevronRight,
  Filter,
  Loader2,
  Plus,
  RefreshCw,
  Search,
} from 'lucide-react';
import { getIncidentsApi } from '../api/incidents';
import { getServicesApi } from '../api/services';
import { PriorityBadge, SlaPill, StatusChip } from '../components/Badges';
import { useAuth } from '../context/AuthContext';
import { IncidentPriority, IncidentSeverity, IncidentStatus } from '../types';

export const IncidentListPage: React.FC = () => {
  const navigate = useNavigate();
  const { user } = useAuth();
  const [searchParams, setSearchParams] = useSearchParams();

  // URL Query state
  const q = searchParams.get('q') || '';
  const status = (searchParams.get('status') || '') as IncidentStatus | '';
  const priority = (searchParams.get('priority') || '') as IncidentPriority | '';
  const severity = (searchParams.get('severity') || '') as IncidentSeverity | '';
  const serviceId = searchParams.get('serviceId') || '';
  const page = parseInt(searchParams.get('page') || '0', 10);

  // Local search text for debouncing
  const [searchInput, setSearchInput] = useState(q);

  useEffect(() => {
    setSearchInput(q);
  }, [q]);

  const updateParam = (key: string, value: string) => {
    const next = new URLSearchParams(searchParams);
    if (value) {
      next.set(key, value);
    } else {
      next.delete(key);
    }
    next.set('page', '0'); // reset page on filter change
    setSearchParams(next);
  };

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    updateParam('q', searchInput);
  };

  // Queries
  const { data: servicesData } = useQuery({
    queryKey: ['services', 'active'],
    queryFn: () => getServicesApi(false),
  });

  const {
    data: incidentsData,
    isLoading,
    isFetching,
    refetch,
  } = useQuery({
    queryKey: ['incidents', { q, status, priority, severity, serviceId, page }],
    queryFn: () =>
      getIncidentsApi({
        q,
        status,
        priority,
        severity,
        serviceId: serviceId || undefined,
        page,
        size: 20,
      }),
  });

  const services = servicesData?.content || [];
  const incidents = incidentsData?.content || [];
  const totalPages = incidentsData?.page?.totalPages ?? incidentsData?.totalPages ?? 1;
  const totalElements = incidentsData?.page?.totalElements ?? incidentsData?.totalElements ?? 0;

  const canCreate = user && (user.role === 'ADMIN' || user.role === 'ENGINEER' || user.role === 'SUPPORT');

  return (
    <div className="space-y-6">
      {/* Top Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-xl font-bold tracking-tight text-text">Incidents</h1>
          <p className="text-xs text-muted mt-0.5">
            Active and resolved system incidents across all services ({totalElements} total)
          </p>
        </div>

        <div className="flex items-center gap-2">
          <button
            onClick={() => refetch()}
            disabled={isFetching}
            className="p-2 rounded-md border border-border bg-surface text-muted hover:text-text hover:bg-slate-50 transition-colors disabled:opacity-50"
            title="Refresh"
          >
            <RefreshCw className={`w-4 h-4 ${isFetching ? 'animate-spin' : ''}`} />
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

      {/* Filter Toolbar */}
      <div className="card-surface p-4 space-y-3">
        <div className="flex flex-col md:flex-row items-stretch md:items-center gap-3">
          {/* Keyword Search */}
          <form onSubmit={handleSearchSubmit} className="flex-1 relative">
            <Search className="w-4 h-4 text-muted absolute left-3 top-1/2 -translate-y-1/2" />
            <input
              type="text"
              placeholder="Filter by title, description or tag..."
              value={searchInput}
              onChange={(e) => setSearchInput(e.target.value)}
              className="w-full bg-slate-50 border border-border rounded-md pl-9 pr-3 py-1.5 text-xs text-text placeholder-muted focus:bg-white focus:border-primary focus:outline-none transition-colors"
            />
          </form>

          {/* Status Filter */}
          <div className="flex items-center gap-2 flex-wrap">
            <div className="flex items-center gap-1 text-xs text-muted">
              <Filter className="w-3.5 h-3.5" />
              <span>Filters:</span>
            </div>

            <select
              value={status}
              onChange={(e) => updateParam('status', e.target.value)}
              className="bg-slate-50 border border-border rounded-md px-2.5 py-1.5 text-xs text-text focus:bg-white focus:outline-none"
            >
              <option value="">All Statuses</option>
              <option value="OPEN">Open</option>
              <option value="ACKNOWLEDGED">Acknowledged</option>
              <option value="IN_PROGRESS">In Progress</option>
              <option value="RESOLVED">Resolved</option>
              <option value="CLOSED">Closed</option>
            </select>

            <select
              value={priority}
              onChange={(e) => updateParam('priority', e.target.value)}
              className="bg-slate-50 border border-border rounded-md px-2.5 py-1.5 text-xs text-text focus:bg-white focus:outline-none"
            >
              <option value="">All Priorities</option>
              <option value="P1">P1 (Critical)</option>
              <option value="P2">P2 (High)</option>
              <option value="P3">P3 (Medium)</option>
              <option value="P4">P4 (Low)</option>
            </select>

            <select
              value={severity}
              onChange={(e) => updateParam('severity', e.target.value)}
              className="bg-slate-50 border border-border rounded-md px-2.5 py-1.5 text-xs text-text focus:bg-white focus:outline-none"
            >
              <option value="">All Severities</option>
              <option value="CRITICAL">Critical</option>
              <option value="HIGH">High</option>
              <option value="MEDIUM">Medium</option>
              <option value="LOW">Low</option>
            </select>

            <select
              value={serviceId}
              onChange={(e) => updateParam('serviceId', e.target.value)}
              className="bg-slate-50 border border-border rounded-md px-2.5 py-1.5 text-xs text-text focus:bg-white focus:outline-none max-w-[160px]"
            >
              <option value="">All Services</option>
              {services.map((svc) => (
                <option key={svc.id} value={svc.id}>
                  {svc.name}
                </option>
              ))}
            </select>

            {(q || status || priority || severity || serviceId) && (
              <button
                type="button"
                onClick={() => setSearchParams(new URLSearchParams())}
                className="text-xs text-primary hover:text-primary-hover font-medium px-2 py-1"
              >
                Reset
              </button>
            )}
          </div>
        </div>
      </div>

      {/* Incidents Table */}
      <div className="card-surface overflow-hidden shadow-sm">
        {isLoading ? (
          <div className="py-20 flex flex-col items-center justify-center text-muted">
            <Loader2 className="w-8 h-8 animate-spin mb-3 text-primary" />
            <p className="text-sm">Loading incidents...</p>
          </div>
        ) : incidents.length === 0 ? (
          <div className="py-16 text-center">
            <AlertTriangle className="w-10 h-10 text-slate-300 mx-auto mb-3" />
            <h3 className="text-sm font-semibold text-text mb-1">No incidents match the filters</h3>
            <p className="text-xs text-muted max-w-sm mx-auto mb-4">
              Try adjusting your search criteria or report a new incident.
            </p>
            {canCreate && (
              <Link
                to="/incidents/new"
                className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-md bg-primary hover:bg-primary-hover text-white text-xs font-semibold transition-colors"
              >
                <Plus className="w-4 h-4" />
                Report Incident
              </Link>
            )}
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
                  <th className="py-3 px-4">Assignee</th>
                  <th className="py-3 px-4">SLA Deadline</th>
                  <th className="py-3 px-4">Created</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border">
                {incidents.map((inc) => (
                  <tr
                    key={inc.id}
                    onClick={() => navigate(`/incidents/${inc.id}`)}
                    className="hover:bg-slate-50/80 cursor-pointer transition-colors"
                  >
                    <td className="py-3 px-4 font-mono font-medium text-primary">
                      {inc.formattedNumber}
                    </td>
                    <td className="py-3 px-4 font-medium text-text max-w-md">
                      <div className="truncate font-semibold">{inc.title}</div>
                      {inc.tags && inc.tags.length > 0 && (
                        <div className="flex gap-1 mt-1 flex-wrap">
                          {inc.tags.slice(0, 3).map((t) => (
                            <span
                              key={t}
                              className="inline-block px-1.5 py-0.2 text-[10px] rounded bg-slate-100 text-slate-600 font-mono"
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
                    <td className="py-3 px-4 text-text font-medium">
                      {inc.serviceName || '—'}
                    </td>
                    <td className="py-3 px-4 text-muted">
                      {inc.assigneeName || <span className="italic text-slate-400">Unassigned</span>}
                    </td>
                    <td className="py-3 px-4">
                      <SlaPill
                        responseDueAt={inc.slaResponseDueAt}
                        resolveDueAt={inc.slaResolveDueAt}
                        breached={inc.slaBreached}
                      />
                    </td>
                    <td className="py-3 px-4 text-muted tabular-nums whitespace-nowrap">
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

        {/* Pagination Footer */}
        {totalPages > 1 && (
          <div className="p-3 border-t border-border bg-slate-50 flex items-center justify-between text-xs text-muted">
            <span>
              Page <span className="font-semibold text-text">{page + 1}</span> of{' '}
              <span className="font-semibold text-text">{totalPages}</span>
            </span>

            <div className="flex items-center gap-1">
              <button
                disabled={page <= 0}
                onClick={() => {
                  const next = new URLSearchParams(searchParams);
                  next.set('page', (page - 1).toString());
                  setSearchParams(next);
                }}
                className="p-1 rounded border border-border bg-white text-text hover:bg-slate-100 disabled:opacity-40 transition-colors"
              >
                <ChevronLeft className="w-4 h-4" />
              </button>
              <button
                disabled={page >= totalPages - 1}
                onClick={() => {
                  const next = new URLSearchParams(searchParams);
                  next.set('page', (page + 1).toString());
                  setSearchParams(next);
                }}
                className="p-1 rounded border border-border bg-white text-text hover:bg-slate-100 disabled:opacity-40 transition-colors"
              >
                <ChevronRight className="w-4 h-4" />
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};
