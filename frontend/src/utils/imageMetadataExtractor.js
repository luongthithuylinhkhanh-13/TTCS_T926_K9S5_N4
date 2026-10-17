/**
 * Tiện ích trích xuất và kiểm tra siêu dữ liệu (metadata) của ảnh hiện trường (T-74 / S-42).
 */

/**
 * Trích xuất metadata EXIF từ buffer ảnh JPEG (nếu có).
 * @param {ArrayBuffer} buffer
 * @returns {{ capturedAt: string|null, latitude: number|null, longitude: number|null }}
 */
export function parseExifFromBuffer(buffer) {
  const result = {
    capturedAt: null,
    latitude: null,
    longitude: null,
  };

  if (!buffer || buffer.byteLength < 16) {
    return result;
  }

  const view = new DataView(buffer);

  // Kiểm tra SOI (0xFFD8)
  if (view.getUint16(0, false) !== 0xFFD8) {
    return result;
  }

  let offset = 2;
  const length = buffer.byteLength;

  while (offset + 4 < length) {
    const marker = view.getUint16(offset, false);
    offset += 2;

    if (marker === 0xFFE1) { // APP1 Marker
      const app1Length = view.getUint16(offset, false);
      const app1Start = offset + 2;

      // Kiểm tra header "Exif\0\0"
      if (
        app1Start + 6 <= length &&
        view.getUint8(app1Start) === 0x45 && // E
        view.getUint8(app1Start + 1) === 0x78 && // x
        view.getUint8(app1Start + 2) === 0x69 && // i
        view.getUint8(app1Start + 3) === 0x66 && // f
        view.getUint8(app1Start + 4) === 0x00 &&
        view.getUint8(app1Start + 5) === 0x00
      ) {
        const tiffHeaderOffset = app1Start + 6;
        parseTiffHeader(view, tiffHeaderOffset, length, result);
      }
      break;
    } else if ((marker & 0xFF00) === 0xFF00 && marker !== 0xFFD8 && marker !== 0xFFD9) {
      // Bỏ qua các marker khác
      const markerLength = view.getUint16(offset, false);
      offset += markerLength;
    } else {
      break;
    }
  }

  return result;
}

