import React, { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { ROLES }   from '../utils/constants';
import './Auth.css';

/* ── Password strength calculator ───────────────────────────── */
const getStrength = (pwd) => {
  if (!pwd) return { score: 0, label: '', color: '' };
  let score = 0;
  if (pwd.length >= 8)               score++;
  if (pwd.length >= 12)              score++;
  if (/[A-Z]/.test(pwd))            score++;
  if (/[0-9]/.test(pwd))            score++;
  if (/[^A-Za-z0-9]/.test(pwd))    score++;
  const map = {
    0: { label: '',          color: 'transparent' },
    1: { label: 'Weak',      color: '#ef4444' },
    2: { label: 'Fair',      color: '#f97316' },
    3: { label: 'Good',      color: '#f59e0b' },
    4: { label: 'Strong',    color: '#3b82f6' },
    5: { label: 'Very Strong', color: '#10b981' },
  };
  return { score, ...map[score] };
};

const ROLE_OPTIONS = [
  { value: ROLES.PATIENT, icon: '🧑‍🤝‍🧑', label: 'Patient',  desc: 'Book & manage appointments' },
  { value: ROLES.DOCTOR,  icon: '👨‍⚕️', label: 'Doctor',   desc: 'Manage your schedule' },
];

const Register = () => {
  const [form,       setForm]       = useState({
    name: '', email: '', password: '', confirmPassword: '', role: ROLES.PATIENT,
  });
  const [showPwd,    setShowPwd]    = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [fieldErr,   setFieldErr]   = useState({});
  const [step,       setStep]       = useState(1); // 2-step form

  const { register, error, clearError, isAuthenticated } = useAuth();
  const navigate = useNavigate();

  /* Redirect if already logged in */
  useEffect(() => {
    if (isAuthenticated) navigate('/dashboard', { replace: true });
  }, [isAuthenticated, navigate]);

  const strength = getStrength(form.password);

  /* ── Validate ───────────────────────────────────────────── */
  const validateStep1 = () => {
    const errs = {};
    if (!form.name || form.name.trim().length < 2)
      errs.name = 'Full name must be at least 2 characters.';
    if (!form.email || !/\S+@\S+\.\S+/.test(form.email))
      errs.email = 'Enter a valid email address.';
    return errs;
  };

  const validateStep2 = () => {
    const errs = {};
    if (!form.password || form.password.length < 8)
      errs.password = 'Password must be at least 8 characters.';
    if (form.password !== form.confirmPassword)
      errs.confirmPassword = 'Passwords do not match.';
    return errs;
  };

  const handleChange = (e) => {
    clearError();
    const { name, value } = e.target;
    setForm(f => ({ ...f, [name]: value }));
    if (fieldErr[name]) setFieldErr(f => ({ ...f, [name]: '' }));
  };

  /* ── Step 1 → Step 2 ────────────────────────────────────── */
  const handleNextStep = (e) => {
    e.preventDefault();
    const errs = validateStep1();
    if (Object.keys(errs).length) { setFieldErr(errs); return; }
    setFieldErr({});
    setStep(2);
  };

  /* ── Final submit ───────────────────────────────────────── */
  const handleSubmit = async (e) => {
    e.preventDefault();
    const errs = validateStep2();
    if (Object.keys(errs).length) { setFieldErr(errs); return; }

    setSubmitting(true);
    try {
      await register({
        name:     form.name.trim(),
        email:    form.email.trim(),
        password: form.password,
        role:     form.role,
      });
      /* AuthContext already saved tokens to localStorage */
      navigate('/dashboard', { replace: true });
    } catch { /* Error rendered via AuthContext */ }
    finally   { setSubmitting(false); }
  };

  return (
    <div className="auth-page">
      <div className="auth-card auth-card--wide">

        {/* Logo */}
        <div className="auth-logo">
          <div className="auth-logo-icon">🏥</div>
          <div>
            <h1 className="auth-title">MediCare</h1>
            <p className="auth-tagline">Hospital Management System</p>
          </div>
        </div>

        <h2 className="auth-heading">Create your account</h2>

        {/* Step indicator */}
        <div className="step-indicator">
          <div className={`step-dot ${step >= 1 ? 'step-dot--active' : ''}`}>1</div>
          <div className={`step-line ${step >= 2 ? 'step-line--active' : ''}`} />
          <div className={`step-dot ${step >= 2 ? 'step-dot--active' : ''}`}>2</div>
        </div>
        <p className="auth-subtitle">
          {step === 1 ? 'Your basic information' : 'Secure your account'}
        </p>

        {/* API error */}
        {error && (
          <div className="alert alert-error">
            <span>⚠️</span> {error}
          </div>
        )}

        {/* ── STEP 1 — Identity ─────────────────────────────── */}
        {step === 1 && (
          <form onSubmit={handleNextStep} className="auth-form" noValidate>

            {/* Role selector */}
            <div className="form-group">
              <label className="form-label">I am registering as</label>
              <div className="role-selector">
                {ROLE_OPTIONS.map(({ value, icon, label, desc }) => (
                  <label
                    key={value}
                    className={`role-option ${form.role === value ? 'role-option--active' : ''}`}
                  >
                    <input
                      type="radio" name="role" value={value}
                      checked={form.role === value}
                      onChange={handleChange} hidden
                    />
                    <span className="role-icon">{icon}</span>
                    <span className="role-label">{label}</span>
                    <span className="role-desc">{desc}</span>
                  </label>
                ))}
              </div>
            </div>

            {/* Full name */}
            <div className="form-group">
              <label className="form-label" htmlFor="reg-name">Full Name</label>
              <div className="input-wrap">
                <span className="input-icon">👤</span>
                <input
                  id="reg-name"
                  className={`form-input form-input--icon ${fieldErr.name ? 'form-input--error' : ''}`}
                  type="text"
                  name="name"
                  placeholder="Jane Smith"
                  value={form.name}
                  onChange={handleChange}
                  autoFocus
                />
              </div>
              {fieldErr.name && <p className="field-error">{fieldErr.name}</p>}
            </div>

            {/* Email */}
            <div className="form-group">
              <label className="form-label" htmlFor="reg-email">Email Address</label>
              <div className="input-wrap">
                <span className="input-icon">✉️</span>
                <input
                  id="reg-email"
                  className={`form-input form-input--icon ${fieldErr.email ? 'form-input--error' : ''}`}
                  type="email"
                  name="email"
                  placeholder="you@example.com"
                  value={form.email}
                  onChange={handleChange}
                  autoComplete="email"
                />
              </div>
              {fieldErr.email && <p className="field-error">{fieldErr.email}</p>}
            </div>

            <button type="submit" className="btn btn-primary auth-submit">
              Continue →
            </button>
          </form>
        )}

        {/* ── STEP 2 — Password ─────────────────────────────── */}
        {step === 2 && (
          <form onSubmit={handleSubmit} className="auth-form" noValidate>

            {/* Password */}
            <div className="form-group">
              <label className="form-label" htmlFor="reg-password">Password</label>
              <div className="input-wrap">
                <span className="input-icon">🔒</span>
                <input
                  id="reg-password"
                  className={`form-input form-input--icon ${fieldErr.password ? 'form-input--error' : ''}`}
                  type={showPwd ? 'text' : 'password'}
                  name="password"
                  placeholder="Min. 8 characters"
                  value={form.password}
                  onChange={handleChange}
                  autoComplete="new-password"
                  autoFocus
                />
                <button
                  type="button" className="pwd-toggle"
                  onClick={() => setShowPwd(v => !v)}
                  tabIndex={-1}
                >
                  {showPwd ? '🙈' : '👁️'}
                </button>
              </div>
              {fieldErr.password && <p className="field-error">{fieldErr.password}</p>}

              {/* Strength meter */}
              {form.password && (
                <div className="strength-meter">
                  <div className="strength-bars">
                    {[1,2,3,4,5].map(i => (
                      <div
                        key={i}
                        className="strength-bar"
                        style={{
                          background: i <= strength.score ? strength.color : 'var(--bg-600)',
                          transition: 'background 0.3s ease',
                        }}
                      />
                    ))}
                  </div>
                  <span className="strength-label" style={{ color: strength.color }}>
                    {strength.label}
                  </span>
                </div>
              )}

              {/* Password rules */}
              <ul className="pwd-rules">
                {[
                  { ok: form.password.length >= 8,             text: 'At least 8 characters' },
                  { ok: /[A-Z]/.test(form.password),           text: 'One uppercase letter' },
                  { ok: /[0-9]/.test(form.password),           text: 'One number' },
                  { ok: /[^A-Za-z0-9]/.test(form.password),   text: 'One special character' },
                ].map(({ ok, text }) => (
                  <li key={text} className={`pwd-rule ${ok ? 'pwd-rule--ok' : ''}`}>
                    <span>{ok ? '✅' : '○'}</span> {text}
                  </li>
                ))}
              </ul>
            </div>

            {/* Confirm password */}
            <div className="form-group">
              <label className="form-label" htmlFor="reg-confirm">Confirm Password</label>
              <div className="input-wrap">
                <span className="input-icon">🔒</span>
                <input
                  id="reg-confirm"
                  className={`form-input form-input--icon ${fieldErr.confirmPassword ? 'form-input--error' : ''}`}
                  type={showPwd ? 'text' : 'password'}
                  name="confirmPassword"
                  placeholder="Repeat password"
                  value={form.confirmPassword}
                  onChange={handleChange}
                  autoComplete="new-password"
                />
                {form.confirmPassword && (
                  <span className="match-icon">
                    {form.password === form.confirmPassword ? '✅' : '❌'}
                  </span>
                )}
              </div>
              {fieldErr.confirmPassword && (
                <p className="field-error">{fieldErr.confirmPassword}</p>
              )}
            </div>

            {/* JWT storage info */}
            <div className="jwt-info-box">
              <span>🔐</span>
              <div>
                <p className="jwt-info-title">Secure token storage</p>
                <p className="jwt-info-body">
                  After registration, your JWT access token (24h) and refresh token (7d)
                  will be stored in <code>localStorage</code> under keys{' '}
                  <code>hms_access_token</code> and <code>hms_refresh_token</code>.
                </p>
              </div>
            </div>

            <div style={{ display: 'flex', gap: '0.75rem' }}>
              <button
                type="button"
                className="btn btn-ghost"
                onClick={() => setStep(1)}
                disabled={submitting}
              >
                ← Back
              </button>
              <button
                type="submit"
                className="btn btn-primary auth-submit"
                style={{ flex: 1 }}
                disabled={submitting}
              >
                {submitting ? (
                  <><span className="btn-spinner" /> Creating account…</>
                ) : (
                  'Create Account'
                )}
              </button>
            </div>
          </form>
        )}

        <p className="auth-footer">
          Already have an account?{' '}
          <Link to="/login" className="auth-link">Sign in →</Link>
        </p>

        <div className="storage-info">
          <span className="storage-info-dot" />
          <span>Tokens stored in <code>localStorage</code> · Auto-refresh on expiry</span>
        </div>
      </div>

      <div className="auth-blob auth-blob-1" />
      <div className="auth-blob auth-blob-2" />
      <div className="auth-blob auth-blob-3" />
    </div>
  );
};

export default Register;
