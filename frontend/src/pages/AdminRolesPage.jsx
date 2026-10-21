import React, { useEffect, useState, useCallback } from 'react';
import {
  Card,
  Table,
  Tag,
  Button,
  Space,
  Modal,
  Form,
  Select,
  Alert,
  message,
  Typography,
  Row,
  Col,
  Input
} from 'antd';
import {
  SafetyCertificateOutlined,
  ReloadOutlined,
  EditOutlined,
  SearchOutlined,
  UserOutlined
} from '@ant-design/icons';
import { getAdminUsers, getAdminRoles, updateUserRole } from '../services/adminApi';

const { Title, Text, Paragraph } = Typography;

// 1 Vai trò Quản trị hệ thống riêng biệt
const SYSTEM_ROLE = {
  role: 'ADMIN',
  name: 'Quản trị hệ thống',
  label: 'ADMIN - Quản trị hệ thống',
  description: 'Quản trị toàn diện hệ thống, phê duyệt thành viên mới, quản lý người dùng và phân quyền tài khoản.',
  color: '#DC2626'
};

// 6 Vai trò nghiệp vụ của dự án theo đúng Backlog
const BUSINESS_ROLES = [
  {
    role: 'PROJECT_MANAGER',
    name: 'Ban quản lý dự án',
    label: 'PROJECT_MANAGER - Ban quản lý dự án',
    description: 'Quản lý và điều hành dự án, cơ cấu công việc, tiến độ, thành viên và các nghiệp vụ quản lý dự án theo quyền được cấp.',
    color: '#2563EB'
  },
  {
    role: 'CUSTOMER',
    name: 'Chủ đầu tư',
    label: 'CUSTOMER - Chủ đầu tư',
    description: 'Theo dõi dự án và thực hiện các nghiệp vụ phê duyệt, mốc, thay đổi và báo cáo thuộc quyền Chủ đầu tư.',
    color: '#0284C7'
  },
  {
    role: 'SITE_MANAGER',
    name: 'Chỉ huy trưởng công trường',
    label: 'SITE_MANAGER - Chỉ huy trưởng công trường',
    description: 'Điều hành hoạt động thi công tại công trường, theo dõi tiến độ và giao công việc cho đội thi công.',
    color: '#D97706'
  },
  {
    role: 'SITE_ENGINEER',
    name: 'Kỹ sư giám sát',
    label: 'SITE_ENGINEER - Kỹ sư giám sát',
    description: 'Giám sát hiện trường, lập nhật ký thi công, ghi nhận nhân lực, thiết bị, thời tiết, ảnh và khối lượng.',
    color: '#0D9488'
  },
  {
    role: 'WORKER',
    name: 'Đội trưởng thi công',
    label: 'WORKER - Đội trưởng thi công',
    description: 'Nhận công việc được giao, tổ chức thực hiện, báo khối lượng hoàn thành và các vướng mắc.',
    color: '#10B981'
  },
  {
    role: 'STAFF',
    name: 'Kế toán chủ đầu tư',
    label: 'STAFF - Kế toán chủ đầu tư',
    description: 'Thực hiện và theo dõi các nghiệp vụ tài chính, thanh toán và tạm giữ theo hợp đồng thuộc phạm vi được cấp.',
    color: '#7C3AED'
  }
];

const ROLE_DISPLAY_MAP = {
  ADMIN: { name: 'Quản trị hệ thống', color: '#DC2626', label: 'ADMIN - Quản trị hệ thống' },
  PROJECT_MANAGER: { name: 'Ban quản lý dự án', color: '#2563EB', label: 'PROJECT_MANAGER - Ban quản lý dự án' },
  CUSTOMER: { name: 'Chủ đầu tư', color: '#0284C7', label: 'CUSTOMER - Chủ đầu tư' },
  SITE_MANAGER: { name: 'Chỉ huy trưởng công trường', color: '#D97706', label: 'SITE_MANAGER - Chỉ huy trưởng công trường' },
  SITE_ENGINEER: { name: 'Kỹ sư giám sát', color: '#0D9488', label: 'SITE_ENGINEER - Kỹ sư giám sát' },
  WORKER: { name: 'Đội trưởng thi công', color: '#10B981', label: 'WORKER - Đội trưởng thi công' },
  STAFF: { name: 'Kế toán chủ đầu tư', color: '#7C3AED', label: 'STAFF - Kế toán chủ đầu tư' },
  VIEWER: { name: 'Người xem / Kế toán (Legacy)', color: '#6B7280', label: 'VIEWER - Legacy' }
};

