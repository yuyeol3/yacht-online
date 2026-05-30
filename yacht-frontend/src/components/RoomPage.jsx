import { useEffect, useMemo, useRef, useState } from "react";
import { useLocation, useNavigate, useParams } from "react-router-dom";
import TopBar from "./TopBar.jsx";
import { apiFetch } from "../lib/api.js";
import { clearAuth, getAccessToken, getProfile, mergeProfile, parseJwt } from "../lib/auth.js";
import { connectWs, publishWs, subscribeWs } from "../lib/wsClient.js";

function normalizeRoom(room) {
  if (!room) return room;
  return {
    ...room,
    id: room.id ?? room.roomId
  };
}

export default function RoomPage() {
  const { roomId } = useParams();
  const navigate = useNavigate();
  const location = useLocation();
  const [room, setRoom] = useState(() => normalizeRoom(location.state?.room) ?? null);
  const [error, setError] = useState("");
  const [socketError, setSocketError] = useState("");
  const [connected, setConnected] = useState(false);
  const [enterRequested, setEnterRequested] = useState(false);
  const myUserIdRef = useRef(null);
  const roomRef = useRef(null);

  const profile = getProfile();
  const roomWsUrl = room?.gameServer?.wsUrl;

  const myUserId = useMemo(() => {
    const numericId = Number(profile?.userId);
    if (Number.isFinite(numericId)) return numericId;
    const parsed = parseJwt(getAccessToken() ?? "");
    const tokenUserId = Number(parsed?.sub ?? parsed?.userId ?? parsed?.id);
    if (Number.isFinite(tokenUserId)) return tokenUserId;
    if (!room) return null;
    if (profile?.nickname && room?.participants?.length) {
      const match = room.participants.find((p) => p.userNick === profile.nickname);
      if (match) {
        mergeProfile({ userId: match.userId });
        return match.userId;
      }
    }
    return null;
  }, [room, profile?.userId, profile?.nickname]);

  const hostUserId = Number(room?.host?.userId ?? room?.hostUserId ?? room?.hostId);
  const normalizedMyUserId = Number(myUserId);
  const participantCount = Number.isFinite(Number(room?.participatedUsers))
    ? Number(room?.participatedUsers)
    : (room?.participants?.length ?? 0);
  const hostNick = room?.host?.userNick ?? room?.hostNickName ?? null;
  const myNick = profile?.nickname ?? null;
  const isHost =
    (
      Number.isFinite(hostUserId) &&
      Number.isFinite(normalizedMyUserId) &&
      normalizedMyUserId === hostUserId
    ) ||
    (hostNick && myNick && hostNick === myNick);
  const canStart = connected && isHost && participantCount >= 2;
  const isParticipant = Boolean(
    myUserId != null && room?.participants?.some((p) => Number(p.userId) === Number(myUserId))
  );

  useEffect(() => {
    myUserIdRef.current = myUserId;
  }, [myUserId]);

  useEffect(() => {
    roomRef.current = room;
  }, [room]);

  const refreshRoom = async ({ silent = false } = {}) => {
    try {
      const data = normalizeRoom(await apiFetch(`/rooms/${roomId}`));
      setRoom(data);
      setError("");
      return data;
    } catch (err) {
      if (!silent) {
        setError(err.message);
      }
      throw err;
    }
  };

  useEffect(() => {
    setEnterRequested(false);
    setConnected(false);
    setSocketError("");
    if (location.state?.room) {
      setRoom(normalizeRoom(location.state.room));
    }
    refreshRoom().catch(() => {});
  }, [roomId, location.state]);

  useEffect(() => {
    if (!roomWsUrl) return;

    let unsubRoom = null;
    let unsubErr = null;
    let unsubErrLegacy = null;
    let active = true;

    setConnected(false);
    setSocketError("");

    connectWs({
      wsUrl: roomWsUrl,
      onWebSocketError: () => setSocketError("WebSocket 연결이 끊겼습니다."),
      onStompError: () => setSocketError("서버에서 오류를 반환했습니다. 다시 시도해 주세요."),
      onAuthFailure: () => {
        clearAuth();
        navigate("/login");
      }
    })
      .then(() => {
        if (!active) return;
        setConnected(true);

        unsubRoom = subscribeWs(`/sub/rooms/${roomId}`, (msg) => {
          try {
            const payload = JSON.parse(msg.body);
            if (payload?.type === "START") {
              if (payload?.data) {
                sessionStorage.setItem(`yacht.initialGameState.${roomId}`, JSON.stringify(payload.data));
                navigate(`/game/${roomId}`, { state: { initialGameState: payload.data, room: roomRef.current } });
              } else {
                navigate(`/game/${roomId}`, { state: { room: roomRef.current } });
              }
              return;
            }
            if (["ENTER", "QUIT", "TOGGLE_READY"].includes(payload?.type)) {
              if (payload?.data && (payload?.data?.host?.userId != null || payload?.data?.participatedUsers != null)) {
                setRoom((prev) => normalizeRoom({
                  ...prev,
                  ...payload.data,
                  gameServer: payload.data.gameServer ?? prev?.gameServer
                }));
              }
              refreshRoom({ silent: true }).catch(() => {});
            }
          } catch (err) {
            setSocketError("방 상태 업데이트를 처리하지 못했습니다.");
          }
        });

        const handleSocketError = (msg) => {
          try {
            const payload = JSON.parse(msg.body);
            const errorData = payload?.data ?? payload;
            const code = errorData?.code;
            const message = errorData?.message ?? "동작이 허용되지 않습니다.";
            const shouldSuppressNotFound = code === "C003" && roomRef.current != null;
            if (!shouldSuppressNotFound) {
              setSocketError(message);
            }

            if (code === "G005") {
              refreshRoom({ silent: true })
                .then((latestRoom) => {
                  const userId = myUserIdRef.current;
                  const inRoom = latestRoom?.participants?.some((p) => Number(p.userId) === Number(userId));
                  if (inRoom) navigate(`/game/${roomId}`);
                  else navigate("/lobby");
                })
                .catch(() => navigate("/lobby"));
            }

            if (code === "G007") {
              navigate("/lobby", { state: { roomError: message } });
            }
          } catch (err) {
            setSocketError("서버 에러 메시지를 처리하지 못했습니다.");
          }
        };

        unsubErr = subscribeWs(`/user/queue/errors`, handleSocketError);
        // Fallback: some brokers expose user queue without `/user` prefix.
        unsubErrLegacy = subscribeWs(`/queue/errors`, handleSocketError);
      })
      .catch(() => {
        if (active) {
          setSocketError("실시간 연결에 실패했습니다.");
        }
      });

    return () => {
      active = false;
      setConnected(false);
      if (unsubRoom) unsubRoom();
      if (unsubErr) unsubErr();
      if (unsubErrLegacy) unsubErrLegacy();
    };
  }, [roomId, roomWsUrl, navigate]);

  useEffect(() => {
    if (!connected || !room || enterRequested) return;
    if (!Array.isArray(room.participants)) return;
    if (isParticipant) return;
    publishWs({ destination: `/pub/rooms/${roomId}/enter` });
    setEnterRequested(true);
  }, [connected, room, enterRequested, isParticipant, roomId]);

  const toggleReady = () => {
    publishWs({ destination: `/pub/rooms/${roomId}/toggleReady` });
  };

  const startGame = () => {
    publishWs({ destination: `/pub/rooms/${roomId}/start` });
  };

  const leaveRoom = () => {
    publishWs({ destination: `/pub/rooms/${roomId}/leave` });
    navigate("/lobby");
  };

  return (
    <div>
      <TopBar subtitle="Room" />
      <div className="container">
        {error ? <div className="error">{error}</div> : null}
        {socketError ? <div className="error" style={{ marginTop: 12 }}>{socketError}</div> : null}
        <div className="grid two" style={{ marginTop: 12 }}>
          <div className="card">
            <h2>{room?.roomName ?? "Room"}</h2>
            <p className="subtle">Host: {room?.host?.userNick ?? "-"}</p>
            <div className="notice" style={{ marginTop: 12 }}>
              {connected ? "실시간 연결됨" : "연결 중..."}
            </div>
            <div className="footer-actions">
              <button className="button ghost" onClick={toggleReady} disabled={!connected || !isParticipant}>준비 토글</button>
              <button className="button primary" onClick={startGame} disabled={!canStart}>
                게임 시작
              </button>
              <button className="button warn" onClick={leaveRoom}>방 나가기</button>
            </div>
            {!isHost ? (
              <div className="subtle" style={{ marginTop: 10 }}>방장만 시작 버튼을 누를 수 있습니다.</div>
            ) : null}
          </div>
          <div className="card">
            <h3>참여자 목록</h3>
            <div className="grid" style={{ marginTop: 12 }}>
              {room?.participants?.map((p) => (
                <div className="card" key={p.userId} style={{ padding: 14 }}>
                  <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                    <div>
                      <div style={{ fontWeight: 600 }}>{p.userNick}</div>
                      <div className="subtle">Participant</div>
                    </div>
                    <div className="tag" style={{ background: p.isReady ? "rgba(52, 211, 153, 0.2)" : "rgba(148, 163, 184, 0.2)", color: p.isReady ? "var(--ok)" : "var(--ink-3)" }}>
                      {p.isReady ? "READY" : "WAIT"}
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
