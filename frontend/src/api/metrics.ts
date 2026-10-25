// frontend/src/api/metrics.ts
import { apiFetch } from './client';

export type MetricType =
  | 'CPU_PERCENT'
  | 'MEMORY_PERCENT'
  | 'DISK_PERCENT'
  | 'LATENCY_P95_MS'
  | 'THROUGHPUT_RPS'
  | 'ERROR_RATE_PERCENT';

export type PredictionRiskLevel = 'LOW' | 'MEDIUM' | 'HIGH';

export interface MetricDataPoint {
  timestamp: string;
  value: number;
}

export interface MetricSeries {
  metricName: MetricType;
  displayName: string;
  unit: string;
  current: number | null;
  min: number | null;
  max: number | null;
  avg: number | null;
  points: MetricDataPoint[];
}

export interface Prediction {
  id: string;
  serviceId: string;
  serviceName: string;
  anomalyScore: number;
  riskLevel: PredictionRiskLevel;
  failureProbability: number | null;
  horizonMinutes: number;
  source: 'STATISTICAL' | 'ML';
  modelVersion: string;
  contributingMetrics: Record<string, any>;
  status: 'ACTIVE' | 'RESOLVED' | 'DISMISSED';
  incidentId: string | null;
  createdAt: string;
}

export interface ServiceHealthSummary {
  serviceId: string;
  serviceName: string;
  environment: string;
  ownerTeam: string;
  criticality: string;
  healthStatus: 'HEALTHY' | 'DEGRADED' | 'CRITICAL';
  riskLevel: PredictionRiskLevel;
  openIncidentCount: number;
  activePredictionCount: number;
  latestMetrics: Record<string, number>;
  lastMetricRecordedAt: string | null;
}

export interface ServiceDeploymentItem {
  id: string;
  version: string;
  deployedAt: string;
  deployedBy: string | null;
  notes: string | null;
}

export interface ServiceIncidentItem {
  id: string;
  incidentNumber: number;
  title: string;
  status: string;
  priority: string;
  severity: string;
  createdAt: string;
}

export interface ServiceHealthDetail {
  serviceId: string;
  serviceName: string;
  environment: string;
  ownerTeam: string;
  criticality: string;
  healthStatus: 'HEALTHY' | 'DEGRADED' | 'CRITICAL';
  riskLevel: PredictionRiskLevel;
  metricSeries: MetricSeries[];
  activePredictions: Prediction[];
  recentDeployments: ServiceDeploymentItem[];
  openIncidents: ServiceIncidentItem[];
}

export interface MetricBatchResponse {
  totalSubmitted: number;
  inserted: number;
  duplicatesSkipped: number;
  anomaliesDetected: number;
}

export async function getServicesHealthApi(): Promise<ServiceHealthSummary[]> {
  return apiFetch<ServiceHealthSummary[]>('/api/services/health');
}

export async function getServiceHealthApi(serviceId: string): Promise<ServiceHealthDetail> {
  return apiFetch<ServiceHealthDetail>(`/api/services/${serviceId}/health`);
}

export async function getActivePredictionsApi(
  serviceId?: string,
  riskLevel?: PredictionRiskLevel
): Promise<Prediction[]> {
  const params = new URLSearchParams();
  if (serviceId) params.append('serviceId', serviceId);
  if (riskLevel) params.append('riskLevel', riskLevel);
  const q = params.toString() ? `?${params.toString()}` : '';
  return apiFetch<Prediction[]>(`/api/alerts/predictions${q}`);
}

export async function createIncidentFromPredictionApi(
  predictionId: string,
  data?: {
    title?: string;
    description?: string;
    priority?: string;
    severity?: string;
  }
): Promise<any> {
  return apiFetch<any>(`/api/alerts/predictions/${predictionId}/create-incident`, {
    method: 'POST',
    body: data ? JSON.stringify(data) : undefined,
  });
}

export async function dismissPredictionApi(predictionId: string): Promise<Prediction> {
  return apiFetch<Prediction>(`/api/alerts/predictions/${predictionId}/dismiss`, {
    method: 'POST',
  });
}

export async function ingestMetricsBatchApi(metrics: any[]): Promise<MetricBatchResponse> {
  return apiFetch<MetricBatchResponse>('/api/metrics/batch', {
    method: 'POST',
    body: JSON.stringify({ metrics }),
  });
}
