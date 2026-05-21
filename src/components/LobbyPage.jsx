import { useEffect, useRef, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import TopBar from "./TopBar.jsx";
import { apiFetch } from "../lib/api.js";

export default function LobbyPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const [rooms, setRooms] = useState([]);
  const [page, setPage] = useState(0);
  const [last, setLast] = useState(false);
  const [loading, setLoading] = useState(false);
  const [roomName, setRoomName] = useState("");
  const [error, setError] = useState("");
  const loadingRef = useRef(false);

  const loadRooms = async (nextPage = 0, reset = false, { silent = false } = {}) => {
    if (loadingRef.current) return;
    loadingRef.current = true;
    setLoading(true);
    if (!silent) setError("");
    try {
      const data = await apiFetch(`/rooms?page=${nextPage}&size=12&sort=id,desc`);
      const content = data?.content ?? [];
      setRooms((prev) => (reset ? content : [...prev, ...content]));
      setPage(nextPage);
      setLast(Boolean(data?.last));
    } catch (err) {
      if (!silent) setError(err.message);
    } finally {
      loadingRef.current = false;
      setLoading(false);
    }
  };

  useEffect(() => {
    loadRooms(0, true);
  }, []);

  useEffect(() => {
    const roomError = location.state?.roomError;
    if (roomError) {
      setError(roomError);
      navigate(location.pathname, { replace: true, state: {} });
    }
  }, [location.state, location.pathname, navigate]);

  useEffect(() => {
    const poll = () => {
      if (document.visibilityState !== "visible") return;
      loadRooms(0, true, { silent: true });
    };

    const intervalId = setInterval(poll, 60_000);
    const onVisibilityChange = () => {
      if (document.visibilityState === "visible") {
        poll();
      }
    };

    document.addEventListener("visibilitychange", onVisibilityChange);
    return () => {
      clearInterval(intervalId);
      document.removeEventListener("visibilitychange", onVisibilityChange);
    };
  }, []);

  const createRoom = async () => {
    if (!roomName.trim()) return;
    setError("");
    try {
      const createdRoom = await apiFetch("/rooms", {
        method: "POST",
        body: JSON.stringify({ roomName: roomName.trim() })
      });
      navigate(`/rooms/${createdRoom.roomId}`, { state: { room: createdRoom } });
    } catch (err) {
      setError(err.message);
    }
  };

  return (
    <div>
      <TopBar subtitle="Lobby" />
      <div className="container">
        <div className="grid two">
          <div className="card">
            <h2>게임 방 만들기</h2>
            <p className="subtle">친구들을 초대할 방을 만들어주세요.</p>
            {error ? <div className="error" style={{ marginTop: 12 }}>{error}</div> : null}
            <div className="grid" style={{ marginTop: 16 }}>
              <input
                placeholder="Room name"
                value={roomName}
                onChange={(e) => setRoomName(e.target.value)}
              />
              <button className="button primary" onClick={createRoom}>Create Room</button>
            </div>
            <div className="notice" style={{ marginTop: 16 }}>
              방장은 게임 시작 버튼을 누를 수 있습니다. 모든 참가자가 준비되면 시작하세요.
            </div>
          </div>
          <div className="card">
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
              <h2>열려있는 방</h2>
              <button className="button ghost" onClick={() => loadRooms(0, true)}>Refresh</button>
            </div>
            <div className="grid" style={{ marginTop: 12 }}>
              {rooms.length === 0 && !loading ? (
                <div className="subtle">아직 방이 없습니다.</div>
              ) : null}
              {rooms.map((room) => (
                room.participatedUsers == 0 ?
                <></> :
                <div className="card" key={room.id} style={{ padding: 16 }}>
                  <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                    <div>
                      <div style={{ fontWeight: 600 }}>{room.roomName}</div>
                      <div className="subtle">Host: {room.hostNickName}</div>
                    </div>
                    <div className="tag">{room.participatedUsers ?? 0} / 4</div>
                  </div>
                  <div className="footer-actions">
                    {room?.status == "PLAYING" ? 
                      <button className="button">Playing</button> :
                      <button className="button primary" onClick={() => navigate(`/rooms/${room.id}`, { state: { room } })}>Enter</button>
                    }
                  </div>
                </div>
              ))}
            </div>
            <div className="footer-actions" style={{ justifyContent: "center" }}>
              <button className="button ghost" disabled={last || loading} onClick={() => loadRooms(page + 1)}>
                {last ? "Last page" : loading ? "Loading..." : "Load more"}
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
