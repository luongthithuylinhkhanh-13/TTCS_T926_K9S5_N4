import React, { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Alert, Button, Empty, Progress, Select, Spin, Table, Tag } from 'antd';
import { CheckSquareOutlined, UserSwitchOutlined } from '@ant-design/icons';
import { getProjects, getProjectWbs } from '../services/wbsApi';
import { getAuthUser } from '../utils/auth';
import { isProjectManager } from '../utils/permissions';

const formatDate = value => value
  ? new Date(`${value}T00:00:00`).toLocaleDateString('vi-VN')
  : '--';

const AssignedTasksPage = () => {
  const navigate = useNavigate();
  const authUser = getAuthUser();
  const [projects, setProjects] = useState([]);
  const [selectedProjectId, setSelectedProjectId] = useState('all');
  const [tasks, setTasks] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    const loadAssignedTasks = async () => {
      setLoading(true);
      try {
        const projectList = await getProjects();
        const projectItems = Array.isArray(projectList) ? projectList : [];
        const taskGroups = await Promise.all(projectItems.map(async project => {
          const items = await getProjectWbs(project.id);
          return (Array.isArray(items) ? items : [])
            .filter(item => item.type === 'task' && (item.assignedTeamMemberId || item.assignedTeamName))
            .map(item => ({ ...item, projectId: project.id, projectName: `${project.code} - ${project.name}` }));
        }));
        setProjects(projectItems);
        setTasks(taskGroups.flat());
      } catch (loadError) {
        setError(loadError.message || 'Không thể tải danh sách công việc đã giao.');
      } finally {
        setLoading(false);
      }
    };
    loadAssignedTasks();
  }, []);

  const isWorker = authUser?.roleName?.toUpperCase() === 'WORKER';
  const canManage = isProjectManager(authUser);

  const visibleTasks = useMemo(
    () => {
      let filtered = selectedProjectId === 'all'
        ? tasks
        : tasks.filter(task => task.projectId === selectedProjectId);
      if (isWorker) {
        // If worker has user id or name matching assignee, prioritize those
        const userMatched = filtered.filter(task => 
          task.assignedTeamMemberId === authUser?.id ||
          (authUser?.fullName && task.assignedTeamName?.toLowerCase().includes(authUser.fullName.toLowerCase()))
        );
        if (userMatched.length > 0) {
          filtered = userMatched;
        }
      }
      return filtered;
    },
    [selectedProjectId, tasks, isWorker, authUser]
  );

  const columns = [
    { title: 'Mã công việc', dataIndex: 'wbsCode', key: 'wbsCode', width: 130 },
    { title: 'Công việc', dataIndex: 'name', key: 'name', width: 260 },
    { title: 'Dự án', dataIndex: 'projectName', key: 'projectName', width: 260 },
    {
      title: isWorker ? 'Đội thực hiện' : 'Đã giao cho',
      key: 'assignee',
      render: (_, task) => task.assignedTeamName || task.assigneeName || 'Đội thi công'
    },
    {
      title: 'Thời gian',
      key: 'dates',
      render: (_, task) => `${formatDate(task.startDate)} - ${formatDate(task.endDate)}`
    },
    {
      title: 'Trạng thái',
      key: 'status',
      render: (_, task) => <Tag color={task.progress >= 100 ? 'green' : 'blue'}>{task.status || 'Đang thực hiện'}</Tag>
    },
    {
      title: 'Tiến độ',
      key: 'progress',
      width: 160,
      render: (_, task) => (
        <div className="assigned-task-progress">
          <Progress percent={task.progress || 0} size="small" />
        </div>
      )
    },
    {
      title: 'Thao tác',
      key: 'action',
      width: 140,
      align: 'center',
      render: (_, task) => isWorker ? (
        <Button
          size="small"
          type="primary"
          ghost
          icon={<CheckSquareOutlined />}
          onClick={() => navigate(`/acceptance?projectId=${task.projectId}`)}
        >
          Báo khối lượng
        </Button>
      ) : (
        <Button
          size="small"
          type="primary"
          ghost
          icon={<UserSwitchOutlined />}
          onClick={() => navigate(`/task-assignment?projectId=${task.projectId}&taskId=${task.id}`)}
        >
          Điều chỉnh
        </Button>
      )
    }
  ];

  if (!canManage && !isWorker) {
    return (
      <div className="page-container">
        <Alert type="warning" showIcon message="Chức năng hạn chế" description="Bạn không có quyền truy cập trang quản lý công việc này." />
      </div>
    );
  }

  return (
    <div className="page-container assigned-tasks-page">
      <div className="page-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: 16 }}>
        <div>
          <h1 className="page-title">
            <CheckSquareOutlined /> {isWorker ? 'Công việc được giao' : 'Công việc đã giao'}
          </h1>
          <p className="page-subtitle">
            {isWorker
              ? 'Theo dõi các công việc được Chỉ huy trưởng phân công thực hiện'
              : 'Theo dõi các công việc Chỉ huy trưởng đã phân công cho đội thi công'}
          </p>
        </div>
        <div style={{ display: 'flex', gap: 12, alignItems: 'center' }}>
          {!isWorker && (
            <Button
              type="primary"
              icon={<UserSwitchOutlined />}
              onClick={() => navigate('/task-assignment')}
            >
              Giao việc mới
            </Button>
          )}
          <Select
            value={selectedProjectId}
            onChange={setSelectedProjectId}
            style={{ width: 280 }}
            options={[
              { value: 'all', label: 'Tất cả dự án' },
              ...projects.map(project => ({ value: project.id, label: `${project.code} - ${project.name}` }))
            ]}
          />
        </div>
      </div>
      {error && <Alert type="error" showIcon message={error} style={{ marginBottom: 16 }} />}
      <section className="wbs-main-card assigned-tasks-card">
        <div className="wbs-card-header">
          <div className="wbs-card-title-box">
            <span className="wbs-card-title">Danh sách công việc đã giao</span>
            <span className="wbs-card-subtitle">{visibleTasks.length} công việc</span>
          </div>
        </div>
        <Spin spinning={loading}>
          <Table
            className="assigned-tasks-table"
            rowKey={task => `${task.projectId}-${task.id}`}
            columns={columns}
            dataSource={visibleTasks}
            pagination={false}
            scroll={{ x: 1100, y: 360 }}
            locale={{ emptyText: <Empty description="Chưa có công việc nào được giao" /> }}
          />
        </Spin>
      </section>
    </div>
  );
};

export default AssignedTasksPage;
