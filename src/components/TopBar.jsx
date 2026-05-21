import { useNavigate } from "react-router-dom";
import { apiFetch } from "../lib/api.js";
import { clearAuth, getProfile } from "../lib/auth.js";

export default function TopBar({ subtitle, rightLabel, showAccountActions = true }) {
  const navigate = useNavigate();
  const profile = showAccountActions ? getProfile() : null;

  const logout = async () => {
    try {
      await apiFetch("/auth/logout", { method: "POST" });
    } catch (err) {
      // ignore
    }
    clearAuth();
    navigate("/login");
  };

  return (
    <div className="top-bar">
      <div className="brand">
        <span className="brand-mark">Y</span>
        Yacht Multiplayer
        {subtitle ? <span className="page-chip">{subtitle}</span> : null}
      </div>
      {showAccountActions ? (
        <div className="top-bar-actions">
          <span className="subtle">{profile?.nickname ?? profile?.loginId ?? "Player"}</span>
          <button className="button ghost" onClick={() => navigate("/lobby")}>Lobby</button>
          <button className="button primary" onClick={logout}>Logout</button>
        </div>
      ) : (
        <div className="subtle">{rightLabel}</div>
      )}
    </div>
  );
}
