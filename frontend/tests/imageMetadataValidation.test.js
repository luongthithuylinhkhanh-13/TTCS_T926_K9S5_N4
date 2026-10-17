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

// =========================================================================
// TEST SUITE: T-75 Xác định và đánh dấu ảnh thiếu metadata (S-42)
// =========================================================================

import {
  METADATA_STATUS,
  METADATA_STATUS_LABELS,
  determineImageMetadataStatus,
} from '../src/utils/imageMetadataExtractor.js';

test('T-75 - Test 1: Ảnh đầy đủ metadata -> Meta: Hợp lệ', () => {
  const fullMeta = {
    fileName: 'full_valid.jpg',
    fileSize: 1024 * 1024,
    mimeType: 'image/jpeg',
    capturedAt: '2026-10-17T08:00:00',
    latitude: 21.0285,
    longitude: 105.8542,
  };

  const result = validateImageMetadata(fullMeta, { requireGps: true });
  assert.equal(result.status, METADATA_STATUS.VALID);
  assert.equal(result.statusLabel, 'Meta: Hợp lệ');
  assert.equal(result.valid, true);
  assert.equal(result.missingFields.length, 0);
  assert.equal(result.invalidFields.length, 0);

  // Kiểm tra qua helper determineImageMetadataStatus
  const statusCheck = determineImageMetadataStatus(fullMeta, { requireGps: true });
  assert.equal(statusCheck.status, 'VALID');
  assert.equal(statusCheck.label, 'Meta: Hợp lệ');
});

test('T-75 - Test 2: Ảnh thiếu metadata bắt buộc -> Meta: Thiếu', () => {
  const missingCaptureTimeMeta = {
    fileName: 'missing_time.jpg',
    fileSize: 500000,
    mimeType: 'image/jpeg',
    capturedAt: null,
    latitude: 21.0,
    longitude: 105.0,
  };

  const result = validateImageMetadata(missingCaptureTimeMeta);
  assert.equal(result.status, METADATA_STATUS.MISSING);
  assert.equal(result.statusLabel, 'Meta: Thiếu');
  assert.equal(result.valid, false);
  assert.ok(result.missingFields.includes('capturedAt'));

  const statusCheck = determineImageMetadataStatus(missingCaptureTimeMeta);
  assert.equal(statusCheck.status, 'MISSING');
  assert.equal(statusCheck.label, 'Meta: Thiếu');
});

test('T-75 - Test 3: Ảnh có metadata nhưng giá trị sai -> Meta: Không hợp lệ', () => {
  // 3a. Thời gian chụp ở tương lai
  const futureMeta = {
    fileName: 'future.jpg',
    fileSize: 500000,
    mimeType: 'image/jpeg',
    capturedAt: '2099-01-01T12:00:00',
    latitude: 21.0,
    longitude: 105.0,
  };
  const futureRes = validateImageMetadata(futureMeta);
  assert.equal(futureRes.status, METADATA_STATUS.INVALID);
  assert.equal(futureRes.statusLabel, 'Meta: Không hợp lệ');
  assert.equal(futureRes.valid, false);
  assert.ok(futureRes.invalidFields.includes('capturedAt'));

  // 3b. Latitude ngoài -90 đến 90
  const badLatMeta = {
    fileName: 'bad_lat.jpg',
    fileSize: 500000,
    mimeType: 'image/jpeg',
    capturedAt: '2026-10-17T09:00:00',
    latitude: 95.0,
    longitude: 105.0,
  };
  const badLatRes = validateImageMetadata(badLatMeta);
  assert.equal(badLatRes.status, METADATA_STATUS.INVALID);
  assert.equal(badLatRes.statusLabel, 'Meta: Không hợp lệ');
  assert.ok(badLatRes.invalidFields.includes('gps'));

  // 3c. Longitude ngoài -180 đến 180
  const badLonMeta = {
    fileName: 'bad_lon.jpg',
    fileSize: 500000,
    mimeType: 'image/jpeg',
    capturedAt: '2026-10-17T09:00:00',
    latitude: 21.0,
    longitude: 200.0,
  };
  const badLonRes = validateImageMetadata(badLonMeta);
  assert.equal(badLonRes.status, METADATA_STATUS.INVALID);
  assert.equal(badLonRes.statusLabel, 'Meta: Không hợp lệ');

  // 3d. Có latitude nhưng không có longitude
  const unpairedGpsMeta = {
    fileName: 'unpaired.jpg',
    fileSize: 500000,
    mimeType: 'image/jpeg',
    capturedAt: '2026-10-17T09:00:00',
    latitude: 21.0,
    longitude: null,
  };
  const unpairedRes = validateImageMetadata(unpairedGpsMeta);
  assert.equal(unpairedRes.status, METADATA_STATUS.INVALID);
  assert.equal(unpairedRes.statusLabel, 'Meta: Không hợp lệ');
  assert.ok(unpairedRes.invalidFields.includes('gps'));
});

