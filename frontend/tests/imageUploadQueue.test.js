
import assert from 'node:assert/strict';
import test from 'node:test';
import imageUploadQueueService from '../src/services/imageUploadQueueService.js';

test('T-76: từ chối khi không có file ảnh', () => {
  assert.throws(
    () => imageUploadQueueService.validateFile(null),
    /Vui lòng chọn ảnh/
  );
});

test('T-76: từ chối file không phải định dạng ảnh được hỗ trợ', () => {
  const file = {
    name: 'document.pdf',
    type: 'application/pdf',
    size: 1024
  };

  assert.throws(
    () => imageUploadQueueService.validateFile(file),
    /Chỉ chấp nhận ảnh/
  );
});

test('T-76: từ chối ảnh lớn hơn 20 MB', () => {
  const file = {
    name: 'large-image.jpg',
    type: 'image/jpeg',
    size: 20 * 1024 * 1024 + 1
  };

  assert.throws(
    () => imageUploadQueueService.validateFile(file),
    /vượt quá giới hạn 20 MB/
  );
});

test('T-76: chấp nhận ảnh JPEG hợp lệ', () => {
  const file = {
    name: 'site-photo.jpg',
    type: 'image/jpeg',
    size: 1024
  };

  assert.equal(imageUploadQueueService.validateFile(file), true);
});
