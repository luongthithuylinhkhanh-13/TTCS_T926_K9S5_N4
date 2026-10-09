/**
 * API Client cho Nhật ký công trường (Site Diary)
 * Tích hợp chế độ ngoại tuyến (Offline Mode) và hàng đợi tự động đồng bộ (S-30).
 */

import apiClient from './apiClient';
import syncQueueService, { generateUUID } from './syncQueueService';
import autoSyncService from './autoSyncService';

/**
 * Tạo mới hoặc xếp vào hàng đợi ngoại tuyến nếu mất mạng (T-70, T-71)
 */
export const submitSiteDiary = async (projectId, diaryData) => {
  const clientSyncId = diaryData.clientSyncId || `diary-sync-${generateUUID()}`;
  const payload = {
    ...diaryData,
    clientSyncId,
    offlineCreatedAt: diaryData.offlineCreatedAt || new Date().toISOString()
  };

  const endpoint = `/api/projects/${projectId}/site-diaries/sync`;

  // 1. Nếu đang offline -> trực tiếp đưa vào hàng đợi
  if (!syncQueueService.isOnline()) {
    const queueItem = syncQueueService.enqueue({
      projectId,
      entityType: 'SITE_DIARY',
      action: 'CREATE',
      endpoint,
      method: 'POST',
      payload,
      title: `Nhật ký ngày ${diaryData.diaryDate || new Date().toLocaleDateString('vi-VN')}`
    });

    return {
      success: true,
      isOfflineQueued: true,
      clientSyncId,
      queueId: queueItem.id,
      message: 'Đang ngoại tuyến. Nhật ký đã được lưu vào hàng đợi và sẽ tự gửi khi có mạng.'
    };
  }

  // 2. Nếu đang online -> thử gửi lên server
  try {
    const response = await apiClient.post(endpoint, payload);
    return {
      success: true,
      isOfflineQueued: false,
      data: response.data,
      message: 'Nhật ký đã được lưu thành công trên máy chủ.'
    };
  } catch (error) {
    // Nếu gặp lỗi mạng (mất kết nối đột ngột hoặc server không phản hồi)
    const isNetworkError =
      !error.response ||
      error.message?.includes('Network Error') ||
      error.message?.includes('Failed to fetch') ||
      error.message?.includes('Không thể kết nối');

    if (isNetworkError) {
      const queueItem = syncQueueService.enqueue({
        projectId,
        entityType: 'SITE_DIARY',
        action: 'CREATE',
        endpoint,
        method: 'POST',
        payload,
        title: `Nhật ký ngày ${diaryData.diaryDate || new Date().toLocaleDateString('vi-VN')}`
      });

      return {
        success: true,
        isOfflineQueued: true,
        fallback: true,
        clientSyncId,
        queueId: queueItem.id,
        message: 'Mất kết nối tới máy chủ. Đã chuyển nhật ký vào hàng đợi tự động gửi.'
      };
    }

    throw error;
  }
};

/**
 * Lấy danh sách nhật ký công trường, gộp các bản ghi đang chờ đồng bộ trong hàng đợi
 */
export const getSiteDiaries = async projectId => {
  let serverList = [];
  try {
    const res = await apiClient.get(`/api/projects/${projectId}/site-diaries`);
    serverList = Array.isArray(res.data) ? res.data : [];
  } catch (err) {
    console.warn('[SiteDiary] Không thể tải từ máy chủ, lấy từ bộ nhớ đệm:', err.message);
  }

  // Lấy các bản ghi đang chờ đồng bộ từ hàng đợi local
  const pendingItems = syncQueueService
    .getPendingItems()
    .filter(i => i.projectId === projectId && i.entityType === 'SITE_DIARY')
    .map(i => ({
      id: i.id,
      clientSyncId: i.clientSyncId,
      projectId: i.projectId,
      diaryDate: i.payload.diaryDate,
      weather: i.payload.weather,
      temperature: i.payload.temperature,
      workerCount: i.payload.workerCount,
      equipmentStatus: i.payload.equipmentStatus,
      workSummary: i.payload.workSummary,
      issues: i.payload.issues,
      syncStatus: i.status === 'FAILED' ? 'Lỗi đồng bộ' : 'Chờ đồng bộ',
      _isPendingSync: true,
      _retryCount: i.retryCount,
      _lastError: i.lastError,
      offlineCreatedAt: i.payload.offlineCreatedAt
    }));

  // Gộp bản ghi chờ gửi lên đầu danh sách
  return [...pendingItems, ...serverList];
};

/**
 * Kích hoạt đồng bộ thủ công ngay lập tức
 */
export const triggerManualSync = () => {
  return autoSyncService.syncQueueNow();
};
