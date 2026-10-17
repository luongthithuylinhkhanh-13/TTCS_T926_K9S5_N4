import assert from 'node:assert/strict';
import test from 'node:test';
import { validateRetentionRate } from '../src/services/acceptanceApi.js';

test('T-85 (S-45): retentionRate = 5 -> hợp lệ', () => {
  const result = validateRetentionRate(5);
  assert.equal(result.valid, true);
  assert.equal(result.value, 5);
});

test('T-85 (S-45): retentionRate = 0 -> hợp lệ', () => {
  const result = validateRetentionRate(0);
  assert.equal(result.valid, true);
  assert.equal(result.value, 0);
});

test('T-85 (S-45): retentionRate = 100 -> hợp lệ', () => {
  const result = validateRetentionRate(100);
  assert.equal(result.valid, true);
  assert.equal(result.value, 100);
});

test('T-85 (S-45): retentionRate = 7.5 -> hợp lệ', () => {
  const result = validateRetentionRate(7.5);
  assert.equal(result.valid, true);
  assert.equal(result.value, 7.5);
});

test('T-85 (S-45): retentionRate = -1 -> từ chối', () => {
  const result = validateRetentionRate(-1);
  assert.equal(result.valid, false);
  assert.equal(result.error, 'Tỷ lệ tạm giữ phải từ 0% đến 100%');
});

test('T-85 (S-45): retentionRate = 101 -> từ chối', () => {
  const result = validateRetentionRate(101);
  assert.equal(result.valid, false);
  assert.equal(result.error, 'Tỷ lệ tạm giữ phải từ 0% đến 100%');
});

test('T-85 (S-45): retentionRate là chuỗi chữ "abc" -> frontend không chấp nhận', () => {
  const result = validateRetentionRate('abc');
  assert.equal(result.valid, false);
  assert.equal(result.error, 'Tỷ lệ tạm giữ phải là số hợp lệ');
});

test('T-85 (S-45): retentionRate rỗng / null / undefined -> từ chối do là trường bắt buộc', () => {
  assert.equal(validateRetentionRate('').valid, false);
  assert.equal(validateRetentionRate(null).valid, false);
  assert.equal(validateRetentionRate(undefined).valid, false);
  assert.equal(validateRetentionRate('').error, 'Vui lòng nhập tỷ lệ tạm giữ theo hợp đồng');
});

test('T-85 (S-45): retentionRate dạng chuỗi số "5.00" hoặc "7.5" -> hợp lệ', () => {
  const res1 = validateRetentionRate('5.00');
  assert.equal(res1.valid, true);
  assert.equal(res1.value, 5);

  const res2 = validateRetentionRate('7.5');
  assert.equal(res2.valid, true);
  assert.equal(res2.value, 7.5);
});
