import React, { useEffect, useState, useCallback } from 'react';
import {
  Card,
  Table,
  Tag,
  Button,
  Space,
  Modal,
  Input,
  Select,
  Alert,
  message,
  Typography,
  Row,
  Col,
  Statistic,
  Descriptions,
  Popconfirm
} from 'antd';
import {
  SearchOutlined,
  ReloadOutlined,
  UserOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
  StopOutlined,
  EyeOutlined,
  LockOutlined,
  UnlockOutlined
} from '@ant-design/icons';
import { getAdminUsers, getAdminRoles, updateUserStatus } from '../services/adminApi';

const { Title, Text } = Typography;

const statusColorMap = {
  ACTIVE: 'green',
  PENDING: 'gold',
  REJECTED: 'red',
  DISABLED: 'default'
};

const statusTextMap = {
  ACTIVE: 'Đang hoạt động',
  PENDING: 'Chờ duyệt',
  REJECTED: 'Đã từ chối',
  DISABLED: 'Đã khóa'
};

const ROLE_MAP = {
  ADMIN: { label: 'Quản trị hệ thống', color: '#DC2626' },
  PROJECT_MANAGER: { label: 'Ban quản lý dự án', color: '#2563EB' },
  CUSTOMER: { label: 'Chủ đầu tư', color: '#0284C7' },
  SITE_MANAGER: { label: 'Chỉ huy trưởng công trường', color: '#D97706' },
  SITE_ENGINEER: { label: 'Kỹ sư giám sát', color: '#0D9488' },
  WORKER: { label: 'Đội trưởng thi công', color: '#10B981' },
  STAFF: { label: 'Kế toán chủ đầu tư', color: '#7C3AED' },
  VIEWER: { label: 'Người xem / Legacy', color: '#6B7280' }
};

