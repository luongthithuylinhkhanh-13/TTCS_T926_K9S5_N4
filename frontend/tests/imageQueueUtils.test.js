import test from 'node:test';
import assert from 'node:assert/strict';
import {
  formatFileSize,
  createQueueItem,
  revokeQueueItem,
  revokeAllQueueItems,
} from '../src/utils/imageQueueUtils.js';

test('formatFileSize formats byte amounts accurately', () => {
  assert.equal(formatFileSize(null), '0 B');
  assert.equal(formatFileSize(undefined), '0 B');
  assert.equal(formatFileSize(0), '0 B');
  assert.equal(formatFileSize(500), '500 B');
  assert.equal(formatFileSize(1024), '1.0 KB');
  assert.equal(formatFileSize(1536), '1.5 KB');
  assert.equal(formatFileSize(1048576), '1.0 MB');
  assert.equal(formatFileSize(5242880), '5.0 MB');
});

test('createQueueItem attaches clientImageId, metadata, and generates preview URL', () => {
  const dummyFile = {
    name: 'site-photo.jpg',
    size: 204800,
    type: 'image/jpeg',
  };

  const createdUrls = [];
  const fakeCreateObjectURL = (f) => {
    const url = `blob:http://localhost/${f.name}-${Date.now()}`;
    createdUrls.push(url);
    return url;
  };

  const item = createQueueItem(dummyFile, fakeCreateObjectURL);

  assert.ok(item.clientImageId, 'clientImageId must be generated');
  assert.equal(item.name, 'site-photo.jpg');
  assert.equal(item.size, 204800);
  assert.equal(item.type, 'image/jpeg');
  assert.ok(item.previewUrl.startsWith('blob:http://localhost/site-photo.jpg'));
  assert.equal(createdUrls.length, 1);
});

test('revokeQueueItem calls revokeObjectURL for item preview URL', () => {
  const revokedUrls = [];
  const fakeRevokeObjectURL = (url) => {
    revokedUrls.push(url);
  };

  const item = {
    clientImageId: 'img-1',
    previewUrl: 'blob:http://localhost/photo1',
  };

  revokeQueueItem(item, fakeRevokeObjectURL);
  assert.deepEqual(revokedUrls, ['blob:http://localhost/photo1']);
});

test('revokeAllQueueItems safely revokes all URLs in array', () => {
  const revokedUrls = [];
  const fakeRevokeObjectURL = (url) => {
    revokedUrls.push(url);
  };

  const items = [
    { clientImageId: '1', previewUrl: 'blob:test/1' },
    { clientImageId: '2', previewUrl: 'blob:test/2' },
    { clientImageId: '3', previewUrl: null },
  ];

  revokeAllQueueItems(items, fakeRevokeObjectURL);
  assert.deepEqual(revokedUrls, ['blob:test/1', 'blob:test/2']);
});
