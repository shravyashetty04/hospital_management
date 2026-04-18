import React, { useState, useEffect } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { getToken } from '../utils/helpers';
import './Auth.css';

/* ── Token storage indicator (shown briefly after login) ───── */
const TokenStoredBadge = ({ show }) => (
  <div className={`token-badge ${show ? 'token-badge--visible' : ''}`}>
    <span className="token-badge-icon">🔐</span>
    <div>
      <p className="token-badge-title">JWT Stored</p>
      <p className="token-badge-sub">Saved securely in localStorage</p>
    </div>
  </div>
);

const Login = () => {
  const [form,       setForm]       = useState({ email: '', password: '' });
  const [showPwd,    setShowPwd]    = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [fieldErr,   setFieldErr]   = useState({});
  const [tokenSaved, setTokenSaved] = useState(false);

  const { login, error, clearError, isAuthenticated } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const from     = location.state?.from?.pathname || '/dashboard';

  /* Redirect if already logged in */
  useEffect(() => {
    if (isAuthenticated) navigate(from, { replace: true });
  }, [isAuthenticated, navigate, from]);

  /* ── Client-side validation ─────────────────────────────── */
  const validate = () => {
    const errs = {};
    if (!form.email)                        errs.email    = 'Email is required.';
    else if (!/\S+@\S+\.\S+/.test(form.email)) errs.email = 'Enter a valid email.';
    if (!form.password)                     errs.password = 'Password is required.';
    else if (form.password.length < 8)      errs.password = 'Min. 8 characters.';
    return errs;
  };

  const handleChange = (e) => {
    clearError();
    const { name, value } = e.target;
    setForm(f => ({ ...f, [name]: value }));
    if (fieldErr[name]) setFieldErr(f => ({ ...f, [name]: '' }));
  };

  /* ── Submit ─────────────────────────────────────────────── */
  const handleSubmit = async (e) => {
    e.preventDefault();
    const errs = validate();
    if (Object.keys(errs).length) { setFieldErr(errs); return; }

    setSubmitting(true);
    try {
      await login(form.email, form.password);

      /* Show JWT-stored badge, then navigate */
      setTokenSaved(true);
      setTimeout(() => navigate(from, { replace: true }), 1000);
    } catch { /* Error rendered via AuthContext */ }
    finally   { setSubmitting(false); }
  };

  /* ── Check stored token live (for display purposes) ────── */
  const storedToken = getToken();

  return (
    <div className="auth-page">

      {/* ── Main card ──────────────────────────────────────── */}
      <div className="auth-card">

        {/* Logo */}
        <div className="auth-logo">
          <div className="auth-logo-icon">🏥</div>
          <div>
            <h1 className="auth-title">MediCare</h1>
            <p className="auth-tagline">Hospital Management System</p>
          </div>
        </div>

        <h2 className="auth-heading">Welcome back</h2>
        <p className="auth-subtitle">Sign in to access your dashboard</p>

        {/* JWT already stored notice */}
        {storedToken && !submitting && (
          <div className="alert alert-info" style={{ fontSize: '0.78rem' }}>
            🔑 Session active — you are already signed in.{' '}
            <Link to="/dashboard" className="auth-link">Go to Dashboard →</Link>
          </div>
        )}

        {/* API error */}
        {error && (
          <div className="alert alert-error">
            <span>⚠️</span> {error}
          </div>
        )}

        {/* Form */}
        <form onSubmit={handleSubmit} className="auth-form" noValidate>

          {/* Email */}
          <div className="form-group">
            <label className="form-label" htmlFor="login-email">
              Email Address
            </label>
            <div className="input-wrap">
              <span className="input-icon">✉️</span>
              <input
                id="login-email"
                className={`form-input form-input--icon ${fieldErr.email ? 'form-input--error' : ''}`}
                type="email"
                name="email"
                placeholder="you@example.com"
                value={form.email}
                onChange={handleChange}
                autoComplete="email"
                autoFocus
              />
            </div>
            {fieldErr.email && <p className="field-error">{fieldErr.email}</p>}
          </div>

          {/* Password */}
          <div className="form-group">
            <label className="form-label" htmlFor="login-password">
              Password
            </label>
            <div className="input-wrap">
              <span className="input-icon">🔒</span>
              <input
                id="login-password"
                className={`form-input form-input--icon ${fieldErr.password ? 'form-input--error' : ''}`}
                type={showPwd ? 'text' : 'password'}
                name="password"
                placeholder="Min. 8 characters"
                value={form.password}
                onChange={handleChange}
                autoComplete="current-password"
              />
              <button
                type="button"
                className="pwd-toggle"
                onClick={() => setShowPwd(v => !v)}
                tabIndex={-1}
                aria-label={showPwd ? 'Hide password' : 'Show password'}
              >
                {showPwd ? '🙈' : '👁️'}
              </button>
            </div>
            {fieldErr.password && <p className="field-error">{fieldErr.password}</p>}
          </div>

          {/* Submit */}
          <button
            type="submit"
            className="btn btn-primary auth-submit"
            disabled={submitting}
          >
            {submitting ? (
              <><span className="btn-spinner" /> Signing in…</>
            ) : (
              'Sign In'
            )}
          </button>
        </form>

        {/* JWT stored badge */}
        <TokenStoredBadge show={tokenSaved} />

        {/* Footer */}
        <p className="auth-footer">
          Don't have an account?{' '}
          <Link to="/register" className="auth-link">Create one →</Link>
        </p>

        {/* localStorage info strip */}
        <div className="storage-info">
          <span className="storage-info-dot" />
          <span>JWT tokens stored in <code>localStorage</code></span>
        </div>
      </div>

      {/* Decorative blobs */}
      <div className="auth-blob auth-blob-1" />
      <div className="auth-blob auth-blob-2" />
      <div className="auth-blob auth-blob-3" />
    </div>
  );
};

export default Login;
