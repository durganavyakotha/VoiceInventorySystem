import api from './api';

export const contactService = {
  submit: (data) => api.post('/api/contact', data),
};
