import React, { useEffect, useState, useMemo, useCallback } from 'react';
import { useSearchParams } from 'react-router-dom';
import {
  Card,
  Row,
  Col,
  Table,
  Select,
  Button,
  Space,
  Tag,
  Typography,
  Spin,
  Alert,
  Popconfirm,
  Tooltip,
  Statistic,
  Empty,
  notification
} from 'antd';
import {
  TeamOutlined,
  UserAddOutlined,
  EditOutlined,
  DeleteOutlined,
  ProjectOutlined,
  ReloadOutlined,
  CheckCircleOutlined,
  SafetyCertificateOutlined,
  CrownOutlined,
  UserOutlined
} from '@ant-design/icons';
import { getProjects } from '../services/wbsApi.js';
import {
  getProjectMembers,
  removeProjectMember
} from '../services/projectMemberApi.js';
import { getAuthUser } from '../utils/auth.js';
import { ROLE_DISPLAY_NAMES } from '../utils/permissions.js';
import AddProjectMemberModal from '../components/project/AddProjectMemberModal.jsx';
import UpdateProjectMemberRoleModal from '../components/project/UpdateProjectMemberRoleModal.jsx';
import { useProjectContext } from '../utils/ProjectContext.jsx';

const { Title, Text } = Typography;

const ROLE_COLORS = {
  ADMIN: 'red',
  PROJECT_MANAGER: 'blue',
  CUSTOMER: 'cyan',
  SITE_MANAGER: 'gold',
  SITE_ENGINEER: 'geekblue',
  WORKER: 'green',
  STAFF: 'purple',
  VIEWER: 'default'
};

