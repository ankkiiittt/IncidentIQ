// src/api/auth.ts
import { apiFetch, setAccessToken } from './client';
import { User } from '../types';

export interface LoginResponse {
  accessToken: string;
  user: User;
}

export async function loginApi(email: string, password: string): Promise<LoginResponse> {
  const data = await apiFetch<LoginResponse>('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify({ email, password }),
  });
  setAccessToken(data.accessToken);
  return data;
}

export async function refreshApi(): Promise<{ accessToken: string }> {
  const data = await apiFetch<{ accessToken: string }>('/api/auth/refresh', {
    method: 'POST',
  });
  setAccessToken(data.accessToken);
  return data;
}

export async function logoutApi(): Promise<void> {
  try {
    await apiFetch('/api/auth/logout', { method: 'POST' });
  } finally {
    setAccessToken(null);
  }
}

export async function changePasswordApi(currentPassword: string, newPassword: string): Promise<void> {
  await apiFetch('/api/auth/change-password', {
    method: 'POST',
    body: JSON.stringify({ currentPassword, newPassword }),
  });
}

export async function getCurrentUserApi(): Promise<User> {
  return apiFetch<User>('/api/auth/me');
}
