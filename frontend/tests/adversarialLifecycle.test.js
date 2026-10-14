import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import {
  formatFileSize,
  createQueueItem,
  revokeQueueItem,
  revokeAllQueueItems,
} from '../src/utils/imageQueueUtils.js';
import { uploadQueueItemStages } from '../src/services/diaryUploadService.js';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const uploaderFilePath = path.resolve(__dirname, '../src/components/diary/ImageUploader.jsx');
const uploaderSource = fs.readFileSync(uploaderFilePath, 'utf-8');

// Quy tắc xác thực theo đúng ImageUploader.jsx
export const ALLOWED_MIME_TYPES = ['image/jpeg', 'image/png', 'image/webp'];
export const ALLOWED_EXTENSIONS = /\.(jpe?g|png|webp)$/i;
export const MAX_FILE_SIZE_BYTES = 20 * 1024 * 1024; // 20 MiB

export function validateDiaryImageFile(file) {
  if (!file) {
    return { valid: false, error: 'Vui lòng chọn tệp ảnh.' };
  }

  const fileName = file.name || '';
  const fileType = (file.type || '').toLowerCase();

  const hasValidExt = ALLOWED_EXTENSIONS.test(fileName);
  const hasValidMime = fileType ? ALLOWED_MIME_TYPES.includes(fileType) : false;

  if (!hasValidExt || (fileType && !hasValidMime)) {
    return {
      valid: false,
      error: `Tệp "${fileName || 'Không rõ'}" không đúng định dạng. Chỉ chấp nhận ảnh JPEG, PNG, WebP.`,
    };
  }

  if (typeof file.size === 'number' && file.size <= 0) {
    return {
      valid: false,
      error: `Tệp "${fileName}" không được để trống (dung lượng 0 byte).`,
    };
  }

  if (typeof file.size === 'number' && file.size > MAX_FILE_SIZE_BYTES) {
    return {
      valid: false,
      error: `Tệp "${fileName}" (${formatFileSize(file.size)}) vượt quá dung lượng tối đa 20 MiB.`,
    };
  }

  return { valid: true };
}

/**
 * Harness mô phỏng chính xác hành vi, closure và lifecycle của ImageUploader.jsx,
 * cho phép điều khiển linh hoạt props (disabled, maxCount, items)
 * và ghi nhận tất cả cảnh báo, lỗi, URL được tạo / thu hồi.
 */
function createAdversarialUploaderHarness(initialProps = {}) {
  let props = {
    items: [],
    disabled: false,
    maxCount: null,
    onChange: null,
    ...initialProps,
  };

  const itemsRef = { current: [...props.items] };
  const errorLogs = [];
  const warningLogs = [];
  const createdUrls = new Set();
  const revokedUrls = [];

  const LIST_IGNORE = '__LIST_IGNORE__';

  const mockMessage = {
    error: (msg) => errorLogs.push(msg),
    warning: (msg) => warningLogs.push(msg),
  };

  const fakeCreateObjectURL = (file) => {
    const url = `blob:http://localhost/blob-${file.name}-${Math.random().toString(36).slice(2, 9)}`;
    createdUrls.add(url);
    return url;
  };

  const fakeRevokeObjectURL = (url) => {
    revokedUrls.push(url);
  };

  // Cập nhật props động (như khi React re-render cha)
  const setProps = (newProps) => {
    props = { ...props, ...newProps };
    if ('items' in newProps) {
      itemsRef.current = [...newProps.items];
    }
  };

  const handleBeforeUpload = (file) => {
    if (props.disabled) {
      return LIST_IGNORE;
    }

    if (props.maxCount && itemsRef.current.length >= props.maxCount) {
      mockMessage.warning(`Đã đạt giới hạn tối đa ${props.maxCount} ảnh. Không thể thêm "${file.name || 'ảnh mới'}".`);
      return LIST_IGNORE;
    }

    const validation = validateDiaryImageFile(file);
    if (!validation.valid) {
      mockMessage.error(validation.error);
      return LIST_IGNORE;
    }

    return true;
  };

  const handleCustomUpload = ({ file, onSuccess }) => {
    if (props.disabled) {
      return;
    }
    if (props.maxCount && itemsRef.current.length >= props.maxCount) {
      mockMessage.warning(`Đã đạt giới hạn tối đa ${props.maxCount} ảnh. Không thể thêm "${file.name || 'ảnh mới'}".`);
      return;
    }

    const newItem = createQueueItem(file, fakeCreateObjectURL);
    const updated = [...itemsRef.current, newItem];
    itemsRef.current = updated;

    if (props.onChange) {
      props.onChange(updated);
    }
    if (onSuccess) {
      onSuccess('ok');
    }
  };

  const handleDrop = (e) => {
    if (props.disabled) {
      return;
    }
    const droppedFiles = Array.from(e.dataTransfer?.files || []);
    if (droppedFiles.length === 0) {
      return;
    }

    droppedFiles.forEach((file) => {
      const validation = validateDiaryImageFile(file);
      if (!validation.valid) {
        mockMessage.error(validation.error);
      }
    });
  };

  const handleRemove = (clientImageId) => {
    if (props.disabled) {
      return;
    }
    const target = itemsRef.current.find((it) => it.clientImageId === clientImageId);
    if (target) {
      revokeQueueItem(target, fakeRevokeObjectURL);
    }
    const updated = itemsRef.current.filter((it) => it.clientImageId !== clientImageId);
    itemsRef.current = updated;
    if (props.onChange) {
      props.onChange(updated);
    }
  };

  const unmount = () => {
    revokeAllQueueItems(itemsRef.current, fakeRevokeObjectURL);
  };

  return {
    getItems: () => itemsRef.current,
    getItemsRef: () => itemsRef,
    getErrors: () => errorLogs,
    getWarnings: () => warningLogs,
    getCreatedUrls: () => createdUrls,
    getRevokedUrls: () => revokedUrls,
    setProps,
    handleBeforeUpload,
    handleCustomUpload,
    handleDrop,
    handleRemove,
    unmount,
    LIST_IGNORE,
  };
}

