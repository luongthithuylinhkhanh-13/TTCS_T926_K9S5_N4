import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Card,
  Row,
  Col,
  Statistic,
  Progress,
  Tag,
  Button,
  Table,
  Spin,
  Alert,
  Empty
} from 'antd';
import {
  ProjectOutlined,
  PlayCircleOutlined,
  CheckSquareOutlined,
  WarningOutlined,
  ThunderboltOutlined,
  NodeIndexOutlined,
  HistoryOutlined,
  ArrowRightOutlined,
  UserSwitchOutlined,
  BookOutlined,
  WalletOutlined,
  CalculatorOutlined,
  AuditOutlined
} from '@ant-design/icons';
import { getProjects, getProjectSchedule, getProjectWbs } from '../services/wbsApi';
import AdminDashboard from '../components/admin/AdminDashboard';
import { getAuthUser } from '../utils/auth';

const ConstructionDashboard = () => {
  const navigate = useNavigate();
  const authUser = getAuthUser();
  const role = String(authUser?.roleName || '').toUpperCase();
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [projects, setProjects] = useState([]);
  const [summaryData, setSummaryData] = useState({
    totalProjects: 0,
    inProgressProjects: 0,
    totalTasks: 0,
    criticalTasks: 0,
    avgProgress: 0,
    completedTasks: 0,
    inProgressTasks: 0,
    notStartedTasks: 0,
    criticalTaskList: []
  });

  const getDashboardHeader = () => {
    switch (role) {
      case 'SITE_MANAGER':
        return {
          title: 'Bảng điều hành công trường - Chỉ huy trưởng',
          subtitle: 'Tổng hợp chỉ số điều hành công trường, phân công công việc và công việc găng'
        };
      case 'PROJECT_MANAGER':
        return {
          title: 'Bảng điều hành - Ban quản lý dự án',
          subtitle: 'Quản lý tổng thể danh mục dự án, tiến độ WBS, kế hoạch và tài chính'
        };
      case 'CUSTOMER':
        return {
          title: 'Bảng theo dõi Chủ đầu tư',
          subtitle: 'Theo dõi tiến độ tổng thể các dự án, mốc quan trọng, nghiệm thu và thanh toán'
        };
      case 'SITE_ENGINEER':
        return {
          title: 'Bảng điều hành Kỹ sư giám sát',
          subtitle: 'Giám sát thi công hiện trường, nhật ký công trình và biên bản nghiệm thu'
        };
      case 'WORKER':
        return {
          title: 'Bảng công việc Đội trưởng thi công',
          subtitle: 'Theo dõi công việc được phân công, cập nhật khối lượng và lịch thi công'
        };
      case 'STAFF':
        return {
          title: 'Bảng điều khiển Kế toán chủ đầu tư',
          subtitle: 'Tổng hợp chi phí thực tế, đề nghị thanh toán và dự toán công trình'
        };
      default:
        return {
          title: 'Bảng điều khiển tổng quan',
          subtitle: 'Tổng hợp chỉ số điều hành, tiến độ các dự án và công việc găng'
        };
    }
  };

  const renderQuickActions = () => {
    switch (role) {
      case 'SITE_MANAGER':
        return (
          <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap' }}>
            <Button type="primary" icon={<UserSwitchOutlined />} onClick={() => navigate('/task-assignment')}>
              Giao việc mới
            </Button>
            <Button icon={<CheckSquareOutlined />} onClick={() => navigate('/assigned-tasks')}>
              Công việc đã giao
            </Button>
            <Button icon={<BookOutlined />} onClick={() => navigate('/site-diary')}>
              Nhật ký công trường
            </Button>
          </div>
        );
      case 'PROJECT_MANAGER':
        return (
          <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap' }}>
            <Button type="primary" icon={<HistoryOutlined />} onClick={() => navigate('/progress')}>
              Tiến độ Gantt
            </Button>
            <Button icon={<CalculatorOutlined />} onClick={() => navigate('/budget')}>
              Dự toán
            </Button>
            <Button icon={<WalletOutlined />} onClick={() => navigate('/payment-requests')}>
              Đề nghị thanh toán
            </Button>
          </div>
        );
      case 'CUSTOMER':
        return (
          <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap' }}>
            <Button type="primary" icon={<HistoryOutlined />} onClick={() => navigate('/progress')}>
              Xem tiến độ
            </Button>
            <Button icon={<AuditOutlined />} onClick={() => navigate('/acceptance-records')}>
              Phiếu nghiệm thu
            </Button>
            <Button icon={<WalletOutlined />} onClick={() => navigate('/payment-requests')}>
              Đề nghị thanh toán
            </Button>
          </div>
        );
      case 'SITE_ENGINEER':
        return (
          <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap' }}>
            <Button type="primary" icon={<BookOutlined />} onClick={() => navigate('/site-diary')}>
              Ghi nhật ký
            </Button>
            <Button icon={<AuditOutlined />} onClick={() => navigate('/acceptance-records')}>
              Phiếu nghiệm thu
            </Button>
            <Button icon={<HistoryOutlined />} onClick={() => navigate('/progress')}>
              Xem tiến độ
            </Button>
          </div>
        );
      case 'WORKER':
        return (
          <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap' }}>
            <Button type="primary" icon={<CheckSquareOutlined />} onClick={() => navigate('/assigned-tasks')}>
              Việc được giao
            </Button>
            <Button icon={<AuditOutlined />} onClick={() => navigate('/acceptance')}>
              Cập nhật khối lượng
            </Button>
            <Button icon={<BookOutlined />} onClick={() => navigate('/site-diary')}>
              Nhật ký hôm nay
            </Button>
          </div>
        );
      case 'STAFF':
        return (
          <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap' }}>
            <Button type="primary" icon={<WalletOutlined />} onClick={() => navigate('/payment-requests')}>
              Đề nghị thanh toán
            </Button>
            <Button icon={<CalculatorOutlined />} onClick={() => navigate('/budget')}>
              Dự toán
            </Button>
            <Button icon={<AuditOutlined />} onClick={() => navigate('/acceptance')}>
              Nghiệm thu khối lượng
            </Button>
          </div>
        );
      default:
        return null;
    }
  };

  const renderKpiCards = () => {
    if (role === 'WORKER') {
      return (
        <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
          <Col xs={24} sm={12} md={8} lg={5}>
            <Card variant="borderless" style={{ boxShadow: '0 1px 3px rgba(0,0,0,0.05)', borderRadius: 8 }}>
              <Statistic
                title="Dự án tham gia"
                value={summaryData.totalProjects}
                prefix={<ProjectOutlined style={{ color: '#2563EB' }} />}
              />
            </Card>
          </Col>
          <Col xs={24} sm={12} md={8} lg={5}>
            <Card variant="borderless" style={{ boxShadow: '0 1px 3px rgba(0,0,0,0.05)', borderRadius: 8 }}>
              <Statistic
                title="Tổng việc cần làm"
                value={summaryData.totalTasks}
                prefix={<CheckSquareOutlined style={{ color: '#6366F1' }} />}
              />
            </Card>
          </Col>
          <Col xs={24} sm={12} md={8} lg={5}>
            <Card variant="borderless" style={{ boxShadow: '0 1px 3px rgba(0,0,0,0.05)', borderRadius: 8 }}>
              <Statistic
                title="Đang thực hiện"
                value={summaryData.inProgressTasks}
                prefix={<PlayCircleOutlined style={{ color: '#3B82F6' }} />}
              />
            </Card>
          </Col>
          <Col xs={24} sm={12} md={8} lg={5}>
            <Card variant="borderless" style={{ boxShadow: '0 1px 3px rgba(0,0,0,0.05)', borderRadius: 8 }}>
              <Statistic
                title="Đã hoàn thành"
                value={summaryData.completedTasks}
                prefix={<CheckSquareOutlined style={{ color: '#10B981' }} />}
                valueStyle={{ color: '#10B981' }}
              />
            </Card>
          </Col>
          <Col xs={24} sm={12} md={8} lg={4}>
            <Card variant="borderless" style={{ boxShadow: '0 1px 3px rgba(0,0,0,0.05)', borderRadius: 8 }}>
              <Statistic
                title="Tiến độ đạt"
                value={summaryData.avgProgress}
                suffix="%"
                valueStyle={{ color: '#0F766E' }}
              />
            </Card>
          </Col>
        </Row>
      );
    }

    if (role === 'CUSTOMER') {
      return (
        <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
          <Col xs={24} sm={12} md={8} lg={5}>
            <Card variant="borderless" style={{ boxShadow: '0 1px 3px rgba(0,0,0,0.05)', borderRadius: 8 }}>
              <Statistic
                title="Tổng dự án đầu tư"
                value={summaryData.totalProjects}
                prefix={<ProjectOutlined style={{ color: '#2563EB' }} />}
              />
            </Card>
          </Col>
          <Col xs={24} sm={12} md={8} lg={5}>
            <Card variant="borderless" style={{ boxShadow: '0 1px 3px rgba(0,0,0,0.05)', borderRadius: 8 }}>
              <Statistic
                title="Dự án đang thi công"
                value={summaryData.inProgressProjects}
                prefix={<PlayCircleOutlined style={{ color: '#10B981' }} />}
              />
            </Card>
          </Col>
          <Col xs={24} sm={12} md={8} lg={5}>
            <Card variant="borderless" style={{ boxShadow: '0 1px 3px rgba(0,0,0,0.05)', borderRadius: 8 }}>
              <Statistic
                title="Tiến độ trung bình"
                value={summaryData.avgProgress}
                suffix="%"
                valueStyle={{ color: '#0F766E' }}
              />
            </Card>
          </Col>
          <Col xs={24} sm={12} md={8} lg={5}>
            <Card variant="borderless" style={{ boxShadow: '0 1px 3px rgba(0,0,0,0.05)', borderRadius: 8 }}>
              <Statistic
                title="Việc đã hoàn thành"
                value={summaryData.completedTasks}
                prefix={<CheckSquareOutlined style={{ color: '#10B981' }} />}
              />
            </Card>
          </Col>
          <Col xs={24} sm={12} md={8} lg={4}>
            <Card variant="borderless" style={{ boxShadow: '0 1px 3px rgba(0,0,0,0.05)', borderRadius: 8 }}>
              <Statistic
                title="Hạng mục găng"
                value={summaryData.criticalTasks}
                prefix={<ThunderboltOutlined style={{ color: '#EF4444' }} />}
                valueStyle={{ color: '#EF4444' }}
              />
            </Card>
          </Col>
        </Row>
      );
    }

    // Default for SITE_MANAGER, PROJECT_MANAGER, SITE_ENGINEER, STAFF
    return (
      <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
        <Col xs={24} sm={12} md={8} lg={4}>
          <Card variant="borderless" style={{ boxShadow: '0 1px 3px rgba(0,0,0,0.05)', borderRadius: 8 }}>
            <Statistic
              title={role === 'SITE_MANAGER' ? 'Dự án phụ trách' : role === 'SITE_ENGINEER' ? 'Dự án giám sát' : 'Tổng số dự án'}
              value={summaryData.totalProjects}
              prefix={<ProjectOutlined style={{ color: '#2563EB' }} />}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={8} lg={5}>
          <Card variant="borderless" style={{ boxShadow: '0 1px 3px rgba(0,0,0,0.05)', borderRadius: 8 }}>
            <Statistic
              title="Đang thi công"
              value={summaryData.inProgressProjects}
              prefix={<PlayCircleOutlined style={{ color: '#10B981' }} />}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={8} lg={5}>
          <Card variant="borderless" style={{ boxShadow: '0 1px 3px rgba(0,0,0,0.05)', borderRadius: 8 }}>
            <Statistic
              title="Tổng công việc"
              value={summaryData.totalTasks}
              prefix={<CheckSquareOutlined style={{ color: '#6366F1' }} />}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={8} lg={5}>
          <Card variant="borderless" style={{ boxShadow: '0 1px 3px rgba(0,0,0,0.05)', borderRadius: 8 }}>
            <Statistic
              title="Công việc găng"
              value={summaryData.criticalTasks}
              prefix={<ThunderboltOutlined style={{ color: '#EF4444' }} />}
              valueStyle={{ color: '#EF4444' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={8} lg={5}>
          <Card variant="borderless" style={{ boxShadow: '0 1px 3px rgba(0,0,0,0.05)', borderRadius: 8 }}>
            <Statistic
              title="Tiến độ bình quân"
              value={summaryData.avgProgress}
              suffix="%"
              valueStyle={{ color: '#0F766E' }}
            />
          </Card>
        </Col>
      </Row>
    );
  };

  useEffect(() => {
    const fetchDashboardData = async () => {
      setLoading(true);
      setError('');
      try {
        const projectList = await getProjects();
        const safeProjects = Array.isArray(projectList) ? projectList : [];
        setProjects(safeProjects);

        let totalTasksCount = 0;
        let criticalTasksCount = 0;
        let completedCount = 0;
        let inProgressCount = 0;
        let notStartedCount = 0;
        const allCriticalTasks = [];

        // Lấy thông tin chi tiết từng dự án (nếu có)
        await Promise.all(
          safeProjects.map(async (project) => {
            try {
              const [wbsItems, schedule] = await Promise.all([
                getProjectWbs(project.id).catch(() => []),
                getProjectSchedule(project.id).catch(() => null)
              ]);

              const taskItems = Array.isArray(wbsItems)
                ? wbsItems.filter(item => item.type === 'task')
                : [];
              totalTasksCount += taskItems.length;

              taskItems.forEach(t => {
                const status = (t.status || '').toLowerCase();
                if (status === 'completed' || t.progress === 100) {
                  completedCount++;
                } else if (status === 'in_progress' || (t.progress > 0 && t.progress < 100)) {
                  inProgressCount++;
                } else {
                  notStartedCount++;
                }
              });

              if (schedule?.tasks) {
                const critTasks = schedule.tasks.filter(t => t.isCritical);
                criticalTasksCount += critTasks.length;
                critTasks.forEach(ct => {
                  allCriticalTasks.push({
                    ...ct,
                    projectName: project.name,
                    projectId: project.id
                  });
                });
              }
            } catch {
              // Ignore single project errors to keep dashboard resilient
            }
          })
        );

        const activeProjects = safeProjects.filter(
          p => p.status === 'in_progress' || p.status === 'active'
        ).length;

        const avg = safeProjects.length > 0
          ? Math.round(
              safeProjects.reduce((sum, p) => sum + (Number(p.progress) || 0), 0) / safeProjects.length
            )
          : 0;

        setSummaryData({
          totalProjects: safeProjects.length,
          inProgressProjects: activeProjects,
          totalTasks: totalTasksCount,
          criticalTasks: criticalTasksCount,
          avgProgress: avg,
          completedTasks: completedCount,
          inProgressTasks: inProgressCount,
          notStartedTasks: notStartedCount,
          criticalTaskList: allCriticalTasks.slice(0, 5) // Top 5
        });
      } catch (err) {
        setError(err.message || 'Không thể tải dữ liệu bảng điều khiển tổng quan');
      } finally {
        setLoading(false);
      }
    };

    fetchDashboardData();
  }, []);

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

  const criticalColumns = [
    {
      title: 'Mã WBS',
      dataIndex: 'wbsCode',
      key: 'wbsCode',
      width: 100,
      render: code => <strong>{code || '--'}</strong>
    },
    {
      title: 'Công việc găng',
      dataIndex: 'name',
      key: 'name',
      render: (name, record) => (
        <div>
          <div style={{ fontWeight: 600 }}>{name}</div>
          <small style={{ color: '#64748B' }}>Dự án: {record.projectName}</small>
        </div>
      )
    },
    {
      title: 'Thời lượng',
      dataIndex: 'duration',
      key: 'duration',
      width: 110,
      align: 'right',
      render: d => (d != null ? `${d} ngày` : '--')
    },
    {
      title: 'Tiến độ',
      dataIndex: 'progress',
      key: 'progress',
      width: 130,
      render: p => <Progress percent={p || 0} size="small" />
    },
    {
      title: 'Thao tác',
      key: 'action',
      width: 100,
      align: 'center',
      render: (_, record) => (
        <Button
          type="link"
          size="small"
          onClick={() => navigate(`/progress?projectId=${record.projectId}`)}
        >
          Xem Gantt
        </Button>
      )
    }
  ];

  const headerInfo = getDashboardHeader();

  return (
    <div className="dashboard-page" style={{ padding: '4px 0 24px' }}>
      <div className="page-header" style={{ marginBottom: 20, display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: 16 }}>
        <div>
          <h1 className="page-title">{headerInfo.title}</h1>
          <p className="page-subtitle">{headerInfo.subtitle}</p>
        </div>
        {renderQuickActions()}
      </div>

      {error && (
        <Alert
          type="error"
          showIcon
          closable
          message="Lỗi tải dữ liệu"
          description={error}
          style={{ marginBottom: 20 }}
          onClose={() => setError('')}
        />
      )}

      <Spin spinning={loading}>
        {/* KPI Summary Cards */}
        {renderKpiCards()}

        {/* Project Cards Grid */}
        <div style={{ marginBottom: 24 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
            <h2 style={{ fontSize: 16, fontWeight: 700, margin: 0, color: '#1E293B' }}>
              Danh sách dự án ({projects.length})
            </h2>
            <Button
              type="link"
              onClick={() => navigate('/projects')}
              icon={<ArrowRightOutlined />}
            >
              Xem tất cả dự án
            </Button>
          </div>

          {projects.length === 0 && !loading ? (
            <Card><Empty description="Chưa có dự án nào" /></Card>
          ) : (
            <Row gutter={[16, 16]}>
              {projects.map(project => (
                <Col xs={24} md={12} lg={8} key={project.id}>
                  <Card
                    variant="borderless"
                    style={{
                      borderRadius: 8,
                      boxShadow: '0 1px 3px rgba(0,0,0,0.05)',
                      height: '100%',
                      display: 'flex',
                      flexDirection: 'column',
                      justifyContent: 'space-between'
                    }}
                  >
                    <div>
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 8 }}>
                        <span style={{ fontSize: 12, fontWeight: 700, color: '#2563EB', backgroundColor: '#EFF6FF', padding: '2px 8px', borderRadius: 4 }}>
                          {project.code}
                        </span>
                        {getStatusTag(project.status)}
                      </div>
                      <h3 style={{ fontSize: 15, fontWeight: 600, color: '#0F172A', marginBottom: 6 }}>
                        {project.name}
                      </h3>
                      <p style={{ fontSize: 13, color: '#64748B', minHeight: 38, marginBottom: 12, overflow: 'hidden', textOverflow: 'ellipsis', display: '-webkit-box', WebkitLineClamp: 2, WebkitBoxOrient: 'vertical' }}>
                        {project.description || 'Chưa có mô tả cho dự án này.'}
                      </p>

                      <div style={{ marginBottom: 14 }}>
                        <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 12, marginBottom: 4 }}>
                          <span style={{ color: '#64748B' }}>Tiến độ tổng thể</span>
                          <span style={{ fontWeight: 600 }}>{project.progress || 0}%</span>
                        </div>
                        <Progress percent={project.progress || 0} showInfo={false} strokeColor="#2563EB" />
                      </div>

                      <div style={{ fontSize: 12, color: '#64748B', marginBottom: 14 }}>
                        <div>Thời gian: {project.startDate || '--'} ~ {project.endDate || '--'}</div>
                      </div>
                    </div>

                    <div style={{ display: 'flex', gap: 8, borderTop: '1px solid #F1F5F9', paddingTop: 12 }}>
                      <Button
                        size="small"
                        icon={<NodeIndexOutlined />}
                        style={{ flex: 1 }}
                        onClick={() => navigate(`/wbs?projectId=${project.id}`)}
                      >
                        Xem WBS
                      </Button>
                      <Button
                        size="small"
                        type="primary"
                        ghost
                        icon={<HistoryOutlined />}
                        style={{ flex: 1 }}
                        onClick={() => navigate(`/progress?projectId=${project.id}`)}
                      >
                        Tiến độ Gantt
                      </Button>
                    </div>
                  </Card>
                </Col>
              ))}
            </Row>
          )}
        </div>

        {/* Work Status Breakdown & Critical Tasks */}
        <Row gutter={[16, 16]}>
          <Col xs={24} lg={8}>
            <Card
              title={<span style={{ fontSize: 15, fontWeight: 600 }}>Phân bố trạng thái công việc</span>}
              variant="borderless"
              style={{ borderRadius: 8, boxShadow: '0 1px 3px rgba(0,0,0,0.05)', height: '100%' }}
            >
              <div style={{ display: 'flex', flexDirection: 'column', gap: 14, padding: '10px 0' }}>
                <div>
                  <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 4 }}>
                    <span>Đã hoàn thành</span>
                    <strong style={{ color: '#10B981' }}>{summaryData.completedTasks}</strong>
                  </div>
                  <Progress
                    percent={summaryData.totalTasks > 0 ? Math.round((summaryData.completedTasks / summaryData.totalTasks) * 100) : 0}
                    strokeColor="#10B981"
                  />
                </div>
                <div>
                  <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 4 }}>
                    <span>Đang thực hiện</span>
                    <strong style={{ color: '#3B82F6' }}>{summaryData.inProgressTasks}</strong>
                  </div>
                  <Progress
                    percent={summaryData.totalTasks > 0 ? Math.round((summaryData.inProgressTasks / summaryData.totalTasks) * 100) : 0}
                    strokeColor="#3B82F6"
                  />
                </div>
                <div>
                  <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 4 }}>
                    <span>Chưa bắt đầu</span>
                    <strong style={{ color: '#94A3B8' }}>{summaryData.notStartedTasks}</strong>
                  </div>
                  <Progress
                    percent={summaryData.totalTasks > 0 ? Math.round((summaryData.notStartedTasks / summaryData.totalTasks) * 100) : 0}
                    strokeColor="#94A3B8"
                  />
                </div>
              </div>
            </Card>
          </Col>

          <Col xs={24} lg={16}>
            <Card
              title={
                <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                  <WarningOutlined style={{ color: '#EF4444' }} />
                  <span style={{ fontSize: 15, fontWeight: 600 }}>Công việc trên đường găng trọng yếu</span>
                </div>
              }
              variant="borderless"
              style={{ borderRadius: 8, boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}
            >
              <Table
                dataSource={summaryData.criticalTaskList}
                columns={criticalColumns}
                rowKey={r => `${r.projectId}-${r.id || r.wbsCode}`}
                pagination={false}
                size="small"
                locale={{ emptyText: <Empty description="Không có công việc găng hoặc chưa tính toán tiến độ" /> }}
              />
            </Card>
          </Col>
        </Row>
      </Spin>
    </div>
  );
};

const DashboardPage = () => {
  const user = getAuthUser();
  if (user?.roleName?.toUpperCase() === 'ADMIN') {
    return <AdminDashboard />;
  }
  return <ConstructionDashboard />;
};

export default DashboardPage;
