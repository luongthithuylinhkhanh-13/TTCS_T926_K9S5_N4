import apiClient from './apiClient.js';

export const getSystemHealth = async () => {
  const response = await apiClient.get('/api/admin/system-health');
  return response.data;
};
