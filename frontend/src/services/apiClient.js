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
  const auth = getAuthUser();
  if (auth?.token) {
    config.headers.Authorization = `Bearer ${auth.token}`;
  }
  if (auth?.userId) {
    config.headers['X-User-Id'] = auth.userId;
  }
  return config;
});

apiClient.interceptors.response.use(
  response => response,
  error => {
    const message = error.response?.data?.message
      || error.message
      || 'Không thể kết nối đến máy chủ';
    const requestError = new Error(message);
    requestError.status = error.response?.status;

    const isExpiredToken = /token.*(hết hạn|không hợp lệ)|unauthorized|invalid token/i.test(message);

    if (error.response?.status === 401 && isExpiredToken) {
      logoutUser();
      if (window.location.pathname !== '/login') {
        window.location.href = '/login';
      }
    }

    return Promise.reject(requestError);
  }
);

export default apiClient;