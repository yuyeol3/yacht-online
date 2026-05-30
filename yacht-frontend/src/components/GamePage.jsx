import { useEffect, useMemo, useRef, useState } from "react";
import { useLocation, useNavigate, useParams } from "react-router-dom";
import TopBar from "./TopBar.jsx";
import { apiFetch } from "../lib/api.js";
import { getAccessToken, getProfile, mergeProfile, parseJwt } from "../lib/auth.js";
import { connectWs, publishWs, subscribeWs } from "../lib/wsClient.js";
import { SCORE_CATEGORIES, calcPossibleScores } from "../lib/score.js";
import DiceRoll3DOverlay from "./DiceRoll3DOverlay.jsx";

function normalizeRoom(room) {
  if (!room) return room;
  return {
    ...room,
    id: room.id ?? room.roomId
  };
}

function formatLeftMillis(ms) {
  const totalSeconds = Math.max(0, Math.ceil(ms / 1000));
  const mins = Math.floor(totalSeconds / 60);
  const secs = totalSeconds % 60;
  return `${String(mins).padStart(2, "0")}:${String(secs).padStart(2, "0")}`;
}

export default function GamePage() {
  const { roomId } = useParams();
  const navigate = useNavigate();
  const location = useLocation();
  const [room, setRoom] = useState(() => normalizeRoom(location.state?.room) ?? null);
  const [gameState, setGameState] = useState(null);
  const [gameOver, setGameOver] = useState(false);
  const [ranking, setRanking] = useState([]);
  const [pendingSelection, setPendingSelection] = useState(null);
  const [timedOutBanner, setTimedOutBanner] = useState(false);
  const [error, setError] = useState("");
  const [socketError, setSocketError] = useState("");
  const [timeLeft, setTimeLeft] = useState("--:--");
  const [showRollOverlay, setShowRollOverlay] = useState(false);
  const [overlayDiceValues, setOverlayDiceValues] = useState([]);
  const [overlaySeed, setOverlaySeed] = useState(1);
  const rollOverlayMs = 2400;
  const [showCategoryToast, setShowCategoryToast] = useState(false);
  const [categoryToastText, setCategoryToastText] = useState("");
  const [localKept, setLocalKept] = useState([false, false, false, false, false]);
  const [pendingRollAfterKeepSync, setPendingRollAfterKeepSync] = useState(false);
  const pendingRollRef = useRef(false);
  const expectedKeptRef = useRef(null);
  const profile = getProfile();
  const roomWsUrl = room?.gameServer?.wsUrl;

  useEffect(() => {
    const loadRoom = async () => {
      try {
        const data = normalizeRoom(await apiFetch(`/rooms/${roomId}`));
        setRoom(data);
        setError("");
      } catch (err) {
        setError(err.message);
      }
    };
    loadRoom();
  }, [roomId]);

  useEffect(() => {
    if (gameState) return;
    const fromNav = location.state?.initialGameState;
    if (fromNav?.roomId) {
      setGameState(fromNav);
      return;
    }
    const cached = sessionStorage.getItem(`yacht.initialGameState.${roomId}`);
    if (cached) {
      try {
        const parsed = JSON.parse(cached);
        if (parsed?.roomId) setGameState(parsed);
      } catch (err) {
        // ignore
      }
    }
  }, [roomId, location.state, gameState]);

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

  const participantsMap = useMemo(() => {
    const map = new Map();
    (room?.participants ?? []).forEach((p) => map.set(p.userId, p.userNick));
    return map;
  }, [room]);

  const turnList = gameState?.turnList ?? [];
  const columns = turnList.length ? turnList : Array.from(participantsMap.keys());
  const turnUserId = gameState?.curTurnUserId ?? null;

  const isMyTurn = gameState?.curTurnUserId && myUserId === gameState.curTurnUserId;
  const canToggleKeep = Boolean(isMyTurn) && !gameOver && (gameState?.leftRollCnt ?? 3) < 3;
  const serverKept = useMemo(
    () => (Array.isArray(gameState?.kept) ? gameState.kept.map(Boolean) : [false, false, false, false, false]),
    [gameState?.kept]
  );
  const displayedKept = isMyTurn ? localKept : serverKept;
  const possibleScores = useMemo(() => calcPossibleScores(gameState?.dice ?? []), [gameState?.dice]);

  const findBestCategoryLabel = (state, userId) => {
    const dice = state?.dice ?? [];
    const scores = calcPossibleScores(dice);
    const excluded = new Set(["ones", "twos", "threes", "fours", "fives", "sixes", "choice"]);
    const selectedScores = state?.scores?.[String(userId)] ?? {};
    let bestLabel = "";
    let bestScore = -1;
    for (const cat of SCORE_CATEGORIES) {
      if (excluded.has(cat.key)) continue;
      if (selectedScores?.[cat.key] != null) continue;
      const value = scores?.[cat.key];
      if (value == null) continue;
      if (value > bestScore) {
        bestScore = value;
        bestLabel = cat.label;
      }
    }
    return bestScore > 0 ? bestLabel : "";
  };

  const isSameKept = (a = [], b = []) => {
    if (a.length !== b.length) return false;
    for (let i = 0; i < a.length; i += 1) {
      if (Boolean(a[i]) !== Boolean(b[i])) return false;
    }
    return true;
  };

  useEffect(() => {
    pendingRollRef.current = pendingRollAfterKeepSync;
  }, [pendingRollAfterKeepSync]);

  const computeRanking = (state) => {
    if (Array.isArray(state?.playedResults) && state.playedResults.length > 0) {
      return [...state.playedResults]
        .map((entry) => ({
          id: entry.id,
          name: entry.nickname ?? "Player",
          total: entry.score ?? 0,
          rank: entry.rank ?? null,
          result: entry.result ?? null
        }))
        .sort((a, b) => {
          if (a.rank != null && b.rank != null) return a.rank - b.rank;
          return b.total - a.total;
        });
    }

    const scores = state?.scores ?? {};
    return Object.keys(scores)
      .map((id) => ({
        id: Number(id),
        name: participantsMap.get(Number(id)) ?? "Player",
        total: scores[id]?.total ?? 0
      }))
      .sort((a, b) => b.total - a.total);
  };

  useEffect(() => {
    if (!roomWsUrl) return;

    let unsubRoom = null;
    let unsubErr = null;
    let unsubErrLegacy = null;
    let active = true;

    setSocketError("");

    connectWs({
      wsUrl: roomWsUrl,
      onWebSocketError: () => setSocketError("WebSocket 연결이 끊겼습니다."),
      onStompError: () => setSocketError("서버에서 오류를 반환했습니다. 다시 시도해 주세요."),
      onAuthFailure: () => navigate("/login")
    })
      .then(() => {
        if (!active) return;

        unsubRoom = subscribeWs(`/sub/rooms/${roomId}`, (msg) => {
          try {
            const payload = JSON.parse(msg.body);
            if (payload?.type === "GAME_OVER") {
              setGameOver(true);
              if (payload?.data) {
                setGameState(payload.data);
                setRanking(computeRanking(payload.data));
              }
              return;
            }
            if (payload?.type === "TIME_OUT") {
              if (payload?.data) {
                setGameState(payload.data);
              }
              setPendingSelection(null);
              setTimedOutBanner(true);
              return;
            }
            if (payload?.type === "ROLL" && payload?.data) {
              const reducedMotion = typeof window !== "undefined"
                && window.matchMedia
                && window.matchMedia("(prefers-reduced-motion: reduce)").matches;
              const isCompactViewport = typeof window !== "undefined" && window.innerWidth < 640;
              const rollingDice = (payload.data.dice ?? []).filter((_, idx) => !payload.data.kept?.[idx]);

              if (!reducedMotion && !isCompactViewport && rollingDice.length > 0) {
                setOverlayDiceValues(rollingDice);
                setOverlaySeed(Date.now() % 100000);
                setShowRollOverlay(true);
              }

              const turnUserIdFromPayload = Number(payload.data.curTurnUserId);
              const bestLabel = findBestCategoryLabel(payload.data, turnUserIdFromPayload);
              if (bestLabel) {
                setCategoryToastText(bestLabel);
                setShowCategoryToast(false);
                setTimeout(() => setShowCategoryToast(true), 10);
              }
            }

            if (["START", "ROLL", "KEEP_TOGGLE", "SELECT_SCORE"].includes(payload?.type) && payload?.data) {
              setGameState(payload.data);
            }

            if (payload?.type === "KEEP_TOGGLE" && payload?.data && pendingRollRef.current) {
              const expected = expectedKeptRef.current;
              const serverKept = Array.isArray(payload.data.kept) ? payload.data.kept.map(Boolean) : [];
              if (expected && isSameKept(serverKept, expected)) {
                setPendingRollAfterKeepSync(false);
                pendingRollRef.current = false;
                expectedKeptRef.current = null;
                sendAction({ type: "ROLL" });
              }
            }
          } catch (err) {
            setSocketError("게임 상태 업데이트를 처리하지 못했습니다.");
          }
        });

        const handleSocketError = (msg) => {
          try {
            const payload = JSON.parse(msg.body);
            const errorData = payload?.data ?? payload;
            setSocketError(errorData?.message ?? "동작이 허용되지 않습니다.");
            setPendingRollAfterKeepSync(false);
            pendingRollRef.current = false;
            expectedKeptRef.current = null;
          } catch (err) {
            setSocketError("서버 에러 메시지를 처리하지 못했습니다.");
          }
        };

        unsubErr = subscribeWs(`/user/queue/errors`, handleSocketError);
        // Fallback: some brokers expose user queue without `/user` prefix.
        unsubErrLegacy = subscribeWs(`/queue/errors`, handleSocketError);
      })
      .catch(() => {
        if (active) setSocketError("실시간 연결에 실패했습니다.");
      });

    return () => {
      active = false;
      if (unsubRoom) unsubRoom();
      if (unsubErr) unsubErr();
      if (unsubErrLegacy) unsubErrLegacy();
    };
  }, [roomId, roomWsUrl, navigate]);

  useEffect(() => {
    if (!pendingSelection || !gameState) return;
    const score = gameState.scores?.[String(pendingSelection.userId)]?.[pendingSelection.key];
    if (score != null) {
      setPendingSelection(null);
    }
  }, [pendingSelection, gameState, myUserId]);

  useEffect(() => {
    if (!timedOutBanner) return;
    const timer = setTimeout(() => setTimedOutBanner(false), 1800);
    return () => clearTimeout(timer);
  }, [timedOutBanner]);

  useEffect(() => {
    if (!showRollOverlay) return;
    const timer = setTimeout(() => setShowRollOverlay(false), rollOverlayMs);
    return () => clearTimeout(timer);
  }, [showRollOverlay, rollOverlayMs]);

  useEffect(() => {
    if (!showCategoryToast) return;
    const timer = setTimeout(() => setShowCategoryToast(false), 1200);
    return () => clearTimeout(timer);
  }, [showCategoryToast]);

  useEffect(() => {
    const serverLeftMillis = Number(gameState?.leftMillis);
    if (Number.isFinite(serverLeftMillis)) {
      const receivedAt = Date.now();
      const update = () => {
        const elapsed = Date.now() - receivedAt;
        setTimeLeft(formatLeftMillis(serverLeftMillis - elapsed));
      };
      update();
      const timer = setInterval(update, 1000);
      return () => clearInterval(timer);
    }

    if (!gameState?.turnTimeoutTime) {
      setTimeLeft("--:--");
      return;
    }

    const update = () => {
      const normalizedTime = gameState.turnTimeoutTime.replace(/(\.\d{3})\d+$/, "$1");
      const target = new Date(`${normalizedTime}Z`).getTime();
      const diff = Math.max(0, target - Date.now());
      setTimeLeft(formatLeftMillis(diff));
    };
    update();
    const timer = setInterval(update, 1000);
    return () => clearInterval(timer);
  }, [gameState?.leftMillis, gameState?.turnTimeoutTime]);

  useEffect(() => {
    if (!isMyTurn) {
      setLocalKept(serverKept);
      return;
    }
    if (!pendingRollAfterKeepSync) {
      setLocalKept(serverKept);
    }
  }, [isMyTurn, serverKept, pendingRollAfterKeepSync]);

  useEffect(() => {
    if (isMyTurn) return;
    setPendingRollAfterKeepSync(false);
    pendingRollRef.current = false;
    expectedKeptRef.current = null;
  }, [isMyTurn]);

  useEffect(() => {
    if (!pendingRollAfterKeepSync || !gameState) return;
    const expected = expectedKeptRef.current;
    if (!expected) return;
    const serverKept = Array.isArray(gameState.kept) ? gameState.kept.map(Boolean) : [];
    if (isSameKept(serverKept, expected)) {
      setPendingRollAfterKeepSync(false);
      pendingRollRef.current = false;
      expectedKeptRef.current = null;
      sendAction({ type: "ROLL" });
    }
  }, [pendingRollAfterKeepSync, gameState?.kept]);

  const sendAction = (payload) => {
    publishWs({
      destination: `/pub/games/${roomId}/action`,
      body: JSON.stringify({ roomId: Number(roomId), ...payload })
    });
  };

  const rollDice = () => {
    const keepIndices = [];
    for (let i = 0; i < Math.max(serverKept.length, localKept.length); i += 1) {
      if (Boolean(serverKept[i]) !== Boolean(localKept[i])) {
        keepIndices.push(i);
      }
    }
    if (keepIndices.length > 0) {
      expectedKeptRef.current = localKept.map(Boolean);
      setPendingRollAfterKeepSync(true);
      pendingRollRef.current = true;
      sendAction({ type: "KEEP_TOGGLE", keepIndices });
      return;
    }
    sendAction({ type: "ROLL" });
  };

  const toggleKeep = (index) => {
    setLocalKept((prev) => {
      const next = [...prev];
      next[index] = !next[index];
      return next;
    });
  };
  const selectScore = (category, key) => {
    if (!myUserId) return;
    setPendingSelection({ userId: myUserId, scoreCategory: category, key });
    sendAction({ type: "SELECT_SCORE", scoreCategory: category });
  };

  const leaveGame = () => {
    publishWs({ destination: `/pub/rooms/${roomId}/leave` });
    navigate("/lobby");
  };

  const rematchInRoom = () => {
    navigate(`/rooms/${roomId}`);
  };

  return (
    <div>
      <TopBar subtitle="Game" />
      <div className={`category-toast ${showCategoryToast ? "show" : ""}`}>
        {categoryToastText}
      </div>
      <DiceRoll3DOverlay
        visible={showRollOverlay}
        diceValues={overlayDiceValues}
        seed={overlaySeed}
        onDone={() => setShowRollOverlay(false)}
      />
      <div className="container">
        {error ? <div className="error">{error}</div> : null}
        {socketError ? <div className="error" style={{ marginTop: 12 }}>{socketError}</div> : null}
        {timedOutBanner ? <div className="notice" style={{ marginTop: 12 }}>시간 초과로 자동 점수 처리 후 다음 턴으로 넘어갔습니다.</div> : null}
        {!gameState ? (
          <div className="card" style={{ marginTop: 12 }}>
            <h2>게임 시작 대기 중</h2>
            <p className="subtle">방장이 시작하면 자동으로 상태가 표시됩니다.</p>
            <button className="button ghost" onClick={() => navigate(`/rooms/${roomId}`)}>대기실로 이동</button>
          </div>
        ) : (
          <div className="grid two" style={{ marginTop: 12 }}>
            <div className="card">
              <h2>현재 턴</h2>
              <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                <div>
                  <div style={{ fontSize: 22, fontWeight: 700 }}>
                    {participantsMap.get(gameState.curTurnUserId) ?? "Player"}
                  </div>
                  <div className="subtle">Round {gameState.round} / Turn {gameState.turn + 1}</div>
                </div>
                <div className="tag">남은 시간 {timeLeft}</div>
              </div>
              <div className="banner" style={{ marginTop: 12 }}>
                남은 굴림 횟수: {gameState.leftRollCnt}
              </div>
              {gameOver ? (
                <div className="notice" style={{ marginTop: 12 }}>
                  게임이 종료되었습니다.
                </div>
              ) : null}
              <div className="dice-grid">
                {(gameState.dice ?? []).map((die, idx) => (
                  <button
                    key={`die-${idx}`}
                    className={`die ${displayedKept?.[idx] ? "kept" : ""}`}
                    onClick={() => canToggleKeep && toggleKeep(idx)}
                    disabled={!canToggleKeep}
                  >
                    {die}
                  </button>
                ))}
              </div>
              <div className="footer-actions" style={{ justifyContent: "space-between" }}>
                <div className="subtle">{isMyTurn ? "내 턴입니다." : "상대 턴 진행 중"}</div>
                <button className="button primary" disabled={!isMyTurn || gameState.leftRollCnt === 0 || gameOver || pendingRollAfterKeepSync} onClick={rollDice}>
                  Roll
                </button>
              </div>
            </div>
            <div className="card">
              <h2>점수판</h2>
              {gameOver && ranking.length ? (
                <div className="banner" style={{ marginBottom: 12 }}>
                  {ranking.map((r, idx) => `${r.rank ?? idx + 1}위 ${r.name} (${r.total})`).join(" · ")}
                </div>
              ) : null}
              <div style={{ overflowX: "auto" }}>
                <table className="table">
                  <thead>
                    <tr>
                      <th>Category</th>
                      {columns.map((id) => (
                        <th key={`head-${id}`}>{participantsMap.get(id) ?? "Player"}</th>
                      ))}
                    </tr>
                  </thead>
                  <tbody>
                    {SCORE_CATEGORIES.map((cat) => (
                      <tr key={cat.key}>
                        <td>{cat.label}</td>
                        {columns.map((id) => {
                          const score = gameState.scores?.[String(id)]?.[cat.key];
                          const isTurnColumn = Number(id) === Number(turnUserId);
                          const canShowPossible = !gameOver && isTurnColumn && gameState.leftRollCnt < 3 && score == null;
                          const canSelect = canShowPossible && isMyTurn && id === myUserId;
                          const possible = canShowPossible ? possibleScores?.[cat.key] : null;
                          const isPending =
                            pendingSelection &&
                            pendingSelection.userId === id &&
                            pendingSelection.key === cat.key &&
                            score == null;
                          const cellClass = score != null
                            ? "score-cell score-cell-filled"
                            : isPending
                              ? "score-cell score-cell-pending"
                              : canSelect
                                ? "score-cell score-cell-selectable"
                                : canShowPossible
                                  ? "score-cell score-cell-preview"
                                : "score-cell score-cell-empty";

                          return (
                            <td
                              key={`${cat.key}-${id}`}
                              className={cellClass}
                              onClick={() => canSelect && !isPending && selectScore(cat.upper, cat.key)}
                            >
                              {score != null
                                ? score
                                : isPending
                                  ? "..."
                                  : canShowPossible
                                    ? (possible ?? 0)
                                    : "-"}
                            </td>
                          );
                        })}
                      </tr>
                    ))}
                    <tr>
                      <td>Upper</td>
                      {columns.map((id) => (
                        <td key={`upper-${id}`}>{(gameState.scores?.[String(id)]?.upperScore ?? "-") + "/63"}</td>
                      ))}
                    </tr>
                    <tr>
                      <td>Bonus</td>
                      {columns.map((id) => (
                        <td key={`bonus-${id}`}>{gameState.scores?.[String(id)]?.bonus ?? "-"}</td>
                      ))}
                    </tr>
                    <tr>
                      <td>Total</td>
                      {columns.map((id) => (
                        <td key={`total-${id}`}>{gameState.scores?.[String(id)]?.total ?? "-"}</td>
                      ))}
                    </tr>
                  </tbody>
                </table>
              </div>
              <div className="footer-actions">
                {gameOver ? <button className="button primary" onClick={rematchInRoom}>한 판 더 하기</button> : null}
                <button className="button ghost" onClick={leaveGame}>게임 나가기</button>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
