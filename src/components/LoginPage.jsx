import { useState } from "react";
import { useNavigate } from "react-router-dom";
import TopBar from "./TopBar.jsx";
import { apiFetch } from "../lib/api.js";
import { applyAuthResponse } from "../lib/auth.js";

export default function LoginPage() {
  const navigate = useNavigate();
  const [loginId, setLoginId] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const onSubmit = async (e) => {
    e.preventDefault();
    setError("");
    setLoading(true);
    try {
      const authResult = await apiFetch("/auth/login", {
        method: "POST",
        body: JSON.stringify({ loginId, password })
      }, { auth: false });
      applyAuthResponse(authResult, { loginId });
      navigate("/lobby");
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      <TopBar rightLabel="Welcome!" showAccountActions={false} />
      <div className="container">
        <div className="card" style={{ maxWidth: 460, margin: "0 auto" }}>
          <h2>로그인</h2>
          <p className="subtle">계정 정보로 접속하세요.</p>
          {error ? <div className="error" style={{ marginTop: 12 }}>{error}</div> : null}
          <form onSubmit={onSubmit} className="grid" style={{ marginTop: 16 }}>
            <input
              placeholder="Login ID"
              value={loginId}
              onChange={(e) => setLoginId(e.target.value)}
              required
            />
            <input
              type="password"
              placeholder="Password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
            />
            <button className="button primary" type="submit" disabled={loading}>
              {loading ? "접속 중..." : "Login"}
            </button>
          </form>
          <div className="footer-actions" style={{ justifyContent: "space-between" }}>
            <span className="subtle">아직 계정이 없나요?</span>
            <button className="button ghost" onClick={() => navigate("/signup")}>Sign Up</button>
          </div>
        </div>
      </div>
    </div>
  );
}
