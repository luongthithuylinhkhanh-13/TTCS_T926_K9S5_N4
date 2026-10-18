import React, { useEffect, useState, useCallback } from 'react';
import { Modal, Button, Spin, Typography, Space, Tooltip, Tag, message } from 'antd';
import {
  LeftOutlined,
  RightOutlined,
  DownloadOutlined,
  CloseOutlined,
  FileImageOutlined,
} from '@ant-design/icons';
import { getDisplayImageUrl, getOriginalImageUrl } from '../../services/diaryApi.js';
import { determineImageMetadataStatus, formatCapturedAt } from '../../utils/imageMetadataExtractor.js';
import apiClient from '../../services/apiClient.js';
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
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px', overflow: 'hidden', flexWrap: 'wrap' }}>
            <Text ellipsis style={{ color: '#fff', fontSize: '15px', fontWeight: 500, maxWidth: '350px' }}>
              {currentImage.originalFilename || 'Ảnh nhật ký'}
            </Text>
            <Text style={{ color: '#aaa', fontSize: '12px' }}>
              ({currentIndex + 1} / {images.length})
            </Text>
            <Tooltip title={currentImage.capturedAt ? `Thời điểm chụp: ${formatCapturedAt(currentImage.capturedAt)}` : 'Chưa có thời gian chụp'}>
              <Text style={{ color: currentImage.capturedAt ? '#4ade80' : '#d1d5db', fontSize: '12px' }}>
                📅 {currentImage.capturedAt ? `Chụp: ${formatCapturedAt(currentImage.capturedAt)}` : 'Chưa có thời gian chụp'}
              </Text>
            </Tooltip>
            {currentImage.createdAt && (
              <Tooltip title={`Thời điểm tải lên: ${formatCapturedAt(currentImage.createdAt)}`}>
                <Text style={{ color: '#9ca3af', fontSize: '11px' }}>
                  (Tải lên: {formatCapturedAt(currentImage.createdAt)})
                </Text>
              </Tooltip>
            )}
            {typeof currentImage.latitude === 'number' && typeof currentImage.longitude === 'number' && (
              <Text style={{ color: '#60a5fa', fontSize: '12px' }}>
                📍 {currentImage.latitude.toFixed(4)}, {currentImage.longitude.toFixed(4)}
              </Text>
            )}
            {(() => {
              const metaStatus = determineImageMetadataStatus(currentImage);
              const badge = (() => {
                if (metaStatus.status === 'VALID') {
                  return (
                    <Tag color="success" style={{ margin: 0, fontSize: '11px', padding: '1px 6px' }}>
                      ✓ Meta: Hợp lệ
                    </Tag>
                  );
                }
                if (metaStatus.status === 'MISSING') {
                  return (
                    <Tag color="warning" style={{ margin: 0, fontSize: '11px', padding: '1px 6px' }}>
                      ⚠ Meta: Thiếu
                    </Tag>
                  );
                }
                return (
                  <Tag color="error" style={{ margin: 0, fontSize: '11px', padding: '1px 6px' }}>
                    ⚠ Meta: Không hợp lệ
                  </Tag>
                );
              })();

              if (metaStatus.reasons && metaStatus.reasons.length > 0) {
                return (
                  <Tooltip
                    title={
                      <div>
                        <div style={{ fontWeight: 600, marginBottom: 2 }}>{metaStatus.label}:</div>
                        <ul style={{ margin: 0, paddingLeft: 14 }}>
                          {metaStatus.reasons.map((r, i) => (
                            <li key={i}>{r}</li>
                          ))}
                        </ul>
                      </div>
                    }
                  >
                    {badge}
                  </Tooltip>
                );
              }
              return badge;
            })()}
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

        {/* T-76: Banner cảnh báo chi tiết lý do metadata không hợp lệ / thiếu */}
        {(() => {
          const metaStatus = determineImageMetadataStatus(currentImage);
          if (metaStatus.status === 'VALID' || !metaStatus.reasons || metaStatus.reasons.length === 0) {
            return null;
          }
          const isMissing = metaStatus.status === 'MISSING';
          return (
            <div
              className="lightbox-metadata-warning-banner"
              style={{
                background: isMissing ? 'rgba(113, 63, 18, 0.4)' : 'rgba(127, 29, 29, 0.4)',
                borderBottom: `1px solid ${isMissing ? 'rgba(234, 179, 8, 0.35)' : 'rgba(239, 68, 68, 0.35)'}`,
                padding: '8px 20px',
                display: 'flex',
                alignItems: 'center',
                gap: '12px',
                fontSize: '12px',
                flexWrap: 'wrap',
                zIndex: 9,
              }}
            >
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px', whiteSpace: 'nowrap' }}>
                <span style={{ color: '#d1d5db' }}>Trạng thái metadata:</span>
                {isMissing ? (
                  <Tag color="warning" style={{ margin: 0, fontSize: '11px', padding: '1px 8px' }}>
                    ⚠ Meta: Thiếu
                  </Tag>
                ) : (
                  <Tag color="error" style={{ margin: 0, fontSize: '11px', padding: '1px 8px' }}>
                    ⚠ Meta: Không hợp lệ
                  </Tag>
                )}
              </div>

              <div style={{ display: 'flex', alignItems: 'center', gap: '8px', flexWrap: 'wrap' }}>
                <span style={{ fontWeight: 600, color: isMissing ? '#fde047' : '#fca5a5' }}>
                  Lý do:
                </span>
                <div style={{ display: 'flex', gap: '12px', flexWrap: 'wrap' }}>
                  {metaStatus.reasons.map((r, i) => (
                    <span
                      key={i}
                      style={{
                        color: '#f9fafb',
                        display: 'inline-flex',
                        alignItems: 'center',
                        gap: '4px',
                        background: 'rgba(0,0,0,0.25)',
                        padding: '1px 8px',
                        borderRadius: '4px',
                      }}
                    >
                      <span style={{ color: isMissing ? '#facc15' : '#f87171', fontWeight: 'bold' }}>•</span>
                      {r}
                    </span>
                  ))}
                </div>
              </div>
            </div>
          );
        })()}

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
              onError={(e) => {
                if (!e.target.dataset.fallbackTried) {
                  e.target.dataset.fallbackTried = 'true';
                  apiClient.get(`/api/projects/${projectId}/diary-entries/${entryId}/images/${currentImage.id}/display`, {
                    responseType: 'blob',
                  })
                  .then((res) => {
                    e.target.src = URL.createObjectURL(res.data);
                    setLoading(false);
                    setLoadError(false);
                  })
                  .catch(() => {
                    setLoading(false);
                    setLoadError(true);
                  });
                } else {
                  setLoading(false);
                  setLoadError(true);
                }
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
