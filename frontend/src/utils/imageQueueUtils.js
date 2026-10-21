/**
 * Tiện ích quản lý danh sách file ảnh và preview URL trong hàng đợi tải lên.
 */

/**
 * Định dạng kích thước file sang chuỗi hiển thị thân thiện (B, KB, MB).
 * @param {number} bytes
 * @returns {string}
 */
export function formatFileSize(bytes) {
  if (bytes === null || bytes === undefined || isNaN(bytes)) {
    return '0 B';
  }
  if (bytes < 1024) {
    return `${bytes} B`;
  }
  if (bytes < 1024 * 1024) {
    return `${(bytes / 1024).toFixed(1)} KB`;
  }
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

import { validateImageMetadata } from './imageMetadataExtractor.js';

/**
 * Tạo một item ảnh mới đưa vào hàng đợi với ID client duy nhất và URL preview.
 * @param {File} file
 * @param {Function} [createObjectURLFn] Hàm tạo object URL (mặc định URL.createObjectURL)
 * @param {object} [initialMetadata] Siêu dữ liệu ban đầu nếu đã trích xuất
 * @param {object} [validationOptions] Tùy chọn kiểm tra (ví dụ requireGps)
 * @returns {object}
 */
export function createQueueItem(
  file,
  createObjectURLFn = (f) => URL.createObjectURL(f),
  initialMetadata = null,
  validationOptions = {}
) {
  const clientImageId = typeof crypto !== 'undefined' && crypto.randomUUID
    ? crypto.randomUUID()
    : 'temp-' + Math.random().toString(36).substring(2, 11);

  const previewUrl = createObjectURLFn(file);

  const metadata = initialMetadata ? { ...initialMetadata } : {
    fileName: file?.name || '',
    fileSize: typeof file?.size === 'number' ? file.size : 0,
    mimeType: (file?.type || '').toLowerCase(),
    capturedAt: null,
    latitude: null,
    longitude: null,
    width: null,
    height: null,
  };

  const metadataValidation = validateImageMetadata(metadata, validationOptions);

  return {
    clientImageId,
    file,
    name: file?.name,
    size: file?.size,
    type: file?.type,
    previewUrl,
    metadata,
    metadataValidation,
    displayStatus: 'idle', // 'idle' | 'uploading' | 'completed' | 'failed'
    originalStatus: 'idle', // 'idle' | 'uploading' | 'completed' | 'failed'
    displayError: null,
    originalError: null,
    displayProgress: 0,
    originalProgress: 0,
  };
}

/**
 * Thu hồi preview URL của một item ảnh an toàn.
 * @param {object} item
 * @param {Function} [revokeObjectURLFn]
 */
export function revokeQueueItem(item, revokeObjectURLFn = (u) => URL.revokeObjectURL(u)) {
  if (item && item.previewUrl) {
    try {
      revokeObjectURLFn(item.previewUrl);
    } catch {
      // bỏ qua lỗi nếu URL không hợp lệ hoặc đã thu hồi
    }
  }
}

/**
 * Thu hồi preview URL của toàn bộ danh sách item.
 * @param {Array<object>} items
 * @param {Function} [revokeObjectURLFn]
 */
export function revokeAllQueueItems(items, revokeObjectURLFn = (u) => URL.revokeObjectURL(u)) {
  if (!Array.isArray(items)) return;
  for (const item of items) {
    revokeQueueItem(item, revokeObjectURLFn);
  }
}
