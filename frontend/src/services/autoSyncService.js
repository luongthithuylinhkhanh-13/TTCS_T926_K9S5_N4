/**
 * Dịch vụ Tự động Đồng bộ Ngoại tuyến (Auto Sync Service)
 * Phục vụ Task:
 *   - NTDHTCT-214: S-30 - Hàng đợi đồng bộ tự gửi khi có mạng
 *   - NTDHTCT-256: T-71 – Tự động gửi dữ liệu lên máy chủ khi kết nối mạng trở lại
 */

import apiClient from './apiClient.js';
import syncQueueService from './syncQueueService.js';

class AutoSyncService {
  constructor() {
    this.isSyncing = false;
    this.timerId = null;
    this.initialized = false;
    this.onOnlineHandler = this.handleOnline.bind(this);
    this.onOfflineHandler = this.handleOffline.bind(this);
  }

  /**
   * Khởi tạo lắng nghe sự kiện mạng của trình duyệt và thiết lập timer đồng bộ định kỳ
   */
  init() {
    if (this.initialized || typeof window === 'undefined') {
      return;
    }

    window.addEventListener('online', this.onOnlineHandler);
    window.addEventListener('offline', this.onOfflineHandler);

    // Chu kỳ kiểm tra định kỳ mỗi 30 giây khi có mạng
    this.timerId = setInterval(() => {
      if (syncQueueService.isOnline() && !this.isSyncing) {
        const stats = syncQueueService.getStats();
        if (stats.hasPending) {
          this.syncQueueNow();
        }
      }
    }, 30000);

    this.initialized = true;

    // Nếu đang online khi vừa mở trang và có dữ liệu tồn đọng -> tự động gửi ngay
    if (syncQueueService.isOnline()) {
      setTimeout(() => {
        const stats = syncQueueService.getStats();
        if (stats.hasPending) {
          this.syncQueueNow();
        }
      }, 1500);
    }
  }

  /**
   * Hủy đăng ký lắng nghe sự kiện khi không còn dùng
   */
  destroy() {
    if (typeof window !== 'undefined') {
      window.removeEventListener('online', this.onOnlineHandler);
      window.removeEventListener('offline', this.onOfflineHandler);
    }
    if (this.timerId) {
      clearInterval(this.timerId);
      this.timerId = null;
    }
    this.initialized = false;
  }

  /**
   * T-71: Khi có mạng trở lại -> Tự động kích hoạt cơ chế gửi dữ liệu lên máy chủ
   */
  handleOnline() {
    console.log('[AutoSync] Phát hiện kết nối Internet được khôi phục. Đang kiểm tra hàng đợi...');
    this.dispatchStatusEvent('ONLINE', 'Đã kết nối Internet trở lại');
    this.syncQueueNow();
  }

  handleOffline() {
    console.log('[AutoSync] Thiết bị mất kết nối mạng. Chuyển sang chế độ lưu trữ ngoại tuyến.');
    this.dispatchStatusEvent('OFFLINE', 'Mất kết nối Internet. Dữ liệu sẽ lưu vào hàng đợi.');
  }

  /**
   * T-71: Xử lý toàn bộ hàng đợi gửi lên máy chủ
   */
  async syncQueueNow() {
    if (this.isSyncing) {
      return { success: false, message: 'Quá trình đồng bộ đang chạy' };
    }

    if (!syncQueueService.isOnline()) {
      return { success: false, message: 'Thiết bị đang offline' };
    }

    const pendingItems = syncQueueService.getPendingItems();
    if (pendingItems.length === 0) {
      return { success: true, processed: 0, message: 'Hàng đợi trống' };
    }

    this.isSyncing = true;
    this.dispatchStatusEvent('SYNCING', `Đang tự động đồng bộ ${pendingItems.length} mục dữ liệu...`);

    let successCount = 0;
    let failCount = 0;

    for (const item of pendingItems) {
      try {
        const response = await apiClient({
          url: item.endpoint,
          method: item.method,
          data: item.payload
        });

        // Kiểm tra kết quả trả về từ endpoint sync có mang status LOCKED hay không
        if (response.data?.status === 'LOCKED' || response.data?.errorCode === 'DIARY_DATE_LOCKED') {
          const lockMessage = response.data?.message || 'Ngày nhật ký đã bị khóa sổ. Thao tác bị từ chối.';
          syncQueueService.markLocked(item.id, lockMessage);
          failCount++;
        } else {
          syncQueueService.markSuccess(item.id);
          successCount++;
        }
      } catch (error) {
        // Kiểm tra xem lỗi HTTP có phải 409 Conflict hoặc mang mã DIARY_DATE_LOCKED không
        const isLockedError =
          error.response?.status === 409 ||
          error.response?.data?.code === 'DIARY_DATE_LOCKED' ||
          error.response?.data?.errorCode === 'DIARY_DATE_LOCKED' ||
          error.response?.data?.status === 'LOCKED';

        if (isLockedError) {
          const lockMessage =
            error.response?.data?.message || 'Ngày nhật ký đã bị khóa sổ. Thao tác bị từ chối.';
          syncQueueService.markLocked(item.id, lockMessage);
        } else {
          const errorMessage = error.message || 'Lỗi gửi dữ liệu lên máy chủ';
          syncQueueService.markFailed(item.id, errorMessage);
        }
        failCount++;
      }
    }

    this.isSyncing = false;

    if (successCount > 0 && failCount === 0) {
      this.dispatchStatusEvent('SYNC_SUCCESS', `Đã đồng bộ thành công ${successCount} mục dữ liệu lên máy chủ.`);
    } else if (failCount > 0) {
      this.dispatchStatusEvent('SYNC_PARTIAL', `Đồng bộ hoàn tất: ${successCount} thành công, ${failCount} thất bại.`);
    }

    return {
      success: failCount === 0,
      total: pendingItems.length,
      successCount,
      failCount
    };
  }

  dispatchStatusEvent(type, message) {
    if (typeof window !== 'undefined' && typeof window.dispatchEvent === 'function') {
      window.dispatchEvent(
        new CustomEvent('ntdhtcct:sync-status', {
          detail: {
            type,
            message,
            timestamp: new Date().toISOString(),
            stats: syncQueueService.getStats()
          }
        })
      );
    }
  }
}

export const autoSyncService = new AutoSyncService();
export default autoSyncService;