export default function ProjectMembersPage() {
  const authUser = getAuthUser();
  const { refreshMemberships } = useProjectContext();
  const [searchParams, setSearchParams] = useSearchParams();

  const [loadingProjects, setLoadingProjects] = useState(true);
  const [projects, setProjects] = useState([]);
  const [selectedProjectId, setSelectedProjectId] = useState(null);

  const [loadingMembers, setLoadingMembers] = useState(false);
  const [members, setMembers] = useState([]);
  const [error, setError] = useState('');

  const [addModalOpen, setAddModalOpen] = useState(false);
  const [editMember, setEditMember] = useState(null);
  const [deletingUserId, setDeletingUserId] = useState(null);

  // 1. Fetch accessible projects
  const fetchProjects = useCallback(async () => {
    setLoadingProjects(true);
    setError('');
    try {
      const data = await getProjects();
      const projectList = Array.isArray(data) ? data : [];
      setProjects(projectList);

      const queryProjectId = searchParams.get('projectId');
      const foundQueryProject = projectList.find(p => String(p.id) === queryProjectId);

      if (foundQueryProject) {
        setSelectedProjectId(foundQueryProject.id);
      } else if (projectList.length > 0) {
        setSelectedProjectId(projectList[0].id);
      }
    } catch (err) {
      setError(err.message || 'Không thể tải danh sách dự án');
    } finally {
      setLoadingProjects(false);
    }
  }, [searchParams]);

  useEffect(() => {
    fetchProjects();
  }, [fetchProjects]);

  // Selected project object
  const currentProject = useMemo(() => {
    return projects.find(p => p.id === selectedProjectId) || null;
  }, [projects, selectedProjectId]);

  // 2. Fetch project members when selectedProjectId changes
  const fetchMembers = useCallback(async (projectId) => {
    if (!projectId) {
      setMembers([]);
      return;
    }
    setLoadingMembers(true);
    setError('');
    try {
      const data = await getProjectMembers(projectId);
      setMembers(Array.isArray(data) ? data : []);
    } catch (err) {
      setError(err.message || 'Không thể tải danh sách thành viên dự án');
      setMembers([]);
    } finally {
      setLoadingMembers(false);
    }
  }, []);

  useEffect(() => {
    if (selectedProjectId) {
      fetchMembers(selectedProjectId);
    }
  }, [selectedProjectId, fetchMembers]);

  // Handle project selector change
  const handleSelectProject = (projectId) => {
    setSelectedProjectId(projectId);
    setSearchParams({ projectId });
  };

  // Handle member removal
  const handleRemoveMember = async (member) => {
    if (!selectedProjectId || !member?.userId) return;

    setDeletingUserId(member.userId);
    try {
      await removeProjectMember(selectedProjectId, member.userId);
      notification.success({
        message: 'Thành công',
        description: `Đã xóa thành viên "${member.fullName || member.email}" khỏi dự án.`
      });
      fetchMembers(selectedProjectId);
    } catch (err) {
      notification.error({
        message: 'Lỗi',
        description: err.message || 'Không thể xóa thành viên khỏi dự án'
      });
    } finally {
      setDeletingUserId(null);
    }
  };

  // Format date helper
  const formatDate = (dateStr) => {
    if (!dateStr) return '-';
    try {
      const d = new Date(dateStr);
      return d.toLocaleDateString('vi-VN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
      });
    } catch {
      return dateStr;
    }
  };

  // Summary statistics
  const stats = useMemo(() => {
    const total = members.length;
    const byRole = {};
    members.forEach(m => {
      const r = m.roleCode || m.roleName || 'OTHER';
      byRole[r] = (byRole[r] || 0) + 1;
    });
    return { total, byRole };
  }, [members]);

  const columns = [
    {
      title: 'Họ và tên',
      dataIndex: 'fullName',
      key: 'fullName',
      render: (text, record) => (
        <Space>
          <div style={{
            width: 32,
            height: 32,
            borderRadius: '50%',
            background: '#EFF6FF',
            color: '#2563EB',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            fontWeight: 600,
            fontSize: 14
          }}>
            {(text || record.email || 'U').charAt(0).toUpperCase()}
          </div>
          <div>
            <div style={{ fontWeight: 600, color: '#0F172A' }}>
              {text || record.email}
            </div>
            {record.username && record.username !== record.email && (
              <div style={{ fontSize: 12, color: '#64748B' }}>
                @{record.username}
              </div>
            )}
          </div>
        </Space>
      )
    },
    {
      title: 'Email',
      dataIndex: 'email',
      key: 'email',
      render: (email) => (
        <Text copyable style={{ color: '#334155' }}>
          {email}
        </Text>
      )
    },
    {
      title: 'Vai trò trong dự án',
      dataIndex: 'roleCode',
      key: 'roleCode',
      render: (roleCode, record) => {
        const code = roleCode || record.roleName || '';
        const color = ROLE_COLORS[code] || 'blue';
        const label = ROLE_DISPLAY_NAMES[code] || code || 'Thành viên';
        return (
          <Tag color={color} style={{ fontSize: 13, padding: '3px 10px', borderRadius: 4 }}>
            {label}
          </Tag>
        );
      }
    },
    {
      title: 'Trạng thái',
      dataIndex: 'status',
      key: 'status',
      render: (status) => {
        const isActive = 'ACTIVE'.equalsIgnoreCase ? 'ACTIVE'.equalsIgnoreCase(status) : String(status).toUpperCase() === 'ACTIVE';
        return isActive ? (
          <Tag icon={<CheckCircleOutlined />} color="success" style={{ fontSize: 12 }}>
            Hoạt động
          </Tag>
        ) : (
          <Tag color="default" style={{ fontSize: 12 }}>
            {status || 'Không xác định'}
          </Tag>
        );
      }
    },
    {
      title: 'Ngày tham gia',
      dataIndex: 'joinedAt',
      key: 'joinedAt',
      render: (val) => (
        <span style={{ color: '#64748B', fontSize: 13 }}>
          {formatDate(val)}
        </span>
      )
    },
    {
      title: 'Thao tác',
      key: 'actions',
      align: 'center',
      width: 220,
      render: (_, record) => {
        const isSelf = authUser?.id && String(authUser.id) === String(record.userId);
        const isAdminMember = String(record.roleCode || record.roleName || '').toUpperCase() === 'ADMIN';

        return (
          <Space orientation="horizontal" size={8}>
            <Tooltip title={isAdminMember ? 'Không thể đổi vai trò Quản trị hệ thống' : 'Đổi vai trò trong dự án này'}>
              <Button
                type="primary"
                ghost
                size="small"
                icon={<EditOutlined />}
                disabled={isAdminMember}
                onClick={() => setEditMember(record)}
              >
                Đổi vai trò
              </Button>
            </Tooltip>

            <Tooltip
              title={
                isAdminMember
                  ? 'Không thể xóa Quản trị hệ thống'
                  : isSelf
                  ? 'Không thể tự xóa chính mình khỏi dự án'
                  : 'Xóa thành viên khỏi dự án'
              }
            >
              <Popconfirm
                title="Xóa thành viên khỏi dự án?"
                description={
                  <div style={{ maxWidth: 300 }}>
                    Bạn có chắc chắn muốn xóa thành viên <strong>{record.fullName || record.email}</strong> khỏi dự án này không?
                    <br />
                    <span style={{ fontSize: 12, color: '#64748B', marginTop: 4, display: 'block' }}>
                      (Lưu ý: Thao tác này chỉ hủy phân quyền trong dự án, tài khoản người dùng vẫn được bảo lưu).
                    </span>
                  </div>
                }
                okText="Xóa thành viên"
                cancelText="Hủy"
                okButtonProps={{ danger: true, loading: deletingUserId === record.userId }}
                disabled={isAdminMember || isSelf}
                onConfirm={() => handleRemoveMember(record)}
              >
                <Button
                  danger
                  size="small"
                  icon={<DeleteOutlined />}
                  disabled={isAdminMember || isSelf}
                  loading={deletingUserId === record.userId}
                >
                  Xóa
                </Button>
              </Popconfirm>
            </Tooltip>
          </Space>
        );
      }
    }
  ];

  return (
    <div style={{ padding: '24px', maxWidth: 1400, margin: '0 auto' }}>
      {/* 1. Page Header */}
      <div style={{ marginBottom: 20 }}>
        <Row justify="space-between" align="middle" gutter={[16, 16]}>
          <Col xs={24} md={14}>
            <Space align="center" size={12}>
              <div style={{
                width: 44,
                height: 44,
                borderRadius: 10,
                background: '#EFF6FF',
                color: '#2563EB',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                fontSize: 22
              }}>
                <TeamOutlined />
              </div>
              <div>
                <Title level={3} style={{ margin: 0, color: '#0F172A', fontWeight: 700 }}>
                  QUẢN LÝ THÀNH VIÊN DỰ ÁN
                </Title>
                <Text type="secondary" style={{ fontSize: 14 }}>
                  Phân công vai trò và quản lý thành viên cho từng dự án do Ban quản lý điều hành
                </Text>
              </div>
            </Space>
          </Col>

          {/* Project Selector Dropdown */}
          <Col xs={24} md={10} style={{ textAlign: 'right' }}>
            <Space align="center" size={10} style={{ width: '100%', justifyContent: 'flex-end' }}>
              <Text strong style={{ fontSize: 14, color: '#334155', whiteSpace: 'nowrap' }}>
                Dự án:
              </Text>
              <Select
                loading={loadingProjects}
                value={selectedProjectId}
                onChange={handleSelectProject}
                placeholder="Chọn dự án..."
                style={{ minWidth: 280, maxWidth: 400, textAlign: 'left' }}
                options={projects.map(p => ({
                  value: p.id,
                  label: (
                    <Space>
                      <ProjectOutlined style={{ color: '#2563EB' }} />
                      <span style={{ fontWeight: 600 }}>{p.code}</span>
                      <span>-</span>
                      <span>{p.name}</span>
                    </Space>
                  )
                }))}
              />
              <Button
                icon={<ReloadOutlined />}
                onClick={() => {
                  fetchProjects();
                  if (selectedProjectId) fetchMembers(selectedProjectId);
                }}
              />
            </Space>
          </Col>
        </Row>
      </div>

      {error && (
        <Alert
          type="error"
          message={error}
          showIcon
          style={{ marginBottom: 20 }}
        />
      )}

      {/* 2. Project Card Banner */}
      {currentProject && (
        <Card
          style={{
            marginBottom: 20,
            borderRadius: 10,
            border: '1px solid #E2E8F0',
            boxShadow: '0 1px 3px rgba(0,0,0,0.05)'
          }}
          bodyStyle={{ padding: '16px 20px' }}
        >
          <Row justify="space-between" align="middle" gutter={[16, 16]}>
            <Col xs={24} md={16}>
              <Space direction="vertical" size={4}>
                <Space align="center">
                  <Tag color="#2563EB" style={{ fontSize: 13, fontWeight: 700, padding: '2px 8px' }}>
                    {currentProject.code}
                  </Tag>
                  <Title level={4} style={{ margin: 0, color: '#0F172A' }}>
                    {currentProject.name}
                  </Title>
                </Space>
                {currentProject.description && (
                  <Text type="secondary" style={{ fontSize: 13 }}>
                    {currentProject.description}
                  </Text>
                )}
              </Space>
            </Col>

            <Col xs={24} md={8} style={{ textAlign: 'right' }}>
              <Button
                type="primary"
                size="large"
                icon={<UserAddOutlined />}
                onClick={() => setAddModalOpen(true)}
                style={{
                  background: '#2563EB',
                  borderColor: '#2563EB',
                  fontWeight: 600,
                  boxShadow: '0 2px 4px rgba(37,99,235,0.2)'
                }}
              >
                + Thêm thành viên
              </Button>
            </Col>
          </Row>
        </Card>
      )}

      {/* 3. Summary Statistics */}
      {currentProject && (
        <Row gutter={[16, 16]} style={{ marginBottom: 20 }}>
          <Col xs={12} sm={8} md={4}>
            <Card size="small" style={{ borderRadius: 8, borderColor: '#E2E8F0' }}>
              <Statistic
                title="Tổng thành viên"
                value={stats.total}
                valueStyle={{ color: '#2563EB', fontWeight: 700 }}
                prefix={<TeamOutlined />}
              />
            </Card>
          </Col>
          <Col xs={12} sm={8} md={4}>
            <Card size="small" style={{ borderRadius: 8, borderColor: '#E2E8F0' }}>
              <Statistic
                title="Chủ đầu tư"
                value={stats.byRole.CUSTOMER || 0}
                valueStyle={{ color: '#0891B2', fontWeight: 700 }}
              />
            </Card>
          </Col>
          <Col xs={12} sm={8} md={4}>
            <Card size="small" style={{ borderRadius: 8, borderColor: '#E2E8F0' }}>
              <Statistic
                title="Chỉ huy trưởng"
                value={stats.byRole.SITE_MANAGER || 0}
                valueStyle={{ color: '#D97706', fontWeight: 700 }}
              />
            </Card>
          </Col>
          <Col xs={12} sm={8} md={4}>
            <Card size="small" style={{ borderRadius: 8, borderColor: '#E2E8F0' }}>
              <Statistic
                title="Kỹ sư giám sát"
                value={stats.byRole.SITE_ENGINEER || 0}
                valueStyle={{ color: '#1D4ED8', fontWeight: 700 }}
              />
            </Card>
          </Col>
          <Col xs={12} sm={8} md={4}>
            <Card size="small" style={{ borderRadius: 8, borderColor: '#E2E8F0' }}>
              <Statistic
                title="Đội trưởng thi công"
                value={stats.byRole.WORKER || 0}
                valueStyle={{ color: '#16A34A', fontWeight: 700 }}
              />
            </Card>
          </Col>
          <Col xs={12} sm={8} md={4}>
            <Card size="small" style={{ borderRadius: 8, borderColor: '#E2E8F0' }}>
              <Statistic
                title="Kế toán CĐT"
                value={stats.byRole.STAFF || 0}
                valueStyle={{ color: '#7C3AED', fontWeight: 700 }}
              />
            </Card>
          </Col>
        </Row>
      )}

      {/* 4. Members Table */}
      <Card
        style={{
          borderRadius: 10,
          border: '1px solid #E2E8F0',
          boxShadow: '0 1px 3px rgba(0,0,0,0.05)'
        }}
        bodyStyle={{ padding: 0 }}
      >
        <div style={{
          padding: '16px 20px',
          borderBottom: '1px solid #F1F5F9',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center'
        }}>
          <Space>
            <Text strong style={{ fontSize: 16, color: '#0F172A' }}>
              Danh sách thành viên
            </Text>
            <Tag color="blue">{members.length} người</Tag>
          </Space>
          <Button
            size="small"
            icon={<ReloadOutlined />}
            onClick={() => selectedProjectId && fetchMembers(selectedProjectId)}
            loading={loadingMembers}
          >
            Làm mới
          </Button>
        </div>

        <Table
          columns={columns}
          dataSource={members}
          rowKey={(record) => record.id || record.userId}
          loading={loadingMembers}
          pagination={{
            pageSize: 10,
            showTotal: (total, range) => `Hiển thị ${range[0]}-${range[1]} trên tổng số ${total} thành viên`,
            style: { padding: '16px 20px' }
          }}
          locale={{
            emptyText: (
              <Empty
                image={Empty.PRESENTED_IMAGE_SIMPLE}
                description="Dự án chưa có thành viên nào. Bấm '+ Thêm thành viên' để gán vai trò cho thành viên mới."
              />
            )
          }}
        />
      </Card>

      {/* 5. Modals */}
      <AddProjectMemberModal
        open={addModalOpen}
        project={currentProject}
        onClose={() => setAddModalOpen(false)}
        onSuccess={() => {
          if (selectedProjectId) fetchMembers(selectedProjectId);
          refreshMemberships();
        }}
      />

      <UpdateProjectMemberRoleModal
        open={Boolean(editMember)}
        project={currentProject}
        member={editMember}
        onClose={() => setEditMember(null)}
        onSuccess={() => {
          if (selectedProjectId) fetchMembers(selectedProjectId);
          refreshMemberships();
        }}
      />
    </div>
  );
}
