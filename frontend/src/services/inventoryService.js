import api from './api';

export const inventoryService = {
  list: (params = {}) => {
    const query = typeof params === 'string' ? { search: params } : params;
    return api.get('/api/inventory', { params: query });
  },
  lowStock: () => api.get('/api/inventory/low-stock'),
  add: (data) => api.post('/api/inventory', data),
  addWithImage: ({ productName, quantity, unit, category, threshold, barcode, costPerUnit, file }) => {
    const form = new FormData();
    form.append('productName', productName);
    form.append('quantity', quantity);
    if (unit) form.append('unit', unit);
    if (category) form.append('category', category);
    if (threshold != null) form.append('threshold', threshold);
    if (barcode) form.append('barcode', barcode);
    if (costPerUnit != null) form.append('costPerUnit', costPerUnit);
    form.append('file', file);
    return api.post('/api/inventory/with-image', form);
  },
  updateQuantity: (id, quantity) =>
    api.put(`/api/inventory/${id}/quantity`, null, { params: { quantity } }),
  setThreshold: (id, threshold) =>
    api.put(`/api/inventory/${id}/threshold`, null, { params: { threshold } }),
  removeStock: (id, quantity) =>
    api.post(`/api/inventory/${id}/remove`, null, { params: { quantity } }),
  delete: (id) => api.delete(`/api/inventory/${id}`),
  voiceCommand: (command, language, previewOnly = false) =>
    api.post('/api/inventory/voice-command', { command, language, previewOnly }),
  barcodeWithImage: ({ barcode, productName, quantity, unit, category, file }) => {
    const form = new FormData();
    form.append('barcode', barcode);
    if (productName) form.append('productName', productName);
    if (quantity != null) form.append('quantity', quantity);
    if (unit) form.append('unit', unit);
    if (category) form.append('category', category);
    form.append('file', file);
    return api.post('/api/inventory/barcode', form);
  },
  uploadImage: (id, file) => {
    const form = new FormData();
    form.append('file', file);
    return api.post(`/api/inventory/${id}/image`, form);
  },
};
