import axios from 'axios';
import { getAuthUser, logoutUser } from '../utils/auth';

const apiClient = axios.create({
  baseURL: '/',
  headers: {
    Accept: 'application/json',
    'Content-Type': 'application/json'
  }
});

apiClient.interceptors.request.use(config => {
  const token = getAuthUser()?.token;
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

apiClient.interceptors.response.use(
  response => response,
  error => {
    const message = error.response?.data?.message
      || error.message
      || 'Không thể kết nối đến máy chủ';

    const isExpiredToken = /token.*(hết hạn|không hợp lệ)|unauthorized|invalid token/i.test(message);

    if (error.response?.status === 401 && isExpiredToken) {
      logoutUser();
      if (window.location.pathname !== '/login') {
        window.location.href = '/login';
      }
    }

    return Promise.reject(new Error(message));
  }
);

export default apiClient;