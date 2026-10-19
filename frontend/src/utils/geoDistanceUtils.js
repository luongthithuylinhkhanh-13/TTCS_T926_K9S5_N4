/**
 * Tiện ích tính toán khoảng cách địa lý Haversine và đánh giá vị trí ảnh so với công trường (S-41 / T-72).
 */

export const EARTH_RADIUS_METERS = 6371000.0; // Bán kính Trái Đất trung bình chuẩn WGS84

export const LOCATION_STATUS = {
  WITHIN_RADIUS: 'WITHIN_RADIUS',
  OUTSIDE_RADIUS: 'OUTSIDE_RADIUS',
  PROJECT_NOT_CONFIGURED: 'PROJECT_NOT_CONFIGURED',
  NO_GPS: 'NO_GPS',
  INVALID_GPS: 'INVALID_GPS'
};

/**
 * Tính khoảng cách địa lý theo công thức Haversine giữa 2 cặp tọa độ (vĩ độ, kinh độ).
 *
 * @param {number|string} lat1 Vĩ độ điểm 1 (độ thập phân)
 * @param {number|string} lon1 Kinh độ điểm 1 (độ thập phân)
 * @param {number|string} lat2 Vĩ độ điểm 2 (độ thập phân)
 * @param {number|string} lon2 Kinh độ điểm 2 (độ thập phân)
 * @returns {number} Khoảng cách bằng mét, làm tròn 1 chữ số thập phân
 */
export const calculateHaversineDistance = (lat1, lon1, lat2, lon2) => {
  if (
    lat1 === null || lat1 === undefined || lat1 === '' ||
    lon1 === null || lon1 === undefined || lon1 === '' ||
    lat2 === null || lat2 === undefined || lat2 === '' ||
    lon2 === null || lon2 === undefined || lon2 === ''
  ) {
    return NaN;
  }

  const p1Lat = Number(lat1);
  const p1Lon = Number(lon1);
  const p2Lat = Number(lat2);
  const p2Lon = Number(lon2);

  if (
    isNaN(p1Lat) || isNaN(p1Lon) ||
    isNaN(p2Lat) || isNaN(p2Lon)
  ) {
    return NaN;
  }

  const toRad = deg => (deg * Math.PI) / 180.0;

  const dLat = toRad(p2Lat - p1Lat);
  const dLon = toRad(p2Lon - p1Lon);

  const a =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos(toRad(p1Lat)) * Math.cos(toRad(p2Lat)) *
    Math.sin(dLon / 2) * Math.sin(dLon / 2);

  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  const distance = EARTH_RADIUS_METERS * c;

  return Math.round(distance * 10) / 10;
};

/**
 * Đánh giá vị trí ảnh so với cấu hình bán kính công trường.
 *
 * @param {{ latitude?: number|string|null, longitude?: number|string|null }} photoCoords
 * @param {{ siteLatitude?: number|string|null, siteLongitude?: number|string|null, siteRadiusMeters?: number|string|null, latitude?: number|string|null, longitude?: number|string|null, radiusMeters?: number|string|null }} siteConfig
 * @returns {{
 *   locationStatus: string,
 *   distanceToCenterMeters: number|null,
 *   excessDistanceMeters: number|null,
 *   isSuspicious: boolean,
 *   locationMessage: string
 * }}
 */
export const evaluatePhotoLocation = (photoCoords, siteConfig) => {
  const siteLat = siteConfig?.siteLatitude ?? siteConfig?.latitude;
  const siteLon = siteConfig?.siteLongitude ?? siteConfig?.longitude;
  const siteRadius = siteConfig?.siteRadiusMeters ?? siteConfig?.radiusMeters;

  // 1. Kiểm tra cấu hình công trường
  if (
    siteLat === undefined || siteLat === null || siteLat === '' ||
    siteLon === undefined || siteLon === null || siteLon === '' ||
    siteRadius === undefined || siteRadius === null || siteRadius === ''
  ) {
    return {
      locationStatus: LOCATION_STATUS.PROJECT_NOT_CONFIGURED,
      distanceToCenterMeters: null,
      excessDistanceMeters: null,
      isSuspicious: false,
      locationMessage: 'Dự án chưa cấu hình vị trí công trường'
    };
  }

  const numSiteLat = Number(siteLat);
  const numSiteLon = Number(siteLon);
  const numRadius = Number(siteRadius);

  if (isNaN(numSiteLat) || isNaN(numSiteLon) || isNaN(numRadius) || numRadius <= 0) {
    return {
      locationStatus: LOCATION_STATUS.PROJECT_NOT_CONFIGURED,
      distanceToCenterMeters: null,
      excessDistanceMeters: null,
      isSuspicious: false,
      locationMessage: 'Dự án chưa cấu hình vị trí công trường hợp lệ'
    };
  }

  // 2. Kiểm tra tọa độ GPS của ảnh
  const photoLat = photoCoords?.latitude;
  const photoLon = photoCoords?.longitude;

  if (
    photoLat === undefined || photoLat === null || photoLat === '' ||
    photoLon === undefined || photoLon === null || photoLon === ''
  ) {
    return {
      locationStatus: LOCATION_STATUS.NO_GPS,
      distanceToCenterMeters: null,
      excessDistanceMeters: null,
      isSuspicious: false,
      locationMessage: 'Ảnh không có thông tin tọa độ GPS'
    };
  }

  const numPhotoLat = Number(photoLat);
  const numPhotoLon = Number(photoLon);

  if (
    isNaN(numPhotoLat) || numPhotoLat < -90 || numPhotoLat > 90 ||
    isNaN(numPhotoLon) || numPhotoLon < -180 || numPhotoLon > 180
  ) {
    return {
      locationStatus: LOCATION_STATUS.INVALID_GPS,
      distanceToCenterMeters: null,
      excessDistanceMeters: null,
      isSuspicious: false,
      locationMessage: 'Tọa độ GPS của ảnh không hợp lệ'
    };
  }

  // 3. Tính khoảng cách
  const distance = calculateHaversineDistance(numPhotoLat, numPhotoLon, numSiteLat, numSiteLon);

  // 4. So sánh với bán kính
  if (distance <= numRadius) {
    return {
      locationStatus: LOCATION_STATUS.WITHIN_RADIUS,
      distanceToCenterMeters: distance,
      excessDistanceMeters: 0.0,
      isSuspicious: false,
      locationMessage: `Trong bán kính công trường (cách tâm ${distance}m, bán kính ${numRadius}m)`
    };
  } else {
    const rawExcess = distance - numRadius;
    const excess = Math.max(0.0, Math.round(rawExcess * 10) / 10);
    return {
      locationStatus: LOCATION_STATUS.OUTSIDE_RADIUS,
      distanceToCenterMeters: distance,
      excessDistanceMeters: excess,
      isSuspicious: true,
      locationMessage: `Ngoài bán kính công trường (vượt ${excess}m so với bán kính ${numRadius}m)`
    };
  }
};