// =========================================================================
// ADVERSARIAL TEST SUITE
// =========================================================================

test('ADV-01: Dynamic prop switching: disabled transitions false -> true -> false protects locked state without crash', () => {
  let queueState = [];
  const harness = createAdversarialUploaderHarness({
    disabled: false,
    onChange: (items) => {
      queueState = items;
      harness.setProps({ items });
    },
  });

  const file1 = { name: 'f1.jpg', size: 1024 * 100, type: 'image/jpeg' };
  const file2 = { name: 'f2.png', size: 1024 * 200, type: 'image/png' };
  const file3 = { name: 'f3.webp', size: 1024 * 150, type: 'image/webp' };

  // 1. Khi disabled = false: thêm 2 ảnh thành công
  assert.equal(harness.handleBeforeUpload(file1), true);
  harness.handleCustomUpload({ file: file1 });
  assert.equal(harness.handleBeforeUpload(file2), true);
  harness.handleCustomUpload({ file: file2 });

  assert.equal(queueState.length, 2, 'Queue phải có 2 ảnh ban đầu');
  const initialId1 = queueState[0].clientImageId;
  const initialUrl1 = queueState[0].previewUrl;

  // 2. Chuyển prop disabled = true (Ngày bị khóa sổ S-24 trong lúc Form đang mở)
  harness.setProps({ disabled: true });

  // 2a. Cố gắng thêm ảnh mới khi đã khóa
  const beforeBlocked = harness.handleBeforeUpload(file3);
  assert.equal(beforeBlocked, harness.LIST_IGNORE, 'handleBeforeUpload phải từ chối khi disabled=true');
  harness.handleCustomUpload({ file: file3 });
  assert.equal(queueState.length, 2, 'Queue không được tăng khi disabled=true');

  // 2b. Cố gắng kéo thả khi đã khóa
  harness.handleDrop({ dataTransfer: { files: [file3] } });
  assert.equal(harness.getErrors().length, 0, 'Drop phải bị vô hiệu hóa khi disabled=true');

  // 2c. Cố gắng xóa ảnh đã có khi đã khóa
  harness.handleRemove(initialId1);
  assert.equal(queueState.length, 2, 'Queue không được giảm khi disabled=true');
  assert.equal(harness.getRevokedUrls().length, 0, 'Không được thu hồi URL khi thao tác xóa bị từ chối');

  // 3. Chuyển prop disabled trở lại false (Người dùng chọn ngày chưa khóa)
  harness.setProps({ disabled: false });

  // 3a. Thêm ảnh file3 bây giờ phải thành công
  assert.equal(harness.handleBeforeUpload(file3), true);
  harness.handleCustomUpload({ file: file3 });
  assert.equal(queueState.length, 3, 'Queue phải thêm được file3 sau khi mở khóa');

  // 3b. Xóa ảnh file1 bây giờ phải thành công và thu hồi đúng Object URL
  harness.handleRemove(initialId1);
  assert.equal(queueState.length, 2, 'Queue phải còn 2 ảnh sau khi xóa file1');
  assert.equal(harness.getRevokedUrls().length, 1, 'Đúng 1 URL bị thu hồi');
  assert.equal(harness.getRevokedUrls()[0], initialUrl1, 'URL bị thu hồi phải là của file1');
  assert.ok(queueState.every((it) => it.clientImageId !== initialId1), 'file1 không còn trong queue');
});

