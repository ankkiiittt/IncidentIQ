// src/api/services.ts
import { apiFetch } from './client';
import { Deployment, PagedResponse, Service, ServiceCriticality, ServiceEnvironment } from '../types';

export interface CreateServicePayload {
  name: string;
  environment: ServiceEnvironment;
  ownerTeam: string;
  criticality: ServiceCriticality;
  description?: string;
}

export interface UpdateServicePayload {
  name?: string;
  environment?: ServiceEnvironment;
  ownerTeam?: string;
  criticality?: ServiceCriticality;
  description?: string;
  active?: boolean;
}

export interface CreateDeploymentPayload {
  version: string;
  deployedAt?: string;
  deployedBy?: string;
  notes?: string;
}

export async function getServicesApi(activeOnly = false, page = 0, size = 50): Promise<PagedResponse<Service>> {
  return apiFetch<PagedResponse<Service>>(`/api/services?activeOnly=${activeOnly}&page=${page}&size=${size}`);
}

export async function getServiceApi(id: string): Promise<Service> {
  return apiFetch<Service>(`/api/services/${id}`);
}

export async function createServiceApi(payload: CreateServicePayload): Promise<Service> {
  return apiFetch<Service>('/api/services', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export async function updateServiceApi(id: string, payload: UpdateServicePayload): Promise<Service> {
  return apiFetch<Service>(`/api/services/${id}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  });
}

export async function deactivateServiceApi(id: string): Promise<void> {
  await apiFetch(`/api/services/${id}`, {
    method: 'DELETE',
  });
}

export async function recordDeploymentApi(serviceId: string, payload: CreateDeploymentPayload): Promise<Deployment> {
  return apiFetch<Deployment>(`/api/services/${serviceId}/deployments`, {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export async function getDeploymentsApi(serviceId: string, page = 0, size = 20): Promise<PagedResponse<Deployment>> {
  return apiFetch<PagedResponse<Deployment>>(`/api/services/${serviceId}/deployments?page=${page}&size=${size}`);
}
