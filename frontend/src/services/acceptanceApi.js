/**
 * API Client cho Nghiệm thu khối lượng theo đợt (Acceptance Reports)
 * Phục vụ Story S-36 (NTDHTCT-220):
 *   - NTDHTCT-268 (T-83): Tạo phiếu nghiệm thu theo từng đợt
 *   - NTDHTCT-269 (T-84): Chọn hạng mục và nhập khối lượng nghiệm thu
 *   - NTDHTCT-270 (T-85): Tự động tính giá trị nghiệm thu của phiếu
 */

import apiClient from './apiClient.js';

/**
 * T-85 (NTDHTCT-270): Hàm tiện ích tính thành tiền cho một hạng mục
 * amount = acceptedQuantity * unitPrice
 */
export const calculateItemAmount = (acceptedQuantity, unitPrice) => {
  const qty = Number(acceptedQuantity) || 0;
  const price = Number(unitPrice) || 0;
  if (qty < 0 || price < 0) return 0;
  return Math.round(qty * price * 100) / 100;
};

/**
 * T-85 (NTDHTCT-270): Hàm tiện ích tính tổng giá trị nghiệm thu của toàn phiếu
 * totalAmount = sum(item.amount)
 */
export const calculateTotalAmount = items => {
  if (!Array.isArray(items) || items.length === 0) return 0;
  const total = items.reduce((sum, item) => {
    const itemAmount = calculateItemAmount(item.acceptedQuantity, item.unitPrice);
    return sum + itemAmount;
  }, 0);
  return Math.round(total * 100) / 100;
};

/**
 * Lấy danh sách phiếu nghiệm thu theo đợt của một dự án
 */
export const getAcceptanceReports = async projectId => {
  const response = await apiClient.get(`/api/projects/${projectId}/acceptance-reports`);
  return Array.isArray(response.data) ? response.data : [];
};

/**
 * Xem chi tiết một phiếu nghiệm thu kèm các hạng mục và giá trị
 */
export const getAcceptanceReport = async (projectId, reportId) => {
  const response = await apiClient.get(`/api/projects/${projectId}/acceptance-reports/${reportId}`);
  return response.data;
};

/**
 * Lấy tóm tắt thống kê nghiệm thu của dự án
 */
export const getAcceptanceSummary = async projectId => {
  try {
    const response = await apiClient.get(`/api/projects/${projectId}/acceptance-reports/summary`);
    return response.data;
  } catch (err) {
    console.warn('[AcceptanceApi] Không thể lấy summary:', err.message);
    return null;
  }
};

/**
 * Lấy số đợt và mã biên bản gợi ý tiếp theo
 */
export const getNextPeriod = async projectId => {
  const response = await apiClient.get(`/api/projects/${projectId}/acceptance-reports/next-period`);
  return response.data;
};

/**
 * T-83, T-84, T-85: Tạo mới phiếu nghiệm thu theo từng đợt
 */
export const createAcceptanceReport = async (projectId, reportData) => {
  const response = await apiClient.post(`/api/projects/${projectId}/acceptance-reports`, reportData);
  return response.data;
};

/**
 * Cập nhật thông tin phiếu nghiệm thu
 */
export const updateAcceptanceReport = async (projectId, reportId, reportData) => {
  const response = await apiClient.put(`/api/projects/${projectId}/acceptance-reports/${reportId}`, reportData);
  return response.data;
};

/**
 * Xóa phiếu nghiệm thu
 */
export const deleteAcceptanceReport = async (projectId, reportId) => {
  const response = await apiClient.delete(`/api/projects/${projectId}/acceptance-reports/${reportId}`);
  return response.data;
};