function parseTiffHeader(view, tiffStart, maxLen, result) {
  if (tiffStart + 8 > maxLen) return;

  const byteOrderCode = view.getUint16(tiffStart, false);
  let littleEndian;
  if (byteOrderCode === 0x4949) { // "II"
    littleEndian = true;
  } else if (byteOrderCode === 0x4D4D) { // "MM"
    littleEndian = false;
  } else {
    return;
  }

  const fortyTwo = view.getUint16(tiffStart + 2, littleEndian);
  if (fortyTwo !== 42) return;

  const firstIfdOffset = view.getUint32(tiffStart + 4, littleEndian);
  if (firstIfdOffset < 8) return;

  let exifIfdOffset = null;
  let gpsIfdOffset = null;

  // Đọc IFD0
  const ifd0Start = tiffStart + firstIfdOffset;
  if (ifd0Start + 2 <= maxLen) {
    const numEntries = view.getUint16(ifd0Start, littleEndian);
    let entryOffset = ifd0Start + 2;

    for (let i = 0; i < numEntries && entryOffset + 12 <= maxLen; i++, entryOffset += 12) {
      const tag = view.getUint16(entryOffset, littleEndian);
      if (tag === 0x0132 && !result.capturedAt) { // DateTime
        result.capturedAt = parseExifDateString(readAsciiString(view, entryOffset, tiffStart, maxLen, littleEndian));
      } else if (tag === 0x8769) { // Exif IFD pointer
        exifIfdOffset = view.getUint32(entryOffset + 8, littleEndian);
      } else if (tag === 0x8825) { // GPS IFD pointer
        gpsIfdOffset = view.getUint32(entryOffset + 8, littleEndian);
      }
    }
  }

  // Đọc Exif IFD (chứa DateTimeOriginal 0x9003)
  if (exifIfdOffset && tiffStart + exifIfdOffset + 2 <= maxLen) {
    const exifStart = tiffStart + exifIfdOffset;
    const numEntries = view.getUint16(exifStart, littleEndian);
    let entryOffset = exifStart + 2;

    for (let i = 0; i < numEntries && entryOffset + 12 <= maxLen; i++, entryOffset += 12) {
      const tag = view.getUint16(entryOffset, littleEndian);
      if (tag === 0x9003 || tag === 0x9004) { // DateTimeOriginal / DateTimeDigitized
        const dateStr = readAsciiString(view, entryOffset, tiffStart, maxLen, littleEndian);
        const parsed = parseExifDateString(dateStr);
        if (parsed) {
          result.capturedAt = parsed;
          break;
        }
      }
    }
  }

  // Đọc GPS IFD
  if (gpsIfdOffset && tiffStart + gpsIfdOffset + 2 <= maxLen) {
    const gpsStart = tiffStart + gpsIfdOffset;
    const numEntries = view.getUint16(gpsStart, littleEndian);
    let entryOffset = gpsStart + 2;

    let latRef = null;
    let lonRef = null;
    let latCoords = null;
    let lonCoords = null;

    for (let i = 0; i < numEntries && entryOffset + 12 <= maxLen; i++, entryOffset += 12) {
      const tag = view.getUint16(entryOffset, littleEndian);
      if (tag === 0x0001) { // GPSLatitudeRef
        latRef = String.fromCharCode(view.getUint8(entryOffset + 8));
      } else if (tag === 0x0002) { // GPSLatitude
        latCoords = readRationals(view, entryOffset, tiffStart, 3, littleEndian);
      } else if (tag === 0x0003) { // GPSLongitudeRef
        lonRef = String.fromCharCode(view.getUint8(entryOffset + 8));
      } else if (tag === 0x0004) { // GPSLongitude
        lonCoords = readRationals(view, entryOffset, tiffStart, 3, littleEndian);
      }
    }

    if (latCoords && latCoords.length === 3) {
      let lat = latCoords[0] + latCoords[1] / 60 + latCoords[2] / 3600;
      if (latRef === 'S') lat = -lat;
      result.latitude = Math.round(lat * 1000000) / 1000000;
    }

    if (lonCoords && lonCoords.length === 3) {
      let lon = lonCoords[0] + lonCoords[1] / 60 + lonCoords[2] / 3600;
      if (lonRef === 'W') lon = -lon;
      result.longitude = Math.round(lon * 1000000) / 1000000;
    }
  }
}

function readAsciiString(view, entryOffset, tiffStart, maxLen, littleEndian) {
  const count = view.getUint32(entryOffset + 4, littleEndian);
  if (count <= 0) return null;

  let strOffset = entryOffset + 8;
  if (count > 4) {
    strOffset = tiffStart + view.getUint32(entryOffset + 8, littleEndian);
  }

  if (strOffset + count > maxLen) return null;

  let chars = [];
  for (let i = 0; i < count; i++) {
    const c = view.getUint8(strOffset + i);
    if (c === 0) break; // null terminator
    chars.push(String.fromCharCode(c));
  }
  return chars.join('').trim();
}

function readRationals(view, entryOffset, tiffStart, count, littleEndian) {
  const offset = tiffStart + view.getUint32(entryOffset + 8, littleEndian);
  const rationals = [];
  for (let i = 0; i < count; i++) {
    const num = view.getUint32(offset + i * 8, littleEndian);
    const den = view.getUint32(offset + i * 8 + 4, littleEndian);
    rationals.push(den === 0 ? 0 : num / den);
  }
  return rationals;
}

/**
 * Định dạng chuỗi ngày EXIF (YYYY:MM:DD HH:MM:SS) thành chuẩn ISO (YYYY-MM-DDTHH:mm:ss).
 * @param {string|null} dateStr
 * @returns {string|null}
 */
