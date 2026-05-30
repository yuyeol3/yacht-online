import { API_BASE } from "./api.js";

const TOKEN_KEY = "yacht.accessToken";
const PROFILE_KEY = "yacht.profile";
let refreshPromise = null;
let accessTokenMemory = null;

export function getAccessToken() {
  return accessTokenMemory;
}

export function setAccessToken(token) {
  if (!token) return;
  accessTokenMemory = token;
  localStorage.removeItem(TOKEN_KEY);
  const parsed = parseJwt(token);
  if (parsed) {
    const profile = {
      userId: parsed.userId ?? parsed.id ?? parsed.sub ?? null,
      nickname: parsed.nickname ?? null,
      loginId: parsed.loginId ?? null
    };
    mergeProfile(profile);
  }
}

export function clearAuth() {
  accessTokenMemory = null;
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(PROFILE_KEY);
}

export function getProfile() {
  const raw = localStorage.getItem(PROFILE_KEY);
  if (!raw) return null;
  try {
    return JSON.parse(raw);
  } catch (err) {
    return null;
  }
}

export function setProfile(profile) {
  if (!profile) return;
  localStorage.setItem(PROFILE_KEY, JSON.stringify(profile));
}

export function mergeProfile(patch) {
  const current = getProfile() ?? {};
  setProfile({ ...current, ...patch });
}

export function parseJwt(token) {
  try {
    const payload = token.split(".")[1];
    if (!payload) return null;
    const normalized = payload.replace(/-/g, "+").replace(/_/g, "/");
    const padded = normalized.padEnd(normalized.length + ((4 - (normalized.length % 4)) % 4), "=");
    const decoded = atob(padded);
    return JSON.parse(decoded);
  } catch (err) {
    return null;
  }
}

export function extractAccessToken(payload) {
  if (!payload) return null;
  if (typeof payload === "string") return payload;
  if (typeof payload === "object" && Object.prototype.hasOwnProperty.call(payload, "data")) {
    return extractAccessToken(payload.data);
  }
  if (typeof payload === "object") {
    return payload.accessToken ?? payload.token ?? payload.access_token ?? null;
  }
  return null;
}

export function applyAuthResponse(payload, { loginId = null } = {}) {
  const accessToken = extractAccessToken(payload);
  if (accessToken) {
    setAccessToken(accessToken);
  }

  if (loginId) {
    mergeProfile({ loginId });
  }

  return { accessToken };
}

export async function refreshAccessToken() {
  if (refreshPromise) {
    return await refreshPromise;
  }

  refreshPromise = (async () => {
    const candidates = [];
    candidates.push("/auth/refresh");
    if (API_BASE) {
      candidates.push(`${API_BASE}/auth/refresh`);
    }

    try {
      for (const url of Array.from(new Set(candidates))) {
        const res = await fetch(url, {
          method: "POST",
          credentials: "include"
        });
        if (!res.ok) continue;
        const body = await res.json();
        const { accessToken } = applyAuthResponse(body);
        if (accessToken) return accessToken;
      }
      return null;
    } catch (err) {
      return null;
    } finally {
      refreshPromise = null;
    }
  })();

  return await refreshPromise;
}

export function isTokenExpired(token, skewSeconds = 10) {
  const parsed = parseJwt(token);
  const exp = Number(parsed?.exp);
  if (!Number.isFinite(exp)) return true;
  const now = Math.floor(Date.now() / 1000);
  return exp <= now + skewSeconds;
}

export async function getSocketAccessToken() {
  const token = getAccessToken();
  if (!token) {
    return await refreshAccessToken();
  }
  if (isTokenExpired(token)) {
    const refreshed = await refreshAccessToken();
    return refreshed ?? null;
  }
  return token;
}
