import assert from 'node:assert/strict';
import test from 'node:test';
import {
  calculateHaversineDistance,
  evaluatePhotoLocation,
  formatLocationStatus,
  LOCATION_STATUS,
  EARTH_RADIUS_METERS
} from '../src/utils/geoDistanceUtils.js';

test('calculateHaversineDistance returns 0 for identical coordinates', () => {
  const dist = calculateHaversineDistance(21.028511, 105.804817, 21.028511, 105.804817);
  assert.equal(dist, 0);
});

test('calculateHaversineDistance computes accurate distance for 1 degree latitude (~111.195 km)', () => {
  const dist = calculateHaversineDistance(0, 0, 1, 0);
  assert.ok(dist > 111000 && dist < 112000, `Expected ~111km, got ${dist}`);
});

test('calculateHaversineDistance returns NaN for non-numeric coordinates', () => {
  assert.ok(isNaN(calculateHaversineDistance('invalid', 105.8, 21.0, 105.8)));
  assert.ok(isNaN(calculateHaversineDistance(21.0, null, 21.0, 105.8)));
});

test('evaluatePhotoLocation returns PROJECT_NOT_CONFIGURED when site config is missing or invalid', () => {
  // Config null or empty
  assert.equal(evaluatePhotoLocation({ latitude: 21.0, longitude: 105.8 }, null).locationStatus, LOCATION_STATUS.PROJECT_NOT_CONFIGURED);
  assert.equal(evaluatePhotoLocation({ latitude: 21.0, longitude: 105.8 }, {}).locationStatus, LOCATION_STATUS.PROJECT_NOT_CONFIGURED);

  // Missing radius or radius <= 0
  assert.equal(
    evaluatePhotoLocation(
      { latitude: 21.0, longitude: 105.8 },
      { siteLatitude: 21.0, siteLongitude: 105.8, siteRadiusMeters: 0 }
    ).locationStatus,
    LOCATION_STATUS.PROJECT_NOT_CONFIGURED
  );
  assert.equal(
    evaluatePhotoLocation(
      { latitude: 21.0, longitude: 105.8 },
      { siteLatitude: 21.0, siteLongitude: 105.8, siteRadiusMeters: -50 }
    ).locationStatus,
    LOCATION_STATUS.PROJECT_NOT_CONFIGURED
  );
});

test('evaluatePhotoLocation returns NO_GPS when photo coordinates are missing', () => {
  const siteConfig = { siteLatitude: 21.028511, siteLongitude: 105.804817, siteRadiusMeters: 500 };

  const resNull = evaluatePhotoLocation(null, siteConfig);
  assert.equal(resNull.locationStatus, LOCATION_STATUS.NO_GPS);
  assert.equal(resNull.isSuspicious, false);
  assert.equal(resNull.distanceToCenterMeters, null);

  const resEmpty = evaluatePhotoLocation({}, siteConfig);
  assert.equal(resEmpty.locationStatus, LOCATION_STATUS.NO_GPS);
  assert.equal(resEmpty.isSuspicious, false);

  const resLatOnly = evaluatePhotoLocation({ latitude: 21.0 }, siteConfig);
  assert.equal(resLatOnly.locationStatus, LOCATION_STATUS.NO_GPS);
});

test('evaluatePhotoLocation returns INVALID_GPS when coordinates are out of valid range', () => {
  const siteConfig = { siteLatitude: 21.028511, siteLongitude: 105.804817, siteRadiusMeters: 500 };

  // Latitude > 90
  assert.equal(
    evaluatePhotoLocation({ latitude: 91, longitude: 105.8 }, siteConfig).locationStatus,
    LOCATION_STATUS.INVALID_GPS
  );

  // Latitude < -90
  assert.equal(
    evaluatePhotoLocation({ latitude: -90.5, longitude: 105.8 }, siteConfig).locationStatus,
    LOCATION_STATUS.INVALID_GPS
  );

  // Longitude > 180
  assert.equal(
    evaluatePhotoLocation({ latitude: 21.0, longitude: 181 }, siteConfig).locationStatus,
    LOCATION_STATUS.INVALID_GPS
  );

  // Non-numeric
  assert.equal(
    evaluatePhotoLocation({ latitude: 'abc', longitude: 'def' }, siteConfig).locationStatus,
    LOCATION_STATUS.INVALID_GPS
  );
});

test('evaluatePhotoLocation detects photo WITHIN_RADIUS with excess = 0 and isSuspicious = false', () => {
  const siteConfig = { siteLatitude: 21.028511, siteLongitude: 105.804817, siteRadiusMeters: 500 };

  // Photo at exact center
  const atCenter = evaluatePhotoLocation({ latitude: 21.028511, longitude: 105.804817 }, siteConfig);
  assert.equal(atCenter.locationStatus, LOCATION_STATUS.WITHIN_RADIUS);
  assert.equal(atCenter.distanceToCenterMeters, 0);
  assert.equal(atCenter.excessDistanceMeters, 0);
  assert.equal(atCenter.isSuspicious, false);
  assert.match(atCenter.locationMessage, /Trong bán kính công trường/);

  // Photo ~150m away (< 500m)
  const inside = evaluatePhotoLocation({ latitude: 21.0298, longitude: 105.804817 }, siteConfig);
  assert.equal(inside.locationStatus, LOCATION_STATUS.WITHIN_RADIUS);
  assert.ok(inside.distanceToCenterMeters < 500);
  assert.equal(inside.excessDistanceMeters, 0);
  assert.equal(inside.isSuspicious, false);
});

