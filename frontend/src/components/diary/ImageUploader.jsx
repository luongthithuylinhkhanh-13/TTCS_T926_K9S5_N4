import React, { useEffect, useRef } from 'react';
import { Upload, Button, Card, Typography, Tooltip, Tag, message } from 'antd';
import {
  InboxOutlined,
  DeleteOutlined,
  FileImageOutlined,
  ReloadOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  SyncOutlined,
} from '@ant-design/icons';
import { formatFileSize, createQueueItem, revokeQueueItem, revokeAllQueueItems } from '../../utils/imageQueueUtils.js';
import { extractImageMetadata, validateImageMetadata } from '../../utils/imageMetadataExtractor.js';

const { Dragger } = Upload;
const { Text } = Typography;

export { validateImageMetadata };

export const ALLOWED_MIME_TYPES = ['image/jpeg', 'image/png', 'image/webp'];
export const ALLOWED_EXTENSIONS = /\.(jpe?g|png|webp)$/i;
export const MAX_FILE_SIZE_BYTES = 20 * 1024 * 1024; // 20 MiB

/**
 * Kiểm tra tính hợp lệ của tệp ảnh trước khi đưa vào hàng đợi
 */
export function validateDiaryImageFile(file) {
  if (!file) {
    return { valid: false, error: 'Vui lòng chọn tệp ảnh.' };
  }

  const fileName = file.name || '';
  const fileType = (file.type || '').toLowerCase();

  const hasValidExt = ALLOWED_EXTENSIONS.test(fileName);
  const hasValidMime = fileType ? ALLOWED_MIME_TYPES.includes(fileType) : false;

  // Kiểm tra định dạng: phần mở rộng và MIME type
  if (!hasValidExt || (fileType && !hasValidMime)) {
    return {
      valid: false,
      error: `Tệp "${fileName || 'Không rõ'}" không đúng định dạng. Chỉ chấp nhận ảnh JPEG, PNG, WebP.`,
    };
  }

  // Kiểm tra tệp rỗng
  if (typeof file.size === 'number' && file.size <= 0) {
    return {
      valid: false,
      error: `Tệp "${fileName}" không được để trống (dung lượng 0 byte).`,
    };
  }

  // Kiểm tra dung lượng tối đa (<= 20 MiB)
  if (typeof file.size === 'number' && file.size > MAX_FILE_SIZE_BYTES) {
    return {
      valid: false,
      error: `Tệp "${fileName}" (${formatFileSize(file.size)}) vượt quá dung lượng tối đa 20 MiB.`,
    };
  }

  return { valid: true };
}

/**
 * Component T-62: Chọn nhiều ảnh, nén hiển thị, theo dõi trạng thái hai giai đoạn (Display & Original),
 * retry độc lập từng giai đoạn, dọn dẹp object URL an toàn.
 */
