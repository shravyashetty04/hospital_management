import api from './api';

export const patientService = {
  getMe:        ()             => api.get('/patients/me'),
  getHistory:   ()             => api.get('/patients/me/history'),
  getById:      (id)           => api.get(`/patients/${id}`),
  getByUserId:  (userId)       => api.get(`/patients/user/${userId}`),
  getAll:       ()             => api.get('/patients'),
  create:       (userId, data) => api.post(`/patients/user/${userId}`, data),
  update:       (id, data)     => api.put(`/patients/${id}`, data),
  delete:       (id)           => api.delete(`/patients/${id}`),
};
