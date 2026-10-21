import apiClient from './apiClient';

export const getCategoryTasks = (projectId, categoryId) => {
  return apiClient
    .get(`/api/projects/${projectId}/categories/${categoryId}/tasks`)
    .then(response => response.data);
};

export const createCategoryTask = (projectId, categoryId, task) => {
  return apiClient
    .post(`/api/projects/${projectId}/categories/${categoryId}/tasks`, task)
    .then(response => response.data);
};