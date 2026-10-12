/**
 * Dịch vụ Quản lý Hàng đợi Đồng bộ Ngoại tuyến (Offline Sync Queue Service)
 * Phục vụ Task:
 *   - NTDHTCT-214: S-30 - Hàng đợi đồng bộ tự gửi khi có mạng
 *   - NTDHTCT-255: T-70 – Tạo hàng đợi cho dữ liệu chưa đồng bộ
 */

const STORAGE_KEY = 'ntdhtcct_offline_sync_queue_v1';
const MAX_DEFAULT_RETRIES = 5;

// Hàm tạo UUID tương thích mọi trình duyệt và môi trường test
export const generateUUID = () => {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID();
  }
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, c => {
    const r = (Math.random() * 16) | 0;
    const v = c === 'x' ? r : (r & 0x3) | 0x8;
    return v.toString(16);
  });
};

class SyncQueueService {
  /**
   * Lấy toàn bộ hàng đợi từ LocalStorage
   */
  getQueue() {
    try {
      if (typeof window === 'undefined' || !window.localStorage) {
        return [];
      }
      const data = window.localStorage.getItem(STORAGE_KEY);
      return data ? JSON.parse(data) : [];
    } catch (err) {
      console.error('Lỗi khi đọc hàng đợi đồng bộ:', err);
      throw err;
    }
  }

  /**
   * Lưu hàng đợi vào LocalStorage
   */
  saveQueue(queue) {
    try {
      if (typeof window !== 'undefined' && window.localStorage) {
        window.localStorage.setItem(STORAGE_KEY, JSON.stringify(queue));
      } else {
        throw new Error('Không thể truy cập bộ nhớ cục bộ của trình duyệt.');
      }
      this.dispatchQueueEvent();
    } catch (err) {
      console.error('Lỗi khi lưu hàng đợi đồng bộ:', err);
      throw err;
    }
  }

  /**
   * T-70: Thêm một bản ghi vào hàng đợi khi thiết bị mất mạng hoặc gửi lỗi
   */
  enqueue({
    projectId,
    entityType = 'SITE_DIARY',
    action = 'CREATE',
    endpoint,
    method = 'POST',
    payload = {},
    title = 'Bản ghi nhật ký'
  }) {
    const queue = this.getQueue();
    const id = generateUUID();
    const clientSyncId = payload.clientSyncId || `sync-${generateUUID()}`;

    const queueItem = {
      id,
      clientSyncId,
      projectId,
      entityType,
      action,
      endpoint,
      method,
      title,
      payload: {
        ...payload,
        clientSyncId,
        offlineCreatedAt: payload.offlineCreatedAt || new Date().toISOString()
      },
      status: 'PENDING', // PENDING | SYNCING | FAILED | SYNCED
      retryCount: 0,
      maxRetries: MAX_DEFAULT_RETRIES,
      lastError: null,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString()
    };

    queue.push(queueItem);
    this.saveQueue(queue);

    return queueItem;
  }

  /**
   * Lấy danh sách các bản ghi đang chờ được đồng bộ
   */
  getPendingItems() {
    const queue = this.getQueue();
    return queue.filter(
      item => item.status === 'PENDING' || (item.status === 'FAILED' && item.retryCount < item.maxRetries)
    );
  }

  /**
   * Đánh dấu item đang trong quá trình gửi lên server
   */
  markSyncing(id) {
    const queue = this.getQueue();
    const item = queue.find(i => i.id === id);
    if (item) {
      item.status = 'SYNCING';
      item.updatedAt = new Date().toISOString();
      this.saveQueue(queue);
    }
  }

  /**
   * T-71: Đánh dấu item đồng bộ thành công và loại bỏ khỏi hàng đợi
   */
  markSuccess(id) {
    let queue = this.getQueue();
    queue = queue.filter(i => i.id !== id);
    this.saveQueue(queue);
  }

  /**
   * Đánh dấu item đồng bộ thất bại, tăng số lần thử lại (retry count)
   */
  markFailed(id, errorMessage) {
    const queue = this.getQueue();
    const item = queue.find(i => i.id === id);
    if (item) {
      item.retryCount = (item.retryCount || 0) + 1;
      item.lastError = errorMessage;
      item.status = 'FAILED';
      item.updatedAt = new Date().toISOString();
      this.saveQueue(queue);
    }
  }

  /**
   * Xóa thủ công một bản ghi khỏi hàng đợi
   */
  remove(id) {
    let queue = this.getQueue();
    queue = queue.filter(i => i.id !== id);
    this.saveQueue(queue);
  }

  /**
   * Xóa sạch toàn bộ hàng đợi
   */
  clearAll() {
    this.saveQueue([]);
  }

  /**
   * Thống kê trạng thái hàng đợi
   */
  getStats() {
    const queue = this.getQueue();
    const pending = queue.filter(i => i.status === 'PENDING').length;
    const syncing = queue.filter(i => i.status === 'SYNCING').length;
    const failed = queue.filter(i => i.status === 'FAILED').length;
    return {
      total: queue.length,
      pending,
      syncing,
      failed,
      hasPending: pending + failed > 0
    };
  }

  /**
   * Kiểm tra thiết bị có đang kết nối mạng không
   */
  isOnline() {
    if (typeof navigator !== 'undefined' && 'onLine' in navigator) {
      return navigator.onLine;
    }
    return true;
  }

  /**
   * Phát CustomEvent để UI tự động cập nhật
   */
  dispatchQueueEvent() {
    if (typeof window !== 'undefined' && typeof window.dispatchEvent === 'function') {
      const event = new CustomEvent('ntdhtcct:sync-queue-updated', {
        detail: this.getStats()
      });
      window.dispatchEvent(event);
    }
  }
}

export const syncQueueService = new SyncQueueService();
export default syncQueueService;
