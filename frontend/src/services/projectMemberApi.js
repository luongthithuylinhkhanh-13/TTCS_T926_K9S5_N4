import apiClient from './apiClient';

const unwrap = (response) => (response.data?.data !== undefined ? response.data.data : response.data);

/**
 * Lấy vai trò của chính người dùng hiện tại trong một dự án
 */
export const getMyProjectMembership = (projectId) => {
  return apiClient.get(`/api/projects/${projectId}/members/me`).then(unwrap);
};

/**
 * Lấy danh sách thành viên của một dự án
 */
export const getProjectMembers = (projectId) => {
  return apiClient.get(`/api/projects/${projectId}/members`).then(unwrap);
};

/**
 * Lấy danh sách tài khoản ACTIVE hợp lệ để thêm vào dự án
 */
export const getAvailableUsers = (projectId) => {
  return apiClient.get(`/api/projects/${projectId}/members/available-users`).then(unwrap);
};

/**
 * Thêm thành viên vào dự án với vai trò xác định
 */
export const addProjectMember = (projectId, userId, roleCode) => {
  return apiClient.post(`/api/projects/${projectId}/members`, {
    userId,
    roleCode
  }).then(unwrap);
};

/**
 * Cập nhật vai trò của thành viên trong dự án
 */
export const updateProjectMemberRole = (projectId, userId, newRoleName) => {
  return apiClient.put(`/api/projects/${projectId}/members/${userId}/role`, {
    newRoleName
  }).then(unwrap);
};

/**
 * Xóa thành viên khỏi dự án (không xóa tài khoản người dùng)
 */
export const removeProjectMember = (projectId, userId) => {
  return apiClient.delete(`/api/projects/${projectId}/members/${userId}`).then(unwrap);
};
