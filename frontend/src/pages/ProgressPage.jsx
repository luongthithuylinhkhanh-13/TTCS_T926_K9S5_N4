import React, { useEffect, useState } from 'react';
import { Alert, Button, Empty, Progress, Select, Spin, Table, Tooltip } from 'antd';
import { BranchesOutlined, ReloadOutlined } from '@ant-design/icons';
import { getProjects, getProjectSchedule } from '../services/wbsApi';
import MilestoneAlertSection from '../components/wbs/MilestoneAlertSection';

const ProgressPage = () => {
  const [projects, setProjects] = useState([]);
  const [selectedProjectId, setSelectedProjectId] = useState(null);
  const [schedule, setSchedule] = useState(null);
  const [loadingProjects, setLoadingProjects] = useState(true);
  const [loadingSchedule, setLoadingSchedule] = useState(false);
  const [pageError, setPageError] = useState('');

  const loadSchedule = async projectId => {
    if (!projectId) {
      setSchedule(null);
      return;
    }

    setLoadingSchedule(true);
    setPageError('');

    try {
      const result = await getProjectSchedule(projectId);
      setSchedule(result);
    } catch (error) {
      setSchedule(null);
      setPageError(error.message || 'Không thể tính tiến độ dự án');
    } finally {
      setLoadingSchedule(false);
    }
  };

  useEffect(() => {
    const loadProjects = async () => {
      setLoadingProjects(true);
      setPageError('');

      try {
        const result = await getProjects();
        const projectList = Array.isArray(result) ? result : [];
        setProjects(projectList);
        setSelectedProjectId(projectList[0]?.id || null);
      } catch (error) {
        setPageError(error.message || 'Không thể tải danh sách dự án');
      } finally {
        setLoadingProjects(false);
      }
    };

    loadProjects();
  }, []);

  useEffect(() => {
    if (selectedProjectId) {
      loadSchedule(selectedProjectId);
    } else {
      setSchedule(null);
    }
  }, [selectedProjectId]);

  const tasks = schedule?.tasks || [];
  const criticalTasks = tasks.filter(task => task.isCritical);
  const scheduledTasks = tasks.filter(task => task.es != null && task.ef != null && task.duration > 0);
  const projectDuration = Math.max(
    schedule?.summary?.projectDuration || 0,
    ...scheduledTasks.map(task => task.ef)
  );
  const tickInterval = Math.max(1, Math.ceil(projectDuration / 6));
  const timelineTicks = projectDuration > 0
    ? Array.from({ length: Math.floor(projectDuration / tickInterval) + 1 }, (_, index) => index * tickInterval)
    : [];
  if (projectDuration > 0 && timelineTicks[timelineTicks.length - 1] !== projectDuration) {
    timelineTicks.push(projectDuration);
  }

  const renderCpmTooltip = task => (
    <div className="gantt-tooltip">
      <strong>{task.wbsCode} · {task.name}</strong>
      <span>ES (bắt đầu sớm nhất): {task.es ?? '--'}</span>
      <span>EF (kết thúc sớm nhất): {task.ef ?? '--'}</span>
      <span>LS (bắt đầu muộn nhất): {task.ls ?? '--'}</span>
      <span>LF (kết thúc muộn nhất): {task.lf ?? '--'}</span>
      <span>Slack (độ trễ cho phép): {task.slack ?? '--'} ngày</span>
    </div>
  );

  const columns = [
    {
      title: 'Mã WBS',
      dataIndex: 'wbsCode',
      key: 'wbsCode',
      width: 110
    },
    {
      title: 'Công việc găng',
      dataIndex: 'name',
      key: 'name',
      width: 260
    },
    {
      title: 'Thời lượng',
      dataIndex: 'duration',
      key: 'duration',
      align: 'right',
      render: duration => duration == null ? '--' : `${duration} ngày`
    },
    { title: 'ES', dataIndex: 'es', key: 'es', align: 'right' },
    { title: 'EF', dataIndex: 'ef', key: 'ef', align: 'right' },
    { title: 'LS', dataIndex: 'ls', key: 'ls', align: 'right' },
    { title: 'LF', dataIndex: 'lf', key: 'lf', align: 'right' },
    {
      title: 'Slack',
      dataIndex: 'slack',
      key: 'slack',
      align: 'right',
      render: slack => <strong>{slack ?? '--'}</strong>
    },
    {
      title: 'Tiến độ',
      dataIndex: 'progress',
      key: 'progress',
      width: 160,
      render: progress => (
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <Progress percent={progress || 0} size="small" showInfo={false} />
          <span style={{ minWidth: 36 }}>{progress || 0}%</span>
        </div>
      )
    }
  ];

  const projectOptions = projects.map(project => ({
    value: project.id,
    label: `${project.code} - ${project.name}`
  }));

  return (
    <div>
      <div className="page-header">
        <div>
          <h1 className="page-title">Tiến độ dự án</h1>
          <p className="page-subtitle">Phân tích tiến độ và các công việc trên đường găng</p>
        </div>
        <div className="progress-page-controls">
          <Select
            aria-label="Chọn dự án"
            value={selectedProjectId}
            options={projectOptions}
            onChange={setSelectedProjectId}
            placeholder="Chọn dự án"
            style={{ width: 300, maxWidth: '100%' }}
            loading={loadingProjects}
            disabled={loadingProjects || projects.length === 0}
          />
          <Button
            type="primary"
            icon={schedule ? <ReloadOutlined /> : <BranchesOutlined />}
            onClick={() => loadSchedule(selectedProjectId)}
            loading={loadingSchedule}
            disabled={!selectedProjectId}
          >
            {schedule ? 'Tính lại công việc găng' : 'Tính công việc găng'}
          </Button>
        </div>
      </div>

      {pageError && (
        <Alert
          type="error"
          showIcon
          closable
          message="Không thể tải tiến độ"
          description={pageError}
          style={{ marginBottom: 16 }}
          onClose={() => setPageError('')}
        />
      )}

      <Spin spinning={loadingProjects}>
        {!loadingProjects && projects.length === 0 ? (
          <div className="wbs-main-card progress-empty-state">
            <Empty description="Chưa có dự án để tính tiến độ" />
          </div>
        ) : schedule ? (
          <>
            {schedule.summary?.complete === false && (
              <Alert
                type="warning"
                showIcon
                style={{ marginBottom: 16 }}
                message="Kết quả chưa đầy đủ"
                description={`${schedule.summary.unscheduledTaskCount} công việc thiếu thời lượng hợp lệ nên chưa được tính vào đường găng.`}
              />
            )}

            <div className="critical-path-summary progress-summary">
              <div className="critical-path-metrics">
                <div>
                  <span>Tổng công việc</span>
                  <strong>{schedule.summary?.totalTasks ?? 0}</strong>
                </div>
                <div>
                  <span>Công việc găng</span>
                  <strong>{schedule.summary?.criticalTasksCount ?? criticalTasks.length}</strong>
                </div>
                <div>
                  <span>Thời lượng đường găng</span>
                  <strong>{schedule.summary?.projectDuration ?? 0} ngày</strong>
                </div>
                <div>
                  <span>Số đường găng</span>
                  <strong>{schedule.summary?.criticalPathCount ?? '0'}</strong>
                </div>
              </div>
            </div>

            <section className="wbs-main-card gantt-section">
              <div className="wbs-card-header">
                <div className="wbs-card-title-box">
                  <span className="wbs-card-title">Biểu đồ Gantt tiến độ</span>
                  <span className="wbs-card-subtitle">Di chuột hoặc dùng bàn phím để xem thông số CPM</span>
                </div>
              </div>
              {scheduledTasks.length > 0 ? (
                <div className="gantt-scroll-area">
                  <div className="gantt-chart" role="table" aria-label="Biểu đồ Gantt và lịch CPM">
                    <div className="gantt-header" role="row">
                      <span className="gantt-task-heading" role="columnheader">Công việc</span>
                      <div className="gantt-axis" role="columnheader" aria-label="Ngày dự án">
                        <div className="gantt-plot">
                          {timelineTicks.map(tick => (
                            <span
                              className="gantt-tick"
                              key={tick}
                              style={{ left: `${(tick / projectDuration) * 100}%` }}
                            >
                              {tick}
                            </span>
                          ))}
                        </div>
                      </div>
                    </div>
                    <div className="gantt-body" role="rowgroup">
                      {scheduledTasks.map(task => {
                        const left = (task.es / projectDuration) * 100;
                        const width = ((task.ef - task.es) / projectDuration) * 100;
                        const tooltipDescription = [
                          `${task.name}`,
                          `ES: ${task.es ?? '--'}`,
                          `EF: ${task.ef ?? '--'}`,
                          `LS: ${task.ls ?? '--'}`,
                          `LF: ${task.lf ?? '--'}`,
                          `Slack: ${task.slack ?? '--'} ngày`
                        ].join('. ');

                        return (
                          <div className="gantt-row" role="row" key={task.id}>
                            <div className="gantt-task-label" role="rowheader">
                              <span className="gantt-task-code">{task.wbsCode}</span>
                              <span className="gantt-task-name">{task.name}</span>
                              {task.isCritical && <span className="gantt-critical-tag">GĂNG</span>}
                            </div>
                            <div className="gantt-track" role="cell">
                              <div className="gantt-plot">
                                {timelineTicks.map(tick => (
                                  <span
                                    className="gantt-gridline"
                                    key={tick}
                                    style={{ left: `${(tick / projectDuration) * 100}%` }}
                                    aria-hidden="true"
                                  />
                                ))}
                                <Tooltip title={renderCpmTooltip(task)} placement="top">
                                  <div
                                    className={`gantt-bar${task.isCritical ? ' is-critical' : ''}`}
                                    style={{ left: `${left}%`, width: `${width}%` }}
                                    role="img"
                                    tabIndex={0}
                                    aria-label={tooltipDescription}
                                  />
                                </Tooltip>
                                {task.isCritical && (
                                  <span
                                    className="gantt-critical-marker"
                                    style={{ left: `calc(${left + width}% + 5px)` }}
                                    aria-hidden="true"
                                  >
                                    ⚠ GĂNG
                                  </span>
                                )}
                              </div>
                            </div>
                          </div>
                        );
                      })}
                    </div>
                  </div>
                  <div className="gantt-legend">
                    <span><i className="gantt-legend-bar" aria-hidden="true" /> Công việc thường</span>
                    <span><i className="gantt-legend-bar is-critical" aria-hidden="true" /> Công việc găng (viền đậm, sọc chéo, nhãn)</span>
                  </div>
                </div>
              ) : (
                <div className="gantt-empty">
                  <Empty description="Chưa có công việc đủ thời lượng để hiển thị trên biểu đồ" />
                </div>
              )}
            </section>

            <section className="wbs-main-card progress-table-section">
              <div className="wbs-card-header">
                <div className="wbs-card-title-box">
                  <span className="wbs-card-title">Bảng tiến độ công việc găng</span>
                  <span className="wbs-card-subtitle">Critical Path Progress</span>
                </div>
              </div>
              <Table
                rowKey="id"
                columns={columns}
                dataSource={criticalTasks}
                pagination={false}
                size="middle"
                scroll={{ x: 1000 }}
                locale={{ emptyText: <Empty description="Chưa có công việc găng trong dự án này" /> }}
              />
            </section>

            <MilestoneAlertSection projectId={selectedProjectId} />
          </>
        ) : (
          <div className="wbs-main-card progress-empty-state">
            <Empty description={selectedProjectId ? 'Đang tải kết quả tiến độ...' : 'Chọn dự án để xem tiến độ'} />
          </div>
        )}
      </Spin>
    </div>
  );
};

export default ProgressPage;
