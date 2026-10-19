/**
 * Utility nén ảnh phía client (browser / canvas) thành JPEG có dung lượng < 1 MiB (1,048,576 bytes).
 */

export const MAX_DISPLAY_BYTES = 1024 * 1024; // 1 MiB

/**
 * Nén một file ảnh đầu vào thành JPEG Blob với dung lượng < 1 MiB.
 * Thuật toán thử giảm quality trước (0.85 -> 0.7 -> 0.55), nếu vẫn >= 1 MiB thì giảm kích thước (scaling down).
 *
 * @param {File|Blob} file
 * @param {object} [options]
 * @param {number} [options.maxBytes=1048576]
 * @returns {Promise<{ blob: Blob, width: number, height: number }>}
 */
export async function compressImageToDisplay(file, options = {}) {
  const maxBytes = options.maxBytes || MAX_DISPLAY_BYTES;

  // Trực quan tạo Image element từ File hoặc Blob
  const imageBitmapOrImg = await loadImage(file);
  let curWidth = imageBitmapOrImg.width;
  let curHeight = imageBitmapOrImg.height;

  // Thử nhiều bước nén và resize
  const qualities = [0.85, 0.72, 0.58, 0.45];
  let bestBlob = null;
  let bestWidth = curWidth;
  let bestHeight = curHeight;

  // Thử tối đa 5 vòng lặp điều chỉnh
  for (let iteration = 0; iteration < 5; iteration++) {
    for (const quality of qualities) {
      const blob = await renderToJpegBlob(imageBitmapOrImg, curWidth, curHeight, quality);
      if (blob.size < maxBytes) {
        return {
          blob,
          width: curWidth,
          height: curHeight,
        };
      }
      bestBlob = blob;
      bestWidth = curWidth;
      bestHeight = curHeight;
    }

    // Nếu sau các mức quality mà vẫn >= maxBytes, scale nhỏ kích thước 75%
    curWidth = Math.max(1, Math.round(curWidth * 0.75));
    curHeight = Math.max(1, Math.round(curHeight * 0.75));
  }

  // Trường hợp biên cùng cực, trả về kết quả nhỏ nhất đạt được
  return {
    blob: bestBlob,
    width: bestWidth,
    height: bestHeight,
  };
}

function loadImage(file) {
  return new Promise((resolve, reject) => {
    if (typeof createImageBitmap === 'function') {
      createImageBitmap(file).then(resolve).catch(reject);
      return;
    }

    // Fallback Image element
    const img = new Image();
    const url = URL.createObjectURL(file);
    img.onload = () => {
      URL.revokeObjectURL(url);
      resolve(img);
    };
    img.onerror = (err) => {
      URL.revokeObjectURL(url);
      reject(new Error('Không thể giải mã file ảnh'));
    };
    img.src = url;
  });
}

function renderToJpegBlob(source, width, height, quality) {
  return new Promise((resolve, reject) => {
    let canvas;
    if (typeof OffscreenCanvas !== 'undefined') {
      canvas = new OffscreenCanvas(width, height);
      const ctx = canvas.getContext('2d');
      ctx.drawImage(source, 0, 0, width, height);
      canvas.convertToBlob({ type: 'image/jpeg', quality }).then(resolve).catch(reject);
      return;
    }

    if (typeof document !== 'undefined') {
      canvas = document.createElement('canvas');
      canvas.width = width;
      canvas.height = height;
      const ctx = canvas.getContext('2d');
      ctx.drawImage(source, 0, 0, width, height);
      canvas.toBlob(
        (blob) => {
          if (blob) resolve(blob);
          else reject(new Error('Lỗi xuất canvas toBlob'));
        },
        'image/jpeg',
        quality
      );
      return;
    }

    reject(new Error('Môi trường không hỗ trợ Canvas rendering'));
  });
}
