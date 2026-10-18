
export const normalizeEstimateItems = (response, projectId) => {
  const items = Array.isArray(response)
    ? response
    : Array.isArray(response?.items)
      ? response.items
      : null;

  if (!items) {
    throw new Error('Dữ liệu WBS trả về không hợp lệ');
  }

  return items.map((item) => {
    const quantity = item.quantity ?? item.totalVolume ?? null;
    const unitPrice = item.unitPrice ?? null;

    const totalPrice = item.totalPrice != null
      ? Number(item.totalPrice)
      : quantity != null && unitPrice != null
        ? Math.round(Number(quantity) * Number(unitPrice) * 100) / 100
        : null;

    return {
      id: item.id,
      projectId: item.projectId ?? projectId,
      parentId: item.parentId ?? null,
      wbsCode: item.wbsCode ?? '',
      name: item.name ?? '',
      type: item.type ?? '',
      quantity: quantity == null ? null : Number(quantity),
      unit: item.unit ?? null,
      unitPrice: unitPrice == null ? null : Number(unitPrice),
      totalPrice,
    };
  });
};
