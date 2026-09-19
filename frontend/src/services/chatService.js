import api from './api';

export const chatService = {
  listConversations: () => api.get('/api/chat/conversations'),
  getOrCreate: (otherUserId) =>
    api.post('/api/chat/conversations', null, { params: { otherUserId } }),
  getMessages: (id) => api.get(`/api/chat/conversations/${id}/messages`),
  sendText: (data) => api.post('/api/chat/messages', data),
  sendVoice: (data) => api.post('/api/chat/messages/voice', data),
};