test('ADV-02: Max count boundary: maxCount=5 with 6 concurrent files strictly caps queue at 5 without leaking memory', () => {
  let queueState = [];
  const harness = createAdversarialUploaderHarness({
    maxCount: 5,
    onChange: (items) => {
      queueState = items;
      harness.setProps({ items });
    },
  });

  const files = [
    { name: 'batch_1.jpg', size: 1000, type: 'image/jpeg' },
    { name: 'batch_2.jpg', size: 2000, type: 'image/jpeg' },
    { name: 'batch_3.jpg', size: 3000, type: 'image/jpeg' },
    { name: 'batch_4.jpg', size: 4000, type: 'image/jpeg' },
    { name: 'batch_5.jpg', size: 5000, type: 'image/jpeg' },
    { name: 'batch_6.jpg', size: 6000, type: 'image/jpeg' }, // Vượt giới hạn 5
  ];

  // Mô phỏng Ant Design multi-file drop: tất cả 6 tệp chạy beforeUpload khi queue đang rỗng
  const beforeResults = files.map((f) => harness.handleBeforeUpload(f));
  assert.deepEqual(
    beforeResults,
    [true, true, true, true, true, true],
    'Tất cả 6 tệp đều vượt qua beforeUpload khi queue rỗng'
  );

  // Sau đó Ant Design gọi customRequest đồng thời/liên tiếp cho cả 6 tệp trong cùng event loop
  let successCount = 0;
  for (const f of files) {
    harness.handleCustomUpload({
      file: f,
      onSuccess: () => {
        successCount++;
      },
    });
  }

  // Xác minh hàng đợi bị chặn nghiêm ngặt ở 5 tệp
  assert.equal(queueState.length, 5, 'Queue phải bị chặn đúng 5 tệp');
  assert.equal(successCount, 5, 'Chỉ 5 tệp được gọi onSuccess');
  assert.equal(harness.getItems().length, 5, 'itemsRef.current phải có đúng 5 tệp');

  // Xác minh tệp thứ 6 phát sinh cảnh báo
  const warnings = harness.getWarnings();
  assert.ok(warnings.length >= 1, 'Phải có ít nhất 1 cảnh báo vượt maxCount');
  assert.ok(
    warnings.some((w) => w.includes('Đã đạt giới hạn tối đa 5 ảnh') && w.includes('batch_6.jpg')),
    'Cảnh báo phải nêu rõ giới hạn 5 ảnh và tên tệp vi phạm batch_6.jpg'
  );

  // Xác minh không rò rỉ bộ nhớ: createQueueItem chỉ được gọi cho 5 tệp thành công
  assert.equal(harness.getCreatedUrls().size, 5, 'Chỉ sinh 5 Object URL, tệp thứ 6 không được tạo URL');

  // Thử thêm tiếp tệp thứ 7 một cách tuần tự khi queue đã đủ 5 tệp
  const file7 = { name: 'batch_7.jpg', size: 7000, type: 'image/jpeg' };
  const beforeRes7 = harness.handleBeforeUpload(file7);
  assert.equal(beforeRes7, harness.LIST_IGNORE, 'beforeUpload phải chặn ngay tệp thứ 7 khi queue đã đầy');
  assert.equal(queueState.length, 5, 'Queue vẫn giữ nguyên 5 tệp');
});

