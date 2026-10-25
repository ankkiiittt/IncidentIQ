// src/components/NotificationBell.tsx
import React, { useState, useRef, useEffect } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import {
  Bell,
  Check,
  CheckCheck,
  AlertTriangle,
  UserCheck,
  ArrowUpRight,
  Sparkles,
  Activity,
  Info,
} from 'lucide-react';
import {
  getRecentNotifications,
  getUnreadNotificationCount,
  markNotificationAsRead,
  markAllNotificationsAsRead,
  InAppNotification,
  NotificationType,
} from '../api/notifications';

function formatRelativeTime(dateString: string): string {
  const date = new Date(dateString);
  const now = new Date();
  const diffSec = Math.floor((now.getTime() - date.getTime()) / 1000);

  if (diffSec < 60) return 'Just now';
  if (diffSec < 3600) return `${Math.floor(diffSec / 60)}m ago`;
  if (diffSec < 86400) return `${Math.floor(diffSec / 3600)}h ago`;
  return `${Math.floor(diffSec / 86400)}d ago`;
}

function getNotificationIcon(type: NotificationType) {
  switch (type) {
    case 'INCIDENT_ASSIGNED':
      return <UserCheck className="w-4 h-4 text-blue-600 shrink-0" />;
    case 'INCIDENT_ESCALATED':
      return <ArrowUpRight className="w-4 h-4 text-red-600 shrink-0" />;
    case 'INCIDENT_STATUS_CHANGED':
      return <Activity className="w-4 h-4 text-indigo-600 shrink-0" />;
    case 'PREDICTION_ALERT':
      return <AlertTriangle className="w-4 h-4 text-amber-600 shrink-0" />;
    case 'AI_ANALYSIS_COMPLETED':
      return <Sparkles className="w-4 h-4 text-emerald-600 shrink-0" />;
    default:
      return <Info className="w-4 h-4 text-slate-500 shrink-0" />;
  }
}

export const NotificationBell: React.FC = () => {
  const [isOpen, setIsOpen] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const { data: unreadCount = 0 } = useQuery({
    queryKey: ['notifications-unread-count'],
    queryFn: getUnreadNotificationCount,
    refetchInterval: 30000,
  });

  const { data: notifications = [], isLoading } = useQuery({
    queryKey: ['notifications'],
    queryFn: getRecentNotifications,
    enabled: isOpen,
  });

  const markReadMutation = useMutation({
    mutationFn: markNotificationAsRead,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['notifications-unread-count'] });
      queryClient.invalidateQueries({ queryKey: ['notifications'] });
    },
  });

  const markAllReadMutation = useMutation({
    mutationFn: markAllNotificationsAsRead,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['notifications-unread-count'] });
      queryClient.invalidateQueries({ queryKey: ['notifications'] });
    },
  });

  // Close dropdown on outside click
  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(e.target as Node)) {
        setIsOpen(false);
      }
    };
    if (isOpen) {
      document.addEventListener('mousedown', handleClickOutside);
    }
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, [isOpen]);

  const handleNotificationClick = (notif: InAppNotification) => {
    if (!notif.read) {
      markReadMutation.mutate(notif.id);
    }
    setIsOpen(false);

    if (notif.entityType === 'INCIDENT' && notif.entityId) {
      navigate(`/incidents/${notif.entityId}`);
    } else if (notif.entityType === 'PREDICTION') {
      navigate('/services');
    }
  };

  return (
    <div className="relative" ref={dropdownRef}>
      <button
        type="button"
        onClick={() => setIsOpen((prev) => !prev)}
        className="relative p-2 rounded-lg text-slate-600 hover:text-slate-900 hover:bg-slate-100 transition-colors focus:outline-none focus:ring-2 focus:ring-primary/20"
        title="Notifications"
        aria-label={`Notifications: ${unreadCount} unread`}
      >
        <Bell className="w-5 h-5" />
        {unreadCount > 0 && (
          <span className="absolute top-1 right-1 flex items-center justify-center min-w-[18px] h-[18px] px-1 bg-red-600 text-white text-[10px] font-bold rounded-full border-2 border-surface animate-in zoom-in-50">
            {unreadCount > 99 ? '99+' : unreadCount}
          </span>
        )}
      </button>

      {isOpen && (
        <div className="absolute right-0 mt-2 w-84 sm:w-96 bg-surface border border-border rounded-xl shadow-xl z-50 overflow-hidden flex flex-col max-h-[480px] animate-in fade-in-50 zoom-in-95 duration-150">
          {/* Header */}
          <div className="p-3.5 border-b border-border flex items-center justify-between bg-slate-50/70">
            <div className="flex items-center gap-2">
              <span className="font-semibold text-sm text-text">Notifications</span>
              {unreadCount > 0 && (
                <span className="bg-primary-light text-primary text-[11px] font-bold px-2 py-0.5 rounded-full">
                  {unreadCount} new
                </span>
              )}
            </div>
            {unreadCount > 0 && (
              <button
                type="button"
                onClick={() => markAllReadMutation.mutate()}
                disabled={markAllReadMutation.isPending}
                className="inline-flex items-center gap-1 text-xs text-primary hover:text-primary-hover font-medium disabled:opacity-50"
              >
                <CheckCheck className="w-3.5 h-3.5" />
                Mark all read
              </button>
            )}
          </div>

          {/* List */}
          <div className="overflow-y-auto divide-y divide-border/60 flex-1">
            {isLoading ? (
              <div className="p-6 text-center text-xs text-muted">Loading notifications...</div>
            ) : notifications.length === 0 ? (
              <div className="p-8 text-center">
                <Check className="w-8 h-8 text-emerald-500 mx-auto mb-2 opacity-60" />
                <p className="text-xs font-semibold text-text">All caught up!</p>
                <p className="text-[11px] text-muted mt-0.5">No new alerts or incident updates.</p>
              </div>
            ) : (
              notifications.map((notif) => (
                <div
                  key={notif.id}
                  onClick={() => handleNotificationClick(notif)}
                  className={`p-3.5 flex items-start gap-3 hover:bg-slate-50 cursor-pointer transition-colors relative ${
                    !notif.read ? 'bg-indigo-50/20' : ''
                  }`}
                >
                  <div className="mt-0.5 p-1.5 rounded-lg bg-slate-100 shrink-0">
                    {getNotificationIcon(notif.type)}
                  </div>
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center justify-between gap-1">
                      <span className={`text-xs font-medium truncate ${!notif.read ? 'text-text font-semibold' : 'text-slate-700'}`}>
                        {notif.title}
                      </span>
                      <span className="text-[10px] text-muted shrink-0">
                        {formatRelativeTime(notif.createdAt)}
                      </span>
                    </div>
                    <p className="text-xs text-muted line-clamp-2 mt-0.5 leading-relaxed">
                      {notif.message}
                    </p>
                  </div>
                  {!notif.read && (
                    <span className="w-2 h-2 rounded-full bg-primary shrink-0 mt-1.5" />
                  )}
                </div>
              ))
            )}
          </div>
        </div>
      )}
    </div>
  );
};
