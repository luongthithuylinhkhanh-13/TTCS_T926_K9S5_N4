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
 * Định dạng thời điểm chụp ảnh (capturedAt) chuẩn hóa dạng HH:mm DD/MM/YYYY cho hiển thị giao diện (T-70).
 * Trả về chuỗi thông báo rõ ràng "Chưa có thời gian chụp" nếu null/undefined hoặc không hợp lệ, không để xuất hiện "Invalid Date".
 *
 * @param {string|null|undefined} dateStr
 * @param {object} [options]
 * @param {string} [options.fallback='Chưa có thời gian chụp']
 * @returns {string}
 */
export function formatCapturedAt(dateStr, options = {}) {
  const fallback = options.fallback || 'Chưa có thời gian chụp';
  if (!dateStr || typeof dateStr !== 'string' || dateStr.trim() === '') {
    return fallback;
  }

  const d = new Date(dateStr);
  if (isNaN(d.getTime())) {
    return fallback;
  }

  const hours = String(d.getHours()).padStart(2, '0');
  const minutes = String(d.getMinutes()).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  const month = String(d.getMonth() + 1).padStart(2, '0');
  const year = d.getFullYear();

  return `${hours}:${minutes} ${day}/${month}/${year}`;
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
 * Định nghĩa các trạng thái kiểm tra siêu dữ liệu theo đặc tả T-75 / S-42.
 */
export const METADATA_STATUS = {
  VALID: 'VALID',
  MISSING: 'MISSING',
  INVALID: 'INVALID',
};

export const METADATA_STATUS_LABELS = {
  [METADATA_STATUS.VALID]: 'Meta: Hợp lệ',
  [METADATA_STATUS.MISSING]: 'Meta: Thiếu',
  [METADATA_STATUS.INVALID]: 'Meta: Không hợp lệ',
};

/**
 * Kiểm tra siêu dữ liệu bắt buộc của ảnh theo đặc tả T-74 / S-42 và phân loại trạng thái T-75.
 *
 * @param {object} meta Metadata đã trích xuất
 * @param {object} [options] Tùy chọn cấu hình
 * @param {boolean} [options.requireGps=false] Có bắt buộc thông tin tọa độ GPS hay không
 * @param {number} [options.maxSizeBytes=20971520] Dung lượng tối đa (20 MiB)
 * @returns {{ valid: boolean, status: string, statusLabel: string, missingFields: string[], invalidFields: string[], errors: string[], warnings: string[], metadata: object }}
 */
export function validateImageMetadata(meta, options = {}) {
  const {
    requireGps = false,
    maxSizeBytes = 20 * 1024 * 1024,
  } = options;

  const errors = [];
  const warnings = [];
  const missingFields = [];
  const invalidFields = [];
  const reasons = [];

  if (!meta) {
    return {
      valid: false,
      status: METADATA_STATUS.MISSING,
      statusLabel: METADATA_STATUS_LABELS[METADATA_STATUS.MISSING],
      missingFields: ['metadata'],
      invalidFields: [],
      reasons: ['Thiếu toàn bộ thông tin siêu dữ liệu'],
      errors: ['Không tìm thấy thông tin siêu dữ liệu của tệp ảnh.'],
      warnings: [],
      metadata: null,
    };
  }

  // 1. Kiểm tra tên tệp & dung lượng
  if (!meta.fileName || String(meta.fileName).trim() === '') {
    errors.push('Tên tệp ảnh không được để trống.');
    missingFields.push('fileName');
    reasons.push('Thiếu tên tệp ảnh');
  }

  if (typeof meta.fileSize === 'number') {
    if (meta.fileSize <= 0) {
      errors.push('Tệp ảnh trống (dung lượng 0 byte).');
      invalidFields.push('fileSize');
      reasons.push('File ảnh rỗng');
    } else if (meta.fileSize > maxSizeBytes) {
      errors.push(`Dung lượng ảnh (${(meta.fileSize / (1024 * 1024)).toFixed(1)} MB) vượt quá giới hạn 20 MiB.`);
      invalidFields.push('fileSize');
      reasons.push('File ảnh vượt quá 20 MiB');
    }
  }

  // 2. Kiểm tra định dạng MIME
  const allowedMimes = ['image/jpeg', 'image/png', 'image/webp'];
  if (meta.mimeType && !allowedMimes.includes(meta.mimeType.toLowerCase())) {
    errors.push(`Định dạng "${meta.mimeType}" không được hỗ trợ. Chỉ chấp nhận JPEG, PNG hoặc WebP.`);
    invalidFields.push('mimeType');
    reasons.push('Định dạng ảnh không được hỗ trợ');
  }

  // 3. Kiểm tra thời gian chụp (capturedAt) - Bắt buộc
  if (!meta.capturedAt) {
    errors.push('Ảnh thiếu thời gian chụp (capturedAt). Đây là siêu dữ liệu bắt buộc của ảnh hiện trường.');
    missingFields.push('capturedAt');
    reasons.push('Thiếu thời gian chụp');
  } else {
    const captureDate = new Date(meta.capturedAt);
    if (isNaN(captureDate.getTime())) {
      errors.push(`Thời gian chụp "${meta.capturedAt}" không đúng định dạng ngày giờ.`);
      invalidFields.push('capturedAt');
      reasons.push('Metadata ngày giờ không hợp lệ');
    } else {
      // Dấu hiệu bất thường: thời gian chụp trong tương lai (cho phép dung sai 5 phút)
      const nowWithSkew = Date.now() + 5 * 60 * 1000;
      if (captureDate.getTime() > nowWithSkew) {
        errors.push(`Thời gian chụp (${meta.capturedAt}) ở tương lai. Đây là dấu hiệu bất thường.`);
        invalidFields.push('capturedAt');
        reasons.push('Thời gian chụp ở tương lai');
      }

      // Kiểm tra thời gian quá cũ (trước năm 2000)
      const minDate = new Date('2000-01-01T00:00:00Z').getTime();
      if (captureDate.getTime() < minDate) {
        errors.push(`Thời gian chụp (${meta.capturedAt}) quá cũ (trước năm 2000).`);
        invalidFields.push('capturedAt');
        reasons.push('Thời gian chụp trước năm 2000');
      }
    }
  }

  // 4. Kiểm tra thông tin vị trí / GPS
  const hasLat = typeof meta.latitude === 'number' && !isNaN(meta.latitude);
  const hasLon = typeof meta.longitude === 'number' && !isNaN(meta.longitude);

  if (requireGps) {
    if (!hasLat && !hasLon) {
      errors.push('Ảnh thiếu thông tin vị trí GPS (vĩ độ, kinh độ). Nghiệp vụ yêu cầu ảnh hiện trường phải có tọa độ GPS.');
      missingFields.push('gps');
      reasons.push('Thiếu thông tin vị trí GPS');
    }
  } else {
    if (!hasLat && !hasLon) {
      warnings.push('Ảnh chưa có thông tin tọa độ GPS từ thiết bị.');
    }
  }

  // Kiểm tra tính toàn vẹn và phạm vi GPS khi có dữ liệu
  if (hasLat !== hasLon) {
    errors.push('Thông tin GPS không đồng bộ: thiếu một trong hai giá trị vĩ độ (latitude) hoặc kinh độ (longitude).');
    invalidFields.push('gps');
    if (hasLat && !hasLon) {
      reasons.push('Thiếu kinh độ GPS (tọa độ GPS không đầy đủ)');
    } else {
      reasons.push('Thiếu vĩ độ GPS (tọa độ GPS không đầy đủ)');
    }
  } else if (hasLat && hasLon) {
    if (meta.latitude < -90 || meta.latitude > 90) {
      errors.push(`Vĩ độ GPS không hợp lệ (${meta.latitude}°). Vĩ độ phải nằm trong khoảng từ -90° đến +90°.`);
      invalidFields.push('gps');
      reasons.push('Vĩ độ GPS ngoài phạm vi -90 đến 90');
    }
    if (meta.longitude < -180 || meta.longitude > 180) {
      errors.push(`Kinh độ GPS không hợp lệ (${meta.longitude}°). Kinh độ phải nằm trong khoảng từ -180° đến +180°.`);
      invalidFields.push('gps');
      reasons.push('Kinh độ GPS ngoài phạm vi -180 đến 180');
    }
  }

  // Xác định trạng thái theo T-75:
  // - A: HỢP LỆ: Không có lỗi -> 'VALID' ("Meta: Hợp lệ")
  // - B: THIẾU METADATA: Thiếu trường bắt buộc -> 'MISSING' ("Meta: Thiếu")
  // - C: METADATA KHÔNG HỢP LỆ: Có metadata nhưng giá trị sai/bất thường -> 'INVALID' ("Meta: Không hợp lệ")
  let status = METADATA_STATUS.VALID;
  if (missingFields.length > 0) {
    status = METADATA_STATUS.MISSING;
  } else if (invalidFields.length > 0) {
    status = METADATA_STATUS.INVALID;
  }

  return {
    valid: status === METADATA_STATUS.VALID,
    status,
    statusLabel: METADATA_STATUS_LABELS[status],
    reasons,
    missingFields,
    invalidFields,
    errors,
    warnings,
    metadata: meta,
  };
}

/**
 * Xác định trạng thái metadata của ảnh (Hợp lệ / Thiếu / Không hợp lệ) theo T-75 và lý do theo T-76.
 * Áp dụng được cho cả item trong hàng đợi tải lên và đối tượng ảnh đã lưu trong DB.
 *
 * @param {object} imageOrMeta Đối tượng ảnh (DiaryImageResponse hoặc item queue)
 * @param {object} [options] Tùy chọn kiểm tra (ví dụ requireGps)
 * @returns {{ status: 'VALID'|'MISSING'|'INVALID', label: string, reasons: string[], missingFields: string[], invalidFields: string[] }}
 */
export function determineImageMetadataStatus(imageOrMeta, options = {}) {
  if (!imageOrMeta) {
    return {
      status: METADATA_STATUS.MISSING,
      label: METADATA_STATUS_LABELS[METADATA_STATUS.MISSING],
      reasons: ['Thiếu toàn bộ thông tin siêu dữ liệu'],
      missingFields: ['metadata'],
      invalidFields: [],
    };
  }

  // Nếu đối tượng đã có kết quả kiểm tra metadataValidation từ T-74/T-75
  if (imageOrMeta.metadataValidation && imageOrMeta.metadataValidation.status) {
    const v = imageOrMeta.metadataValidation;
    return {
      status: v.status,
      label: v.statusLabel || METADATA_STATUS_LABELS[v.status],
      reasons: Array.isArray(v.reasons) ? v.reasons : (v.status !== METADATA_STATUS.VALID ? (v.errors || []) : []),
      missingFields: v.missingFields || [],
      invalidFields: v.invalidFields || [],
    };
  }

  // Nếu đối tượng từ server đã có sẵn metadataStatus và metadataReasons (T-76)
  if (Array.isArray(imageOrMeta.metadataReasons)) {
    const status = imageOrMeta.metadataStatus || (imageOrMeta.metadataReasons.length > 0 ? METADATA_STATUS.INVALID : METADATA_STATUS.VALID);
    return {
      status,
      label: METADATA_STATUS_LABELS[status] || 'Meta: Hợp lệ',
      reasons: imageOrMeta.metadataReasons,
      missingFields: [],
      invalidFields: [],
    };
  }

  // Nếu là đối tượng metadata trực tiếp hoặc ảnh tải từ DB (DiaryImageResponse)
  const meta = imageOrMeta.metadata || imageOrMeta;
  const validation = validateImageMetadata(
    {
      fileName: meta.originalFilename || meta.fileName || 'image.jpg',
      fileSize: meta.originalFileSize || meta.fileSize || 1024,
      mimeType: meta.contentType || meta.mimeType || 'image/jpeg',
      capturedAt: meta.capturedAt || null,
      latitude: typeof meta.latitude === 'number' ? meta.latitude : null,
      longitude: typeof meta.longitude === 'number' ? meta.longitude : null,
    },
    options
  );

  return {
    status: validation.status,
    label: validation.statusLabel,
    reasons: validation.reasons,
    missingFields: validation.missingFields,
    invalidFields: validation.invalidFields,
  };
}
