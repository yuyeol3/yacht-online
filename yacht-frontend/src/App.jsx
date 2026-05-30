import { useEffect, useState } from "react";
import { HashRouter, Routes, Route, Navigate, useLocation } from "react-router-dom";
import LoginPage from "./components/LoginPage.jsx";
import SignupPage from "./components/SignupPage.jsx";
import LobbyPage from "./components/LobbyPage.jsx";
import RoomPage from "./components/RoomPage.jsx";
import GamePage from "./components/GamePage.jsx";
import { clearAuth, getAccessToken, isTokenExpired, refreshAccessToken } from "./lib/auth.js";
import { disconnectWs } from "./lib/wsClient.js";

function isRealtimePath(pathname) {
  return pathname.startsWith("/rooms/") || pathname.startsWith("/game/");
}

function SocketLifecycle() {
  const location = useLocation();

  useEffect(() => {
    if (!isRealtimePath(location.pathname)) {
      disconnectWs();
    }
  }, [location.pathname]);

  return null;
}

function RequireAuth({ children }) {
  const [ready, setReady] = useState(false);
  const [authed, setAuthed] = useState(false);

  useEffect(() => {
    let active = true;

    const check = async () => {
      const token = getAccessToken();
      if (!token) {
        const refreshed = await refreshAccessToken();
        if (!active) return;
        if (active) {
          if (refreshed) {
            setAuthed(true);
          } else {
            clearAuth();
            setAuthed(false);
          }
          setReady(true);
        }
        return;
      }

      if (!isTokenExpired(token)) {
        if (active) {
          setAuthed(true);
          setReady(true);
        }
        return;
      }

      const refreshed = await refreshAccessToken();
      if (!active) return;

      if (refreshed) {
        setAuthed(true);
      } else {
        clearAuth();
        setAuthed(false);
      }
      setReady(true);
    };

    check();
    return () => {
      active = false;
    };
  }, []);

  if (!ready) return <div />;
  if (!authed) return <Navigate to="/login" replace />;
  return children;
}

function PublicOnly({ children }) {
  const [ready, setReady] = useState(false);
  const [authed, setAuthed] = useState(false);

  useEffect(() => {
    let active = true;

    const check = async () => {
      const token = getAccessToken();
      if (!token) {
        const refreshed = await refreshAccessToken();
        if (!active) return;
        if (active) {
          if (refreshed) {
            setAuthed(true);
          } else {
            clearAuth();
            setAuthed(false);
          }
          setReady(true);
        }
        return;
      }

      if (!isTokenExpired(token)) {
        if (active) {
          setAuthed(true);
          setReady(true);
        }
        return;
      }

      const refreshed = await refreshAccessToken();
      if (!active) return;

      if (refreshed) {
        setAuthed(true);
      } else {
        clearAuth();
        setAuthed(false);
      }
      setReady(true);
    };

    check();
    return () => {
      active = false;
    };
  }, []);

  if (!ready) return <div />;
  if (authed) return <Navigate to="/lobby" replace />;
  return children;
}

export default function App() {
  return (
    <HashRouter>
      <div className="app-shell">
        <SocketLifecycle />
        <Routes>
          <Route
            path="/"
            element={<Navigate to="/login" replace />}
          />
          <Route
            path="/login"
            element={
              <PublicOnly>
                <LoginPage />
              </PublicOnly>
            }
          />
          <Route
            path="/signup"
            element={
              <PublicOnly>
                <SignupPage />
              </PublicOnly>
            }
          />
          <Route
            path="/lobby"
            element={
              <RequireAuth>
                <LobbyPage />
              </RequireAuth>
            }
          />
          <Route
            path="/rooms/:roomId"
            element={
              <RequireAuth>
                <RoomPage />
              </RequireAuth>
            }
          />
          <Route
            path="/game/:roomId"
            element={
              <RequireAuth>
                <GamePage />
              </RequireAuth>
            }
          />
          <Route path="*" element={<Navigate to="/login" replace />} />
        </Routes>
      </div>
    </HashRouter>
  );
}
