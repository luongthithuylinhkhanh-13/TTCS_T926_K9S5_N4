import assert from 'node:assert/strict';
import test from 'node:test';
import { validateRetentionRate, calculateRetentionAmount } from '../src/services/acceptanceApi.js';

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

// ==========================================
// T-86: TỰ ĐỘNG TÍNH SỐ TIỀN TẠM GIỮ
// ==========================================

test('T-86 (S-45) CASE 1: 100,000,000 x 5% = 5,000,000 VNĐ', () => {
  const amount = calculateRetentionAmount(100000000, 5);
  assert.equal(amount, 5000000);
});

test('T-86 (S-45) CASE 2: 100,000,000 x 7.5% = 7,500,000 VNĐ', () => {
  const amount = calculateRetentionAmount(100000000, 7.5);
  assert.equal(amount, 7500000);
});

test('T-86 (S-45) CASE 3: 100,000,000 x 0% = 0 VNĐ', () => {
  const amount = calculateRetentionAmount(100000000, 0);
  assert.equal(amount, 0);
});

test('T-86 (S-45) CASE 4: 0 VNĐ x 7.5% = 0 VNĐ', () => {
  const amount = calculateRetentionAmount(0, 7.5);
  assert.equal(amount, 0);
});

test('T-86 (S-45) CASE 5: 50,000,000 x 10% = 5,000,000 VNĐ', () => {
  const amount = calculateRetentionAmount(50000000, 10);
  assert.equal(amount, 5000000);
});

test('T-86 (S-45) CASE 6: Rounding với số thập phân (100.55 x 7.5% = 7.54)', () => {
  // 100.55 * 0.075 = 7.54125 -> làm tròn 7.54
  const amount = calculateRetentionAmount(100.55, 7.5);
  assert.equal(amount, 7.54);
});

test('T-86 (S-45) CASE 7: Thay đổi tỷ lệ từ 5% -> 7.5% cập nhật số tiền tương ứng', () => {
  const total = 100000000;
  let rate = 5;
  let retention = calculateRetentionAmount(total, rate);
  assert.equal(retention, 5000000);

  rate = 7.5;
  retention = calculateRetentionAmount(total, rate);
  assert.equal(retention, 7500000);
});

test('T-86 (S-45) CASE 8: Giá trị âm hoặc không hợp lệ -> trả về 0 (tránh NaN, Infinity)', () => {
  assert.equal(calculateRetentionAmount(-100, 5), 0);
  assert.equal(calculateRetentionAmount(100, -5), 0);
  assert.equal(calculateRetentionAmount('abc', 5), 0);
  assert.equal(calculateRetentionAmount(100, 'xyz'), 0);
  assert.equal(calculateRetentionAmount(null, undefined), 0);
});
