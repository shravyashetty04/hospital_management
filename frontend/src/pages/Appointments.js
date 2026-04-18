import React, { useEffect, useState, useCallback } from 'react';
import { useNavigate }         from 'react-router-dom';
import { appointmentService }  from '../services/appointmentService';
import { doctorService }       from '../services/doctorService';
import { useAuth }             from '../context/AuthContext';
import { ROLES, APPOINTMENT_STATUS, STATUS_COLORS } from '../utils/constants';
import { formatDate, formatTime } from '../utils/helpers';
import './Appointments.css';

/* ── Status badge ──────────────────────────────────────────── */
const StatusBadge = ({ status }) => (
  <span
    className="appt-badge"
    style={{
      background: `${STATUS_COLORS[status] || '#6b7280'}1a`,
      color:       STATUS_COLORS[status] || '#6b7280',
      border:     `1px solid ${STATUS_COLORS[status] || '#6b7280'}44`,
    }}
  >
    <span className="appt-badge-dot"
      style={{ background: STATUS_COLORS[status] || '#6b7280' }} />
    {status}
  </span>
);

/* ── Cancel confirmation dialog ────────────────────────────── */
const CancelDialog = ({ appointment, onConfirm, onCancel, busy }) => (
  <div className="modal-overlay" onClick={onCancel}>
    <div className="modal appt-cancel-modal" onClick={e => e.stopPropagation()}>
      <div className="appt-cancel-icon">⚠️</div>
      <h3 className="modal-title" style={{ textAlign: 'center' }}>
        Cancel Appointment?
      </h3>
      <p className="appt-cancel-body">
        You are about to cancel your appointment with{' '}
        <strong>Dr. {appointment.doctorName}</strong> on{' '}
        <strong>{formatDate(appointment.date)}</strong> at{' '}
        <strong>{formatTime(appointment.time)}</strong>.
      </p>
      <p className="appt-cancel-warn">
        This action cannot be undone.
      </p>
      <div className="modal-actions">
        <button className="btn btn-ghost" onClick={onCancel} disabled={busy}>
          Keep Appointment
        </button>
        <button className="btn btn-danger" onClick={onConfirm} disabled={busy}>
          {busy ? 'Cancelling…' : 'Yes, Cancel'}
        </button>
      </div>
    </div>
  </div>
);

/* ── Empty state ───────────────────────────────────────────── */
const EmptyState = ({ filter, onBook }) => (
  <div className="appt-empty">
    <div className="appt-empty-icon">📭</div>
    <h3 className="appt-empty-title">
      {filter === 'ALL' ? 'No appointments yet' : `No ${filter} appointments`}
    </h3>
    <p className="appt-empty-sub">
      {filter === 'ALL'
        ? 'Book your first appointment with a doctor.'
        : `You don't have any ${filter.toLowerCase()} appointments.`}
    </p>
    {onBook && filter === 'ALL' && (
      <button className="btn btn-primary" onClick={onBook}>
        + Book Appointment
      </button>
    )}
  </div>
);

/* ═══════════════════════════════════════════════════════════
   MAIN COMPONENT
   ═══════════════════════════════════════════════════════════ */
