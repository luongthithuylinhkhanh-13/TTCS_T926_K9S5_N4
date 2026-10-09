import React, { useState, useEffect } from 'react';
import { Badge, Button, Tooltip, notification } from 'antd';
import {
  CloudSyncOutlined,
  WifiOutlined,
  DisconnectOutlined,
  LoadingOutlined,
  UnorderedListOutlined
} from '@ant-design/icons';
import syncQueueService from '../../services/syncQueueService';
import autoSyncService from '../../services/autoSyncService';
import SyncQueueModal from './SyncQueueModal';

const SyncQueueStatusBar = () => {
  const [online, setOnline] = useState(syncQueueService.isOnline());
  const [stats, setStats] = useState(syncQueueService.getStats());
  const [isSyncing, setIsSyncing] = useState(false);
  const [isModalOpen, setIsModalOpen] = useState(false);

  useEffect(() => {
    // Khởi tạo autoSyncService
    autoSyncService.init();

    const updateStats = () => {
      setOnline(syncQueueService.isOnline());
      setStats(syncQueueService.getStats());
    };

    const handleSyncStatus = e => {
      const { type, message } = e.detail;
      setOnline(syncQueueService.isOnline());
      setStats(syncQueueService.getStats());

      if (type === 'SYNCING') {
        setIsSyncing(true);
      } else if (type === 'SYNC_SUCCESS') {
        setIsSyncing(false);
        notification.success({
          message: 'Tự động đồng bộ thành công (T-71)',
          description: message,
          placement: 'bottomRight'
        });
      } else if (type === 'SYNC_PARTIAL') {
        setIsSyncing(false);
        notification.warning({
          message: 'Đồng bộ một phần',
          description: message,
          placement: 'bottomRight'
        });
      } else if (type === 'ONLINE') {
        notification.info({
          message: 'Đã có mạng Internet trở lại',
          description: 'Hệ thống đang tự động gửi dữ liệu trong hàng đợi lên máy chủ...',
          placement: 'bottomRight'
        });
      }
    };

    window.addEventListener('online', updateStats);
    window.addEventListener('offline', updateStats);
    window.addEventListener('ntdhtcct:sync-queue-updated', updateStats);
    window.addEventListener('ntdhtcct:sync-status', handleSyncStatus);

    return () => {
      window.removeEventListener('online', updateStats);
      window.removeEventListener('offline', updateStats);
      window.removeEventListener('ntdhtcct:sync-queue-updated', updateStats);
      window.removeEventListener('ntdhtcct:sync-status', handleSyncStatus);
    };
  }, []);

  const handleManualSync = async () => {
    setIsSyncing(true);
    await autoSyncService.syncQueueNow();
    setIsSyncing(false);
  };

  return (
    <>
      <div
        style={{
          display: 'inline-flex',
          alignItems: 'center',
          gap: '10px',
          padding: '4px 12px',
          borderRadius: '20px',
          background: online ? '#f6ffed' : '#fffbe6',
          border: `1px solid ${online ? '#b7eb8f' : '#ffe58f'}`,
          fontSize: '13px'
        }}
      >
        {/* Chỉ báo trạng thái mạng */}
        {online ? (
          <span style={{ color: '#52c41a', display: 'flex', alignItems: 'center', gap: '4px' }}>
            <WifiOutlined /> Trực tuyến
          </span>
        ) : (
          <span style={{ color: '#fa8c16', display: 'flex', alignItems: 'center', gap: '4px', fontWeight: 600 }}>
            <DisconnectOutlined /> Ngoại tuyến (Offline)
          </span>
        )}

        {/* Badge số lượng dữ liệu chưa đồng bộ (T-70) */}
        <Tooltip title="Số lượng bản ghi chưa đồng bộ trong hàng đợi">
          <Badge
            count={stats.pending + stats.failed}
            style={{ backgroundColor: stats.failed > 0 ? '#ff4d4f' : '#faad14' }}
          >
            <Button
              type="text"
              size="small"
              icon={<UnorderedListOutlined />}
              onClick={() => setIsModalOpen(true)}
              style={{ color: '#595959' }}
            >
              Hàng đợi ({stats.total})
            </Button>
          </Badge>
        </Tooltip>

        {/* Nút kích hoạt đồng bộ (T-71) */}
        {stats.hasPending && online && (
          <Button
            type="primary"
            size="small"
            icon={isSyncing ? <LoadingOutlined /> : <CloudSyncOutlined />}
            loading={isSyncing}
            onClick={handleManualSync}
            style={{ backgroundColor: '#1890ff' }}
          >
            {isSyncing ? 'Đang gửi...' : 'Đồng bộ ngay'}
          </Button>
        )}
      </div>

      <SyncQueueModal
        open={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        onSyncNow={handleManualSync}
        isSyncing={isSyncing}
      />
    </>
  );
};

export default SyncQueueStatusBar;
