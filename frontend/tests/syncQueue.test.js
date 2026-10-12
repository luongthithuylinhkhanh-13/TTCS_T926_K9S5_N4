import assert from 'node:assert/strict';
import test from 'node:test';
import { syncQueueService, generateUUID } from '../src/services/syncQueueService.js';
import apiClient from '../src/services/apiClient.js';
import autoSyncService from '../src/services/autoSyncService.js';
import { submitSiteDiary, getSiteDiaries, getPendingSiteDiaryCount } from '../src/services/siteDiaryApi.js';

// Mock localStorage cho môi trường Node test
class LocalStorageMock {
  constructor() {
    this.store = {};
  }
  clear() {
    this.store = {};
  }
  getItem(key) {
    return this.store[key] || null;
  }
  setItem(key, value) {
    this.store[key] = String(value);
  }
  removeItem(key) {
    delete this.store[key];
  }
}

globalThis.localStorage = new LocalStorageMock();
globalThis.sessionStorage = globalThis.localStorage;
if (typeof globalThis.window === 'undefined') {
  globalThis.window = {
    localStorage: globalThis.localStorage,
    dispatchEvent: () => true
  };
} else {
  globalThis.window.localStorage = globalThis.localStorage;
  globalThis.window.dispatchEvent = () => true;
}

try {
  Object.defineProperty(globalThis.navigator, 'onLine', {
    value: true,
    configurable: true,
    writable: true
  });
} catch {
  // Bỏ qua nếu môi trường đã có sẵn thuộc tính onLine
}

test('TC-SYNC-01: generateUUID tạo chuỗi UUID hợp lệ', () => {
  const id1 = generateUUID();
  const id2 = generateUUID();
  assert.ok(id1);
  assert.ok(id2);
  assert.notEqual(id1, id2);
  assert.equal(typeof id1, 'string');
});

test('TC-SYNC-02: T-70 - Enqueue thêm bản ghi vào hàng đợi khi mất mạng', () => {
  syncQueueService.clearAll();

  const item = syncQueueService.enqueue({
    projectId: 'project-uuid-1',
    entityType: 'SITE_DIARY',
    action: 'CREATE',
    endpoint: '/api/projects/project-uuid-1/site-diaries/sync',
    method: 'POST',
    payload: {
      diaryDate: '2026-10-09',
      weather: 'Nắng',
      workSummary: 'Đổ bê tông dầm sàn tầng 3',
      workerCount: 25
    },
    title: 'Nhật ký ngày 09/10/2026'
  });

  assert.ok(item.id);
  assert.ok(item.clientSyncId);
  assert.equal(item.status, 'PENDING');
  assert.equal(item.retryCount, 0);
  assert.ok(item.payload.offlineCreatedAt);

  const queue = syncQueueService.getQueue();
  assert.equal(queue.length, 1);
  assert.equal(queue[0].id, item.id);
  assert.equal(queue[0].payload.workSummary, 'Đổ bê tông dầm sàn tầng 3');
});

test('TC-SYNC-03: T-70 - getPendingItems chỉ lấy các bản ghi PENDING hoặc FAILED chưa quá số lần retry', () => {
  syncQueueService.clearAll();

  const item1 = syncQueueService.enqueue({
    projectId: 'p1',
    payload: { workSummary: 'Công việc 1' }
  });
  const item2 = syncQueueService.enqueue({
    projectId: 'p1',
    payload: { workSummary: 'Công việc 2' }
  });

  // item2 thất bại 2 lần
  syncQueueService.markFailed(item2.id, 'Timeout kết nối');
  syncQueueService.markFailed(item2.id, 'Timeout kết nối');

  const pending = syncQueueService.getPendingItems();
  assert.equal(pending.length, 2);

  // item2 vượt quá số lần retry tối đa (5 lần)
  syncQueueService.markFailed(item2.id, 'Timeout');
  syncQueueService.markFailed(item2.id, 'Timeout');
  syncQueueService.markFailed(item2.id, 'Timeout');

  const pendingAfterMaxRetries = syncQueueService.getPendingItems();
  assert.equal(pendingAfterMaxRetries.length, 1);
  assert.equal(pendingAfterMaxRetries[0].id, item1.id);
});

