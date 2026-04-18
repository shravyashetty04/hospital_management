import React, { useEffect, useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth }            from '../context/AuthContext';
import { appointmentService } from '../services/appointmentService';
import { doctorService }      from '../services/doctorService';
import { patientService }     from '../services/patientService';
import { ROLES, APPOINTMENT_STATUS, STATUS_COLORS } from '../utils/constants';
import { formatDate, formatTime } from '../utils/helpers';
import './Dashboard.css';

/* ── Shared ── */
const Spinner = () => (
  <div className="spinner-wrap"><div className="spinner" /></div>
);

const StatCard = ({ icon, value, label, color, onClick }) => (
  <div className={`stat-card ${onClick ? 'stat-card--click' : ''}`} onClick={onClick}>
    <div className="stat-card-icon" style={{ color }}>{icon}</div>
    <div className="stat-card-value" style={{ color }}>{value}</div>
    <div className="stat-card-label">{label}</div>
  </div>
);

const StatusBadge = ({ status }) => (
  <span className="badge" style={{
    background: `${STATUS_COLORS[status]}22`,
    color: STATUS_COLORS[status],
  }}>{status}</span>
);

/* ─────────────────────────────────────────────────────────────
   PATIENT DASHBOARD
   ───────────────────────────────────────────────────────────── */