test('evaluatePhotoLocation detects photo OUTSIDE_RADIUS with positive excess and isSuspicious = true', () => {
  const siteConfig = { siteLatitude: 21.028511, siteLongitude: 105.804817, siteRadiusMeters: 500 };

  // Photo ~1.1km away (> 500m)
  const outside = evaluatePhotoLocation({ latitude: 21.038511, longitude: 105.804817 }, siteConfig);
  assert.equal(outside.locationStatus, LOCATION_STATUS.OUTSIDE_RADIUS);
  assert.ok(outside.distanceToCenterMeters > 500);
  assert.ok(outside.excessDistanceMeters > 0);
  assert.equal(outside.isSuspicious, true);
  assert.match(outside.locationMessage, /Ngoài bán kính công trường.*vượt/);
});

test('formatLocationStatus returns appropriate badge, color, and excess distance for OUTSIDE_RADIUS', () => {
  const img = {
    locationStatus: LOCATION_STATUS.OUTSIDE_RADIUS,
    isSuspicious: true,
    distanceToCenterMeters: 620.5,
    excessDistanceMeters: 120.5
  };
  const formatted = formatLocationStatus(img);
  assert.equal(formatted.status, LOCATION_STATUS.OUTSIDE_RADIUS);
  assert.equal(formatted.isSuspicious, true);
  assert.equal(formatted.color, 'error');
  assert.equal(formatted.label, 'Ngoài bán kính');
  assert.equal(formatted.badgeText, '🚨 Ngoài bán kính (+120.5m)');
  assert.equal(formatted.excessDistanceMeters, 120.5);
  assert.equal(formatted.distanceToCenterMeters, 620.5);
});

test('formatLocationStatus returns success badge and excess = 0 for WITHIN_RADIUS', () => {
  const img = {
    locationStatus: LOCATION_STATUS.WITHIN_RADIUS,
    isSuspicious: false,
    distanceToCenterMeters: 250.0,
    excessDistanceMeters: 0.0
  };
  const formatted = formatLocationStatus(img);
  assert.equal(formatted.status, LOCATION_STATUS.WITHIN_RADIUS);
  assert.equal(formatted.isSuspicious, false);
  assert.equal(formatted.color, 'success');
  assert.equal(formatted.label, 'Trong bán kính');
  assert.equal(formatted.badgeText, '✓ Trong bán kính (250m)');
  assert.equal(formatted.excessDistanceMeters, 0);
  assert.equal(formatted.distanceToCenterMeters, 250.0);
});

test('formatLocationStatus does not mark NO_GPS as suspicious', () => {
  const img = {
    locationStatus: LOCATION_STATUS.NO_GPS,
    isSuspicious: false
  };
  const formatted = formatLocationStatus(img);
  assert.equal(formatted.status, LOCATION_STATUS.NO_GPS);
  assert.equal(formatted.isSuspicious, false);
  assert.equal(formatted.color, 'default');
  assert.equal(formatted.label, 'Chưa có GPS');
  assert.equal(formatted.badgeText, '📍 Chưa có GPS');
  assert.equal(formatted.excessDistanceMeters, null);
});

test('formatLocationStatus does not mark PROJECT_NOT_CONFIGURED as suspicious', () => {
  const img = {
    locationStatus: LOCATION_STATUS.PROJECT_NOT_CONFIGURED,
    isSuspicious: false
  };
  const formatted = formatLocationStatus(img);
  assert.equal(formatted.status, LOCATION_STATUS.PROJECT_NOT_CONFIGURED);
  assert.equal(formatted.isSuspicious, false);
  assert.equal(formatted.color, 'default');
  assert.equal(formatted.label, 'Chưa cấu hình vị trí');
  assert.equal(formatted.badgeText, '📍 Chưa cấu hình vị trí');
});

test('formatLocationStatus handles INVALID_GPS correctly', () => {
  const img = {
    locationStatus: LOCATION_STATUS.INVALID_GPS,
    isSuspicious: false
  };
  const formatted = formatLocationStatus(img);
  assert.equal(formatted.status, LOCATION_STATUS.INVALID_GPS);
  assert.equal(formatted.isSuspicious, false);
  assert.equal(formatted.color, 'error');
  assert.equal(formatted.label, 'GPS không hợp lệ');
});

test('formatLocationStatus handles null or empty input gracefully', () => {
  const formattedNull = formatLocationStatus(null);
  assert.equal(formattedNull.isSuspicious, false);
  assert.equal(formattedNull.status, LOCATION_STATUS.PROJECT_NOT_CONFIGURED);

  const formattedEmpty = formatLocationStatus({});
  assert.equal(formattedEmpty.isSuspicious, false);
  assert.equal(formattedEmpty.status, LOCATION_STATUS.PROJECT_NOT_CONFIGURED);
});

