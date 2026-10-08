import React, { useEffect, useMemo, useState } from 'react';
import { Alert, Empty, Progress, Select, Spin, Table, Tag } from 'antd';
import { CheckSquareOutlined } from '@ant-design/icons';
import { getProjects, getProjectWbs } from '../services/wbsApi';
import { getAuthUser } from '../utils/auth';
import { isProjectManager } from '../utils/permissions';

const formatDate = value => value
  ? new Date(`${value}T00:00:00`).toLocaleDateString('vi-VN')
  : '--';

const AssignedTasksPage = () => {
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

  const visibleTasks = useMemo(
    () => selectedProjectId === 'all'
      ? tasks
      : tasks.filter(task => task.projectId === selectedProjectId),
    [selectedProjectId, tasks]
  );

  const columns = [
    { title: 'Mã công việc', dataIndex: 'wbsCode', key: 'wbsCode', width: 130 },
    { title: 'Công việc', dataIndex: 'name', key: 'name', width: 260 },
    { title: 'Dự án', dataIndex: 'projectName', key: 'projectName', width: 260 },
    {
      title: 'Đã giao cho',
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
    }
  ];

  if (!isProjectManager(authUser)) {
    return (
      <div className="page-container">
        <Alert type="warning" showIcon message="Chức năng dành cho Chỉ huy trưởng" description="Bạn chỉ có quyền xem các công việc được giao cho mình." />
      </div>
    );
  }

  return (
    <div className="page-container assigned-tasks-page">
      <div className="page-header">
        <div>
          <h1 className="page-title"><CheckSquareOutlined /> Công việc đã giao</h1>
          <p className="page-subtitle">Theo dõi các công việc Chỉ huy trưởng đã phân công cho đội thi công</p>
        </div>
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
