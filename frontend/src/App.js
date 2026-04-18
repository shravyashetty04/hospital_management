import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import PrivateRoute  from './components/PrivateRoute';
import Sidebar       from './components/Sidebar';

/* Pages */
import Login          from './pages/Login';
import Register       from './pages/Register';
import Dashboard      from './pages/Dashboard';
import Doctors        from './pages/Doctors';
import Appointments   from './pages/Appointments';
import PatientProfile from './pages/PatientProfile';
import Patients          from './pages/Patients';
import BookAppointment   from './pages/BookAppointment';

import { ROLES } from './utils/constants';
import './App.css';

/* ── Unauthorised page ───────────────────────────────────────── */
const Unauthorized = () => (
  <div style={{ display:'flex', flexDirection:'column', alignItems:'center',
                justifyContent:'center', minHeight:'100vh', gap:'1rem' }}>
    <span style={{ fontSize:'4rem' }}>🚫</span>
    <h2 style={{ fontSize:'1.5rem', fontWeight:700 }}>Access Denied</h2>
    <p style={{ color:'var(--text-secondary)' }}>
      You don't have permission to view this page.
    </p>
    <a href="/dashboard" className="btn btn-primary">Go to Dashboard</a>
  </div>
);

/* ── Authenticated shell (sidebar + outlet) ──────────────────── */
const AppShell = ({ children }) => (
  <div className="app-shell">
    <Sidebar />
    <main className="app-main">{children}</main>
  </div>
);

/* ── Root redirect based on auth state ───────────────────────── */
const RootRedirect = () => {
  const { isAuthenticated } = useAuth();
  return <Navigate to={isAuthenticated ? '/dashboard' : '/login'} replace />;
};

/* ── App ─────────────────────────────────────────────────────── */
const App = () => (
  <BrowserRouter>
    <AuthProvider>
      <Routes>
        {/* Public routes */}
        <Route path="/login"    element={<Login />}    />
        <Route path="/register" element={<Register />} />
        <Route path="/unauthorized" element={<Unauthorized />} />

        {/* Root redirect */}
        <Route path="/" element={<RootRedirect />} />

        {/* Protected routes */}
        <Route path="/dashboard" element={
          <PrivateRoute>
            <AppShell><Dashboard /></AppShell>
          </PrivateRoute>
        }/>

        <Route path="/doctors" element={
          <PrivateRoute>
            <AppShell><Doctors /></AppShell>
          </PrivateRoute>
        }/>

        <Route path="/appointments" element={
          <PrivateRoute>
            <AppShell><Appointments /></AppShell>
          </PrivateRoute>
        }/>

        <Route path="/book" element={
          <PrivateRoute roles={[ROLES.PATIENT]}>
            <AppShell><BookAppointment /></AppShell>
          </PrivateRoute>
        }/>

        <Route path="/patients" element={
          <PrivateRoute roles={[ROLES.ADMIN]}>
            <AppShell><Patients /></AppShell>
          </PrivateRoute>
        }/>

        <Route path="/profile" element={
          <PrivateRoute roles={[ROLES.PATIENT, ROLES.DOCTOR]}>
            <AppShell><PatientProfile /></AppShell>
          </PrivateRoute>
        }/>

        {/* 404 */}
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </AuthProvider>
  </BrowserRouter>
);

export default App;
