import React, { useEffect, useState } from 'react';
import { patientService } from '../services/patientService';
import { useAuth }        from '../context/AuthContext';
import { displayRole }    from '../utils/helpers';

const ProfileField = ({ label, value }) => (
  <div style={{ padding: '0.85rem 0', borderBottom: '1px solid var(--border)' }}>
    <p style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', textTransform: 'uppercase', letterSpacing: '0.05em', marginBottom: '0.25rem' }}>
      {label}
    </p>
    <p style={{ fontWeight: 500 }}>{value ?? '—'}</p>
  </div>
);

const PatientProfile = () => {
  const { user }                     = useAuth();
  const [profile, setProfile]        = useState(null);
  const [editing, setEditing]        = useState(false);
  const [form,    setForm]           = useState({ age: '', medicalHistory: '' });
  const [loading, setLoading]        = useState(true);
  const [saving,  setSaving]         = useState(false);
  const [message, setMessage]        = useState({ type: '', text: '' });
  const [creating, setCreating]      = useState(false);

  const notify = (type, text) => {
    setMessage({ type, text });
    setTimeout(() => setMessage({ type: '', text: '' }), 4000);
  };

  useEffect(() => {
    const load = async () => {
      try {
        const res = await patientService.getMe();
        setProfile(res.data.data);
        setForm({
          age:           res.data.data.age ?? '',
          medicalHistory: res.data.data.medicalHistory ?? '',
        });
      } catch (err) {
        if (err.response?.status === 404) setCreating(true);
      } finally { setLoading(false); }
    };
    load();
  }, []);

  const handleCreate = async (e) => {
    e.preventDefault(); setSaving(true);
    try {
      const res = await patientService.create(user.userId, {
        age:            parseInt(form.age, 10),
        medicalHistory: form.medicalHistory,
      });
      setProfile(res.data.data);
      setCreating(false);
      notify('success', 'Profile created!');
    } catch (ex) {
      notify('error', ex.response?.data?.message || 'Create failed.');
    } finally { setSaving(false); }
  };

  const handleUpdate = async (e) => {
    e.preventDefault(); setSaving(true);
    try {
      const res = await patientService.update(profile.id, {
        age:            parseInt(form.age, 10),
        medicalHistory: form.medicalHistory,
      });
      setProfile(res.data.data);
      setEditing(false);
      notify('success', 'Profile updated!');
    } catch (ex) {
      notify('error', ex.response?.data?.message || 'Update failed.');
    } finally { setSaving(false); }
  };

  if (loading) return <div className="spinner-wrap"><div className="spinner" /></div>;

  return (
    <div className="page-content">
      <div className="page-header">
        <h2 className="page-title">My Profile</h2>
        {profile && !editing && (
          <button className="btn btn-primary" onClick={() => setEditing(true)}>
            ✏️ Edit Profile
          </button>
        )}
      </div>

      {message.text && (
        <div className={`alert alert-${message.type === 'error' ? 'error' : 'success'}`}>
          {message.text}
        </div>
      )}

      {/* User Account Card */}
      <div className="grid-2" style={{ marginBottom: '1.5rem' }}>
        <div className="card">
          <div style={{ display: 'flex', gap: '1rem', alignItems: 'center', marginBottom: '1rem' }}>
            <div style={{
              width: 64, height: 64,
              background: 'linear-gradient(135deg, var(--primary), var(--accent))',
              borderRadius: '50%',
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              fontSize: '1.75rem', fontWeight: 700, color: '#fff',
            }}>
              {user?.name?.charAt(0) ?? 'U'}
            </div>
            <div>
              <p style={{ fontWeight: 700, fontSize: '1.1rem' }}>{user?.name}</p>
              <p style={{ color: 'var(--accent)', fontSize: '0.8rem' }}>{displayRole(user?.role)}</p>
            </div>
          </div>
          <ProfileField label="Email"   value={user?.email} />
          <ProfileField label="Role"    value={displayRole(user?.role)} />
          <ProfileField label="User ID" value={`#${user?.userId}`} />
        </div>

        {/* Medical Profile Card */}
        <div className="card">
          <h3 style={{ fontWeight: 600, marginBottom: '1rem' }}>Medical Profile</h3>

          {creating ? (
            <form onSubmit={handleCreate}>
              <p className="alert alert-info" style={{ marginBottom: '1rem' }}>
                No medical profile found. Create one to book appointments.
              </p>
              <div className="form-group">
                <label className="form-label">Age</label>
                <input
                  className="form-input" type="number" min="0" max="150"
                  value={form.age}
                  onChange={e => setForm(f => ({ ...f, age: e.target.value }))}
                  required
                />
              </div>
              <div className="form-group">
                <label className="form-label">Medical History</label>
                <textarea
                  className="form-input" rows={4}
                  placeholder="Known conditions, allergies, medications…"
                  value={form.medicalHistory}
                  onChange={e => setForm(f => ({ ...f, medicalHistory: e.target.value }))}
                />
              </div>
              <button className="btn btn-primary" type="submit" disabled={saving}>
                {saving ? 'Creating…' : 'Create Profile'}
              </button>
            </form>
          ) : editing ? (
            <form onSubmit={handleUpdate}>
              <div className="form-group">
                <label className="form-label">Age</label>
                <input
                  className="form-input" type="number" min="0" max="150"
                  value={form.age}
                  onChange={e => setForm(f => ({ ...f, age: e.target.value }))}
                  required
                />
              </div>
              <div className="form-group">
                <label className="form-label">Medical History</label>
                <textarea
                  className="form-input" rows={4}
                  value={form.medicalHistory}
                  onChange={e => setForm(f => ({ ...f, medicalHistory: e.target.value }))}
                />
              </div>
              <div style={{ display: 'flex', gap: '0.75rem', marginTop: '0.5rem' }}>
                <button className="btn btn-primary" type="submit" disabled={saving}>
                  {saving ? 'Saving…' : 'Save Changes'}
                </button>
                <button className="btn btn-ghost" type="button" onClick={() => setEditing(false)}>
                  Cancel
                </button>
              </div>
            </form>
          ) : (
            <>
              <ProfileField label="Patient ID"      value={`#${profile?.id}`} />
              <ProfileField label="Age"             value={profile?.age ? `${profile.age} years` : null} />
              <ProfileField label="Medical History" value={profile?.medicalHistory} />
            </>
          )}
        </div>
      </div>
    </div>
  );
};

export default PatientProfile;
