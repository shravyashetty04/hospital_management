import { TOKEN_KEY, REFRESH_TOKEN_KEY, USER_KEY, STATUS_COLORS } from './constants';

// ── Token helpers ─────────────────────────────────────────────────────────────
export const getToken        = () => localStorage.getItem(TOKEN_KEY);
export const getRefreshToken = () => localStorage.getItem(REFRESH_TOKEN_KEY);
export const getStoredUser   = () => {
  try { return JSON.parse(localStorage.getItem(USER_KEY)); }
  catch { return null; }
};

export const saveAuthData = ({ accessToken, refreshToken, ...user }) => {
  localStorage.setItem(TOKEN_KEY, accessToken);
  localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken);
  localStorage.setItem(USER_KEY, JSON.stringify(user));
};

export const clearAuthData = () => {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(REFRESH_TOKEN_KEY);
  localStorage.removeItem(USER_KEY);
};

// ── Date / time helpers ───────────────────────────────────────────────────────
export const formatDate = (dateStr) => {
  if (!dateStr) return '—';
  const [y, m, d] = dateStr.split('-');
  return `${d}/${m}/${y}`;
};

export const formatTime = (timeStr) => {
  if (!timeStr) return '—';
  const [h, m] = timeStr.split(':');
  const hour = parseInt(h, 10);
  const ampm = hour >= 12 ? 'PM' : 'AM';
  const displayHour = hour % 12 || 12;
  return `${displayHour}:${m} ${ampm}`;
};

// ── Status badge ──────────────────────────────────────────────────────────────
export const getStatusColor = (status) =>
  STATUS_COLORS[status] || '#6b7280';

// ── Role display ──────────────────────────────────────────────────────────────
export const displayRole = (role) =>
  role?.replace('ROLE_', '') ?? '—';

// ── Capitalize ────────────────────────────────────────────────────────────────
export const capitalize = (str) =>
  str ? str.charAt(0).toUpperCase() + str.slice(1).toLowerCase() : '';
