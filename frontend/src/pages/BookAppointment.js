import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate }        from 'react-router-dom';
import { doctorService }      from '../services/doctorService';
import { appointmentService } from '../services/appointmentService';
import { formatTime }         from '../utils/helpers';
import './BookAppointment.css';

/* ── Step labels ───────────────────────────────────────────── */
const STEPS = ['Choose Doctor', 'Pick Date & Time', 'Review & Book'];

/* ── Step indicator ────────────────────────────────────────── */
const StepBar = ({ current }) => (
  <div className="ba-steps">
    {STEPS.map((label, i) => (
      <React.Fragment key={label}>
        <div className={`ba-step ${i <= current ? 'ba-step--done' : ''} ${i === current ? 'ba-step--active' : ''}`}>
          <div className="ba-step-dot">
            {i < current ? '✓' : i + 1}
          </div>
          <span className="ba-step-label">{label}</span>
        </div>
        {i < STEPS.length - 1 && (
          <div className={`ba-step-line ${i < current ? 'ba-step-line--done' : ''}`} />
        )}
      </React.Fragment>
    ))}
  </div>
);

/* ── Success screen ─────────────────────────────────────────── */
const SuccessScreen = ({ appointment, doctor, onBook, onHistory }) => (
  <div className="ba-success">
    <div className="ba-success-icon">✅</div>
    <h2 className="ba-success-title">Appointment Booked!</h2>
    <p className="ba-success-sub">
      Your request is <strong>submitted</strong> and will be <strong>PENDING</strong> until the doctor confirms it.
    </p>
    <div className="ba-success-card">
      <div className="ba-confirm-row">
        <span className="ba-confirm-label">Doctor</span>
        <span className="ba-confirm-value">{doctor?.name}</span>
      </div>
      <div className="ba-confirm-row">
        <span className="ba-confirm-label">Specialization</span>
        <span className="ba-confirm-value">{doctor?.specialization}</span>
      </div>
      <div className="ba-confirm-row">
        <span className="ba-confirm-label">Date</span>
        <span className="ba-confirm-value">
          {appointment?.date
            ? new Date(appointment.date + 'T00:00').toLocaleDateString('en-IN', {
                weekday:'long', day:'numeric', month:'long', year:'numeric'
              })
            : '—'}
        </span>
      </div>
      <div className="ba-confirm-row">
        <span className="ba-confirm-label">Time</span>
        <span className="ba-confirm-value">{formatTime(appointment?.time)}</span>
      </div>
      <div className="ba-confirm-row">
        <span className="ba-confirm-label">Status</span>
        <span className="ba-status-pending">PENDING</span>
      </div>
    </div>
    <div className="ba-success-actions">
      <button className="btn btn-ghost" onClick={onHistory}>View History</button>
      <button className="btn btn-primary" onClick={onBook}>Book Another</button>
    </div>
  </div>
);

/* ═══════════════════════════════════════════════════════════════
   MAIN COMPONENT
   ═══════════════════════════════════════════════════════════════ */