test('ADV-03: Removal synchronization: add 4, remove 2, add 2 keeps itemsRef in sync and revokes URLs cleanly', () => {
  let queueState = [];
  const harness = createAdversarialUploaderHarness({
    onChange: (items) => {
      queueState = items;
      harness.setProps({ items });
    },
  });

  const f1 = { name: 'photo_1.jpg', size: 1000, type: 'image/jpeg' };
  const f2 = { name: 'photo_2.png', size: 2000, type: 'image/png' };
  const f3 = { name: 'photo_3.webp', size: 3000, type: 'image/webp' };
  const f4 = { name: 'photo_4.jpg', size: 4000, type: 'image/jpeg' };

  // 1. Thêm 4 ảnh
  [f1, f2, f3, f4].forEach((f) => {
    assert.equal(harness.handleBeforeUpload(f), true);
    harness.handleCustomUpload({ file: f });
  });

  assert.equal(queueState.length, 4);
  const [item1, item2, item3, item4] = queueState;
  assert.equal(harness.getCreatedUrls().size, 4);

  // 2. Xóa 2 ảnh: item2 (ở giữa) và item4 (ở cuối)
  harness.handleRemove(item2.clientImageId);
  harness.handleRemove(item4.clientImageId);

  assert.equal(queueState.length, 2);
  assert.equal(harness.getItems().length, 2);
  assert.deepEqual(
    queueState.map((it) => it.name),
    ['photo_1.jpg', 'photo_3.webp'],
    'Chỉ còn photo_1 và photo_3 trong queue'
  );

  // Kiểm tra URL bị thu hồi đúng là của item2 và item4
  assert.deepEqual(
    harness.getRevokedUrls(),
    [item2.previewUrl, item4.previewUrl],
    'Chỉ thu hồi URL của item2 và item4'
  );

  // 3. Thêm 2 ảnh mới: f5 và f6
  const f5 = { name: 'photo_5.jpg', size: 5000, type: 'image/jpeg' };
  const f6 = { name: 'photo_6.png', size: 6000, type: 'image/png' };
  [f5, f6].forEach((f) => {
    assert.equal(harness.handleBeforeUpload(f), true);
    harness.handleCustomUpload({ file: f });
  });

  assert.equal(queueState.length, 4);
  assert.deepEqual(
    queueState.map((it) => it.name),
    ['photo_1.jpg', 'photo_3.webp', 'photo_5.jpg', 'photo_6.png'],
    'Queue có đúng 4 ảnh [1, 3, 5, 6]'
  );

  // itemsRef.current phải đồng bộ 100% với queueState
  assert.deepEqual(harness.getItems(), queueState);

  // 4. Giả lập unmount component (người dùng đóng Form hoặc chuyển trang)
  harness.unmount();
  // Tất cả 6 ảnh đã từng tạo (2 ảnh bị xóa trước đó + 4 ảnh đang còn) đều phải được thu hồi
  assert.equal(
    harness.getRevokedUrls().length,
    6,
    'Tất cả 6 Object URL đã tạo đều được thu hồi hoàn toàn khi unmount, không rò rỉ bộ nhớ'
  );
});

