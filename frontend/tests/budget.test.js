import test from 'node:test';
import assert from 'node:assert/strict';
import {
  calculateItemTotalPrice,
  validateBudgetItem,
  recalculateBudgetTreeRollup,
  calculateBudgetSummary
} from '../src/utils/budgetUtils.js';

test('TC-BUD-01: T-92 - Tính chính xác thành tiền dự toán = khối lượng * đơn giá cho hạng mục lá', () => {
  const total = calculateItemTotalPrice(125.5, 340000);
  assert.equal(total, 42670000);

  // Kiểm tra làm tròn 2 chữ số thập phân với số tiền lớn
  const decimalTotal = calculateItemTotalPrice(33.33, 10000.5);
  assert.equal(decimalTotal, 333316.66);
});

test('TC-BUD-02: T-92 - Thành tiền trả về 0 khi khối lượng hoặc đơn giá chưa nhập hoặc bằng 0', () => {
  assert.equal(calculateItemTotalPrice(null, 100000), 0);
  assert.equal(calculateItemTotalPrice(50, null), 0);
  assert.equal(calculateItemTotalPrice(0, 500000), 0);
  assert.equal(calculateItemTotalPrice(-10, 500000), 0);
  assert.equal(calculateItemTotalPrice(10, -500000), 0);
});

test('TC-BUD-03: T-92 - Tự động cộng dồn (rollup) từ các hạng mục lá lên hạng mục cha', () => {
  const sampleTree = [
    {
      id: 'parent-1',
      wbsCode: '1.0',
      name: 'Phần móng',
      children: [
        {
          id: 'leaf-1',
          wbsCode: '1.1',
          name: 'Đào đất móng',
          quantity: 100,
          unit: 'm3',
          unitPrice: 150000 // 15,000,000
        },
        {
          id: 'leaf-2',
          wbsCode: '1.2',
          name: 'Đổ bê tông móng',
          quantity: 40,
          unit: 'm3',
          unitPrice: 1250000 // 50,000,000
        }
      ]
    }
  ];

  const result = recalculateBudgetTreeRollup(sampleTree);
  assert.equal(result.length, 1);
  const parentNode = result[0];

  assert.equal(parentNode.isLeaf, false);
  assert.equal(parentNode.directTotalPrice, null);
  assert.equal(parentNode.rollupTotalPrice, 65000000);
  assert.equal(parentNode.totalPrice, 65000000);

  // Kiểm tra từng lá
  assert.equal(parentNode.children[0].isLeaf, true);
  assert.equal(parentNode.children[0].totalPrice, 15000000);
  assert.equal(parentNode.children[1].isLeaf, true);
  assert.equal(parentNode.children[1].totalPrice, 50000000);
});

test('TC-BUD-04: T-92 - Tự động cộng dồn cây đa cấp (Cụ -> Ông -> Cha -> Con lá)', () => {
  const multiLevelTree = [
    {
      id: 'root-1',
      wbsCode: '1',
      name: 'Khối tháp A',
      children: [
        {
          id: 'phase-1',
          wbsCode: '1.1',
          name: 'Phần thân thô',
          children: [
            {
              id: 'group-1',
              wbsCode: '1.1.1',
              name: 'Thi công tầng 1',
              children: [
                {
                  id: 'leaf-1',
                  wbsCode: '1.1.1.1',
                  name: 'Cột bê tông tầng 1',
                  quantity: 20,
                  unit: 'm3',
                  unitPrice: 1500000 // 30,000,000
                },
                {
                  id: 'leaf-2',
                  wbsCode: '1.1.1.2',
                  name: 'Dầm sàn tầng 1',
                  quantity: 60,
                  unit: 'm3',
                  unitPrice: 1500000 // 90,000,000
                }
              ]
            }
          ]
        }
      ]
    }
  ];

  const result = recalculateBudgetTreeRollup(multiLevelTree);
  const root = result[0];
  const phase = root.children[0];
  const group = phase.children[0];

  assert.equal(group.totalPrice, 120000000);
  assert.equal(phase.totalPrice, 120000000);
  assert.equal(root.totalPrice, 120000000);
});

test('TC-BUD-05: T-82 & T-93 - Kiểm tra tính hợp lệ của dữ liệu dự toán', () => {
  // Hợp lệ
  assert.deepEqual(validateBudgetItem(100, 'm3', 250000), { valid: true });
  assert.deepEqual(validateBudgetItem(null, null, null), { valid: true });

  // Lỗi khối lượng âm
  const negQty = validateBudgetItem(-5, 'm3', 250000);
  assert.equal(negQty.valid, false);
  assert.equal(negQty.error, 'Khối lượng công việc không được âm');

  // Lỗi đơn giá âm
  const negPrice = validateBudgetItem(5, 'm3', -1000);
  assert.equal(negPrice.valid, false);
  assert.equal(negPrice.error, 'Đơn giá không được âm');

  // Có khối lượng nhưng thiếu ĐVT
  const missingUnit = validateBudgetItem(5, '', 250000);
  assert.equal(missingUnit.valid, false);
  assert.equal(missingUnit.error, 'Vui lòng nhập đơn vị tính khi đã khai báo khối lượng');

  // ĐVT dài quá 50 ký tự
  const longUnit = validateBudgetItem(5, 'a'.repeat(51), 250000);
  assert.equal(longUnit.valid, false);
  assert.equal(longUnit.error, 'Đơn vị tính không được vượt quá 50 ký tự');
});

test('TC-BUD-06: T-91 - Tính toán chính xác thống kê tổng quan dự toán (Budget Summary)', () => {
  const nodes = [
    {
      id: 'p1',
      name: 'Giai đoạn 1',
      children: [
        { id: 'l1', name: 'Task 1', quantity: 10, unit: 'm3', unitPrice: 2000000 }, // 20M
        { id: 'l2', name: 'Task 2', quantity: 5, unit: 'tấn', unitPrice: 18000000 }  // 90M
      ]
    },
    {
      id: 'p2',
      name: 'Giai đoạn 2',
      children: [
        { id: 'l3', name: 'Task 3 chưa nhập dự toán', quantity: 0, unit: '', unitPrice: 0 }
      ]
    }
  ];

  const summary = calculateBudgetSummary(nodes);
  assert.equal(summary.totalItems, 5);
  assert.equal(summary.totalParentItems, 2);
  assert.equal(summary.totalLeafItems, 3);
  assert.equal(summary.budgetedLeafItems, 2);
  assert.equal(summary.totalBudget, 110000000);
  assert.equal(summary.completionRate, 66.7);
});