export function parseExifDateString(dateStr) {
  if (!dateStr || typeof dateStr !== 'string') return null;
  const match = dateStr.match(/^(\d{4})[:/-](\d{2})[:/-](\d{2})\s+(\d{2}):(\d{2}):(\d{2})/);
  if (!match) return null;

  const [, y, m, d, hh, mm, ss] = match;
  return `${y}-${m}-${d}T${hh}:${mm}:${ss}`;
}

/**
 * Trích xuất toàn diện siêu dữ liệu từ tệp ảnh (Frontend).
 * @param {File|Blob} file
 * @returns {Promise<object>}
 */
export async function extractImageMetadata(file) {
  if (!file) {
    return {
      fileName: '',
      mimeType: '',
      fileSize: 0,
      capturedAt: null,
      latitude: null,
      longitude: null,
      width: null,
      height: null,
      source: 'unknown',
    };
  }

  const baseMeta = {
    fileName: file.name || 'unnamed',
    mimeType: (file.type || '').toLowerCase(),
    fileSize: typeof file.size === 'number' ? file.size : 0,
    capturedAt: null,
    latitude: null,
    longitude: null,
    width: null,
    height: null,
    source: 'device',
  };

  // Trích xuất EXIF nếu là buffer khả dụng
  if (typeof file.arrayBuffer === 'function') {
    try {
      const buffer = await file.arrayBuffer();
      const exif = parseExifFromBuffer(buffer);
      if (exif.capturedAt) {
        baseMeta.capturedAt = exif.capturedAt;
        baseMeta.source = 'exif';
      }
      if (typeof exif.latitude === 'number') {
        baseMeta.latitude = exif.latitude;
      }
      if (typeof exif.longitude === 'number') {
        baseMeta.longitude = exif.longitude;
      }
    } catch {
      // Lỗi parse buffer không làm sập luồng
    }
  }

  // Fallback capturedAt từ lastModified nếu chưa có trong EXIF
  if (!baseMeta.capturedAt && typeof file.lastModified === 'number' && file.lastModified > 0) {
    const d = new Date(file.lastModified);
    if (!isNaN(d.getTime())) {
      baseMeta.capturedAt = d.toISOString().slice(0, 19);
      if (baseMeta.source !== 'exif') {
        baseMeta.source = 'file_system';
      }
    }
  }

  // Trích xuất kích thước ảnh nếu trong môi trường DOM
  if (typeof Image !== 'undefined' && typeof URL !== 'undefined' && URL.createObjectURL) {
    try {
      await new Promise((resolve) => {
        const img = new Image();
        const url = URL.createObjectURL(file);
        img.onload = () => {
          baseMeta.width = img.naturalWidth || img.width;
          baseMeta.height = img.naturalHeight || img.height;
          URL.revokeObjectURL(url);
          resolve();
        };
        img.onerror = () => {
          URL.revokeObjectURL(url);
          resolve();
        };
        img.src = url;
      });
    } catch {
      // bỏ qua lỗi đọc kích thước
    }
  }

  return baseMeta;
}

/**
 * Kiểm tra siêu dữ liệu bắt buộc của ảnh theo đặc tả T-74 / S-42.
 *
 * @param {object} meta Metadata đã trích xuất
 * @param {object} [options] Tùy chọn cấu hình
 * @param {boolean} [options.requireGps=false] Có bắt buộc thông tin tọa độ GPS hay không
 * @param {number} [options.maxSizeBytes=20971520] Dung lượng tối đa (20 MiB)
 * @returns {{ valid: boolean, errors: string[], warnings: string[], metadata: object }}
 */
