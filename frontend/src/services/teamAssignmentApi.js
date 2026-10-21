import apiClient from './apiClient';

export const getProjectCrews = projectId =>
  apiClient.get(`/api/projects/${projectId}/crew-members`)
    .then(response => response.data);

export const assignTaskCrew = (projectId, taskId, teamMemberId) =>
  apiClient.put(
    `/api/projects/${projectId}/tasks/${taskId}/team-assignment`,
    { teamMemberId: teamMemberId || null }
  ).then(response => response.data);

export const getTaskCrewAssignmentHistory = (projectId, taskId) =>
  apiClient.get(
    `/api/projects/${projectId}/tasks/${taskId}/team-assignment/history`
  ).then(response => response.data);
