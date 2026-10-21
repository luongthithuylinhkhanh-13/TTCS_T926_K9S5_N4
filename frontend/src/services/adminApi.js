import apiClient from './apiClient.js';

export const getAdminDashboardStats = async () => {
  const response = await apiClient.get('/api/admin/dashboard-stats');
  return response.data;
};

export const getAdminUsers = async (params = {}) => {
  const response = await apiClient.get('/api/admin/users', { params });
  return response.data;
};

export const getAdminRoles = async () => {
  const response = await apiClient.get('/api/admin/roles');
  return response.data;
};

export const approveUser = async (userId, roleName) => {
  const response = await apiClient.put(`/api/admin/users/${userId}/approve`, { roleName });
  return response.data;
};

export const rejectUser = async (userId, reason) => {
  const response = await apiClient.put(`/api/admin/users/${userId}/reject`, { reason });
  return response.data;
};

export const updateUserRole = async (userId, roleName) => {
  const response = await apiClient.put(`/api/admin/users/${userId}/role`, { roleName });
  return response.data;
};

export const updateUserStatus = async (userId, status) => {
  const response = await apiClient.put(`/api/admin/users/${userId}/status`, { status });
  return response.data;
};

export const getProjectManager = async (projectId) => {
  const response = await apiClient.get(`/api/admin/projects/${projectId}/manager`);
  return response.data;
};

export const assignProjectManager = async (projectId, userId) => {
  const response = await apiClient.post(`/api/admin/projects/${projectId}/manager`, { userId });
  return response.data;
};