test('T-75 - Test 4: Ảnh không có GPS nhưng GPS không bắt buộc -> Không được đánh dấu thiếu GPS', () => {
  const noGpsMeta = {
    fileName: 'no_gps_optional.jpg',
    fileSize: 500000,
    mimeType: 'image/jpeg',
    capturedAt: '2026-10-17T08:00:00',
    latitude: null,
    longitude: null,
  };

  const result = validateImageMetadata(noGpsMeta, { requireGps: false });
  // Không bắt buộc GPS nên ảnh có capturedAt hợp lệ vẫn là Hợp lệ
  assert.equal(result.status, METADATA_STATUS.VALID);
  assert.equal(result.statusLabel, 'Meta: Hợp lệ');
  assert.equal(result.valid, true);
  assert.ok(!result.missingFields.includes('gps'));

  const statusCheck = determineImageMetadataStatus(noGpsMeta, { requireGps: false });
  assert.equal(statusCheck.status, 'VALID');
  assert.equal(statusCheck.label, 'Meta: Hợp lệ');
});

test('T-75 - Test 5: Ảnh không có GPS và requireGps=true -> Meta: Thiếu', () => {
  const noGpsMeta = {
    fileName: 'no_gps_strict.jpg',
    fileSize: 500000,
    mimeType: 'image/jpeg',
    capturedAt: '2026-10-17T08:00:00',
    latitude: null,
    longitude: null,
  };

  const result = validateImageMetadata(noGpsMeta, { requireGps: true });
  assert.equal(result.status, METADATA_STATUS.MISSING);
  assert.equal(result.statusLabel, 'Meta: Thiếu');
  assert.equal(result.valid, false);
  assert.ok(result.missingFields.includes('gps'));

  const statusCheck = determineImageMetadataStatus(noGpsMeta, { requireGps: true });
  assert.equal(statusCheck.status, 'MISSING');
  assert.equal(statusCheck.label, 'Meta: Thiếu');
});

test('T-75: determineImageMetadataStatus đánh giá ảnh tải từ DB (DiaryImageResponse)', () => {
  // Ảnh DB hợp lệ
  const dbImageValid = {
    id: 'img-1',
    originalFilename: 'db_photo_1.jpg',
    originalFileSize: 800000,
    contentType: 'image/jpeg',
    capturedAt: '2026-10-17T08:00:00',
    latitude: 21.0285,
    longitude: 105.8542,
  };
  const res1 = determineImageMetadataStatus(dbImageValid);
  assert.equal(res1.status, 'VALID');
  assert.equal(res1.label, 'Meta: Hợp lệ');

  // Ảnh DB thiếu ngày chụp
  const dbImageMissing = {
    id: 'img-2',
    originalFilename: 'db_photo_2.jpg',
    originalFileSize: 800000,
    contentType: 'image/jpeg',
    capturedAt: null,
    latitude: 21.0285,
    longitude: 105.8542,
  };
  const res2 = determineImageMetadataStatus(dbImageMissing);
  assert.equal(res2.status, 'MISSING');
  assert.equal(res2.label, 'Meta: Thiếu');

  // Ảnh DB thời gian tương lai
  const dbImageFuture = {
    id: 'img-3',
    originalFilename: 'db_photo_3.jpg',
    originalFileSize: 800000,
    contentType: 'image/jpeg',
    capturedAt: '2099-01-01T00:00:00',
    latitude: 21.0285,
    longitude: 105.8542,
  };
  const res3 = determineImageMetadataStatus(dbImageFuture);
  assert.equal(res3.status, 'INVALID');
  assert.equal(res3.label, 'Meta: Không hợp lệ');
});
