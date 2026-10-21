import apiClient from './apiClient';

export const getMilestones = projectId => {
  return apiClient
    .get(`/api/projects/${projectId}/milestones`)
    .then(response => response.data);
};

export const createMilestone = (projectId, milestone) => {
  return apiClient
    .post(`/api/projects/${projectId}/milestones`, milestone)
    .then(response => response.data);
};

export const updateMilestone = (projectId, milestoneId, milestone) => {
  return apiClient
    .put(`/api/projects/${projectId}/milestones/${milestoneId}`, milestone)
    .then(response => response.data);
};

export const deleteMilestone = (projectId, milestoneId) => {
  return apiClient
    .delete(`/api/projects/${projectId}/milestones/${milestoneId}`)
    .then(response => response.data);
};

export const getMilestoneWarnings = projectId => {
  return apiClient
    .get(`/api/projects/${projectId}/milestones/warnings`)
    .then(response => response.data);
};
