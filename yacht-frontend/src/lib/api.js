import { refreshAccessToken, getAccessToken, clearAuth, isTokenExpired } from "./auth.js";

export const API_BASE = import.meta.env.VITE_API_BASE_URL ?? "/api";

export class ApiError extends Error {
  constructor(message, status, code) {
    super(message);
    this.status = status;
    this.code = code;
  }
}

function normalizeHeaders(headers = {}) {
  const normalized = new Headers(headers);
  if (!normalized.has("Content-Type")) {
    normalized.set("Content-Type", "application/json");
  }
  return normalized;
}

async function parseJsonSafe(res) {
  const text = await res.text();
  if (!text) return null;
  try {
    return JSON.parse(text);
  } catch (err) {
    return null;
  }
}

function forceLogout() {
  clearAuth();
  if (typeof window !== "undefined") {
    window.location.hash = "/login";
  }
}

export async function apiFetch(path, options = {}, { auth = true } = {}) {
  const url = `${API_BASE}${path}`;
  const headers = normalizeHeaders(options.headers ?? {});

  if (auth) {
    let token = getAccessToken();
    if (token && isTokenExpired(token)) {
      token = await refreshAccessToken();
      if (!token) {
        forceLogout();
        throw new ApiError("인증이 만료되었습니다. 다시 로그인해 주세요.", 401, "AUTH_EXPIRED");
      }
    }

    if (token) {
      headers.set("Authorization", `Bearer ${token}`);
    }
  }

  const res = await fetch(url, {
    ...options,
    headers,
    credentials: "include"
  });

  if (res.status === 401 && auth) {
    const refreshed = await refreshAccessToken();
    if (refreshed) {
      headers.set("Authorization", `Bearer ${refreshed}`);
      const retry = await fetch(url, {
        ...options,
        headers,
        credentials: "include"
      });
      return handleResponse(retry);
    }

    forceLogout();
    throw new ApiError("인증이 만료되었습니다. 다시 로그인해 주세요.", 401, "AUTH_EXPIRED");
  }

  return handleResponse(res);
}

async function handleResponse(res) {
  if (res.status === 204) return null;
  const body = await parseJsonSafe(res);
  if (!res.ok) {
    const message = body?.message ?? body?.error ?? "요청이 실패했습니다.";
    throw new ApiError(message, res.status, body?.code ?? null);
  }
  if (body && Object.prototype.hasOwnProperty.call(body, "data")) {
    return body.data;
  }
  return body;
}
