import test from 'node:test';
import assert from 'node:assert/strict';
import { MAX_DISPLAY_BYTES, compressImageToDisplay } from '../src/utils/imageCompressor.js';
import { uploadQueueItemStages } from '../src/services/diaryUploadService.js';

test('MAX_DISPLAY_BYTES is strictly 1048576 bytes (1 MiB)', () => {
  assert.equal(MAX_DISPLAY_BYTES, 1048576);
});

test('compressImageToDisplay handles decoding errors gracefully', async () => {
  const invalidFile = {
    name: 'corrupt.jpg',
    size: 500,
    type: 'image/jpeg',
  };

  await assert.rejects(
    async () => {
      await compressImageToDisplay(invalidFile);
    },
    (err) => {
      return err instanceof Error;
    }
  );
});

test('uploadQueueItemStages allows display upload failure while original retries independently', async () => {
  const mockItem = {
    clientImageId: 'img-test-1',
    name: 'photo.jpg',
    size: 2000000,
    file: { name: 'photo.jpg', size: 2000000 },
    displayStatus: 'failed',
    originalStatus: 'idle',
  };

  const updates = [];
  const callbacks = {
    onUpdate: (item) => updates.push({ ...item }),
  };

  let originalUploaded = false;
  const mockApiFns = {
    uploadDisplayImage: async () => {
      throw new Error('Display network error');
    },
    uploadOriginalImage: async () => {
      originalUploaded = true;
      return { success: true };
    },
  };

  // Giả lập chỉ retry original (originalOnly = true)
  const result = await uploadQueueItemStages(
    'p1',
    'e1',
    mockItem,
    callbacks,
    { originalOnly: true },
    mockApiFns
  );

  // displayStatus giữ nguyên failed, originalStatus hoàn tất
  assert.equal(result.displayStatus, 'failed');
  assert.equal(result.originalStatus, 'completed');
  assert.equal(originalUploaded, true);
});

test('uploadQueueItemStages rejects original file larger than 20 MiB', async () => {
  const largeItem = {
    clientImageId: 'img-large',
    name: 'huge.jpg',
    size: 25 * 1024 * 1024,
    file: { name: 'huge.jpg', size: 25 * 1024 * 1024 },
    displayStatus: 'completed',
    originalStatus: 'idle',
  };

  const mockApiFns = {
    uploadDisplayImage: async () => ({ success: true }),
    uploadOriginalImage: async () => ({ success: true }),
  };

  const result = await uploadQueueItemStages('p1', 'e1', largeItem, {}, { originalOnly: true }, mockApiFns);
  assert.equal(result.originalStatus, 'failed');
  assert.match(result.originalError, /20 MiB/);
});
