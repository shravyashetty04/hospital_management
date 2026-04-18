// Direct backend URL — Spring Boot CORS config allows localhost:3000
export const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080/api/v1';

export const ROLES = {
  ADMIN:   'ROLE_ADMIN',
  DOCTOR:  'ROLE_DOCTOR',
  PATIENT: 'ROLE_PATIENT',
};

export const APPOINTMENT_STATUS = {
  PENDING:   'PENDING',
  CONFIRMED: 'CONFIRMED',
  COMPLETED: 'COMPLETED',
  CANCELLED: 'CANCELLED',
  NO_SHOW:   'NO_SHOW',
};

export const STATUS_COLORS = {
  PENDING:   '#f59e0b',
  CONFIRMED: '#3b82f6',
  COMPLETED: '#10b981',
  CANCELLED: '#ef4444',
  NO_SHOW:   '#6b7280',
};

export const TOKEN_KEY         = 'hms_access_token';
export const REFRESH_TOKEN_KEY = 'hms_refresh_token';
export const USER_KEY          = 'hms_user';
