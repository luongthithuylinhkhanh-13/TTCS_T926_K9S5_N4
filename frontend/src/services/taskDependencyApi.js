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

  if (response.status === 204) {
    return null;
  }

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

export const getTaskDependencies = (taskId) => {
  return request(`/api/tasks/${taskId}/dependencies`);
};

export const addTaskDependency = (taskId, payload) => {
  return request(`/api/tasks/${taskId}/dependencies`, {
    method: 'POST',
    body: JSON.stringify(payload)
  });
};

export const deleteTaskDependency = (dependencyId) => {
  return request(`/api/tasks/dependencies/${dependencyId}`, {
    method: 'DELETE'
  });
};