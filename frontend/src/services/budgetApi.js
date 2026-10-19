import apiClient from './apiClient';

/**
 * T-91, T-92: Lấy cây WBS dự toán công trình với các giá trị cộng dồn
 */
export const getProjectBudgetTree = async (projectId) => {
  const response = await apiClient.get(`/api/projects/${projectId}/budget`);
  return response.data;
};

/**
 * T-91: Lấy thông tin thống kê tổng hợp dự toán
 */
export const getBudgetSummary = async (projectId) => {
  const response = await apiClient.get(`/api/projects/${projectId}/budget/summary`);
  return response.data;
};

/**
 * T-92, T-93: Cập nhật dự toán cho một hạng mục lá
 */
export const updateBudgetItem = async (projectId, itemId, { quantity, unit, unitPrice }) => {
  const response = await apiClient.put(`/api/projects/${projectId}/budget/${itemId}`, {
    quantity: quantity != null ? Number(quantity) : null,
    unit: unit ? unit.trim() : null,
    unitPrice: unitPrice != null ? Number(unitPrice) : null,
  });
  return response.data;
};

/**
 * T-92, T-93: Cập nhật hàng loạt (Batch update) dự toán cho nhiều hạng mục lá
 */
export const batchUpdateBudget = async (projectId, items) => {
  const response = await apiClient.put(`/api/projects/${projectId}/budget/batch`, {
    items: items.map((item) => ({
      itemId: item.itemId || item.id,
      quantity: item.quantity != null ? Number(item.quantity) : null,
      unit: item.unit ? item.unit.trim() : null,
      unitPrice: item.unitPrice != null ? Number(item.unitPrice) : null,
    })),
  });
  return response.data;
};
