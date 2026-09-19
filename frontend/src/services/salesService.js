import api from './api';

export const salesService = {
  list: () => api.get('/api/sales'),
  record: (productName, quantity) =>
    api.post('/api/sales', null, { params: { productName, quantity } }),
  history: (params = {}) => api.get('/api/sales/history', { params }),
  highestSelling: (params = {}) => api.get('/api/sales/highest-selling', { params }),
};
