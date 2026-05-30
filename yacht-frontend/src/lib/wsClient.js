import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client";
import { getSocketAccessToken } from "./auth.js";

let client = null;
let connectPromise = null;
let clientWsUrl = null;

export function toSockJsUrl(wsUrl) {
  return wsUrl
    .replace(/^ws:\/\//, "http://")
    .replace(/^wss:\/\//, "https://");
}

function createClient(wsUrl) {
  const sockJsUrl = toSockJsUrl(wsUrl);
  const next = new Client({
    webSocketFactory: () => new SockJS(sockJsUrl),
    reconnectDelay: 4000
  });
  client = next;
  clientWsUrl = wsUrl;
  return next;
}

export async function connectWs({
  wsUrl,
  onWebSocketError,
  onStompError,
  onAuthFailure
} = {}) {
  if (!wsUrl) {
    throw new Error("NO_WS_URL");
  }

  if (client && clientWsUrl !== wsUrl) {
    disconnectWs();
  }

  if (client) {
    if (typeof onWebSocketError === "function") {
      client.onWebSocketError = onWebSocketError;
    }
    if (typeof onStompError === "function") {
      client.onStompError = onStompError;
    }
  }

  if (client?.connected) {
    return client;
  }
  if (connectPromise) {
    return connectPromise;
  }

  const next = client ?? createClient(wsUrl);

  next.beforeConnect = async () => {
    const token = await getSocketAccessToken();
    if (!token) {
      if (typeof onAuthFailure === "function") onAuthFailure();
      throw new Error("NO_SOCKET_TOKEN");
    }
    next.connectHeaders = { Authorization: `Bearer ${token}` };
  };

  if (typeof onWebSocketError === "function") {
    next.onWebSocketError = onWebSocketError;
  }
  if (typeof onStompError === "function") {
    next.onStompError = onStompError;
  }

  let pendingPromise;
  pendingPromise = new Promise((resolve, reject) => {
    next.onConnect = () => {
      if (connectPromise === pendingPromise) {
        connectPromise = null;
      }
      resolve(next);
    };

    next.onWebSocketClose = (evt) => {
      if (connectPromise === pendingPromise) {
        connectPromise = null;
        reject(new Error(evt?.reason || "WS_CONNECT_CLOSED"));
      }
    };
  });

  connectPromise = pendingPromise;
  next.activate();
  return connectPromise;
}

export function subscribeWs(destination, callback) {
  if (!client || !client.connected) return () => {};
  const sub = client.subscribe(destination, callback);
  return () => {
    try {
      sub.unsubscribe();
    } catch (err) {
      // ignore
    }
  };
}

export function publishWs({ destination, body }) {
  if (!client || !client.connected) return false;
  client.publish({ destination, body });
  return true;
}

export function disconnectWs() {
  if (connectPromise) {
    connectPromise = null;
  }
  if (client) {
    client.deactivate();
    client = null;
  }
  clientWsUrl = null;
}

export function isWsConnected() {
  return Boolean(client?.connected);
}
