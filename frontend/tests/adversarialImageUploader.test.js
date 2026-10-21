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
const uploaderSource = fs.readFileSync(uploaderFilePath, 'utf-8');

// =========================================================================
// EMPIRICAL EXTRACTION: Dynamically evaluate validateDiaryImageFile directly
// from ImageUploader.jsx source to guarantee zero discrepancies.
// =========================================================================
const validationSectionMatch = uploaderSource.match(
  /(export const ALLOWED_MIME_TYPES[\s\S]*?export function validateDiaryImageFile[\s\S]*?\n})/
);

assert.ok(validationSectionMatch, 'Must locate validateDiaryImageFile and constants in ImageUploader.jsx');

// Prepare isolated evaluator by stripping 'export ' and returning the symbols
const cleanCodeToEval = `
  ${validationSectionMatch[1].replace(/export /g, '')}
  return { ALLOWED_MIME_TYPES, ALLOWED_EXTENSIONS, MAX_FILE_SIZE_BYTES, validateDiaryImageFile };
`;

const evalFunc = new Function('formatFileSize', cleanCodeToEval);
const {
  ALLOWED_MIME_TYPES,
  ALLOWED_EXTENSIONS,
  MAX_FILE_SIZE_BYTES,
  validateDiaryImageFile,
} = evalFunc(formatFileSize);

assert.equal(typeof validateDiaryImageFile, 'function', 'Extracted validateDiaryImageFile must be a function');
assert.equal(MAX_FILE_SIZE_BYTES, 20 * 1024 * 1024, 'MAX_FILE_SIZE_BYTES must be 20 MiB');

/**
 * ImageUploader logic harness faithful to ImageUploader.jsx
 */
