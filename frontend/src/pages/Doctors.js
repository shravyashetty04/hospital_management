import React, { useEffect, useState, useCallback } from 'react';
import { doctorService }       from '../services/doctorService';
import { appointmentService }  from '../services/appointmentService';
import { useAuth }             from '../context/AuthContext';
import { ROLES }               from '../utils/constants';
import { formatTime }          from '../utils/helpers';

const BookModal = ({ doctor, onClose, onBooked }) => {
  const [form, setForm]   = useState({ date: '', time: '' });
  const [busy, setBusy]   = useState(false);
  const [err,  setErr]    = useState('');

  const submit = async (e) => {
    e.preventDefault(); setBusy(true); setErr('');
    try {
      await appointmentService.book({
        doctorId: doctor.id,
        date:     form.date,
        time:     form.time,
      });
      onBooked();
    } catch (ex) {
      setErr(ex.response?.data?.message || 'Booking failed. Try a different slot.');
    } finally { setBusy(false); }
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal" onClick={e => e.stopPropagation()}>
        <h3 className="modal-title">Book with Dr. {doctor.name}</h3>
        <p style={{ color: 'var(--accent)', fontSize: '0.8rem', marginBottom: '1rem' }}>
          {doctor.specialization} · Available {formatTime(doctor.availableFrom)} – {formatTime(doctor.availableTo)}
        </p>
        {err && <div className="alert alert-error">{err}</div>}
        <form onSubmit={submit}>
          <div className="form-group">
            <label className="form-label">Date</label>
            <input
              type="date"
              className="form-input"
              value={form.date}
              min={new Date().toISOString().split('T')[0]}
              onChange={e => setForm(f => ({ ...f, date: e.target.value }))}
              required
            />
          </div>
          <div className="form-group">
            <label className="form-label">Time (within doctor's hours)</label>
            <input
              type="time"
              className="form-input"
              value={form.time}
              onChange={e => setForm(f => ({ ...f, time: e.target.value }))}
              required
            />
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

const Doctors = () => {
  const { user }                     = useAuth();
  const [doctors,  setDoctors]       = useState([]);
  const [filtered, setFiltered]      = useState([]);
  const [search,   setSearch]        = useState('');
  const [spec,     setSpec]          = useState('');
  const [loading,  setLoading]       = useState(true);
  const [booking,  setBooking]       = useState(null);
  const [success,  setSuccess]       = useState('');

  const loadDoctors = useCallback(async () => {
    setLoading(true);
    try {
      const res = await doctorService.getAll();
      const list = res.data.data ?? [];
      setDoctors(list); setFiltered(list);
    } catch { /* ignore */ }
    finally { setLoading(false); }
  }, []);

  useEffect(() => { loadDoctors(); }, [loadDoctors]);

  useEffect(() => {
    let list = doctors;
    if (search) list = list.filter(d =>
      d.name.toLowerCase().includes(search.toLowerCase())
    );
    if (spec)   list = list.filter(d =>
      d.specialization.toLowerCase().includes(spec.toLowerCase())
    );
    setFiltered(list);
  }, [search, spec, doctors]);

  const handleBooked = () => {
    setBooking(null);
    setSuccess('Appointment booked successfully!');
    setTimeout(() => setSuccess(''), 4000);
  };

  const specializations = [...new Set(doctors.map(d => d.specialization))].sort();

  return (
    <div className="page-content">
      <div className="page-header">
        <h2 className="page-title">Find a Doctor</h2>
      </div>

      {success && <div className="alert alert-success">{success}</div>}

      {/* Filters */}
      <div className="card" style={{ marginBottom: '1.5rem' }}>
        <div className="grid-2">
          <div className="form-group" style={{ marginBottom: 0 }}>
            <label className="form-label">Search by Name</label>
            <input
              className="form-input"
              placeholder="Dr. Smith…"
              value={search}
              onChange={e => setSearch(e.target.value)}
            />
          </div>
          <div className="form-group" style={{ marginBottom: 0 }}>
            <label className="form-label">Specialization</label>
            <select
              className="form-input"
              value={spec}
              onChange={e => setSpec(e.target.value)}
            >
              <option value="">All Specializations</option>
              {specializations.map(s => (
                <option key={s} value={s}>{s}</option>
              ))}
            </select>
          </div>
        </div>
      </div>

      {loading ? (
        <div className="spinner-wrap"><div className="spinner" /></div>
      ) : filtered.length === 0 ? (
        <div className="card" style={{ textAlign: 'center', color: 'var(--text-secondary)' }}>
          No doctors found.
        </div>
      ) : (
        <div className="grid-3">
          {filtered.map(doctor => (
            <div key={doctor.id} className="doctor-card">
              <div style={{ display: 'flex', gap: '1rem', alignItems: 'center' }}>
                <div className="doctor-avatar">
                  {doctor.name?.charAt(0) ?? 'D'}
                </div>
                <div>
                  <p className="doctor-name">{doctor.name}</p>
                  <p className="doctor-spec">{doctor.specialization}</p>
                </div>
              </div>
              <p className="doctor-hours">
                🕐 {formatTime(doctor.availableFrom)} – {formatTime(doctor.availableTo)}
              </p>
              <p style={{ fontSize: '0.8rem', color: 'var(--text-secondary)' }}>
                📧 {doctor.email}
              </p>
              {user?.role === ROLES.PATIENT && (
                <button
                  className="btn btn-primary btn-sm"
                  style={{ marginTop: 'auto' }}
                  onClick={() => setBooking(doctor)}
                >
                  Book Appointment
                </button>
              )}
            </div>
          ))}
        </div>
      )}

      {booking && (
        <BookModal
          doctor={booking}
          onClose={() => setBooking(null)}
          onBooked={handleBooked}
        />
      )}
    </div>
  );
};

export default Doctors;
