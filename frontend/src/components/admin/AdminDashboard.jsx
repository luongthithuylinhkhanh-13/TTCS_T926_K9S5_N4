import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Card,
  Row,
  Col,
  Statistic,
  Table,
  Tag,
  Button,
  Spin,
  Alert,
  Empty,
  Typography,
  Space,
  Badge,
  Modal,
  Form,
  Select,
  Input,
  message
} from 'antd';
import {
  UserOutlined,
  ClockCircleOutlined,
  CheckCircleOutlined,
  StopOutlined,
  ProjectOutlined,
  HeartOutlined,
  ArrowRightOutlined,
  SafetyCertificateOutlined,
  SettingOutlined,
  CheckOutlined,
  CloseOutlined
} from '@ant-design/icons';
import { getAdminDashboardStats, approveUser, rejectUser, getAdminRoles } from '../../services/adminApi';

const { Title, Text } = Typography;
const { TextArea } = Input;

const AdminDashboard = () => {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [stats, setStats] = useState({
    totalUsers: 0,
    pendingUsers: 0,
    activeUsers: 0,
    rejectedUsers: 0,
    totalProjects: 0,
    systemStatus: 'HEALTHY',
    recentPendingUsers: []
  });

  // Quick Action Modals
  const [roles, setRoles] = useState([]);
  const [approveModalOpen, setApproveModalOpen] = useState(false);
  const [rejectModalOpen, setRejectModalOpen] = useState(false);
  const [targetUser, setTargetUser] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const [approveForm] = Form.useForm();
  const [rejectForm] = Form.useForm();

  const fetchStats = async () => {
    setLoading(true);
    setError('');
    try {
      const data = await getAdminDashboardStats();
      setStats(data || {});
    } catch (err) {
      setError(err.message || 'Không thể tải số liệu bảng điều khiển quản trị');
    } finally {
      setLoading(false);
    }
  };

  const fetchRoles = async () => {
    try {
      const roleList = await getAdminRoles();
      if (Array.isArray(roleList)) setRoles(roleList);
    } catch (err) {
      console.error('Error fetching roles:', err);
    }
  };

  useEffect(() => {
    fetchStats();
    fetchRoles();
  }, []);

  const handleOpenApprove = (user) => {
    setTargetUser(user);
    approveForm.setFieldsValue({ roleName: user.roleName || 'WORKER' });
    setApproveModalOpen(true);
  };

  const handleConfirmApprove = async () => {
    try {
      const values = await approveForm.validateFields();
      setSubmitting(true);
      await approveUser(targetUser.id, values.roleName);
      message.success(`Đã duyệt tài khoản ${targetUser.fullName || targetUser.email}!`);
      setApproveModalOpen(false);
      setTargetUser(null);
      fetchStats();
    } catch (err) {
      if (err.errorFields) return;
      message.error(err.message || 'Lỗi khi duyệt');
    } finally {
      setSubmitting(false);
    }
  };

  const handleOpenReject = (user) => {
    setTargetUser(user);
    rejectForm.resetFields();
    setRejectModalOpen(true);
  };

  const handleConfirmReject = async () => {
    try {
      const values = await rejectForm.validateFields();
      setSubmitting(true);
      await rejectUser(targetUser.id, values.reason);
      message.success(`Đã từ chối tài khoản ${targetUser.fullName || targetUser.email}!`);
      setRejectModalOpen(false);
      setTargetUser(null);
      fetchStats();
    } catch (err) {
      if (err.errorFields) return;
      message.error(err.message || 'Lỗi khi từ chối');
    } finally {
      setSubmitting(false);
    }
  };

  const pendingColumns = [
    {
      title: 'Họ và tên',
      dataIndex: 'fullName',
      key: 'fullName',
      render: (name, record) => (
        <div>
          <Text strong>{name || 'Chưa cập nhật'}</Text>
          <div>
            <Text type="secondary" style={{ fontSize: 12 }}>{record.email}</Text>
          </div>
        </div>
      )
    },
    {
      title: 'Vai trò đề xuất',
      dataIndex: 'roleName',
      key: 'roleName',
      render: role => <Tag color="blue">{role || 'CUSTOMER'}</Tag>
    },
    {
      title: 'Ngày đăng ký',
      dataIndex: 'createdAt',
      key: 'createdAt',
      render: d => (d ? new Date(d).toLocaleDateString('vi-VN') : '--')
    },
    {
      title: 'Trạng thái',
      dataIndex: 'status',
      key: 'status',
      render: () => <Tag color="gold">Chờ duyệt</Tag>
    },
    {
      title: 'Hành động nhanh',
      key: 'actions',
      align: 'center',
      width: 180,
      render: (_, record) => (
        <Space>
          <Button
            type="primary"
            size="small"
            icon={<CheckOutlined />}
            style={{ backgroundColor: '#10B981', borderColor: '#10B981' }}
            onClick={() => handleOpenApprove(record)}
          >
            Duyệt
          </Button>
          <Button
            danger
            size="small"
            icon={<CloseOutlined />}
            onClick={() => handleOpenReject(record)}
          >
            Từ chối
          </Button>
        </Space>
      )
    }
  ];

  return (
    <div className="admin-dashboard" style={{ padding: '4px 0 24px' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 20 }}>
        <div>
          <Title level={3} style={{ marginBottom: 4 }}>Bảng điều khiển Quản trị viên</Title>
          <Text type="secondary">
            Giám sát tài khoản người dùng, hàng chờ phê duyệt và tình trạng vận hành hệ thống
          </Text>
        </div>
        <Button
          type="primary"
          ghost
          icon={<HeartOutlined />}
          onClick={() => navigate('/admin/system-health')}
        >
          Sức khỏe hệ thống
        </Button>
      </div>

      {error && (
        <Alert
          type="error"
          showIcon
          message="Lỗi tải dữ liệu"
          description={error}
          style={{ marginBottom: 20 }}
          closable
          onClose={() => setError('')}
        />
      )}

      <Spin spinning={loading}>
        {/* KPI Row */}
        <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
          <Col xs={24} sm={12} md={8} lg={4}>
            <Card variant="borderless" style={{ boxShadow: '0 1px 3px rgba(0,0,0,0.05)', borderRadius: 8 }}>
              <Statistic
                title="Tổng tài khoản"
                value={stats.totalUsers || 0}
                prefix={<UserOutlined style={{ color: '#2563EB' }} />}
              />
            </Card>
          </Col>

          <Col xs={24} sm={12} md={8} lg={4}>
            <Card
              variant="borderless"
              style={{
                boxShadow: '0 1px 3px rgba(0,0,0,0.05)',
                borderRadius: 8,
                borderLeft: (stats.pendingUsers || 0) > 0 ? '4px solid #F59E0B' : undefined
              }}
            >
              <Statistic
                title="Chờ duyệt"
                value={stats.pendingUsers || 0}
                prefix={<ClockCircleOutlined style={{ color: '#F59E0B' }} />}
                valueStyle={{ color: '#F59E0B', fontWeight: 700 }}
              />
            </Card>
          </Col>

          <Col xs={24} sm={12} md={8} lg={4}>
            <Card variant="borderless" style={{ boxShadow: '0 1px 3px rgba(0,0,0,0.05)', borderRadius: 8 }}>
              <Statistic
                title="Đã hoạt động"
                value={stats.activeUsers || 0}
                prefix={<CheckCircleOutlined style={{ color: '#10B981' }} />}
                valueStyle={{ color: '#10B981' }}
              />
            </Card>
          </Col>

          <Col xs={24} sm={12} md={8} lg={4}>
            <Card variant="borderless" style={{ boxShadow: '0 1px 3px rgba(0,0,0,0.05)', borderRadius: 8 }}>
              <Statistic
                title="Khóa / Từ chối"
                value={stats.rejectedUsers || 0}
                prefix={<StopOutlined style={{ color: '#EF4444' }} />}
                valueStyle={{ color: '#64748B' }}
              />
            </Card>
          </Col>

          <Col xs={24} sm={12} md={8} lg={4}>
            <Card variant="borderless" style={{ boxShadow: '0 1px 3px rgba(0,0,0,0.05)', borderRadius: 8 }}>
              <Statistic
                title="Tổng dự án"
                value={stats.totalProjects || 0}
                prefix={<ProjectOutlined style={{ color: '#6366F1' }} />}
              />
            </Card>
          </Col>

          <Col xs={24} sm={12} md={8} lg={4}>
            <Card variant="borderless" style={{ boxShadow: '0 1px 3px rgba(0,0,0,0.05)', borderRadius: 8 }}>
              <Statistic
                title="Trạng thái hệ thống"
                value={stats.systemStatus === 'HEALTHY' ? 'ỔN ĐỊNH' : 'CẢNH BÁO'}
                prefix={<HeartOutlined style={{ color: stats.systemStatus === 'HEALTHY' ? '#10B981' : '#EF4444' }} />}
                valueStyle={{
                  color: stats.systemStatus === 'HEALTHY' ? '#10B981' : '#EF4444',
                  fontSize: 18,
                  fontWeight: 600
                }}
              />
            </Card>
          </Col>
        </Row>

        {/* Quick Access Action Cards */}
        <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
          <Col xs={24} sm={12} md={6}>
            <Card
              hoverable
              onClick={() => navigate('/admin/approvals')}
              style={{ borderRadius: 8, boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}
            >
              <div style={{ display: 'flex', alignItems: 'center', gap: 14 }}>
                <div style={{
                  width: 44,
                  height: 44,
                  borderRadius: 8,
                  backgroundColor: '#FEF3C7',
                  color: '#D97706',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  fontSize: 20
                }}>
                  <ClockCircleOutlined />
                </div>
                <div>
                  <div style={{ fontWeight: 600, fontSize: 15 }}>Duyệt thành viên</div>
                  <div style={{ fontSize: 12, color: '#64748B' }}>
                    {stats.pendingUsers > 0 ? `${stats.pendingUsers} tài khoản đang chờ` : 'Không có yêu cầu chờ'}
                  </div>
                </div>
              </div>
            </Card>
          </Col>

          <Col xs={24} sm={12} md={6}>
            <Card
              hoverable
              onClick={() => navigate('/admin/users')}
              style={{ borderRadius: 8, boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}
            >
              <div style={{ display: 'flex', alignItems: 'center', gap: 14 }}>
                <div style={{
                  width: 44,
                  height: 44,
                  borderRadius: 8,
                  backgroundColor: '#DBEAFE',
                  color: '#2563EB',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  fontSize: 20
                }}>
                  <UserOutlined />
                </div>
                <div>
                  <div style={{ fontWeight: 600, fontSize: 15 }}>Quản lý người dùng</div>
                  <div style={{ fontSize: 12, color: '#64748B' }}>Xem và phân loại tài khoản</div>
                </div>
              </div>
            </Card>
          </Col>

          <Col xs={24} sm={12} md={6}>
            <Card
              hoverable
              onClick={() => navigate('/admin/roles')}
              style={{ borderRadius: 8, boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}
            >
              <div style={{ display: 'flex', alignItems: 'center', gap: 14 }}>
                <div style={{
                  width: 44,
                  height: 44,
                  borderRadius: 8,
                  backgroundColor: '#E0E7FF',
                  color: '#4F46E5',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  fontSize: 20
                }}>
                  <SafetyCertificateOutlined />
                </div>
                <div>
                  <div style={{ fontWeight: 600, fontSize: 15 }}>Phân quyền vai trò</div>
                  <div style={{ fontSize: 12, color: '#64748B' }}>Cấu hình RBAC hệ thống</div>
                </div>
              </div>
            </Card>
          </Col>

          <Col xs={24} sm={12} md={6}>
            <Card
              hoverable
              onClick={() => navigate('/admin/system-health')}
              style={{ borderRadius: 8, boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}
            >
              <div style={{ display: 'flex', alignItems: 'center', gap: 14 }}>
                <div style={{
                  width: 44,
                  height: 44,
                  borderRadius: 8,
                  backgroundColor: '#D1FAE5',
                  color: '#059669',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  fontSize: 20
                }}>
                  <SettingOutlined />
                </div>
                <div>
                  <div style={{ fontWeight: 600, fontSize: 15 }}>Sức khỏe hệ thống</div>
                  <div style={{ fontSize: 12, color: '#64748B' }}>Kiểm tra DB, Mail, App</div>
                </div>
              </div>
            </Card>
          </Col>
        </Row>

        {/* Pending Users Queue Table */}
        <Card
          title={
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                <ClockCircleOutlined style={{ color: '#F59E0B' }} />
                <span>Yêu cầu đăng ký chờ phê duyệt</span>
                {stats.pendingUsers > 0 && <Badge count={stats.pendingUsers} />}
              </div>
              <Button
                type="link"
                icon={<ArrowRightOutlined />}
                onClick={() => navigate('/admin/approvals')}
              >
                Xem tất cả ({stats.pendingUsers || 0})
              </Button>
            </div>
          }
          variant="borderless"
          style={{ boxShadow: '0 1px 3px rgba(0,0,0,0.05)', borderRadius: 8 }}
        >
          <Table
            dataSource={stats.recentPendingUsers || []}
            columns={pendingColumns}
            rowKey="id"
            pagination={false}
            locale={{ emptyText: <Empty description="Hiện tại không có yêu cầu nào đang chờ phê duyệt" /> }}
          />
        </Card>
      </Spin>

      {/* Approve Modal */}
      <Modal
        title="Phê duyệt nhanh tài khoản"
        open={approveModalOpen}
        onOk={handleConfirmApprove}
        onCancel={() => setApproveModalOpen(false)}
        confirmLoading={submitting}
        okText="Xác nhận duyệt"
        cancelText="Hủy"
        okButtonProps={{ style: { backgroundColor: '#10B981', borderColor: '#10B981' } }}
      >
        {targetUser && (
          <Form form={approveForm} layout="vertical">
            <p>Phê duyệt tài khoản: <strong>{targetUser.fullName || targetUser.email}</strong></p>
            <Form.Item name="roleName" label="Chỉ định vai trò">
              <Select>
                {['WORKER', 'PROJECT_MANAGER', 'ADMIN'].map(r => (
                  <Select.Option key={r} value={r}>
                    {r === 'ADMIN' ? 'ADMIN - Quản trị viên' : r === 'PROJECT_MANAGER' ? 'PROJECT_MANAGER - Chỉ huy trưởng' : 'WORKER - Nhân viên'}
                  </Select.Option>
                ))}
              </Select>
            </Form.Item>
          </Form>
        )}
      </Modal>

      {/* Reject Modal */}
      <Modal
        title="Từ chối yêu cầu tài khoản"
        open={rejectModalOpen}
        onOk={handleConfirmReject}
        onCancel={() => setRejectModalOpen(false)}
        confirmLoading={submitting}
        okText="Từ chối"
        cancelText="Hủy"
        okButtonProps={{ danger: true }}
      >
        {targetUser && (
          <Form form={rejectForm} layout="vertical">
            <p>Từ chối tài khoản: <strong>{targetUser.fullName || targetUser.email}</strong></p>
            <Form.Item
              name="reason"
              label="Lý do từ chối"
              rules={[{ required: true, message: 'Vui lòng nhập lý do từ chối' }]}
            >
              <TextArea rows={3} placeholder="Lý do từ chối..." />
            </Form.Item>
          </Form>
        )}
      </Modal>
    </div>
  );
};

export default AdminDashboard;
