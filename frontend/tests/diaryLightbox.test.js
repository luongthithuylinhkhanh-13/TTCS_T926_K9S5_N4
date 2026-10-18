import test from 'node:test';
import assert from 'node:assert/strict';
import {
  getDisplayImageUrl,
  getOriginalImageUrl,
} from '../src/services/diaryApi.js';

test('getDisplayImageUrl generates secure path with thumb flag', () => {
  const urlFull = getDisplayImageUrl('proj-1', 'entry-1', 'img-1', false);
  const urlThumb = getDisplayImageUrl('proj-1', 'entry-1', 'img-1', true);

  assert.equal(
    urlFull,
    '/api/projects/proj-1/diary-entries/entry-1/images/img-1/display'
  );
  assert.equal(
    urlThumb,
    '/api/projects/proj-1/diary-entries/entry-1/images/img-1/display?thumb=true'
  );
});

test('getOriginalImageUrl generates direct original download URL preserving bytes', () => {
  const urlOrig = getOriginalImageUrl('proj-1', 'entry-1', 'img-1');
  assert.equal(
    urlOrig,
    '/api/projects/proj-1/diary-entries/entry-1/images/img-1/original'
  );
});

test('getDisplayImageUrl and getOriginalImageUrl append token parameter when token is provided', () => {
  const urlWithToken = getDisplayImageUrl('proj-1', 'entry-1', 'img-1', true, 'test-token-xyz');
  assert.equal(
    urlWithToken,
    '/api/projects/proj-1/diary-entries/entry-1/images/img-1/display?thumb=true&token=test-token-xyz'
  );

  const origWithToken = getOriginalImageUrl('proj-1', 'entry-1', 'img-1', 'test-token-xyz');
  assert.equal(
    origWithToken,
    '/api/projects/proj-1/diary-entries/entry-1/images/img-1/original?token=test-token-xyz'
  );
});

test('Lightbox navigation index wrap-around logic', () => {
  const images = [{ id: '1' }, { id: '2' }, { id: '3' }];

  const computePrev = (current, total) => (current - 1 + total) % total;
  const computeNext = (current, total) => (current + 1) % total;

  assert.equal(computePrev(0, images.length), 2);
  assert.equal(computeNext(2, images.length), 0);
  assert.equal(computeNext(0, images.length), 1);
});

// =========================================================================
// T-70: Kiểm thử hiển thị thời điểm chụp (formatCapturedAt)
// =========================================================================
import { formatCapturedAt } from '../src/utils/imageMetadataExtractor.js';

test('T-70: formatCapturedAt formats ISO string into HH:mm DD/MM/YYYY', () => {
  const formatted = formatCapturedAt('2026-10-18T14:35:00');
  // Date object parses in local timezone
  const d = new Date('2026-10-18T14:35:00');
  const hh = String(d.getHours()).padStart(2, '0');
  const mm = String(d.getMinutes()).padStart(2, '0');
  const expected = `${hh}:${mm} 18/10/2026`;
  assert.equal(formatted, expected);
});

test('T-70: formatCapturedAt returns default fallback when capturedAt is null or empty', () => {
  assert.equal(formatCapturedAt(null), 'Chưa có thời gian chụp');
  assert.equal(formatCapturedAt(undefined), 'Chưa có thời gian chụp');
  assert.equal(formatCapturedAt(''), 'Chưa có thời gian chụp');
  assert.equal(formatCapturedAt('   '), 'Chưa có thời gian chụp');
});

test('T-70: formatCapturedAt handles invalid date strings safely without Invalid Date', () => {
  assert.equal(formatCapturedAt('not-a-valid-date'), 'Chưa có thời gian chụp');
  assert.equal(formatCapturedAt('2026-99-99T99:99:99'), 'Chưa có thời gian chụp');
  assert.notEqual(formatCapturedAt('not-a-valid-date'), 'Invalid Date');
});

test('T-70: formatCapturedAt respects custom fallback option', () => {
  assert.equal(
    formatCapturedAt(null, { fallback: 'Chưa có ngày chụp' }),
    'Chưa có ngày chụp'
  );
});
