import React, { useState, useEffect } from 'react';
import { Modal, Table, Tag, Button, Popconfirm, Empty, Space } from 'antd';
import {
  CloudSyncOutlined,
  DeleteOutlined,
  ReloadOutlined,
  CheckCircleOutlined,
  SyncOutlined,
  CloseCircleOutlined
} from '@ant-design/icons';
import syncQueueService from '../../services/syncQueueService';

const SyncQueueModal = ({ open, onClose, onSyncNow, isSyncing }) => {
  const [queue, setQueue] = useState([]);

  const refreshQueue = () => {
    setQueue(syncQueueService.getQueue());
  };

  useEffect(() => {
    if (open) {
      refreshQueue();
    }
  }, [open]);

  const handleRemove = id => {
    syncQueueService.remove(id);
    refreshQueue();
  };

  const handleClearAll = () => {
    syncQueueService.clearAll();
    refreshQueue();
  };

  const columns = [
    {
      title: 'Mục dữ liệu',
      dataIndex: 'title',
      key: 'title',
      render: (text, item) => (
        <div>
          <div style={{ fontWeight: 600 }}>{text || 'Nhật ký công trường'}</div>
          <div style={{ fontSize: '12px', color: '#8c8c8c' }}>
            ID: {item.clientSyncId?.substring(0, 18)}...
          </div>
        </div>
      )
    },
    {
      title: 'Thời điểm tạo (Offline)',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 170,
      render: date => (date ? new Date(date).toLocaleString('vi-VN') : '--')
    },
    {
      title: 'Trạng thái',
      dataIndex: 'status',
      key: 'status',
      width: 130,
      render: (status, item) => {
        if (status === 'SYNCING') {
          return <Tag icon={<SyncOutlined spin />} color="processing">Đang gửi</Tag>;
        }
        if (status === 'FAILED') {
          return (
            <Tag icon={<CloseCircleOutlined />} color="error">
              Lỗi (thử {item.retryCount} lần)
            </Tag>
          );
        }
        if (status === 'SYNCED') {
          return <Tag icon={<CheckCircleOutlined />} color="success">Đã gửi</Tag>;
        }
        return <Tag color="warning">Chờ gửi</Tag>;
      }
    },
    {
      title: 'Chi tiết lỗi',
      dataIndex: 'lastError',
      key: 'lastError',
      render: text => text ? <span style={{ color: '#ff4d4f', fontSize: '12px' }}>{text}</span> : <span style={{ color: '#bfbfbf' }}>-</span>
    },
    {
      title: 'Thao tác',
      key: 'action',
      width: 100,
      render: (_, item) => (
        <Popconfirm
          title="Xác nhận xóa bản ghi này khỏi hàng đợi?"
          onConfirm={() => handleRemove(item.id)}
          okText="Xóa"
          cancelText="Hủy"
        >
          <Button type="text" danger size="small" icon={<DeleteOutlined />}>
            Xóa
          </Button>
        </Popconfirm>
      )
    }
  ];

  return (
    <Modal
      title={
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <CloudSyncOutlined style={{ color: '#1890ff' }} />
          <span>Hàng đợi dữ liệu chưa đồng bộ (T-70, T-71 - S-30)</span>
        </div>
      }
      open={open}
      onCancel={onClose}
      width={780}
      footer={[
        <Popconfirm
          key="clear"
          title="Xóa toàn bộ các bản ghi trong hàng đợi?"
          onConfirm={handleClearAll}
          okText="Xóa hết"
          cancelText="Hủy"
          disabled={queue.length === 0}
        >
          <Button danger disabled={queue.length === 0}>
            Xóa tất cả
          </Button>
        </Popconfirm>,
        <Button key="refresh" icon={<ReloadOutlined />} onClick={refreshQueue}>
          Làm mới
        </Button>,
        <Button
          key="sync"
          type="primary"
          icon={<CloudSyncOutlined />}
          loading={isSyncing}
          onClick={async () => {
            await onSyncNow();
            refreshQueue();
          }}
          disabled={queue.length === 0}
        >
          Đồng bộ ngay
        </Button>
      ]}
    >
      <div style={{ marginBottom: '12px', fontSize: '13px', color: '#595959' }}>
        Khi thiết bị mất kết nối mạng, các thao tác tạo mới và cập nhật sẽ được lưu an toàn vào hàng đợi ngoại tuyến.
        Hệ thống sẽ <strong>tự động gửi lên máy chủ</strong> ngay khi có mạng trở lại.
      </div>

      <Table
        dataSource={queue}
        columns={columns}
        rowKey="id"
        size="small"
        pagination={{ pageSize: 5 }}
        locale={{
          emptyText: <Empty description="Hàng đợi đồng bộ đang trống. Không có dữ liệu chờ gửi." />
        }}
      />
    </Modal>
  );
};

export default SyncQueueModal;
