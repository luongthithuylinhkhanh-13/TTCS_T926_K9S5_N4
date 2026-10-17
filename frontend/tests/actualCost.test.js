import assert from 'node:assert/strict';
import test from 'node:test';

import {
  validateActualCost,
  calculateTotalCost,
  formatCurrency
} from '../src/services/actualCostApi.js';

test('TC-COST-01: T-94 - Chấp nhận dữ liệu nhập chi phí thực tế hợp lệ', () => {
  const result = validateActualCost({
    name: 'Mua cát xây dựng đợt 1',
    amount: 15000000,
    costType: 'VẬT_TƯ',
    incurredDate: '2026-10-17'
  });
  assert.equal(result.valid, true);
  assert.equal(result.errors.length, 0);
});

test('TC-COST-02: T-94 - Báo lỗi khi số tiền chi phí nhỏ hơn hoặc bằng 0', () => {
  const resZero = validateActualCost({
    name: 'Nhân công',
    amount: 0,
    costType: 'NHÂN_CÔNG',
    incurredDate: '2026-10-17'
  });
  assert.equal(resZero.valid, false);
  assert.ok(resZero.errors.includes('Số tiền chi phí phải lớn hơn 0'));

  const resNegative = validateActualCost({
    name: 'Nhân công',
    amount: -500000,
    costType: 'NHÂN_CÔNG',
    incurredDate: '2026-10-17'
  });
  assert.equal(resNegative.valid, false);
  assert.ok(resNegative.errors.includes('Số tiền chi phí phải lớn hơn 0'));
});

test('TC-COST-03: T-94 - Báo lỗi khi tên khoản chi để trống', () => {
  const result = validateActualCost({
    name: '   ',
    amount: 1000000,
    costType: 'VẬT_TƯ',
    incurredDate: '2026-10-17'
  });
  assert.equal(result.valid, false);
  assert.ok(result.errors.includes('Tên chi phí không được để trống'));
});

test('TC-COST-04: T-94 - Báo lỗi khi thiếu loại chi phí hoặc ngày phát sinh', () => {
  const result = validateActualCost({
    name: 'Chi phí vận chuyển',
    amount: 2000000,
    costType: '',
    incurredDate: null
  });
  assert.equal(result.valid, false);
  assert.ok(result.errors.includes('Vui lòng chọn loại chi phí'));
  assert.ok(result.errors.includes('Ngày phát sinh chi phí không được để trống'));
});

test('TC-COST-05: T-96 - Tính chính xác tổng chi phí thực tế lũy kế', () => {
  const items = [
    { amount: 12500000 },
    { amount: 27500000 },
    { amount: 5000000 }
  ];
  const total = calculateTotalCost(items);
  assert.equal(total, 45000000);
});

test('TC-COST-06: T-96 - Trả về 0 khi danh sách chi phí trống hoặc rỗng', () => {
  assert.equal(calculateTotalCost([]), 0);
  assert.equal(calculateTotalCost(null), 0);
  assert.equal(calculateTotalCost(undefined), 0);
});

test('TC-COST-07: Định dạng tiền tệ VNĐ hiển thị đúng định dạng', () => {
  const formatted = formatCurrency(50000000);
  assert.ok(formatted.includes('50.000.000') || formatted.includes('50,000,000'));
});
