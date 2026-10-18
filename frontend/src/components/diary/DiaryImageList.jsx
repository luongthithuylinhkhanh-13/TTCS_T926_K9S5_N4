import React, { useState, useEffect } from 'react';
import { Card, Spin, Empty, Alert, Typography, Tooltip, Tag, Popconfirm, Button, message } from 'antd';
import { FileImageOutlined, EyeOutlined, DeleteOutlined } from '@ant-design/icons';
import { getDiaryImages, getDisplayImageUrl, deleteDiaryImage } from '../../services/diaryApi.js';
import { determineImageMetadataStatus, formatCapturedAt } from '../../utils/imageMetadataExtractor.js';
import apiClient from '../../services/apiClient.js';
import DiaryImageLightbox from './DiaryImageLightbox.jsx';

const { Text } = Typography;

/**
 * Component T-63: Hiển thị danh sách thumbnail các ảnh thuộc nhật ký thi công,
 * xử lý các trạng thái rỗng, đang tải, lỗi mạng, xóa ảnh và tích hợp Lightbox xem chi tiết.
 */
export default function DiaryImageList({
  projectId,
  entryId,
  refreshTrigger = 0,
  readOnly = false,
  disabled = false,
  onDelete,
}) {
  const [images, setImages] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [deletingId, setDeletingId] = useState(null);

  // Trạng thái Lightbox
  const [lightboxOpen, setLightboxOpen] = useState(false);
  const [selectedImageIndex, setSelectedImageIndex] = useState(0);

  const fetchImages = async () => {
    if (!projectId || !entryId) return;
    setLoading(true);
    setError(null);
    try {
      const data = await getDiaryImages(projectId, entryId);
      setImages(Array.isArray(data) ? data : []);
    } catch (err) {
      setError(err.message || 'Không thể tải danh sách ảnh');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchImages();
  }, [projectId, entryId, refreshTrigger]);

  const handleOpenLightbox = (index) => {
    setSelectedImageIndex(index);
    setLightboxOpen(true);
  };

  const handleDeleteImage = async (img) => {
    if (disabled || readOnly) return;
    setDeletingId(img.id);
    try {
      await deleteDiaryImage(projectId, entryId, img.id);
      message.success('Đã xóa ảnh thành công');
      if (onDelete) {
        onDelete(img.id);
      }
      await fetchImages();
    } catch (err) {
      message.error(err.message || 'Không thể xóa ảnh');
    } finally {
      setDeletingId(null);
    }
  };

  if (loading) {
    return (
      <div style={{ textAlign: 'center', padding: '32px' }}>
        <Spin tip="Đang tải danh sách ảnh nhật ký..." />
      </div>
    );
  }

  if (error) {
    return (
      <Alert
        type="error"
        message="Lỗi tải ảnh"
        description={error}
        showIcon
        style={{ margin: '16px 0' }}
      />
    );
  }

  if (!images || images.length === 0) {
    return (
      <Empty
        image={Empty.PRESENTED_IMAGE_SIMPLE}
        description="Chưa có ảnh nào được đính kèm vào nhật ký này"
        style={{ padding: '24px 0' }}
      />
    );
  }

  return (
    <div className="diary-image-list-container" style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
      <div
        className="diary-thumbnail-grid"
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fill, minmax(130px, 1fr))',
          gap: '12px',
        }}
      >
        {images.map((img, idx) => {
          const isCompleted = img.displayStatus === 'COMPLETED';
          const thumbUrl = isCompleted
            ? getDisplayImageUrl(projectId, entryId, img.id, true)
            : null;

          return (
            <Card
              key={img.id || idx}
              hoverable
              size="small"
              styles={{ body: { padding: '8px' } }}
              style={{
                position: 'relative',
                cursor: isCompleted ? 'pointer' : 'default',
                overflow: 'hidden',
              }}
              onClick={() => isCompleted && handleOpenLightbox(idx)}
              cover={
                <div
                  style={{
                    height: '110px',
                    backgroundColor: '#f5f5f5',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    position: 'relative',
                    overflow: 'hidden',
                  }}
                >
                  {!readOnly && (
                    <Tooltip title={disabled ? 'Ngày nhật ký đã bị khóa, không thể xóa ảnh' : 'Xóa ảnh'}>
                      <div
                        style={{
                          position: 'absolute',
                          top: 4,
                          right: 4,
                          zIndex: 3,
                        }}
                        onClick={(e) => e.stopPropagation()}
                      >
                        <Popconfirm
                          title="Xác nhận xóa ảnh"
                          description="Bạn có chắc chắn muốn xóa ảnh này khỏi nhật ký không?"
                          okText="Xóa"
                          cancelText="Hủy"
                          okButtonProps={{ danger: true }}
                          disabled={disabled || deletingId === img.id}
                          onConfirm={() => handleDeleteImage(img)}
                        >
                          <Button
                            type="text"
                            danger
                            size="small"
                            disabled={disabled || deletingId === img.id}
                            loading={deletingId === img.id}
                            icon={<DeleteOutlined />}
                            style={{
                              backgroundColor: 'rgba(255, 255, 255, 0.85)',
                              borderRadius: '50%',
                              width: 26,
                              height: 26,
                              display: 'flex',
                              alignItems: 'center',
                              justifyContent: 'center',
                              boxShadow: '0 1px 4px rgba(0,0,0,0.15)',
                            }}
                          />
                        </Popconfirm>
                      </div>
                    </Tooltip>
                  )}
                  {isCompleted ? (
                    <>
                      <img
                        src={thumbUrl}
                        alt={img.originalFilename}
                        style={{
                          width: '100%',
                          height: '100%',
                          objectFit: 'cover',
                          transition: 'transform 0.2s',
                        }}
                        onError={(e) => {
                          if (!e.target.dataset.fallbackTried) {
                            e.target.dataset.fallbackTried = 'true';
                            apiClient.get(`/api/projects/${projectId}/diary-entries/${entryId}/images/${img.id}/display?thumb=true`, {
                              responseType: 'blob',
                            })
                            .then((res) => {
                              e.target.src = URL.createObjectURL(res.data);
                            })
                            .catch(() => {
                              apiClient.get(`/api/projects/${projectId}/diary-entries/${entryId}/images/${img.id}/display`, {
                                responseType: 'blob',
                              })
                              .then((res) => {
                                e.target.src = URL.createObjectURL(res.data);
                              })
                              .catch((err) => {
                                console.error('Lỗi tải ảnh fallback:', err);
                              });
                            });
                          }
                        }}
                      />
                      <div
                        className="thumb-hover-overlay"
                        style={{
                          position: 'absolute',
                          inset: 0,
                          backgroundColor: 'rgba(0,0,0,0.3)',
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'center',
                          opacity: 0,
                          transition: 'opacity 0.2s',
                        }}
                        onMouseEnter={(e) => (e.currentTarget.style.opacity = '1')}
                        onMouseLeave={(e) => (e.currentTarget.style.opacity = '0')}
                      >
                        <EyeOutlined style={{ color: '#fff', fontSize: '20px' }} />
                      </div>
                    </>
                  ) : (
                    <div style={{ textAlign: 'center', color: '#8c8c8c' }}>
                      <FileImageOutlined style={{ fontSize: '28px', color: '#bfbfbf' }} />
                      <div style={{ fontSize: '11px', marginTop: 4 }}>
                        <Tag color="warning" style={{ margin: 0, fontSize: '10px' }}>
                          Đang xử lý
                        </Tag>
                      </div>
                    </div>
                  )}
                </div>
              }
            >
              <Tooltip title={img.originalFilename}>
                <Text ellipsis style={{ fontSize: '12px', display: 'block', fontWeight: 500 }}>
                  {img.originalFilename || 'Ảnh'}
                </Text>
              </Tooltip>
              {(() => {
                const metaStatus = determineImageMetadataStatus(img);
                const hasReasons = metaStatus.reasons && metaStatus.reasons.length > 0;
                const isMissing = metaStatus.status === 'MISSING';

                const badgeElement = (() => {
                  if (metaStatus.status === 'VALID') {
                    return (
                      <Tag color="success" style={{ margin: 0, fontSize: '9px', padding: '0 4px', lineHeight: '16px' }}>
                        ✓ Meta: Hợp lệ
                      </Tag>
                    );
                  }
                  if (isMissing) {
                    return (
                      <Tag color="warning" style={{ margin: 0, fontSize: '9px', padding: '0 4px', lineHeight: '16px' }}>
                        ⚠ Meta: Thiếu
                      </Tag>
                    );
                  }
                  return (
                    <Tag color="error" style={{ margin: 0, fontSize: '9px', padding: '0 4px', lineHeight: '16px' }}>
                      ⚠ Meta: Không hợp lệ
                    </Tag>
                  );
                })();

                return (
                  <>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: 4 }}>
                      <Tooltip title={img.capturedAt ? `Thời điểm chụp: ${formatCapturedAt(img.capturedAt)}` : 'Chưa có thời gian chụp'}>
                        <Text style={{ fontSize: '10px', color: img.capturedAt ? '#262626' : '#8c8c8c' }} ellipsis>
                          {img.capturedAt ? `📅 ${formatCapturedAt(img.capturedAt)}` : 'Chưa có thời gian chụp'}
                        </Text>
                      </Tooltip>
                      {hasReasons ? (
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
                          {badgeElement}
                        </Tooltip>
                      ) : (
                        badgeElement
                      )}
                    </div>

                    {/* T-76: Hiển thị ngắn gọn lý do cảnh báo dưới badge */}
                    {metaStatus.status !== 'VALID' && hasReasons && (
                      <div
                        className="saved-image-metadata-warning"
                        style={{
                          marginTop: 4,
                          padding: '2px 4px',
                          borderRadius: 3,
                          background: isMissing ? '#fffbe6' : '#fff2f0',
                          border: `1px solid ${isMissing ? '#ffe58f' : '#ffccc7'}`,
                          fontSize: '9px',
                          color: isMissing ? '#ad4e00' : '#cf1322',
                          lineHeight: '1.25',
                          overflow: 'hidden',
                          textOverflow: 'ellipsis',
                          whiteSpace: 'nowrap',
                        }}
                        title={metaStatus.reasons.join('; ')}
                      >
                        ⚠ {metaStatus.reasons.length === 1 ? metaStatus.reasons[0] : `${metaStatus.reasons[0]} (+${metaStatus.reasons.length - 1})`}
                      </div>
                    )}
                  </>
                );
              })()}
            </Card>
          );
        })}
      </div>

      <DiaryImageLightbox
        visible={lightboxOpen}
        images={images}
        currentIndex={selectedImageIndex}
        projectId={projectId}
        entryId={entryId}
        onClose={() => setLightboxOpen(false)}
        onIndexChange={(newIdx) => setSelectedImageIndex(newIdx)}
      />
    </div>
  );
}
