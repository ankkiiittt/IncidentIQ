import { apiFetch } from './client';

export type AnalysisTrigger = 'AUTO' | 'MANUAL';
export type AnalysisStatus = 'PENDING' | 'RUNNING' | 'COMPLETED' | 'INSUFFICIENT_EVIDENCE' | 'FAILED';
export type RecommendationStatus = 'SUGGESTED' | 'ACCEPTED' | 'REJECTED' | 'COMPLETED';
export type FeedbackRating = 'HELPFUL' | 'NOT_HELPFUL';

export interface EvidenceItem {
  chunkId: string;
  documentId: string;
  documentTitle: string;
  sourceType: string;
  similarity: number;
  snippet: string;
}

export interface RootCauseHypothesis {
  cause: string;
  confidence: number;
  evidenceRefs: string[];
}

export interface RecommendationResponse {
  id: string;
  position: number;
  action: string;
  rationale: string;
  status: RecommendationStatus;
  feedbackRating?: FeedbackRating | null;
  feedbackComment?: string | null;
}

export interface AiAnalysisResponse {
  id: string;
  incidentId: string;
  trigger: AnalysisTrigger;
  requestedByName?: string | null;
  status: AnalysisStatus;
  model: string;
  embeddingModel: string;
  promptVersion: string;
  category?: string | null;
  suggestedPriority?: string | null;
  summary: string;
  confidence?: number | null;
  rootCauses: RootCauseHypothesis[];
  evidence: EvidenceItem[];
  evidenceSufficient: boolean;
  inputTokens?: number | null;
  outputTokens?: number | null;
  latencyMs: number;
  errorCode?: string | null;
  createdAt: string;
  completedAt?: string | null;
  recommendations: RecommendationResponse[];
}

export interface AcceptSuggestionPayload {
  acceptCategory: boolean;
  acceptPriority: boolean;
  version: number;
}

export interface RecommendationFeedbackPayload {
  rating: FeedbackRating;
  status?: RecommendationStatus;
  comment?: string;
}

import { IncidentDetail } from '../types';

export interface AppSettingResponse {
  key: string;
  value: unknown;
  description: string;
  updatedAt: string;
  updatedByName?: string | null;
}

export interface UpdateAppSettingPayload {
  value: unknown;
  description?: string;
}

export async function triggerAnalysis(incidentId: string): Promise<AiAnalysisResponse> {
  return apiFetch<AiAnalysisResponse>(`/api/incidents/${incidentId}/ai/analyze`, {
    method: 'POST',
  });
}

export async function getAnalysesForIncident(incidentId: string): Promise<AiAnalysisResponse[]> {
  return apiFetch<AiAnalysisResponse[]>(`/api/incidents/${incidentId}/ai/analyses`);
}

export async function getAnalysis(incidentId: string, analysisId: string): Promise<AiAnalysisResponse> {
  return apiFetch<AiAnalysisResponse>(`/api/incidents/${incidentId}/ai/analyses/${analysisId}`);
}

export async function acceptSuggestion(
  incidentId: string,
  payload: AcceptSuggestionPayload
): Promise<IncidentDetail> {
  return apiFetch<IncidentDetail>(`/api/incidents/${incidentId}/ai/accept-suggestion`, {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export async function submitRecommendationFeedback(
  recommendationId: string,
  payload: RecommendationFeedbackPayload
): Promise<RecommendationResponse> {
  return apiFetch<RecommendationResponse>(`/api/recommendations/${recommendationId}/feedback`, {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export async function getAdminSettings(): Promise<AppSettingResponse[]> {
  return apiFetch<AppSettingResponse[]>('/api/admin/settings');
}

export async function updateAdminSetting(key: string, payload: UpdateAppSettingPayload): Promise<AppSettingResponse> {
  return apiFetch<AppSettingResponse>(`/api/admin/settings/${key}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  });
}
