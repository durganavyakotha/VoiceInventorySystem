import api from './api';

export const orderService = {
  create: (data) => api.post('/api/orders', data),
  voiceBook: (vendorId, command) =>
    api.post('/api/orders/voice-book', { vendorId, command }),
  voiceWithdraw: (vendorId, command) =>
    api.post('/api/orders/voice-withdraw', { vendorId, command }),
  offer: (data) => api.post('/api/orders/offer', data),
  list: () => api.get('/api/orders'),
  accept: (id) => api.post(`/api/orders/${id}/accept`),
  reject: (id) => api.post(`/api/orders/${id}/reject`),
  cancel: (id) => api.post(`/api/orders/${id}/cancel`),
  withdraw: (id) => api.post(`/api/orders/${id}/withdraw`),
  updateStatus: (id, status) =>
    api.patch(`/api/orders/${id}/status`, null, { params: { status } }),
};
