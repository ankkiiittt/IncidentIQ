// src/pages/IncidentDetailPage.tsx
import React, { useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useMutation, useQuery } from '@tanstack/react-query';
import {
  AlertCircle,
  AlertTriangle,
  ArrowLeft,
  CheckCircle2,
  Code2,
  ExternalLink,
  History,
  Loader2,
  MessageSquare,
  RefreshCw,
  Send,
  ShieldAlert,
  Sparkles,
  TrendingUp,
  UserPlus,
} from 'lucide-react';
import {
  addCommentApi,
  addLogSnippetApi,
  assignIncidentApi,
  escalateIncidentApi,
  getCommentsApi,
  getIncidentApi,
  getTimelineApi,
  transitionStatusApi,
} from '../api/incidents';
import { getSimilarIncidentsApi } from '../api/knowledge';
import { getUsersApi } from '../api/users';
import { AiDiagnosticsCard } from '../components/AiDiagnosticsCard';
import { PriorityBadge, SeverityBadge, SlaPill, StatusChip } from '../components/Badges';
import { useAuth } from '../context/AuthContext';
import { IncidentStatus, SimilarIncident } from '../types';

export const IncidentDetailPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { user } = useAuth();

  // Modals & Action States
  const [isEscalateOpen, setIsEscalateOpen] = useState(false);
  const [escalateReason, setEscalateReason] = useState('');

  const [isAssignOpen, setIsAssignOpen] = useState(false);
  const [selectedAssigneeId, setSelectedAssigneeId] = useState('');

  const [isResolveOpen, setIsResolveOpen] = useState(false);
  const [rootCause, setRootCause] = useState('');
  const [resolution, setResolution] = useState('');

  const [isLogSnippetOpen, setIsLogSnippetOpen] = useState(false);
  const [newLogSnippet, setNewLogSnippet] = useState('');

  const [newCommentBody, setNewCommentBody] = useState('');
  const [actionError, setActionError] = useState<string | null>(null);

  // Queries
  const {
    data: incident,
    isLoading: incidentLoading,
    error: incidentError,
    refetch: refetchIncident,
  } = useQuery({
    queryKey: ['incident', id],
    queryFn: () => getIncidentApi(id!),
    enabled: !!id,
    refetchInterval: 15000, // refresh SLA/status every 15s
  });

  const { data: comments = [], refetch: refetchComments } = useQuery({
    queryKey: ['incident-comments', id],
    queryFn: () => getCommentsApi(id!),
    enabled: !!id,
  });

  const { data: timeline = [], refetch: refetchTimeline } = useQuery({
    queryKey: ['incident-timeline', id],
    queryFn: () => getTimelineApi(id!),
    enabled: !!id,
  });

  const { data: usersData } = useQuery({
    queryKey: ['users', 'assignees'],
    queryFn: () => getUsersApi(),
    enabled: isAssignOpen,
  });

  const {
    data: similarIncidents = [],
    isLoading: similarLoading,
    refetch: refetchSimilar,
  } = useQuery({
    queryKey: ['similar-incidents', id],
    queryFn: () => getSimilarIncidentsApi(id!),
    enabled: !!id,
  });

  const refreshAll = () => {
    refetchIncident();
    refetchComments();
    refetchTimeline();
    refetchSimilar();
  };

  // Mutations
  const transitionMutation = useMutation({
    mutationFn: ({
      targetStatus,
      rc,
      res,
    }: {
      targetStatus: IncidentStatus;
      rc?: string;
      res?: string;
    }) =>
      transitionStatusApi(id!, targetStatus, rc, res, incident!.version),
    onSuccess: () => {
      setIsResolveOpen(false);
      setActionError(null);
      refreshAll();
    },
    onError: (err: Error) => setActionError(err.message),
  });

  const escalateMutation = useMutation({
    mutationFn: () => escalateIncidentApi(id!, escalateReason.trim() || undefined, incident!.version),
    onSuccess: () => {
      setIsEscalateOpen(false);
      setEscalateReason('');
      setActionError(null);
      refreshAll();
    },
    onError: (err: Error) => setActionError(err.message),
  });

  const assignMutation = useMutation({
    mutationFn: () => assignIncidentApi(id!, selectedAssigneeId, incident!.version),
    onSuccess: () => {
      setIsAssignOpen(false);
      setSelectedAssigneeId('');
      setActionError(null);
      refreshAll();
    },
    onError: (err: Error) => setActionError(err.message),
  });

  const addCommentMutation = useMutation({
    mutationFn: () => addCommentApi(id!, newCommentBody.trim()),
    onSuccess: () => {
      setNewCommentBody('');
      refetchComments();
      refetchTimeline();
    },
    onError: (err: Error) => setActionError(err.message),
  });

  const addLogSnippetMutation = useMutation({
    mutationFn: () => addLogSnippetApi(id!, newLogSnippet.trim()),
    onSuccess: () => {
      setIsLogSnippetOpen(false);
      setNewLogSnippet('');
      refetchTimeline();
    },
    onError: (err: Error) => setActionError(err.message),
  });

  if (incidentLoading) {
    return (
      <div className="py-24 flex flex-col items-center justify-center text-muted">
        <Loader2 className="w-8 h-8 animate-spin text-primary mb-3" />
        <p className="text-sm">Loading incident details...</p>
      </div>
    );
  }

  if (incidentError || !incident) {
    return (
      <div className="card-surface p-8 text-center max-w-md mx-auto my-12">
        <AlertCircle className="w-10 h-10 text-red-500 mx-auto mb-3" />
        <h2 className="text-base font-semibold text-text mb-1">Incident Not Found</h2>
        <p className="text-xs text-muted mb-4">
          The requested incident could not be found or you do not have permission to view it.
        </p>
        <button
          onClick={() => navigate('/incidents')}
          className="px-3 py-1.5 rounded bg-primary text-white text-xs font-semibold"
        >
          Back to Incidents
        </button>
      </div>
    );
  }

  // Role permissions
  const isEngineerOrAdmin = user && (user.role === 'ADMIN' || user.role === 'ENGINEER');
  const canUpdate = user && (user.role === 'ADMIN' || user.role === 'ENGINEER' || user.role === 'SUPPORT');

  const usersList = usersData?.content || [];

  return (
    <div className="space-y-6 max-w-6xl mx-auto">
      {/* Top Breadcrumb & Actions Bar */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div className="flex items-center gap-3">
          <button
            onClick={() => navigate('/incidents')}
            className="p-1.5 rounded-md border border-border bg-surface text-muted hover:text-text hover:bg-slate-50 transition-colors"
            title="Back to Incidents"
          >
            <ArrowLeft className="w-4 h-4" />
          </button>
          <div>
            <div className="flex items-center gap-2">
              <span className="font-mono text-xs font-bold text-primary px-1.5 py-0.5 rounded bg-primary-light">
                {incident.formattedNumber}
              </span>
              <StatusChip status={incident.status} />
              <PriorityBadge priority={incident.priority} />
              <SeverityBadge severity={incident.severity} />
            </div>
            <h1 className="text-xl font-bold tracking-tight text-text mt-1">{incident.title}</h1>
          </div>
        </div>

        {/* Action Buttons based on status */}
        <div className="flex items-center gap-2 flex-wrap">
          <button
            onClick={refreshAll}
            className="p-2 rounded-md border border-border bg-surface text-muted hover:text-text hover:bg-slate-50"
            title="Refresh"
          >
            <RefreshCw className="w-4 h-4" />
          </button>

          {/* Workflow Transitions */}
          {canUpdate && incident.status === 'OPEN' && (
            <button
              onClick={() => transitionMutation.mutate({ targetStatus: 'ACKNOWLEDGED' })}
              disabled={transitionMutation.isPending}
              className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-md bg-sky-600 hover:bg-sky-700 text-white text-xs font-semibold shadow-sm transition-colors"
            >
              <CheckCircle2 className="w-4 h-4" />
              Acknowledge
            </button>
          )}

          {canUpdate && (incident.status === 'OPEN' || incident.status === 'ACKNOWLEDGED') && (
            <button
              onClick={() => transitionMutation.mutate({ targetStatus: 'IN_PROGRESS' })}
              disabled={transitionMutation.isPending}
              className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-md bg-indigo-600 hover:bg-indigo-700 text-white text-xs font-semibold shadow-sm transition-colors"
            >
              Start Working
            </button>
          )}

          {canUpdate && incident.status !== 'RESOLVED' && incident.status !== 'CLOSED' && (
            <button
              onClick={() => setIsEscalateOpen(true)}
              className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-md border border-border bg-surface hover:bg-slate-50 text-xs font-semibold text-text shadow-sm transition-colors"
            >
              <TrendingUp className="w-4 h-4 text-orange-600" />
              Escalate
            </button>
          )}

          {isEngineerOrAdmin && incident.status !== 'RESOLVED' && incident.status !== 'CLOSED' && (
            <button
              onClick={() => setIsAssignOpen(true)}
              className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-md border border-border bg-surface hover:bg-slate-50 text-xs font-semibold text-text shadow-sm transition-colors"
            >
              <UserPlus className="w-4 h-4 text-primary" />
              Assign
            </button>
          )}

          {isEngineerOrAdmin && incident.status === 'IN_PROGRESS' && (
            <button
              onClick={() => setIsResolveOpen(true)}
              className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-md bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-semibold shadow-sm transition-colors"
            >
              <CheckCircle2 className="w-4 h-4" />
              Resolve
            </button>
          )}

          {isEngineerOrAdmin && incident.status === 'RESOLVED' && (
            <>
              <button
                onClick={() => transitionMutation.mutate({ targetStatus: 'CLOSED' })}
                disabled={transitionMutation.isPending}
                className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-md bg-slate-800 hover:bg-slate-900 text-white text-xs font-semibold shadow-sm transition-colors"
              >
                Close Incident
              </button>
              <button
                onClick={() => transitionMutation.mutate({ targetStatus: 'IN_PROGRESS' })}
                disabled={transitionMutation.isPending}
                className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-md border border-border bg-surface hover:bg-slate-50 text-xs font-semibold text-text transition-colors"
              >
                Reopen
              </button>
            </>
          )}
        </div>
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

      {/* SLA Target & Health Banner */}
      <div className="card-surface p-4 flex flex-col md:flex-row items-start md:items-center justify-between gap-4 bg-slate-50/50">
        <div className="flex items-center gap-6">
          <div>
            <span className="text-[11px] font-semibold uppercase tracking-wider text-muted block mb-0.5">
              Response SLA
            </span>
            <div className="flex items-center gap-2">
              <SlaPill
                responseDueAt={incident.slaResponseDueAt}
                breached={incident.slaBreached && !incident.acknowledgedAt}
                acknowledgedAt={incident.acknowledgedAt}
              />
              {incident.acknowledgedAt && (
                <span className="text-[11px] text-muted">
                  Ack'd at {new Date(incident.acknowledgedAt).toLocaleTimeString()}
                </span>
              )}
            </div>
          </div>

          <div className="border-l border-border pl-6">
            <span className="text-[11px] font-semibold uppercase tracking-wider text-muted block mb-0.5">
              Resolution SLA
            </span>
            <div className="flex items-center gap-2">
              <SlaPill
                resolveDueAt={incident.slaResolveDueAt}
                breached={incident.slaBreached && !incident.resolvedAt}
                resolvedAt={incident.resolvedAt}
              />
              {incident.resolvedAt && (
                <span className="text-[11px] text-muted">
                  Resolved at {new Date(incident.resolvedAt).toLocaleTimeString()}
                </span>
              )}
            </div>
          </div>
        </div>

        {incident.slaBreached && (
          <div className="inline-flex items-center gap-2 px-3 py-1.5 rounded-md bg-red-100 text-red-800 border border-red-300 text-xs font-semibold">
            <ShieldAlert className="w-4 h-4 text-red-700" />
            Target SLA breach recorded for this incident
          </div>
        )}
      </div>

      {/* Main Grid: 2 Columns */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Left 2 Columns: Description, Log Snippets, Comments */}
        <div className="lg:col-span-2 space-y-6">
          {/* Description */}
          <div className="card-surface p-5 space-y-3">
            <h3 className="text-xs font-bold text-text uppercase tracking-wider">Description</h3>
            <p className="text-sm text-text whitespace-pre-wrap leading-relaxed">
              {incident.description}
            </p>
          </div>

          {/* AI Diagnostics & Root Cause Analysis Panel (Screen S4/S5) */}
          <AiDiagnosticsCard
            incident={incident}
            canManage={!!canUpdate}
            onIncidentUpdated={() => {
              refetchIncident();
              refetchTimeline();
            }}
          />

          {/* Root Cause & Resolution (if resolved) */}
          {(incident.rootCause || incident.resolution) && (
            <div className="card-surface p-5 border-emerald-200 bg-emerald-50/20 space-y-4">
              <div className="flex items-center gap-2 text-emerald-800 font-semibold text-xs uppercase tracking-wider">
                <CheckCircle2 className="w-4 h-4 text-emerald-600" />
                Resolution Details
              </div>

              {incident.rootCause && (
                <div>
                  <h4 className="text-xs font-bold text-slate-700 mb-1">Root Cause:</h4>
                  <p className="text-xs text-text bg-white p-3 rounded border border-border leading-relaxed">
                    {incident.rootCause}
                  </p>
                </div>
              )}

              {incident.resolution && (
                <div>
                  <h4 className="text-xs font-bold text-slate-700 mb-1">Corrective Action / Resolution:</h4>
                  <p className="text-xs text-text bg-white p-3 rounded border border-border leading-relaxed">
                    {incident.resolution}
                  </p>
                </div>
              )}
            </div>
          )}

          {/* Comments Section */}
          <div className="card-surface p-5 space-y-4">
            <div className="flex items-center justify-between border-b border-border pb-3">
              <div className="flex items-center gap-2">
                <MessageSquare className="w-4 h-4 text-muted" />
                <h3 className="text-sm font-bold text-text">Comments ({comments.length})</h3>
              </div>
            </div>

            {/* Existing Comments */}
            <div className="space-y-3">
              {comments.length === 0 ? (
                <p className="text-xs text-muted italic py-2">No comments added yet.</p>
              ) : (
                comments.map((c) => (
                  <div key={c.id} className="p-3 rounded-md bg-slate-50 border border-border space-y-1">
                    <div className="flex items-center justify-between text-xs">
                      <span className="font-semibold text-text">{c.userName}</span>
                      <span className="text-muted tabular-nums">
                        {new Date(c.createdAt).toLocaleString(undefined, {
                          month: 'short',
                          day: 'numeric',
                          hour: '2-digit',
                          minute: '2-digit',
                        })}
                      </span>
                    </div>
                    <p className="text-xs text-text whitespace-pre-wrap leading-relaxed">{c.body}</p>
                  </div>
                ))
              )}
            </div>

            {/* Add Comment Input */}
            {canUpdate && (
              <form
                onSubmit={(e) => {
                  e.preventDefault();
                  if (newCommentBody.trim()) addCommentMutation.mutate();
                }}
                className="pt-2 border-t border-border flex items-start gap-2"
              >
                <textarea
                  rows={2}
                  maxLength={4000}
                  placeholder="Leave a comment, investigation update, or diagnostic note..."
                  value={newCommentBody}
                  onChange={(e) => setNewCommentBody(e.target.value)}
                  className="flex-1 bg-slate-50 border border-border rounded-md p-2.5 text-xs text-text placeholder-muted focus:bg-white focus:border-primary focus:outline-none"
                />
                <button
                  type="submit"
                  disabled={addCommentMutation.isPending || !newCommentBody.trim()}
                  className="px-3 py-2 bg-primary hover:bg-primary-hover text-white rounded-md text-xs font-semibold transition-colors disabled:opacity-50 shrink-0 flex items-center gap-1.5"
                >
                  <Send className="w-3.5 h-3.5" />
                  Post
                </button>
              </form>
            )}
          </div>
        </div>

        {/* Right 1 Column: Metadata & Activity Timeline */}
        <div className="space-y-6">
          {/* Metadata Card */}
          <div className="card-surface p-4 space-y-3 text-xs">
            <h3 className="font-bold text-text uppercase tracking-wider text-[11px] border-b border-border pb-2">
              Incident Info
            </h3>

            <div className="space-y-2.5">
              <div className="flex justify-between items-center">
                <span className="text-muted">Service:</span>
                <span className="font-semibold text-text">{incident.serviceName || '—'}</span>
              </div>

              <div className="flex justify-between items-center">
                <span className="text-muted">Reporter:</span>
                <span className="font-medium text-text">{incident.reporterName || '—'}</span>
              </div>

              <div className="flex justify-between items-center">
                <span className="text-muted">Assignee:</span>
                <span className="font-medium text-text">
                  {incident.assigneeName || <span className="text-slate-400 italic">Unassigned</span>}
                </span>
              </div>

              <div className="flex justify-between items-center">
                <span className="text-muted">Category:</span>
                <span className="font-medium text-text">{incident.category || '—'}</span>
              </div>

              <div className="flex justify-between items-center">
                <span className="text-muted">Escalations:</span>
                <span className="font-semibold text-text">{incident.escalationCount}</span>
              </div>

              <div className="flex justify-between items-center">
                <span className="text-muted">Created:</span>
                <span className="text-text tabular-nums">
                  {new Date(incident.createdAt).toLocaleString(undefined, {
                    month: 'short',
                    day: 'numeric',
                    hour: '2-digit',
                    minute: '2-digit',
                  })}
                </span>
              </div>
            </div>

            {/* Tags */}
            {incident.tags && incident.tags.length > 0 && (
              <div className="pt-2 border-t border-border">
                <span className="text-muted block mb-1.5">Tags:</span>
                <div className="flex flex-wrap gap-1">
                  {incident.tags.map((t) => (
                    <span
                      key={t}
                      className="px-1.5 py-0.5 rounded bg-slate-100 text-slate-700 font-mono text-[10px]"
                    >
                      #{t}
                    </span>
                  ))}
                </div>
              </div>
            )}

            {canUpdate && (
              <div className="pt-2 border-t border-border">
                <button
                  onClick={() => setIsLogSnippetOpen(true)}
                  className="w-full text-center py-1.5 rounded border border-dashed border-border hover:bg-slate-50 text-xs font-semibold text-muted hover:text-text transition-colors flex items-center justify-center gap-1.5"
                >
                  <Code2 className="w-3.5 h-3.5" />
                  Attach Log Snippet
                </button>
              </div>
            )}
          </div>

          {/* Similar Incidents Card (pgvector similarity search) */}
          <div className="card-surface p-4 space-y-3 text-xs">
            <div className="flex items-center justify-between border-b border-border pb-2">
              <div className="flex items-center gap-1.5 font-bold text-text uppercase tracking-wider text-[11px]">
                <Sparkles className="w-3.5 h-3.5 text-primary" />
                Similar Resolved Incidents ({similarIncidents.length})
              </div>
              <span className="text-[10px] text-muted font-medium bg-slate-100 px-1.5 py-0.5 rounded">
                pgvector
              </span>
            </div>

            {similarLoading ? (
              <div className="py-4 text-center text-muted">
                <Loader2 className="w-4 h-4 animate-spin mx-auto text-primary mb-1" />
                <span className="text-[11px]">Searching resolved incident vectors...</span>
              </div>
            ) : similarIncidents.length === 0 ? (
              <p className="text-[11px] text-muted italic py-1 leading-relaxed">
                No sufficiently similar past incidents found in the knowledge base (≥35% match).
              </p>
            ) : (
              <div className="space-y-2.5 pt-1">
                {similarIncidents.map((sim: SimilarIncident) => {
                  const matchPct = Math.round(sim.similarity * 100);
                  return (
                    <div
                      key={sim.documentId}
                      className="p-3 rounded-lg bg-slate-50 border border-border space-y-2 hover:border-primary/40 transition-colors"
                    >
                      <div className="flex items-center justify-between gap-1">
                        <span className="px-1.5 py-0.5 rounded text-[10px] font-bold bg-emerald-50 text-emerald-700 border border-emerald-200 tabular-nums">
                          {matchPct}% Match
                        </span>
                        {sim.incidentId && (
                          <button
                            onClick={() => navigate(`/incidents/${sim.incidentId}`)}
                            className="text-primary hover:text-primary-hover font-semibold text-[11px] inline-flex items-center gap-1"
                          >
                            {sim.formattedNumber || sim.incidentNumber || 'View'}
                            <ExternalLink className="w-3 h-3" />
                          </button>
                        )}
                      </div>

                      <h4 className="font-semibold text-text text-xs leading-snug">
                        {sim.title}
                      </h4>

                      {sim.resolution && (
                        <div className="bg-white p-2 rounded border border-border/80 text-[11px] text-slate-700 leading-relaxed">
                          <strong className="text-slate-900 block text-[10px] uppercase font-bold text-emerald-800 mb-0.5">
                            Resolution:
                          </strong>
                          <p className="line-clamp-2">{sim.resolution}</p>
                        </div>
                      )}
                    </div>
                  );
                })}
              </div>
            )}
          </div>

          {/* Timeline Stream */}
          <div className="card-surface p-4 space-y-3 text-xs">
            <div className="flex items-center gap-1.5 font-bold text-text uppercase tracking-wider text-[11px] border-b border-border pb-2">
              <History className="w-3.5 h-3.5 text-muted" />
              Timeline History ({timeline.length})
            </div>

            <div className="space-y-3 pt-1">
              {timeline.map((ev) => (
                <div key={ev.id} className="flex gap-2.5 items-start text-xs leading-relaxed">
                  <div className="w-2 h-2 rounded-full bg-primary mt-1.5 shrink-0" />
                  <div className="flex-1">
                    <div className="flex items-center justify-between text-[11px]">
                      <span className="font-semibold text-text">{ev.eventType}</span>
                      <span className="text-muted tabular-nums">
                        {new Date(ev.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                      </span>
                    </div>
                    {ev.actorName && (
                      <span className="text-[11px] text-muted block">by {ev.actorName}</span>
                    )}

                    {/* Event payload hints */}
                    {ev.payload && typeof ev.payload === 'object' && (
                      <div className="mt-1 text-[11px] text-slate-600 bg-slate-50 p-1.5 rounded font-mono break-all">
                        {ev.eventType === 'STATUS_CHANGED' && (
                          <span>Status: {String(ev.payload.from)} → {String(ev.payload.to)}</span>
                        )}
                        {ev.eventType === 'ESCALATED' && (
                          <span>Priority: {String(ev.payload.from)} → {String(ev.payload.to)}</span>
                        )}
                        {ev.eventType === 'ASSIGNED' && (
                          <span>Assignee: {String(ev.payload.assigneeName)}</span>
                        )}
                        {ev.eventType === 'LOG_ADDED' && typeof ev.payload.snippet === 'string' && (
                          <pre className="text-[10px] whitespace-pre-wrap">{ev.payload.snippet.slice(0, 150)}...</pre>
                        )}
                      </div>
                    )}
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>

      {/* Escalate Modal */}
      {isEscalateOpen && (
        <div className="fixed inset-0 bg-slate-900/40 backdrop-blur-sm flex items-center justify-center p-4 z-50">
          <div className="card-surface p-5 max-w-md w-full shadow-lg space-y-4">
            <h3 className="text-sm font-bold text-text">Escalate Incident Priority</h3>
            <p className="text-xs text-muted">
              Current Priority is <span className="font-semibold text-text">{incident.priority}</span>. Escalating will increase priority to{' '}
              <span className="font-semibold text-p1-text">
                {incident.priority === 'P4' ? 'P3' : incident.priority === 'P3' ? 'P2' : 'P1'}
              </span>{' '}
              and recalculate SLA targets.
            </p>
            <textarea
              rows={3}
              placeholder="Reason for escalation (customer impact, SLA approaching...)"
              value={escalateReason}
              onChange={(e) => setEscalateReason(e.target.value)}
              className="w-full bg-slate-50 border border-border rounded p-2.5 text-xs text-text focus:bg-white focus:outline-none"
            />
            <div className="flex justify-end gap-2">
              <button
                type="button"
                onClick={() => setIsEscalateOpen(false)}
                className="px-3 py-1.5 rounded border border-border text-xs text-muted hover:text-text"
              >
                Cancel
              </button>
              <button
                type="button"
                disabled={escalateMutation.isPending}
                onClick={() => escalateMutation.mutate()}
                className="px-3 py-1.5 rounded bg-primary hover:bg-primary-hover text-white text-xs font-semibold"
              >
                Confirm Escalation
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Assign Modal */}
      {isAssignOpen && (
        <div className="fixed inset-0 bg-slate-900/40 backdrop-blur-sm flex items-center justify-center p-4 z-50">
          <div className="card-surface p-5 max-w-md w-full shadow-lg space-y-4">
            <h3 className="text-sm font-bold text-text">Assign Incident</h3>
            <div className="space-y-2">
              <label htmlFor="assignee-select" className="text-xs text-muted block">Select Engineer or Admin:</label>
              <select
                id="assignee-select"
                value={selectedAssigneeId}
                onChange={(e) => setSelectedAssigneeId(e.target.value)}
                className="w-full bg-slate-50 border border-border rounded p-2 text-xs text-text focus:bg-white focus:outline-none"
              >
                <option value="">Choose User...</option>
                {usersList
                  .filter((u) => u.enabled && (u.role === 'ENGINEER' || u.role === 'ADMIN'))
                  .map((u) => (
                    <option key={u.id} value={u.id}>
                      {u.name} ({u.role}) - {u.email}
                    </option>
                  ))}
              </select>
            </div>
            <div className="flex justify-end gap-2">
              <button
                type="button"
                onClick={() => setIsAssignOpen(false)}
                className="px-3 py-1.5 rounded border border-border text-xs text-muted hover:text-text"
              >
                Cancel
              </button>
              <button
                type="button"
                disabled={assignMutation.isPending || !selectedAssigneeId}
                onClick={() => assignMutation.mutate()}
                className="px-3 py-1.5 rounded bg-primary hover:bg-primary-hover text-white text-xs font-semibold disabled:opacity-50"
              >
                Assign
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Resolve Modal */}
      {isResolveOpen && (
        <div className="fixed inset-0 bg-slate-900/40 backdrop-blur-sm flex items-center justify-center p-4 z-50">
          <div className="card-surface p-5 max-w-lg w-full shadow-lg space-y-4">
            <h3 className="text-sm font-bold text-text">Resolve Incident</h3>
            <p className="text-xs text-muted">
              Resolving requires documenting the root cause and resolution. These details are stored for future postmortems and AI training.
            </p>
            <div className="space-y-3">
              <div>
                <label htmlFor="root-cause-input" className="block text-xs font-semibold text-text mb-1">Root Cause *</label>
                <textarea
                  id="root-cause-input"
                  rows={3}
                  required
                  placeholder="e.g., PostgreSQL connection pool exhaustion due to leak in reporting service..."
                  value={rootCause}
                  onChange={(e) => setRootCause(e.target.value)}
                  className="w-full bg-slate-50 border border-border rounded p-2 text-xs text-text focus:bg-white focus:outline-none"
                />
              </div>
              <div>
                <label htmlFor="resolution-input" className="block text-xs font-semibold text-text mb-1">Resolution / Action Taken *</label>
                <textarea
                  id="resolution-input"
                  rows={3}
                  required
                  placeholder="e.g., Patched connection leak in v1.4.1 and restarted deployment workers..."
                  value={resolution}
                  onChange={(e) => setResolution(e.target.value)}
                  className="w-full bg-slate-50 border border-border rounded p-2 text-xs text-text focus:bg-white focus:outline-none"
                />
              </div>
            </div>
            <div className="flex justify-end gap-2">
              <button
                type="button"
                onClick={() => setIsResolveOpen(false)}
                className="px-3 py-1.5 rounded border border-border text-xs text-muted hover:text-text"
              >
                Cancel
              </button>
              <button
                type="button"
                disabled={transitionMutation.isPending || !rootCause.trim() || !resolution.trim()}
                onClick={() =>
                  transitionMutation.mutate({
                    targetStatus: 'RESOLVED',
                    rc: rootCause.trim(),
                    res: resolution.trim(),
                  })
                }
                className="px-3 py-1.5 rounded bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-semibold disabled:opacity-50"
              >
                Confirm Resolution
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Attach Log Snippet Modal */}
      {isLogSnippetOpen && (
        <div className="fixed inset-0 bg-slate-900/40 backdrop-blur-sm flex items-center justify-center p-4 z-50">
          <div className="card-surface p-5 max-w-lg w-full shadow-lg space-y-4">
            <h3 className="text-sm font-bold text-text">Attach Diagnostic Log Snippet</h3>
            <textarea
              rows={6}
              placeholder="Paste log output (max 4 KB)..."
              value={newLogSnippet}
              onChange={(e) => setNewLogSnippet(e.target.value)}
              className="w-full bg-slate-900 border border-slate-700 text-slate-100 font-mono text-xs rounded p-3 focus:outline-none"
            />
            <div className="flex justify-end gap-2">
              <button
                type="button"
                onClick={() => setIsLogSnippetOpen(false)}
                className="px-3 py-1.5 rounded border border-border text-xs text-muted hover:text-text"
              >
                Cancel
              </button>
              <button
                type="button"
                disabled={addLogSnippetMutation.isPending || !newLogSnippet.trim()}
                onClick={() => addLogSnippetMutation.mutate()}
                className="px-3 py-1.5 rounded bg-primary hover:bg-primary-hover text-white text-xs font-semibold disabled:opacity-50"
              >
                Attach Snippet
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
