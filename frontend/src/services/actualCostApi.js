/**
 * API Client & Tiện ích cho Quản lý chi phí thực tế (Actual Costs)
 * Phục vụ Story S-47 (NTDHTCT-237):
 *   - NTDHTCT-306 (T-94): Xây dựng chức năng nhập chi phí thực tế
 *   - NTDHTCT-307 (T-95): Liên kết chi phí với hạng mục tương ứng
 *   - NTDHTCT-308 (T-96): Lưu và hiển thị lịch sử chi phí
 *   - T-98: Lấy và chuẩn hóa dữ liệu chi phí thực tế
 */

import apiClient from './apiClient.js';
import { normalizeActualCostData } from './actualCostData.js';

/**
 * T-94: Kiểm tra tính hợp lệ của dữ liệu nhập chi phí thực tế
 */
export const validateActualCost = ({ name, amount, costType, incurredDate }) => {
  const errors = [];

  if (!name || !name.trim()) {
    errors.push('Tên chi phí không được để trống');
  } else if (name.trim().length > 255) {
    errors.push('Tên chi phí không được vượt quá 255 ký tự');
  }

  const numAmount = Number(amount);
  if (
    amount === undefined ||
    amount === null ||
    isNaN(numAmount) ||
    numAmount <= 0
  ) {
    errors.push('Số tiền chi phí phải lớn hơn 0');
  }

  if (!costType || !costType.trim()) {
    errors.push('Vui lòng chọn loại chi phí');
  }

  if (!incurredDate) {
    errors.push('Ngày phát sinh chi phí không được để trống');
  }

  return {
    valid: errors.length === 0,
    errors,
  };
};

/**
 * Tiện ích định dạng tiền tệ VNĐ
 */
export const formatCurrency = (amount) => {
  if (amount === null || amount === undefined || isNaN(amount)) {
    return '0 đ';
  }

  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
  }).format(amount);
};

/**
 * T-96: Tiện ích tính tổng chi phí lũy kế từ danh sách
 */
export const calculateTotalCost = (costs = []) => {
  if (!Array.isArray(costs) || costs.length === 0) return 0;

  return costs.reduce(
    (sum, item) => sum + (Number(item?.amount) || 0),
    0,
  );
};

/**
 * T-96, T-98: Lấy và chuẩn hóa danh sách chi phí thực tế của dự án.
 */
export const getActualCosts = async (projectId, params = {}) => {
  const response = await apiClient.get(
    `/api/projects/${projectId}/actual-costs`,
    { params },
  );

  return Array.isArray(response.data)
    ? normalizeActualCostData(response.data, projectId)
    : [];
};

/**
 * T-94 & T-95: Nhập chi phí thực tế mới (kèm liên kết hạng mục)
 */
export const createActualCost = async (projectId, costData) => {
  const response = await apiClient.post(
    `/api/projects/${projectId}/actual-costs`,
    costData,
  );

  return response.data;
};

/**
 * T-95 & T-96: Lấy tổng hợp chi phí thực tế theo dự án hoặc theo hạng mục
 */
export const getActualCostSummary = async (projectId, wbsItemId = null) => {
  const params = wbsItemId ? { wbsItemId } : {};

  const response = await apiClient.get(
    `/api/projects/${projectId}/actual-costs/summary`,
    { params },
  );

  return response.data;
};

/**
 * T-95 & T-96: Lấy lịch sử chi phí của riêng một hạng mục
 */
export const getActualCostsByWbsItem = async (projectId, wbsItemId) => {
  const response = await apiClient.get(
    `/api/projects/${projectId}/actual-costs/wbs-items/${wbsItemId}`,
  );

  return Array.isArray(response.data) ? response.data : [];
};