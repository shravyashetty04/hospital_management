import React from 'react';
import { NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { displayRole } from '../utils/helpers';
import { ROLES } from '../utils/constants';
import './Sidebar.css';

const NAV_ITEMS = {
  [ROLES.PATIENT]: [
    { to: '/dashboard',    icon: '⊞',  label: 'Dashboard'        },
    { to: '/book',         icon: '➕',  label: 'Book Appointment' },
    { to: '/doctors',      icon: '👨‍⚕️', label: 'Find Doctors'     },
    { to: '/appointments', icon: '📅',  label: 'My Appointments'  },
    { to: '/profile',      icon: '👤',  label: 'My Profile'       },
  ],
  [ROLES.DOCTOR]: [
    { to: '/dashboard',    icon: '⊞',  label: 'Dashboard'   },
    { to: '/appointments', icon: '📅',  label: 'My Schedule' },
    { to: '/doctors',      icon: '👨‍⚕️', label: 'Doctors'     },
  ],
  [ROLES.ADMIN]: [
    { to: '/dashboard',    icon: '⊞', label: 'Dashboard'    },
    { to: '/doctors',      icon: '🏥', label: 'Doctors'      },
    { to: '/patients',     icon: '🧑‍🤝‍🧑', label: 'Patients'   },
    { to: '/appointments', icon: '📅', label: 'Appointments' },
  ],
};

const Sidebar = () => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const items = NAV_ITEMS[user?.role] ?? [];

  const handleLogout = () => { logout(); navigate('/login'); };

  return (
    <aside className="sidebar">
      <div className="sidebar-logo">
        <span className="sidebar-logo-icon">🏨</span>
        <span className="sidebar-logo-text">MediCare</span>
      </div>

      <div className="sidebar-user">
        <div className="sidebar-avatar">
          {user?.name?.charAt(0)?.toUpperCase() ?? 'U'}
        </div>
        <div className="sidebar-user-info">
          <span className="sidebar-user-name">{user?.name}</span>
          <span className="sidebar-user-role">{displayRole(user?.role)}</span>
        </div>
      </div>

      <nav className="sidebar-nav">
        {items.map(({ to, icon, label }) => (
          <NavLink
            key={to}
            to={to}
            className={({ isActive }) =>
              `sidebar-link${isActive ? ' sidebar-link--active' : ''}`
            }
          >
            <span className="sidebar-link-icon">{icon}</span>
            <span>{label}</span>
          </NavLink>
        ))}
      </nav>

      <button className="sidebar-logout" onClick={handleLogout}>
        <span>🚪</span>
        <span>Logout</span>
      </button>
    </aside>
  );
};

export default Sidebar;
