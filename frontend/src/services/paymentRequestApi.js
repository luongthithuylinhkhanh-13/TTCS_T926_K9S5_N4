import apiClient from './apiClient';

export const getPendingPaymentRequests = projectId => apiClient
  .get(`/api/projects/${projectId}/payment-requests`)
  .then(response => response.data);

export const getMyPaymentRequests = projectId => apiClient
  .get(`/api/projects/${projectId}/payment-requests/my-requests`)
  .then(response => response.data);

export const getEligiblePaymentItems = projectId => apiClient
  .get(`/api/projects/${projectId}/payment-requests/eligible-items`)
  .then(response => response.data);

export const submitPaymentRequest = (projectId, request) => apiClient
  .post(`/api/projects/${projectId}/payment-requests`, request)
  .then(response => response.data);

export const createPaymentRequestDraft = (projectId, request) => apiClient
  .post(`/api/projects/${projectId}/payment-requests/drafts`, request)
  .then(response => response.data);

export const updatePaymentRequestDraft = (projectId, requestId, request) => apiClient
  .put(`/api/projects/${projectId}/payment-requests/${requestId}`, request)
  .then(response => response.data);

export const sendPaymentRequestDraft = (projectId, requestId) => apiClient
  .post(`/api/projects/${projectId}/payment-requests/${requestId}/submit`)
  .then(response => response.data);

export const approvePaymentRequest = (projectId, requestId) => apiClient
  .post(`/api/projects/${projectId}/payment-requests/${requestId}/approve`)
  .then(response => response.data);

export const rejectPaymentRequest = (projectId, requestId, reason) => apiClient
  .post(`/api/projects/${projectId}/payment-requests/${requestId}/reject`, { reason })
  .then(response => response.data);