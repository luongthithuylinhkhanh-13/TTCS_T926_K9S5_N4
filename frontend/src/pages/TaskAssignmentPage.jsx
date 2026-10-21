import React, { useEffect, useMemo, useState } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import {
  Card,
  Table,
  Select,
  Input,
  Tag,
  Button,
  Space,
  Alert,
  Spin,
  Drawer,
  Empty,
  Result,
  Tooltip
} from 'antd';
import {
  UserSwitchOutlined,
  SearchOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
  TeamOutlined,
  ArrowRightOutlined,
  ThunderboltOutlined
} from '@ant-design/icons';
import { getProjects, getProjectWbs, getProjectSchedule } from '../services/wbsApi';
import TeamAssignmentSection from '../components/wbs/TeamAssignmentSection';
import { getAuthUser } from '../utils/auth';
import { isProjectManager } from '../utils/permissions';
import { useLocale } from '../utils/LocaleContext';
import { useProjectContext } from '../utils/ProjectContext';

const formatDate = (val) => {
  if (!val) return '--';
  return new Date(`${val}T00:00:00`).toLocaleDateString('vi-VN');
};

const TaskAssignmentPage = () => {
  const { t } = useLocale();
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const authUser = getAuthUser();

  const {
    activeProjectId,
    activeProject,
    effectiveRole,
    currentProjectRole,
    projects: contextProjects,
    setActiveProjectId,
    projectMemberships
  } = useProjectContext();

  const [projects, setProjects] = useState(contextProjects || []);
  const [selectedProjectId, setSelectedProjectId] = useState(
    () => searchParams.get('projectId') || activeProjectId || null
  );

  const [tasks, setTasks] = useState([]);
  const [criticalTaskIds, setCriticalTaskIds] = useState(new Set());
  const [loadingProjects, setLoadingProjects] = useState(true);
  const [loadingTasks, setLoadingTasks] = useState(false);
  const [error, setError] = useState('');

  // Search & Filter
  const [searchKeyword, setSearchKeyword] = useState('');
  const [assignmentFilter, setAssignmentFilter] = useState('ALL'); // ALL, ASSIGNED, UNASSIGNED

  // Drawer assignment state
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [selectedTask, setSelectedTask] = useState(null);

  // Sync projects from context
  useEffect(() => {
    if (contextProjects && contextProjects.length > 0) {
      setProjects(contextProjects);
    }
  }, [contextProjects]);

  // Load accessible projects
  useEffect(() => {
    const fetchProjects = async () => {
      setLoadingProjects(true);
      setError('');
      try {
        const data = await getProjects();
        const safeList = Array.isArray(data) ? data : [];
        setProjects(safeList);

        const paramId = searchParams.get('projectId');
        if (paramId && safeList.some(p => p.id === paramId)) {
          setSelectedProjectId(paramId);
          setActiveProjectId(paramId);
        } else if (activeProjectId && safeList.some(p => p.id === activeProjectId)) {
          setSelectedProjectId(activeProjectId);
        } else if (safeList.length > 0) {
          const firstId = safeList[0].id;
          setSelectedProjectId(firstId);
          setActiveProjectId(firstId);
        }
      } catch (err) {
        setError(err.message || t('Không thể tải danh sách dự án'));
      } finally {
        setLoadingProjects(false);
      }
    };

    fetchProjects();
  }, [searchParams, t, activeProjectId, setActiveProjectId]);

  // Load Tasks for selected project
  const fetchTasks = async (projId) => {
    if (!projId) {
      setTasks([]);
      return;
    }
    setLoadingTasks(true);
    setError('');
    try {
      const [wbsData, scheduleData] = await Promise.all([
        getProjectWbs(projId).catch(() => []),
        getProjectSchedule(projId).catch(() => null)
      ]);

      const rawWbs = Array.isArray(wbsData) ? wbsData : [];
      const critSet = new Set();

      if (scheduleData?.tasks) {
        scheduleData.tasks.forEach(t => {
          if (t.isCritical) {
            if (t.id) critSet.add(t.id);
            if (t.wbsCode) critSet.add(t.wbsCode);
          }
        });
      }

      setCriticalTaskIds(critSet);
      // Filter leaf tasks suitable for assignment (type === 'task' or non-phase)
      const leafTasks = rawWbs.filter(item => item.type === 'task' || !item.type);
      setTasks(leafTasks);

      // Check if taskId param is present to open drawer automatically
      const paramTaskId = searchParams.get('taskId');
      if (paramTaskId) {
        const target = leafTasks.find(it => it.id === paramTaskId);
        if (target) {
          setSelectedTask(target);
          setDrawerOpen(true);
        }
      }
    } catch (err) {
      setError(err.message || t('Không thể tải danh sách công việc'));
      setTasks([]);
    } finally {
      setLoadingTasks(false);
    }
  };

  useEffect(() => {
    if (selectedProjectId) {
      fetchTasks(selectedProjectId);
    } else {
      setTasks([]);
    }
  }, [selectedProjectId]);

  const handleProjectChange = (val) => {
    setSelectedProjectId(val);
    setActiveProjectId(val);
    setSearchParams(val ? { projectId: val } : {});
    setDrawerOpen(false);
    setSelectedTask(null);
  };

  const handleOpenAssignDrawer = (task) => {
    setSelectedTask(task);
    setDrawerOpen(true);
  };

  const handleAssignmentCompleted = () => {
    // Refresh task list after assignment
    if (selectedProjectId) {
      fetchTasks(selectedProjectId);
    }
  };

  const filteredTasks = useMemo(() => {
    return tasks.filter(task => {
      const matchKeyword =
        !searchKeyword ||
        task.name?.toLowerCase().includes(searchKeyword.toLowerCase()) ||
        task.wbsCode?.toLowerCase().includes(searchKeyword.toLowerCase());

      const hasAssignment = Boolean(task.assignedTeamMemberId || task.assignedTeamName);
      const matchAssignment =
        assignmentFilter === 'ALL' ||
        (assignmentFilter === 'ASSIGNED' && hasAssignment) ||
        (assignmentFilter === 'UNASSIGNED' && !hasAssignment);

      return matchKeyword && matchAssignment;
    });
  }, [tasks, searchKeyword, assignmentFilter]);

  const projectOptions = useMemo(() => {
    return projects.map(p => ({
      value: p.id,
      label: `${p.code} - ${p.name}`
    }));
  }, [projects]);

  const canAssign = useMemo(() => {
    const user = authUser || getAuthUser();
    const globalRole = String(user?.roleName || '').toUpperCase();
    if (globalRole === 'ADMIN') return true;

    const projId = selectedProjectId || activeProjectId;
    const memberships = (projectMemberships && projectMemberships.length > 0)
      ? projectMemberships
      : (user?.projectMemberships || []);

    if (projId && memberships.length > 0) {
      const activeMember = memberships.find(
        m => m.projectId === projId && String(m.status || '').toUpperCase() === 'ACTIVE'
      );
      if (activeMember) {
        const projRole = String(activeMember.roleName || '').toUpperCase();
        return ['ADMIN', 'PROJECT_MANAGER', 'SITE_MANAGER'].includes(projRole);
      }
      return false;
    }

    if (projId && projId === activeProjectId && currentProjectRole) {
      return ['ADMIN', 'PROJECT_MANAGER', 'SITE_MANAGER'].includes(String(currentProjectRole).toUpperCase());
    }

    if (effectiveRole) {
      return ['ADMIN', 'PROJECT_MANAGER', 'SITE_MANAGER'].includes(String(effectiveRole).toUpperCase());
    }

    return isProjectManager(user, projId);
  }, [authUser, selectedProjectId, activeProjectId, projectMemberships, currentProjectRole, effectiveRole]);

  if (loadingProjects && projects.length === 0) {
    return (
      <div style={{ padding: '60px 24px', textAlign: 'center' }}>
        <Spin size="large" tip={t('Đang tải danh sách dự án...')} />
      </div>
    );
  }

  if (!canAssign) {
    return (
      <div style={{ padding: '60px 24px', textAlign: 'center' }}>
        <Result
          status="403"
          title="403"
          subTitle={t('Chức năng Giao việc chỉ dành cho Chỉ huy trưởng và Quản trị viên trong dự án này.')}
          extra={
            <Space wrap>
              {projects.length > 1 && (
                <Select
                  style={{ minWidth: 260 }}
                  value={selectedProjectId}
                  onChange={handleProjectChange}
                  options={projectOptions}
                  placeholder={t('Chọn dự án khác')}
                />
              )}
              <Button type="primary" onClick={() => navigate('/dashboard')}>
                {t('Về trang chủ')}
              </Button>
            </Space>
          }
        />
      </div>
    );
  }

  const columns = [
    {
      title: 'Mã WBS',
      dataIndex: 'wbsCode',
      key: 'wbsCode',
      width: 110,
      render: code => <strong>{code}</strong>
    },
    {
      title: 'Tên công việc',
      dataIndex: 'name',
      key: 'name',
      width: 280,
      render: (name, record) => {
        const isCrit = criticalTaskIds.has(record.id) || criticalTaskIds.has(record.wbsCode);
        return (
          <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
            <span>{name}</span>
            {isCrit && (
              <Tag color="red" icon={<ThunderboltOutlined />} style={{ marginLeft: 4 }}>
                Găng
              </Tag>
            )}
          </div>
        );
      }
    },
    {
      title: 'Thời gian',
      key: 'dates',
      width: 200,
      render: (_, r) => (
        <span style={{ fontSize: 13, color: '#4B5563' }}>
          {formatDate(r.startDate)} → {formatDate(r.endDate)}
        </span>
      )
    },
    {
      title: 'Trạng thái',
      dataIndex: 'status',
      key: 'status',
      width: 140,
      render: s => {
        if (s === 'completed') return <Tag color="success">Hoàn thành</Tag>;
        if (s === 'in_progress') return <Tag color="processing">Đang thực hiện</Tag>;
        return <Tag color="default">Chưa bắt đầu</Tag>;
      }
    },
    {
      title: 'Đội / Nhân sự được giao',
      key: 'assignedTeam',
      width: 240,
      render: (_, record) => {
        const teamName = record.assignedTeamName;
        if (teamName) {
          return (
            <Tag color="blue" icon={<TeamOutlined />} style={{ fontSize: 13, padding: '3px 8px' }}>
              {teamName}
            </Tag>
          );
        }
        return <Tag color="default" style={{ color: '#9CA3AF' }}>Chưa phân công</Tag>;
      }
    },
    {
      title: 'Thao tác',
      key: 'action',
      width: 150,
      align: 'center',
      render: (_, record) => {
        const isAssigned = Boolean(record.assignedTeamMemberId || record.assignedTeamName);
        return (
          <Button
            type={isAssigned ? 'default' : 'primary'}
            size="small"
            icon={<UserSwitchOutlined />}
            onClick={() => handleOpenAssignDrawer(record)}
          >
            {isAssigned ? 'Đổi đội' : 'Giao việc'}
          </Button>
        );
      }
    }
  ];



  return (
    <div className="task-assignment-page" style={{ padding: '4px 0 24px' }}>
      {/* Header */}
      <div className="page-header" style={{ marginBottom: 20, display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: 16 }}>
        <div>
          <h1 className="page-title" style={{ fontSize: 22, fontWeight: 700, margin: 0, color: '#111827', display: 'flex', alignItems: 'center', gap: 8 }}>
            <UserSwitchOutlined style={{ color: '#2563EB' }} />
            Giao việc cho đội thi công
          </h1>
          <p className="page-subtitle" style={{ color: '#6B7280', marginTop: 4, marginBottom: 0 }}>
            Chỉ huy trưởng thiết lập phân công công việc cho các đội thi công trong dự án
          </p>
        </div>

        <div style={{ display: 'flex', gap: 12, alignItems: 'center' }}>
          <Button
            icon={<ArrowRightOutlined />}
            onClick={() => navigate('/assigned-tasks')}
            style={{ fontWeight: 500 }}
          >
            Xem công việc đã giao
          </Button>

          <div style={{ minWidth: 260 }}>
            <Select
              style={{ width: '100%' }}
              value={selectedProjectId}
              onChange={handleProjectChange}
              options={projectOptions}
              placeholder="Chọn dự án"
              loading={loadingProjects}
            />
          </div>
        </div>
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

      {/* Main Table Card */}
      <Card variant="borderless" style={{ borderRadius: 8, boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}>
        {/* Filters */}
        <div
          style={{
            display: 'flex',
            gap: 12,
            marginBottom: 16,
            flexWrap: 'wrap',
            alignItems: 'center',
            justifyContent: 'space-between'
          }}
        >
          <div style={{ display: 'flex', gap: 12, flexWrap: 'wrap', alignItems: 'center' }}>
            <Input
              placeholder="Tìm theo tên hoặc mã WBS..."
              prefix={<SearchOutlined style={{ color: '#94A3B8' }} />}
              value={searchKeyword}
              onChange={e => setSearchKeyword(e.target.value)}
              style={{ width: 260 }}
              allowClear
            />

            <Select
              value={assignmentFilter}
              onChange={setAssignmentFilter}
              style={{ width: 180 }}
              options={[
                { value: 'ALL', label: 'Tất cả trạng thái' },
                { value: 'UNASSIGNED', label: 'Chưa phân công' },
                { value: 'ASSIGNED', label: 'Đã phân công' }
              ]}
            />
          </div>

          <div style={{ color: '#6B7280', fontSize: 13 }}>
            Hiển thị <strong>{filteredTasks.length}</strong> / {tasks.length} công việc
          </div>
        </div>

        <Table
          rowKey="id"
          columns={columns}
          dataSource={filteredTasks}
          loading={loadingTasks}
          pagination={{ pageSize: 15, showSizeChanger: true }}
          locale={{ emptyText: <Empty description="Không có công việc nào" /> }}
          scroll={{ x: 1000 }}
        />
      </Card>

      {/* Assignment Drawer */}
      <Drawer
        title={
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <UserSwitchOutlined style={{ color: '#2563EB' }} />
            <span>Phân công đội thi công</span>
          </div>
        }
        placement="right"
        width={480}
        onClose={() => {
          setDrawerOpen(false);
          setSelectedTask(null);
        }}
        open={drawerOpen}
        destroyOnClose
      >
        {selectedTask && (
          <div>
            <div style={{ padding: '12px 16px', background: '#F8FAFC', borderRadius: 8, marginBottom: 20 }}>
              <div style={{ fontSize: 12, color: '#64748B', fontWeight: 600 }}>MÃ CÔNG VIỆC: {selectedTask.wbsCode}</div>
              <div style={{ fontSize: 16, fontWeight: 700, color: '#1E293B', marginTop: 4 }}>{selectedTask.name}</div>
              <div style={{ fontSize: 13, color: '#475569', marginTop: 6 }}>
                Thời gian: <strong>{formatDate(selectedTask.startDate)}</strong> → <strong>{formatDate(selectedTask.endDate)}</strong>
              </div>
              <div style={{ marginTop: 6 }}>
                Trạng thái:{' '}
                <Tag color={selectedTask.status === 'completed' ? 'green' : selectedTask.status === 'in_progress' ? 'blue' : 'default'}>
                  {selectedTask.status === 'completed' ? 'Hoàn thành' : selectedTask.status === 'in_progress' ? 'Đang thực hiện' : 'Chưa bắt đầu'}
                </Tag>
              </div>
            </div>

            <TeamAssignmentSection
              projectId={selectedProjectId}
              task={selectedTask}
              onAssigned={handleAssignmentCompleted}
            />
          </div>
        )}
      </Drawer>
    </div>
  );
};

export default TaskAssignmentPage;
