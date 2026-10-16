import React, { useEffect, useState } from 'react';
import { Alert, Badge, Button, Card, Space, Table, Tag, Typography } from 'antd';
import { ReloadOutlined } from '@ant-design/icons';
import { getSystemHealth } from '../services/systemHealthApi.js';

const { Title, Text } = Typography;

const serviceNames = {
  application: 'Ứng dụng',
  database: 'Cơ sở dữ liệu PostgreSQL',
  email: 'Dịch vụ email SMTP',
  storage: 'Lưu trữ tệp'
};

const SystemHealthPage = () => {
  const [health, setHealth] = useState(null);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState('');
  const [lastCheckedAt, setLastCheckedAt] = useState(null);

  useEffect(() => {
    let active = true;

    const refresh = async () => {
      try {
        const result = await getSystemHealth();
        if (!active) return;
        setHealth(result);
        setError('');
        setLastCheckedAt(new Date());
      } catch (requestError) {
        if (!active) return;
        setError(requestError.message || 'Không thể kiểm tra trạng thái hệ thống.');
      } finally {
        if (active) setLoading(false);
      }
    };

    refresh();
    const intervalId = window.setInterval(refresh, 30000);
    return () => {
      active = false;
      window.clearInterval(intervalId);
    };
  }, []);

  const services = Object.entries(health?.services || {}).map(([key, service]) => ({
    key,
    name: serviceNames[key] || key,
    status: service.status,
    message: service.message
  }));
  const hasDownService = health?.status === 'DOWN'
    || services.some(service => service.status !== 'UP');

  const handleRefresh = async () => {
    setRefreshing(true);
    try {
      const result = await getSystemHealth();
      setHealth(result);
      setError('');
      setLastCheckedAt(new Date());
    } catch (requestError) {
      setError(requestError.message || 'Không thể kiểm tra trạng thái hệ thống.');
    } finally {
      setRefreshing(false);
    }
  };

  return (
    <div style={{ padding: 24 }}>
      <Space direction="vertical" size="large" style={{ display: 'flex' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', gap: 16, alignItems: 'center' }}>
          <div>
            <Title level={2} style={{ marginBottom: 4 }}>Sức khỏe hệ thống</Title>
            <Text type="secondary">
              Tự động kiểm tra mỗi 30 giây
              {lastCheckedAt && ` · Cập nhật lúc ${lastCheckedAt.toLocaleTimeString('vi-VN')}`}
            </Text>
          </div>
          <Button icon={<ReloadOutlined />} loading={refreshing} onClick={handleRefresh}>
            Kiểm tra lại
          </Button>
        </div>

        {error && (
          <Alert
            type="error"
            showIcon
            message="Không thể tải trạng thái hệ thống"
            description={error}
          />
        )}

        {hasDownService && (
          <Alert
            type="error"
            showIcon
            banner
            message="Cảnh báo: Có dịch vụ đang gặp sự cố. Vui lòng kiểm tra danh sách bên dưới."
          />
        )}

        {health && !hasDownService && (
          <Alert
            type="success"
            showIcon
            message="Tất cả dịch vụ đang hoạt động bình thường."
          />
        )}

        <Card
          title="Trạng thái các dịch vụ"
          extra={health && (
            <Tag color={health.status === 'UP' ? 'success' : 'error'}>
              <Badge status={health.status === 'UP' ? 'success' : 'error'} />
              {' '}{health.status}
            </Tag>
          )}
        >
          <Table
            rowKey="key"
            loading={loading}
            pagination={false}
            dataSource={services}
            locale={{ emptyText: error || 'Đang tải trạng thái dịch vụ...' }}
            columns={[
              { title: 'Dịch vụ', dataIndex: 'name', key: 'name' },
              {
                title: 'Trạng thái',
                dataIndex: 'status',
                key: 'status',
                width: 150,
                render: status => (
                  <Tag color={status === 'UP' ? 'success' : 'error'}>
                    <Badge status={status === 'UP' ? 'success' : 'error'} />
                    {' '}{status}
                  </Tag>
                )
              },
              { title: 'Thông tin', dataIndex: 'message', key: 'message' }
            ]}
          />
        </Card>
      </Space>
    </div>
  );
};

export default SystemHealthPage;
