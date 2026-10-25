// src/api/knowledge.ts
// Knowledge Base and Vector Search API Client

import { apiFetch } from './client';
import {
  AccessLevel,
  DocumentSourceType,
  KnowledgeChunk,
  KnowledgeDocument,
  PagedResponse,
  SemanticSearchResult,
  SimilarIncident,
} from '../types';

export interface ListKnowledgeParams {
  sourceType?: DocumentSourceType;
  serviceId?: string;
  page?: number;
  size?: number;
}

export interface CreateKnowledgeDocumentPayload {
  title: string;
  sourceType: DocumentSourceType;
  accessLevel: AccessLevel;
  serviceId?: string;
  content: string;
}

export interface SemanticSearchPayload {
  query: string;
  sourceType?: DocumentSourceType;
  serviceId?: string;
  minSimilarity?: number;
  limit?: number;
}

export async function listKnowledgeDocumentsApi(
  params?: ListKnowledgeParams
): Promise<PagedResponse<KnowledgeDocument>> {
  const queryParams = new URLSearchParams();
  if (params?.sourceType) queryParams.set('sourceType', params.sourceType);
  if (params?.serviceId) queryParams.set('serviceId', params.serviceId);
  if (params?.page !== undefined) queryParams.set('page', params.page.toString());
  if (params?.size !== undefined) queryParams.set('size', params.size.toString());

  const url = `/api/knowledge/documents${queryParams.toString() ? `?${queryParams.toString()}` : ''}`;
  return apiFetch<PagedResponse<KnowledgeDocument>>(url);
}

export async function getKnowledgeDocumentApi(id: string): Promise<KnowledgeDocument> {
  return apiFetch<KnowledgeDocument>(`/api/knowledge/documents/${id}`);
}

export async function getKnowledgeDocumentChunksApi(id: string): Promise<KnowledgeChunk[]> {
  return apiFetch<KnowledgeChunk[]>(`/api/knowledge/documents/${id}/chunks`);
}

export async function createKnowledgeDocumentApi(
  payload: CreateKnowledgeDocumentPayload
): Promise<KnowledgeDocument> {
  return apiFetch<KnowledgeDocument>('/api/knowledge/documents', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export async function semanticSearchApi(
  payload: SemanticSearchPayload
): Promise<SemanticSearchResult[]> {
  return apiFetch<SemanticSearchResult[]>('/api/knowledge/search', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export async function getSimilarIncidentsApi(
  incidentId: string,
  minSimilarity?: number,
  limit?: number
): Promise<SimilarIncident[]> {
  const queryParams = new URLSearchParams();
  if (minSimilarity !== undefined) queryParams.set('minSimilarity', minSimilarity.toString());
  if (limit !== undefined) queryParams.set('limit', limit.toString());

  const url = `/api/incidents/${incidentId}/similar${
    queryParams.toString() ? `?${queryParams.toString()}` : ''
  }`;
  return apiFetch<SimilarIncident[]>(url);
}
