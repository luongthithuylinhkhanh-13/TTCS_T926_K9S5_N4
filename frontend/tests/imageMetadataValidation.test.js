import test from 'node:test';
import assert from 'node:assert/strict';
import {
  parseExifFromBuffer,
  parseExifDateString,
  extractImageMetadata,
  validateImageMetadata,
} from '../src/utils/imageMetadataExtractor.js';
import { createQueueItem } from '../src/utils/imageQueueUtils.js';

test('T-74: parseExifDateString formats EXIF date to ISO string', () => {
  assert.equal(parseExifDateString('2026:10:17 14:30:00'), '2026-10-17T14:30:00');
  assert.equal(parseExifDateString('2026-05-20 08:15:30'), '2026-05-20T08:15:30');
  assert.equal(parseExifDateString(null), null);
  assert.equal(parseExifDateString('invalid-date'), null);
});

test('T-74: validateImageMetadata accepts valid image metadata with capturedAt and GPS', () => {
  const validMeta = {
    fileName: 'site_foundation_beam.jpg',
    fileSize: 1024 * 1024,
    mimeType: 'image/jpeg',
    capturedAt: '2026-10-17T10:00:00',
    latitude: 21.0285,
    longitude: 105.8542,
  };

  const result = validateImageMetadata(validMeta, { requireGps: true });
  assert.equal(result.valid, true);
  assert.equal(result.errors.length, 0);
});

test('T-74: validateImageMetadata detects missing capturedAt', () => {
  const metaWithoutTime = {
    fileName: 'photo.jpg',
    fileSize: 500000,
    mimeType: 'image/jpeg',
    capturedAt: null,
    latitude: 21.0,
    longitude: 105.0,
  };

  const result = validateImageMetadata(metaWithoutTime);
  assert.equal(result.valid, false);
  assert.ok(result.errors.some((err) => err.includes('thiếu thời gian chụp')));
});

test('T-74: validateImageMetadata detects future capture time anomaly', () => {
  const futureMeta = {
    fileName: 'future_photo.jpg',
    fileSize: 500000,
    mimeType: 'image/jpeg',
    capturedAt: '2099-01-01T00:00:00',
    latitude: null,
    longitude: null,
  };

  const result = validateImageMetadata(futureMeta);
  assert.equal(result.valid, false);
  assert.ok(result.errors.some((err) => err.includes('tương lai') || err.includes('bất thường')));
});

test('T-74: validateImageMetadata detects capture time earlier than year 2000', () => {
  const ancientMeta = {
    fileName: 'old_photo.jpg',
    fileSize: 500000,
    mimeType: 'image/jpeg',
    capturedAt: '1980-05-10T12:00:00',
    latitude: null,
    longitude: null,
  };

  const result = validateImageMetadata(ancientMeta);
  assert.equal(result.valid, false);
  assert.ok(result.errors.some((err) => err.includes('quá cũ')));
});

test('T-74: validateImageMetadata enforces GPS when requireGps=true', () => {
  const metaNoGps = {
    fileName: 'no_gps.jpg',
    fileSize: 500000,
    mimeType: 'image/jpeg',
    capturedAt: '2026-10-15T08:00:00',
    latitude: null,
    longitude: null,
  };

  const strictResult = validateImageMetadata(metaNoGps, { requireGps: true });
  assert.equal(strictResult.valid, false);
  assert.ok(strictResult.errors.some((err) => err.includes('thiếu thông tin vị trí GPS')));

  const normalResult = validateImageMetadata(metaNoGps, { requireGps: false });
  assert.equal(normalResult.valid, true);
  assert.ok(normalResult.warnings.some((w) => w.includes('chưa có thông tin tọa độ GPS')));
});

test('T-74: validateImageMetadata rejects out-of-range GPS coordinates', () => {
  const invalidLatMeta = {
    fileName: 'bad_lat.jpg',
    fileSize: 500000,
    mimeType: 'image/jpeg',
    capturedAt: '2026-10-15T08:00:00',
    latitude: 95.5, // > 90
    longitude: 105.0,
  };

  const latResult = validateImageMetadata(invalidLatMeta);
  assert.equal(latResult.valid, false);
  assert.ok(latResult.errors.some((err) => err.includes('Vĩ độ GPS không hợp lệ')));

  const invalidLonMeta = {
    fileName: 'bad_lon.jpg',
    fileSize: 500000,
    mimeType: 'image/jpeg',
    capturedAt: '2026-10-15T08:00:00',
    latitude: 21.0,
    longitude: -200.0, // < -180
  };

  const lonResult = validateImageMetadata(invalidLonMeta);
  assert.equal(lonResult.valid, false);
  assert.ok(lonResult.errors.some((err) => err.includes('Kinh độ GPS không hợp lệ')));
});

test('T-74: validateImageMetadata rejects incomplete GPS pair', () => {
  const partialGps = {
    fileName: 'partial_gps.jpg',
    fileSize: 500000,
    mimeType: 'image/jpeg',
    capturedAt: '2026-10-15T08:00:00',
    latitude: 21.0,
    longitude: null,
  };

  const result = validateImageMetadata(partialGps);
  assert.equal(result.valid, false);
  assert.ok(result.errors.some((err) => err.includes('thiếu một trong hai')));
});

test('T-74: validateImageMetadata rejects empty or oversized files', () => {
  const emptyFile = {
    fileName: 'empty.jpg',
    fileSize: 0,
    mimeType: 'image/jpeg',
    capturedAt: '2026-10-15T08:00:00',
  };
  const emptyRes = validateImageMetadata(emptyFile);
  assert.equal(emptyRes.valid, false);
  assert.ok(emptyRes.errors.some((e) => e.includes('0 byte')));

  const oversizedFile = {
    fileName: 'oversized.jpg',
    fileSize: 25 * 1024 * 1024,
    mimeType: 'image/jpeg',
    capturedAt: '2026-10-15T08:00:00',
  };
  const overRes = validateImageMetadata(oversizedFile);
  assert.equal(overRes.valid, false);
  assert.ok(overRes.errors.some((e) => e.includes('vượt quá')));
});

test('T-74: extractImageMetadata extracts file properties and falls back to lastModified', async () => {
  const mockFile = {
    name: 'test_photo.jpg',
    size: 150000,
    type: 'image/jpeg',
    lastModified: new Date('2026-10-16T09:30:00Z').getTime(),
  };

  const meta = await extractImageMetadata(mockFile);
  assert.equal(meta.fileName, 'test_photo.jpg');
  assert.equal(meta.fileSize, 150000);
  assert.equal(meta.mimeType, 'image/jpeg');
  assert.ok(meta.capturedAt.includes('2026-10-16'));
});

test('T-74: createQueueItem integrates metadata and validation', () => {
  const mockFile = {
    name: 'site_photo_1.jpg',
    size: 200000,
    type: 'image/jpeg',
    lastModified: Date.now() - 3600000,
  };

  const item = createQueueItem(mockFile, () => 'blob:preview-1');
  assert.ok(item.metadata);
  assert.equal(item.metadata.fileName, 'site_photo_1.jpg');
  assert.ok(item.metadataValidation);
  assert.equal(item.metadataValidation.valid, true);
});