function createAdversarialUploaderHarness({
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

  const fakeCreateObjectURL = (file) => `blob:http://localhost/adversarial-${file.name}-${Math.random().toString(36).slice(2, 9)}`;
  const fakeRevokeObjectURL = (url) => revokedUrls.push(url);

  const LIST_IGNORE = '__LIST_IGNORE__';

  const handleBeforeUpload = (file) => {
    if (disabled) {
      return LIST_IGNORE;
    }

    if (maxCount && itemsRef.current.length >= maxCount) {
      mockMessage.warning(`Đã đạt giới hạn tối đa ${maxCount} ảnh. Không thể thêm "${file?.name || 'ảnh mới'}".`);
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
    if (disabled) {
      return;
    }
    if (maxCount && itemsRef.current.length >= maxCount) {
      mockMessage.warning(`Đã đạt giới hạn tối đa ${maxCount} ảnh. Không thể thêm "${file?.name || 'ảnh mới'}".`);
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

  const unmount = () => {
    revokeAllQueueItems(itemsRef.current, fakeRevokeObjectURL);
  };

  return {
    getItems: () => itemsRef.current,
    getErrorMessages: () => errorMessages,
    getWarningMessages: () => warningMessages,
    getRevokedUrls: () => revokedUrls,
    handleBeforeUpload,
    handleCustomUpload,
    handleDrop,
    handleRemove,
    unmount,
    LIST_IGNORE,
  };
}

// =========================================================================
// CHALLENGE SUITE 1: Extreme File Size Boundaries
// =========================================================================

test('ADV-SIZE-01: Exactly 20 MiB boundary (20 * 1024 * 1024 bytes) is accepted', () => {
  const file = {
    name: 'exact_20mb.jpg',
    size: 20 * 1024 * 1024,
    type: 'image/jpeg',
  };
  const result = validateDiaryImageFile(file);
  assert.equal(result.valid, true, 'Exactly 20 MiB must be accepted');
  assert.equal(result.error, undefined);
});

test('ADV-SIZE-02: 20 MiB + 1 byte boundary is rejected with proper error and formatting', () => {
  const file = {
    name: 'exceed_20mb_plus_1.png',
    size: 20 * 1024 * 1024 + 1,
    type: 'image/png',
  };
  const result = validateDiaryImageFile(file);
  assert.equal(result.valid, false, '20 MiB + 1 byte must be rejected');
  assert.match(result.error, /vượt quá dung lượng tối đa 20 MiB/);
  assert.match(result.error, /20\.0 MB/);
});

test('ADV-SIZE-03: Zero bytes (0 bytes) is rejected as empty file', () => {
  const file = {
    name: 'zero_byte.webp',
    size: 0,
    type: 'image/webp',
  };
  const result = validateDiaryImageFile(file);
  assert.equal(result.valid, false, '0 byte file must be rejected');
  assert.match(result.error, /không được để trống \(dung lượng 0 byte\)/);
});

test('ADV-SIZE-04: Negative byte count (-1 byte) is rejected', () => {
  const file = {
    name: 'negative.jpg',
    size: -1,
    type: 'image/jpeg',
  };
  const result = validateDiaryImageFile(file);
  assert.equal(result.valid, false, 'Negative size must be rejected');
  assert.match(result.error, /không được để trống/);
});

test('ADV-SIZE-05: 1 byte non-empty file is accepted', () => {
  const file = {
    name: 'minimal_1byte.png',
    size: 1,
    type: 'image/png',
  };
  const result = validateDiaryImageFile(file);
  assert.equal(result.valid, true, '1 byte file must be accepted');
});

test('ADV-SIZE-06: Fractional byte size exceeding boundary is rejected', () => {
  const file = {
    name: 'fractional_exceed.jpg',
    size: 20 * 1024 * 1024 + 0.5,
    type: 'image/jpeg',
  };
  const result = validateDiaryImageFile(file);
  assert.equal(result.valid, false, 'Fractional bytes over boundary must be rejected');
});

test('ADV-SIZE-07: Astronomical size (100 GiB and Number.MAX_SAFE_INTEGER) is rejected', () => {
  const file100Gb = {
    name: 'huge_100gb.jpg',
    size: 100 * 1024 * 1024 * 1024,
    type: 'image/jpeg',
  };
  const res1 = validateDiaryImageFile(file100Gb);
  assert.equal(res1.valid, false);

  const fileMaxSafe = {
    name: 'max_safe.png',
    size: Number.MAX_SAFE_INTEGER,
    type: 'image/png',
  };
  const res2 = validateDiaryImageFile(fileMaxSafe);
  assert.equal(res2.valid, false);

  const fileInfinity = {
    name: 'infinity.webp',
    size: Infinity,
    type: 'image/webp',
  };
  const res3 = validateDiaryImageFile(fileInfinity);
  assert.equal(res3.valid, false);
});

// =========================================================================
// CHALLENGE SUITE 2: Case Sensitivity and Extension Permutations
// =========================================================================

test('ADV-CASE-01: Case variations of supported extensions (.JPG, .PNG, .WebP, .JpEg) are accepted', () => {
  const validExtensions = [
    { name: 'photo.JPG', type: 'image/jpeg' },
    { name: 'photo.PNG', type: 'image/png' },
    { name: 'photo.WEBP', type: 'image/webp' },
    { name: 'photo.JPEG', type: 'image/jpeg' },
    { name: 'photo.JpEg', type: 'image/jpeg' },
    { name: 'photo.PnG', type: 'image/png' },
    { name: 'photo.wEbP', type: 'image/webp' },
    { name: 'photo.jPg', type: 'image/jpeg' },
  ];

  for (const item of validExtensions) {
    const res = validateDiaryImageFile({ ...item, size: 1024 });
    assert.equal(res.valid, true, `File ${item.name} must be accepted regardless of case`);
  }
});

test('ADV-CASE-02: MIME type casing variations (IMAGE/JPEG, image/PNG, Image/WebP) are normalized and accepted', () => {
  const mimeVariations = [
    { name: 'test1.jpg', type: 'IMAGE/JPEG' },
    { name: 'test2.png', type: 'image/PNG' },
    { name: 'test3.webp', type: 'Image/WebP' },
    { name: 'test4.jpeg', type: 'IMAGE/JPEG' },
  ];

  for (const item of mimeVariations) {
    const res = validateDiaryImageFile({ ...item, size: 2048 });
    assert.equal(res.valid, true, `MIME type ${item.type} must be normalized to lowercase and accepted`);
  }
});

test('ADV-CASE-03: Disallowed extensions are strictly rejected regardless of case (.PDF, .EXE, .SVG, .GIF, .HTML)', () => {
  const forbiddenFiles = [
    { name: 'virus.EXE', type: 'application/x-msdownload' },
    { name: 'drawing.SVG', type: 'image/svg+xml' },
    { name: 'animation.GIF', type: 'image/gif' },
    { name: 'document.PDF', type: 'application/pdf' },
    { name: 'page.HTML', type: 'text/html' },
    { name: 'script.JS', type: 'text/javascript' },
    { name: 'archive.ZIP', type: 'application/zip' },
    { name: 'vector.Ai', type: 'application/postscript' },
    { name: 'image.bmp', type: 'image/bmp' },
    { name: 'image.tiff', type: 'image/tiff' },
  ];

  for (const item of forbiddenFiles) {
    const res = validateDiaryImageFile({ ...item, size: 5000 });
    assert.equal(res.valid, false, `Forbidden file ${item.name} must be rejected`);
    assert.match(res.error, /không đúng định dạng/i);
  }
});

// =========================================================================
// CHALLENGE SUITE 3: MIME Type Mismatch & Extension Spoofing
// =========================================================================

test('ADV-MIME-01: Valid extension with spoofed/malicious MIME type is rejected', () => {
  const spoofedFiles = [
    { name: 'innocent.jpg', type: 'application/pdf', size: 1000 },
    { name: 'innocent.png', type: 'text/html', size: 1000 },
    { name: 'innocent.webp', type: 'application/x-msdownload', size: 1000 },
    { name: 'innocent.jpeg', type: 'application/javascript', size: 1000 },
    { name: 'innocent.jpg', type: 'image/gif', size: 1000 }, // GIF not allowed in diary preview
  ];

  for (const file of spoofedFiles) {
    const res = validateDiaryImageFile(file);
    assert.equal(res.valid, false, `Spoofed MIME ${file.type} on ${file.name} must be rejected`);
    assert.match(res.error, /không đúng định dạng/i);
  }
});

test('ADV-MIME-02: Valid MIME type with invalid extension is rejected', () => {
  const invalidExtFiles = [
    { name: 'contract.pdf', type: 'image/jpeg', size: 1000 },
    { name: 'exploit.exe', type: 'image/png', size: 1000 },
    { name: 'script.sh', type: 'image/webp', size: 1000 },
    { name: 'vector.svg', type: 'image/jpeg', size: 1000 },
    { name: 'noextension', type: 'image/jpeg', size: 1000 },
  ];

  for (const file of invalidExtFiles) {
    const res = validateDiaryImageFile(file);
    assert.equal(res.valid, false, `Invalid extension ${file.name} with spoofed MIME ${file.type} must be rejected`);
    assert.match(res.error, /không đúng định dạng/i);
  }
});

test('ADV-MIME-03: Double extension attack (.jpg.exe vs .exe.jpg)', () => {
  // 1. .jpg.exe -> should be rejected because terminal extension is .exe
  const doubleExtMalware = {
    name: 'evidence.jpg.exe',
    type: 'application/x-msdownload',
    size: 2000,
  };
  const res1 = validateDiaryImageFile(doubleExtMalware);
  assert.equal(res1.valid, false, 'File ending in .exe must be rejected');

  // 2. .exe.jpg with mismatched MIME -> rejected
  const fakeExtWithBadMime = {
    name: 'document.exe.jpg',
    type: 'application/x-msdownload',
    size: 2000,
  };
  const res2 = validateDiaryImageFile(fakeExtWithBadMime);
  assert.equal(res2.valid, false, '.exe.jpg with executable MIME must be rejected');

  // 3. .subpart.photo.jpg with image/jpeg -> accepted
  const complexNameValid = {
    name: 'foundation.block_A.section_1.jpg',
    type: 'image/jpeg',
    size: 2000,
  };
  const res3 = validateDiaryImageFile(complexNameValid);
  assert.equal(res3.valid, true, 'Complex multi-dot valid filename must be accepted');
});

test('ADV-MIME-04: Missing or empty MIME type fallback with valid extension', () => {
  // When OS does not provide MIME type (common on certain Windows setups)
  const emptyMime = {
    name: 'captured_photo.jpg',
    type: '',
    size: 15000,
  };
  const res1 = validateDiaryImageFile(emptyMime);
  assert.equal(res1.valid, true, 'Valid image extension with empty MIME is accepted');

  const undefinedMime = {
    name: 'captured_photo.png',
    size: 15000,
  };
  const res2 = validateDiaryImageFile(undefinedMime);
  assert.equal(res2.valid, true, 'Valid image extension with undefined MIME is accepted');

  // But invalid extension with empty MIME must STILL be rejected!
  const badExtEmptyMime = {
    name: 'script.sh',
    type: '',
    size: 15000,
  };
  const res3 = validateDiaryImageFile(badExtEmptyMime);
  assert.equal(res3.valid, false, 'Invalid extension with empty MIME must be rejected');
});

test('ADV-MIME-05: Malformed file objects (null, undefined, missing name)', () => {
  assert.equal(validateDiaryImageFile(null).valid, false);
  assert.equal(validateDiaryImageFile(undefined).valid, false);
  assert.equal(validateDiaryImageFile({}).valid, false);
  assert.equal(validateDiaryImageFile({ size: 100 }).valid, false);
});

// =========================================================================
// CHALLENGE SUITE 4: Multi-File Rapid Burst & High Concurrency
// =========================================================================

test('ADV-BURST-01: Rapid concurrent burst of 50 valid items produces exactly 50 queue items with unique IDs', () => {
  let latestQueue = [];
  const harness = createAdversarialUploaderHarness({
    onChange: (items) => {
      latestQueue = items;
    },
  });

  const count = 50;
  const files = Array.from({ length: count }, (_, i) => ({
    name: `burst_img_${i + 1}.jpg`,
    size: 1024 * (100 + i),
    type: 'image/jpeg',
  }));

  // Simulate concurrent beforeUpload
  for (const f of files) {
    const res = harness.handleBeforeUpload(f);
    assert.equal(res, true);
  }

  // Simulate concurrent customRequest triggered in rapid loop
  for (const f of files) {
    harness.handleCustomUpload({
      file: f,
      onSuccess: () => {},
    });
  }

  assert.equal(latestQueue.length, count, `Queue must have all ${count} items without dropped items`);
  assert.equal(harness.getItems().length, count);

  // Assert all IDs are strictly unique
  const ids = new Set(latestQueue.map((it) => it.clientImageId));
  assert.equal(ids.size, count, `All ${count} clientImageIds must be completely unique`);

  // Assert all items have valid previewUrls
  for (const it of latestQueue) {
    assert.ok(it.previewUrl && it.previewUrl.startsWith('blob:'), 'Every item must have a preview URL');
  }
});

test('ADV-BURST-02: Rapid burst with maxCount=5 strictly enforces ceiling on 50 concurrent items', () => {
  let latestQueue = [];
  const harness = createAdversarialUploaderHarness({
    maxCount: 5,
    onChange: (items) => {
      latestQueue = items;
    },
  });

  const files = Array.from({ length: 50 }, (_, i) => ({
    name: `flood_${i + 1}.png`,
    size: 1000,
    type: 'image/png',
  }));

  let acceptedCount = 0;
  let rejectedCount = 0;

  for (const f of files) {
    const before = harness.handleBeforeUpload(f);
    if (before === true) {
      acceptedCount++;
      harness.handleCustomUpload({ file: f, onSuccess: () => {} });
    } else {
      rejectedCount++;
    }
  }

  assert.equal(latestQueue.length, 5, 'Queue length must never exceed maxCount');
  assert.equal(acceptedCount, 5, 'Exactly 5 items must be accepted');
  assert.equal(rejectedCount, 45, 'Remaining 45 items must be rejected');
  assert.equal(harness.getWarningMessages().length, 45, 'Warning message for each rejected item beyond maxCount');
});

test('ADV-BURST-03: Mixed burst (valid, invalid format, oversized) accurately filters and reports', () => {
  const harness = createAdversarialUploaderHarness();

  const mixedBatch = [
    { name: 'valid_1.jpg', size: 1000, type: 'image/jpeg' },
    { name: 'invalid_doc.pdf', size: 1000, type: 'application/pdf' },
    { name: 'valid_2.png', size: 2000, type: 'image/png' },
    { name: 'oversized.jpg', size: 25 * 1024 * 1024, type: 'image/jpeg' },
    { name: 'empty.webp', size: 0, type: 'image/webp' },
    { name: 'valid_3.webp', size: 3000, type: 'image/webp' },
  ];

  // Test drop behavior
  harness.handleDrop({
    dataTransfer: { files: mixedBatch },
  });

  const errors = harness.getErrorMessages();
  assert.equal(errors.length, 3, 'Must have 3 error messages for the 3 invalid files');
  assert.ok(errors.some((e) => e.includes('invalid_doc.pdf')));
  assert.ok(errors.some((e) => e.includes('oversized.jpg')));
  assert.ok(errors.some((e) => e.includes('empty.webp')));

  // Test beforeUpload and customRequest on the valid ones
  const validFiles = mixedBatch.filter((f) => harness.handleBeforeUpload(f) === true);
  assert.equal(validFiles.length, 3);
  for (const vf of validFiles) {
    harness.handleCustomUpload({ file: vf, onSuccess: () => {} });
  }

  assert.equal(harness.getItems().length, 3);
});

// =========================================================================
// CHALLENGE SUITE 5: S-24 Locked Date Absolute Immunity
// =========================================================================

test('ADV-LOCK-01: When disabled=true, queue is immutable to additions, drops, and removals', () => {
  const initialItems = [
    {
      clientImageId: 'locked-item-1',
      name: 'pre_existing.jpg',
      size: 50000,
      type: 'image/jpeg',
      previewUrl: 'blob:http://localhost/locked-item-1',
      displayStatus: 'completed',
      originalStatus: 'completed',
    },
    {
      clientImageId: 'locked-item-2',
      name: 'pre_existing_2.png',
      size: 60000,
      type: 'image/png',
      previewUrl: 'blob:http://localhost/locked-item-2',
      displayStatus: 'completed',
      originalStatus: 'completed',
    },
  ];

  let changeCalls = 0;
  const harness = createAdversarialUploaderHarness({
    items: initialItems,
    disabled: true, // S-24 Date Locked
    onChange: () => {
      changeCalls++;
    },
  });

  const attackerFile = {
    name: 'injected_evidence.jpg',
    size: 1000,
    type: 'image/jpeg',
  };

  // 1. Adversarial beforeUpload attempt
  const beforeRes = harness.handleBeforeUpload(attackerFile);
  assert.equal(beforeRes, harness.LIST_IGNORE, 'beforeUpload must return LIST_IGNORE when disabled');

  // 2. Direct adversarial customRequest call
  harness.handleCustomUpload({ file: attackerFile, onSuccess: () => {} });
  assert.equal(harness.getItems().length, 2, 'No items can be added');

  // 3. Direct adversarial drop call
  harness.handleDrop({ dataTransfer: { files: [attackerFile] } });
  assert.equal(harness.getErrorMessages().length, 0, 'Drop is completely inert when disabled');

  // 4. Adversarial remove attempts
  harness.handleRemove('locked-item-1');
  harness.handleRemove('locked-item-2');
  assert.equal(harness.getItems().length, 2, 'No items can be removed when disabled');
  assert.equal(harness.getRevokedUrls().length, 0, 'No URLs revoked for preserved items');

  // 5. onChange must NEVER have been called
  assert.equal(changeCalls, 0, 'onChange was never called while disabled');
  assert.deepEqual(harness.getItems(), initialItems, 'Queue state remained 100% identical');
});

// =========================================================================
// CHALLENGE SUITE 6: Resource Lifecycle & Object URL Memory Management
// =========================================================================

test('ADV-MEM-01: Object URLs are promptly and safely revoked upon item removal', () => {
  const harness = createAdversarialUploaderHarness();

  const fileA = { name: 'mem_a.jpg', size: 1000, type: 'image/jpeg' };
  const fileB = { name: 'mem_b.png', size: 2000, type: 'image/png' };

  harness.handleCustomUpload({ file: fileA });
  harness.handleCustomUpload({ file: fileB });

  const [itemA, itemB] = harness.getItems();
  assert.ok(itemA.previewUrl);
  assert.ok(itemB.previewUrl);

  harness.handleRemove(itemA.clientImageId);
  assert.deepEqual(harness.getRevokedUrls(), [itemA.previewUrl]);
  assert.equal(harness.getItems().length, 1);
  assert.equal(harness.getItems()[0].clientImageId, itemB.clientImageId);

  harness.handleRemove(itemB.clientImageId);
  assert.deepEqual(harness.getRevokedUrls(), [itemA.previewUrl, itemB.previewUrl]);
  assert.equal(harness.getItems().length, 0);
});

test('ADV-MEM-02: Component unmount revokes all remaining active URLs in queue', () => {
  const harness = createAdversarialUploaderHarness();

  const count = 10;
  for (let i = 0; i < count; i++) {
    harness.handleCustomUpload({
      file: { name: `unmount_${i}.jpg`, size: 1000, type: 'image/jpeg' },
    });
  }

  const items = harness.getItems();
  assert.equal(items.length, count);
  const expectedUrls = items.map((it) => it.previewUrl);

  // Trigger unmount
  harness.unmount();
  assert.equal(harness.getRevokedUrls().length, count);
  assert.deepEqual(harness.getRevokedUrls(), expectedUrls);
});

// =========================================================================
// CHALLENGE SUITE 7: Static AST & Template Integrity
// =========================================================================

test('ADV-STATIC-01: Template guarantees Delete button disabled during upload transfer', () => {
  assert.ok(
    uploaderSource.includes("item.displayStatus === 'uploading'"),
    'Must check item.displayStatus === "uploading" to disable Delete button during transfer'
  );
  assert.ok(
    uploaderSource.includes("item.originalStatus === 'uploading'"),
    'Must check item.originalStatus === "uploading" to disable Delete button during transfer'
  );
  assert.ok(
    uploaderSource.includes('disabled={disabled || item.displayStatus === \'uploading\' || item.originalStatus === \'uploading\'}'),
    'Delete button must combine disabled and uploading states'
  );
});

test('ADV-STATIC-02: Two-stage status badges render and connect onRetry callbacks', () => {
  assert.ok(uploaderSource.includes("onRetryDisplay && item.displayStatus === 'failed'"), 'Must bind onRetryDisplay when failed');
  assert.ok(uploaderSource.includes("onRetryOriginal && item.originalStatus === 'failed'"), 'Must bind onRetryOriginal when failed');
  assert.ok(/renderStatusBadge\s*\(\s*['"]Hiển thị['"]/.test(uploaderSource), 'Must render Hiển thị stage badge');
  assert.ok(/renderStatusBadge\s*\(\s*['"]Ảnh gốc['"]/.test(uploaderSource), 'Must render Ảnh gốc stage badge');
});
