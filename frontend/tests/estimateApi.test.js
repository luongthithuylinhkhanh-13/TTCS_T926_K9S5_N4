
import assert from 'node:assert/strict';
import test from 'node:test';

import { normalizeEstimateItems } from '../src/services/estimateUtils.js';

test('T-97: chuẩn hóa dữ liệu dự toán theo hạng mục', () => {
  const result = normalizeEstimateItems([
    {
      id: '1',
      projectId: 'project-1',
      parentId: null,
      wbsCode: '1',
      name: 'Phần móng',
      type: 'category',
      quantity: 10,
      unit: 'm3',
      unitPrice: 500000,
    },
  ], 'project-1');

  assert.equal(result.length, 1);
  assert.equal(result[0].name, 'Phần móng');
  assert.equal(result[0].quantity, 10);
  assert.equal(result[0].unitPrice, 500000);
  assert.equal(result[0].totalPrice, 5000000);
});

test('T-97: giữ thành tiền null khi thiếu khối lượng hoặc đơn giá', () => {
  const result = normalizeEstimateItems([
    { id: '1', name: 'Hạng mục A', quantity: null, unitPrice: 500000 },
    { id: '2', name: 'Hạng mục B', quantity: 10, unitPrice: null },
  ], 'project-1');

  assert.equal(result[0].totalPrice, null);
  assert.equal(result[1].totalPrice, null);
});

test('T-97: không cộng dồn hoặc loại bỏ hạng mục cha và con', () => {
  const result = normalizeEstimateItems([
    { id: '1', name: 'Hạng mục cha', type: 'category' },
    { id: '2', name: 'Công việc con', parentId: '1', type: 'task' },
  ], 'project-1');

  assert.equal(result.length, 2);
  assert.equal(result[1].parentId, '1');
});

test('T-97: báo lỗi khi dữ liệu đầu vào không phải danh sách', () => {
  assert.throws(
    () => normalizeEstimateItems(null, 'project-1'),
    /không hợp lệ/,
  );
});
