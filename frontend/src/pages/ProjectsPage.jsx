import React, { useEffect, useState, useMemo, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Card,
  Row,
  Col,
  Statistic,
  Table,
  Input,
  Select,
  Tag,
  Progress,
  Button,
  Spin,
  Alert,
  Space,
  Tooltip
} from 'antd';
import {
  ProjectOutlined,
  PlayCircleOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
  SearchOutlined,
  NodeIndexOutlined,
  HistoryOutlined,
  EnvironmentOutlined,
  UserSwitchOutlined
} from '@ant-design/icons';
import { getProjects } from '../services/wbsApi';
import { getAuthUser } from '../utils/auth';
import ProjectSiteLocationModal from '../components/project/ProjectSiteLocationModal.jsx';
import AssignProjectManagerModal from '../components/project/AssignProjectManagerModal.jsx';

const ProjectsPage = () => {
  const navigate = useNavigate();
  const authUser = getAuthUser();
  const isAdmin = String(authUser?.roleName || authUser?.role || '').toUpperCase() === 'ADMIN';

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [projects, setProjects] = useState([]);
  const [searchKeyword, setSearchKeyword] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [locationModalProject, setLocationModalProject] = useState(null);
  const [assignManagerProject, setAssignManagerProject] = useState(null);

  const fetchProjects = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const data = await getProjects();
      setProjects(Array.isArray(data) ? data : []);
    } catch (err) {
      setError(err.message || 'Không thể tải danh sách dự án');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchProjects();
  }, [fetchProjects]);

  const filteredProjects = useMemo(() => {
    return projects.filter(project => {
      const matchKeyword =
        !searchKeyword ||
        project.name?.toLowerCase().includes(searchKeyword.toLowerCase()) ||
        project.code?.toLowerCase().includes(searchKeyword.toLowerCase());

      const matchStatus =
        statusFilter === 'ALL' ||
        project.status?.toLowerCase() === statusFilter.toLowerCase();

      return matchKeyword && matchStatus;
    });
  }, [projects, searchKeyword, statusFilter]);

  const stats = useMemo(() => {
    const total = projects.length;
    const inProgress = projects.filter(
      p => p.status === 'in_progress' || p.status === 'active'
    ).length;
    const completed = projects.filter(p => p.status === 'completed').length;
    const notStarted = projects.filter(
      p => p.status === 'not_started' || (!p.status && p.progress === 0)
    ).length;

    return { total, inProgress, completed, notStarted };
  }, [projects]);

  const getStatusTag = (status) => {
    switch (status) {
      case 'in_progress':
      case 'active':
        return <Tag color="processing">Đang thi công</Tag>;
      case 'completed':
        return <Tag color="success">Hoàn thành</Tag>;
      case 'not_started':
      default:
        return <Tag color="default">Chưa khởi công</Tag>;
    }
  };

  const columns = [
    {
      title: 'Mã dự án',
      dataIndex: 'code',
      key: 'code',
      width: 120,
      render: code => (
        <span style={{ fontWeight: 700, color: '#2563EB' }}>{code}</span>
      )
    },
    {
      title: 'Tên dự án',
      dataIndex: 'name',
      key: 'name',
      width: 250,
      render: name => <strong>{name}</strong>
    },
    {
      title: 'Mô tả',
      dataIndex: 'description',
      key: 'description',
      ellipsis: true,
      render: text => text || <span style={{ color: '#94A3B8' }}>--</span>
    },
    {
      title: 'Ngày bắt đầu',
      dataIndex: 'startDate',
      key: 'startDate',
      width: 130,
      render: d => d || '--'
    },
    {
      title: 'Ngày kết thúc',
      dataIndex: 'endDate',
      key: 'endDate',
      width: 130,
      render: d => d || '--'
    },
    {
      title: 'Tiến độ',
      dataIndex: 'progress',
      key: 'progress',
      width: 160,
      render: p => (
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <Progress percent={p || 0} size="small" style={{ flex: 1 }} />
        </div>
      )
    },
    {
      title: 'Trạng thái',
      dataIndex: 'status',
      key: 'status',
      width: 140,
      render: status => getStatusTag(status)
    },
    {
      title: 'Vị trí công trường',
      key: 'siteLocation',
      width: 170,
      render: (_, record) => {
        const isConfigured =
          record.siteLatitude != null &&
          record.siteLongitude != null &&
          record.siteRadiusMeters != null;
        return isConfigured ? (
          <Tag color="success" icon={<EnvironmentOutlined />}>
            R = {record.siteRadiusMeters}m
          </Tag>
        ) : (
          <Tag color="default">Chưa thiết lập</Tag>
        );
      }
    },
    {
      title: 'Thao tác',
      key: 'action',
      width: isAdmin ? 360 : 250,
      align: 'center',
      render: (_, record) => (
        <Space size="small">
          {isAdmin && (
            <Tooltip title="Phân Ban quản lý dự án">
              <Button
                size="small"
                type="dashed"
                icon={<UserSwitchOutlined />}
                onClick={() => setAssignManagerProject(record)}
                style={{ borderColor: '#6366F1', color: '#6366F1' }}
              >
                Phân quản lý
              </Button>
            </Tooltip>
          )}
          <Button
            size="small"
            icon={<NodeIndexOutlined />}
            onClick={() => navigate(`/wbs?projectId=${record.id}`)}
          >
            WBS
          </Button>
          <Button
            size="small"
            type="primary"
            ghost
            icon={<HistoryOutlined />}
            onClick={() => navigate(`/progress?projectId=${record.id}`)}
          >
            Tiến độ
          </Button>
          <Button
            size="small"
            icon={<EnvironmentOutlined />}
            onClick={() => setLocationModalProject(record)}
          >
            Vị trí
          </Button>
        </Space>
      )
    }
  ];

  return (
    <div className="projects-page" style={{ padding: '4px 0 24px' }}>
      <div className="page-header" style={{ marginBottom: 20 }}>
        <div>
          <h1 className="page-title">Quản lý Dự án</h1>
          <p className="page-subtitle">Danh sách các công trình và trạng thái thi công tổng thể</p>
        </div>
      </div>

      {error && (
        <Alert
          type="error"
          showIcon
          closable
          message="Lỗi tải danh sách dự án"
          description={error}
          style={{ marginBottom: 20 }}
          onClose={() => setError('')}
        />
      )}

      {/* Summary KPI Cards */}
      <Row gutter={[16, 16]} style={{ marginBottom: 20 }}>
        <Col xs={24} sm={12} md={6}>
          <Card variant="borderless" style={{ borderRadius: 8, boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}>
            <Statistic
              title="Tổng số dự án"
              value={stats.total}
              prefix={<ProjectOutlined style={{ color: '#2563EB' }} />}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card variant="borderless" style={{ borderRadius: 8, boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}>
            <Statistic
              title="Đang thi công"
              value={stats.inProgress}
              prefix={<PlayCircleOutlined style={{ color: '#10B981' }} />}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card variant="borderless" style={{ borderRadius: 8, boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}>
            <Statistic
              title="Đã hoàn thành"
              value={stats.completed}
              prefix={<CheckCircleOutlined style={{ color: '#3B82F6' }} />}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card variant="borderless" style={{ borderRadius: 8, boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}>
            <Statistic
              title="Chưa khởi công"
              value={stats.notStarted}
              prefix={<ClockCircleOutlined style={{ color: '#94A3B8' }} />}
            />
          </Card>
        </Col>
      </Row>

      {/* Main Table Card */}
      <Card variant="borderless" style={{ borderRadius: 8, boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}>
        {/* Search & Filter Toolbar */}
        <div style={{ display: 'flex', gap: 12, marginBottom: 16, flexWrap: 'wrap', justifyContent: 'space-between' }}>
          <div style={{ display: 'flex', gap: 12, flex: 1, maxWidth: 500 }}>
            <Input
              placeholder="Tìm theo mã dự án hoặc tên dự án..."
              prefix={<SearchOutlined style={{ color: '#94A3B8' }} />}
              value={searchKeyword}
              onChange={e => setSearchKeyword(e.target.value)}
              allowClear
            />
            <Select
              value={statusFilter}
              onChange={setStatusFilter}
              style={{ width: 170 }}
              options={[
                { value: 'ALL', label: 'Tất cả trạng thái' },
                { value: 'in_progress', label: 'Đang thi công' },
                { value: 'completed', label: 'Hoàn thành' },
                { value: 'not_started', label: 'Chưa khởi công' }
              ]}
            />
          </div>
          <div>
            <span style={{ fontSize: 13, color: '#64748B' }}>
              Hiển thị <strong>{filteredProjects.length}</strong> / {projects.length} dự án
            </span>
          </div>
        </div>

        <Table
          dataSource={filteredProjects}
          columns={columns}
          rowKey="id"
          loading={loading}
          pagination={{ pageSize: 10, showSizeChanger: true }}
          scroll={{ x: 1000 }}
        />
      </Card>

      <ProjectSiteLocationModal
        open={Boolean(locationModalProject)}
        project={locationModalProject}
        onClose={() => setLocationModalProject(null)}
        onSuccess={fetchProjects}
      />

      <AssignProjectManagerModal
        open={Boolean(assignManagerProject)}
        project={assignManagerProject}
        onClose={() => setAssignManagerProject(null)}
        onSuccess={fetchProjects}
      />
    </div>
  );
};

export default ProjectsPage;