test('TC-SYNC-04: T-71 - markSuccess xóa bản ghi khỏi hàng đợi sau khi gửi máy chủ thành công', () => {
  syncQueueService.clearAll();

  const item = syncQueueService.enqueue({
    projectId: 'p-success',
    payload: { workSummary: 'Gia công cốt thép' }
  });

  assert.equal(syncQueueService.getQueue().length, 1);

  // Gửi thành công
  syncQueueService.markSuccess(item.id);

  assert.equal(syncQueueService.getQueue().length, 0);
  assert.equal(syncQueueService.getStats().total, 0);
});

test('TC-SYNC-05: T-71 - markFailed cập nhật trạng thái FAILED và ghi nhận chi tiết lỗi', () => {
  syncQueueService.clearAll();

  const item = syncQueueService.enqueue({
    projectId: 'p-fail',
    payload: { workSummary: 'Lắp cốp pha' }
  });

  syncQueueService.markFailed(item.id, 'HTTP 500: Server Internal Error');

  const queue = syncQueueService.getQueue();
  assert.equal(queue[0].status, 'FAILED');
  assert.equal(queue[0].retryCount, 1);
  assert.equal(queue[0].lastError, 'HTTP 500: Server Internal Error');
});

test('TC-SYNC-06: T-70 - getStats tính toán đúng số lượng trạng thái hàng đợi', () => {
  syncQueueService.clearAll();

  const item1 = syncQueueService.enqueue({ payload: { workSummary: '1' } });
  const item2 = syncQueueService.enqueue({ payload: { workSummary: '2' } });
  const item3 = syncQueueService.enqueue({ payload: { workSummary: '3' } });

  syncQueueService.markSyncing(item2.id);
  syncQueueService.markFailed(item3.id, 'Lỗi mạng');

  const stats = syncQueueService.getStats();
  assert.equal(stats.total, 3);
  assert.equal(stats.pending, 1);
  assert.equal(stats.syncing, 1);
  assert.equal(stats.failed, 1);
  assert.equal(stats.hasPending, true);
});

test('TC-S29-01: Nhật ký tạo khi offline được lưu trên thiết bị với trạng thái chờ đồng bộ', () => {
  syncQueueService.clearAll();
  Object.defineProperty(globalThis.navigator, 'onLine', {
    value: false,
    configurable: true,
    writable: true
  });
  const diaryData = {
    diaryDate: '2026-10-09',
    weather: 'Mưa nhẹ',
    temperature: '27°C',
    engineerCount: 2,
    workerCount: 24,
    crewCount: 3,
    crewDetails: 'Tổ cốt thép: 8 người',
    equipmentStatus: 'Máy đào: 1 chiếc - hoạt động',
    workSummary: 'Đổ bê tông móng',
    workingConditions: 'Mặt bằng trơn do mưa',
    issues: 'Không có'
  };

  try {
    assert.equal(syncQueueService.isOnline(), false);
    const item = syncQueueService.enqueue({
      projectId: 'project-offline',
      entityType: 'SITE_DIARY',
      action: 'CREATE',
      endpoint: '/api/projects/project-offline/site-diaries/sync',
      method: 'POST',
      payload: diaryData,
      title: 'Nhật ký ngày 09/10/2026'
    });
    const storedQueue = JSON.parse(localStorage.getItem('ntdhtcct_offline_sync_queue_v1'));

    assert.equal(storedQueue.length, 1);
    assert.equal(storedQueue[0].status, 'PENDING');
    assert.equal(storedQueue[0].payload.crewDetails, diaryData.crewDetails);
    assert.equal(storedQueue[0].payload.workingConditions, diaryData.workingConditions);
    assert.equal(syncQueueService.getPendingItems()[0].id, item.id);
  } finally {
    Object.defineProperty(globalThis.navigator, 'onLine', {
      value: true,
      configurable: true,
      writable: true
    });
    syncQueueService.clearAll();
  }
});

