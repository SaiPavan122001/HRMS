/**
 * HRMS API client - replaces the Supabase integration.
 * Handles JWT auth headers, automatic refresh-token rotation and error normalization.
 */

const BASE = import.meta.env.VITE_API_BASE_URL || ""; // same-origin (vite proxy / nginx)

const ACCESS_KEY = "hrms_access_token";
const REFRESH_KEY = "hrms_refresh_token";

export interface MeResponse {
  user: { id: string; email: string; firstName: string; lastName: string; emailVerified: boolean; roles: string[] };
  profile: ProfileDto | null;
  role: string;
  permissions: { module: string; action: string; description?: string }[];
}

export interface ProfileDto {
  id: string; userId: string; email: string; firstName: string; lastName: string;
  employeeId: string | null; department: string | null; jobTitle: string | null; phone: string | null;
  managerId: string | null; hireDate: string | null; active: boolean;
}

export interface AuthResponse {
  accessToken: string; refreshToken: string; tokenType: string; expiresIn: number;
  role: string; user: MeResponse["user"]; profile: ProfileDto | null;
  permissions: { module: string; action: string }[];
}

export class ApiError extends Error {
  status: number;
  code: string;
  constructor(status: number, code: string, message: string) {
    super(message);
    this.status = status;
    this.code = code;
  }
}

export const tokens = {
  get access() { return localStorage.getItem(ACCESS_KEY); },
  get refresh() { return localStorage.getItem(REFRESH_KEY); },
  set(access: string, refresh: string) {
    localStorage.setItem(ACCESS_KEY, access);
    localStorage.setItem(REFRESH_KEY, refresh);
  },
  clear() {
    localStorage.removeItem(ACCESS_KEY);
    localStorage.removeItem(REFRESH_KEY);
  },
};

let refreshing: Promise<boolean> | null = null;

async function tryRefresh(): Promise<boolean> {
  if (!tokens.refresh) return false;
  refreshing ??= (async () => {
    try {
      const res = await fetch(`${BASE}/api/v1/auth/refresh`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ refreshToken: tokens.refresh }),
      });
      if (!res.ok) { tokens.clear(); return false; }
      const data = (await res.json()) as AuthResponse;
      tokens.set(data.accessToken, data.refreshToken);
      return true;
    } catch {
      tokens.clear();
      return false;
    } finally {
      setTimeout(() => (refreshing = null), 0);
    }
  })();
  return refreshing;
}

export async function api<T = any>(
  path: string,
  options: { method?: string; body?: unknown; retry?: boolean } = {}
): Promise<T> {
  const { method = "GET", body, retry = true } = options;
  const headers: Record<string, string> = {};
  if (body !== undefined) headers["Content-Type"] = "application/json";
  const access = tokens.access;
  if (access) headers["Authorization"] = `Bearer ${access}`;

  const res = await fetch(`${BASE}${path}`, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
  });

  if (res.status === 401 && retry && tokens.refresh) {
    const ok = await tryRefresh();
    if (ok) return api<T>(path, { ...options, retry: false });
  }

  if (res.status === 204) return undefined as T;

  let data: any = null;
  const text = await res.text();
  if (text) {
    try { data = JSON.parse(text); } catch { data = { message: text }; }
  }

  if (!res.ok) {
    throw new ApiError(res.status, data?.code || "REQUEST_FAILED", data?.message || `Request failed (${res.status})`);
  }
  return data as T;
}

// ---------- Notifications over WebSocket ----------
export function connectNotifications(onNotification: (n: any) => void): () => void {
  let ws: WebSocket | null = null;
  let closed = false;
  let retryTimer: ReturnType<typeof setTimeout> | null = null;

  const connect = () => {
    if (closed || !tokens.access) return;
    const proto = window.location.protocol === "https:" ? "wss" : "ws";
    ws = new WebSocket(`${proto}://${window.location.host}/ws/notifications?token=${tokens.access}`);
    ws.onmessage = (ev) => {
      try { onNotification(JSON.parse(ev.data)); } catch { /* ignore */ }
    };
    ws.onclose = () => {
      if (!closed) retryTimer = setTimeout(connect, 5000);
    };
    ws.onerror = () => ws?.close();
  };
  connect();
  return () => {
    closed = true;
    if (retryTimer) clearTimeout(retryTimer);
    ws?.close();
  };
}
