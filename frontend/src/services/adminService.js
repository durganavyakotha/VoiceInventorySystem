import api from './api';

export const adminService = {
  getVendors: (params = {}) => api.get('/api/admin/vendors', { params }),
  getShopkeepers: (params = {}) => api.get('/api/admin/shopkeepers', { params }),
  getCounts: () => api.get('/api/admin/counts'),
  getLocationStats: (location) => api.get(`/api/admin/location/${encodeURIComponent(location)}`),
  getLocations: () => api.get('/api/admin/locations'),
  updateUserStatus: (id, status) =>
    api.patch(`/api/admin/users/${id}/status`, null, { params: { status } }),
  getQueries: () => api.get('/api/admin/queries'),
  updateQuery: (id, status) => api.put(`/api/admin/queries/${id}`, { status }),
};
