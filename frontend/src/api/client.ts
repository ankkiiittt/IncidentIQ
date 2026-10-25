// src/api/client.ts
// Centralized fetch client with token handling, silent refresh, and RFC 7807 error parsing

let inMemoryToken: string | null = null;
let onUnauthorizedCallback: (() => void) | null = null;
let isRefreshing = false;
let refreshSubscribers: ((token: string) => void)[] = [];

export function setAccessToken(token: string | null): void {
  inMemoryToken = token;
}

export function getAccessToken(): string | null {
  return inMemoryToken;
}

export function setOnUnauthorizedCallback(cb: () => void): void {
  onUnauthorizedCallback = cb;
}

export class ApiError extends Error {
  public status: number;
  public errorCode?: string;
  public details?: Record<string, unknown>;

  constructor(status: number, message: string, errorCode?: string, details?: Record<string, unknown>) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.errorCode = errorCode;
    this.details = details;
  }
}

function onRefreshed(token: string): void {
  refreshSubscribers.forEach((cb) => cb(token));
  refreshSubscribers = [];
}

export async function apiFetch<T>(endpoint: string, options: RequestInit = {}): Promise<T> {
  const headers = new Headers(options.headers || {});

  if (!headers.has('Content-Type') && !(options.body instanceof FormData)) {
    headers.set('Content-Type', 'application/json');
  }

  if (inMemoryToken && !headers.has('Authorization')) {
    headers.set('Authorization', `Bearer ${inMemoryToken}`);
  }

  const config: RequestInit = {
    ...options,
    headers,
    credentials: 'include', // essential for httpOnly refresh cookie
  };

  const response = await fetch(endpoint, config);

  // If 401 Unauthorized and not already refreshing or calling login/refresh endpoint
  if (
    response.status === 401 &&
    !endpoint.includes('/api/auth/login') &&
    !endpoint.includes('/api/auth/refresh')
  ) {
    if (!isRefreshing) {
      isRefreshing = true;
      try {
        const refreshResponse = await fetch('/api/auth/refresh', {
          method: 'POST',
          credentials: 'include',
        });

        if (!refreshResponse.ok) {
          throw new Error('Refresh failed');
        }

        const refreshData = await refreshResponse.json();
        const newToken = refreshData.accessToken as string;
        setAccessToken(newToken);
        isRefreshing = false;
        onRefreshed(newToken);

        // Retry original request with new token
        headers.set('Authorization', `Bearer ${newToken}`);
        const retryResponse = await fetch(endpoint, { ...config, headers });
        return handleResponse<T>(retryResponse);
      } catch {
        isRefreshing = false;
        setAccessToken(null);
        if (onUnauthorizedCallback) {
          onUnauthorizedCallback();
        }
        throw new ApiError(401, 'Session expired. Please sign in again.', 'UNAUTHORIZED');
      }
    } else {
      // Queue requests while refresh is in flight
      return new Promise<T>((resolve, reject) => {
        refreshSubscribers.push(async (newToken: string) => {
          try {
            headers.set('Authorization', `Bearer ${newToken}`);
            const retryRes = await fetch(endpoint, { ...config, headers });
            const data = await handleResponse<T>(retryRes);
            resolve(data);
          } catch (e) {
            reject(e);
          }
        });
      });
    }
  }

  return handleResponse<T>(response);
}

async function handleResponse<T>(response: Response): Promise<T> {
  if (response.status === 204) {
    return {} as T;
  }

  const contentType = response.headers.get('content-type');
  const isJson = contentType && contentType.includes('application/json');

  if (!response.ok) {
    let message = `Request failed with status ${response.status}`;
    let errorCode: string | undefined;
    let details: Record<string, unknown> | undefined;

    if (isJson) {
      try {
        const problem = await response.json();
        message = problem.detail || problem.message || message;
        errorCode = problem.errorCode;
        details = problem.details || problem.errors;
      } catch {
        // fallback to default text
      }
    } else {
      const text = await response.text();
      if (text) message = text;
    }

    throw new ApiError(response.status, message, errorCode, details);
  }

  if (isJson) {
    return response.json() as Promise<T>;
  }

  return response.text() as unknown as Promise<T>;
}
