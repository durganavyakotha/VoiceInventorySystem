import api from './api';

export const inventoryService = {
  list: (search) => api.get('/api/inventory', { params: search ? { search } : {} }),
  lowStock: () => api.get('/api/inventory/low-stock'),
  add: (data) => api.post('/api/inventory', data),
  updateQuantity: (id, quantity) =>
    api.put(`/api/inventory/${id}/quantity`, null, { params: { quantity } }),
  setThreshold: (id, threshold) =>
    api.put(`/api/inventory/${id}/threshold`, null, { params: { threshold } }),
  removeStock: (id, quantity) =>
    api.post(`/api/inventory/${id}/remove`, null, { params: { quantity } }),
  delete: (id) => api.delete(`/api/inventory/${id}`),
  voiceCommand: (command, language) =>
    api.post('/api/inventory/voice-command', { command, language }),
  barcode: (barcode, productName, quantity) =>
    api.post('/api/inventory/barcode', null, {
      params: { barcode, productName, quantity },
    }),
  uploadImage: (id, file) => {
    const form = new FormData();
    form.append('file', file);
    return api.post(`/api/inventory/${id}/image`, form);
  },
};
