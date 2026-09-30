import apiClient from './apiClient';

export const getProjects = () => {
  return apiClient.get('/api/projects').then(response => response.data);
};

export const getProjectWbs = (projectId) => {
  return apiClient.get(`/api/projects/${projectId}/wbs`).then(response => response.data);
};

export const getProjectSchedule = projectId => {
  return apiClient.get(`/api/projects/${projectId}/schedule`).then(response => response.data);
};

export const getCriticalPathProgress = projectId => {
  return apiClient.get(`/api/projects/${projectId}/critical-path`).then(response => response.data);
};

export const login = credentials => {
  return apiClient.post('/api/auth/login', credentials).then(response => response.data);
};

export const register = registration => {
  return apiClient.post('/api/auth/register', registration).then(response => response.data);
};

export const logout = () => {
  return apiClient.post('/api/auth/logout').then(response => response.data);
};

export const createWbsItem = (projectId, item) => {
  return apiClient.post(`/api/projects/${projectId}/tasks`, item).then(response => response.data);
};

export const updateWbsItem = (projectId, itemId, item) => {
  return apiClient.put(`/api/projects/${projectId}/tasks/${itemId}`, item).then(response => response.data);
};

export const deleteWbsItem = (projectId, itemId) => {
  return apiClient.delete(`/api/projects/${projectId}/wbs/${itemId}`).then(response => response.data);
};