import test from 'node:test';
import assert from 'node:assert/strict';

import {
  calculateVariance,
  calculateActualCostVariance,
} from '../src/services/actualCostVariance.js';

test('T-99: tính đúng khi chi phí thực tế vượt dự toán', () => {
  const result = calculateVariance(1000000, 1200000);

  assert.equal(result.budget, 1000000);
  assert.equal(result.actual, 1200000);
  assert.equal(result.difference, 200000);
  assert.equal(result.percentage, 20);
  assert.equal(result.status, 'OVER_BUDGET');
});

test('T-99: tính đúng khi chi phí thực tế thấp hơn dự toán', () => {
  const result = calculateVariance(1000000, 800000);

  assert.equal(result.difference, -200000);
  assert.equal(result.percentage, -20);
  assert.equal(result.status, 'UNDER_BUDGET');
});

test('T-99: nhận diện khi chi phí bằng dự toán', () => {
  const result = calculateVariance(500000, 500000);

  assert.equal(result.difference, 0);
  assert.equal(result.percentage, 0);
  assert.equal(result.status, 'ON_BUDGET');
});

test('T-99: không chia cho 0 khi dự toán bằng 0', () => {
  const result = calculateVariance(0, 100000);

  assert.equal(result.difference, 100000);
  assert.equal(result.percentage, null);
  assert.equal(result.status, 'OVER_BUDGET');
});

test('T-99: cộng chi phí thực tế theo hạng mục và tổng hợp lên hạng mục cha', () => {
  const budgetTree = [
    {
      id: 'parent',
      name: 'Hạng mục cha',
      totalPrice: 300000,
      children: [
        {
          id: 'child-1',
          name: 'Hạng mục con 1',
          totalPrice: 100000,
          children: [],
        },
        {
          id: 'child-2',
          name: 'Hạng mục con 2',
          totalPrice: 200000,
          children: [],
        },
      ],
    },
  ];

  const actualCosts = [
    { wbsItemId: 'child-1', amount: 120000 },
    { wbsItemId: 'child-2', amount: 180000 },
  ];

  const result = calculateActualCostVariance(budgetTree, actualCosts);

  assert.equal(result.budget, 300000);
  assert.equal(result.actual, 300000);
  assert.equal(result.difference, 0);
  assert.equal(result.items[0].actual, 300000);
  assert.equal(result.items[0].children[0].actual, 120000);
  assert.equal(result.items[0].children[1].actual, 180000);
});

test('T-99: tính tổng dự toán từ các hạng mục gốc, không cộng trùng hạng mục con', () => {
  const budgetTree = [
    {
      id: 'parent',
      totalPrice: 300000,
      children: [
        {
          id: 'child',
          totalPrice: 300000,
          children: [],
        },
      ],
    },
  ];

  const result = calculateActualCostVariance(budgetTree, [
    { wbsItemId: 'child', amount: 250000 },
  ]);

  assert.equal(result.budget, 300000);
  assert.equal(result.actual, 250000);
  assert.equal(result.difference, -50000);
});

test('T-99: vẫn tính tổng chi phí thực tế không gắn với hạng mục WBS', () => {
  const result = calculateActualCostVariance(
    [
      {
        id: 'item-1',
        totalPrice: 100000,
        children: [],
      },
    ],
    [
      { wbsItemId: 'item-1', amount: 80000 },
      { wbsItemId: null, amount: 20000 },
    ],
  );

  assert.equal(result.budget, 100000);
  assert.equal(result.actual, 100000);
  assert.equal(result.items[0].actual, 80000);
});

test('T-99: xử lý dữ liệu đầu vào rỗng', () => {
  const result = calculateActualCostVariance([], []);

  assert.equal(result.budget, 0);
  assert.equal(result.actual, 0);
  assert.equal(result.difference, 0);
  assert.equal(result.percentage, null);
  assert.equal(result.status, 'ON_BUDGET');
});