
import { getProjectWbs } from './wbsApi.js';
import { normalizeEstimateItems } from './estimateUtils.js';

export { normalizeEstimateItems };

export const getEstimateByCategory = async (projectId) => {
  if (!projectId) {
    throw new Error('Vui lòng chọn dự án để lấy dữ liệu dự toán');
  }

  const response = await getProjectWbs(projectId);
  return normalizeEstimateItems(response, projectId);
};
