import apiClient from './apiClient';

export const getPendingPaymentRequests = projectId => apiClient
  .get(`/api/projects/${projectId}/payment-requests`)
  .then(response => response.data);

export const getMyPaymentRequests = projectId => apiClient
  .get(`/api/projects/${projectId}/payment-requests/my-requests`)
  .then(response => response.data);

export const submitPaymentRequest = (projectId, request) => apiClient
  .post(`/api/projects/${projectId}/payment-requests`, request)
  .then(response => response.data);

export const approvePaymentRequest = (projectId, requestId) => apiClient
  .post(`/api/projects/${projectId}/payment-requests/${requestId}/approve`)
  .then(response => response.data);

export const rejectPaymentRequest = (projectId, requestId, reason) => apiClient
  .post(`/api/projects/${projectId}/payment-requests/${requestId}/reject`, { reason })
  .then(response => response.data);