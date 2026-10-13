import React, { useState, useEffect } from 'react';
import { Card, Spin, Empty, Alert, Typography, Tooltip, Tag } from 'antd';
import { FileImageOutlined, EyeOutlined } from '@ant-design/icons';
import { getDiaryImages, getDisplayImageUrl } from '../../services/diaryApi.js';
import DiaryImageLightbox from './DiaryImageLightbox.jsx';

const { Text } = Typography;

/**
 * Component T-63: Hiển thị danh sách thumbnail các ảnh thuộc nhật ký thi công,
 * xử lý các trạng thái rỗng, đang tải, lỗi mạng và tích hợp Lightbox xem chi tiết.
 */
export default function DiaryImageList({
  projectId,
  entryId,
  refreshTrigger = 0,
}) {
  const [images, setImages] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

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
