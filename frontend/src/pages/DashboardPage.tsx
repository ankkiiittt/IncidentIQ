// src/pages/DashboardPage.tsx
import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import {
  AlertTriangle,
  ArrowRight,
  CheckCircle2,
  Clock,
  Plus,
  Server,
  ShieldAlert,
} from 'lucide-react';
import { getIncidentsApi } from '../api/incidents';
import { getServicesApi } from '../api/services';
import { PriorityBadge, SlaPill, StatusChip } from '../components/Badges';
import { useAuth } from '../context/AuthContext';

export const DashboardPage: React.FC = () => {
  const { user } = useAuth();
  const navigate = useNavigate();

  const { data: incidentsData, isLoading: incidentsLoading } = useQuery({
    queryKey: ['dashboard-incidents'],
    queryFn: () => getIncidentsApi({ size: 5 }),
  });

  const { data: servicesData } = useQuery({
    queryKey: ['dashboard-services'],
    queryFn: () => getServicesApi(false, 0, 5),
  });

  const incidents = incidentsData?.content || [];
  const services = servicesData?.content || [];
  const totalIncidents = incidentsData?.page?.totalElements ?? incidentsData?.totalElements ?? 0;

  const openIncidents = incidents.filter(
    (i) => i.status === 'OPEN' || i.status === 'ACKNOWLEDGED' || i.status === 'IN_PROGRESS'
  );
  const breachedIncidents = incidents.filter((i) => i.slaBreached);

  const canCreate = user && (user.role === 'ADMIN' || user.role === 'ENGINEER' || user.role === 'SUPPORT');

  return (
    <div className="space-y-6">
      {/* Welcome Banner */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-xl font-bold tracking-tight text-text">Operational Overview</h1>
          <p className="text-xs text-muted mt-0.5">
            Welcome back, <span className="font-semibold text-text">{user?.name}</span>. Here is the current incident state across all systems.
          </p>
        </div>

        {canCreate && (
          <Link
            to="/incidents/new"
            className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-md bg-primary hover:bg-primary-hover text-white text-xs font-semibold shadow-sm transition-colors"
          >
            <Plus className="w-4 h-4" />
            Report Incident
          </Link>
        )}
      </div>

      {/* KPI Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <div className="card-surface p-4 space-y-1 shadow-sm">
          <div className="flex items-center justify-between text-muted text-xs font-medium">
            <span>Total Incidents</span>
            <AlertTriangle className="w-4 h-4 text-primary" />
          </div>
          <div className="text-2xl font-bold text-text tabular-nums">{totalIncidents}</div>
          <span className="text-[11px] text-muted">Recorded platform incidents</span>
        </div>

        <div className="card-surface p-4 space-y-1 shadow-sm">
          <div className="flex items-center justify-between text-muted text-xs font-medium">
            <span>Active / Open</span>
            <Clock className="w-4 h-4 text-orange-500" />
          </div>
          <div className="text-2xl font-bold text-orange-600 tabular-nums">
            {openIncidents.length}
          </div>
          <span className="text-[11px] text-muted">Requiring active remediation</span>
        </div>

        <div className="card-surface p-4 space-y-1 shadow-sm">
          <div className="flex items-center justify-between text-muted text-xs font-medium">
            <span>SLA Breached</span>
            <ShieldAlert className="w-4 h-4 text-red-500" />
          </div>
          <div className="text-2xl font-bold text-red-600 tabular-nums">
            {breachedIncidents.length}
          </div>
          <span className="text-[11px] text-muted">Target response or resolve missed</span>
        </div>

        <div className="card-surface p-4 space-y-1 shadow-sm">
          <div className="flex items-center justify-between text-muted text-xs font-medium">
            <span>Registered Services</span>
            <Server className="w-4 h-4 text-emerald-600" />
          </div>
          <div className="text-2xl font-bold text-text tabular-nums">
            {services.length}
          </div>
          <span className="text-[11px] text-muted">Monitored systems and components</span>
        </div>
      </div>

      {/* Main Grid: Recent Incidents & Services Overview */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Recent Incidents Table */}
        <div className="lg:col-span-2 card-surface p-5 space-y-4 shadow-sm">
          <div className="flex items-center justify-between border-b border-border pb-3">
            <div>
              <h2 className="text-sm font-bold text-text">Recent Incidents</h2>
              <span className="text-xs text-muted">Latest reported issues across all services</span>
            </div>
            <Link
              to="/incidents"
              className="text-xs font-semibold text-primary hover:text-primary-hover flex items-center gap-1"
            >
              View all <ArrowRight className="w-3.5 h-3.5" />
            </Link>
          </div>

          {incidentsLoading ? (
            <div className="py-12 text-center text-xs text-muted">Loading incidents...</div>
          ) : incidents.length === 0 ? (
            <div className="py-12 text-center text-xs text-muted">
              <CheckCircle2 className="w-8 h-8 text-emerald-500 mx-auto mb-2" />
              All systems operational. No open incidents.
            </div>
          ) : (
            <div className="space-y-2.5">
              {incidents.map((inc) => (
                <div
                  key={inc.id}
                  onClick={() => navigate(`/incidents/${inc.id}`)}
                  className="p-3 rounded-md bg-slate-50 hover:bg-slate-100/80 cursor-pointer border border-border flex items-center justify-between gap-4 transition-colors text-xs"
                >
                  <div className="space-y-1 min-w-0">
                    <div className="flex items-center gap-2">
                      <span className="font-mono font-bold text-primary">{inc.formattedNumber}</span>
                      <PriorityBadge priority={inc.priority} size="sm" />
                      <StatusChip status={inc.status} />
                      <span className="text-muted font-medium truncate">• {inc.serviceName}</span>
                    </div>
                    <span className="font-semibold text-text truncate block">{inc.title}</span>
                  </div>

                  <div className="shrink-0 text-right">
                    <SlaPill
                      responseDueAt={inc.slaResponseDueAt}
                      resolveDueAt={inc.slaResolveDueAt}
                      breached={inc.slaBreached}
                    />
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Services Quick List */}
        <div className="card-surface p-5 space-y-4 shadow-sm">
          <div className="flex items-center justify-between border-b border-border pb-3">
            <div>
              <h2 className="text-sm font-bold text-text">Service Health</h2>
              <span className="text-xs text-muted">Critical production systems</span>
            </div>
            <Link
              to="/services"
              className="text-xs font-semibold text-primary hover:text-primary-hover flex items-center gap-1"
            >
              Registry <ArrowRight className="w-3.5 h-3.5" />
            </Link>
          </div>

          <div className="space-y-2 text-xs">
            {services.map((svc) => (
              <div
                key={svc.id}
                onClick={() => navigate('/services')}
                className="p-2.5 rounded bg-slate-50 hover:bg-slate-100 border border-border flex items-center justify-between cursor-pointer transition-colors"
              >
                <div>
                  <span className="font-semibold text-text block">{svc.name}</span>
                  <span className="text-[11px] text-muted">
                    {svc.environment} • {svc.ownerTeam}
                  </span>
                </div>
                <span
                  className={`px-1.5 py-0.5 rounded text-[10px] font-bold ${
                    svc.criticality === 'CRITICAL'
                      ? 'bg-red-100 text-red-800'
                      : svc.criticality === 'HIGH'
                      ? 'bg-orange-100 text-orange-800'
                      : 'bg-slate-100 text-slate-700'
                  }`}
                >
                  {svc.criticality}
                </span>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
};
