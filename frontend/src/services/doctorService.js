import api from './api';

export const doctorService = {
  getAll:              ()             => api.get('/doctors'),
  getById:             (id)           => api.get(`/doctors/${id}`),
  getByUserId:         (userId)       => api.get(`/doctors/user/${userId}`),
  getAvailableAt:      (time)         => api.get('/doctors/available', { params: { time } }),
  getBySpecialization: (spec)         => api.get(`/doctors/specialization/${spec}`),
  search:              (keyword)      => api.get('/doctors/search', { params: { keyword } }),
  create:              (userId, data) => api.post(`/doctors/user/${userId}`, data),
  update:              (id, data)     => api.put(`/doctors/${id}`, data),
  delete:              (id)           => api.delete(`/doctors/${id}`),
};
