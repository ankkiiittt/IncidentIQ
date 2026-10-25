// src/api/incidents.ts
import { apiFetch } from './client';
import {
  Comment,
  IncidentDetail,
  IncidentEvent,
  IncidentPriority,
  IncidentSeverity,
  IncidentStatus,
  IncidentSummary,
  PagedResponse,
} from '../types';

export interface IncidentFilterParams {
  q?: string;
  status?: IncidentStatus | '';
  priority?: IncidentPriority | '';
  severity?: IncidentSeverity | '';
  category?: string;
  serviceId?: string;
  tag?: string;
  page?: number;
  size?: number;
}

export interface CreateIncidentPayload {
  title: string;
  description: string;
  severity: IncidentSeverity;
  priority?: IncidentPriority;
  category?: string;
  serviceId: string;
  tags?: string[];
  startedAt?: string;
  logSnippet?: string;
}

export interface UpdateIncidentPayload {
  title?: string;
  description?: string;
  severity?: IncidentSeverity;
  category?: string;
  tags?: string[];
  version: number;
}

export async function getIncidentsApi(params: IncidentFilterParams = {}): Promise<PagedResponse<IncidentSummary>> {
  const query = new URLSearchParams();
  if (params.q) query.append('q', params.q);
  if (params.status) query.append('status', params.status);
  if (params.priority) query.append('priority', params.priority);
  if (params.severity) query.append('severity', params.severity);
  if (params.category) query.append('category', params.category);
  if (params.serviceId) query.append('serviceId', params.serviceId);
  if (params.tag) query.append('tag', params.tag);
  if (params.page !== undefined) query.append('page', params.page.toString());
  if (params.size !== undefined) query.append('size', params.size.toString());

  const qs = query.toString();
  return apiFetch<PagedResponse<IncidentSummary>>(`/api/incidents${qs ? `?${qs}` : ''}`);
}

export async function getIncidentApi(id: string): Promise<IncidentDetail> {
  return apiFetch<IncidentDetail>(`/api/incidents/${id}`);
}

export async function createIncidentApi(payload: CreateIncidentPayload): Promise<IncidentDetail> {
  return apiFetch<IncidentDetail>('/api/incidents', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export async function updateIncidentApi(id: string, payload: UpdateIncidentPayload): Promise<IncidentDetail> {
  return apiFetch<IncidentDetail>(`/api/incidents/${id}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  });
}

export async function assignIncidentApi(id: string, assigneeId: string, version: number): Promise<IncidentDetail> {
  return apiFetch<IncidentDetail>(`/api/incidents/${id}/assign`, {
    method: 'POST',
    body: JSON.stringify({ assigneeId, version }),
  });
}

export async function escalateIncidentApi(id: string, reason: string | undefined, version: number): Promise<IncidentDetail> {
  return apiFetch<IncidentDetail>(`/api/incidents/${id}/escalate`, {
    method: 'POST',
    body: JSON.stringify({ reason, version }),
  });
}

export async function transitionStatusApi(
  id: string,
  status: IncidentStatus,
  rootCause: string | undefined,
  resolution: string | undefined,
  version: number
): Promise<IncidentDetail> {
  return apiFetch<IncidentDetail>(`/api/incidents/${id}/status`, {
    method: 'POST',
    body: JSON.stringify({ status, rootCause, resolution, version }),
  });
}

export async function addCommentApi(id: string, body: string): Promise<Comment> {
  return apiFetch<Comment>(`/api/incidents/${id}/comments`, {
    method: 'POST',
    body: JSON.stringify({ body }),
  });
}

export async function getCommentsApi(id: string): Promise<Comment[]> {
  return apiFetch<Comment[]>(`/api/incidents/${id}/comments`);
}

export async function addLogSnippetApi(id: string, snippet: string): Promise<IncidentEvent> {
  return apiFetch<IncidentEvent>(`/api/incidents/${id}/logs`, {
    method: 'POST',
    body: JSON.stringify({ snippet }),
  });
}

export async function getTimelineApi(id: string): Promise<IncidentEvent[]> {
  return apiFetch<IncidentEvent[]>(`/api/incidents/${id}/timeline`);
}
