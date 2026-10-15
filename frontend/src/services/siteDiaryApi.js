/**
 * API Client cho Nhật ký công trường (Site Diary)
 * Tích hợp chế độ ngoại tuyến (Offline Mode) và hàng đợi tự động đồng bộ (S-30).
 */

import apiClient from './apiClient.js';
import syncQueueService, { generateUUID } from './syncQueueService.js';
import autoSyncService from './autoSyncService.js';

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

export const updateSiteDiary = async (projectId, diaryId, diaryData) => {
  const response = await apiClient.put(
    `/api/projects/${projectId}/site-diaries/${diaryId}`,
    diaryData
  );
  return response.data;
};

/**
 * Xóa một bản ghi nhật ký công trường.
 * Nếu bản ghi đang nằm trong hàng đợi offline -> xóa khỏi hàng đợi.
 * Nếu đã đồng bộ lên server -> gọi API xóa trên server.
 */
export const deleteSiteDiary = async (projectId, diaryId) => {
  const queue = syncQueueService.getQueue();
  const offlineItem = queue.find(i => i.id === diaryId || i.clientSyncId === diaryId);
  if (offlineItem) {
    syncQueueService.removeItem(offlineItem.id);
    return { success: true, offlineDeleted: true };
  }
  const response = await apiClient.delete(
    `/api/projects/${projectId}/site-diaries/${diaryId}`
  );
  return response.data;
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

  // Lấy các bản ghi từ hàng đợi local (bao gồm PENDING, SYNCING, FAILED và REJECTED_LOCKED)
  const pendingItems = syncQueueService
    .getQueue()
    .filter(
      i =>
        i.projectId === projectId &&
        i.entityType === 'SITE_DIARY' &&
        (i.status === 'PENDING' || i.status === 'SYNCING' || i.status === 'FAILED' || i.status === 'REJECTED_LOCKED' || !i.status)
    )
    .map(i => {
      let syncStatusText = 'Chưa đồng bộ';
      if (i.status === 'FAILED') syncStatusText = 'Lỗi đồng bộ';
      else if (i.status === 'SYNCING') syncStatusText = 'Đang đồng bộ';
      else if (i.status === 'REJECTED_LOCKED') syncStatusText = 'Bị từ chối (Đã khóa)';

      return {
        id: i.id,
        clientSyncId: i.clientSyncId,
        projectId: i.projectId,
        diaryDate: i.payload.diaryDate,
        weather: i.payload.weather,
        temperature: i.payload.temperature,
        engineerCount: i.payload.engineerCount,
        workerCount: i.payload.workerCount,
        crewCount: i.payload.crewCount,
        crewDetails: i.payload.crewDetails,
        equipmentStatus: i.payload.equipmentStatus,
        workSummary: i.payload.workSummary,
        issues: i.payload.issues,
        workingConditions: i.payload.workingConditions,
        syncStatus: syncStatusText,
        rawSyncStatus: i.status || 'PENDING',
        _isPendingSync: i.status !== 'REJECTED_LOCKED',
        _isRejectedLocked: i.status === 'REJECTED_LOCKED',
        _retryCount: i.retryCount || 0,
        _lastError: i.lastError,
        offlineCreatedAt: i.payload.offlineCreatedAt
      };
    });

  const formattedServerList = serverList.map(item => ({
    ...item,
    syncStatus: (!item.syncStatus || item.syncStatus === 'SYNCED') ? 'Đã đồng bộ' : item.syncStatus,
    rawSyncStatus: 'SYNCED',
    _isPendingSync: false
  }));

  // Gộp bản ghi local lên đầu danh sách
  return [...pendingItems, ...formattedServerList];
};

/**
 * T-73 (NTDHTCT-258): Đếm số lượng mục nhật ký đang chờ đồng bộ của một dự án
 */
export const getPendingSiteDiaryCount = (projectId = null) => {
  return syncQueueService.getPendingCount(projectId);
};

/**
 * T-72, T-73 (S-31): Lấy thông tin thống kê trạng thái đồng bộ nhật ký từ máy chủ
 */
export const getSiteDiarySyncStatus = async projectId => {
  try {
    const res = await apiClient.get(`/api/projects/${projectId}/site-diaries/sync-status`);
    return res.data;
  } catch (err) {
    console.warn('[SiteDiary] Không thể lấy trạng thái đồng bộ từ máy chủ:', err.message);
    return null;
  }
};

/**
 * Kích hoạt đồng bộ thủ công ngay lập tức
 */
export const triggerManualSync = () => {
  return autoSyncService.syncQueueNow();
};

/**
 * T-65: Đưa một bản ghi từ trạng thái REJECTED_LOCKED trở lại PENDING để đồng bộ lại
 */
export const retryQueueItem = queueId => {
  const queue = syncQueueService.getQueue();
  const item = queue.find(i => i.id === queueId);
  if (item) {
    item.status = 'PENDING';
    item.isLocked = false;
    item.lastError = null;
    item.retryCount = 0;
    item.updatedAt = new Date().toISOString();
    syncQueueService.saveQueue(queue);
    return true;
  }
  return false;
};

/**
 * T-64: Khóa sổ nhật ký công trường theo ngày
 */
export const lockSiteDiaryDate = async (projectId, lockDate, reason) => {
  const response = await apiClient.post(
    `/api/projects/${projectId}/site-diaries/locks`,
    { lockDate, reason }
  );
  return response.data;
};

/**
 * T-64: Mở khóa sổ nhật ký công trường theo ngày
 */
export const unlockSiteDiaryDate = async (projectId, lockDate, reason) => {
  const response = await apiClient.post(
    `/api/projects/${projectId}/site-diaries/unlocks`,
    { lockDate, reason }
  );
  return response.data;
};

/**
 * T-64: Lấy danh sách tất cả các ngày đã thiết lập khóa/mở khóa sổ của dự án
 */
export const getProjectDiaryLocks = async projectId => {
  try {
    const response = await apiClient.get(`/api/projects/${projectId}/site-diaries/locks`);
    return Array.isArray(response.data) ? response.data : [];
  } catch (err) {
    console.warn('[SiteDiary] Không thể tải danh sách khóa sổ:', err.message);
    return [];
  }
};

/**
 * T-64: Tra cứu trạng thái khóa sổ của một ngày cụ thể
 */
export const getDiaryLockStatus = async (projectId, date) => {
  const response = await apiClient.get(
    `/api/projects/${projectId}/site-diaries/locks/status`,
    { params: { date } }
  );
  return response.data;
};
