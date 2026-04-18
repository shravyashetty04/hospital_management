import api from './api';

export const appointmentService = {
  book:               (data)               => api.post('/appointments', data),
  cancel:             (id)                 => api.delete(`/appointments/${id}/cancel`),
  getById:            (id)                 => api.get(`/appointments/${id}`),
  getMy:              ()                   => api.get('/appointments/my'),
  getAll:             ()                   => api.get('/appointments'),
  getByDoctor:        (doctorId)           => api.get(`/appointments/doctor/${doctorId}`),
  getByDoctorDate:    (doctorId, date)     => api.get(`/appointments/doctor/${doctorId}/date`, { params: { date } }),
  getByDoctorStatus:  (doctorId, status)   => api.get(`/appointments/doctor/${doctorId}/status`, { params: { status } }),
  getByPatient:       (patientId)          => api.get(`/appointments/patient/${patientId}`),
  getByPatientStatus: (patientId, status)  => api.get(`/appointments/patient/${patientId}/status`, { params: { status } }),
  reschedule:         (id, data)           => api.put(`/appointments/${id}/reschedule`, data),
  updateStatus:       (id, status)         => api.patch(`/appointments/${id}/status`, null, { params: { status } }),
};