test('TC-S29-02: Không báo lưu thành công nếu thiết bị không ghi được localStorage', () => {
  syncQueueService.clearAll();
  Object.defineProperty(globalThis.navigator, 'onLine', {
    value: false,
    configurable: true,
    writable: true
  });
  const originalSetItem = localStorage.setItem;
  const originalError = console.error;
  localStorage.setItem = () => {
    throw new Error('Storage full');
  };
  console.error = () => {};

  try {
    assert.throws(
      () => syncQueueService.enqueue({
        projectId: 'project-offline',
        endpoint: '/api/projects/project-offline/site-diaries/sync',
        payload: {
          diaryDate: '2026-10-09',
          workSummary: 'Đổ bê tông móng'
        }
      }),
      /Storage full/
    );
  } finally {
    localStorage.setItem = originalSetItem;
    console.error = originalError;
    Object.defineProperty(globalThis.navigator, 'onLine', {
      value: true,
      configurable: true,
      writable: true
    });
    syncQueueService.clearAll();
  }
});

test('TC-S34: Ghi 20 nhật ký khi mất mạng và đồng bộ đầy đủ khi kết nối trở lại', async () => {
  syncQueueService.clearAll();
  const originalAdapter = apiClient.defaults.adapter;
  const requests = [];
  Object.defineProperty(globalThis.navigator, 'onLine', {
    value: false,
    configurable: true,
    writable: true
  });
  apiClient.defaults.adapter = async config => {
    requests.push(config);
    return {
      data: { success: true },
      status: 200,
      statusText: 'OK',
      headers: {},
      config
    };
  };

  try {
    const submissions = [];
    for (let index = 1; index <= 20; index++) {
      submissions.push(await submitSiteDiary('project-s34', {
        diaryDate: `2026-10-${String(index).padStart(2, '0')}`,
        workSummary: `Nhật ký S-34 mục ${index}`
      }));
    }

    assert.ok(submissions.every(result => result.success && result.isOfflineQueued));
    assert.equal(syncQueueService.getPendingItems().length, 20);
    assert.equal(new Set(submissions.map(result => result.clientSyncId)).size, 20);

    Object.defineProperty(globalThis.navigator, 'onLine', {
      value: true,
      configurable: true,
      writable: true
    });
    const syncResult = await autoSyncService.syncQueueNow();

    assert.equal(syncResult.total, 20);
    assert.equal(syncResult.successCount, 20);
    assert.equal(syncResult.failCount, 0);
    assert.equal(requests.length, 20);
    assert.deepEqual(
      new Set(requests.map(request => JSON.parse(request.data).clientSyncId)),
      new Set(submissions.map(result => result.clientSyncId))
    );
    assert.equal(syncQueueService.getQueue().length, 0);
  } finally {
    apiClient.defaults.adapter = originalAdapter;
    Object.defineProperty(globalThis.navigator, 'onLine', {
      value: true,
      configurable: true,
      writable: true
    });
    syncQueueService.clearAll();
  }
});

// ============================================================================
// Story S-31 (NTDHTCT-215): Trạng thái chưa đồng bộ và số mục
//   - T-72 (NTDHTCT-257): Hiển thị trạng thái chưa đồng bộ của nhật ký
//   - T-73 (NTDHTCT-258): Hiển thị số lượng mục đang chờ đồng bộ
// ============================================================================

test('TC-S31-01: T-72 - Bản ghi ngoại tuyến có trạng thái "Chưa đồng bộ" và _isPendingSync = true', async () => {
  syncQueueService.clearAll();
  const projectId = 'p-s31-status';

  // Thêm một bản ghi vào hàng đợi ngoại tuyến
  const queueItem = syncQueueService.enqueue({
    projectId,
    entityType: 'SITE_DIARY',
    payload: {
      diaryDate: '2026-10-12',
      workSummary: 'Thi công ép cọc đại trà'
    }
  });

  assert.equal(queueItem.status, 'PENDING');

  // Lấy danh sách nhật ký qua service API
  const diaries = await getSiteDiaries(projectId);
  const target = diaries.find(d => d.clientSyncId === queueItem.clientSyncId);

  assert.ok(target, 'Bản ghi phải có trong danh sách');
  assert.equal(target._isPendingSync, true, 'Bản ghi ngoại tuyến phải có cờ _isPendingSync = true');
  assert.equal(target.syncStatus, 'Chưa đồng bộ', 'Trạng thái hiển thị phải là "Chưa đồng bộ"');
  assert.equal(target.rawSyncStatus, 'PENDING');

  syncQueueService.clearAll();
});

