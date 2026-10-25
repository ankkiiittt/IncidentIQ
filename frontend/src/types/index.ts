// src/types/index.ts
// IncidentIQ Frontend Domain Types

export type UserRole = 'ADMIN' | 'ENGINEER' | 'SUPPORT' | 'VIEWER';

export interface User {
  id: string;
  name: string;
  email: string;
  role: UserRole;
  enabled: boolean;
  mustChangePassword?: boolean;
  createdAt?: string;
}

export type ServiceEnvironment = 'PROD' | 'STAGING' | 'DEV';
export type ServiceCriticality = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';

export interface Service {
  id: string;
  name: string;
  environment: ServiceEnvironment;
  ownerTeam: string;
  criticality: ServiceCriticality;
  description?: string;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface Deployment {
  id: string;
  serviceId: string;
  serviceName: string;
  version: string;
  deployedAt: string;
  deployedBy?: string;
  notes?: string;
  createdAt: string;
}

export type IncidentSeverity = 'CRITICAL' | 'HIGH' | 'MEDIUM' | 'LOW';
export type IncidentPriority = 'P1' | 'P2' | 'P3' | 'P4';
export type IncidentStatus = 'OPEN' | 'ACKNOWLEDGED' | 'IN_PROGRESS' | 'RESOLVED' | 'CLOSED';
export type IncidentCategory =
  | 'DATABASE'
  | 'NETWORK'
  | 'APPLICATION'
  | 'INFRASTRUCTURE'
  | 'DEPLOYMENT'
  | 'SECURITY'
  | 'THIRD_PARTY'
  | 'PERFORMANCE'
  | 'OTHER';

export interface IncidentSummary {
  id: string;
  number: number;
  formattedNumber: string;
  title: string;
  severity: IncidentSeverity;
  priority: IncidentPriority;
  status: IncidentStatus;
  category?: IncidentCategory;
  serviceId?: string;
  serviceName?: string;
  reporterId?: string;
  reporterName?: string;
  assigneeId?: string;
  assigneeName?: string;
  tags: string[];
  startedAt?: string;
  slaBreached: boolean;
  slaResponseDueAt?: string;
  slaResolveDueAt?: string;
  version: number;
  createdAt: string;
  updatedAt: string;
}

export interface IncidentDetail extends IncidentSummary {
  description: string;
  acknowledgedAt?: string;
  resolvedAt?: string;
  closedAt?: string;
  escalatedAt?: string;
  escalationCount: number;
  rootCause?: string;
  resolution?: string;
  allowedNextStatuses: IncidentStatus[];
}

export type IncidentEventType =
  | 'CREATED'
  | 'STATUS_CHANGED'
  | 'ASSIGNED'
  | 'ESCALATED'
  | 'PRIORITY_CHANGED'
  | 'COMMENT_ADDED'
  | 'LOG_ADDED'
  | 'DEPLOYMENT_LINKED'
  | 'AI_ANALYSIS_COMPLETED'
  | 'AI_ANALYSIS_FAILED'
  | 'PREDICTION_LINKED';

export interface IncidentEvent {
  id: string;
  incidentId: string;
  eventType: IncidentEventType;
  actorId?: string;
  actorName?: string;
  payload: Record<string, unknown>;
  createdAt: string;
}

export interface Comment {
  id: string;
  incidentId: string;
  userId: string;
  userName: string;
  body: string;
  createdAt: string;
  editedAt?: string;
}

export interface PagedResponse<T> {
  content?: T[];
  page?: {
    size: number;
    totalElements: number;
    totalPages: number;
    number: number;
  };
  // Fallbacks if backend serialization varies
  totalElements?: number;
  totalPages?: number;
  size?: number;
  number?: number;
}

export interface DashboardKpis {
  openP1: number;
  openP2: number;
  openP3: number;
  openP4: number;
  totalOpen: number;
  unassigned: number;
  slaBreached: number;
  slaAtRisk: number;
  resolvedLast7Days: number;
  mttrMinutes: number;
}

export interface DailyTrendPoint {
  date: string;
  createdCount: number;
  resolvedCount: number;
}

export interface TopAffectedService {
  serviceId: string;
  serviceName: string;
  serviceTier: string;
  activeIncidents: number;
  totalIncidents: number;
}

export interface ServiceHealthSummary {
  totalServices: number;
  healthyServices: number;
  degradedServices: number;
}

export interface DashboardData {
  kpis: DashboardKpis;
  trend30Days: DailyTrendPoint[];
  topAffectedServices: TopAffectedService[];
  recentIncidents: IncidentSummary[];
  serviceHealth: ServiceHealthSummary;
}
