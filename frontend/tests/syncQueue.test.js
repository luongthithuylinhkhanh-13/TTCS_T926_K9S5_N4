import assert from 'node:assert/strict';
import test from 'node:test';
import { syncQueueService, generateUUID } from '../src/services/syncQueueService.js';

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
