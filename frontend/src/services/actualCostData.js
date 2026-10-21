
/**
 * T-98: Chuẩn hóa dữ liệu chi phí thực tế theo dự án.
 * Dữ liệu được lấy từ API actual-costs hiện có.
 */
export const normalizeActualCostData = (response, projectId) => {
  if (!Array.isArray(response)) {
    throw new Error('Dữ liệu chi phí thực tế không hợp lệ');
  }

  return response.map((item) => ({
    id: item.id,
    projectId: item.projectId ?? projectId,
    wbsItemId: item.wbsItemId ?? null,
    wbsItemName: item.wbsItemName ?? null,
    wbsCode: item.wbsCode ?? null,
    taskId: item.taskId ?? null,
    taskName: item.taskName ?? null,
    name: item.name ?? '',
    amount: item.amount == null ? null : Number(item.amount),
    costType: item.costType ?? '',
    incurredDate: item.incurredDate ?? null,
    note: item.note ?? null,
    createdBy: item.createdBy ?? null,
    createdAt: item.createdAt ?? null,
    updatedAt: item.updatedAt ?? null,
  }));
};
