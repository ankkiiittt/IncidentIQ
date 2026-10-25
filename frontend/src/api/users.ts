// src/api/users.ts
import { apiFetch } from './client';
import { PagedResponse, User, UserRole } from '../types';

export interface CreateUserPayload {
  name: string;
  email: string;
  role: UserRole;
  temporaryPassword?: string;
}

export interface UpdateUserPayload {
  role?: UserRole;
  enabled?: boolean;
}

export interface ResetPasswordResponse {
  temporaryPassword: string;
}

export async function getUsersApi(page = 0, size = 50): Promise<PagedResponse<User>> {
  return apiFetch<PagedResponse<User>>(`/api/users?page=${page}&size=${size}`);
}

export async function getUserApi(id: string): Promise<User> {
  return apiFetch<User>(`/api/users/${id}`);
}

export async function createUserApi(payload: CreateUserPayload): Promise<User> {
  return apiFetch<User>('/api/users', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export async function updateUserApi(id: string, payload: UpdateUserPayload): Promise<User> {
  return apiFetch<User>(`/api/users/${id}`, {
    method: 'PATCH',
    body: JSON.stringify(payload),
  });
}

export async function resetPasswordApi(id: string): Promise<ResetPasswordResponse> {
  return apiFetch<ResetPasswordResponse>(`/api/users/${id}/reset-password`, {
    method: 'POST',
  });
}