export function validateImageMetadata(meta, options = {}) {
  const {
    requireGps = false,
    maxSizeBytes = 20 * 1024 * 1024,
  } = options;

  const errors = [];
  const warnings = [];

  if (!meta) {
    return {
      valid: false,
      errors: ['Không tìm thấy thông tin siêu dữ liệu của tệp ảnh.'],
      warnings: [],
      metadata: null,
    };
  }

  // 1. Kiểm tra tên tệp & dung lượng
  if (!meta.fileName || String(meta.fileName).trim() === '') {
    errors.push('Tên tệp ảnh không được để trống.');
  }

  if (typeof meta.fileSize === 'number') {
    if (meta.fileSize <= 0) {
      errors.push('Tệp ảnh trống (dung lượng 0 byte).');
    } else if (meta.fileSize > maxSizeBytes) {
      errors.push(`Dung lượng ảnh (${(meta.fileSize / (1024 * 1024)).toFixed(1)} MB) vượt quá giới hạn 20 MiB.`);
    }
  }

  // 2. Kiểm tra định dạng MIME
  const allowedMimes = ['image/jpeg', 'image/png', 'image/webp'];
  if (meta.mimeType && !allowedMimes.includes(meta.mimeType.toLowerCase())) {
    errors.push(`Định dạng "${meta.mimeType}" không được hỗ trợ. Chỉ chấp nhận JPEG, PNG hoặc WebP.`);
  }

  // 3. Kiểm tra thời gian chụp (capturedAt) - Bắt buộc
  if (!meta.capturedAt) {
    errors.push('Ảnh thiếu thời gian chụp (capturedAt). Đây là siêu dữ liệu bắt buộc của ảnh hiện trường.');
  } else {
    const captureDate = new Date(meta.capturedAt);
    if (isNaN(captureDate.getTime())) {
      errors.push(`Thời gian chụp "${meta.capturedAt}" không đúng định dạng ngày giờ.`);
    } else {
      // Dấu hiệu bất thường: thời gian chụp trong tương lai (cho phép dung sai 5 phút)
      const nowWithSkew = Date.now() + 5 * 60 * 1000;
      if (captureDate.getTime() > nowWithSkew) {
        errors.push(`Thời gian chụp (${meta.capturedAt}) ở tương lai. Đây là dấu hiệu bất thường.`);
      }

      // Kiểm tra thời gian quá cũ (trước năm 2000)
      const minDate = new Date('2000-01-01T00:00:00Z').getTime();
      if (captureDate.getTime() < minDate) {
        errors.push(`Thời gian chụp (${meta.capturedAt}) quá cũ (trước năm 2000).`);
      }
    }
  }

  // 4. Kiểm tra thông tin vị trí / GPS
  const hasLat = typeof meta.latitude === 'number' && !isNaN(meta.latitude);
  const hasLon = typeof meta.longitude === 'number' && !isNaN(meta.longitude);

  if (requireGps) {
    if (!hasLat || !hasLon) {
      errors.push('Ảnh thiếu thông tin vị trí GPS (vĩ độ, kinh độ). Nghiệp vụ yêu cầu ảnh hiện trường phải có tọa độ GPS.');
    }
  } else {
    if (!hasLat && !hasLon) {
      warnings.push('Ảnh chưa có thông tin tọa độ GPS từ thiết bị.');
    }
  }

  // Kiểm tra tính toàn vẹn và phạm vi GPS khi có dữ liệu
  if (hasLat !== hasLon) {
    errors.push('Thông tin GPS không đồng bộ: thiếu một trong hai giá trị vĩ độ (latitude) hoặc kinh độ (longitude).');
  } else if (hasLat && hasLon) {
    if (meta.latitude < -90 || meta.latitude > 90) {
      errors.push(`Vĩ độ GPS không hợp lệ (${meta.latitude}°). Vĩ độ phải nằm trong khoảng từ -90° đến +90°.`);
    }
    if (meta.longitude < -180 || meta.longitude > 180) {
      errors.push(`Kinh độ GPS không hợp lệ (${meta.longitude}°). Kinh độ phải nằm trong khoảng từ -180° đến +180°.`);
    }
  }

  return {
    valid: errors.length === 0,
    errors,
    warnings,
    metadata: meta,
  };
}
