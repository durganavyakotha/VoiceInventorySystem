import api from './api';

export const vendorService = {
  listAll: () => api.get('/api/vendors'),
  getItems: (id) => api.get(`/api/vendors/${id}/items`),
  nearby: (params) => api.get('/api/vendors/nearby', { params }),
};
