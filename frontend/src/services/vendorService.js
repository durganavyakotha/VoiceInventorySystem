import api from './api';

export const vendorService = {
  nearby: (params) => api.get('/api/vendors/nearby', { params }),
};
