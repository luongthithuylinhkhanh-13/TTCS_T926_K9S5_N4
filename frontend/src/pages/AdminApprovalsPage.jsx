import React, { useEffect, useState, useCallback } from 'react';
import {
  Card,
  Table,
  Tag,
  Button,
  Space,
  Modal,
  Form,
  Input,
  Select,
  Alert,
  message,
  Typography,
  Radio,
  Badge
} from 'antd';
import {
  CheckCircleOutlined,
  CloseCircleOutlined,
  ReloadOutlined,
  UserOutlined,
  SearchOutlined
} from '@ant-design/icons';
import { getAdminUsers, approveUser, rejectUser, getAdminRoles } from '../services/adminApi';

const { Title, Text } = Typography;
const { TextArea } = Input;

const statusColorMap = {
  PENDING: 'gold',
  ACTIVE: 'green',
  REJECTED: 'red',
  DISABLED: 'default'
};

const statusTextMap = {
  PENDING: 'Chờ duyệt',
  ACTIVE: 'Đã kích hoạt',
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

const BUSINESS_ROLES = [
  { role: 'PROJECT_MANAGER', label: 'PROJECT_MANAGER - Ban quản lý dự án' },
  { role: 'CUSTOMER', label: 'CUSTOMER - Chủ đầu tư' },
  { role: 'SITE_MANAGER', label: 'SITE_MANAGER - Chỉ huy trưởng công trường' },
  { role: 'SITE_ENGINEER', label: 'SITE_ENGINEER - Kỹ sư giám sát' },
  { role: 'WORKER', label: 'WORKER - Đội trưởng thi công' },
  { role: 'STAFF', label: 'STAFF - Kế toán chủ đầu tư' }
];

const AdminApprovalsPage = () => {
  const [users, setUsers] = useState([]);
  const [roles, setRoles] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [statusFilter, setStatusFilter] = useState('PENDING');
  const [searchText, setSearchText] = useState('');

  // Modal states
  const [approveModalVisible, setApproveModalVisible] = useState(false);
  const [rejectModalVisible, setRejectModalVisible] = useState(false);
  const [selectedUser, setSelectedUser] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  const [approveForm] = Form.useForm();
  const [rejectForm] = Form.useForm();

  const fetchRoles = async () => {
    try {
      const data = await getAdminRoles();
      if (Array.isArray(data)) {
        setRoles(data);
      }
    } catch (err) {
      console.error('Failed to load roles:', err);
    }
  };

  const fetchUsers = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const params = {};
      if (statusFilter !== 'ALL') {
        params.status = statusFilter;
      }
      if (searchText.trim()) {
        params.search = searchText.trim();
      }
      const data = await getAdminUsers(params);
      setUsers(Array.isArray(data) ? data : []);
    } catch (err) {
      setError(err.message || 'Không thể tải danh sách tài khoản');
    } finally {
      setLoading(false);
    }
  }, [statusFilter, searchText]);

  useEffect(() => {
    fetchRoles();
  }, []);

  useEffect(() => {
    fetchUsers();
  }, [fetchUsers]);

  const handleOpenApprove = (user) => {
    setSelectedUser(user);
    approveForm.setFieldsValue({
      roleName: user.roleName || 'WORKER'
    });
    setApproveModalVisible(true);
  };

  const handleConfirmApprove = async () => {
    try {
      const values = await approveForm.validateFields();
      setSubmitting(true);
      await approveUser(selectedUser.id, values.roleName);
      message.success(`Đã phê duyệt tài khoản ${selectedUser.fullName || selectedUser.email} thành công!`);
      setApproveModalVisible(false);
      setSelectedUser(null);
      fetchUsers();
    } catch (err) {
      if (err.errorFields) return;
      message.error(err.message || 'Lỗi khi phê duyệt tài khoản');
    } finally {
      setSubmitting(false);
    }
  };

  const handleOpenReject = (user) => {
    setSelectedUser(user);
    rejectForm.resetFields();
    setRejectModalVisible(true);
  };

  const handleConfirmReject = async () => {
    try {
      const values = await rejectForm.validateFields();
      setSubmitting(true);
      await rejectUser(selectedUser.id, values.reason);
      message.success(`Đã từ chối tài khoản ${selectedUser.fullName || selectedUser.email}!`);
      setRejectModalVisible(false);
      setSelectedUser(null);
      fetchUsers();
    } catch (err) {
      if (err.errorFields) return;
      message.error(err.message || 'Lỗi khi từ chối tài khoản');
    } finally {
      setSubmitting(false);
    }
  };

  const pendingCount = users.filter(u => (u.status || '').toUpperCase() === 'PENDING').length;

  const columns = [
    {
      title: 'Họ và tên',
      dataIndex: 'fullName',
      key: 'fullName',
      render: (name, record) => (
        <Space orientation="vertical" size={2}>
          <Text strong>{name || 'Chưa cập nhật'}</Text>
          <Text orientation="secondary" style={{ fontSize: 12, color: '#64748B' }}>
            ID: {record.id ? record.id.substring(0, 8) + '...' : '--'}
          </Text>
        </Space>
      )
    },
    {
      title: 'Email',
      dataIndex: 'email',
      key: 'email',
      render: email => <Text copyable>{email}</Text>
    },
    {
      title: 'Vai trò',
      dataIndex: 'roleName',
      key: 'roleName',
      render: role => {
        const info = ROLE_MAP[role] || { label: role || 'WORKER', color: 'blue' };
        return <Tag color={info.color}>{info.label}</Tag>;
      }
    },
    {
      title: 'Ngày đăng ký',
      dataIndex: 'createdAt',
      key: 'createdAt',
      render: date => {
        if (!date) return '--';
        try {
          return new Date(date).toLocaleString('vi-VN');
        } catch {
          return date;
        }
      }
    },
    {
      title: 'Trạng thái',
      dataIndex: 'status',
      key: 'status',
      render: (status, record) => (
        <Space orientation="vertical" size={2}>
          <Tag color={statusColorMap[status] || 'default'}>
            {statusTextMap[status] || status}
          </Tag>
          {status === 'REJECTED' && record.rejectionReason && (
            <Text type="danger" style={{ fontSize: 12 }}>
              Lý do: {record.rejectionReason}
            </Text>
          )}
        </Space>
      )
    },
    {
      title: 'Hành động',
      key: 'actions',
      align: 'center',
      width: 220,
      render: (_, record) => {
        const isPending = (record.status || '').toUpperCase() === 'PENDING';
        return (
          <Space>
            <Button
              type="primary"
              size="small"
              icon={<CheckCircleOutlined />}
              style={{ backgroundColor: '#10B981', borderColor: '#10B981' }}
              onClick={() => handleOpenApprove(record)}
            >
              {isPending ? 'Duyệt' : 'Kích hoạt lại'}
            </Button>
            {isPending && (
              <Button
                danger
                size="small"
                icon={<CloseCircleOutlined />}
                onClick={() => handleOpenReject(record)}
              >
                Từ chối
              </Button>
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
          <Title level={3} style={{ marginBottom: 4 }}>Duyệt thành viên</Title>
          <Text type="secondary">Xét duyệt và cấp quyền hoạt động cho các tài khoản người dùng đăng ký vào hệ thống</Text>
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

      <Card variant="borderless" style={{ boxShadow: '0 1px 3px rgba(0,0,0,0.05)', borderRadius: 8 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 16, marginBottom: 16 }}>
          <Radio.Group
            value={statusFilter}
            onChange={e => setStatusFilter(e.target.value)}
            buttonStyle="solid"
          >
            <Radio.Button value="PENDING">
              Chờ duyệt {statusFilter === 'PENDING' && <Badge count={pendingCount} offset={[6, -2]} />}
            </Radio.Button>
            <Radio.Button value="REJECTED">Đã từ chối</Radio.Button>
            <Radio.Button value="ACTIVE">Đã duyệt</Radio.Button>
            <Radio.Button value="ALL">Tất cả</Radio.Button>
          </Radio.Group>

          <Input
            placeholder="Tìm theo họ tên, email..."
            prefix={<SearchOutlined style={{ color: '#94A3B8' }} />}
            style={{ width: 280 }}
            value={searchText}
            onChange={e => setSearchText(e.target.value)}
            allowClear
          />
        </div>

        <Table
          dataSource={users}
          columns={columns}
          rowKey="id"
          loading={loading}
          pagination={{ pageSize: 10, showSizeChanger: true }}
          locale={{ emptyText: 'Không có tài khoản nào phù hợp' }}
        />
      </Card>

      {/* Modal Duyệt tài khoản */}
      <Modal
        title={
          <Space>
            <CheckCircleOutlined style={{ color: '#10B981' }} />
            <span>Phê duyệt tài khoản</span>
          </Space>
        }
        open={approveModalVisible}
        onOk={handleConfirmApprove}
        onCancel={() => setApproveModalVisible(false)}
        confirmLoading={submitting}
        okText="Xác nhận duyệt"
        cancelText="Hủy"
        okButtonProps={{ style: { backgroundColor: '#10B981', borderColor: '#10B981' } }}
      >
        {selectedUser && (
          <Form form={approveForm} layout="vertical">
            <p>
              Bạn đang phê duyệt tài khoản: <strong>{selectedUser.fullName || selectedUser.email}</strong> ({selectedUser.email})
            </p>
            <Form.Item
              name="roleName"
              label="Chỉ định vai trò trong hệ thống"
              rules={[{ required: true, message: 'Vui lòng chọn vai trò cho tài khoản' }]}
            >
              <Select placeholder="Chọn 1 trong 6 vai trò nghiệp vụ">
                {BUSINESS_ROLES.map(r => (
                  <Select.Option key={r.role} value={r.role}>
                    {r.label}
                  </Select.Option>
                ))}
              </Select>
            </Form.Item>
            <Alert
              type="info"
              showIcon
              message="Sau khi được duyệt, người dùng có thể đăng nhập và truy cập các chức năng tương ứng với vai trò được giao."
            />
          </Form>
        )}
      </Modal>

      {/* Modal Từ chối tài khoản */}
      <Modal
        title={
          <Space>
            <CloseCircleOutlined style={{ color: '#EF4444' }} />
            <span>Từ chối yêu cầu tài khoản</span>
          </Space>
        }
        open={rejectModalVisible}
        onOk={handleConfirmReject}
        onCancel={() => setRejectModalVisible(false)}
        confirmLoading={submitting}
        okText="Xác nhận từ chối"
        cancelText="Hủy"
        okButtonProps={{ danger: true }}
      >
        {selectedUser && (
          <Form form={rejectForm} layout="vertical">
            <p>
              Bạn đang từ chối tài khoản: <strong>{selectedUser.fullName || selectedUser.email}</strong> ({selectedUser.email})
            </p>
            <Form.Item
              name="reason"
              label="Lý do từ chối"
              rules={[{ required: true, message: 'Vui lòng nhập lý do từ chối để thông báo cho người dùng' }]}
            >
              <TextArea
                rows={3}
                placeholder="Nhập lý do từ chối (ví dụ: Thông tin chưa xác thực, không thuộc danh sách nhân sự dự án...)"
              />
            </Form.Item>
          </Form>
        )}
      </Modal>
    </div>
  );
};

export default AdminApprovalsPage;