test('ADV-04: Form submission contract compatibility: queue items conform to uploadQueueItemStages expectations', async () => {
  let queueState = [];
  const harness = createAdversarialUploaderHarness({
    onChange: (items) => {
      queueState = items;
      harness.setProps({ items });
    },
  });

  const rawFile = {
    name: 'site_survey.jpg',
    size: 1024 * 500,
    type: 'image/jpeg',
  };

  harness.handleCustomUpload({ file: rawFile });
  assert.equal(queueState.length, 1);
  const queueItem = queueState[0];

  // 1. Kiểm tra cấu trúc contract của queueItem
  assert.ok(typeof queueItem.clientImageId === 'string' && queueItem.clientImageId.length > 0, 'clientImageId hợp lệ');
  assert.equal(queueItem.name, 'site_survey.jpg');
  assert.equal(queueItem.size, 1024 * 500);
  assert.equal(queueItem.type, 'image/jpeg');
  assert.ok(typeof queueItem.previewUrl === 'string' && queueItem.previewUrl.length > 0, 'previewUrl hợp lệ');
  assert.equal(queueItem.displayStatus, 'idle');
  assert.equal(queueItem.originalStatus, 'idle');
  assert.equal(queueItem.displayError, null);
  assert.equal(queueItem.originalError, null);
  assert.equal(queueItem.file, rawFile);

  // 2. Chạy qua luồng uploadQueueItemStages trong SiteDiaryPage (thành công)
  // Mock image compressor & upload APIs cho môi trường Node test
  const recordedUpdates = [];
  const mockApiFns = {
    uploadDisplayImage: async (projId, entryId, clientImgId, name, blob) => {
      assert.equal(projId, 'proj-123');
      assert.equal(entryId, 'entry-456');
      assert.equal(clientImgId, queueItem.clientImageId);
      assert.equal(name, 'site_survey.jpg');
      assert.ok(blob, 'Display image blob phải tồn tại');
      return { success: true };
    },
    uploadOriginalImage: async (projId, entryId, clientImgId, name, file) => {
      assert.equal(projId, 'proj-123');
      assert.equal(entryId, 'entry-456');
      assert.equal(clientImgId, queueItem.clientImageId);
      assert.equal(name, 'site_survey.jpg');
      assert.equal(file, rawFile);
      return { success: true };
    },
  };

  // Do môi trường Node không có Image DOM / Canvas, ta kiểm tra giai đoạn original và mock display
  const finalItem = await uploadQueueItemStages(
    'proj-123',
    'entry-456',
    queueItem,
    {
      onUpdate: (upd) => recordedUpdates.push(upd),
    },
    { originalOnly: true },
    mockApiFns
  );

  assert.equal(finalItem.originalStatus, 'completed');
  assert.equal(finalItem.originalError, null);

  // 3. Mô phỏng luồng retry độc lập khi ảnh gốc bị lỗi mạng
  const failingOriginalApiFns = {
    uploadDisplayImage: async () => ({ success: true }),
    uploadOriginalImage: async () => {
      throw new Error('Connection reset by peer (504 Gateway Timeout)');
    },
  };

  const failedItem = await uploadQueueItemStages(
    'proj-123',
    'entry-456',
    queueItem,
    {},
    { originalOnly: true },
    failingOriginalApiFns
  );

  assert.equal(failedItem.originalStatus, 'failed', 'Ảnh gốc thất bại');
  assert.ok(failedItem.originalError.includes('Connection reset'), 'Lưu chi tiết lỗi');

  // Thử lại riêng ảnh gốc (originalOnly = true)
  const retryingApiFns = {
    uploadDisplayImage: async () => {
      throw new Error('Should not be called');
    },
    uploadOriginalImage: async () => ({ success: true, retried: true }),
  };

  const retriedItem = await uploadQueueItemStages(
    'proj-123',
    'entry-456',
    failedItem,
    {},
    { originalOnly: true },
    retryingApiFns
  );

  assert.equal(retriedItem.originalStatus, 'completed');
  assert.equal(retriedItem.originalError, null);
});

test('ADV-05: Robustness against non-existent ID removal and interleaved rapid mutations', () => {
  let queue = [];
  const harness = createAdversarialUploaderHarness({
    onChange: (items) => {
      queue = items;
      harness.setProps({ items });
    },
  });

  // Thêm 10 ảnh
  for (let i = 1; i <= 10; i++) {
    const f = { name: `bulk_${i}.jpg`, size: i * 1000, type: 'image/jpeg' };
    harness.handleCustomUpload({ file: f });
  }
  assert.equal(queue.length, 10);

  // Xóa ID không tồn tại
  harness.handleRemove('non-existent-uuid-999');
  assert.equal(queue.length, 10, 'Queue không bị ảnh hưởng khi xóa id không tồn tại');
  assert.equal(harness.getRevokedUrls().length, 0, 'Không thu hồi URL nào khi ID không tồn tại');

  // Xóa 5 ảnh có chỉ số chẵn
  const removedIds = [];
  for (let i = 0; i < 5; i++) {
    const target = queue[i]; // lấy ảnh
    removedIds.push(target.clientImageId);
    harness.handleRemove(target.clientImageId);
  }

  assert.equal(queue.length, 5);
  assert.equal(harness.getRevokedUrls().length, 5);

  // Thêm tiếp 3 ảnh nữa
  for (let i = 11; i <= 13; i++) {
    const f = { name: `bulk_${i}.png`, size: i * 1000, type: 'image/png' };
    harness.handleCustomUpload({ file: f });
  }

  assert.equal(queue.length, 8);
  assert.equal(harness.getItems().length, 8);

  // Dọn dẹp cuối cùng
  harness.unmount();
  assert.equal(harness.getRevokedUrls().length, 13, 'Toàn bộ 13 ảnh đã tạo đều được thu hồi an toàn');
});
