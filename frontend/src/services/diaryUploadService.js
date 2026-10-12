import { compressImageToDisplay, MAX_DISPLAY_BYTES } from '../utils/imageCompressor.js';
import { uploadDisplayImage, uploadOriginalImage } from './diaryApi.js';

/**
 * Điều phối tải lên một ảnh trong hàng đợi theo 2 giai đoạn độc lập:
 * 1. Giai đoạn hiển thị: Nén thành JPEG < 1 MiB rồi gửi lên endpoint display
 * 2. Giai đoạn ảnh gốc: Giữ nguyên file gốc và gửi lên endpoint original (tối đa 20 MiB)
 *
 * @param {string} projectId
 * @param {string} entryId
 * @param {object} item
 * @param {object} callbacks
 * @param {Function} callbacks.onUpdate Trả về trạng thái cập nhật của item
 * @param {boolean} [options.displayOnly=false]
 * @param {boolean} [options.originalOnly=false]
 */
export async function uploadQueueItemStages(
  projectId,
  entryId,
  item,
  callbacks = {},
  options = {},
  apiFns = { uploadDisplayImage, uploadOriginalImage }
) {
  const { onUpdate } = callbacks;
  const { displayOnly = false, originalOnly = false } = options;
  const {
    uploadDisplayImage: doUploadDisplay = uploadDisplayImage,
    uploadOriginalImage: doUploadOriginal = uploadOriginalImage,
  } = apiFns;

  let currentItem = { ...item };

  // 1. Giai đoạn hiển thị
  if (!originalOnly && currentItem.displayStatus !== 'completed') {
    currentItem = {
      ...currentItem,
      displayStatus: 'uploading',
      displayError: null,
    };
    if (onUpdate) onUpdate(currentItem);

    try {
      const { blob } = await compressImageToDisplay(currentItem.file, { maxBytes: MAX_DISPLAY_BYTES });
      await doUploadDisplay(
        projectId,
        entryId,
        currentItem.clientImageId,
        currentItem.name,
        blob
      );
      currentItem = {
        ...currentItem,
        displayStatus: 'completed',
        displayError: null,
      };
      if (onUpdate) onUpdate(currentItem);
    } catch (err) {
      currentItem = {
        ...currentItem,
        displayStatus: 'failed',
        displayError: err.message || 'Lỗi tải ảnh hiển thị',
      };
      if (onUpdate) onUpdate(currentItem);
    }
  }

  // 2. Giai đoạn ảnh gốc (giữ nguyên bytes)
  if (!displayOnly && currentItem.originalStatus !== 'completed') {
    currentItem = {
      ...currentItem,
      originalStatus: 'uploading',
      originalError: null,
    };
    if (onUpdate) onUpdate(currentItem);

    try {
      if (currentItem.file.size > 20 * 1024 * 1024) {
        throw new Error('Ảnh gốc vượt quá 20 MiB');
      }

      await doUploadOriginal(
        projectId,
        entryId,
        currentItem.clientImageId,
        currentItem.name,
        currentItem.file
      );
      currentItem = {
        ...currentItem,
        originalStatus: 'completed',
        originalError: null,
      };
      if (onUpdate) onUpdate(currentItem);
    } catch (err) {
      currentItem = {
        ...currentItem,
        originalStatus: 'failed',
        originalError: err.message || 'Lỗi tải ảnh gốc',
      };
      if (onUpdate) onUpdate(currentItem);
    }
  }

  return currentItem;
}
