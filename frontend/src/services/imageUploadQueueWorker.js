import imageUploadQueueService from './imageUploadQueueService';
import processImageUploadQueueItem from './imageUploadQueueProcessor';
import { getProjectWbs, updateWbsItem } from './wbsApi';

let isProcessing = false;

const isOffline = () =>
  typeof navigator !== 'undefined' && !navigator.onLine;

const notifyQueueChanged = async (onChange) => {
  if (typeof onChange !== 'function') return;

  try {
    const items = await imageUploadQueueService.getAll();
    onChange(items);
  } catch (error) {
    console.error('Không thể đọc trạng thái hàng đợi ảnh:', error);
  }
};

const saveUploadedImageToTask = async (imageUrl, metadata) => {
  if (!metadata?.projectId || !metadata?.itemId) {
    throw new Error('Thiếu thông tin dự án hoặc công việc để lưu ảnh.');
  }

  const tasks = await getProjectWbs(metadata.projectId);
  const currentTask = tasks.find(
    task => String(task.id) === String(metadata.itemId)
  );

  if (!currentTask) {
    throw new Error('Không tìm thấy công việc để lưu ảnh.');
  }

  await updateWbsItem(metadata.projectId, metadata.itemId, {
    ...currentTask,
    image: imageUrl
  });
};

export const processPendingImageUploads = async (onChange) => {
  if (isProcessing || isOffline()) return;

  isProcessing = true;

  try {
    // Khôi phục các ảnh bị kẹt ở UPLOADING sau khi trang được tải lại.
    // Cờ isProcessing ngăn worker này tự khôi phục trạng thái giữa lúc đang chạy.
    const existingItems = await imageUploadQueueService.getAll();

    for (const item of existingItems) {
      if (item.status === 'UPLOADING') {
        await imageUploadQueueService.updateStatus(item.id, 'PENDING');
      }
    }

    await notifyQueueChanged(onChange);

    const pendingItems = await imageUploadQueueService.getPendingItems();

    for (const item of pendingItems) {
      if (isOffline()) break;

      try {
        await processImageUploadQueueItem(
          item.id,
          saveUploadedImageToTask
        );
      } catch (error) {
        console.error(
          `Không thể tải ảnh ${item.fileName || item.id}:`,
          error
        );
      }

      await notifyQueueChanged(onChange);
    }
  } catch (error) {
    console.error('Không thể xử lý hàng đợi ảnh:', error);
  } finally {
    isProcessing = false;
    await notifyQueueChanged(onChange);
  }
};

export const startImageUploadQueueWorker = (onChange) => {
  if (typeof window === 'undefined') return () => {};

  const run = () => {
    void processPendingImageUploads(onChange);
  };

  window.addEventListener('online', run);
  run();

  return () => {
    window.removeEventListener('online', run);
  };
};

export default startImageUploadQueueWorker;