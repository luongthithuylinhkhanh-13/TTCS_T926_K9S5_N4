import assert from 'node:assert/strict';
import test from 'node:test';
import { calculateItemAmount, calculateTotalAmount } from '../src/services/acceptanceApi.js';

// Logic validation nghiệp vụ phía client cho Acceptance Report
function validateAcceptanceReport(report) {
  const errors = [];
  if (!report.reportNumber || !report.reportNumber.trim()) {
    errors.push('Số biên bản không được để trống');
  }
  if (!report.acceptanceDate) {
    errors.push('Ngày nghiệm thu không được để trống');
  }
  if (report.periodNumber != null && Number(report.periodNumber) <= 0) {
    errors.push('Số đợt phải là số nguyên dương');
  }
  if (!Array.isArray(report.items) || report.items.length === 0) {
    errors.push('Phiếu nghiệm thu phải có ít nhất một hạng mục');
  } else {
    const itemIds = new Set();
    report.items.forEach((item, idx) => {
      if (item.wbsItemId != null) {
        if (itemIds.has(item.wbsItemId)) {
          errors.push(`Hạng mục trùng lặp tại vị trí ${idx + 1}`);
        }
        itemIds.add(item.wbsItemId);
      }
      if (item.acceptedQuantity == null || Number(item.acceptedQuantity) <= 0) {
        errors.push(`Khối lượng nghiệm thu dòng ${idx + 1} phải lớn hơn 0`);
      }
      if (item.unitPrice != null && Number(item.unitPrice) < 0) {
        errors.push(`Đơn giá dòng ${idx + 1} không được âm`);
      }
    });
  }
  return errors;
}

test('TC-ACC-01: T-83 - Kiểm tra tính hợp lệ khi tạo phiếu nghiệm thu theo đợt', () => {
  const validReport = {
    reportNumber: 'NT-2026-001',
    name: 'Nghiệm thu phần thô đợt 1',
    periodNumber: 1,
    acceptanceDate: '2026-10-12',
    status: 'DRAFT',
    items: [
      {
        wbsItemId: 101,
        itemName: 'Bê tông móng M250',
        unit: 'm3',
        acceptedQuantity: 45.5,
        unitPrice: 1250000,
      },
    ],
  };

  const errors = validateAcceptanceReport(validReport);
  assert.equal(errors.length, 0);
});

test('TC-ACC-02: T-83 - Báo lỗi khi thiếu số biên bản, ngày nghiệm thu hoặc đợt <= 0', () => {
  const invalidReport = {
    reportNumber: '   ',
    periodNumber: -1,
    acceptanceDate: null,
    items: [
      {
        wbsItemId: 101,
        acceptedQuantity: 10,
        unitPrice: 100000,
      },
    ],
  };

  const errors = validateAcceptanceReport(invalidReport);
  assert.ok(errors.includes('Số biên bản không được để trống'));
  assert.ok(errors.includes('Ngày nghiệm thu không được để trống'));
  assert.ok(errors.includes('Số đợt phải là số nguyên dương'));
});

test('TC-ACC-03: T-84 - Chọn hạng mục và xác thực khối lượng nghiệm thu', () => {
  const reportWithItems = {
    reportNumber: 'NT-2026-002',
    periodNumber: 2,
    acceptanceDate: '2026-10-15',
    items: [
      {
        wbsItemId: 201,
        itemName: 'Cốt thép dầm sàn CB300-V',
        unit: 'tấn',
        acceptedQuantity: 12.8,
        unitPrice: 18500000,
      },
      {
        wbsItemId: 202,
        itemName: 'Ván khuôn phủ phim',
        unit: 'm2',
        acceptedQuantity: 320,
        unitPrice: 165000,
      },
    ],
  };

  const errors = validateAcceptanceReport(reportWithItems);
  assert.equal(errors.length, 0);
  assert.equal(reportWithItems.items.length, 2);
});

test('TC-ACC-04: T-84 - Báo lỗi khi khối lượng nghiệm thu âm hoặc bằng 0 hoặc trùng hạng mục', () => {
  const invalidItemsReport = {
    reportNumber: 'NT-2026-003',
    periodNumber: 1,
    acceptanceDate: '2026-10-15',
    items: [
      {
        wbsItemId: 301,
        acceptedQuantity: -5,
        unitPrice: 200000,
      },
      {
        wbsItemId: 301, // Trùng wbsItemId
        acceptedQuantity: 0,
        unitPrice: -50000, // Đơn giá âm
      },
    ],
  };

  const errors = validateAcceptanceReport(invalidItemsReport);
  assert.ok(errors.some(e => e.includes('Khối lượng nghiệm thu dòng 1 phải lớn hơn 0')));
  assert.ok(errors.some(e => e.includes('Hạng mục trùng lặp')));
  assert.ok(errors.some(e => e.includes('Khối lượng nghiệm thu dòng 2 phải lớn hơn 0')));
  assert.ok(errors.some(e => e.includes('Đơn giá dòng 2 không được âm')));
});

test('TC-ACC-05: T-85 - Tự động tính chính xác giá trị nghiệm thu từng dòng (amount = acceptedQuantity * unitPrice)', () => {
  const amount1 = calculateItemAmount(10, 150000);
  assert.equal(amount1, 1500000);

  const amount2 = calculateItemAmount(12.5, 240000);
  assert.equal(amount2, 3000000);

  const amount3 = calculateItemAmount(33.33, 12345.67);
  assert.equal(amount3, 411481.18);
});

test('TC-ACC-06: T-85 - Giá trị nghiệm thu trả về 0 khi khối lượng hoặc đơn giá âm hoặc không hợp lệ', () => {
  assert.equal(calculateItemAmount(-5, 100000), 0);
  assert.equal(calculateItemAmount(10, -50000), 0);
  assert.equal(calculateItemAmount(0, 100000), 0);
  assert.equal(calculateItemAmount(null, 100000), 0);
  assert.equal(calculateItemAmount(undefined, undefined), 0);
});

test('TC-ACC-07: T-85 - Tự động tính tổng giá trị nghiệm thu của toàn phiếu (totalAmount = sum(items))', () => {
  const items = [
    { acceptedQuantity: 50, unitPrice: 1200000 },   // 60,000,000
    { acceptedQuantity: 12.5, unitPrice: 18000000 }, // 225,000,000
    { acceptedQuantity: 200, unitPrice: 150000 },   // 30,000,000
  ];

  const total = calculateTotalAmount(items);
  assert.equal(total, 315000000);
});

test('TC-ACC-08: T-85 - Tổng giá trị an toàn khi danh sách rỗng hoặc có phần tử không hợp lệ', () => {
  assert.equal(calculateTotalAmount([]), 0);
  assert.equal(calculateTotalAmount(null), 0);
  assert.equal(
    calculateTotalAmount([
      { acceptedQuantity: -10, unitPrice: 50000 },
      { acceptedQuantity: 5, unitPrice: 20000 },
    ]),
    100000
  );
});
