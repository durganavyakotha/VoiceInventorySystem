import api from './api';

export const chatbotService = {
  open: () => api.post('/api/chatbot/open'),
  ask: (message) => api.post('/api/chatbot/ask', { message }),
};
