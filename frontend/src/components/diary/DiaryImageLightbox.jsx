import React, { useEffect, useState, useCallback } from 'react';
import { Modal, Button, Spin, Typography, Space, Tooltip, message } from 'antd';
import {
  LeftOutlined,
  RightOutlined,
  DownloadOutlined,
  CloseOutlined,
  FileImageOutlined,
} from '@ant-design/icons';
import { getDisplayImageUrl, getOriginalImageUrl } from '../../services/diaryApi.js';
import { formatFileSize } from '../../utils/imageQueueUtils.js';

const { Text } = Typography;

/**
 * Component T-63 Lightbox:
 * - Mở ảnh hiển thị kích thước lớn
 * - Nút đóng và phím Escape
 * - Chuyển qua lại (Next/Prev) bằng nút bấm hoặc phím mũi tên trái/phải
 * - Tải / xem ảnh gốc HD giữ nguyên bytes
 * - Không tạo object URL rò rỉ bộ nhớ
 */
export default function DiaryImageLightbox({
  visible = false,
  images = [],
  currentIndex = 0,
  projectId,
  entryId,
  onClose,
  onIndexChange,
}) {
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState(false);

  const currentImage = images[currentIndex] || null;

  useEffect(() => {
    setLoading(true);
    setLoadError(false);
  }, [currentIndex, currentImage?.id]);

  const handlePrev = useCallback(() => {
    if (images.length <= 1) return;
    const newIdx = (currentIndex - 1 + images.length) % images.length;
    if (onIndexChange) onIndexChange(newIdx);
  }, [currentIndex, images.length, onIndexChange]);

  const handleNext = useCallback(() => {
    if (images.length <= 1) return;
    const newIdx = (currentIndex + 1) % images.length;
    if (onIndexChange) onIndexChange(newIdx);
  }, [currentIndex, images.length, onIndexChange]);

  // Lắng nghe phím bấm: Escape (đóng), ArrowLeft (ảnh trước), ArrowRight (ảnh sau)
  useEffect(() => {
    if (!visible) return;

    const handleKeyDown = (e) => {
      if (e.key === 'Escape') {
        if (onClose) onClose();
      } else if (e.key === 'ArrowLeft') {
        handlePrev();
      } else if (e.key === 'ArrowRight') {
        handleNext();
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => {
      window.removeEventListener('keydown', handleKeyDown);
    };
  }, [visible, onClose, handlePrev, handleNext]);

  if (!visible || !currentImage) {
    return null;
  }

  const isDisplayReady = currentImage.displayStatus === 'COMPLETED';
  const isOriginalReady = currentImage.originalStatus === 'COMPLETED';

  const displayUrl = isDisplayReady
    ? getDisplayImageUrl(projectId, entryId, currentImage.id, false)
    : null;

  const originalUrl = isOriginalReady
    ? getOriginalImageUrl(projectId, entryId, currentImage.id)
    : null;

  return (
    <Modal
      open={visible}
      footer={null}
      closable={false}
      centered
      width="85vw"
      styles={{
        content: {
          padding: 0,
          background: 'rgba(20, 20, 20, 0.95)',
          borderRadius: '8px',
          overflow: 'hidden',
        },
      }}
      onCancel={onClose}
    >
      <div
        className="diary-lightbox-container"
        style={{
          position: 'relative',
          display: 'flex',
          flexDirection: 'column',
          minHeight: '75vh',
          maxHeight: '90vh',
          color: '#fff',
        }}
      >
        {/* Thanh công cụ đỉnh */}
        <div
          style={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            padding: '12px 20px',
            background: 'rgba(0, 0, 0, 0.6)',
            zIndex: 10,
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px', overflow: 'hidden' }}>
            <Text ellipsis style={{ color: '#fff', fontSize: '15px', fontWeight: 500, maxWidth: '400px' }}>
              {currentImage.originalFilename || 'Ảnh nhật ký'}
            </Text>
            <Text style={{ color: '#aaa', fontSize: '12px' }}>
              ({currentIndex + 1} / {images.length})
            </Text>
          </div>

          <Space>
            {isOriginalReady ? (
              <Button
                type="primary"
                icon={<DownloadOutlined />}
                size="small"
                href={originalUrl}
                target="_blank"
                download={currentImage.originalFilename}
                title="Tải ảnh gốc HD nguyên bản"
              >
                Tải ảnh gốc HD {currentImage.originalFileSize ? `(${formatFileSize(currentImage.originalFileSize)})` : ''}
              </Button>
            ) : (
              <Tooltip title="Ảnh gốc đang được xử lý hoặc chưa hoàn tất tải lên">
                <Button size="small" disabled icon={<DownloadOutlined />}>
                  Ảnh gốc chưa sẵn sàng
                </Button>
              </Tooltip>
            )}

            <Button
              type="text"
              icon={<CloseOutlined style={{ color: '#fff', fontSize: '16px' }} />}
              onClick={onClose}
              title="Đóng (Escape)"
            />
          </Space>
        </div>

        {/* Khu vực hiển thị ảnh chính */}
        <div
          style={{
            flex: 1,
            display: 'flex',
            justifyContent: 'center',
            alignItems: 'center',
            position: 'relative',
            padding: '16px',
            overflow: 'hidden',
          }}
        >
          {loading && !loadError && isDisplayReady && (
            <div style={{ position: 'absolute', zIndex: 5 }}>
              <Spin size="large" />
            </div>
          )}

          {loadError || !isDisplayReady ? (
            <div style={{ textAlign: 'center', color: '#ff7875' }}>
              <FileImageOutlined style={{ fontSize: '48px', marginBottom: '12px' }} />
              <div>
                {!isDisplayReady
                  ? 'Ảnh hiển thị chưa hoàn tất quá trình tải lên.'
                  : 'Không thể tải ảnh hiển thị. Vui lòng thử lại sau.'}
              </div>
            </div>
          ) : (
            <img
              src={displayUrl}
              alt={currentImage.originalFilename}
              onLoad={() => setLoading(false)}
              onError={() => {
                setLoading(false);
                setLoadError(true);
              }}
              style={{
                maxWidth: '100%',
                maxHeight: '75vh',
                objectFit: 'contain',
                display: loading ? 'none' : 'block',
                borderRadius: '4px',
              }}
            />
          )}

          {/* Nút Previous */}
          {images.length > 1 && (
            <Button
              type="text"
              icon={<LeftOutlined style={{ fontSize: '24px', color: '#fff' }} />}
              onClick={handlePrev}
              style={{
                position: 'absolute',
                left: '12px',
                top: '50%',
                transform: 'translateY(-50%)',
                background: 'rgba(0, 0, 0, 0.4)',
                borderRadius: '50%',
                width: '44px',
                height: '44px',
              }}
              title="Ảnh trước (Phím mũi tên trái)"
            />
          )}

          {/* Nút Next */}
          {images.length > 1 && (
            <Button
              type="text"
              icon={<RightOutlined style={{ fontSize: '24px', color: '#fff' }} />}
              onClick={handleNext}
              style={{
                position: 'absolute',
                right: '12px',
                top: '50%',
                transform: 'translateY(-50%)',
                background: 'rgba(0, 0, 0, 0.4)',
                borderRadius: '50%',
                width: '44px',
                height: '44px',
              }}
              title="Ảnh sau (Phím mũi tên phải)"
            />
          )}
        </div>
      </div>
    </Modal>
  );
}