const Appointments = () => {
  const { user }                        = useAuth();
  const navigate                        = useNavigate();
  const isPatient = user?.role === ROLES.PATIENT;
  const isDoctor  = user?.role === ROLES.DOCTOR;
  const isAdmin   = user?.role === ROLES.ADMIN;

  const [appointments, setAppts]        = useState([]);
  const [loading,      setLoading]      = useState(true);
  const [doctorProfileId, setDoctorProfileId] = useState(null);
  const [filter,       setFilter]       = useState('ALL');
  const [sortField,    setSortField]    = useState('date');
  const [sortDir,      setSortDir]      = useState('desc');
  const [search,       setSearch]       = useState('');
  const [cancelTarget, setCancelTarget] = useState(null);
  const [cancelling,   setCancelling]   = useState(false);
  const [actionId,     setActionId]     = useState(null);
  const [toast,        setToast]        = useState({ type: '', text: '' });

  /* ── Toast ────────────────────────────────────────────────── */
  const notify = (type, text) => {
    setToast({ type, text });
    setTimeout(() => setToast({ type: '', text: '' }), 4000);
  };

  /* ── Load appointments ────────────────────────────────────── */
  const load = useCallback(async () => {
    setLoading(true);
    try {
      let res;
      if (isAdmin) {
        res = await appointmentService.getAll();
      } else if (isDoctor) {
        // Resolve doctor profile ID from userId, then fetch by doctor ID
        let docId = doctorProfileId;
        if (!docId) {
          const profileRes = await doctorService.getByUserId(user.userId);
          docId = profileRes.data.data.id;
          setDoctorProfileId(docId);
        }
        res = await appointmentService.getByDoctor(docId);
      } else {
        // PATIENT
        res = await appointmentService.getMy();
      }
      setAppts(res.data.data ?? []);
    } catch (err) {
      console.error('[Appointments] load error:', err);
      notify('error', err.response?.data?.message || 'Failed to load appointments.');
    } finally {
      setLoading(false);
    }
  }, [isAdmin, isDoctor, doctorProfileId, user?.userId]);


  useEffect(() => { load(); }, [load]);

  /* ── Sort ─────────────────────────────────────────────────── */
  const toggleSort = (field) => {
    if (sortField === field) setSortDir(d => d === 'asc' ? 'desc' : 'asc');
    else { setSortField(field); setSortDir('asc'); }
  };

  const SortIcon = ({ field }) => {
    if (sortField !== field) return <span className="sort-icon sort-icon--idle">↕</span>;
    return <span className="sort-icon">{sortDir === 'asc' ? '↑' : '↓'}</span>;
  };

  /* ── Filter + search + sort pipeline ─────────────────────── */
  const displayed = appointments
    .filter(a => filter === 'ALL' || a.status === filter)
    .filter(a => {
      if (!search) return true;
      const q = search.toLowerCase();
      return (
        a.doctorName?.toLowerCase().includes(q)  ||
        a.patientName?.toLowerCase().includes(q) ||
        a.specialization?.toLowerCase().includes(q) ||
        a.date?.includes(q)
      );
    })
    .sort((a, b) => {
      let va, vb;
      if (sortField === 'date') { va = a.date + a.time; vb = b.date + b.time; }
      else if (sortField === 'doctor')  { va = a.doctorName;  vb = b.doctorName; }
      else if (sortField === 'patient') { va = a.patientName; vb = b.patientName; }
      else if (sortField === 'status')  { va = a.status;      vb = b.status; }
      else { va = a.id; vb = b.id; }
      if (va < vb) return sortDir === 'asc' ? -1 : 1;
      if (va > vb) return sortDir === 'asc' ?  1 : -1;
      return 0;
    });

  /* ── Cancel flow ─────────────────────────────────────────── */
  const handleCancelConfirm = async () => {
    setCancelling(true);
    try {
      await appointmentService.cancel(cancelTarget.id);
      notify('success', `Appointment with Dr. ${cancelTarget.doctorName} cancelled.`);
      setCancelTarget(null);
      load();
    } catch (ex) {
      notify('error', ex.response?.data?.message || 'Cancel failed.');
    } finally {
      setCancelling(false);
    }
  };

  /* ── Doctor/Admin: status update ─────────────────────────── */
  const handleStatusUpdate = async (id, status) => {
    setActionId(id);
    try {
      await appointmentService.updateStatus(id, status);
      notify('success', `Status updated to ${status}.`);
      load();
    } catch (ex) {
      notify('error', ex.response?.data?.message || 'Update failed.');
    } finally {
      setActionId(null);
    }
  };

  /* ── Stats strip ─────────────────────────────────────────── */
  const counts = Object.values(APPOINTMENT_STATUS).reduce((acc, s) => {
    acc[s] = appointments.filter(a => a.status === s).length;
    return acc;
  }, {});

  const STATUS_FILTERS = ['ALL', ...Object.values(APPOINTMENT_STATUS)];

  return (
    <div className="page-content">

      {/* ── Header ──────────────────────────────────────────── */}
      <div className="page-header">
        <div>
          <h2 className="page-title">Appointments</h2>
          <p className="appt-subtitle">
            {appointments.length} total · {counts[APPOINTMENT_STATUS.PENDING] ?? 0} pending
          </p>
        </div>
        {isPatient && (
          <button className="btn btn-primary" onClick={() => navigate('/book')}>
            + Book Appointment
          </button>
        )}
      </div>

      {/* ── Toast ───────────────────────────────────────────── */}
      {toast.text && (
        <div className={`alert alert-${toast.type === 'error' ? 'error' : 'success'} appt-toast`}>
          {toast.type === 'error' ? '⚠️' : '✅'} {toast.text}
        </div>
      )}

      {/* ── Stats strip ─────────────────────────────────────── */}
      <div className="appt-stats-strip">
        {Object.values(APPOINTMENT_STATUS).map(s => (
          <div key={s} className="appt-stat-pill"
            onClick={() => setFilter(f => f === s ? 'ALL' : s)}
            style={{ cursor: 'pointer' }}>
            <span className="appt-stat-dot" style={{ background: STATUS_COLORS[s] }} />
            <span className="appt-stat-count">{counts[s] ?? 0}</span>
            <span className="appt-stat-label">{s}</span>
          </div>
        ))}
      </div>

      {/* ── Toolbar ─────────────────────────────────────────── */}
      <div className="appt-toolbar">
        {/* Search */}
        <div className="appt-search-wrap">
          <span className="appt-search-icon">🔍</span>
          <input
            className="form-input appt-search"
            placeholder="Search doctor, patient, date…"
            value={search}
            onChange={e => setSearch(e.target.value)}
          />
          {search && (
            <button className="appt-search-clear" onClick={() => setSearch('')}>✕</button>
          )}
        </div>

        {/* Status filter pills */}
        <div className="appt-filter-pills">
          {STATUS_FILTERS.map(s => (
            <button
              key={s}
              onClick={() => setFilter(s)}
              className={`appt-pill ${filter === s ? 'appt-pill--active' : ''}`}
              style={filter === s && s !== 'ALL' ? {
                background:   `${STATUS_COLORS[s]}18`,
                borderColor:  STATUS_COLORS[s],
                color:        STATUS_COLORS[s],
              } : {}}
            >
              {s === 'ALL' ? 'All' : s}
              {s !== 'ALL' && counts[s] > 0 && (
                <span className="appt-pill-count">{counts[s]}</span>
              )}
            </button>
          ))}
        </div>
      </div>

      {/* ── Table ───────────────────────────────────────────── */}
      {loading ? (
        <div className="spinner-wrap"><div className="spinner" /></div>
      ) : displayed.length === 0 ? (
        <EmptyState
          filter={filter}
          onBook={isPatient ? () => navigate('/book') : null}
        />
      ) : (
        <div className="card appt-table-card">
          <div className="appt-result-count">
            Showing <strong>{displayed.length}</strong> of {appointments.length} appointments
          </div>
          <div className="table-wrap">
            <table className="appt-table">
              <thead>
                <tr>
                  <th className="appt-th appt-th--sort" onClick={() => toggleSort('date')}>
                    <span>Date & Time</span> <SortIcon field="date" />
                  </th>
                  <th className="appt-th appt-th--sort" onClick={() => toggleSort('doctor')}>
                    <span>Doctor</span> <SortIcon field="doctor" />
                  </th>
                  {(isAdmin || isDoctor) && (
                    <th className="appt-th appt-th--sort" onClick={() => toggleSort('patient')}>
                      <span>Patient</span> <SortIcon field="patient" />
                    </th>
                  )}
                  <th className="appt-th">Specialization</th>
                  <th className="appt-th appt-th--sort" onClick={() => toggleSort('status')}>
                    <span>Status</span> <SortIcon field="status" />
                  </th>
                  <th className="appt-th">Actions</th>
                </tr>
              </thead>
              <tbody>
                {displayed.map((a, idx) => (
                  <tr
                    key={a.id}
                    className={`appt-row ${idx % 2 === 0 ? 'appt-row--even' : ''}`}
                  >
                    {/* Date & Time */}
                    <td>
                      <div className="appt-date-cell">
                        <div className="appt-date-box">
                          <span className="appt-date-day">
                            {a.date ? new Date(a.date + 'T00:00')
                              .toLocaleDateString('en', { day: '2-digit' }) : '—'}
                          </span>
                          <span className="appt-date-mon">
                            {a.date ? new Date(a.date + 'T00:00')
                              .toLocaleDateString('en', { month: 'short' }) : ''}
                          </span>
                        </div>
                        <div>
                          <p className="appt-date-full">{formatDate(a.date)}</p>
                          <p className="appt-time">{formatTime(a.time)}</p>
                        </div>
                      </div>
                    </td>

                    {/* Doctor */}
                    <td>
                      <div className="appt-person-cell">
                        <div className="appt-avatar" style={{
                          background: 'linear-gradient(135deg, var(--primary), var(--accent))'
                        }}>
                          {a.doctorName?.charAt(0) ?? 'D'}
                        </div>
                        <div>
                          <p className="appt-person-name">{a.doctorName}</p>
                          <p className="appt-person-sub">{a.specialization}</p>
                        </div>
                      </div>
                    </td>

                    {/* Patient (admin/doctor only) */}
                    {(isAdmin || isDoctor) && (
                      <td>
                        <div className="appt-person-cell">
                          <div className="appt-avatar" style={{
                            background: 'linear-gradient(135deg, var(--success), var(--accent))'
                          }}>
                            {a.patientName?.charAt(0) ?? 'P'}
                          </div>
                          <p className="appt-person-name">{a.patientName}</p>
                        </div>
                      </td>
                    )}

                    {/* Specialization */}
                    <td>
                      <span className="appt-spec-tag">{a.specialization}</span>
                    </td>

                    {/* Status */}
                    <td><StatusBadge status={a.status} /></td>

                    {/* Actions */}
                    <td>
                      <div className="appt-actions">
                        {/* PATIENT: cancel button */}
                        {isPatient &&
                          ![APPOINTMENT_STATUS.COMPLETED,
                            APPOINTMENT_STATUS.CANCELLED,
                            APPOINTMENT_STATUS.NO_SHOW].includes(a.status) && (
                          <button
                            className="btn btn-danger btn-sm appt-cancel-btn"
                            onClick={() => setCancelTarget(a)}
                            title="Cancel appointment"
                          >
                            Cancel
                          </button>
                        )}

                        {/* DOCTOR/ADMIN: status transitions */}
                        {(isDoctor || isAdmin) &&
                          a.status === APPOINTMENT_STATUS.PENDING && (
                          <button
                            className="btn btn-success btn-sm"
                            disabled={actionId === a.id}
                            onClick={() => handleStatusUpdate(a.id, APPOINTMENT_STATUS.CONFIRMED)}
                          >
                            {actionId === a.id ? '…' : 'Confirm'}
                          </button>
                        )}
                        {(isDoctor || isAdmin) &&
                          a.status === APPOINTMENT_STATUS.CONFIRMED && (<>
                          <button
                            className="btn btn-primary btn-sm"
                            disabled={actionId === a.id}
                            onClick={() => handleStatusUpdate(a.id, APPOINTMENT_STATUS.COMPLETED)}
                          >
                            {actionId === a.id ? '…' : 'Complete'}
                          </button>
                          <button
                            className="btn btn-ghost btn-sm"
                            disabled={actionId === a.id}
                            onClick={() => handleStatusUpdate(a.id, APPOINTMENT_STATUS.NO_SHOW)}
                            title="Mark as no-show"
                          >
                            No-Show
                          </button>
                        </>)}

                        {/* Admin: cancel any */}
                        {isAdmin &&
                          ![APPOINTMENT_STATUS.COMPLETED,
                            APPOINTMENT_STATUS.CANCELLED,
                            APPOINTMENT_STATUS.NO_SHOW].includes(a.status) && (
                          <button
                            className="btn btn-danger btn-sm"
                            onClick={() => setCancelTarget(a)}
                          >
                            Cancel
                          </button>
                        )}

                        {/* Terminal status — no actions */}
                        {[APPOINTMENT_STATUS.COMPLETED,
                          APPOINTMENT_STATUS.CANCELLED,
                          APPOINTMENT_STATUS.NO_SHOW].includes(a.status) && (
                          <span className="appt-no-action">—</span>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* ── Cancel dialog ────────────────────────────────────── */}
      {cancelTarget && (
        <CancelDialog
          appointment={cancelTarget}
          onConfirm={handleCancelConfirm}
          onCancel={() => setCancelTarget(null)}
          busy={cancelling}
        />
      )}
    </div>
  );
};

export default Appointments;
