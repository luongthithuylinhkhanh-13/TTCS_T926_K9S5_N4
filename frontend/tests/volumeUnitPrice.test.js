import assert from 'node:assert/strict';
import test from 'node:test';

// Helper tính toán thành tiền dự toán và validate khối lượng / đơn giá
function calculateTotalPrice(quantity, unitPrice) {
  const qty = Number(quantity);
  const price = Number(unitPrice);
  if (isNaN(qty) || isNaN(price) || qty <= 0 || price < 0) {
    return 0;
  }
  return Math.round(qty * price * 100) / 100;
}

function validateVolumeAndUnitPrice({ quantity, unit, unitPrice }) {
  const errors = [];
  if (quantity != null) {
    const numQty = Number(quantity);
    if (isNaN(numQty) || numQty < 0) {
      errors.push('Khối lượng phải lớn hơn hoặc bằng 0');
    }
    if (numQty > 0 && (!unit || !unit.trim())) {
      errors.push('Vui lòng nhập đơn vị tính khi đã có khối lượng');
    }
  }
  if (unit != null && unit.length > 50) {
    errors.push('Đơn vị tính không quá 50 ký tự');
  }
  if (unitPrice != null) {
    const numPrice = Number(unitPrice);
    if (isNaN(numPrice) || numPrice < 0) {
      errors.push('Đơn giá phải lớn hơn hoặc bằng 0');
    }
  }
  return errors;
}

test('TC-VOL-01: T-80 - Tính chính xác thành tiền dự toán = khối lượng * đơn giá', () => {
  const total = calculateTotalPrice(120.5, 250000);
  assert.equal(total, 30125000);
});

test('TC-VOL-02: T-80 - Thành tiền trả về 0 khi khối lượng hoặc đơn giá chưa nhập hoặc bằng 0', () => {
  assert.equal(calculateTotalPrice(null, 250000), 0);
  assert.equal(calculateTotalPrice(100, null), 0);
  assert.equal(calculateTotalPrice(0, 50000), 0);
});

test('TC-VOL-03: T-82 - Chấp nhận dữ liệu khối lượng, ĐVT và đơn giá hợp lệ', () => {
  const errors = validateVolumeAndUnitPrice({
    quantity: 50.5,
    unit: 'm3',
    unitPrice: 1200000
  });
  assert.equal(errors.length, 0);
});

test('TC-VOL-04: T-82 - Phát hiện lỗi khi khối lượng âm', () => {
  const errors = validateVolumeAndUnitPrice({
    quantity: -5,
    unit: 'm2',
    unitPrice: 100000
  });
  assert.ok(errors.includes('Khối lượng phải lớn hơn hoặc bằng 0'));
});

test('TC-VOL-05: T-82 - Phát hiện lỗi khi đơn giá âm', () => {
  const errors = validateVolumeAndUnitPrice({
    quantity: 10,
    unit: 'tấn',
    unitPrice: -5000
  });
  assert.ok(errors.includes('Đơn giá phải lớn hơn hoặc bằng 0'));
});

test('TC-VOL-06: T-82 - Bắt buộc đơn vị tính khi khối lượng > 0', () => {
  const errors = validateVolumeAndUnitPrice({
    quantity: 10,
    unit: '   ',
    unitPrice: 50000
  });
  assert.ok(errors.includes('Vui lòng nhập đơn vị tính khi đã có khối lượng'));
});

test('TC-VOL-07: T-82 - Phát hiện đơn vị tính dài quá 50 ký tự', () => {
  const errors = validateVolumeAndUnitPrice({
    quantity: 10,
    unit: 'x'.repeat(51),
    unitPrice: 50000
  });
  assert.ok(errors.includes('Đơn vị tính không quá 50 ký tự'));
});
