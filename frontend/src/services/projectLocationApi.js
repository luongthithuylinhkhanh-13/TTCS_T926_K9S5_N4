/**
 * API Client cho cấu hình vị trí và bán kính công trường (Project Site Location / Geofence)
 * Phục vụ Story S-41 (T-71).
 */

import apiClient from './apiClient.js';

/**
 * Kiểm tra tính hợp lệ của dữ liệu cấu hình vị trí công trường.
 *
 * @param {{ latitude: number|string, longitude: number|string, radiusMeters: number|string }} config
 * @returns {{ valid: boolean, errors: string[] }}
 */
export const validateSiteLocation = config => {
  const errors = [];
  if (!config) {
    return { valid: false, errors: ['Dữ liệu cấu hình không được để trống'] };
  }

  const { latitude, longitude, radiusMeters } = config;

  if (latitude === undefined || latitude === null || latitude === '') {
    errors.push('Vĩ độ (latitude) không được để trống');
  } else {
    const lat = Number(latitude);
    if (isNaN(lat) || lat < -90 || lat > 90) {
      errors.push('Vĩ độ phải là số hợp lệ trong khoảng từ -90° đến +90°');
    }
  }

  if (longitude === undefined || longitude === null || longitude === '') {
    errors.push('Kinh độ (longitude) không được để trống');
  } else {
    const lon = Number(longitude);
    if (isNaN(lon) || lon < -180 || lon > 180) {
      errors.push('Kinh độ phải là số hợp lệ trong khoảng từ -180° đến +180°');
    }
  }

  if (radiusMeters === undefined || radiusMeters === null || radiusMeters === '') {
    errors.push('Bán kính công trường (mét) không được để trống');
  } else {
    const radius = Number(radiusMeters);
    if (isNaN(radius) || radius <= 0) {
      errors.push('Bán kính công trường phải là số dương lớn hơn 0 mét');
    }
  }

  return {
    valid: errors.length === 0,
    errors
  };
};

/**
 * Lấy thông tin cấu hình vị trí công trường của dự án.
 *
 * @param {string} projectId ID dự án
 * @returns {Promise<{ projectId: string, projectCode: string, projectName: string, latitude: number|null, longitude: number|null, radiusMeters: number|null, configured: boolean }>}
 */
export const getProjectSiteLocation = async projectId => {
  const response = await apiClient.get(`/api/projects/${projectId}/site-location`);
  return response.data?.data || response.data;
};

/**
 * Cập nhật cấu hình vị trí và bán kính công trường.
 *
 * @param {string} projectId ID dự án
 * @param {{ latitude: number, longitude: number, radiusMeters: number }} payload
 * @returns {Promise<object>}
 */
export const updateProjectSiteLocation = async (projectId, payload) => {
  const validation = validateSiteLocation(payload);
  if (!validation.valid) {
    throw new Error(validation.errors.join('; '));
  }

  const cleanPayload = {
    latitude: Number(payload.latitude),
    longitude: Number(payload.longitude),
    radiusMeters: Number(payload.radiusMeters)
  };

  const response = await apiClient.put(`/api/projects/${projectId}/site-location`, cleanPayload);
  return response.data?.data || response.data;
};
