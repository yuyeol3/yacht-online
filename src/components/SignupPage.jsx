import { useState } from "react";
import { useNavigate } from "react-router-dom";
import TopBar from "./TopBar.jsx";
import { apiFetch } from "../lib/api.js";
import { applyAuthResponse } from "../lib/auth.js";

export default function SignupPage() {
  const navigate = useNavigate();
  const [loginId, setLoginId] = useState("");
  const [password, setPassword] = useState("");
  const [passwordConfirm, setPasswordConfirm] = useState("");
  const [nickname, setNickname] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const passwordMismatch = passwordConfirm.length > 0 && password !== passwordConfirm;
  const canSubmit = !loading && password.length > 0 && password === passwordConfirm;

  const onSubmit = async (e) => {
    e.preventDefault();
    setError("");
    if (password !== passwordConfirm) {
      setError("비밀번호가 일치하지 않습니다.");
      return;
    }
    setLoading(true);
    try {
      await apiFetch("/users", {
        method: "POST",
        body: JSON.stringify({ loginId, password, nickname })
      }, { auth: false });

      const authResult = await apiFetch("/auth/login", {
        method: "POST",
        body: JSON.stringify({ loginId, password })
      }, { auth: false });
      applyAuthResponse(authResult, { loginId, fallbackNick: nickname });
      navigate("/lobby");
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      <TopBar rightLabel="Create an account" showAccountActions={false} />
      <div className="container">
        <div className="card" style={{ maxWidth: 460, margin: "0 auto" }}>
          <h2>회원가입</h2>
          <p className="subtle">닉네임과 계정 정보를 입력하세요.</p>
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
            <input
              type="password"
              placeholder="Confirm password"
              value={passwordConfirm}
              onChange={(e) => setPasswordConfirm(e.target.value)}
              required
            />
            {passwordMismatch ? <div className="error">비밀번호가 일치하지 않습니다.</div> : null}
            <input
              placeholder="Nickname"
              value={nickname}
              onChange={(e) => setNickname(e.target.value)}
              required
            />
            <button className="button primary" type="submit" disabled={!canSubmit}>
              {loading ? "가입 중..." : "Sign Up"}
            </button>
          </form>
          <div className="footer-actions" style={{ justifyContent: "space-between" }}>
            <span className="subtle">이미 계정이 있나요?</span>
            <button className="button ghost" onClick={() => navigate("/login")}>Login</button>
          </div>
        </div>
      </div>
    </div>
  );
}
