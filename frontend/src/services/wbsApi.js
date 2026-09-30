import { getAuthUser } from '../utils/auth';

const getToken = () => {
  const auth = getAuthUser();

  if (!auth?.token) {
    throw new Error('Bạn chưa đăng nhập hoặc phiên đăng nhập đã hết hạn');
  }

  return auth.token;
};

const request = async (url, options = {}) => {
  const token = getToken();

  const response = await fetch(url, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`,
      ...(options.headers || {})
    }
  });

  let data = null;

  try {
    data = await response.json();
  } catch {
    data = null;
  }

  if (!response.ok) {
    throw new Error(
      data?.message || `Yêu cầu thất bại (${response.status})`
    );
  }

  return data;
};

export const getProjects = () => {
  return request('/api/projects');
};

export const getProjectWbs = (projectId) => {
  return request(`/api/projects/${projectId}/wbs`);
};

export const createWbsItem = (projectId, item) => {
  return request(`/api/projects/${projectId}/wbs`, {
    method: 'POST',
    body: JSON.stringify(item)
  });
};

export const updateWbsItem = (projectId, itemId, item) => {
  return request(`/api/projects/${projectId}/wbs/${itemId}`, {
    method: 'PUT',
    body: JSON.stringify(item)
  });
};

export const deleteWbsItem = (projectId, itemId) => {
  return request(`/api/projects/${projectId}/wbs/${itemId}`, {
    method: 'DELETE'
  });
};