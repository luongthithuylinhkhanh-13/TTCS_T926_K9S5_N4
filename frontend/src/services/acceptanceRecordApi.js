import apiClient from './apiClient';

const endpoint = projectId => `/api/projects/${projectId}/acceptance-records`;

export const getAcceptanceRecords = async projectId => {
  const response = await apiClient.get(endpoint(projectId));
  return response.data;
};

export const getAcceptanceRecord = async (projectId, recordId) => {
  const response = await apiClient.get(`${endpoint(projectId)}/${recordId}`);
  return response.data;
};

export const getAcceptanceRecordHistory = async (projectId, recordId) => {
  const response = await apiClient.get(`${endpoint(projectId)}/${recordId}/history`);
  return response.data;
};

export const createAcceptanceRecord = async (projectId, payload) => {
  const response = await apiClient.post(endpoint(projectId), payload);
  return response.data;
};

export const updateAcceptanceRecord = async (projectId, recordId, payload) => {
  const response = await apiClient.put(`${endpoint(projectId)}/${recordId}`, payload);
  return response.data;
};

export const approveAcceptanceRecord = async (projectId, recordId) => {
  const response = await apiClient.post(`${endpoint(projectId)}/${recordId}/approve`);
  return response.data;
};

export const returnAcceptanceRecord = async (projectId, recordId, comment) => {
  const response = await apiClient.post(`${endpoint(projectId)}/${recordId}/return`, { comment });
  return response.data;
};