/**
 * Định dạng thông tin hiển thị trạng thái vị trí và cờ nghi vấn của ảnh (T-73).
 *
 * @param {object} image Đối tượng ảnh (DiaryImageResponse hoặc kết quả evaluatePhotoLocation)
 * @returns {{
 *   status: string,
 *   label: string,
 *   badgeText: string,
 *   color: string,
 *   isSuspicious: boolean,
 *   message: string,
 *   distanceToCenterMeters: number|null,
 *   excessDistanceMeters: number|null
 * }}
 */
export const formatLocationStatus = (image) => {
  if (!image) {
    return {
      status: LOCATION_STATUS.PROJECT_NOT_CONFIGURED,
      label: 'Chưa xác định',
      badgeText: '📍 Vị trí: Chưa xác định',
      color: 'default',
      isSuspicious: false,
      message: '',
      distanceToCenterMeters: null,
      excessDistanceMeters: null
    };
  }

  const isSuspicious = Boolean(image.isSuspicious);
  const status = image.locationStatus || (isSuspicious ? LOCATION_STATUS.OUTSIDE_RADIUS : LOCATION_STATUS.PROJECT_NOT_CONFIGURED);
  const distance = typeof image.distanceToCenterMeters === 'number' ? image.distanceToCenterMeters : null;
  const excess = typeof image.excessDistanceMeters === 'number' ? image.excessDistanceMeters : null;

  switch (status) {
    case LOCATION_STATUS.WITHIN_RADIUS:
      return {
        status,
        label: 'Trong bán kính',
        badgeText: distance != null ? `✓ Trong bán kính (${distance}m)` : '✓ Trong bán kính',
        color: 'success',
        isSuspicious: false,
        message: image.locationMessage || `Ảnh chụp trong bán kính công trường${distance != null ? ` (cách tâm ${distance}m)` : ''}`,
        distanceToCenterMeters: distance,
        excessDistanceMeters: 0
      };

    case LOCATION_STATUS.OUTSIDE_RADIUS:
      return {
        status,
        label: 'Ngoài bán kính',
        badgeText: excess != null ? `🚨 Ngoài bán kính (+${excess}m)` : '🚨 Ngoài bán kính',
        color: 'error',
        isSuspicious: true,
        message: image.locationMessage || `Ảnh chụp ngoài bán kính công trường${excess != null ? ` (vượt ${excess}m)` : ''}`,
        distanceToCenterMeters: distance,
        excessDistanceMeters: excess
      };

    case LOCATION_STATUS.NO_GPS:
      return {
        status,
        label: 'Chưa có GPS',
        badgeText: '📍 Chưa có GPS',
        color: 'default',
        isSuspicious: false,
        message: image.locationMessage || 'Ảnh không có thông tin tọa độ GPS để kiểm tra bán kính',
        distanceToCenterMeters: null,
        excessDistanceMeters: null
      };

    case LOCATION_STATUS.INVALID_GPS:
      return {
        status,
        label: 'GPS không hợp lệ',
        badgeText: '⚠ GPS không hợp lệ',
        color: 'error',
        isSuspicious: false,
        message: image.locationMessage || 'Tọa độ GPS của ảnh nằm ngoài phạm vi hợp lệ',
        distanceToCenterMeters: null,
        excessDistanceMeters: null
      };

    case LOCATION_STATUS.PROJECT_NOT_CONFIGURED:
    default:
      return {
        status: status || LOCATION_STATUS.PROJECT_NOT_CONFIGURED,
        label: 'Chưa cấu hình vị trí',
        badgeText: '📍 Chưa cấu hình vị trí',
        color: 'default',
        isSuspicious: false,
        message: image.locationMessage || 'Dự án chưa cấu hình vị trí và bán kính công trường',
        distanceToCenterMeters: null,
        excessDistanceMeters: null
      };
  }
};