const AdminRolesPage = () => {
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  // Role edit modal
  const [modalOpen, setModalOpen] = useState(false);
  const [selectedUser, setSelectedUser] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const [form] = Form.useForm();

  // Search & filter
  const [searchText, setSearchText] = useState('');
  const [filterRole, setFilterRole] = useState(null);

  const fetchUsers = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const params = {};
      if (filterRole) params.role = filterRole;
      if (searchText.trim()) params.search = searchText.trim();
      const data = await getAdminUsers(params);
      setUsers(Array.isArray(data) ? data : []);
    } catch (err) {
      setError(err.message || 'Không thể tải danh sách tài khoản');
    } finally {
      setLoading(false);
    }
  }, [filterRole, searchText]);

  useEffect(() => {
    fetchUsers();
  }, [fetchUsers]);

  const handleOpenEdit = (user) => {
    setSelectedUser(user);
    // Nếu role hiện tại thuộc 6 vai trò nghiệp vụ thì giữ nguyên, ngược lại mặc định là WORKER
    const currentRole = user.roleName || 'WORKER';
    const initialRole = BUSINESS_ROLES.some(r => r.role === currentRole) ? currentRole : 'WORKER';
    form.setFieldsValue({
      roleName: initialRole
    });
    setModalOpen(true);
  };

  const handleConfirmEdit = async () => {
    try {
      const values = await form.validateFields();
      setSubmitting(true);
      await updateUserRole(selectedUser.id, values.roleName);
      const roleInfo = ROLE_DISPLAY_MAP[values.roleName];
      message.success(
        `Đã cập nhật vai trò của ${selectedUser.fullName || selectedUser.email} thành ${roleInfo?.name || values.roleName}!`
      );
      setModalOpen(false);
      setSelectedUser(null);
      fetchUsers();
    } catch (err) {
      if (err.errorFields) return;
      message.error(err.message || 'Lỗi khi cập nhật vai trò');
    } finally {
      setSubmitting(false);
    }
  };

  const columns = [
    {
      title: 'Họ và tên',
      dataIndex: 'fullName',
      key: 'fullName',
      render: (name, record) => (
        <Space direction="vertical" size={2}>
          <Text strong>{name || 'Chưa cập nhật'}</Text>
          <Text type="secondary" style={{ fontSize: 12 }}>
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
      title: 'Vai trò hiện tại',
      dataIndex: 'roleName',
      key: 'roleName',
      render: role => {
        const info = ROLE_DISPLAY_MAP[role] || { name: role || 'WORKER', color: 'blue' };
        return (
          <Tag color={info.color} style={{ padding: '2px 10px', fontSize: 13, fontWeight: 500 }}>
            {info.label || role}
          </Tag>
        );
      }
    },
    {
      title: 'Trạng thái tài khoản',
      dataIndex: 'status',
      key: 'status',
      render: status => {
        const color = status === 'ACTIVE' ? 'green' : status === 'PENDING' ? 'gold' : 'red';
        const label = status === 'ACTIVE' ? 'Đang hoạt động' : status === 'PENDING' ? 'Chờ duyệt' : 'Đã khóa / Từ chối';
        return <Tag color={color}>{label}</Tag>;
      }
    },
    {
      title: 'Thao tác',
      key: 'action',
      align: 'center',
      width: 160,
      render: (_, record) => {
        const isSelf = record.email === 'admin@admin.com';
        return (
          <Button
            type="primary"
            ghost
            size="small"
            icon={<EditOutlined />}
            disabled={isSelf}
            onClick={() => handleOpenEdit(record)}
          >
            Đổi vai trò
          </Button>
        );
      }
    }
  ];

  return (
    <div style={{ padding: '4px 0 24px' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 20 }}>
        <div>
          <Title level={3} style={{ marginBottom: 4 }}>Phân quyền và Vai trò (RBAC)</Title>
          <Text type="secondary">Cấu hình vai trò và phân bổ quyền hạn cho từng tài khoản trong hệ thống</Text>
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

      {/* Vai trò Quản trị hệ thống (ADMIN) */}
      <div style={{ marginBottom: 20 }}>
        <h4 style={{ fontSize: 15, fontWeight: 600, color: '#1E293B', marginBottom: 12 }}>
          Vai trò quản trị hệ thống
        </h4>
        <Card
          variant="borderless"
          style={{
            borderRadius: 8,
            boxShadow: '0 1px 3px rgba(0,0,0,0.05)',
            borderTop: `3px solid ${SYSTEM_ROLE.color}`,
            background: '#FEF2F2'
          }}
          bodyStyle={{ padding: 18 }}
        >
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 }}>
            <Tag color={SYSTEM_ROLE.color} style={{ fontWeight: 600, fontSize: 13, padding: '2px 8px' }}>
              {SYSTEM_ROLE.role} - {SYSTEM_ROLE.name}
            </Tag>
            <span style={{ fontSize: 12, color: '#64748B' }}>
              <UserOutlined /> {users.filter(u => u.roleName === SYSTEM_ROLE.role).length} tài khoản quản trị
            </span>
          </div>
          <div style={{ fontSize: 13, color: '#475569', lineHeight: 1.6 }}>
            {SYSTEM_ROLE.description}
          </div>
        </Card>
      </div>

      {/* Ma trận 6 vai trò nghiệp vụ trong hệ thống theo Backlog */}
      <div style={{ marginBottom: 24 }}>
        <h4 style={{ fontSize: 15, fontWeight: 600, color: '#1E293B', marginBottom: 12 }}>
          Ma trận vai trò trong hệ thống (6 vai trò nghiệp vụ theo Backlog)
        </h4>
        <Row gutter={[16, 16]}>
          {BUSINESS_ROLES.map(item => {
            const memberCount = users.filter(u => u.roleName === item.role).length;

            return (
              <Col xs={24} sm={12} lg={8} key={item.role}>
                <Card
                  variant="borderless"
                  style={{
                    borderRadius: 8,
                    boxShadow: '0 1px 3px rgba(0,0,0,0.05)',
                    borderTop: `3px solid ${item.color}`,
                    height: '100%'
                  }}
                  bodyStyle={{ padding: 18 }}
                >
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 }}>
                    <Tag color={item.color} style={{ fontWeight: 600, fontSize: 13, padding: '2px 8px' }}>
                      {item.name}
                    </Tag>
                    <span style={{ fontSize: 12, color: '#64748B' }}>
                      <UserOutlined /> {memberCount} tài khoản
                    </span>
                  </div>
                  <div style={{ fontSize: 12, fontWeight: 500, color: '#64748B', marginBottom: 6 }}>
                    Mã vai trò: <code>{item.role}</code>
                  </div>
                  <div style={{ fontSize: 13, color: '#475569', minHeight: 64, lineHeight: 1.5 }}>
                    {item.description}
                  </div>
                </Card>
              </Col>
            );
          })}
        </Row>
      </div>

      {/* Bảng phân bổ vai trò */}
      <Card
        title={
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <SafetyCertificateOutlined style={{ color: '#2563EB' }} />
            <span>Phân bổ vai trò người dùng ({users.length})</span>
          </div>
        }
        variant="borderless"
        style={{ boxShadow: '0 1px 3px rgba(0,0,0,0.05)', borderRadius: 8 }}
      >
        <div style={{ display: 'flex', gap: 12, flexWrap: 'wrap', marginBottom: 16 }}>
          <Input
            placeholder="Tìm theo họ tên, email..."
            prefix={<SearchOutlined style={{ color: '#94A3B8' }} />}
            style={{ width: 280 }}
            value={searchText}
            onChange={e => setSearchText(e.target.value)}
            allowClear
          />

          <Select
            placeholder="Lọc theo vai trò"
            style={{ width: 280 }}
            allowClear
            value={filterRole}
            onChange={val => setFilterRole(val)}
          >
            <Select.Option value="ADMIN">{SYSTEM_ROLE.label}</Select.Option>
            {BUSINESS_ROLES.map(r => (
              <Select.Option key={r.role} value={r.role}>
                {r.label}
              </Select.Option>
            ))}
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

      {/* Modal Thay đổi vai trò người dùng */}
      <Modal
        title={
          <Space>
            <SafetyCertificateOutlined style={{ color: '#2563EB' }} />
            <span>Thay đổi vai trò người dùng</span>
          </Space>
        }
        open={modalOpen}
        onOk={handleConfirmEdit}
        onCancel={() => setModalOpen(false)}
        confirmLoading={submitting}
        okText="Lưu thay đổi"
        cancelText="Hủy"
      >
        {selectedUser && (
          <Form form={form} layout="vertical">
            <Paragraph>
              Người dùng: <strong>{selectedUser.fullName || selectedUser.email}</strong> ({selectedUser.email})
            </Paragraph>
            <Form.Item
              name="roleName"
              label="Chọn vai trò nghiệp vụ mới"
              rules={[{ required: true, message: 'Vui lòng chọn vai trò nghiệp vụ' }]}
            >
              <Select placeholder="Chọn 1 trong 6 vai trò nghiệp vụ">
                {BUSINESS_ROLES.map(r => (
                  <Select.Option key={r.role} value={r.role}>
                    {r.name} ({r.role})
                  </Select.Option>
                ))}
              </Select>
            </Form.Item>
            <Alert
              type="info"
              showIcon
              message="Lưu ý phân quyền"
              description="Chỉ được phân 1 trong 6 vai trò nghiệp vụ của dự án. Tài khoản quản trị hệ thống (ADMIN) được bảo vệ riêng và không cấp qua luồng này."
            />
          </Form>
        )}
      </Modal>
    </div>
  );
};

export default AdminRolesPage;