const AdminUsersPage = () => {
  const [users, setUsers] = useState([]);
  const [roles, setRoles] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  // Filters
  const [searchText, setSearchText] = useState('');
  const [selectedRole, setSelectedRole] = useState(null);
  const [selectedStatus, setSelectedStatus] = useState(null);

  // Detail Modal
  const [detailUser, setDetailUser] = useState(null);
  const [detailModalOpen, setDetailModalOpen] = useState(false);

  const fetchRoles = async () => {
    try {
      const data = await getAdminRoles();
      if (Array.isArray(data)) setRoles(data);
    } catch (err) {
      console.error('Failed to load roles:', err);
    }
  };

  const fetchUsers = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const params = {};
      if (selectedRole) params.role = selectedRole;
      if (selectedStatus) params.status = selectedStatus;
      if (searchText.trim()) params.search = searchText.trim();

      const data = await getAdminUsers(params);
      setUsers(Array.isArray(data) ? data : []);
    } catch (err) {
      setError(err.message || 'Không thể tải danh sách người dùng');
    } finally {
      setLoading(false);
    }
  }, [selectedRole, selectedStatus, searchText]);

  useEffect(() => {
    fetchRoles();
  }, []);

  useEffect(() => {
    fetchUsers();
  }, [fetchUsers]);

  const handleToggleStatus = async (user) => {
    const isCurrentlyActive = (user.status || '').toUpperCase() === 'ACTIVE';
    const nextStatus = isCurrentlyActive ? 'DISABLED' : 'ACTIVE';
    try {
      await updateUserStatus(user.id, nextStatus);
      message.success(`Đã ${isCurrentlyActive ? 'khóa' : 'kích hoạt'} tài khoản ${user.fullName || user.email}`);
      fetchUsers();
    } catch (err) {
      message.error(err.message || 'Lỗi khi cập nhật trạng thái');
    }
  };

  const activeCount = users.filter(u => (u.status || '').toUpperCase() === 'ACTIVE').length;
  const pendingCount = users.filter(u => (u.status || '').toUpperCase() === 'PENDING').length;
  const lockedCount = users.filter(u => ['DISABLED', 'REJECTED'].includes((u.status || '').toUpperCase())).length;

  const columns = [
    {
      title: 'Họ và tên',
      dataIndex: 'fullName',
      key: 'fullName',
      render: (name, record) => (
        <div>
          <div style={{ fontWeight: 600, color: '#0F172A' }}>{name || 'Chưa cập nhật'}</div>
          <small style={{ color: '#64748B' }}>ID: {record.id ? record.id.substring(0, 8) + '...' : '--'}</small>
        </div>
      )
    },
    {
      title: 'Email',
      dataIndex: 'email',
      key: 'email',
      render: email => <Text copyable>{email}</Text>
    },
    {
      title: 'Số điện thoại',
      dataIndex: 'phoneNumber',
      key: 'phoneNumber',
      render: phone => phone || <span style={{ color: '#94A3B8' }}>--</span>
    },
    {
      title: 'Vai trò',
      dataIndex: 'roleName',
      key: 'roleName',
      render: role => {
        const info = ROLE_MAP[role] || { label: role || 'Chưa phân vai trò', color: 'blue' };
        return <Tag color={info.color}>{info.label}</Tag>;
      }
    },
    {
      title: 'Trạng thái',
      dataIndex: 'status',
      key: 'status',
      render: status => (
        <Tag color={statusColorMap[status] || 'default'}>
          {statusTextMap[status] || status}
        </Tag>
      )
    },
    {
      title: 'Ngày tạo',
      dataIndex: 'createdAt',
      key: 'createdAt',
      render: date => {
        if (!date) return '--';
        try {
          return new Date(date).toLocaleDateString('vi-VN');
        } catch {
          return date;
        }
      }
    },
    {
      title: 'Thao tác',
      key: 'action',
      align: 'center',
      width: 180,
      render: (_, record) => {
        const isActive = (record.status || '').toUpperCase() === 'ACTIVE';
        const isSelf = record.email === 'admin@admin.com';

        return (
          <Space>
            <Button
              type="text"
              icon={<EyeOutlined />}
              onClick={() => {
                setDetailUser(record);
                setDetailModalOpen(true);
              }}
              title="Xem chi tiết"
            >
              Chi tiết
            </Button>

            {!isSelf && (
              <Popconfirm
                title={isActive ? 'Khóa tài khoản này?' : 'Mở khóa tài khoản này?'}
                description={isActive
                  ? 'Người dùng này sẽ không thể đăng nhập vào hệ thống.'
                  : 'Người dùng sẽ có thể đăng nhập bình thường.'}
                onConfirm={() => handleToggleStatus(record)}
                okText="Đồng ý"
                cancelText="Hủy"
              >
                <Button
                  type="text"
                  danger={isActive}
                  icon={isActive ? <LockOutlined /> : <UnlockOutlined />}
                  style={{ color: !isActive ? '#10B981' : undefined }}
                >
                  {isActive ? 'Khóa' : 'Mở'}
                </Button>
              </Popconfirm>
            )}
          </Space>
        );
      }
    }
  ];

  return (
    <div style={{ padding: '4px 0 24px' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 20 }}>
        <div>
          <Title level={3} style={{ marginBottom: 4 }}>Quản lý người dùng</Title>
          <Text type="secondary">Theo dõi, quản lý trạng thái và danh sách tài khoản trong toàn hệ thống</Text>
        </div>
        <Button icon={<ReloadOutlined />} onClick={fetchUsers} loading={loading}>
          Làm mới
        </Button>
      </div>

      {error && (
        <Alert
          type="error"
          showIcon
          message="Lỗi tải dữ liệu"
          description={error}
          style={{ marginBottom: 16 }}
          closable
          onClose={() => setError('')}
        />
      )}

      {/* KPI Cards */}
      <Row gutter={[16, 16]} style={{ marginBottom: 20 }}>
        <Col xs={24} sm={12} md={6}>
          <Card variant="borderless" style={{ boxShadow: '0 1px 3px rgba(0,0,0,0.05)', borderRadius: 8 }}>
            <Statistic
              title="Tổng số tài khoản"
              value={users.length}
              prefix={<UserOutlined style={{ color: '#2563EB' }} />}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card variant="borderless" style={{ boxShadow: '0 1px 3px rgba(0,0,0,0.05)', borderRadius: 8 }}>
            <Statistic
              title="Đang hoạt động"
              value={activeCount}
              prefix={<CheckCircleOutlined style={{ color: '#10B981' }} />}
              valueStyle={{ color: '#10B981' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card variant="borderless" style={{ boxShadow: '0 1px 3px rgba(0,0,0,0.05)', borderRadius: 8 }}>
            <Statistic
              title="Chờ duyệt"
              value={pendingCount}
              prefix={<ClockCircleOutlined style={{ color: '#F59E0B' }} />}
              valueStyle={{ color: '#F59E0B' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card variant="borderless" style={{ boxShadow: '0 1px 3px rgba(0,0,0,0.05)', borderRadius: 8 }}>
            <Statistic
              title="Khóa / Từ chối"
              value={lockedCount}
              prefix={<StopOutlined style={{ color: '#EF4444' }} />}
              valueStyle={{ color: '#64748B' }}
            />
          </Card>
        </Col>
      </Row>

      <Card variant="borderless" style={{ boxShadow: '0 1px 3px rgba(0,0,0,0.05)', borderRadius: 8 }}>
        <div style={{ display: 'flex', gap: 12, flexWrap: 'wrap', marginBottom: 16, alignItems: 'center' }}>
          <Input
            placeholder="Tìm theo tên, email..."
            prefix={<SearchOutlined style={{ color: '#94A3B8' }} />}
            style={{ width: 260 }}
            value={searchText}
            onChange={e => setSearchText(e.target.value)}
            allowClear
          />

          <Select
            placeholder="Lọc vai trò"
            style={{ width: 280 }}
            allowClear
            value={selectedRole}
            onChange={val => setSelectedRole(val)}
          >
            <Select.Option value="ADMIN">ADMIN - Quản trị hệ thống</Select.Option>
            <Select.Option value="PROJECT_MANAGER">PROJECT_MANAGER - Ban quản lý dự án</Select.Option>
            <Select.Option value="CUSTOMER">CUSTOMER - Chủ đầu tư</Select.Option>
            <Select.Option value="SITE_MANAGER">SITE_MANAGER - Chỉ huy trưởng công trường</Select.Option>
            <Select.Option value="SITE_ENGINEER">SITE_ENGINEER - Kỹ sư giám sát</Select.Option>
            <Select.Option value="WORKER">WORKER - Đội trưởng thi công</Select.Option>
            <Select.Option value="STAFF">STAFF - Kế toán chủ đầu tư</Select.Option>
          </Select>

          <Select
            placeholder="Lọc trạng thái"
            style={{ width: 180 }}
            allowClear
            value={selectedStatus}
            onChange={val => setSelectedStatus(val)}
          >
            <Select.Option value="ACTIVE">Đang hoạt động</Select.Option>
            <Select.Option value="PENDING">Chờ duyệt</Select.Option>
            <Select.Option value="REJECTED">Đã từ chối</Select.Option>
            <Select.Option value="DISABLED">Đã khóa</Select.Option>
          </Select>
        </div>

        <Table
          dataSource={users}
          columns={columns}
          rowKey="id"
          loading={loading}
          pagination={{ pageSize: 10, showSizeChanger: true }}
          locale={{ emptyText: 'Không tìm thấy người dùng phù hợp' }}
        />
      </Card>

      {/* Modal Chi tiết người dùng */}
      <Modal
        title="Thông tin chi tiết tài khoản"
        open={detailModalOpen}
        onCancel={() => setDetailModalOpen(false)}
        footer={[
          <Button key="close" onClick={() => setDetailModalOpen(false)}>
            Đóng
          </Button>
        ]}
        width={600}
      >
        {detailUser && (
          <Descriptions bordered column={1} size="small" style={{ marginTop: 16 }}>
            <Descriptions.Item label="Mã định danh (ID)">{detailUser.id}</Descriptions.Item>
            <Descriptions.Item label="Họ và tên">{detailUser.fullName || 'Chưa cập nhật'}</Descriptions.Item>
            <Descriptions.Item label="Email">{detailUser.email}</Descriptions.Item>
            <Descriptions.Item label="Số điện thoại">{detailUser.phoneNumber || 'Chưa cập nhật'}</Descriptions.Item>
            <Descriptions.Item label="Vai trò">
              <Tag color={ROLE_MAP[detailUser.roleName]?.color || 'blue'}>
                {ROLE_MAP[detailUser.roleName]?.label || detailUser.roleName || 'Chưa phân vai trò'}
              </Tag>
            </Descriptions.Item>
            <Descriptions.Item label="Trạng thái">
              <Tag color={statusColorMap[detailUser.status] || 'default'}>
                {statusTextMap[detailUser.status] || detailUser.status}
              </Tag>
            </Descriptions.Item>
            {detailUser.status === 'REJECTED' && (
              <Descriptions.Item label="Lý do từ chối">
                <Text type="danger">{detailUser.rejectionReason || 'Không ghi rõ'}</Text>
              </Descriptions.Item>
            )}
            <Descriptions.Item label="Đã xác thực email">
              {detailUser.emailVerified ? <Tag color="green">Đã xác thực</Tag> : <Tag color="default">Chưa xác thực</Tag>}
            </Descriptions.Item>
            <Descriptions.Item label="Ngày tạo">
              {detailUser.createdAt ? new Date(detailUser.createdAt).toLocaleString('vi-VN') : '--'}
            </Descriptions.Item>
            <Descriptions.Item label="Ngày cập nhật">
              {detailUser.updatedAt ? new Date(detailUser.updatedAt).toLocaleString('vi-VN') : '--'}
            </Descriptions.Item>
          </Descriptions>
        )}
      </Modal>
    </div>
  );
};

export default AdminUsersPage;
