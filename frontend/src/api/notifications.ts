import { apiFetch } from './client';

export type NotificationType =
  | 'INCIDENT_ASSIGNED'
  | 'INCIDENT_ESCALATED'
  | 'INCIDENT_STATUS_CHANGED'
  | 'PREDICTION_ALERT'
  | 'AI_ANALYSIS_COMPLETED'
  | 'SYSTEM';

export interface InAppNotification {
  id: string;
  userId: string;
  title: string;
  message: string;
  type: NotificationType;
  entityType?: string;
  entityId?: string;
  read: boolean;
  createdAt: string;
}

export interface UnreadCountResponse {
  unreadCount: number;
}

export async function getRecentNotifications(): Promise<InAppNotification[]> {
  return apiFetch<InAppNotification[]>('/api/notifications');
}

export async function getUnreadNotificationCount(): Promise<number> {
  const res = await apiFetch<UnreadCountResponse>('/api/notifications/unread-count');
  return res.unreadCount;
}

export async function markNotificationAsRead(id: string): Promise<InAppNotification> {
  return apiFetch<InAppNotification>(`/api/notifications/${id}/read`, {
    method: 'PATCH',
  });
}

export async function markAllNotificationsAsRead(): Promise<{ updated: number; message: string }> {
  return apiFetch<{ updated: number; message: string }>('/api/notifications/read-all', {
    method: 'POST',
  });
}