test('TC-S31-02: T-72 - Cập nhật trạng thái "Đang đồng bộ" và "Lỗi đồng bộ" kèm chi tiết lỗi', async () => {
  syncQueueService.clearAll();
  const projectId = 'p-s31-failed';

  const item1 = syncQueueService.enqueue({
    projectId,
    entityType: 'SITE_DIARY',
    payload: { diaryDate: '2026-10-11', workSummary: 'Công tác đào đất' }
  });

  const item2 = syncQueueService.enqueue({
    projectId,
    entityType: 'SITE_DIARY',
    payload: { diaryDate: '2026-10-12', workSummary: 'Đổ bê tông móng' }
  });

  // item1 chuyển sang SYNCING
  syncQueueService.markSyncing(item1.id);
  assert.equal(syncQueueService.getDiarySyncStatus(item1.clientSyncId), 'SYNCING');

  // item2 chuyển sang FAILED
  syncQueueService.markFailed(item2.id, 'Lỗi 503: Service Unavailable');
  assert.equal(syncQueueService.getDiarySyncStatus(item2.clientSyncId), 'FAILED');

  const diaries = await getSiteDiaries(projectId);
  const diary1 = diaries.find(d => d.clientSyncId === item1.clientSyncId);
  const diary2 = diaries.find(d => d.clientSyncId === item2.clientSyncId);

  assert.equal(diary1.syncStatus, 'Đang đồng bộ');
  assert.equal(diary1.rawSyncStatus, 'SYNCING');

  assert.equal(diary2.syncStatus, 'Lỗi đồng bộ');
  assert.equal(diary2.rawSyncStatus, 'FAILED');
  assert.equal(diary2._retryCount, 1);
  assert.equal(diary2._lastError, 'Lỗi 503: Service Unavailable');

  syncQueueService.clearAll();
});

test('TC-S31-03: T-72 - Kiểm tra hàm getDiarySyncStatus xác định đúng trạng thái', () => {
  syncQueueService.clearAll();

  const item = syncQueueService.enqueue({
    projectId: 'p-test-status',
    payload: { workSummary: 'Kiểm tra trạng thái' }
  });

  assert.equal(syncQueueService.getDiarySyncStatus(item.clientSyncId), 'PENDING');

  syncQueueService.markSyncing(item.id);
  assert.equal(syncQueueService.getDiarySyncStatus(item.clientSyncId), 'SYNCING');

  syncQueueService.markFailed(item.id, 'Timeout');
  assert.equal(syncQueueService.getDiarySyncStatus(item.clientSyncId), 'FAILED');

  // Khi đã đồng bộ thành công và xóa khỏi hàng đợi
  syncQueueService.markSuccess(item.id);
  assert.equal(syncQueueService.getDiarySyncStatus(item.clientSyncId), 'SYNCED');

  syncQueueService.clearAll();
});

test('TC-S31-04: T-73 - Tính toán chính xác số lượng mục đang chờ đồng bộ (pendingCount)', () => {
  syncQueueService.clearAll();

  const projectA = 'project-alpha';
  const projectB = 'project-beta';

  // Dự án A có 3 mục (2 PENDING, 1 FAILED)
  const a1 = syncQueueService.enqueue({ projectId: projectA, entityType: 'SITE_DIARY', payload: { w: 1 } });
  const a2 = syncQueueService.enqueue({ projectId: projectA, entityType: 'SITE_DIARY', payload: { w: 2 } });
  const a3 = syncQueueService.enqueue({ projectId: projectA, entityType: 'SITE_DIARY', payload: { w: 3 } });
  syncQueueService.markFailed(a3.id, 'Lỗi');

  // Dự án B có 2 mục (1 PENDING, 1 SYNCING)
  const b1 = syncQueueService.enqueue({ projectId: projectB, entityType: 'SITE_DIARY', payload: { w: 4 } });
  const b2 = syncQueueService.enqueue({ projectId: projectB, entityType: 'SITE_DIARY', payload: { w: 5 } });
  syncQueueService.markSyncing(b2.id);

  // Đếm theo từng dự án
  assert.equal(syncQueueService.getPendingCount(projectA), 3, 'Dự án A phải có 3 mục chờ đồng bộ');
  assert.equal(syncQueueService.getPendingCount(projectB), 2, 'Dự án B phải có 2 mục chờ đồng bộ');
  assert.equal(getPendingSiteDiaryCount(projectA), 3);
  assert.equal(getPendingSiteDiaryCount(projectB), 2);

  // Đếm toàn cục
  assert.equal(syncQueueService.getPendingCount(), 5, 'Tổng cộng phải có 5 mục chờ đồng bộ');
  const globalStats = syncQueueService.getStats();
  assert.equal(globalStats.pendingCount, 5);
  assert.equal(globalStats.pending, 3);
  assert.equal(globalStats.syncing, 1);
  assert.equal(globalStats.failed, 1);

  // Thống kê lọc theo dự án A
  const statsA = syncQueueService.getStats(projectA);
  assert.equal(statsA.total, 3);
  assert.equal(statsA.pendingCount, 3);
  assert.equal(statsA.failed, 1);

  syncQueueService.clearAll();
});