const BookAppointment = () => {
  const navigate = useNavigate();

  /* ── State ─────────────────────────────────────────────────── */
  const [step,      setStep]      = useState(0);
  const [doctors,   setDoctors]   = useState([]);
  const [filtered,  setFiltered]  = useState([]);
  const [search,    setSearch]    = useState('');
  const [spec,      setSpec]      = useState('');
  const [selected,  setSelected]  = useState(null);   // chosen doctor
  const [date,      setDate]      = useState('');
  const [time,      setTime]      = useState('');
  const [loading,   setLoading]   = useState(true);
  const [submitting,setSubmitting]= useState(false);
  const [error,     setError]     = useState('');
  const [booked,    setBooked]    = useState(null);   // successful response
  const [timeErr,   setTimeErr]   = useState('');

  /* ── Load doctors ───────────────────────────────────────────── */
  useEffect(() => {
    doctorService.getAll()
      .then(r => { setDoctors(r.data.data ?? []); })
      .catch(() => setError('Failed to load doctors. Please refresh.'))
      .finally(() => setLoading(false));
  }, []);

  /* ── Filter doctors ─────────────────────────────────────────── */
  useEffect(() => {
    let list = doctors;
    if (search) list = list.filter(d =>
      d.name.toLowerCase().includes(search.toLowerCase())
    );
    if (spec)   list = list.filter(d =>
      d.specialization.toLowerCase() === spec.toLowerCase()
    );
    setFiltered(list);
  }, [doctors, search, spec]);

  const specializations = [...new Set(doctors.map(d => d.specialization))].sort();

  /* ── Time validation ────────────────────────────────────────── */
  const validateTime = useCallback((t) => {
    if (!selected || !t) { setTimeErr(''); return true; }
    const [h, m]     = t.split(':').map(Number);
    const [fromH, fromM] = selected.availableFrom.split(':').map(Number);
    const [toH, toM]     = selected.availableTo.split(':').map(Number);
    const mins     = h * 60 + m;
    const fromMins = fromH * 60 + fromM;
    const toMins   = toH * 60 + toM;
    if (mins < fromMins || mins > toMins) {
      setTimeErr(
        `Dr. ${selected.name} is available ${formatTime(selected.availableFrom)} – ${formatTime(selected.availableTo)}`
      );
      return false;
    }
    setTimeErr(''); return true;
  }, [selected]);

  const handleTimeChange = (e) => {
    setTime(e.target.value);
    validateTime(e.target.value);
  };

  /* ── Navigation ─────────────────────────────────────────────── */
  const goNext = () => {
    setError('');
    if (step === 0 && !selected) { setError('Please select a doctor.'); return; }
    if (step === 1) {
      if (!date) { setError('Please select a date.'); return; }
      if (!time) { setError('Please select a time.'); return; }
      if (!validateTime(time)) return;
    }
    setStep(s => s + 1);
  };

  const goBack = () => { setError(''); setStep(s => s - 1); };

  /* ── Submit ─────────────────────────────────────────────────── */
  const handleSubmit = async () => {
    setSubmitting(true); setError('');
    try {
      const res = await appointmentService.book({
        doctorId: selected.id,
        date,
        time,
      });
      setBooked(res.data.data);
    } catch (ex) {
      setError(ex.response?.data?.message || 'Booking failed. Please try a different slot.');
      setStep(1);  // go back to date/time step
    } finally { setSubmitting(false); }
  };

  /* ── Reset ──────────────────────────────────────────────────── */
  const reset = () => {
    setStep(0); setSelected(null); setDate('');
    setTime(''); setBooked(null); setError('');
  };

  /* ── Success screen ─────────────────────────────────────────── */
  if (booked) {
    return (
      <div className="page-content">
        <SuccessScreen
          appointment={booked}
          doctor={selected}
          onBook={reset}
          onHistory={() => navigate('/appointments')}
        />
      </div>
    );
  }

  /* ── Main render ────────────────────────────────────────────── */
  return (
    <div className="page-content">
      <div className="page-header">
        <div>
          <h2 className="page-title">Book an Appointment</h2>
          <p style={{ color: 'var(--text-secondary)', fontSize: '0.875rem', marginTop: '0.25rem' }}>
            Find a doctor and schedule your visit in 3 easy steps.
          </p>
        </div>
        <button className="btn btn-ghost" onClick={() => navigate('/appointments')}>
          My Appointments →
        </button>
      </div>

      <StepBar current={step} />

      {error && <div className="alert alert-error" style={{ marginTop: '1rem' }}>⚠️ {error}</div>}

      {/* ── STEP 0: Choose Doctor ──────────────────────────────── */}
      {step === 0 && (
        <div className="ba-panel">
          {/* Filters */}
          <div className="ba-filters">
            <div className="form-group" style={{ marginBottom: 0 }}>
              <label className="form-label">Search by Name</label>
              <input className="form-input" placeholder="Dr. Smith…"
                value={search} onChange={e => setSearch(e.target.value)} />
            </div>
            <div className="form-group" style={{ marginBottom: 0 }}>
              <label className="form-label">Specialization</label>
              <select className="form-input" value={spec} onChange={e => setSpec(e.target.value)}>
                <option value="">All Specializations</option>
                {specializations.map(s => <option key={s} value={s}>{s}</option>)}
              </select>
            </div>
          </div>

          {/* Doctor grid */}
          {loading ? (
            <div className="spinner-wrap"><div className="spinner" /></div>
          ) : filtered.length === 0 ? (
            <div className="ba-empty">
              <span>🔍</span>
              <p>No doctors match your search.</p>
              <button className="btn btn-ghost btn-sm" onClick={() => { setSearch(''); setSpec(''); }}>
                Clear filters
              </button>
            </div>
          ) : (
            <div className="ba-doctor-grid">
              {filtered.map(d => (
                <div
                  key={d.id}
                  className={`ba-doctor-card ${selected?.id === d.id ? 'ba-doctor-card--selected' : ''}`}
                  onClick={() => { setSelected(d); setError(''); }}
                >
                  {selected?.id === d.id && <div className="ba-selected-badge">✓ Selected</div>}
                  <div className="ba-doc-avatar">
                    {d.name?.charAt(0) ?? 'D'}
                  </div>
                  <div className="ba-doc-info">
                    <p className="ba-doc-name">{d.name}</p>
                    <p className="ba-doc-spec">{d.specialization}</p>
                    <p className="ba-doc-hours">
                      🕐 {formatTime(d.availableFrom)} – {formatTime(d.availableTo)}
                    </p>
                    <p className="ba-doc-email">{d.email}</p>
                  </div>
                </div>
              ))}
            </div>
          )}

          <div className="ba-nav">
            <div />
            <button className="btn btn-primary" onClick={goNext} disabled={!selected}>
              Continue →
            </button>
          </div>
        </div>
      )}

      {/* ── STEP 1: Pick Date & Time ───────────────────────────── */}
      {step === 1 && (
        <div className="ba-panel">
          {/* Selected doctor recap */}
          <div className="ba-doctor-recap">
            <div className="ba-doc-avatar ba-doc-avatar--sm">
              {selected?.name?.charAt(0)}
            </div>
            <div>
              <p className="ba-doc-name">{selected?.name}</p>
              <p className="ba-doc-spec">{selected?.specialization}</p>
            </div>
            <button className="btn btn-ghost btn-sm" onClick={() => setStep(0)}>Change</button>
          </div>

          <div className="ba-datetime-grid">
            {/* Date picker */}
            <div className="card ba-datetime-card">
              <h3 className="ba-dt-title">📅 Select Date</h3>
              <div className="form-group" style={{ marginBottom: 0 }}>
                <input
                  className="form-input"
                  type="date"
                  value={date}
                  min={new Date().toISOString().split('T')[0]}
                  onChange={e => { setDate(e.target.value); setError(''); }}
                />
              </div>
              {date && (
                <p className="ba-date-preview">
                  📆 {new Date(date + 'T00:00').toLocaleDateString('en-IN', {
                    weekday: 'long', day: 'numeric', month: 'long', year: 'numeric',
                  })}
                </p>
              )}
            </div>

            {/* Time picker */}
            <div className="card ba-datetime-card">
              <h3 className="ba-dt-title">🕐 Select Time</h3>
              <p className="ba-avail-window">
                Available window: <strong>
                  {formatTime(selected?.availableFrom)} – {formatTime(selected?.availableTo)}
                </strong>
              </p>
              <div className="form-group" style={{ marginBottom: 0 }}>
                <input
                  className={`form-input ${timeErr ? 'form-input--error' : ''}`}
                  type="time"
                  value={time}
                  onChange={handleTimeChange}
                />
              </div>
              {timeErr && (
                <p className="ba-time-err">⚠️ {timeErr}</p>
              )}
              {time && !timeErr && (
                <p className="ba-time-ok">✅ {formatTime(time)} — within availability</p>
              )}
            </div>
          </div>

          <div className="ba-nav">
            <button className="btn btn-ghost" onClick={goBack}>← Back</button>
            <button className="btn btn-primary" onClick={goNext}
              disabled={!date || !time || !!timeErr}>
              Review Booking →
            </button>
          </div>
        </div>
      )}

      {/* ── STEP 2: Confirm ───────────────────────────────────── */}
      {step === 2 && (
        <div className="ba-panel">
          <div className="ba-confirm-box">
            <h3 className="ba-confirm-heading">Review Your Appointment</h3>
            <p className="ba-confirm-hint">Please confirm the details below before booking.</p>

            <div className="ba-confirm-details">
              <div className="ba-confirm-row">
                <span className="ba-confirm-label">Doctor</span>
                <span className="ba-confirm-value">{selected?.name}</span>
              </div>
              <div className="ba-confirm-row">
                <span className="ba-confirm-label">Specialization</span>
                <span className="ba-confirm-value">{selected?.specialization}</span>
              </div>
              <div className="ba-confirm-row">
                <span className="ba-confirm-label">Email</span>
                <span className="ba-confirm-value">{selected?.email}</span>
              </div>
              <div className="ba-confirm-row">
                <span className="ba-confirm-label">Date</span>
                <span className="ba-confirm-value">
                  {new Date(date + 'T00:00').toLocaleDateString('en-IN', {
                    weekday:'long', day:'numeric', month:'long', year:'numeric',
                  })}
                </span>
              </div>
              <div className="ba-confirm-row">
                <span className="ba-confirm-label">Time</span>
                <span className="ba-confirm-value">{formatTime(time)}</span>
              </div>
              <div className="ba-confirm-row">
                <span className="ba-confirm-label">Initial Status</span>
                <span className="ba-status-pending">PENDING</span>
              </div>
            </div>

            <div className="ba-confirm-notice">
              🔔 Your booking will be <strong>PENDING</strong> until the doctor confirms it.
            </div>

            <div className="ba-nav" style={{ marginTop: '1.5rem' }}>
              <button className="btn btn-ghost" onClick={goBack} disabled={submitting}>
                ← Edit
              </button>
              <button className="btn btn-primary" onClick={handleSubmit} disabled={submitting}>
                {submitting
                  ? <><span className="btn-spinner" /> Submitting…</>
                  : '⚡ Submit Booking'
                }
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default BookAppointment;
