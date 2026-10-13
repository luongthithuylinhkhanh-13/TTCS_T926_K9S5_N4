import React, { useEffect, useRef } from 'react';
import { Upload, Button, Card, Typography, Space, Tooltip, Tag, Progress } from 'antd';
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

const { Dragger } = Upload;
const { Text } = Typography;

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
  maxCount = 20,
}) {
  const itemsRef = useRef(items);
  itemsRef.current = items;

  useEffect(() => {
    return () => {
      // Thu hồi toàn bộ preview URL khi component unmount
      revokeAllQueueItems(itemsRef.current);
    };
  }, []);

  const handleBeforeUpload = () => {
    // Ngăn chặn form submit hoặc upload ngầm tự động
    return false;
  };

  const handleCustomUpload = ({ file }) => {
    if (items.length >= maxCount) {
      return;
    }
    const newItem = createQueueItem(file);
    const updated = [...items, newItem];
    if (onChange) {
      onChange(updated);
    }
  };

  const handleRemove = (clientImageId) => {
    const target = items.find((it) => it.clientImageId === clientImageId);
    if (target) {
      revokeQueueItem(target);
    }
    const updated = items.filter((it) => it.clientImageId !== clientImageId);
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
        disabled={disabled || items.length >= maxCount}
        beforeUpload={handleBeforeUpload}
        customRequest={handleCustomUpload}
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
          Hỗ trợ định dạng JPEG, PNG, WebP (Tối đa {maxCount} ảnh, dung lượng gốc tối đa 20 MiB/ảnh).
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