test('TC-S31-05: T-73 - Số lượng mục chờ đồng bộ tự động giảm chính xác sau khi đồng bộ thành công hoặc xóa', () => {
  syncQueueService.clearAll();
  const projectId = 'p-countdown';

  const item1 = syncQueueService.enqueue({ projectId, entityType: 'SITE_DIARY', payload: { w: 1 } });
  const item2 = syncQueueService.enqueue({ projectId, entityType: 'SITE_DIARY', payload: { w: 2 } });
  const item3 = syncQueueService.enqueue({ projectId, entityType: 'SITE_DIARY', payload: { w: 3 } });

  assert.equal(syncQueueService.getPendingCount(projectId), 3);

  // Đồng bộ item1 thành công
  syncQueueService.markSuccess(item1.id);
  assert.equal(syncQueueService.getPendingCount(projectId), 2, 'Còn 2 mục sau khi đồng bộ item1');

  // Xóa thủ công item2
  syncQueueService.remove(item2.id);
  assert.equal(syncQueueService.getPendingCount(projectId), 1, 'Còn 1 mục sau khi xóa item2');

  // Đồng bộ item3 thành công
  syncQueueService.markSuccess(item3.id);
  assert.equal(syncQueueService.getPendingCount(projectId), 0, 'Còn 0 mục khi hoàn tất tất cả');
  assert.equal(syncQueueService.getStats(projectId).hasPending, false);

  syncQueueService.clearAll();
});

test('TC-S31-06: T-72 & T-73 - Phân loại bộ lọc (Filter) giữa Chưa đồng bộ và Đã đồng bộ', async () => {
  syncQueueService.clearAll();
  const projectId = 'p-filter-test';

  const originalAdapter = apiClient.defaults.adapter;
  apiClient.defaults.adapter = async config => {
    // Trả về 2 bản ghi server
    return {
      data: [
        { id: 'server-1', projectId, diaryDate: '2026-10-01', workSummary: 'Đã lưu server 1', syncStatus: 'SYNCED' },
        { id: 'server-2', projectId, diaryDate: '2026-10-02', workSummary: 'Đã lưu server 2', syncStatus: 'SYNCED' }
      ],
      status: 200,
      statusText: 'OK',
      headers: {},
      config
    };
  };

  try {
    // Thêm 1 bản ghi offline
    syncQueueService.enqueue({
      projectId,
      entityType: 'SITE_DIARY',
      payload: { diaryDate: '2026-10-03', workSummary: 'Bản ghi offline mới' }
    });

    const list = await getSiteDiaries(projectId);
    assert.equal(list.length, 3, 'Tổng số bản ghi là 3');

    const unsyncedList = list.filter(d => d._isPendingSync);
    const syncedList = list.filter(d => !d._isPendingSync);

    assert.equal(unsyncedList.length, 1, 'Số lượng bản ghi chưa đồng bộ là 1 (T-73)');
    assert.equal(syncedList.length, 2, 'Số lượng bản ghi đã đồng bộ là 2');

    assert.equal(unsyncedList[0].syncStatus, 'Chưa đồng bộ', 'Mục chưa đồng bộ có nhãn đúng (T-72)');
    assert.equal(syncedList[0].syncStatus, 'Đã đồng bộ', 'Mục đã đồng bộ có nhãn đúng (T-72)');
  } finally {
    apiClient.defaults.adapter = originalAdapter;
    syncQueueService.clearAll();
  }
});

