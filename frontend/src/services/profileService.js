import api from './api';

export const profileService = {
  get: () => api.get('/api/profile'),
  update: (data) => api.put('/api/profile', data),
  uploadImage: (file) => {
    const form = new FormData();
    form.append('file', file);
    return api.post('/api/profile/image', form);
  },
};
