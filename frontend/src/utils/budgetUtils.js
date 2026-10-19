/**
 * T-92: Tính thành tiền dự toán cho hạng mục lá: Khối lượng * Đơn giá
 */
export const calculateItemTotalPrice = (quantity, unitPrice) => {
  const qty = Number(quantity);
  const price = Number(unitPrice);
  if (!Number.isNaN(qty) && !Number.isNaN(price) && qty > 0 && price > 0) {
    return Math.round(qty * price * 100) / 100;
  }
  return 0;
};

/**
 * T-82 / T-93: Kiểm tra tính hợp lệ của dữ liệu dự toán
 */
export const validateBudgetItem = (quantity, unit, unitPrice) => {
  const qty = quantity != null && quantity !== '' ? Number(quantity) : null;
  const price = unitPrice != null && unitPrice !== '' ? Number(unitPrice) : null;

  if (qty != null && (Number.isNaN(qty) || qty < 0)) {
    return { valid: false, error: 'Khối lượng công việc không được âm' };
  }

  if (price != null && (Number.isNaN(price) || price < 0)) {
    return { valid: false, error: 'Đơn giá không được âm' };
  }

  if (unit && unit.length > 50) {
    return { valid: false, error: 'Đơn vị tính không được vượt quá 50 ký tự' };
  }

  if (qty != null && qty > 0 && (!unit || !unit.trim())) {
    return { valid: false, error: 'Vui lòng nhập đơn vị tính khi đã khai báo khối lượng' };
  }

  return { valid: true };
};

/**
 * T-92: Cộng dồn dự toán từ các hạng mục lá lên các hạng mục cha (Rollup bottom-up)
 */
export const recalculateBudgetTreeRollup = (nodes = []) => {
  if (!nodes || nodes.length === 0) return [];

  return nodes.map(node => {
    const hasChildren = Array.isArray(node.children) && node.children.length > 0;

    if (!hasChildren) {
      // Hạng mục lá
      const totalPrice = calculateItemTotalPrice(node.quantity, node.unitPrice);
      return {
        ...node,
        isLeaf: true,
        childCount: 0,
        directTotalPrice: totalPrice,
        rollupTotalPrice: null,
        totalPrice,
        children: []
      };
    }

    // Hạng mục cha: đệ quy tính toán con trước, sau đó cộng dồn
    const updatedChildren = recalculateBudgetTreeRollup(node.children);
    const rollupTotal = updatedChildren.reduce(
      (sum, child) => sum + (Number(child.totalPrice) || 0),
      0
    );
    const roundedRollup = Math.round(rollupTotal * 100) / 100;

    return {
      ...node,
      isLeaf: false,
      childCount: updatedChildren.length,
      directTotalPrice: null,
      rollupTotalPrice: roundedRollup,
      totalPrice: roundedRollup,
      children: updatedChildren
    };
  });
};

/**
 * T-91: Tính toán thống kê tổng quan dự toán của cây WBS
 */
export const calculateBudgetSummary = (nodes = []) => {
  let totalItems = 0;
  let totalLeafItems = 0;
  let budgetedLeafItems = 0;
  let totalParentItems = 0;
  let totalBudget = 0;

  const traverse = (items) => {
    if (!items || !items.length) return;
    items.forEach(item => {
      totalItems += 1;
      const isLeaf = !item.children || item.children.length === 0;

      if (isLeaf) {
        totalLeafItems += 1;
        const qty = Number(item.quantity);
        const price = Number(item.unitPrice);
        if (qty > 0 && price > 0) {
          budgetedLeafItems += 1;
          totalBudget += Math.round(qty * price * 100) / 100;
        }
      } else {
        totalParentItems += 1;
        traverse(item.children);
      }
    });
  };

  traverse(nodes);

  const roundedBudget = Math.round(totalBudget * 100) / 100;
  const completionRate = totalLeafItems > 0
    ? Math.round((budgetedLeafItems / totalLeafItems) * 1000) / 10
    : 0;

  return {
    totalItems,
    totalLeafItems,
    totalParentItems,
    budgetedLeafItems,
    totalBudget: roundedBudget,
    completionRate
  };
};

/**
 * Xuất dữ liệu bảng dự toán ra định dạng CSV (hỗ trợ Excel tiếng Việt UTF-8 BOM)
 */
export const exportBudgetToCsv = (nodes = [], projectName = 'Du_An') => {
  const rows = [
    ['Mã WBS', 'Tên hạng mục công việc', 'Phân loại', 'Khối lượng', 'Đơn vị tính', 'Đơn giá (VNĐ)', 'Thành tiền dự toán (VNĐ)', 'Ghi chú']
  ];

  const traverse = (items, level = 0) => {
    items.forEach(node => {
      const isLeaf = !node.children || node.children.length === 0;
      const indent = '  '.repeat(level);
      const name = `${indent}${node.name || ''}`;
      const type = isLeaf ? 'Hạng mục lá' : 'Hạng mục cha (Cộng dồn)';
      const qty = isLeaf ? (node.quantity ?? '') : '';
      const unit = isLeaf ? (node.unit ?? '') : '';
      const price = isLeaf ? (node.unitPrice ?? '') : '';
      const total = node.totalPrice ?? 0;
      const note = isLeaf ? 'Nhập trực tiếp' : 'Tự động cộng dồn từ cấp con';

      rows.push([
        node.wbsCode || '',
        `"${name.replace(/"/g, '""')}"`,
        type,
        qty,
        unit,
        price,
        total,
        note
      ]);

      if (node.children && node.children.length > 0) {
        traverse(node.children, level + 1);
      }
    });
  };

  traverse(nodes);

  const csvContent = '\uFEFF' + rows.map(r => r.join(',')).join('\n');
  const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.setAttribute('href', url);
  link.setAttribute('download', `Bang_Du_Toan_${projectName.replace(/\s+/g, '_')}.csv`);
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(url);
};