const BookModal = ({ onClose, onBooked }) => {
  const [doctors, setDoctors] = useState([]);
  const [form, setForm]       = useState({ doctorId: '', date: '', time: '' });
  const [busy, setBusy]       = useState(false);
  const [err,  setErr]        = useState('');

  useEffect(() => {
    doctorService.getAll().then(r => setDoctors(r.data.data ?? []));
  }, []);

  const submit = async (e) => {
    e.preventDefault(); setBusy(true); setErr('');
    try {
      await appointmentService.book({
        doctorId: Number(form.doctorId),
        date: form.date,
        time: form.time,
      });
      onBooked();
    } catch (ex) {
      setErr(ex.response?.data?.message || 'Booking failed — check slot availability.');
    } finally { setBusy(false); }
  };

  const selected = doctors.find(d => String(d.id) === String(form.doctorId));

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal" onClick={e => e.stopPropagation()}>
        <h3 className="modal-title">📅 Book Appointment</h3>
        {err && <div className="alert alert-error">{err}</div>}
        <form onSubmit={submit}>
          <div className="form-group">
            <label className="form-label">Select Doctor</label>
            <select className="form-input" value={form.doctorId}
              onChange={e => setForm(f => ({ ...f, doctorId: e.target.value }))} required>
              <option value="">— Choose a doctor —</option>
              {doctors.map(d => (
                <option key={d.id} value={d.id}>
                  {d.name} · {d.specialization}
                </option>
              ))}
            </select>
          </div>
          {selected && (
            <p className="db-avail-hint">
              🕐 Available: {formatTime(selected.availableFrom)} – {formatTime(selected.availableTo)}
            </p>
          )}
          <div className="grid-2">
            <div className="form-group">
              <label className="form-label">Date</label>
              <input className="form-input" type="date"
                min={new Date().toISOString().split('T')[0]}
                value={form.date}
                onChange={e => setForm(f => ({ ...f, date: e.target.value }))} required />
            </div>
            <div className="form-group">
              <label className="form-label">Time</label>
              <input className="form-input" type="time" value={form.time}
                onChange={e => setForm(f => ({ ...f, time: e.target.value }))} required />
            </div>
          </div>
          <div className="modal-actions">
            <button type="button" className="btn btn-ghost" onClick={onClose}>Cancel</button>
            <button type="submit" className="btn btn-primary" disabled={busy}>
              {busy ? 'Booking…' : 'Confirm Booking'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

const PatientDashboard = () => {
  const [appts,   setAppts]   = useState([]);
  const [stats,   setStats]   = useState({});
  const [loading, setLoading] = useState(true);
  const [booking, setBooking] = useState(false);
  const [msg,     setMsg]     = useState('');

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const res   = await appointmentService.getMy();
      const list  = res.data.data ?? [];
      setAppts(list);
      setStats({
        total:     list.length,
        pending:   list.filter(a => a.status === APPOINTMENT_STATUS.PENDING).length,
        confirmed: list.filter(a => a.status === APPOINTMENT_STATUS.CONFIRMED).length,
        completed: list.filter(a => a.status === APPOINTMENT_STATUS.COMPLETED).length,
      });
    } finally { setLoading(false); }
  }, []);

  useEffect(() => { load(); }, [load]);

  const handleBooked = () => {
    setBooking(false);
    setMsg('✅ Appointment booked successfully!');
    setTimeout(() => setMsg(''), 4000);
    load();
  };

  const handleCancel = async (id) => {
    if (!window.confirm('Cancel this appointment?')) return;
    try {
      await appointmentService.cancel(id);
      setMsg('Appointment cancelled.');
      setTimeout(() => setMsg(''), 4000);
      load();
    } catch (ex) {
      setMsg('❌ ' + (ex.response?.data?.message || 'Cancel failed.'));
    }
  };

  if (loading) return <Spinner />;

  return (
    <>
      {/* Quick action */}
      <div className="db-quick-action">
        <div>
          <h3 className="db-quick-title">Ready for your next visit?</h3>
          <p className="db-quick-sub">Find a doctor and book a slot in seconds.</p>
        </div>
        <button className="btn btn-primary" onClick={() => setBooking(true)}>
          + Book Appointment
        </button>
      </div>

      {msg && <div className="alert alert-success">{msg}</div>}

      {/* Stats */}
      <div className="grid-4" style={{ marginBottom: '2rem' }}>
        <StatCard icon="📋" value={stats.total}     label="Total"     color="var(--primary)" />
        <StatCard icon="⏳" value={stats.pending}   label="Pending"   color="var(--warning)" />
        <StatCard icon="✅" value={stats.confirmed} label="Confirmed" color="var(--accent)"  />
        <StatCard icon="🎉" value={stats.completed} label="Completed" color="var(--success)" />
      </div>

      {/* History */}
      <div className="card">
        <div className="db-section-header">
          <h3 className="db-section-title">Appointment History</h3>
          <span className="db-count">{appts.length} total</span>
        </div>
        {appts.length === 0 ? (
          <div className="db-empty">
            <span>📅</span>
            <p>No appointments yet. Book your first one!</p>
            <button className="btn btn-primary btn-sm" onClick={() => setBooking(true)}>
              Book Now
            </button>
          </div>
        ) : (
          <div className="table-wrap">
            <table>
              <thead><tr>
                <th>Date</th><th>Time</th><th>Doctor</th>
                <th>Specialization</th><th>Status</th><th>Action</th>
              </tr></thead>
              <tbody>
                {appts.map(a => (
                  <tr key={a.id}>
                    <td>{formatDate(a.date)}</td>
                    <td>{formatTime(a.time)}</td>
                    <td className="db-name">{a.doctorName}</td>
                    <td className="db-secondary">{a.specialization}</td>
                    <td><StatusBadge status={a.status} /></td>
                    <td>
                      {![APPOINTMENT_STATUS.COMPLETED, APPOINTMENT_STATUS.CANCELLED].includes(a.status) && (
                        <button className="btn btn-danger btn-sm"
                          onClick={() => handleCancel(a.id)}>Cancel</button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {booking && <BookModal onClose={() => setBooking(false)} onBooked={handleBooked} />}
    </>
  );
};

/* ─────────────────────────────────────────────────────────────
   DOCTOR DASHBOARD
   ───────────────────────────────────────────────────────────── */
const DoctorDashboard = ({ userId }) => {
  const [appts,    setAppts]   = useState([]);
  const [doctorId, setDoctorId]= useState(null);
  const [loading,  setLoading] = useState(true);
  const [msg,      setMsg]     = useState('');
  const [filter,   setFilter]  = useState('ALL');

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const docRes = await doctorService.getAll();
      const me     = (docRes.data.data ?? []).find(d => d.userId === userId);
      if (!me) { setLoading(false); return; }
      setDoctorId(me.id);
      const apptRes = await appointmentService.getByDoctor(me.id);
      setAppts(apptRes.data.data ?? []);
    } finally { setLoading(false); }
  }, [userId]);

  useEffect(() => { load(); }, [load]);

  const updateStatus = async (id, status) => {
    try {
      await appointmentService.updateStatus(id, status);
      setMsg(`Status updated to ${status}`);
      setTimeout(() => setMsg(''), 3000);
      load();
    } catch (ex) {
      setMsg('❌ ' + (ex.response?.data?.message || 'Update failed.'));
    }
  };

  const displayed = filter === 'ALL' ? appts
    : appts.filter(a => a.status === filter);

  const today = appts.filter(a => a.date === new Date().toISOString().split('T')[0]);
  const stats = {
    total:     appts.length,
    today:     today.length,
    pending:   appts.filter(a => a.status === APPOINTMENT_STATUS.PENDING).length,
    completed: appts.filter(a => a.status === APPOINTMENT_STATUS.COMPLETED).length,
  };

  if (loading) return <Spinner />;
  if (!doctorId) return (
    <div className="db-empty-full">
      <span>🏥</span>
      <h3>No doctor profile found</h3>
      <p>Contact admin to set up your profile.</p>
    </div>
  );

  return (
    <>
      <div className="grid-4" style={{ marginBottom: '2rem' }}>
        <StatCard icon="📋" value={stats.total}     label="All Appointments" color="var(--primary)" />
        <StatCard icon="📆" value={stats.today}     label="Today"            color="var(--accent)"  />
        <StatCard icon="⏳" value={stats.pending}   label="Pending"          color="var(--warning)" />
        <StatCard icon="✅" value={stats.completed} label="Completed"        color="var(--success)" />
      </div>

      {msg && <div className="alert alert-success">{msg}</div>}

      {/* Filters */}
      <div className="db-filter-bar">
        {['ALL', ...Object.values(APPOINTMENT_STATUS)].map(s => (
          <button key={s} onClick={() => setFilter(s)}
            className={`db-filter-btn ${filter === s ? 'db-filter-btn--active' : ''}`}
            style={filter === s && s !== 'ALL' ? {
              borderColor: STATUS_COLORS[s],
              color: STATUS_COLORS[s],
              background: `${STATUS_COLORS[s]}15`,
            } : {}}>
            {s}
          </button>
        ))}
      </div>

      <div className="card">
        <div className="db-section-header">
          <h3 className="db-section-title">My Schedule</h3>
          <span className="db-count">{displayed.length} appointments</span>
        </div>
        {displayed.length === 0 ? (
          <div className="db-empty"><span>📭</span><p>No appointments for this filter.</p></div>
        ) : (
          <div className="table-wrap">
            <table>
              <thead><tr>
                <th>Date</th><th>Time</th><th>Patient</th>
                <th>Status</th><th>Actions</th>
              </tr></thead>
              <tbody>
                {displayed.map(a => (
                  <tr key={a.id}>
                    <td>{formatDate(a.date)}</td>
                    <td>{formatTime(a.time)}</td>
                    <td className="db-name">{a.patientName}</td>
                    <td><StatusBadge status={a.status} /></td>
                    <td>
                      <div className="db-actions">
                        {a.status === APPOINTMENT_STATUS.PENDING && (
                          <button className="btn btn-success btn-sm"
                            onClick={() => updateStatus(a.id, APPOINTMENT_STATUS.CONFIRMED)}>
                            Confirm
                          </button>
                        )}
                        {a.status === APPOINTMENT_STATUS.CONFIRMED && (<>
                          <button className="btn btn-primary btn-sm"
                            onClick={() => updateStatus(a.id, APPOINTMENT_STATUS.COMPLETED)}>
                            Complete
                          </button>
                          <button className="btn btn-ghost btn-sm"
                            onClick={() => updateStatus(a.id, APPOINTMENT_STATUS.NO_SHOW)}>
                            No-Show
                          </button>
                        </>)}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </>
  );
};

/* ─────────────────────────────────────────────────────────────
   ADMIN DASHBOARD
   ───────────────────────────────────────────────────────────── */
const AdminDashboard = () => {
  const [doctors,  setDoctors]  = useState([]);
  const [appts,    setAppts]    = useState([]);
  const [patients, setPatients] = useState([]);
  const [loading,  setLoading]  = useState(true);
  const [msg,      setMsg]      = useState({ type: '', text: '' });
  const [modal,    setModal]    = useState(null); // 'add'
  const [form,     setForm]     = useState({ userId:'', specialization:'', availableFrom:'', availableTo:'' });
  const [saving,   setSaving]   = useState(false);
  const navigate = useNavigate();

  const notify = (type, text) => {
    setMsg({ type, text });
    setTimeout(() => setMsg({ type:'', text:'' }), 4000);
  };

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [dRes, aRes, pRes] = await Promise.all([
        doctorService.getAll(),
        appointmentService.getAll(),
        patientService.getAll(),
      ]);
      setDoctors(dRes.data.data  ?? []);
      setAppts(aRes.data.data    ?? []);
      setPatients(pRes.data.data ?? []);
    } finally { setLoading(false); }
  }, []);

  useEffect(() => { load(); }, [load]);

  const handleDeleteDoctor = async (id) => {
    if (!window.confirm('Delete this doctor profile?')) return;
    try {
      await doctorService.delete(id);
      notify('success', 'Doctor deleted.');
      load();
    } catch (ex) {
      notify('error', ex.response?.data?.message || 'Delete failed.');
    }
  };

  const handleAddDoctor = async (e) => {
    e.preventDefault(); setSaving(true);
    try {
      await doctorService.create(Number(form.userId), {
        specialization: form.specialization,
        availableFrom:  form.availableFrom,
        availableTo:    form.availableTo,
      });
      notify('success', 'Doctor profile created.');
      setModal(null);
      setForm({ userId:'', specialization:'', availableFrom:'', availableTo:'' });
      load();
    } catch (ex) {
      notify('error', ex.response?.data?.message || 'Failed to create profile.');
    } finally { setSaving(false); }
  };

  const recent = [...appts].sort((a,b) => (b.date > a.date ? 1 : -1)).slice(0, 6);

  if (loading) return <Spinner />;

  return (
    <>
      {/* Stats */}
      <div className="grid-4" style={{ marginBottom: '2rem' }}>
        <StatCard icon="👨‍⚕️" value={doctors.length}
          label="Doctors" color="var(--primary)"
          onClick={() => navigate('/doctors')} />
        <StatCard icon="🧑‍🤝‍🧑" value={patients.length}
          label="Patients" color="var(--accent)"
          onClick={() => navigate('/patients')} />
        <StatCard icon="📅" value={appts.length}
          label="Total Appointments" color="var(--success)"
          onClick={() => navigate('/appointments')} />
        <StatCard icon="⏳"
          value={appts.filter(a => a.status === APPOINTMENT_STATUS.PENDING).length}
          label="Pending" color="var(--warning)"
          onClick={() => navigate('/appointments')} />
      </div>

      {msg.text && (
        <div className={`alert alert-${msg.type === 'error' ? 'error' : 'success'}`}>
          {msg.text}
        </div>
      )}

      <div className="db-grid-split">
        {/* Doctor management panel */}
        <div className="card">
          <div className="db-section-header">
            <h3 className="db-section-title">🏥 Doctors</h3>
            <button className="btn btn-primary btn-sm" onClick={() => setModal('add')}>
              + Add Doctor
            </button>
          </div>
          <div className="db-doctor-list">
            {doctors.length === 0 ? (
              <div className="db-empty"><span>🏥</span><p>No doctors registered yet.</p></div>
            ) : doctors.map(d => (
              <div key={d.id} className="db-doctor-row">
                <div className="db-doctor-avatar">
                  {d.name?.charAt(0) ?? 'D'}
                </div>
                <div className="db-doctor-info">
                  <p className="db-name">{d.name}</p>
                  <p className="db-secondary">{d.specialization}</p>
                  <p className="db-secondary" style={{ fontSize: '0.72rem' }}>
                    🕐 {formatTime(d.availableFrom)} – {formatTime(d.availableTo)}
                  </p>
                </div>
                <button className="btn btn-danger btn-sm"
                  onClick={() => handleDeleteDoctor(d.id)}>
                  Remove
                </button>
              </div>
            ))}
          </div>
        </div>

        {/* Recent appointments */}
        <div className="card">
          <div className="db-section-header">
            <h3 className="db-section-title">🕒 Recent Appointments</h3>
            <button className="btn btn-ghost btn-sm" onClick={() => navigate('/appointments')}>
              View All →
            </button>
          </div>
          {recent.length === 0 ? (
            <div className="db-empty"><span>📭</span><p>No appointments yet.</p></div>
          ) : (
            <div className="db-appt-list">
              {recent.map(a => (
                <div key={a.id} className="db-appt-row">
                  <div className="db-appt-date">
                    <span className="db-appt-day">
                      {a.date ? new Date(a.date).toLocaleDateString('en',{ day:'2-digit' }) : '—'}
                    </span>
                    <span className="db-appt-mon">
                      {a.date ? new Date(a.date).toLocaleDateString('en',{ month:'short' }) : ''}
                    </span>
                  </div>
                  <div className="db-appt-info">
                    <p className="db-name">{a.patientName}</p>
                    <p className="db-secondary">Dr. {a.doctorName} · {formatTime(a.time)}</p>
                  </div>
                  <StatusBadge status={a.status} />
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      {/* Add doctor modal */}
      {modal === 'add' && (
        <div className="modal-overlay" onClick={() => setModal(null)}>
          <div className="modal" onClick={e => e.stopPropagation()}>
            <h3 className="modal-title">Add Doctor Profile</h3>
            <p className="db-secondary" style={{ marginBottom: '1rem' }}>
              The user account must already exist with role ROLE_DOCTOR.
            </p>
            <form onSubmit={handleAddDoctor}>
              <div className="form-group">
                <label className="form-label">User ID</label>
                <input className="form-input" type="number" placeholder="e.g. 3"
                  value={form.userId}
                  onChange={e => setForm(f => ({ ...f, userId: e.target.value }))} required />
              </div>
              <div className="form-group">
                <label className="form-label">Specialization</label>
                <input className="form-input" placeholder="e.g. Cardiology"
                  value={form.specialization}
                  onChange={e => setForm(f => ({ ...f, specialization: e.target.value }))} required />
              </div>
              <div className="grid-2">
                <div className="form-group">
                  <label className="form-label">Available From</label>
                  <input className="form-input" type="time"
                    value={form.availableFrom}
                    onChange={e => setForm(f => ({ ...f, availableFrom: e.target.value }))} required />
                </div>
                <div className="form-group">
                  <label className="form-label">Available To</label>
                  <input className="form-input" type="time"
                    value={form.availableTo}
                    onChange={e => setForm(f => ({ ...f, availableTo: e.target.value }))} required />
                </div>
              </div>
              <div className="modal-actions">
                <button type="button" className="btn btn-ghost" onClick={() => setModal(null)}>Cancel</button>
                <button type="submit" className="btn btn-primary" disabled={saving}>
                  {saving ? 'Creating…' : 'Create Profile'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </>
  );
};

/* ─────────────────────────────────────────────────────────────
   ROOT DASHBOARD — Conditional rendering by role
   ───────────────────────────────────────────────────────────── */
const Dashboard = () => {
  const { user } = useAuth();

  const greeting = () => {
    const h = new Date().getHours();
    if (h < 12) return 'Good morning';
    if (h < 17) return 'Good afternoon';
    return 'Good evening';
  };

  const roleLabel = {
    [ROLES.PATIENT]: 'Patient Portal',
    [ROLES.DOCTOR]:  'Doctor Portal',
    [ROLES.ADMIN]:   'Admin Control Panel',
  };

  return (
    <div className="page-content">
      {/* Header */}
      <div className="page-header">
        <div>
          <h2 className="page-title">
            {greeting()}, {user?.name?.split(' ')[0]} 👋
          </h2>
          <p className="db-portal-label">{roleLabel[user?.role]}</p>
        </div>
        <div className="db-role-badge">
          {user?.role?.replace('ROLE_', '')}
        </div>
      </div>

      {/* Role-based content */}
      {user?.role === ROLES.PATIENT && <PatientDashboard />}
      {user?.role === ROLES.DOCTOR  && <DoctorDashboard userId={user.userId} />}
      {user?.role === ROLES.ADMIN   && <AdminDashboard />}
    </div>
  );
};

export default Dashboard;
