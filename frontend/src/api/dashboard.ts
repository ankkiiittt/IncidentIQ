// src/api/dashboard.ts
import { apiFetch } from './client';
import { DashboardData } from '../types';

export async function getDashboardApi(): Promise<DashboardData> {
  return apiFetch<DashboardData>('/api/dashboard');
}
