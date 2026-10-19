import assert from 'node:assert/strict';
import test from 'node:test';
import { validateSiteLocation, updateProjectSiteLocation } from '../src/services/projectLocationApi.js';

test('validateSiteLocation accepts valid coordinates and positive radius', () => {
  const result = validateSiteLocation({
    latitude: 21.028511,
    longitude: 105.804817,
    radiusMeters: 500
  });

  assert.equal(result.valid, true);
  assert.equal(result.errors.length, 0);
});

test('validateSiteLocation accepts valid numeric strings and boundary values', () => {
  // Boundary tests: lat [-90, 90], lon [-180, 180], min radius > 0
  const minBoundary = validateSiteLocation({
    latitude: '-90',
    longitude: '-180',
    radiusMeters: '0.1'
  });
  assert.equal(minBoundary.valid, true);

  const maxBoundary = validateSiteLocation({
    latitude: 90,
    longitude: 180,
    radiusMeters: 10000
  });
  assert.equal(maxBoundary.valid, true);
});

test('validateSiteLocation rejects null, empty or missing config', () => {
  assert.equal(validateSiteLocation(null).valid, false);
  assert.equal(validateSiteLocation(undefined).valid, false);

  const emptyConfig = validateSiteLocation({});
  assert.equal(emptyConfig.valid, false);
  assert.equal(emptyConfig.errors.length, 3);
});

test('validateSiteLocation rejects out-of-range latitude', () => {
  const tooLow = validateSiteLocation({
    latitude: -90.0001,
    longitude: 105.8,
    radiusMeters: 200
  });
  assert.equal(tooLow.valid, false);
  assert.match(tooLow.errors[0], /Vĩ độ phải là số hợp lệ/);

  const tooHigh = validateSiteLocation({
    latitude: 90.0001,
    longitude: 105.8,
    radiusMeters: 200
  });
  assert.equal(tooHigh.valid, false);
});

test('validateSiteLocation rejects out-of-range longitude', () => {
  const tooLow = validateSiteLocation({
    latitude: 21.0,
    longitude: -180.0001,
    radiusMeters: 200
  });
  assert.equal(tooLow.valid, false);
  assert.match(tooLow.errors[0], /Kinh độ phải là số hợp lệ/);

  const tooHigh = validateSiteLocation({
    latitude: 21.0,
    longitude: 180.0001,
    radiusMeters: 200
  });
  assert.equal(tooHigh.valid, false);
});

test('validateSiteLocation rejects non-positive radius', () => {
  const zeroRadius = validateSiteLocation({
    latitude: 21.0,
    longitude: 105.8,
    radiusMeters: 0
  });
  assert.equal(zeroRadius.valid, false);
  assert.match(zeroRadius.errors[0], /Bán kính công trường phải là số dương lớn hơn 0/);

  const negativeRadius = validateSiteLocation({
    latitude: 21.0,
    longitude: 105.8,
    radiusMeters: -50
  });
  assert.equal(negativeRadius.valid, false);
});

test('validateSiteLocation rejects non-numeric inputs', () => {
  const invalid = validateSiteLocation({
    latitude: 'abc',
    longitude: 'def',
    radiusMeters: 'xyz'
  });
  assert.equal(invalid.valid, false);
  assert.equal(invalid.errors.length, 3);
});

test('updateProjectSiteLocation throws early when payload is invalid without sending network request', async () => {
  await assert.rejects(
    async () => {
      await updateProjectSiteLocation('proj-123', {
        latitude: 95,
        longitude: 105,
        radiusMeters: -10
      });
    },
    /Vĩ độ phải là số hợp lệ.*Bán kính công trường phải là số dương/
  );
});
