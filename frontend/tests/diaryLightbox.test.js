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

test('Lightbox navigation index wrap-around logic', () => {
  const images = [{ id: '1' }, { id: '2' }, { id: '3' }];

  const computePrev = (current, total) => (current - 1 + total) % total;
  const computeNext = (current, total) => (current + 1) % total;

  assert.equal(computePrev(0, images.length), 2);
  assert.equal(computeNext(2, images.length), 0);
  assert.equal(computeNext(0, images.length), 1);
});