export default function ImageUploader({
  items = [],
  onChange,
  onRetryDisplay,
  onRetryOriginal,
  disabled = false,
  maxCount = null,
  requireGps = false,
}) {
  const itemsRef = useRef(items);
  itemsRef.current = items;

  // Luôn đồng bộ itemsRef với prop items mới nhất
  useEffect(() => {
    itemsRef.current = items;
  }, [items]);

  useEffect(() => {
    return () => {
      // Thu hồi toàn bộ preview URL khi component unmount
      revokeAllQueueItems(itemsRef.current);
    };
  }, []);

  const handleBeforeUpload = (file) => {
    if (disabled) {
      return Upload.LIST_IGNORE;
    }

    if (maxCount && itemsRef.current.length >= maxCount) {
      message.warning(`Đã đạt giới hạn tối đa ${maxCount} ảnh. Không thể thêm "${file.name || 'ảnh mới'}".`);
      return Upload.LIST_IGNORE;
    }

    const validation = validateDiaryImageFile(file);
    if (!validation.valid) {
      message.error(validation.error);
      return Upload.LIST_IGNORE;
    }

    // Tệp hợp lệ: trả về true để Ant Design chuyển sang customRequest
    return true;
  };

  const handleCustomUpload = ({ file, onSuccess }) => {
    if (disabled) {
      return;
    }
    if (maxCount && itemsRef.current.length >= maxCount) {
      message.warning(`Đã đạt giới hạn tối đa ${maxCount} ảnh. Không thể thêm "${file.name || 'ảnh mới'}".`);
      return;
    }

    const newItem = createQueueItem(file, undefined, null, { requireGps });
    const updated = [...itemsRef.current, newItem];
    itemsRef.current = updated;

    if (onChange) {
      onChange(updated);
    }
    if (onSuccess) {
      onSuccess('ok');
    }

    // T-74: Trích xuất siêu dữ liệu EXIF / GPS / kích thước ảnh bất đồng bộ
    extractImageMetadata(file).then((extractedMeta) => {
      const validation = validateImageMetadata(extractedMeta, { requireGps });
      const currentList = itemsRef.current;
      const targetIdx = currentList.findIndex((it) => it.clientImageId === newItem.clientImageId);
      if (targetIdx >= 0) {
        const nextList = [...currentList];
        nextList[targetIdx] = {
          ...nextList[targetIdx],
          metadata: extractedMeta,
          metadataValidation: validation,
        };
        itemsRef.current = nextList;
        if (onChange) {
          onChange(nextList);
        }
      }
    }).catch(() => {
      // Bỏ qua lỗi trích xuất
    });
  };

  const handleDrop = (e) => {
    if (disabled) {
      return;
    }
    const droppedFiles = Array.from(e.dataTransfer?.files || []);
    if (droppedFiles.length === 0) {
      return;
    }

    // Duyệt qua các tệp được thả vào để phát hiện và cảnh báo các tệp không hợp lệ
    // (như tệp không phải ảnh bị Ant Design attrAccept tự động bỏ qua ngầm)
    droppedFiles.forEach((file) => {
      const validation = validateDiaryImageFile(file);
      if (!validation.valid) {
        message.error(validation.error);
      }
    });
  };

  const handleRemove = (clientImageId) => {
    if (disabled) {
      return;
    }
    const target = itemsRef.current.find((it) => it.clientImageId === clientImageId);
    if (target) {
      revokeQueueItem(target);
    }
    const updated = itemsRef.current.filter((it) => it.clientImageId !== clientImageId);
    itemsRef.current = updated;
    if (onChange) {
      onChange(updated);
    }
  };

  const renderStatusBadge = (stageName, status, error, onRetry) => {
    switch (status) {
      case 'completed':
        return (
          <Tag icon={<CheckCircleOutlined />} color="success" style={{ margin: 0, fontSize: '10px' }}>
            {stageName}: Xong
          </Tag>
        );
      case 'uploading':
        return (
          <Tag icon={<SyncOutlined spin />} color="processing" style={{ margin: 0, fontSize: '10px' }}>
            {stageName}: Đang tải...
          </Tag>
        );
      case 'failed':
        return (
          <Tooltip title={error || 'Tải lên thất bại'}>
            <Tag
              icon={<CloseCircleOutlined />}
              color="error"
              style={{ margin: 0, fontSize: '10px', cursor: onRetry ? 'pointer' : 'default' }}
              onClick={onRetry}
            >
              {stageName}: Lỗi {onRetry && <ReloadOutlined style={{ marginLeft: 2 }} />}
            </Tag>
          </Tooltip>
        );
      case 'idle':
      default:
        return (
          <Tag color="default" style={{ margin: 0, fontSize: '10px' }}>
            {stageName}: Chờ tải
          </Tag>
        );
    }
  };

  return (
    <div className="diary-image-uploader" style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
      <Dragger
        multiple
        accept="image/jpeg,image/png,image/webp"
        disabled={disabled || (Boolean(maxCount) && items.length >= maxCount)}
        beforeUpload={handleBeforeUpload}
        customRequest={handleCustomUpload}
        onDrop={handleDrop}
        showUploadList={false}
        style={{ padding: '16px', background: '#fafafa', borderRadius: '8px' }}
      >
        <p className="ant-upload-drag-icon" style={{ marginBottom: '8px' }}>
          <InboxOutlined style={{ fontSize: '36px', color: '#1677ff' }} />
        </p>
        <p className="ant-upload-text" style={{ fontSize: '14px', fontWeight: 500, margin: '0 0 4px 0' }}>
          Nhấp hoặc kéo thả ảnh vào khu vực này để chọn
        </p>
        <p className="ant-upload-hint" style={{ color: '#8c8c8c', fontSize: '12px', margin: 0 }}>
          {maxCount ? `Hỗ trợ JPEG, PNG, WebP (Tối đa ${maxCount} ảnh, dung lượng gốc tối đa 20 MiB/ảnh).` : 'Hỗ trợ định dạng JPEG, PNG, WebP (dung lượng gốc tối đa 20 MiB/ảnh).'}
        </p>
      </Dragger>

      {items.length > 0 && (
        <div
          className="image-queue-grid"
          style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fill, minmax(180px, 1fr))',
            gap: '12px',
          }}
        >
          {items.map((item) => (
            <Card
              key={item.clientImageId}
              hoverable
              size="small"
              styles={{ body: { padding: '10px' } }}
              style={{ position: 'relative', overflow: 'hidden' }}
              cover={
                <div
                  style={{
                    height: '110px',
                    backgroundColor: '#f0f0f0',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    overflow: 'hidden',
                  }}
                >
                  {item.previewUrl ? (
                    <img
                      src={item.previewUrl}
                      alt={item.name}
                      style={{
                        width: '100%',
                        height: '100%',
                        objectFit: 'cover',
                      }}
                    />
                  ) : (
                    <FileImageOutlined style={{ fontSize: '28px', color: '#bfbfbf' }} />
                  )}
                </div>
              }
            >
              <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
                <Tooltip title={item.name}>
                  <Text ellipsis style={{ fontSize: '12px', fontWeight: 500 }}>
                    {item.name}
                  </Text>
                </Tooltip>

                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <Text style={{ fontSize: '11px', color: '#8c8c8c' }}>
                    {formatFileSize(item.size)}
                  </Text>
                  <Button
                    type="text"
                    danger
                    size="small"
                    icon={<DeleteOutlined />}
                    disabled={disabled || item.displayStatus === 'uploading' || item.originalStatus === 'uploading'}
                    onClick={() => handleRemove(item.clientImageId)}
                    title="Xóa ảnh khỏi hàng đợi"
                  />
                </div>

                {/* T-74: Thông tin và trạng thái kiểm tra siêu dữ liệu ảnh hiện trường */}
                {item.metadata && (
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '3px', background: '#fafafa', border: '1px solid #f0f0f0', borderRadius: '4px', padding: '4px 6px' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                      <Text style={{ fontSize: '10px', color: '#595959' }}>
                        Chụp: {item.metadata.capturedAt ? item.metadata.capturedAt.slice(0, 16).replace('T', ' ') : 'Chưa có'}
                      </Text>
                      {item.metadataValidation?.valid ? (
                        <Tag color="cyan" style={{ margin: 0, fontSize: '9px', padding: '0 4px', lineHeight: '16px' }}>
                          Meta: Hợp lệ
                        </Tag>
                      ) : (
                        <Tooltip title={item.metadataValidation?.errors?.join(', ') || 'Lỗi siêu dữ liệu'}>
                          <Tag color="error" style={{ margin: 0, fontSize: '9px', padding: '0 4px', lineHeight: '16px', cursor: 'pointer' }}>
                            Meta: Lỗi
                          </Tag>
                        </Tooltip>
                      )}
                    </div>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                      {typeof item.metadata.latitude === 'number' && typeof item.metadata.longitude === 'number' ? (
                        <Text style={{ fontSize: '10px', color: '#1677ff' }}>
                          GPS: {item.metadata.latitude.toFixed(4)}, {item.metadata.longitude.toFixed(4)}
                        </Text>
                      ) : (
                        <Text style={{ fontSize: '10px', color: '#8c8c8c' }}>
                          GPS: Chưa có
                        </Text>
                      )}
                      {item.metadata.width && item.metadata.height ? (
                        <Text style={{ fontSize: '9px', color: '#8c8c8c' }}>
                          {item.metadata.width}x{item.metadata.height}
                        </Text>
                      ) : null}
                    </div>
                  </div>
                )}

                {/* Trạng thái hai giai đoạn: Hiển thị & Ảnh gốc */}
                <div style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    {renderStatusBadge(
                      'Hiển thị',
                      item.displayStatus,
                      item.displayError,
                      onRetryDisplay && item.displayStatus === 'failed'
                        ? () => onRetryDisplay(item)
                        : null
                    )}
                  </div>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    {renderStatusBadge(
                      'Ảnh gốc',
                      item.originalStatus,
                      item.originalError,
                      onRetryOriginal && item.originalStatus === 'failed'
                        ? () => onRetryOriginal(item)
                        : null
                    )}
                  </div>
                </div>
              </div>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}
