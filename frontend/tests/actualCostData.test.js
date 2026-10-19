
import assert from 'node:assert/strict';
import test from 'node:test';

import {
  normalizeActualCostData,
} from '../src/services/actualCostData.js';

test('TC-COST-08: T-98 - Chuẩn hóa danh sách chi phí thực tế', () => {
  const result = normalizeActualCostData(
    [
      {
        id: 'cost-1',
        projectId: 'project-1',
        wbsItemId: 'wbs-1',
        wbsItemName: 'Phần móng',
        wbsCode: '1.1',
        name: 'Mua xi măng',
        amount: '1500000',
        costType: 'VẬT_TƯ',
        incurredDate: '2026-10-17',
      },
    ],
    'project-1',
  );

  assert.equal(result.length, 1);
  assert.equal(result[0].projectId, 'project-1');
  assert.equal(result[0].wbsItemId, 'wbs-1');
  assert.equal(result[0].wbsItemName, 'Phần móng');
  assert.equal(result[0].wbsCode, '1.1');
  assert.equal(result[0].amount, 1500000);
});

test('TC-COST-09: T-98 - Chấp nhận danh sách chi phí rỗng', () => {
  assert.deepEqual(normalizeActualCostData([], 'project-1'), []);
});

test('TC-COST-10: T-98 - Báo lỗi khi dữ liệu trả về không phải danh sách', () => {
  assert.throws(
    () => normalizeActualCostData({ items: [] }, 'project-1'),
    /Dữ liệu chi phí thực tế không hợp lệ/,
  );
});

test('TC-COST-11: T-98 - Giữ chi phí chưa liên kết với WBS', () => {
  const result = normalizeActualCostData(
    [
      {
        id: 'cost-2',
        name: 'Chi phí chung',
        amount: 500000,
      },
    ],
    'project-1',
  );

  assert.equal(result[0].projectId, 'project-1');
  assert.equal(result[0].wbsItemId, null);
  assert.equal(result[0].amount, 500000);
});
