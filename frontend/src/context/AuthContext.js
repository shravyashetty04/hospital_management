import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { authService }              from '../services/authService';
import { saveAuthData, clearAuthData, getStoredUser, getToken } from '../utils/helpers';

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
  const [user,    setUser]    = useState(getStoredUser);
  const [loading, setLoading] = useState(false);
  const [error,   setError]   = useState(null);

  const isAuthenticated = !!user && !!getToken();

  const login = useCallback(async (email, password) => {
    setLoading(true); setError(null);
    try {
      const { data: res } = await authService.login({ email, password });
      const payload = res.data;
      saveAuthData(payload);
      setUser({
        userId:        payload.userId,
        name:          payload.name,
        email:         payload.email,
        role:          payload.role,
        accessToken:   payload.accessToken,
        refreshToken:  payload.refreshToken,
      });
      return payload;
    } catch (err) {
      console.error('[AUTH] Login error:', err);
      const serverMsg = err.response?.data?.message;
      const validationErrs = err.response?.data?.errors;
      const msg = validationErrs?.join(', ') || serverMsg || 'Login failed. Check your credentials.';
      setError(msg);
      throw new Error(msg);
    } finally {
      setLoading(false);
    }
  }, []);

  const register = useCallback(async (formData) => {
    setLoading(true); setError(null);
    try {
      const { data: res } = await authService.register(formData);
      console.log('[AUTH] Register raw response:', res);
      const payload = res.data;
      saveAuthData(payload);
      setUser({
        userId:       payload.userId,
        name:         payload.name,
        email:        payload.email,
        role:         payload.role,
        accessToken:  payload.accessToken,
        refreshToken: payload.refreshToken,
      });
      return payload;
    } catch (err) {
      console.error('[AUTH] Register error:', err);
      const serverMsg = err.response?.data?.message;
      const validationErrs = err.response?.data?.errors;
      const msg = validationErrs?.join(', ') || serverMsg || 'Registration failed. Please try again.';
      setError(msg);
      throw new Error(msg);
    } finally {
      setLoading(false);
    }
  }, []);


  const logout = useCallback(() => {
    clearAuthData();
    setUser(null);
    setError(null);
  }, []);

  const clearError = useCallback(() => setError(null), []);

  // Sync user state if storage changes (multi-tab)
  useEffect(() => {
    const onStorage = (e) => {
      if (e.key === 'hms_user') setUser(getStoredUser());
    };
    window.addEventListener('storage', onStorage);
    return () => window.removeEventListener('storage', onStorage);
  }, []);

  return (
    <AuthContext.Provider value={{
      user, loading, error, isAuthenticated,
      login, register, logout, clearError,
    }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used inside AuthProvider');
  return ctx;
};
