import { apiFetch } from './client';

export type Priority = 'P1' | 'P2' | 'P3' | 'P4';
export type IncidentCategory = 'INFRASTRUCTURE' | 'APPLICATION' | 'DATABASE' | 'SECURITY' | 'THIRD_PARTY' | 'NETWORK';

export interface AnalyticsOverview {
  days: number;
  mttrMinutes: number;
  mttdMinutes: number;
  slaComplianceRate: number;
  totalCreated: number;
  totalResolved: number;
  activeCount: number;
  priorityBreakdown: Record<Priority, number>;
}

export interface VolumeTrendPoint {
  timestamp: string;
  p1: number;
  p2: number;
  p3: number;
  p4: number;
  total: number;
}

export interface AnalyticsVolume {
  days: number;
  groupBy: 'day' | 'week';
  series: VolumeTrendPoint[];
}

export interface PrioritySlaBreakdown {
  priority: Priority;
  targetMinutes: number;
  metCount: number;
  breachedCount: number;
  complianceRate: number;
}

export interface AnalyticsSla {
  days: number;
  overallComplianceRate: number;
  byPriority: PrioritySlaBreakdown[];
}

export interface CategoryDistributionItem {
  category: IncidentCategory;
  count: number;
  percentage: number;
}

export interface RecurringCauseItem {
  serviceId: string;
  serviceName: string;
  category: IncidentCategory;
  incidentCount: number;
  lastIncidentNumber: number;
  lastOccurredAt: string;
  isRecurring: boolean;
}

export interface AnalyticsCauses {
  days: number;
  categoryDistribution: CategoryDistributionItem[];
  recurringCauses: RecurringCauseItem[];
}

export interface AiFeedbackMetrics {
  days: number;
  totalAnalyses: number;
  helpfulCount: number;
  notHelpfulCount: number;
  helpfulRate: number;
  acceptedCount: number;
  rejectedCount: number;
  acceptanceRate: number;
  avgConfidence: number;
  avgLatencyMs: number;
}

export async function getAnalyticsOverview(days: number = 30): Promise<AnalyticsOverview> {
  return apiFetch<AnalyticsOverview>(`/api/analytics/overview?days=${days}`);
}

export async function getAnalyticsVolume(days: number = 30, groupBy: 'day' | 'week' = 'day'): Promise<AnalyticsVolume> {
  return apiFetch<AnalyticsVolume>(`/api/analytics/volume?days=${days}&groupBy=${groupBy}`);
}

export async function getAnalyticsSla(days: number = 30): Promise<AnalyticsSla> {
  return apiFetch<AnalyticsSla>(`/api/analytics/sla?days=${days}`);
}

export async function getAnalyticsCauses(days: number = 30): Promise<AnalyticsCauses> {
  return apiFetch<AnalyticsCauses>(`/api/analytics/causes?days=${days}`);
}

export async function getAnalyticsAiFeedback(days: number = 30): Promise<AiFeedbackMetrics> {
  return apiFetch<AiFeedbackMetrics>(`/api/analytics/ai-feedback?days=${days}`);
}
