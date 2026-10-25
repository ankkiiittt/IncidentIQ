// src/pages/CreateIncidentPage.tsx
import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useMutation, useQuery } from '@tanstack/react-query';
import {
  AlertTriangle,
  ArrowLeft,
  Code2,
  FileText,
  Loader2,
  Server,
  Tag,
} from 'lucide-react';
import { createIncidentApi } from '../api/incidents';
import { getServicesApi } from '../api/services';
import { IncidentCategory, IncidentPriority, IncidentSeverity } from '../types';

export const CreateIncidentPage: React.FC = () => {
  const navigate = useNavigate();

  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [serviceId, setServiceId] = useState('');
  const [severity, setSeverity] = useState<IncidentSeverity>('MEDIUM');
  const [priority, setPriority] = useState<IncidentPriority>('P3');
  const [category, setCategory] = useState<IncidentCategory>('APPLICATION');
  const [tagsInput, setTagsInput] = useState('');
  const [logSnippet, setLogSnippet] = useState('');
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  // Fetch active services
  const { data: servicesData, isLoading: servicesLoading } = useQuery({
    queryKey: ['services', 'activeOnly'],
    queryFn: () => getServicesApi(true),
  });

  const services = servicesData?.content || [];

  const createMutation = useMutation({
    mutationFn: createIncidentApi,
    onSuccess: (data) => {
      navigate(`/incidents/${data.id}`);
    },
    onError: (err: Error) => {
      setErrorMsg(err.message || 'Failed to create incident');
    },
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMsg(null);

    if (!title.trim()) {
      setErrorMsg('Please enter an incident title.');
      return;
    }
    if (!description.trim()) {
      setErrorMsg('Please provide a description.');
      return;
    }
    if (!serviceId) {
      setErrorMsg('Please select an affected service.');
      return;
    }

    const tags = tagsInput
      .split(',')
      .map((t) => t.trim().toLowerCase())
      .filter((t) => t.length > 0);

    createMutation.mutate({
      title: title.trim(),
      description: description.trim(),
      serviceId,
      severity,
      priority,
      category,
      tags,
      logSnippet: logSnippet.trim() || undefined,
    });
  };

  const logBytes = new TextEncoder().encode(logSnippet).length;
  const isLogOversize = logBytes > 4096;

  return (
    <div className="max-w-3xl mx-auto space-y-6">
      {/* Header */}
      <div className="flex items-center gap-3">
        <button
          type="button"
          onClick={() => navigate(-1)}
          className="p-1.5 rounded-md border border-border bg-surface text-muted hover:text-text hover:bg-slate-50 transition-colors"
          title="Back"
        >
          <ArrowLeft className="w-4 h-4" />
        </button>
        <div>
          <h1 className="text-xl font-bold tracking-tight text-text">Report New Incident</h1>
          <p className="text-xs text-muted">
            Document and triage an active production or staging issue
          </p>
        </div>
      </div>

      {errorMsg && (
        <div className="card-surface p-4 bg-red-50 border-red-200 text-xs text-red-800 flex items-start gap-2">
          <AlertTriangle className="w-4 h-4 text-red-600 shrink-0 mt-0.5" />
          <span>{errorMsg}</span>
        </div>
      )}

      {/* Main Form */}
      <form onSubmit={handleSubmit} className="card-surface p-6 space-y-6 shadow-sm">
        {/* Title */}
        <div>
          <label htmlFor="title" className="block text-xs font-semibold text-text uppercase tracking-wider mb-1.5">
            Incident Title <span className="text-red-500">*</span>
          </label>
          <input
            id="title"
            type="text"
            required
            maxLength={200}
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            placeholder="e.g., High 500 error rate on checkout payment processing"
            className="w-full bg-slate-50 border border-border rounded-md px-3 py-2 text-sm text-text placeholder-muted focus:bg-white focus:border-primary focus:outline-none transition-colors"
          />
        </div>

        {/* Affected Service and Category */}
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div>
            <label htmlFor="service" className="block text-xs font-semibold text-text uppercase tracking-wider mb-1.5 flex items-center gap-1.5">
              <Server className="w-3.5 h-3.5 text-muted" />
              Affected Service <span className="text-red-500">*</span>
            </label>
            <select
              id="service"
              required
              value={serviceId}
              onChange={(e) => setServiceId(e.target.value)}
              disabled={servicesLoading}
              className="w-full bg-slate-50 border border-border rounded-md px-3 py-2 text-sm text-text focus:bg-white focus:outline-none"
            >
              <option value="">Select Service...</option>
              {services.map((svc) => (
                <option key={svc.id} value={svc.id}>
                  {svc.name} ({svc.environment})
                </option>
              ))}
            </select>
          </div>

          <div>
            <label htmlFor="category" className="block text-xs font-semibold text-text uppercase tracking-wider mb-1.5">
              Category
            </label>
            <select
              id="category"
              value={category}
              onChange={(e) => setCategory(e.target.value as IncidentCategory)}
              className="w-full bg-slate-50 border border-border rounded-md px-3 py-2 text-sm text-text focus:bg-white focus:outline-none"
            >
              <option value="APPLICATION">Application</option>
              <option value="DATABASE">Database</option>
              <option value="INFRASTRUCTURE">Infrastructure</option>
              <option value="NETWORK">Network</option>
              <option value="DEPLOYMENT">Deployment</option>
              <option value="SECURITY">Security</option>
              <option value="PERFORMANCE">Performance</option>
              <option value="THIRD_PARTY">Third-Party</option>
              <option value="OTHER">Other</option>
            </select>
          </div>
        </div>

        {/* Severity and Priority */}
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div>
            <label htmlFor="severity" className="block text-xs font-semibold text-text uppercase tracking-wider mb-1.5">
              Severity
            </label>
            <select
              id="severity"
              value={severity}
              onChange={(e) => setSeverity(e.target.value as IncidentSeverity)}
              className="w-full bg-slate-50 border border-border rounded-md px-3 py-2 text-sm text-text focus:bg-white focus:outline-none"
            >
              <option value="CRITICAL">Critical (Total outage or data loss)</option>
              <option value="HIGH">High (Major degraded performance)</option>
              <option value="MEDIUM">Medium (Minor customer impact)</option>
              <option value="LOW">Low (Minimal impact / cosmetic)</option>
            </select>
          </div>

          <div>
            <label htmlFor="priority" className="block text-xs font-semibold text-text uppercase tracking-wider mb-1.5">
              Initial Priority
            </label>
            <select
              id="priority"
              value={priority}
              onChange={(e) => setPriority(e.target.value as IncidentPriority)}
              className="w-full bg-slate-50 border border-border rounded-md px-3 py-2 text-sm text-text focus:bg-white focus:outline-none"
            >
              <option value="P1">P1 (Response: 15m, Resolve: 4h)</option>
              <option value="P2">P2 (Response: 30m, Resolve: 8h)</option>
              <option value="P3">P3 (Response: 4h, Resolve: 3d)</option>
              <option value="P4">P4 (Response: 1d, Resolve: 7d)</option>
            </select>
          </div>
        </div>

        {/* Description */}
        <div>
          <label htmlFor="description" className="block text-xs font-semibold text-text uppercase tracking-wider mb-1.5 flex items-center gap-1.5">
            <FileText className="w-3.5 h-3.5 text-muted" />
            Description & Symptoms <span className="text-red-500">*</span>
          </label>
          <textarea
            id="description"
            rows={5}
            required
            maxLength={10000}
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            placeholder="Describe what is occurring, observed symptoms, affected endpoints or users, and preliminary troubleshooting..."
            className="w-full bg-slate-50 border border-border rounded-md p-3 text-sm text-text placeholder-muted focus:bg-white focus:border-primary focus:outline-none transition-colors"
          />
        </div>

        {/* Tags */}
        <div>
          <label htmlFor="tags" className="block text-xs font-semibold text-text uppercase tracking-wider mb-1.5 flex items-center gap-1.5">
            <Tag className="w-3.5 h-3.5 text-muted" />
            Tags (comma-separated)
          </label>
          <input
            id="tags"
            type="text"
            value={tagsInput}
            onChange={(e) => setTagsInput(e.target.value)}
            placeholder="e.g., checkout, postgres, stripe, prod"
            className="w-full bg-slate-50 border border-border rounded-md px-3 py-2 text-sm text-text placeholder-muted focus:bg-white focus:border-primary focus:outline-none transition-colors"
          />
        </div>

        {/* Optional Log Snippet */}
        <div>
          <div className="flex items-center justify-between mb-1.5">
            <label htmlFor="logSnippet" className="block text-xs font-semibold text-text uppercase tracking-wider flex items-center gap-1.5">
              <Code2 className="w-3.5 h-3.5 text-muted" />
              Log Snippet (Optional, max 4 KB)
            </label>
            <span className={`text-[11px] font-mono ${isLogOversize ? 'text-red-600 font-bold' : 'text-muted'}`}>
              {logBytes} / 4096 bytes
            </span>
          </div>
          <textarea
            id="logSnippet"
            rows={4}
            value={logSnippet}
            onChange={(e) => setLogSnippet(e.target.value)}
            placeholder="Paste relevant exception stack traces or service logs..."
            className="w-full bg-slate-900 border border-slate-700 text-slate-100 font-mono text-xs rounded-md p-3 focus:border-primary focus:outline-none"
          />
          {isLogOversize && (
            <p className="mt-1 text-xs text-red-600">
              Log snippet exceeds the 4 KB maximum limit. Please truncate the snippet.
            </p>
          )}
        </div>

        {/* Action Buttons */}
        <div className="flex items-center justify-end gap-3 pt-4 border-t border-border">
          <button
            type="button"
            onClick={() => navigate(-1)}
            className="px-4 py-2 rounded-md border border-border text-xs font-medium text-muted hover:text-text hover:bg-slate-50 transition-colors"
          >
            Cancel
          </button>
          <button
            type="submit"
            disabled={createMutation.isPending || isLogOversize}
            className="inline-flex items-center gap-1.5 px-4 py-2 rounded-md bg-primary hover:bg-primary-hover text-white text-xs font-semibold shadow-sm transition-colors disabled:opacity-50"
          >
            {createMutation.isPending && <Loader2 className="w-3.5 h-3.5 animate-spin" />}
            Create Incident
          </button>
        </div>
      </form>
    </div>
  );
};
