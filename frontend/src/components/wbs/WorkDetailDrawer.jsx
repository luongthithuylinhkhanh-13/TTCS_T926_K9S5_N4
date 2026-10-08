import TaskDependencySection from './TaskDependencySection';
import CategoryTaskSection from './CategoryTaskSection';
import React from 'react';
import { Drawer, Tag, Progress, Button, Space, Avatar, Image, List, Popconfirm } from 'antd';
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  UserOutlined,
  CalendarOutlined,
  FileTextOutlined,
  ClockCircleOutlined,
  PictureOutlined,
  ApartmentOutlined
} from '@ant-design/icons';
import dayjs from 'dayjs';

const WorkDetailDrawer = ({
  visible,
  onClose,
  node,
  projectId,
  parentName,
  onAddChild,
  onEditNode,
  onDeleteNode,
  allTasks = []
}) => {
  if (!node) return null;

  const renderStatusTag = (status) => {
    switch (status) {
      case 'completed':
        return <Tag color="success" style={{ borderRadius: 6, fontWeight: 600 }}>Hoàn thành</Tag>;
      case 'in_progress':
        return <Tag color="processing" style={{ borderRadius: 6, fontWeight: 600 }}>Đang thực hiện</Tag>;
      case 'paused':
        return <Tag color="warning" style={{ borderRadius: 6, fontWeight: 600 }}>Tạm dừng</Tag>;
      case 'not_started':
      default:
        return <Tag color="default" style={{ borderRadius: 6, fontWeight: 600 }}>Chưa bắt đầu</Tag>;
    }
  };

  const renderPriorityTag = (priority) => {
    const normalized = (priority || 'Medium').toLowerCase();
    const mapping = {
      low: { color: 'green', label: 'Low' },
      medium: { color: 'orange', label: 'Medium' },
      high: { color: 'red', label: 'High' },
      critical: { color: 'magenta', label: 'Critical' }
    };

    const config = mapping[normalized] || mapping.medium;
    return <Tag color={config.color} style={{ borderRadius: 6, fontWeight: 600 }}>{config.label}</Tag>;
  };

  const getDurationDays = () => {
    if (!node.startDate || !node.endDate) return '--';
    const start = dayjs(node.startDate);
    const end = dayjs(node.endDate);
    const diff = end.diff(start, 'day') + 1;
    return diff > 0 ? `${diff} ngày` : '--';
  };

  const progressVal = node.progressPercent ?? node.progress ?? 0;
  const progressColor = progressVal === 100 ? '#16A34A' : progressVal >= 50 ? '#2563EB' : '#F59E0B';

  const getInitials = (name) => {
    if (!name) return 'NV';
    const parts = name.trim().split(' ');
    return parts.length >= 2
      ? (parts[0][0] + parts[parts.length - 1][0]).toUpperCase()
      : name.slice(0, 2).toUpperCase();
  };

  return (
    <Drawer
      title={
        <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
          <ApartmentOutlined style={{ color: '#2563EB', fontSize: 18 }} />
          <span style={{ fontSize: 15, fontWeight: 700, color: '#172B4D' }}>Chi tiết công việc</span>
        </div>
      }
      placement="right"
      width={520}
      onClose={onClose}
      open={visible}
      className="work-drawer"
      destroyOnClose
      styles={{ body: { padding: '16px 20px', paddingBottom: 80 } }}
    >
      <div className="drawer-content">

        {/* === HEADER CARD === */}
        <div className="drawer-header-card">
          <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 10 }}>
            <span className="wbs-badge">{node.wbsCode}</span>
            {renderStatusTag(node.status)}
          </div>
          <h2 className="drawer-task-name">{node.name}</h2>
          {parentName && (
            <div className="drawer-parent-label">
              <ApartmentOutlined style={{ marginRight: 5 }} />
              {parentName}
            </div>
          )}
        </div>

        {/* === PROGRESS SECTION === */}
        <div className="drawer-section">
          <div className="drawer-section-title">
            <ClockCircleOutlined style={{ marginRight: 6, color: '#2563EB' }} />
            TIẾN ĐỘ THI CÔNG
          </div>
          <div className="drawer-progress-card">
            <div className="drawer-progress-labels">
              <span>Đã thực hiện: <strong style={{ color: progressColor, fontSize: 18 }}>{progressVal}%</strong></span>
              <span style={{ color: '#94A3B8', fontSize: 13 }}>Còn lại: {100 - progressVal}%</span>
            </div>
            <Progress
              percent={progressVal}
              strokeColor={progressColor}
              trailColor="#E2E8F0"
              strokeWidth={12}
              style={{ marginTop: 6 }}
            />
          </div>
        </div>

        {/* === KEY DETAILS SECTION === */}
        <div className="drawer-section">
          <div className="drawer-section-title">
            <FileTextOutlined style={{ marginRight: 6, color: '#2563EB' }} />
            KEY DETAILS
          </div>
          <div className="drawer-info-grid">
            <div className="drawer-info-item">
              <span className="drawer-info-label">Priority</span>
              <div style={{ marginTop: 4 }}>
                {renderPriorityTag(node.priority || 'Medium')}
              </div>
            </div>

            <div className="drawer-info-item">
              <span className="drawer-info-label">Người phụ trách</span>
              {node.assignee ? (
                <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginTop: 4 }}>
                  <Avatar
                    size={30}
                    style={{ backgroundColor: '#2563EB', fontSize: 12, fontWeight: 700 }}
                  >
                    {getInitials(node.assignee.name)}
                  </Avatar>
                  <span className="drawer-info-value">{node.assignee.name}</span>
                </div>
              ) : (
                <span className="drawer-info-empty">Chưa phân công</span>
              )}
            </div>

            <div className="drawer-info-item">
              <span className="drawer-info-label">Ngày bắt đầu kế hoạch</span>
              <span className="drawer-info-value">
                <CalendarOutlined style={{ marginRight: 5, color: '#64748B' }} />
                {node.startDate ? dayjs(node.startDate).format('DD/MM/YYYY') : '--'}
              </span>
            </div>

            <div className="drawer-info-item">
              <span className="drawer-info-label">Ngày kết thúc kế hoạch</span>
              <span className="drawer-info-value">
                <CalendarOutlined style={{ marginRight: 5, color: '#64748B' }} />
                {node.endDate ? dayjs(node.endDate).format('DD/MM/YYYY') : '--'}
              </span>
            </div>

            <div className="drawer-info-item">
              <span className="drawer-info-label">Ngày bắt đầu thực tế</span>
              <span className="drawer-info-value">
                <CalendarOutlined style={{ marginRight: 5, color: '#0f766e' }} />
                {node.actualStartDate ? dayjs(node.actualStartDate).format('DD/MM/YYYY') : '--'}
              </span>
            </div>

            <div className="drawer-info-item">
              <span className="drawer-info-label">Ngày kết thúc thực tế</span>
              <span className="drawer-info-value">
                <CalendarOutlined style={{ marginRight: 5, color: '#0f766e' }} />
                {node.actualEndDate ? dayjs(node.actualEndDate).format('DD/MM/YYYY') : '--'}
              </span>
            </div>

            <div className="drawer-info-item">
              <span className="drawer-info-label">Thời lượng</span>
              <span className="drawer-info-value" style={{ fontWeight: 700 }}>{getDurationDays()}</span>
            </div>
          </div>
        </div>

        {/* === DESCRIPTION SECTION === */}
        <div className="drawer-section">
          <div className="drawer-section-title">
            <FileTextOutlined style={{ marginRight: 6, color: '#2563EB' }} />
            DESCRIPTION
          </div>
          <div className="drawer-description">
            {node.description || <span className="drawer-info-empty">Chưa có mô tả</span>}
          </div>
        </div>

        {/* === SUBTASKS SECTION === */}
        <div className="drawer-section">
          <div className="drawer-section-title" style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <span>
              <ApartmentOutlined style={{ marginRight: 6, color: '#2563EB' }} />
              SUBTASKS
            </span>
            <span style={{ color: '#64748B', fontSize: 12, fontWeight: 600 }}>
              {Array.isArray(node.children) ? node.children.length : 0} items
            </span>
          </div>

          {Array.isArray(node.children) && node.children.length > 0 ? (
            <div style={{ border: '1px solid #E2E8F0', borderRadius: 10, overflow: 'hidden' }}>
              <div style={{ display: 'grid', gridTemplateColumns: '2fr 0.8fr 0.9fr 1fr', background: '#F8FAFC', borderBottom: '1px solid #E2E8F0', fontWeight: 700, color: '#475569', fontSize: 12 }}>
                <div style={{ padding: '10px 12px' }}>Work</div>
                <div style={{ padding: '10px 12px' }}>Pri...</div>
                <div style={{ padding: '10px 12px' }}>Ass...</div>
                <div style={{ padding: '10px 12px' }}>Status</div>
              </div>

              {node.children.map((child) => (
                <div key={child.id || child.wbsCode} style={{ display: 'grid', gridTemplateColumns: '2fr 0.8fr 0.9fr 1fr', borderBottom: '1px solid #F1F5F9', background: '#fff' }}>
                  <div style={{ padding: '10px 12px', display: 'flex', alignItems: 'center', gap: 8 }}>
                    <span style={{ color: '#2563EB', fontSize: 12 }}>◫</span>
                    <span style={{ fontWeight: 600, color: '#1F2937' }}>{child.wbsCode}</span>
                    <span style={{ color: '#334155' }}>{child.name}</span>
                  </div>
                  <div style={{ padding: '10px 12px', display: 'flex', alignItems: 'center' }}>
                    {renderPriorityTag(child.priority || node.priority || 'Medium')}
                  </div>
                  <div style={{ padding: '10px 12px', display: 'flex', alignItems: 'center' }}>
                    {child.assignee ? (
                      <Avatar size={24} style={{ backgroundColor: '#E2E8F0', color: '#334155', fontSize: 10 }}>
                        {getInitials(child.assignee.name)}
                      </Avatar>
                    ) : (
                      <span style={{ color: '#94A3B8' }}>--</span>
                    )}
                  </div>
                  <div style={{ padding: '10px 12px', display: 'flex', alignItems: 'center' }}>
                    {renderStatusTag(child.status || node.status || 'not_started')}
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <div className="drawer-empty-images">
              <span>Chưa có công việc con</span>
            </div>
          )}
        </div>

        {/* === IMAGES SECTION === */}
        <div className="drawer-section">
          <div className="drawer-section-title">
            <PictureOutlined style={{ marginRight: 6, color: '#2563EB' }} />
            HÌNH ẢNH HIỆN TRƯỜNG
          </div>
          {node.images && node.images.length > 0 ? (
            <Image.PreviewGroup>
              <div className="drawer-image-grid">
                {node.images.map((img, idx) => {
                  const src = typeof img === 'string' ? `/images/construction/${img}` : img.url;
                  const caption = typeof img === 'object' && img.caption ? img.caption : node.name;
                  return (
                    <div key={idx} className="drawer-image-item">
                      <Image
                        src={src}
                        width="100%"
                        height={110}
                        style={{ objectFit: 'cover', borderRadius: 8, display: 'block' }}
                        fallback="data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='120' height='110' viewBox='0 0 120 110'%3E%3Crect width='120' height='110' fill='%23F1F5F9'/%3E%3Ctext x='50%25' y='50%25' dominant-baseline='middle' text-anchor='middle' fill='%2394A3B8' font-size='11'%3EKhông có ảnh%3C/text%3E%3C/svg%3E"
                      />
                      <div className="drawer-image-caption">{caption}</div>
                    </div>
                  );
                })}
              </div>
            </Image.PreviewGroup>
          ) : (
            <div className="drawer-empty-images">
              <PictureOutlined style={{ fontSize: 28, color: '#CBD5E1', marginBottom: 8 }} />
              <span>Chưa có hình ảnh hiện trường</span>
            </div>
          )}
        </div>

        {/* === CHILDREN SECTION === */}
        {node.children && node.children.length > 0 && (
          <div className="drawer-section">
            <div className="drawer-section-title">
              <ApartmentOutlined style={{ marginRight: 6, color: '#2563EB' }} />
              CÔNG VIỆC CON ({node.children.length})
            </div>
            <List
              size="small"
              dataSource={node.children}
              renderItem={child => (
                <List.Item style={{ padding: '8px 12px', borderRadius: 6, marginBottom: 4, background: '#F8FAFC', border: '1px solid #E6EAF0' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', width: '100%', alignItems: 'center' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                      <span className="wbs-badge" style={{ fontSize: 11 }}>{child.wbsCode}</span>
                      <span style={{ fontWeight: 500, color: '#1F2937', fontSize: 13 }}>{child.name}</span>
                    </div>
                    {renderStatusTag(child.status)}
                  </div>
                </List.Item>
              )}
            />
          </div>
        )}
        <CategoryTaskSection
          projectId={projectId}
          category={node}
        />
        {/* === QUAN HỆ PHỤ THUỘC (S-06) === */}
        <TaskDependencySection
          taskId={node?.id || node?.key}
          allTasks={allTasks}
        />
      </div>

      {/* Sticky Footer */}
      <div className="drawer-sticky-footer">
        <Button
          icon={<PlusOutlined />}
          onClick={() => onAddChild(node)}
          size="small"
        >
          Thêm công việc con
        </Button>
        <Space>
          <Button
            icon={<EditOutlined />}
            type="primary"
            size="small"
            onClick={() => onEditNode(node)}
          >
            Chỉnh sửa
          </Button>
          <Button
            danger
            icon={<DeleteOutlined />}
            size="small"
            onClick={() => onDeleteNode(node)}
          >
            Xóa
          </Button>
        </Space>
      </div>
    </Drawer>
  );
};

export default WorkDetailDrawer;