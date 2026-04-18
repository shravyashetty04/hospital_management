import React, { useEffect, useState, useCallback } from 'react';
import { patientService }      from '../services/patientService';
import { appointmentService }  from '../services/appointmentService';
import { STATUS_COLORS } from '../utils/constants';
import { formatDate, formatTime } from '../utils/helpers';

const Patients = () => {
  const [patients,  setPatients]  = useState([]);
  const [selected,  setSelected]  = useState(null);   // patient being viewed
  const [history,   setHistory]   = useState([]);
  const [loading,   setLoading]   = useState(true);
  const [histLoad,  setHistLoad]  = useState(false);
  const [search,    setSearch]    = useState('');
  const [message,   setMessage]   = useState({ type: '', text: '' });

  const notify = (type, text) => {
    setMessage({ type, text });
    setTimeout(() => setMessage({ type: '', text: '' }), 4000);
  };

  const loadPatients = useCallback(async () => {
    setLoading(true);
    try {
      const res = await patientService.getAll();
      setPatients(res.data.data ?? []);
    } catch {
      notify('error', 'Failed to load patients.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { loadPatients(); }, [loadPatients]);

  const viewHistory = async (patient) => {
    setSelected(patient);
    setHistLoad(true);
    try {
      const res = await appointmentService.getByPatient(patient.id);
      setHistory(res.data.data ?? []);
    } catch {
      setHistory([]);
    } finally {
      setHistLoad(false); }
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Delete this patient profile? This cannot be undone.')) return;
    try {
      await patientService.delete(id);
      notify('success', 'Patient deleted.');
      if (selected?.id === id) setSelected(null);
      loadPatients();
    } catch (ex) {
      notify('error', ex.response?.data?.message || 'Delete failed.');
    }
  };

  const filtered = patients.filter(p =>
    p.name?.toLowerCase().includes(search.toLowerCase()) ||
    p.email?.toLowerCase().includes(search.toLowerCase())
  );

  return (
    <div className="page-content">
      <div className="page-header">
        <h2 className="page-title">Patients</h2>
        <span style={{ color: 'var(--text-secondary)', fontSize: '0.875rem' }}>
          {patients.length} registered
        </span>
      </div>

      {message.text && (
        <div className={`alert alert-${message.type === 'error' ? 'error' : 'success'}`}>
          {message.text}
        </div>
      )}

      <div style={{ display: 'flex', gap: '1.5rem', alignItems: 'flex-start' }}>

        {/* ── Patient list panel ──────────────────────────────────── */}
        <div style={{ flex: 1, minWidth: 0 }}>
          <div className="form-group" style={{ marginBottom: '1rem' }}>
            <input
              className="form-input"
              placeholder="🔍  Search by name or email…"
              value={search}
              onChange={e => setSearch(e.target.value)}
            />
          </div>

          {loading ? (
            <div className="spinner-wrap"><div className="spinner" /></div>
          ) : (
            <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
              <div className="table-wrap">
                <table>
                  <thead>
                    <tr>
                      <th>#</th>
                      <th>Name</th>
                      <th>Email</th>
                      <th>Age</th>
                      <th>Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {filtered.length === 0 ? (
                      <tr>
                        <td colSpan={5} style={{ textAlign: 'center', color: 'var(--text-secondary)', padding: '2rem' }}>
                          No patients found.
                        </td>
                      </tr>
                    ) : filtered.map(p => (
                      <tr
                        key={p.id}
                        style={{
                          background: selected?.id === p.id
                            ? 'rgba(99,102,241,0.08)' : undefined,
                          cursor: 'pointer',
                        }}
                        onClick={() => viewHistory(p)}
                      >
                        <td style={{ color: 'var(--text-muted)' }}>#{p.id}</td>
                        <td>
                          <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem' }}>
                            <div style={{
                              width: 32, height: 32,
                              background: 'linear-gradient(135deg, var(--primary), var(--accent))',
                              borderRadius: '50%',
                              display: 'flex', alignItems: 'center', justifyContent: 'center',
                              fontSize: '0.8rem', fontWeight: 700, color: '#fff', flexShrink: 0,
                            }}>
                              {p.name?.charAt(0) ?? 'P'}
                            </div>
                            <span style={{ fontWeight: 500 }}>{p.name}</span>
                          </div>
                        </td>
                        <td style={{ color: 'var(--text-secondary)', fontSize: '0.85rem' }}>{p.email}</td>
                        <td>{p.age ? `${p.age} yrs` : '—'}</td>
                        <td onClick={e => e.stopPropagation()}>
                          <button
                            className="btn btn-danger btn-sm"
                            onClick={() => handleDelete(p.id)}
                          >
                            Delete
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}
        </div>

        {/* ── Patient detail panel ────────────────────────────────── */}
        {selected && (
          <div className="card" style={{ width: 340, flexShrink: 0 }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '1rem' }}>
              <div>
                <p style={{ fontWeight: 700, fontSize: '1rem' }}>{selected.name}</p>
                <p style={{ color: 'var(--text-secondary)', fontSize: '0.8rem' }}>{selected.email}</p>
              </div>
              <button className="btn btn-ghost btn-sm" onClick={() => setSelected(null)}>✕</button>
            </div>

            <div style={{ padding: '0.6rem 0', borderBottom: '1px solid var(--border)', marginBottom: '0.5rem' }}>
              <p style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
                Medical History
              </p>
              <p style={{ fontSize: '0.875rem', marginTop: '0.25rem', lineHeight: 1.6 }}>
                {selected.medicalHistory || 'No history recorded.'}
              </p>
            </div>

            <h4 style={{ fontWeight: 600, fontSize: '0.875rem', margin: '1rem 0 0.75rem' }}>
              Appointment History
            </h4>
            {histLoad ? (
              <div className="spinner-wrap" style={{ padding: '1rem' }}>
                <div className="spinner" style={{ width: 24, height: 24 }} />
              </div>
            ) : history.length === 0 ? (
              <p style={{ color: 'var(--text-secondary)', fontSize: '0.875rem' }}>No appointments.</p>
            ) : (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem', maxHeight: 320, overflowY: 'auto' }}>
                {history.map(a => (
                  <div
                    key={a.id}
                    style={{
                      background: 'var(--bg-700)',
                      borderRadius: 'var(--radius-sm)',
                      padding: '0.65rem 0.85rem',
                      border: '1px solid var(--border)',
                    }}
                  >
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.25rem' }}>
                      <span style={{ fontSize: '0.8rem', fontWeight: 500 }}>
                        {formatDate(a.date)} · {formatTime(a.time)}
                      </span>
                      <span
                        className="badge"
                        style={{
                          background: `${STATUS_COLORS[a.status]}22`,
                          color: STATUS_COLORS[a.status],
                        }}
                      >
                        {a.status}
                      </span>
                    </div>
                    <p style={{ fontSize: '0.75rem', color: 'var(--text-secondary)' }}>
                      Dr. {a.doctorName} · {a.specialization}
                    </p>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
};

export default Patients;
