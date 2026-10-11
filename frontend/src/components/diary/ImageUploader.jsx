import React, { useEffect, useRef } from 'react';
import { Upload, Button, Card, Typography, Space, Tooltip } from 'antd';
import { InboxOutlined, DeleteOutlined, FileImageOutlined } from '@ant-design/icons';
import { formatFileSize, createQueueItem, revokeQueueItem, revokeAllQueueItems } from '../../utils/imageQueueUtils.js';

const { Dragger } = Upload;
const { Text } = Typography;

/**
 * Component T-61: Chọn nhiều ảnh, hiển thị preview thumbnail, tên file, kích thước, và xóa khỏi hàng đợi.
 * Bảo đảm thu hồi Object URL khi xóa và khi component unmount.
 */
export default function ImageUploader({
  items = [],
  onChange,
  disabled = false,
  maxCount = 20,
}) {
  // Lưu tham chiếu danh sách items hiện thời để dọn dẹp khi unmount
  const itemsRef = useRef(items);
  itemsRef.current = items;

  useEffect(() => {
    return () => {
      // Khi component unmount, thu hồi tất cả preview URL đang hoạt động
      revokeAllQueueItems(itemsRef.current);
    };
  }, []);

  const handleBeforeUpload = (file, fileList) => {
    // Ngăn Ant Design tự động gửi HTTP POST
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
          Hỗ trợ định dạng JPEG, PNG, WebP (Tối đa {maxCount} ảnh).
        </p>
      </Dragger>

      {items.length > 0 && (
        <div
          className="image-queue-grid"
          style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fill, minmax(140px, 1fr))',
            gap: '12px',
          }}
        >
          {items.map((item) => (
            <Card
              key={item.clientImageId}
              hoverable
              size="small"
              styles={{ body: { padding: '8px' } }}
              style={{ position: 'relative', overflow: 'hidden' }}
              cover={
                <div
                  style={{
                    height: '100px',
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
              <div style={{ display: 'flex', flexDirection: 'column', gap: '2px' }}>
                <Tooltip title={item.name}>
                  <Text
                    ellipsis
                    style={{ fontSize: '12px', fontWeight: 500 }}
                  >
                    {item.name}
                  </Text>
                </Tooltip>
                <Space orientation="horizontal" style={{ justifyContent: 'space-between', alignItems: 'center' }}>
                  <Text orientation="secondary" orientationstyle={{ fontSize: '11px', color: '#8c8c8c' }}>
                    {formatFileSize(item.size)}
                  </Text>
                  <Button
                    type="text"
                    danger
                    size="small"
                    icon={<DeleteOutlined />}
                    disabled={disabled}
                    onClick={() => handleRemove(item.clientImageId)}
                    title="Xóa ảnh khỏi hàng đợi"
                  />
                </Space>
              </div>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}
