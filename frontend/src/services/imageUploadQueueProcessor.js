import imageUploadApi from './imageUploadApi';
import imageUploadQueueService from './imageUploadQueueService';

/**
 * Xử lý một ảnh trong hàng đợi:
 * PENDING -> UPLOADING -> COMPLETED hoặc FAILED.
 *
 * onUploaded chịu trách nhiệm lưu URL ảnh vào công việc.
 */
export const processImageUploadQueueItem = async (id, onUploaded) => {
  const item = await imageUploadQueueService.getFile(id);

  if (!item) {
    throw new Error('Không tìm thấy ảnh trong hàng đợi.');
  }

  if (!item.metadata?.projectId) {
    const error = new Error('Không tìm thấy mã dự án của ảnh.');
    await imageUploadQueueService.updateStatus(
      id,
      'FAILED',
      error.message
    );
    throw error;
  }

  try {
    await imageUploadQueueService.updateStatus(id, 'UPLOADING');

    const imageUrl = await imageUploadApi.uploadImage(
      item.metadata.projectId,
      item.file
    );

    if (typeof onUploaded !== 'function') {
      throw new Error('Chưa có hàm lưu đường dẫn ảnh vào công việc.');
    }

    await onUploaded(imageUrl, item.metadata);

    await imageUploadQueueService.updateStatus(id, 'COMPLETED');

    return imageUrl;
  } catch (error) {
    await imageUploadQueueService.updateStatus(
      id,
      'FAILED',
      error.message || 'Tải ảnh lên thất bại.'
    );

    throw error;
  }
};

export default processImageUploadQueueItem;
