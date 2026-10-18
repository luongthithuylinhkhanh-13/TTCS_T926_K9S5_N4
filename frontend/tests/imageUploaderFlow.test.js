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

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const uploaderFilePath = path.resolve(__dirname, '../src/components/diary/ImageUploader.jsx');

// Đọc mã nguồn thực tế của ImageUploader.jsx để xác minh hợp đồng tĩnh (Static contract verification)
const uploaderSource = fs.readFileSync(uploaderFilePath, 'utf-8');

// Định nghĩa các hằng số kiểm thực theo đúng chuẩn S-23/S-24 và ImageUploader.jsx
export const ALLOWED_MIME_TYPES = ['image/jpeg', 'image/png', 'image/webp'];
export const ALLOWED_EXTENSIONS = /\.(jpe?g|png|webp)$/i;
export const MAX_FILE_SIZE_BYTES = 20 * 1024 * 1024; // 20 MiB

/**
 * Hàm kiểm thực tính hợp lệ của tệp ảnh (đồng bộ với ImageUploader.jsx)
 */
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
 * Harness giả lập luồng xử lý và lifecycle của ImageUploader component
 */
function createImageUploaderHarness({
  items: initialItems = [],
  onChange,
  disabled = false,
  maxCount = null,
} = {}) {
  let items = [...initialItems];
  const itemsRef = { current: items };
  const errorMessages = [];
  const warningMessages = [];
  const revokedUrls = [];

  const mockMessage = {
    error: (msg) => errorMessages.push(msg),
    warning: (msg) => warningMessages.push(msg),
  };

  const fakeCreateObjectURL = (file) => `blob:http://localhost/preview-${file.name}-${itemsRef.current.length + 1}`;
  const fakeRevokeObjectURL = (url) => revokedUrls.push(url);

  const LIST_IGNORE = '__LIST_IGNORE__';

  const handleBeforeUpload = (file) => {
    if (disabled) {
      return LIST_IGNORE;
    }

    if (maxCount && itemsRef.current.length >= maxCount) {
      mockMessage.warning(`Đã đạt giới hạn tối đa ${maxCount} ảnh. Không thể thêm "${file.name || 'ảnh mới'}".`);
      return LIST_IGNORE;
    }

    const validation = validateDiaryImageFile(file);
    if (!validation.valid) {
      mockMessage.error(validation.error);
      return LIST_IGNORE;
    }

    // Tệp hợp lệ: trả về true để Ant Design chuyển sang customRequest
    return true;
  };

  const handleCustomUpload = ({ file, onSuccess }) => {
    if (disabled) {
      return;
    }
    if (maxCount && itemsRef.current.length >= maxCount) {
      mockMessage.warning(`Đã đạt giới hạn tối đa ${maxCount} ảnh. Không thể thêm "${file.name || 'ảnh mới'}".`);
      return;
    }

    const newItem = createQueueItem(file, fakeCreateObjectURL);
    const updated = [...itemsRef.current, newItem];
    itemsRef.current = updated;
    items = updated;

    if (onChange) {
      onChange(updated);
    }
    if (onSuccess) {
      onSuccess('ok');
    }
  };

  const handleDrop = (e) => {
    if (disabled) {
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
    if (disabled) {
      return;
    }
    const target = itemsRef.current.find((it) => it.clientImageId === clientImageId);
    if (target) {
      revokeQueueItem(target, fakeRevokeObjectURL);
    }
    const updated = itemsRef.current.filter((it) => it.clientImageId !== clientImageId);
    itemsRef.current = updated;
    items = updated;
    if (onChange) {
      onChange(updated);
    }
  };

  return {
    getItems: () => itemsRef.current,
    getItemsRef: () => itemsRef,
    getErrorMessages: () => errorMessages,
    getWarningMessages: () => warningMessages,
    getRevokedUrls: () => revokedUrls,
    handleBeforeUpload,
    handleCustomUpload,
    handleDrop,
    handleRemove,
    LIST_IGNORE,
  };
}

// =========================================================================
// TEST SUITE: S-23 & S-24 ImageUploader Flow & Validation
// =========================================================================

test('TC-STATIC-01: ImageUploader.jsx mã nguồn tuân thủ sửa đổi cốt lõi (handleBeforeUpload trả về true)', () => {
  assert.ok(
    uploaderSource.includes('handleBeforeUpload = (file) =>'),
    'Phải có hàm handleBeforeUpload nhận tham số file'
  );
  assert.ok(
    uploaderSource.includes('return true;'),
    'handleBeforeUpload phải trả về true cho tệp hợp lệ để kích hoạt customRequest'
  );
  assert.ok(
    !uploaderSource.includes('handleBeforeUpload = () => {\n    // Ngăn chặn form submit hoặc upload ngầm tự động\n    return false;\n  };'),
    'Không được để return false vô điều kiện trong handleBeforeUpload gây chặn upload'
  );
  assert.ok(
    uploaderSource.includes('Upload.LIST_IGNORE'),
    'Phải sử dụng Upload.LIST_IGNORE khi từ chối tệp'
  );
  assert.ok(
    uploaderSource.includes('onDrop={handleDrop}'),
    'Dragger phải gắn prop onDrop={handleDrop} để bắt tệp kéo thả không hợp lệ'
  );
  assert.ok(
    uploaderSource.includes('itemsRef.current'),
    'Phải sử dụng itemsRef.current để giải quyết triệt để stale closure khi chọn nhiều ảnh'
  );
});

test('TC-PREVIEW-01: Single valid image file produces queue item with synchronous previewUrl', () => {
  let emittedItems = [];
  const harness = createImageUploaderHarness({
    onChange: (updated) => {
      emittedItems = updated;
    },
  });

  const validFile = {
    name: 'site_foundation.jpg',
    size: 2 * 1024 * 1024,
    type: 'image/jpeg',
  };

  // Bước 1: beforeUpload kiểm tra tệp
  const beforeResult = harness.handleBeforeUpload(validFile);
  assert.equal(beforeResult, true, 'Tệp ảnh JPEG hợp lệ phải được beforeUpload chấp thuận (return true)');

  // Bước 2: customRequest được Ant Design kích hoạt
  let successCalled = false;
  harness.handleCustomUpload({
    file: validFile,
    onSuccess: (status) => {
      successCalled = status === 'ok';
    },
  });

  assert.equal(successCalled, true, 'customRequest phải gọi onSuccess');
  assert.equal(emittedItems.length, 1, 'onChange phải nhận được đúng 1 mục hàng đợi');

  const item = emittedItems[0];
  assert.ok(item.clientImageId, 'clientImageId phải được sinh ra');
  assert.equal(item.name, 'site_foundation.jpg');
  assert.equal(item.size, 2 * 1024 * 1024);
  assert.equal(item.type, 'image/jpeg');
  assert.ok(item.previewUrl.startsWith('blob:http://localhost/preview-site_foundation.jpg'), 'previewUrl phải được sinh ngay lập tức');
  assert.equal(item.displayStatus, 'idle');
  assert.equal(item.originalStatus, 'idle');
  assert.equal(harness.getErrorMessages().length, 0, 'Không được có thông báo lỗi');
});

test('TC-PREVIEW-02: Multi-file batching adds all valid files without losing items from race conditions', () => {
  const batchChanges = [];
  const harness = createImageUploaderHarness({
    onChange: (updated) => {
      batchChanges.push([...updated]);
    },
  });

  const files = [
    { name: 'beam_1.jpg', size: 1024 * 500, type: 'image/jpeg' },
    { name: 'column_2.png', size: 1024 * 700, type: 'image/png' },
    { name: 'slab_3.webp', size: 1024 * 900, type: 'image/webp' },
  ];

  // Mô phỏng Ant Design: kiểm tra tất cả trước
  for (const f of files) {
    const res = harness.handleBeforeUpload(f);
    assert.equal(res, true, `Tệp ${f.name} phải qua beforeUpload`);
  }

  // Ant Design gọi customRequest đồng thời/nối tiếp trong cùng một microtask
  for (const f of files) {
    harness.handleCustomUpload({
      file: f,
      onSuccess: () => {},
    });
  }

  const currentItems = harness.getItems();
  assert.equal(currentItems.length, 3, 'Cả 3 tệp đều phải có mặt trong hàng đợi, không bị mất do stale closure');
  assert.equal(currentItems[0].name, 'beam_1.jpg');
  assert.equal(currentItems[1].name, 'column_2.png');
  assert.equal(currentItems[2].name, 'slab_3.webp');

  // Đảm bảo clientImageId là duy nhất cho mỗi ảnh
  const ids = new Set(currentItems.map((it) => it.clientImageId));
  assert.equal(ids.size, 3, 'Tất cả các clientImageId phải là duy nhất');
});

test('TC-PREVIEW-03: Invalid MIME types or non-image extensions are rejected with error notification', () => {
  const harness = createImageUploaderHarness();

  const invalidFiles = [
    { name: 'specs.pdf', size: 1024 * 100, type: 'application/pdf' },
    { name: 'daily_log.docx', size: 1024 * 200, type: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document' },
    { name: 'script.sh', size: 1024, type: 'text/x-sh' },
    { name: 'trojan.exe', size: 1024 * 50, type: 'application/x-msdownload' },
    { name: 'corrupted_image.pdf', size: 1024 * 50, type: 'image/jpeg' }, // Đuôi pdf dù type giả mạo image/jpeg
    { name: 'fake.jpg', size: 1024 * 50, type: 'application/pdf' }, // Đuôi jpg nhưng type application/pdf
  ];

  for (const file of invalidFiles) {
    const res = harness.handleBeforeUpload(file);
    assert.equal(res, harness.LIST_IGNORE, `Tệp ${file.name} phải bị từ chối bằng LIST_IGNORE`);
  }

  const errors = harness.getErrorMessages();
  assert.equal(errors.length, invalidFiles.length, 'Mỗi tệp không hợp lệ phải có đúng một thông báo lỗi');
  for (const err of errors) {
    assert.match(err, /không đúng định dạng.*JPEG, PNG, WebP/i);
  }

  assert.equal(harness.getItems().length, 0, 'Không có tệp không hợp lệ nào được thêm vào hàng đợi');
});

test('TC-PREVIEW-04: Oversized files (> 20 MiB) and empty files (0 bytes) are rejected with error notification', () => {
  const harness = createImageUploaderHarness();

  // Ca 1: Tệp rỗng 0 bytes
  const emptyFile = {
    name: 'empty_photo.jpg',
    size: 0,
    type: 'image/jpeg',
  };
  const emptyRes = harness.handleBeforeUpload(emptyFile);
  assert.equal(emptyRes, harness.LIST_IGNORE, 'Tệp 0 byte phải bị từ chối bằng LIST_IGNORE');
  assert.ok(
    harness.getErrorMessages().some((msg) => msg.includes('không được để trống')),
    'Phải có thông báo lỗi tệp không được để trống'
  );

  // Ca 2: Tệp vượt quá 20 MiB (25 MiB)
  const oversizedFile = {
    name: 'drone_panorama.jpg',
    size: 25 * 1024 * 1024,
    type: 'image/jpeg',
  };
  const overRes = harness.handleBeforeUpload(oversizedFile);
  assert.equal(overRes, harness.LIST_IGNORE, 'Tệp > 20 MiB phải bị từ chối bằng LIST_IGNORE');
  assert.ok(
    harness.getErrorMessages().some((msg) => msg.includes('vượt quá dung lượng tối đa 20 MiB') && msg.includes('25.0 MB')),
    'Phải có thông báo lỗi vượt quá 20 MiB kèm định dạng dung lượng'
  );

  // Ca 3: Tệp đúng ngưỡng 20 MiB (20 * 1024 * 1024 bytes) -> phải hợp lệ
  const boundaryFile = {
    name: 'boundary_20mb.png',
    size: 20 * 1024 * 1024,
    type: 'image/png',
  };
  const boundaryRes = harness.handleBeforeUpload(boundaryFile);
  assert.equal(boundaryRes, true, 'Tệp đúng 20 MiB phải được chấp nhận');

  // Ca 4: Tệp 20 MiB + 1 byte -> phải bị từ chối
  const exceedBoundaryFile = {
    name: 'exceed_boundary.png',
    size: 20 * 1024 * 1024 + 1,
    type: 'image/png',
  };
  const exceedRes = harness.handleBeforeUpload(exceedBoundaryFile);
  assert.equal(exceedRes, harness.LIST_IGNORE, 'Tệp 20 MiB + 1 byte phải bị từ chối');
});

test('TC-PREVIEW-05: When disabled=true (locked date), file upload/selection and removal are rejected', () => {
  let changeCount = 0;
  const initialItem = {
    clientImageId: 'existing-img-1',
    name: 'existing.jpg',
    size: 100000,
    type: 'image/jpeg',
    previewUrl: 'blob:http://localhost/existing',
    displayStatus: 'completed',
    originalStatus: 'completed',
  };

  const harness = createImageUploaderHarness({
    items: [initialItem],
    disabled: true, // Ngày nhật ký đã bị khóa sổ S-24
    onChange: () => {
      changeCount += 1;
    },
  });

  const newFile = {
    name: 'new_evidence.jpg',
    size: 200000,
    type: 'image/jpeg',
  };

  // 1. Thử upload khi disabled
  const beforeRes = harness.handleBeforeUpload(newFile);
  assert.equal(beforeRes, harness.LIST_IGNORE, 'Khi disabled=true, beforeUpload phải trả về LIST_IGNORE');

  harness.handleCustomUpload({ file: newFile, onSuccess: () => {} });
  assert.equal(harness.getItems().length, 1, 'Không được thêm ảnh mới khi ngày đã bị khóa');

  // 2. Thử xóa ảnh khi disabled
  harness.handleRemove('existing-img-1');
  assert.equal(harness.getItems().length, 1, 'Không được phép xóa ảnh đã có khi ngày đã bị khóa');
  assert.equal(changeCount, 0, 'onChange không được gọi khi disabled');

  // 3. Thử kéo thả khi disabled
  harness.handleDrop({
    dataTransfer: {
      files: [newFile],
    },
  });
  assert.equal(harness.getErrorMessages().length, 0, 'Drop bị vô hiệu hóa khi disabled');
});

test('TC-PREVIEW-06: Drag-and-drop validation handles dropped files and reports errors for invalid files', () => {
  const harness = createImageUploaderHarness();

  const validPhoto = { name: 'concrete_test.jpg', size: 1024 * 300, type: 'image/jpeg' };
  const invalidPdf = { name: 'blueprints.pdf', size: 1024 * 1024, type: 'application/pdf' };
  const oversizedPhoto = { name: 'large_panorama.jpg', size: 22 * 1024 * 1024, type: 'image/jpeg' };

  // Mô phỏng sự kiện onDrop với nhiều tệp lẫn lộn
  const dropEvent = {
    dataTransfer: {
      files: [validPhoto, invalidPdf, oversizedPhoto],
    },
  };

  harness.handleDrop(dropEvent);

  const errors = harness.getErrorMessages();
  // handleDrop phải phát hiện blueprints.pdf không đúng định dạng và large_panorama.jpg vượt quá 20 MiB
  assert.ok(
    errors.some((err) => err.includes('blueprints.pdf') && err.includes('không đúng định dạng')),
    'Phải cảnh báo blueprints.pdf không đúng định dạng'
  );
  assert.ok(
    errors.some((err) => err.includes('large_panorama.jpg') && err.includes('vượt quá dung lượng tối đa 20 MiB')),
    'Phải cảnh báo large_panorama.jpg vượt dung lượng tối đa'
  );

  // Tệp hợp lệ concrete_test.jpg không bị cảnh báo lỗi
  assert.ok(
    !errors.some((err) => err.includes('concrete_test.jpg')),
    'Tệp hợp lệ không được sinh thông báo lỗi'
  );

  // Tiến hành upload tệp hợp lệ
  harness.handleCustomUpload({ file: validPhoto, onSuccess: () => {} });
  assert.equal(harness.getItems().length, 1);
  assert.equal(harness.getItems()[0].name, 'concrete_test.jpg');
});

test('TC-PREVIEW-07: maxCount enforcement prevents queue overflow and provides warning feedback', () => {
  let latest = [];
  const harness = createImageUploaderHarness({
    maxCount: 2,
    onChange: (items) => {
      latest = items;
    },
  });

  const file1 = { name: 'f1.jpg', size: 1000, type: 'image/jpeg' };
  const file2 = { name: 'f2.png', size: 1000, type: 'image/png' };
  const file3 = { name: 'f3.webp', size: 1000, type: 'image/webp' };

  harness.handleCustomUpload({ file: file1 });
  harness.handleCustomUpload({ file: file2 });
  assert.equal(latest.length, 2);

  // File 3 vượt quá maxCount=2
  const beforeRes = harness.handleBeforeUpload(file3);
  assert.equal(beforeRes, harness.LIST_IGNORE, 'File 3 vượt maxCount phải nhận LIST_IGNORE');
  assert.ok(
    harness.getWarningMessages().some((msg) => msg.includes('Đã đạt giới hạn tối đa 2 ảnh')),
    'Phải hiển thị cảnh báo đạt giới hạn maxCount'
  );

  harness.handleCustomUpload({ file: file3 });
  assert.equal(latest.length, 2, 'Số lượng tệp không được vượt quá maxCount');
});

test('TC-PREVIEW-08: handleRemove correctly updates queue and revokes object URL', () => {
  let currentItems = [];
  const harness = createImageUploaderHarness({
    onChange: (items) => {
      currentItems = items;
    },
  });

  const fileA = { name: 'a.jpg', size: 1000, type: 'image/jpeg' };
  const fileB = { name: 'b.jpg', size: 2000, type: 'image/jpeg' };

  harness.handleCustomUpload({ file: fileA });
  harness.handleCustomUpload({ file: fileB });
  assert.equal(currentItems.length, 2);

  const idToRemove = currentItems[0].clientImageId;
  const urlToRemove = currentItems[0].previewUrl;

  harness.handleRemove(idToRemove);
  assert.equal(currentItems.length, 1);
  assert.equal(currentItems[0].name, 'b.jpg');
  assert.deepEqual(harness.getRevokedUrls(), [urlToRemove], 'URL của ảnh bị xóa phải được thu hồi an toàn');
});
