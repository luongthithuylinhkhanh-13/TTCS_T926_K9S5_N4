
const DB_NAME = 'construction-platform-image-uploads';
const DB_VERSION = 1;
const STORE_NAME = 'imageUploadQueue';

const MAX_FILE_SIZE = 20 * 1024 * 1024;
const ALLOWED_TYPES = ['image/jpeg', 'image/png', 'image/webp', 'image/gif'];

const createId = () => {
  if (globalThis.crypto?.randomUUID) {
    return globalThis.crypto.randomUUID();
  }

  return `${Date.now()}-${Math.random().toString(16).slice(2)}`;
};

const openDatabase = () =>
  new Promise((resolve, reject) => {
    if (typeof indexedDB === 'undefined') {
      reject(new Error('Trình duyệt không hỗ trợ IndexedDB.'));
      return;
    }

    const request = indexedDB.open(DB_NAME, DB_VERSION);

    request.onupgradeneeded = () => {
      const database = request.result;

      if (!database.objectStoreNames.contains(STORE_NAME)) {
        const store = database.createObjectStore(STORE_NAME, {
          keyPath: 'id'
        });

        store.createIndex('status', 'status', { unique: false });
        store.createIndex('createdAt', 'createdAt', { unique: false });
      }
    };

    request.onsuccess = () => resolve(request.result);
    request.onerror = () => reject(request.error);
  });

const runTransaction = async (mode, operation) => {
  const database = await openDatabase();

  return new Promise((resolve, reject) => {
    const transaction = database.transaction(STORE_NAME, mode);
    const store = transaction.objectStore(STORE_NAME);

    let result;

    try {
      result = operation(store);
    } catch (error) {
      database.close();
      reject(error);
      return;
    }

    transaction.oncomplete = () => {
      database.close();
      resolve(result?.result);
    };

    transaction.onerror = () => {
      database.close();
      reject(transaction.error || new Error('Không thể thao tác hàng đợi ảnh.'));
    };

    transaction.onabort = () => {
      database.close();
      reject(transaction.error || new Error('Thao tác hàng đợi ảnh đã bị hủy.'));
    };
  });
};

const requestResult = (request) =>
  new Promise((resolve, reject) => {
    request.onsuccess = () => resolve(request.result);
    request.onerror = () => reject(request.error);
  });

export const imageUploadQueueService = {
  MAX_FILE_SIZE,

  validateFile(file) {
    if (!file) {
      throw new Error('Vui lòng chọn ảnh.');
    }

    if (!ALLOWED_TYPES.includes(file.type)) {
      throw new Error('Chỉ chấp nhận ảnh JPEG, PNG, WEBP hoặc GIF.');
    }

    if (file.size <= 0) {
      throw new Error('File ảnh không được để trống.');
    }

    if (file.size > MAX_FILE_SIZE) {
      throw new Error('Ảnh vượt quá giới hạn 20 MB.');
    }

    return true;
  },

  async enqueue(file, metadata = {}) {
    this.validateFile(file);

    const now = new Date().toISOString();

    const item = {
      id: createId(),
      file,
      fileName: file.name,
      fileType: file.type,
      fileSize: file.size,
      metadata,
      status: 'PENDING',
      retryCount: 0,
      lastError: null,
      createdAt: now,
      updatedAt: now
    };

    await runTransaction('readwrite', store => store.add(item));

    return {
      ...item,
      file: undefined
    };
  },

  async getAll() {
    const database = await openDatabase();

    try {
      const transaction = database.transaction(STORE_NAME, 'readonly');
      const request = transaction.objectStore(STORE_NAME).getAll();
      const items = await requestResult(request);

      return items
        .map(({ file, ...item }) => item)
        .sort((a, b) => a.createdAt.localeCompare(b.createdAt));
    } finally {
      database.close();
    }
  },

  async getPendingItems() {
    const database = await openDatabase();

    try {
      const transaction = database.transaction(STORE_NAME, 'readonly');
      const request = transaction.objectStore(STORE_NAME).getAll();
      const items = await requestResult(request);

      return items
        .filter(item => item.status === 'PENDING' || item.status === 'FAILED')
        .sort((a, b) => a.createdAt.localeCompare(b.createdAt));
    } finally {
      database.close();
    }
  },

  async getFile(id) {
    const database = await openDatabase();

    try {
      const transaction = database.transaction(STORE_NAME, 'readonly');
      const request = transaction.objectStore(STORE_NAME).get(id);
      const item = await requestResult(request);

      return item || null;
    } finally {
      database.close();
    }
  },

  async updateStatus(id, status, errorMessage = null) {
    const allowedStatuses = ['PENDING', 'UPLOADING', 'COMPLETED', 'FAILED'];

    if (!allowedStatuses.includes(status)) {
      throw new Error('Trạng thái hàng đợi ảnh không hợp lệ.');
    }

    const database = await openDatabase();

    try {
      return await new Promise((resolve, reject) => {
        const transaction = database.transaction(STORE_NAME, 'readwrite');
        const store = transaction.objectStore(STORE_NAME);
        const request = store.get(id);

        request.onsuccess = () => {
          const item = request.result;

          if (!item) {
            reject(new Error('Không tìm thấy ảnh trong hàng đợi.'));
            return;
          }

          item.status = status;
          item.updatedAt = new Date().toISOString();
          item.lastError = errorMessage;

          if (status === 'FAILED') {
            item.retryCount += 1;
          }

          store.put(item);
        };

        transaction.oncomplete = () => resolve(true);
        transaction.onerror = () => reject(transaction.error);
        transaction.onabort = () => reject(transaction.error);
      });
    } finally {
      database.close();
    }
  },

  async remove(id) {
    await runTransaction('readwrite', store => store.delete(id));
  },

  async getStats() {
    const items = await this.getAll();

    return {
      total: items.length,
      pending: items.filter(item => item.status === 'PENDING').length,
      uploading: items.filter(item => item.status === 'UPLOADING').length,
      completed: items.filter(item => item.status === 'COMPLETED').length,
      failed: items.filter(item => item.status === 'FAILED').length
    };
  }
};

export default imageUploadQueueService;
