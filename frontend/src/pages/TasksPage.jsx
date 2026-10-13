import React, { useEffect, useState, useMemo } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import {
  Card,
  Table,
  Select,
  Input,
  Checkbox,
  Tag,
  Progress,
  Button,
  Avatar,
  Space,
  Alert,
  Empty
} from 'antd';
import {
  SearchOutlined,
  ThunderboltOutlined,
  NodeIndexOutlined,
  HistoryOutlined,
  UserOutlined
} from '@ant-design/icons';
import { getProjects, getProjectWbs, getProjectSchedule } from '../services/wbsApi';

const TasksPage = () => {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();

  const [projects, setProjects] = useState([]);
  const [selectedProjectId, setSelectedProjectId] = useState(
    searchParams.get('projectId') || null
  );

  const [tasks, setTasks] = useState([]);
  const [criticalTaskIds, setCriticalTaskIds] = useState(new Set());
  const [loadingProjects, setLoadingProjects] = useState(true);
  const [loadingTasks, setLoadingTasks] = useState(false);
  const [error, setError] = useState('');

  // Filters
  const [searchKeyword, setSearchKeyword] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [typeFilter, setTypeFilter] = useState('ALL');
  const [criticalOnly, setCriticalOnly] = useState(false);

  // Load Projects
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
        } else if (safeList.length > 0) {
          setSelectedProjectId(safeList[0].id);
        }
      } catch (err) {
        setError(err.message || 'Không thể tải danh sách dự án');
      } finally {
        setLoadingProjects(false);
      }
    };

    fetchProjects();
  }, [searchParams]);

  // Load Tasks for selected project
  useEffect(() => {
    if (!selectedProjectId) {
      setTasks([]);
      return;
    }

    const fetchTasks = async () => {
      setLoadingTasks(true);
      setError('');
      try {
        const [wbsData, scheduleData] = await Promise.all([
          getProjectWbs(selectedProjectId).catch(() => []),
          getProjectSchedule(selectedProjectId).catch(() => null)
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
        setTasks(rawWbs);
      } catch (err) {
        setError(err.message || 'Không thể tải danh sách công việc');
        setTasks([]);
      } finally {
        setLoadingTasks(false);
      }
    };

    fetchTasks();
  }, [selectedProjectId]);

  const handleProjectChange = (val) => {
    setSelectedProjectId(val);
    setSearchParams(val ? { projectId: val } : {});
  };

  const filteredTasks = useMemo(() => {
    return tasks.filter(task => {
      const matchKeyword =
        !searchKeyword ||
        task.name?.toLowerCase().includes(searchKeyword.toLowerCase()) ||
        task.wbsCode?.toLowerCase().includes(searchKeyword.toLowerCase());

      const matchStatus =
        statusFilter === 'ALL' ||
        task.status?.toLowerCase() === statusFilter.toLowerCase();

      const matchType =
        typeFilter === 'ALL' ||
        task.type?.toLowerCase() === typeFilter.toLowerCase();

      const isCritical =
        criticalTaskIds.has(task.id) || criticalTaskIds.has(task.wbsCode);

      const matchCritical = !criticalOnly || isCritical;

      return matchKeyword && matchStatus && matchType && matchCritical;
    });
  }, [tasks, searchKeyword, statusFilter, typeFilter, criticalOnly, criticalTaskIds]);

  const getTypeTag = (type) => {
    switch (type) {
      case 'project':
        return <Tag color="purple">Dự án</Tag>;
      case 'phase':
        return <Tag color="blue">Giai đoạn</Tag>;
      case 'task':
      default:
        return <Tag color="cyan">Công việc</Tag>;
    }
  };

  const getStatusTag = (status) => {
    switch (status) {
      case 'in_progress':
        return <Tag color="processing">Đang thực hiện</Tag>;
      case 'completed':
        return <Tag color="success">Hoàn thành</Tag>;
      case 'not_started':
      default:
        return <Tag color="default">Chưa bắt đầu</Tag>;
    }
  };

  const columns = [
    {
      title: 'Mã WBS',
      dataIndex: 'wbsCode',
      key: 'wbsCode',
      width: 100,
      render: code => <strong>{code}</strong>
    },
    {
      title: 'Tên công việc',
      dataIndex: 'name',
      key: 'name',
      width: 250,
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
      title: 'Loại',
      dataIndex: 'type',
      key: 'type',
      width: 100,
      render: type => getTypeTag(type)
    },
    {
      title: 'Người thực hiện',
      dataIndex: 'assigneeName',
      key: 'assigneeName',
      width: 170,
      render: (name, record) => {
        if (!name && !record.assignee?.name) {
          return <span style={{ color: '#94A3B8' }}>Chưa gán</span>;
        }
        const displayName = name || record.assignee?.name;
        const initials = record.assigneeInitials || record.assignee?.initials || displayName.slice(0, 2).toUpperCase();
        return (
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <Avatar size="small" style={{ backgroundColor: '#2563EB', fontSize: 11 }}>
              {initials}
            </Avatar>
            <span>{displayName}</span>
          </div>
        );
      }
    },
    {
      title: 'Thời lượng',
      dataIndex: 'duration',
      key: 'duration',
      width: 100,
      align: 'right',
      render: d => (d != null ? `${d} ngày` : '--')
    },
    {
      title: 'Kế hoạch',
      key: 'planDates',
      width: 160,
      render: (_, r) => (
        <span style={{ fontSize: 12 }}>
          {r.startDate || '--'} → {r.endDate || '--'}
        </span>
      )
    },
    {
      title: 'Thực tế',
      key: 'actualDates',
      width: 160,
      render: (_, r) => (
        <span style={{ fontSize: 12, color: r.actualStartDate ? '#1E293B' : '#94A3B8' }}>
          {r.actualStartDate || '--'} → {r.actualEndDate || '--'}
        </span>
      )
    },
    {
      title: 'Tiến độ',
      dataIndex: 'progress',
      key: 'progress',
      width: 130,
      render: (p, record) => {
        const val = p ?? record.progressPercent ?? 0;
        return <Progress percent={val} size="small" />;
      }
    },
    {
      title: 'Trạng thái',
      dataIndex: 'status',
      key: 'status',
      width: 130,
      render: s => getStatusTag(s)
    },
    {
      title: 'Thao tác',
      key: 'action',
      width: 160,
      align: 'center',
      render: () => (
        <Space size="small">
          <Button
            size="small"
            icon={<NodeIndexOutlined />}
            onClick={() => navigate(`/wbs?projectId=${selectedProjectId}`)}
          >
            WBS
          </Button>
          <Button
            size="small"
            type="primary"
            ghost
            icon={<HistoryOutlined />}
            onClick={() => navigate(`/progress?projectId=${selectedProjectId}`)}
          >
            Tiến độ
          </Button>
        </Space>
      )
    }
  ];

  const projectOptions = projects.map(p => ({
    value: p.id,
    label: `${p.code} - ${p.name}`
  }));

  return (
    <div className="tasks-page" style={{ padding: '4px 0 24px' }}>
      <div className="page-header" style={{ marginBottom: 20 }}>
        <div>
          <h1 className="page-title">Quản lý Công việc</h1>
          <p className="page-subtitle">Danh mục tất cả công việc theo dự án, người thực hiện và tiến độ</p>
        </div>
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

      {error && (
        <Alert
          type="error"
          showIcon
          closable
          message="Lỗi tải dữ liệu công việc"
          description={error}
          style={{ marginBottom: 20 }}
          onClose={() => setError('')}
        />
      )}

      <Card variant="borderless" style={{ borderRadius: 8, boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}>
        {/* Filter Controls */}
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
              style={{ width: 240 }}
              allowClear
            />
            <Select
              value={statusFilter}
              onChange={setStatusFilter}
              style={{ width: 150 }}
              options={[
                { value: 'ALL', label: 'Tất cả trạng thái' },
                { value: 'not_started', label: 'Chưa bắt đầu' },
                { value: 'in_progress', label: 'Đang thực hiện' },
                { value: 'completed', label: 'Hoàn thành' }
              ]}
            />
            <Select
              value={typeFilter}
              onChange={setTypeFilter}
              style={{ width: 140 }}
              options={[
                { value: 'ALL', label: 'Tất cả phân loại' },
                { value: 'project', label: 'Dự án' },
                { value: 'phase', label: 'Giai đoạn' },
                { value: 'task', label: 'Công việc' }
              ]}
            />
            <Checkbox
              checked={criticalOnly}
              onChange={e => setCriticalOnly(e.target.checked)}
              style={{ fontWeight: 500 }}
            >
              <ThunderboltOutlined style={{ color: '#EF4444', marginRight: 4 }} />
              Chỉ công việc găng
            </Checkbox>
          </div>

          <div>
            <span style={{ fontSize: 13, color: '#64748B' }}>
              Hiển thị <strong>{filteredTasks.length}</strong> / {tasks.length} mục
            </span>
          </div>
        </div>

        <Table
          dataSource={filteredTasks}
          columns={columns}
          rowKey="id"
          loading={loadingTasks}
          pagination={{ pageSize: 10, showSizeChanger: true }}
          scroll={{ x: 1200 }}
          locale={{ emptyText: <Empty description="Không có công việc nào phù hợp với bộ lọc" /> }}
        />
      </Card>
    </div>
  );
};

export default TasksPage;
