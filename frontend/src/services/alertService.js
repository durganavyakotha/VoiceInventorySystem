import api from './api';

export const alertService = {
  list: () => api.get('/api/alerts'),
  markRead: (id) => api.patch(`/api/alerts/${id}/read`),
  markAllRead: () => api.post('/api/alerts/read-all'),
};
