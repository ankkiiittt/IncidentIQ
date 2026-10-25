// src/context/WebSocketContext.tsx
import React, { createContext, useContext, useEffect, useRef, useState, useCallback } from 'react';
import { Client, IMessage, StompSubscription } from '@stomp/stompjs';
import { useQueryClient } from '@tanstack/react-query';
import { useAuth } from './AuthContext';
import { getAccessToken } from '../api/client';

export type WsConnectionStatus = 'CONNECTED' | 'CONNECTING' | 'DISCONNECTED';

export interface LiveAlert {
  id: string;
  title: string;
  message: string;
  type: string;
  timestamp: Date;
}

interface WebSocketContextType {
  status: WsConnectionStatus;
  isConnected: boolean;
  reconnect: () => void;
  subscribeToIncident: (incidentId: string, onEvent?: (event: unknown) => void) => () => void;
  recentLiveAlerts: LiveAlert[];
  dismissAlert: (id: string) => void;
}

const WebSocketContext = createContext<WebSocketContextType | undefined>(undefined);

export const WebSocketProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const { user, isAuthenticated } = useAuth();
  const queryClient = useQueryClient();

  const [status, setStatus] = useState<WsConnectionStatus>('DISCONNECTED');
  const [recentLiveAlerts, setRecentLiveAlerts] = useState<LiveAlert[]>([]);

  const clientRef = useRef<Client | null>(null);
  const backoffDelayRef = useRef<number>(2000);
  const reconnectTimerRef = useRef<number | null>(null);
  const pollingTimerRef = useRef<number | null>(null);

  const incidentSubscriptionsRef = useRef<Map<string, { sub: StompSubscription; listeners: Set<(data: unknown) => void> }>>(new Map());

  const addLiveAlert = useCallback((title: string, message: string, type: string = 'INFO') => {
    const alert: LiveAlert = {
      id: Math.random().toString(36).substring(2, 9),
      title,
      message,
      type,
      timestamp: new Date(),
    };
    setRecentLiveAlerts((prev) => [alert, ...prev.slice(0, 4)]);

    // Auto-dismiss after 7 seconds
    setTimeout(() => {
      setRecentLiveAlerts((prev) => prev.filter((a) => a.id !== alert.id));
    }, 7000);
  }, []);

  const dismissAlert = useCallback((id: string) => {
    setRecentLiveAlerts((prev) => prev.filter((a) => a.id !== id));
  }, []);

  const getBrokerUrl = (): string => {
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    return `${protocol}//${window.location.host}/ws`;
  };

  const connect = useCallback(() => {
    if (!isAuthenticated) return;

    const token = getAccessToken();
    if (!token) return;

    if (clientRef.current?.active) {
      return;
    }

    setStatus('CONNECTING');

    const client = new Client({
      brokerURL: getBrokerUrl(),
      connectHeaders: {
        Authorization: `Bearer ${token}`,
      },
      reconnectDelay: 0, // We control exponential backoff manually
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      onConnect: () => {
        setStatus('CONNECTED');
        backoffDelayRef.current = 2000; // Reset backoff

        // Subscribe to global incident broadcasts
        client.subscribe('/topic/incidents', (message: IMessage) => {
          try {
            const body = JSON.parse(message.body);
            queryClient.invalidateQueries({ queryKey: ['incidents'] });
            queryClient.invalidateQueries({ queryKey: ['dashboard'] });
            queryClient.invalidateQueries({ queryKey: ['services'] });
            queryClient.invalidateQueries({ queryKey: ['analytics'] });

            if (body.action === 'CREATED') {
              addLiveAlert('New Incident Created', `${body.data?.formattedNumber || 'Incident'}: ${body.data?.title || ''}`, 'INCIDENT');
            } else if (body.action === 'ESCALATED') {
              addLiveAlert('Incident Escalated', `${body.data?.formattedNumber || 'Incident'} escalated to ${body.data?.priority}`, 'WARNING');
            }
          } catch {
            queryClient.invalidateQueries({ queryKey: ['incidents'] });
          }
        });

        // Subscribe to failure predictions
        client.subscribe('/topic/predictions', (message: IMessage) => {
          try {
            const body = JSON.parse(message.body);
            queryClient.invalidateQueries({ queryKey: ['predictions'] });
            queryClient.invalidateQueries({ queryKey: ['services-health'] });
            queryClient.invalidateQueries({ queryKey: ['dashboard'] });

            const pred = body.data;
            if (pred) {
              addLiveAlert(
                'Predictive Anomaly Detected',
                `${pred.serviceName || 'Service'}: ${pred.riskLevel} risk (${Math.round((pred.anomalyScore || 0) * 100)}% anomaly score)`,
                'CRITICAL'
              );
            }
          } catch {
            queryClient.invalidateQueries({ queryKey: ['predictions'] });
          }
        });

        // Subscribe to personal targeted user alerts
        if (user?.id) {
          const handlePersonalNotification = (message: IMessage) => {
            try {
              const body = JSON.parse(message.body);
              queryClient.invalidateQueries({ queryKey: ['notifications'] });
              queryClient.invalidateQueries({ queryKey: ['notifications-unread-count'] });

              const notif = body.data;
              if (notif) {
                addLiveAlert(notif.title, notif.message, notif.type || 'INFO');
              }
            } catch {
              queryClient.invalidateQueries({ queryKey: ['notifications'] });
            }
          };

          client.subscribe(`/topic/notifications/${user.id}`, handlePersonalNotification);
          client.subscribe(`/user/queue/notifications`, handlePersonalNotification);
        }

        // Re-subscribe to any active incident rooms
        incidentSubscriptionsRef.current.forEach((val, incidentId) => {
          val.sub = client.subscribe(`/topic/incidents/${incidentId}`, (msg: IMessage) => {
            try {
              const data = JSON.parse(msg.body);
              val.listeners.forEach((cb) => cb(data));
            } catch {
              // Ignore
            }
          });
        });
      },
      onStompError: (frame) => {
        console.warn('STOMP error:', frame.headers['message']);
        setStatus('DISCONNECTED');
        scheduleReconnect();
      },
      onWebSocketClose: () => {
        setStatus('DISCONNECTED');
        scheduleReconnect();
      },
    });

    clientRef.current = client;
    client.activate();
  }, [isAuthenticated, user?.id, queryClient, addLiveAlert]);

  const scheduleReconnect = useCallback(() => {
    if (reconnectTimerRef.current) {
      clearTimeout(reconnectTimerRef.current);
    }
    const delay = backoffDelayRef.current;
    // Exponential backoff capped at 30 seconds
    backoffDelayRef.current = Math.min(delay * 1.5, 30000);

    reconnectTimerRef.current = window.setTimeout(() => {
      connect();
    }, delay);
  }, [connect]);

  const reconnect = useCallback(() => {
    backoffDelayRef.current = 2000;
    if (clientRef.current?.active) {
      clientRef.current.deactivate();
    }
    connect();
  }, [connect]);

  // Handle active session changes
  useEffect(() => {
    if (isAuthenticated) {
      connect();
    } else {
      if (clientRef.current?.active) {
        clientRef.current.deactivate();
      }
      setStatus('DISCONNECTED');
    }

    return () => {
      if (reconnectTimerRef.current) clearTimeout(reconnectTimerRef.current);
      if (clientRef.current?.active) {
        clientRef.current.deactivate();
      }
    };
  }, [isAuthenticated, connect]);

  // Fallback Polling every 30 seconds when disconnected
  useEffect(() => {
    if (status === 'DISCONNECTED' && isAuthenticated) {
      pollingTimerRef.current = window.setInterval(() => {
        queryClient.invalidateQueries({ queryKey: ['incidents'] });
        queryClient.invalidateQueries({ queryKey: ['predictions'] });
        queryClient.invalidateQueries({ queryKey: ['notifications-unread-count'] });
        queryClient.invalidateQueries({ queryKey: ['dashboard'] });
      }, 30000);
    } else {
      if (pollingTimerRef.current) {
        clearInterval(pollingTimerRef.current);
        pollingTimerRef.current = null;
      }
    }

    return () => {
      if (pollingTimerRef.current) clearInterval(pollingTimerRef.current);
    };
  }, [status, isAuthenticated, queryClient]);

  // Subscribe to specific incident room
  const subscribeToIncident = useCallback(
    (incidentId: string, onEvent?: (event: unknown) => void) => {
      let entry = incidentSubscriptionsRef.current.get(incidentId);

      if (!entry) {
        const listeners = new Set<(data: unknown) => void>();
        if (onEvent) listeners.add(onEvent);

        let sub: StompSubscription | null = null;
        if (clientRef.current?.connected) {
          sub = clientRef.current.subscribe(`/topic/incidents/${incidentId}`, (msg: IMessage) => {
            try {
              const data = JSON.parse(msg.body);
              listeners.forEach((cb) => cb(data));
              // Invalidate relevant detail query keys
              queryClient.invalidateQueries({ queryKey: ['incident', incidentId] });
              queryClient.invalidateQueries({ queryKey: ['incident-timeline', incidentId] });
              queryClient.invalidateQueries({ queryKey: ['incident-analyses', incidentId] });
            } catch {
              // Ignore
            }
          });
        }

        entry = { sub: sub!, listeners };
        incidentSubscriptionsRef.current.set(incidentId, entry);
      } else {
        if (onEvent) entry.listeners.add(onEvent);
      }

      // Return unsubscribe cleanup callback
      return () => {
        const currentEntry = incidentSubscriptionsRef.current.get(incidentId);
        if (currentEntry) {
          if (onEvent) currentEntry.listeners.delete(onEvent);
          if (currentEntry.listeners.size === 0) {
            if (currentEntry.sub) {
              currentEntry.sub.unsubscribe();
            }
            incidentSubscriptionsRef.current.delete(incidentId);
          }
        }
      };
    },
    [queryClient]
  );

  return (
    <WebSocketContext.Provider
      value={{
        status,
        isConnected: status === 'CONNECTED',
        reconnect,
        subscribeToIncident,
        recentLiveAlerts,
        dismissAlert,
      }}
    >
      {children}
    </WebSocketContext.Provider>
  );
};

export const useWebSocket = (): WebSocketContextType => {
  const context = useContext(WebSocketContext);
  if (!context) {
    throw new Error('useWebSocket must be used within a WebSocketProvider');
  }
  return context;
};
