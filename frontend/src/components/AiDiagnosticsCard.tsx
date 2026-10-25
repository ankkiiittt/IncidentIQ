// src/components/AiDiagnosticsCard.tsx
import React, { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  AlertCircle,
  AlertTriangle,
  BrainCircuit,
  Check,
  CheckCircle2,
  ChevronDown,
  ChevronRight,
  FileText,
  Loader2,
  Play,
  RotateCcw,
  Shield,
  Sparkles,
  ThumbsDown,
  ThumbsUp,
  X,
} from 'lucide-react';
import {
  acceptSuggestion,
  AiAnalysisResponse,
  getAnalysesForIncident,
  RecommendationResponse,
  submitRecommendationFeedback,
  triggerAnalysis,
} from '../api/ai';
import { IncidentDetail } from '../types';

interface AiDiagnosticsCardProps {
  incident: IncidentDetail;
  canManage: boolean;
  onIncidentUpdated: () => void;
}

export const AiDiagnosticsCard: React.FC<AiDiagnosticsCardProps> = ({
  incident,
  canManage,
  onIncidentUpdated,
}) => {
  const queryClient = useQueryClient();
  const [selectedAnalysisId, setSelectedAnalysisId] = useState<string | null>(null);
  const [expandedChunkId, setExpandedChunkId] = useState<string | null>(null);
  const [feedbackModalRec, setFeedbackModalRec] = useState<RecommendationResponse | null>(null);
  const [feedbackRating, setFeedbackRating] = useState<'HELPFUL' | 'NOT_HELPFUL'>('HELPFUL');
  const [feedbackComment, setFeedbackComment] = useState('');
  const [statusMessage, setStatusMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  // Queries
  const {
    data: analyses = [],
    isLoading: analysesLoading,
    refetch: refetchAnalyses,
  } = useQuery({
    queryKey: ['incident-ai-analyses', incident.id],
    queryFn: () => getAnalysesForIncident(incident.id),
    enabled: !!incident.id,
  });

  const latestAnalysis: AiAnalysisResponse | undefined =
    selectedAnalysisId
      ? analyses.find((a) => a.id === selectedAnalysisId)
      : analyses.length > 0
      ? analyses[0]
      : undefined;

  // Mutations
  const triggerMutation = useMutation({
    mutationFn: () => triggerAnalysis(incident.id),
    onSuccess: (data) => {
      setSelectedAnalysisId(data.id);
      setStatusMessage({ type: 'success', text: 'AI diagnostic run initiated successfully.' });
      queryClient.invalidateQueries({ queryKey: ['incident-ai-analyses', incident.id] });
      queryClient.invalidateQueries({ queryKey: ['incident-timeline', incident.id] });
    },
    onError: (err: Error) => {
      setStatusMessage({ type: 'error', text: err.message || 'Failed to trigger AI diagnostics.' });
    },
  });

  const acceptSuggestionMutation = useMutation({
    mutationFn: ({ acceptCategory, acceptPriority }: { acceptCategory: boolean; acceptPriority: boolean }) =>
      acceptSuggestion(incident.id, {
        acceptCategory,
        acceptPriority,
        version: incident.version,
      }),
    onSuccess: () => {
      setStatusMessage({ type: 'success', text: 'Triage recommendation accepted and applied to incident.' });
      onIncidentUpdated();
      queryClient.invalidateQueries({ queryKey: ['incident', incident.id] });
      queryClient.invalidateQueries({ queryKey: ['incident-timeline', incident.id] });
    },
    onError: (err: Error) => {
      setStatusMessage({ type: 'error', text: err.message || 'Failed to accept AI suggestion.' });
    },
  });

  const feedbackMutation = useMutation({
    mutationFn: ({
      recId,
      rating,
      comment,
      status,
    }: {
      recId: string;
      rating: 'HELPFUL' | 'NOT_HELPFUL';
      comment?: string;
      status?: 'SUGGESTED' | 'ACCEPTED' | 'REJECTED' | 'COMPLETED';
    }) =>
      submitRecommendationFeedback(recId, {
        rating,
        comment,
        status,
      }),
    onSuccess: () => {
      setFeedbackModalRec(null);
      setFeedbackComment('');
      setStatusMessage({ type: 'success', text: 'Feedback submitted. Thank you for training our model!' });
      refetchAnalyses();
    },
    onError: (err: Error) => {
      setStatusMessage({ type: 'error', text: err.message || 'Failed to submit recommendation feedback.' });
    },
  });

  const confidencePct = latestAnalysis?.confidence != null ? Math.round(latestAnalysis.confidence * 100) : 0;
  const isConfidenceHigh = confidencePct >= 70;
  const isConfidenceMedium = confidencePct >= 40 && confidencePct < 70;

  // Category and priority difference check
  const categoryDiffers =
    latestAnalysis?.category &&
    latestAnalysis.category !== 'OTHER' &&
    latestAnalysis.category !== incident.category;

  const priorityDiffers =
    latestAnalysis?.suggestedPriority &&
    latestAnalysis.suggestedPriority !== incident.priority;

  return (
    <div className="card-surface p-5 space-y-5 border-l-4 border-l-indigo-600 shadow-sm transition-all" id="ai-diagnostics-panel">
      {/* 1. Mandatory Advisory Disclaimer */}
      <div className="flex items-start gap-3 p-3 rounded-md bg-amber-500/10 border border-amber-500/20 text-xs text-amber-900 dark:text-amber-200">
        <Shield className="w-4 h-4 text-amber-600 dark:text-amber-400 shrink-0 mt-0.5" />
        <div className="leading-relaxed">
          <strong className="font-semibold block mb-0.5 text-amber-950 dark:text-amber-100">
            Advisory Intelligence Only
          </strong>
          Recommendations are suggestions generated by machine intelligence and historical telemetry. IncidentIQ
          will never execute remediation actions or alter incident parameters without human confirmation.
        </div>
      </div>

      {/* 2. Header and Diagnostic Controls */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 border-b border-border pb-4">
        <div className="flex items-center gap-2.5">
          <div className="p-2 rounded-lg bg-indigo-50 text-indigo-700 border border-indigo-200">
            <BrainCircuit className="w-5 h-5" />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h3 className="text-sm font-bold text-text">AI Diagnostics & RCA Pipeline</h3>
              {latestAnalysis && (
                <span
                  className={`inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-bold uppercase tracking-wider ${
                    latestAnalysis.status === 'COMPLETED'
                      ? 'bg-emerald-100 text-emerald-800'
                      : latestAnalysis.status === 'INSUFFICIENT_EVIDENCE'
                      ? 'bg-amber-100 text-amber-800'
                      : latestAnalysis.status === 'RUNNING'
                      ? 'bg-blue-100 text-blue-800'
                      : 'bg-red-100 text-red-800'
                  }`}
                >
                  {latestAnalysis.status === 'COMPLETED' && <CheckCircle2 className="w-3 h-3" />}
                  {latestAnalysis.status === 'INSUFFICIENT_EVIDENCE' && <AlertTriangle className="w-3 h-3" />}
                  {latestAnalysis.status === 'RUNNING' && <Loader2 className="w-3 h-3 animate-spin" />}
                  {latestAnalysis.status === 'FAILED' && <AlertCircle className="w-3 h-3" />}
                  {latestAnalysis.status.replace('_', ' ')}
                </span>
              )}
            </div>
            <p className="text-xs text-muted mt-0.5">
              RCA v1 grounding engine • Runbooks & telemetry correlation
            </p>
          </div>
        </div>

        <div className="flex items-center gap-2 flex-wrap">
          {analyses.length > 1 && (
            <select
              aria-label="Select AI analysis run"
              value={latestAnalysis?.id || ''}
              onChange={(e) => setSelectedAnalysisId(e.target.value)}
              className="text-xs bg-slate-50 border border-border rounded px-2.5 py-1.5 text-text font-medium focus:bg-white focus:outline-none"
            >
              {analyses.map((a, idx) => (
                <option key={a.id} value={a.id}>
                  Run #{analyses.length - idx} ({new Date(a.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })})
                </option>
              ))}
            </select>
          )}

          {canManage && (
            <button
              onClick={() => triggerMutation.mutate()}
              disabled={triggerMutation.isPending}
              className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-md bg-indigo-600 hover:bg-indigo-700 text-white text-xs font-semibold shadow-sm transition-colors disabled:opacity-50"
              title="Manual trigger (rate limited to 10/hr per user)"
            >
              {triggerMutation.isPending ? (
                <>
                  <Loader2 className="w-3.5 h-3.5 animate-spin" />
                  Analyzing...
                </>
              ) : analyses.length > 0 ? (
                <>
                  <RotateCcw className="w-3.5 h-3.5" />
                  Re-run Diagnostics
                </>
              ) : (
                <>
                  <Play className="w-3.5 h-3.5 fill-current" />
                  Run AI Diagnostics
                </>
              )}
            </button>
          )}
        </div>
      </div>

      {statusMessage && (
        <div
          className={`p-3 rounded-md text-xs flex items-center justify-between ${
            statusMessage.type === 'success'
              ? 'bg-emerald-50 text-emerald-800 border border-emerald-200'
              : 'bg-red-50 text-red-800 border border-red-200'
          }`}
        >
          <span>{statusMessage.text}</span>
          <button onClick={() => setStatusMessage(null)} className="font-bold ml-2 text-slate-500 hover:text-slate-800">
            ×
          </button>
        </div>
      )}

      {/* 3. Empty State (No Analysis Run Yet) */}
      {analysesLoading ? (
        <div className="py-12 flex flex-col items-center justify-center text-muted">
          <Loader2 className="w-6 h-6 animate-spin text-indigo-600 mb-2" />
          <span className="text-xs">Loading AI diagnostics data...</span>
        </div>
      ) : !latestAnalysis ? (
        <div className="py-8 px-4 text-center rounded-lg border border-dashed border-border bg-slate-50/50 space-y-3">
          <Sparkles className="w-8 h-8 text-indigo-500 mx-auto opacity-80" />
          <div className="max-w-md mx-auto">
            <h4 className="text-xs font-bold text-text">No Diagnostics Run for this Incident</h4>
            <p className="text-[11px] text-muted mt-1 leading-relaxed">
              Launch our grounded AI pipeline to correlate telemetry with operational runbooks, extract citations, and draft actionable remediation steps.
            </p>
          </div>
          {canManage && (
            <button
              onClick={() => triggerMutation.mutate()}
              disabled={triggerMutation.isPending}
              className="inline-flex items-center gap-1.5 px-3.5 py-1.5 rounded-md bg-indigo-600 hover:bg-indigo-700 text-white text-xs font-semibold shadow-sm transition-colors"
            >
              <Play className="w-3.5 h-3.5 fill-current" />
              Run AI Diagnostics Now
            </button>
          )}
        </div>
      ) : (
        <div className="space-y-5">
          {/* 4. Confidence Gauge & Metadata Banner */}
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4 p-4 rounded-lg bg-slate-50 border border-border">
            {/* Confidence Score Bar */}
            <div className="md:col-span-1 space-y-1.5">
              <div className="flex justify-between items-center text-xs">
                <span className="font-semibold text-muted text-[11px] uppercase tracking-wider">Grounding Confidence</span>
                <span className="font-bold text-text font-mono">{confidencePct}%</span>
              </div>
              <div className="w-full h-2.5 rounded-full bg-slate-200 overflow-hidden">
                <div
                  className={`h-full transition-all duration-500 rounded-full ${
                    isConfidenceHigh
                      ? 'bg-emerald-500'
                      : isConfidenceMedium
                      ? 'bg-amber-500'
                      : 'bg-slate-400'
                  }`}
                  style={{ width: `${Math.max(5, confidencePct)}%` }}
                />
              </div>
              <span className="text-[10px] text-muted block">
                {isConfidenceHigh
                  ? 'High grounding confidence with operational runbooks'
                  : isConfidenceMedium
                  ? 'Medium confidence; corroboration recommended'
                  : 'Low confidence / insufficient similarity evidence'}
              </span>
            </div>

            {/* Run Metadata */}
            <div className="md:col-span-2 flex flex-wrap items-center justify-between gap-y-2 text-xs border-t md:border-t-0 md:border-l border-border pt-2 md:pt-0 md:pl-4">
              <div>
                <span className="text-[11px] text-muted block">Execution Model:</span>
                <span className="font-mono text-[11px] font-semibold text-text">{latestAnalysis.model}</span>
              </div>
              <div>
                <span className="text-[11px] text-muted block">Pipeline Latency:</span>
                <span className="font-mono text-[11px] font-semibold text-text">{latestAnalysis.latencyMs} ms</span>
              </div>
              <div>
                <span className="text-[11px] text-muted block">Trigger:</span>
                <span className="font-mono text-[11px] font-semibold text-text">
                  {latestAnalysis.trigger}
                  {latestAnalysis.requestedByName ? ` (${latestAnalysis.requestedByName})` : ''}
                </span>
              </div>
              <div>
                <span className="text-[11px] text-muted block">Evidence Chunks:</span>
                <span className="font-mono text-[11px] font-semibold text-text">{latestAnalysis.evidence?.length || 0} cited</span>
              </div>
            </div>
          </div>

          {/* 5. Suggested Triage Banner (Category & Priority) */}
          {(categoryDiffers || priorityDiffers) && canManage && (
            <div className="p-4 rounded-lg bg-indigo-50 border border-indigo-200 flex flex-col sm:flex-row sm:items-center justify-between gap-3">
              <div className="space-y-1">
                <div className="flex items-center gap-1.5 text-xs font-bold text-indigo-900 uppercase tracking-wider">
                  <Sparkles className="w-3.5 h-3.5 text-indigo-600" />
                  Suggested Triage Adjustment
                </div>
                <div className="text-xs text-indigo-900 flex flex-wrap gap-x-4 gap-y-1">
                  {categoryDiffers && (
                    <span>
                      Category:{' '}
                      <strong className="text-slate-500 line-through">{incident.category}</strong> →{' '}
                      <strong className="text-indigo-950 font-bold">{latestAnalysis.category}</strong>
                    </span>
                  )}
                  {priorityDiffers && (
                    <span>
                      Priority:{' '}
                      <strong className="text-slate-500 line-through">{incident.priority}</strong> →{' '}
                      <strong className="text-indigo-950 font-bold">{latestAnalysis.suggestedPriority}</strong>
                    </span>
                  )}
                </div>
              </div>

              <button
                onClick={() =>
                  acceptSuggestionMutation.mutate({
                    acceptCategory: !!categoryDiffers,
                    acceptPriority: !!priorityDiffers,
                  })
                }
                disabled={acceptSuggestionMutation.isPending}
                className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-md bg-indigo-600 hover:bg-indigo-700 text-white text-xs font-semibold shadow-sm transition-colors shrink-0 disabled:opacity-50"
              >
                {acceptSuggestionMutation.isPending ? (
                  <Loader2 className="w-3.5 h-3.5 animate-spin" />
                ) : (
                  <Check className="w-3.5 h-3.5" />
                )}
                Accept Suggestion
              </button>
            </div>
          )}

          {/* 6. Executive Summary */}
          <div className="space-y-2">
            <h4 className="text-xs font-bold text-text uppercase tracking-wider flex items-center gap-1.5">
              <FileText className="w-3.5 h-3.5 text-muted" />
              Executive Diagnostic Summary
            </h4>
            <div className="p-3.5 rounded-lg bg-slate-50 border border-border text-xs leading-relaxed text-text font-normal">
              {latestAnalysis.summary}
            </div>
          </div>

          {/* 7. Ranked Root Causes with Cited Grounded Evidence */}
          <div className="space-y-3">
            <h4 className="text-xs font-bold text-text uppercase tracking-wider flex items-center justify-between">
              <span>Ranked Root Cause Hypotheses ({latestAnalysis.rootCauses?.length || 0})</span>
              <span className="text-[10px] text-muted font-normal lowercase">Grounded via vector chunks</span>
            </h4>

            {(!latestAnalysis.rootCauses || latestAnalysis.rootCauses.length === 0) ? (
              <p className="text-xs text-muted italic p-3 rounded bg-slate-50 border border-border">
                No root cause hypotheses formulated due to insufficient evidence match.
              </p>
            ) : (
              <div className="space-y-3">
                {latestAnalysis.rootCauses.map((rc, idx) => {
                  const rcConfidencePct = Math.round(rc.confidence * 100);
                  return (
                    <div
                      key={idx}
                      className="p-3.5 rounded-lg border border-border bg-white shadow-xs space-y-2.5"
                    >
                      <div className="flex items-start justify-between gap-2">
                        <div className="flex items-start gap-2">
                          <span className="w-5 h-5 rounded-full bg-slate-100 text-slate-700 text-[11px] font-bold flex items-center justify-center shrink-0 mt-0.5">
                            {idx + 1}
                          </span>
                          <p className="text-xs font-semibold text-text leading-snug">{rc.cause}</p>
                        </div>
                        <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-slate-100 text-slate-800 font-mono shrink-0">
                          {rcConfidencePct}% Confidence
                        </span>
                      </div>

                      {/* Evidence Citations */}
                      {rc.evidenceRefs && rc.evidenceRefs.length > 0 && (
                        <div className="pt-2 border-t border-border/60">
                          <span className="text-[10px] font-bold text-muted uppercase tracking-wider block mb-1.5">
                            Cited Evidence ({rc.evidenceRefs.length}):
                          </span>
                          <div className="space-y-1.5">
                            {rc.evidenceRefs.map((chunkIdStr) => {
                              const chunkItem = latestAnalysis.evidence?.find(
                                (e) => e.chunkId === chunkIdStr
                              );
                              const isExpanded = expandedChunkId === chunkIdStr;
                              if (!chunkItem) return null;

                              return (
                                <div
                                  key={chunkIdStr}
                                  className="rounded border border-border bg-slate-50/80 text-xs overflow-hidden"
                                >
                                  <button
                                    onClick={() => setExpandedChunkId(isExpanded ? null : chunkIdStr)}
                                    className="w-full text-left px-2.5 py-1.5 flex items-center justify-between gap-2 hover:bg-slate-100 transition-colors"
                                  >
                                    <div className="flex items-center gap-1.5 truncate">
                                      {isExpanded ? (
                                        <ChevronDown className="w-3 h-3 text-muted shrink-0" />
                                      ) : (
                                        <ChevronRight className="w-3 h-3 text-muted shrink-0" />
                                      )}
                                      <span className="font-semibold text-text text-[11px] truncate">
                                        {chunkItem.documentTitle}
                                      </span>
                                      <span className="px-1 py-0.2 rounded bg-slate-200 text-slate-700 text-[9px] uppercase font-mono">
                                        {chunkItem.sourceType}
                                      </span>
                                    </div>
                                    <span className="text-[10px] text-emerald-700 font-mono font-bold shrink-0">
                                      {Math.round(chunkItem.similarity * 100)}% match
                                    </span>
                                  </button>

                                  {isExpanded && (
                                    <div className="p-2.5 bg-white border-t border-border text-[11px] text-slate-700 font-mono leading-relaxed whitespace-pre-wrap">
                                      {chunkItem.snippet}
                                    </div>
                                  )}
                                </div>
                              );
                            })}
                          </div>
                        </div>
                      )}
                    </div>
                  );
                })}
              </div>
            )}
          </div>

          {/* 8. Recommended Remediation Actions */}
          <div className="space-y-3">
            <h4 className="text-xs font-bold text-text uppercase tracking-wider flex items-center justify-between">
              <span>Remediation Action Recommendations ({latestAnalysis.recommendations?.length || 0})</span>
              <span className="text-[10px] text-muted font-normal lowercase">Advisory steps</span>
            </h4>

            {(!latestAnalysis.recommendations || latestAnalysis.recommendations.length === 0) ? (
              <p className="text-xs text-muted italic p-3 rounded bg-slate-50 border border-border">
                No remediation actions generated.
              </p>
            ) : (
              <div className="space-y-2.5">
                {latestAnalysis.recommendations.map((rec) => {
                  const isAccepted = rec.status === 'ACCEPTED';
                  const isRejected = rec.status === 'REJECTED';

                  return (
                    <div
                      key={rec.id}
                      className={`p-3.5 rounded-lg border transition-all ${
                        isAccepted
                          ? 'border-emerald-300 bg-emerald-50/20'
                          : isRejected
                          ? 'border-slate-300 bg-slate-50 opacity-60'
                          : 'border-border bg-white shadow-xs'
                      }`}
                    >
                      <div className="flex flex-col sm:flex-row sm:items-start justify-between gap-3">
                        <div className="space-y-1">
                          <div className="flex items-center gap-2">
                            <span className="px-1.5 py-0.5 rounded text-[10px] font-bold bg-slate-100 text-slate-700 font-mono">
                              Step {rec.position}
                            </span>
                            <h5 className="font-semibold text-xs text-text">{rec.action}</h5>
                            <span
                              className={`px-1.5 py-0.2 rounded text-[9px] font-bold uppercase tracking-wider ${
                                isAccepted
                                  ? 'bg-emerald-100 text-emerald-800'
                                  : isRejected
                                  ? 'bg-slate-200 text-slate-700'
                                  : 'bg-indigo-50 text-indigo-700 border border-indigo-200'
                              }`}
                            >
                              {rec.status}
                            </span>
                          </div>
                          <p className="text-[11px] text-muted leading-relaxed pl-1">{rec.rationale}</p>
                        </div>

                        {/* Interactive Action Controls */}
                        {canManage && (
                          <div className="flex items-center gap-1.5 shrink-0 pt-1 sm:pt-0">
                            {rec.status === 'SUGGESTED' && (
                              <>
                                <button
                                  onClick={() =>
                                    feedbackMutation.mutate({
                                      recId: rec.id,
                                      rating: 'HELPFUL',
                                      status: 'ACCEPTED',
                                    })
                                  }
                                  className="inline-flex items-center gap-1 px-2 py-1 rounded bg-emerald-50 text-emerald-700 border border-emerald-200 hover:bg-emerald-100 text-[11px] font-semibold transition-colors"
                                  title="Accept this recommended remediation step"
                                >
                                  <Check className="w-3 h-3" />
                                  Accept
                                </button>
                                <button
                                  onClick={() =>
                                    feedbackMutation.mutate({
                                      recId: rec.id,
                                      rating: 'NOT_HELPFUL',
                                      status: 'REJECTED',
                                    })
                                  }
                                  className="inline-flex items-center gap-1 px-2 py-1 rounded bg-slate-50 text-slate-600 border border-border hover:bg-slate-100 text-[11px] font-semibold transition-colors"
                                  title="Reject recommendation"
                                >
                                  <X className="w-3 h-3" />
                                  Dismiss
                                </button>
                              </>
                            )}

                            {/* Thumbs Feedback */}
                            <button
                              onClick={() => {
                                setFeedbackModalRec(rec);
                                setFeedbackRating('HELPFUL');
                              }}
                              className={`p-1.5 rounded transition-colors ${
                                rec.feedbackRating === 'HELPFUL'
                                  ? 'bg-emerald-100 text-emerald-800 font-bold'
                                  : 'text-muted hover:text-text hover:bg-slate-100'
                              }`}
                              title="Helpful recommendation"
                            >
                              <ThumbsUp className="w-3.5 h-3.5" />
                            </button>
                            <button
                              onClick={() => {
                                setFeedbackModalRec(rec);
                                setFeedbackRating('NOT_HELPFUL');
                              }}
                              className={`p-1.5 rounded transition-colors ${
                                rec.feedbackRating === 'NOT_HELPFUL'
                                  ? 'bg-red-100 text-red-800 font-bold'
                                  : 'text-muted hover:text-text hover:bg-slate-100'
                              }`}
                              title="Not helpful / Irrelevant"
                            >
                              <ThumbsDown className="w-3.5 h-3.5" />
                            </button>
                          </div>
                        )}
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        </div>
      )}

      {/* 9. Feedback Comment Modal */}
      {feedbackModalRec && (
        <div className="fixed inset-0 bg-slate-900/40 backdrop-blur-sm flex items-center justify-center p-4 z-50">
          <form
            onSubmit={(e) => {
              e.preventDefault();
              feedbackMutation.mutate({
                recId: feedbackModalRec.id,
                rating: feedbackRating,
                comment: feedbackComment.trim() || undefined,
              });
            }}
            className="card-surface p-5 max-w-md w-full shadow-lg space-y-4"
          >
            <div className="flex items-center justify-between border-b border-border pb-2">
              <h4 className="text-xs font-bold text-text uppercase tracking-wider flex items-center gap-1.5">
                {feedbackRating === 'HELPFUL' ? (
                  <ThumbsUp className="w-4 h-4 text-emerald-600" />
                ) : (
                  <ThumbsDown className="w-4 h-4 text-red-600" />
                )}
                Recommendation Feedback
              </h4>
              <button
                type="button"
                onClick={() => setFeedbackModalRec(null)}
                className="text-muted hover:text-text font-bold"
              >
                ×
              </button>
            </div>

            <p className="text-xs text-text font-medium leading-relaxed">
              "{feedbackModalRec.action}"
            </p>

            <div className="space-y-1.5">
              <label className="block text-xs font-semibold text-muted">
                Additional Comments or Corrections (Optional)
              </label>
              <textarea
                rows={3}
                placeholder="What made this recommendation helpful or inaccurate?"
                value={feedbackComment}
                onChange={(e) => setFeedbackComment(e.target.value)}
                className="w-full bg-slate-50 border border-border rounded p-2 text-xs text-text focus:bg-white focus:outline-none"
              />
            </div>

            <div className="flex justify-end gap-2 pt-2 border-t border-border">
              <button
                type="button"
                onClick={() => setFeedbackModalRec(null)}
                className="px-3 py-1.5 rounded border border-border text-xs text-muted hover:text-text"
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={feedbackMutation.isPending}
                className="px-3 py-1.5 rounded bg-primary hover:bg-primary-hover text-white text-xs font-semibold disabled:opacity-50"
              >
                Submit Feedback
              </button>
            </div>
          </form>
        </div>
      )}
    </div>
  );
};
