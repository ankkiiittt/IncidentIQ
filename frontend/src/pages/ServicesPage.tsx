// src/pages/ServicesPage.tsx
import React, { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  AlertTriangle,
  GitBranch,
  Layers,
  Loader2,
  Plus,
  Server,
} from 'lucide-react';
import {
  createServiceApi,
  deactivateServiceApi,
  getDeploymentsApi,
  getServicesApi,
  recordDeploymentApi,
} from '../api/services';
import { useAuth } from '../context/AuthContext';
import { Service, ServiceCriticality, ServiceEnvironment } from '../types';

export const ServicesPage: React.FC = () => {
  const queryClient = useQueryClient();
  const { user } = useAuth();

  const [selectedService, setSelectedService] = useState<Service | null>(null);
  const [isAddServiceOpen, setIsAddServiceOpen] = useState(false);
  const [isRecordDepOpen, setIsRecordDepOpen] = useState(false);
  const [actionError, setActionError] = useState<string | null>(null);

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

  // Queries
  const { data: servicesData, isLoading: servicesLoading } = useQuery({
    queryKey: ['services', 'all'],
    queryFn: () => getServicesApi(false),
  });

  const { data: deploymentsData, isLoading: deploymentsLoading } = useQuery({
    queryKey: ['deployments', selectedService?.id],
    queryFn: () => getDeploymentsApi(selectedService!.id),
    enabled: !!selectedService,
  });

  const services = servicesData?.content || [];
  const deployments = deploymentsData?.content || [];

  // Mutations
  const createServiceMutation = useMutation({
    mutationFn: createServiceApi,
    onSuccess: () => {
      setIsAddServiceOpen(false);
      resetServiceForm();
      setActionError(null);
      queryClient.invalidateQueries({ queryKey: ['services'] });
    },
    onError: (err: Error) => setActionError(err.message),
  });

  const deactivateMutation = useMutation({
    mutationFn: deactivateServiceApi,
    onSuccess: () => {
      setActionError(null);
      queryClient.invalidateQueries({ queryKey: ['services'] });
    },
    onError: (err: Error) => setActionError(err.message),
  });

  const recordDepMutation = useMutation({
    mutationFn: () =>
      recordDeploymentApi(selectedService!.id, {
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
      queryClient.invalidateQueries({ queryKey: ['deployments', selectedService?.id] });
    },
    onError: (err: Error) => setActionError(err.message),
  });

  const resetServiceForm = () => {
    setNewServiceName('');
    setNewServiceOwner('');
    setNewServiceDesc('');
  };

  const isAdmin = user?.role === 'ADMIN';
  const canDeploy = user && (user.role === 'ADMIN' || user.role === 'ENGINEER');

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-xl font-bold tracking-tight text-text">Service Registry</h1>
          <p className="text-xs text-muted mt-0.5">
            Systems and services catalog tied to deployments, metrics, and incident tracking
          </p>
        </div>

        {isAdmin && (
          <button
            onClick={() => setIsAddServiceOpen(true)}
            className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-md bg-primary hover:bg-primary-hover text-white text-xs font-semibold shadow-sm transition-colors"
          >
            <Plus className="w-4 h-4" />
            Add Service
          </button>
        )}
      </div>

      {actionError && (
        <div className="card-surface p-3 bg-red-50 border-red-200 text-xs text-red-800 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <AlertTriangle className="w-4 h-4 text-red-600 shrink-0" />
            <span>{actionError}</span>
          </div>
          <button onClick={() => setActionError(null)} className="text-red-700 font-bold ml-2">
            ×
          </button>
        </div>
      )}

      {/* Main Grid: Services List & Deployments Panel */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Services Table */}
        <div className="lg:col-span-2 card-surface overflow-hidden shadow-sm">
          {servicesLoading ? (
            <div className="py-20 flex flex-col items-center justify-center text-muted">
              <Loader2 className="w-8 h-8 animate-spin text-primary mb-3" />
              <p className="text-xs">Loading services...</p>
            </div>
          ) : services.length === 0 ? (
            <div className="py-16 text-center">
              <Server className="w-10 h-10 text-slate-300 mx-auto mb-3" />
              <h3 className="text-sm font-semibold text-text mb-1">No services registered</h3>
              <p className="text-xs text-muted mb-4">Register your microservices and infrastructure components.</p>
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs">
                <thead className="bg-slate-50 border-b border-border text-muted font-semibold uppercase tracking-wider text-[11px]">
                  <tr>
                    <th className="py-3 px-4">Service</th>
                    <th className="py-3 px-3">Env</th>
                    <th className="py-3 px-3">Criticality</th>
                    <th className="py-3 px-4">Owner Team</th>
                    <th className="py-3 px-3">Status</th>
                    <th className="py-3 px-4 text-right">Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-border">
                  {services.map((svc) => (
                    <tr
                      key={svc.id}
                      onClick={() => setSelectedService(svc)}
                      className={`hover:bg-slate-50/80 cursor-pointer transition-colors ${
                        selectedService?.id === svc.id ? 'bg-primary-light/40 font-medium' : ''
                      }`}
                    >
                      <td className="py-3 px-4 font-semibold text-text">
                        {svc.name}
                        {svc.description && (
                          <span className="block text-[11px] font-normal text-muted truncate max-w-xs">
                            {svc.description}
                          </span>
                        )}
                      </td>
                      <td className="py-3 px-3">
                        <span className="px-1.5 py-0.5 rounded text-[10px] font-bold bg-slate-100 text-slate-700">
                          {svc.environment}
                        </span>
                      </td>
                      <td className="py-3 px-3">
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
                      </td>
                      <td className="py-3 px-4 text-muted">{svc.ownerTeam}</td>
                      <td className="py-3 px-3">
                        <span
                          className={`inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold ${
                            svc.active
                              ? 'bg-emerald-50 text-emerald-700 border border-emerald-200'
                              : 'bg-slate-100 text-slate-500 border border-slate-200'
                          }`}
                        >
                          {svc.active ? 'Active' : 'Inactive'}
                        </span>
                      </td>
                      <td className="py-3 px-4 text-right">
                        {isAdmin && svc.active && (
                          <button
                            onClick={(e) => {
                              e.stopPropagation();
                              if (confirm(`Deactivate ${svc.name}? Active incidents will prevent deactivation.`)) {
                                deactivateMutation.mutate(svc.id);
                              }
                            }}
                            className="text-[11px] text-red-600 hover:text-red-700 font-medium hover:underline"
                          >
                            Deactivate
                          </button>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>

        {/* Deployments & Service Details Drawer */}
        <div className="card-surface p-4 space-y-4">
          {selectedService ? (
            <>
              <div className="border-b border-border pb-3 flex items-center justify-between">
                <div>
                  <h3 className="text-sm font-bold text-text">{selectedService.name}</h3>
                  <span className="text-[11px] text-muted">
                    {selectedService.environment} • {selectedService.ownerTeam}
                  </span>
                </div>
                {canDeploy && selectedService.active && (
                  <button
                    onClick={() => setIsRecordDepOpen(true)}
                    className="inline-flex items-center gap-1 px-2.5 py-1 bg-slate-800 hover:bg-slate-900 text-white rounded text-xs font-semibold"
                  >
                    <GitBranch className="w-3.5 h-3.5" />
                    Deploy
                  </button>
                )}
              </div>

              {/* Deployments History */}
              <div className="space-y-3">
                <span className="text-[11px] font-bold uppercase tracking-wider text-muted block">
                  Deployment History
                </span>

                {deploymentsLoading ? (
                  <div className="py-8 text-center text-muted">
                    <Loader2 className="w-5 h-5 animate-spin mx-auto mb-1 text-primary" />
                    <span className="text-[11px]">Loading deployments...</span>
                  </div>
                ) : deployments.length === 0 ? (
                  <p className="text-xs text-muted italic py-4 text-center">
                    No deployments recorded yet for this service.
                  </p>
                ) : (
                  <div className="space-y-2">
                    {deployments.map((d) => (
                      <div key={d.id} className="p-2.5 rounded bg-slate-50 border border-border text-xs space-y-1">
                        <div className="flex items-center justify-between">
                          <span className="font-mono font-bold text-primary">{d.version}</span>
                          <span className="text-[10px] text-muted tabular-nums">
                            {new Date(d.deployedAt).toLocaleString(undefined, {
                              month: 'short',
                              day: 'numeric',
                              hour: '2-digit',
                              minute: '2-digit',
                            })}
                          </span>
                        </div>
                        {d.deployedBy && (
                          <span className="text-[11px] text-muted block">by {d.deployedBy}</span>
                        )}
                        {d.notes && <p className="text-xs text-slate-700 pt-0.5">{d.notes}</p>}
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </>
          ) : (
            <div className="py-20 text-center text-muted">
              <Layers className="w-8 h-8 text-slate-300 mx-auto mb-2" />
              <p className="text-xs">Select a service to inspect deployment history</p>
            </div>
          )}
        </div>
      </div>

      {/* Add Service Modal */}
      {isAddServiceOpen && (
        <div className="fixed inset-0 bg-slate-900/40 backdrop-blur-sm flex items-center justify-center p-4 z-50">
          <form
            onSubmit={(e) => {
              e.preventDefault();
              createServiceMutation.mutate({
                name: newServiceName.trim(),
                environment: newServiceEnv,
                ownerTeam: newServiceOwner.trim(),
                criticality: newServiceCrit,
                description: newServiceDesc.trim() || undefined,
              });
            }}
            className="card-surface p-5 max-w-md w-full shadow-lg space-y-4"
          >
            <h3 className="text-sm font-bold text-text">Register New Service</h3>

            <div className="space-y-3">
              <div>
                <label className="block text-xs font-semibold text-text mb-1">Service Name *</label>
                <input
                  type="text"
                  required
                  placeholder="e.g., auth-service, orders-api"
                  value={newServiceName}
                  onChange={(e) => setNewServiceName(e.target.value)}
                  className="w-full bg-slate-50 border border-border rounded p-2 text-xs text-text focus:bg-white focus:outline-none"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-text mb-1">Environment</label>
                  <select
                    value={newServiceEnv}
                    onChange={(e) => setNewServiceEnv(e.target.value as ServiceEnvironment)}
                    className="w-full bg-slate-50 border border-border rounded p-2 text-xs text-text focus:bg-white focus:outline-none"
                  >
                    <option value="PROD">PROD</option>
                    <option value="STAGING">STAGING</option>
                    <option value="DEV">DEV</option>
                  </select>
                </div>

                <div>
                  <label className="block text-xs font-semibold text-text mb-1">Criticality</label>
                  <select
                    value={newServiceCrit}
                    onChange={(e) => setNewServiceCrit(e.target.value as ServiceCriticality)}
                    className="w-full bg-slate-50 border border-border rounded p-2 text-xs text-text focus:bg-white focus:outline-none"
                  >
                    <option value="CRITICAL">CRITICAL</option>
                    <option value="HIGH">HIGH</option>
                    <option value="MEDIUM">MEDIUM</option>
                    <option value="LOW">LOW</option>
                  </select>
                </div>
              </div>

              <div>
                <label className="block text-xs font-semibold text-text mb-1">Owner Team *</label>
                <input
                  type="text"
                  required
                  placeholder="e.g., Core Platform, Checkout SRE"
                  value={newServiceOwner}
                  onChange={(e) => setNewServiceOwner(e.target.value)}
                  className="w-full bg-slate-50 border border-border rounded p-2 text-xs text-text focus:bg-white focus:outline-none"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-text mb-1">Description</label>
                <textarea
                  rows={2}
                  placeholder="Brief service purpose..."
                  value={newServiceDesc}
                  onChange={(e) => setNewServiceDesc(e.target.value)}
                  className="w-full bg-slate-50 border border-border rounded p-2 text-xs text-text focus:bg-white focus:outline-none"
                />
              </div>
            </div>

            <div className="flex justify-end gap-2 pt-2 border-t border-border">
              <button
                type="button"
                onClick={() => setIsAddServiceOpen(false)}
                className="px-3 py-1.5 rounded border border-border text-xs text-muted hover:text-text"
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={createServiceMutation.isPending}
                className="px-3 py-1.5 rounded bg-primary hover:bg-primary-hover text-white text-xs font-semibold disabled:opacity-50"
              >
                Create Service
              </button>
            </div>
          </form>
        </div>
      )}

      {/* Record Deployment Modal */}
      {isRecordDepOpen && selectedService && (
        <div className="fixed inset-0 bg-slate-900/40 backdrop-blur-sm flex items-center justify-center p-4 z-50">
          <form
            onSubmit={(e) => {
              e.preventDefault();
              recordDepMutation.mutate();
            }}
            className="card-surface p-5 max-w-md w-full shadow-lg space-y-4"
          >
            <h3 className="text-sm font-bold text-text">Record Deployment: {selectedService.name}</h3>

            <div className="space-y-3">
              <div>
                <label className="block text-xs font-semibold text-text mb-1">Release Version *</label>
                <input
                  type="text"
                  required
                  placeholder="e.g., v2.3.1, sha-9a4f21"
                  value={depVersion}
                  onChange={(e) => setDepVersion(e.target.value)}
                  className="w-full bg-slate-50 border border-border rounded p-2 text-xs text-text font-mono focus:bg-white focus:outline-none"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-text mb-1">Deployed By</label>
                <input
                  type="text"
                  placeholder="e.g., GitHub Actions, deployer-bot"
                  value={depBy}
                  onChange={(e) => setDepBy(e.target.value)}
                  className="w-full bg-slate-50 border border-border rounded p-2 text-xs text-text focus:bg-white focus:outline-none"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-text mb-1">Release Notes</label>
                <textarea
                  rows={2}
                  placeholder="e.g., Migrated DB pool settings, security patch..."
                  value={depNotes}
                  onChange={(e) => setDepNotes(e.target.value)}
                  className="w-full bg-slate-50 border border-border rounded p-2 text-xs text-text focus:bg-white focus:outline-none"
                />
              </div>
            </div>

            <div className="flex justify-end gap-2 pt-2 border-t border-border">
              <button
                type="button"
                onClick={() => setIsRecordDepOpen(false)}
                className="px-3 py-1.5 rounded border border-border text-xs text-muted hover:text-text"
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={recordDepMutation.isPending || !depVersion.trim()}
                className="px-3 py-1.5 rounded bg-primary hover:bg-primary-hover text-white text-xs font-semibold disabled:opacity-50"
              >
                Record Deployment
              </button>
            </div>
          </form>
        </div>
      )}
    </div>
  );
};
