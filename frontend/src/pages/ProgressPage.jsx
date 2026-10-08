import React, { useEffect, useState } from 'react';
import { Alert, Button, Empty, Progress, Select, Spin, Table, Tooltip } from 'antd';
import { BranchesOutlined, ReloadOutlined } from '@ant-design/icons';
import { getProjects, getProjectSchedule } from '../services/wbsApi';
<<<<<<< HEAD
import { useLocale } from '../utils/LocaleContext';
=======
import MilestoneAlertSection from '../components/wbs/MilestoneAlertSection';
import { GanttChart, formatDateVN, formatVariance } from '../components/gantt';
>>>>>>> f022a01a95bb5a0b18549ae648489720629402b9

const ProgressPage = () => {
  const { t } = useLocale();
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

<<<<<<< HEAD
  const renderCpmTooltip = task => (
    <div className="gantt-tooltip">
      <strong>{task.wbsCode} · {task.name}</strong>
      <span>ES ({t('bắt đầu sớm nhất')}): {task.es ?? '--'}</span>
      <span>EF ({t('kết thúc sớm nhất')}): {task.ef ?? '--'}</span>
      <span>LS ({t('bắt đầu muộn nhất')}): {task.ls ?? '--'}</span>
      <span>LF ({t('kết thúc muộn nhất')}): {task.lf ?? '--'}</span>
      <span>Slack ({t('độ trễ cho phép')}): {task.slack ?? '--'} {t('ngày')}</span>
    </div>
  );
=======
  const renderCpmTooltip = task => {
    const plannedStart = formatDateVN(task.startDate);
    const plannedEnd = formatDateVN(task.endDate);
    const calculatedStart = formatDateVN(task.calculatedStartDate);
    const calculatedEnd = formatDateVN(task.calculatedEndDate);
    const actualStart = formatDateVN(task.actualStartDate);
    const actualEnd = formatDateVN(task.actualEndDate);
    const varianceText = formatVariance(task.scheduleVarianceDays);

    return (
      <div className="gantt-tooltip">
        <strong>{task.wbsCode ? `${task.wbsCode} · ` : ''}{task.name}</strong>

        {(plannedStart || plannedEnd) && (
          <div className="gantt-tooltip-section">
            <span className="gantt-tooltip-section-title">Kế hoạch:</span>
            {plannedStart && <div>Bắt đầu: {plannedStart}</div>}
            {plannedEnd && <div>Kết thúc: {plannedEnd}</div>}
          </div>
        )}

        {(calculatedStart || calculatedEnd) && (
          <div className="gantt-tooltip-section">
            <span className="gantt-tooltip-section-title">Hiện tại:</span>
            {calculatedStart && <div>Bắt đầu: {calculatedStart}</div>}
            {calculatedEnd && <div>Kết thúc: {calculatedEnd}</div>}
          </div>
        )}

        {varianceText && (
          <div className="gantt-tooltip-variance">
            Chênh lệch hoàn thành: <strong>{varianceText}</strong>
          </div>
        )}

        {task.delayedStart && task.startDelayDays != null && task.startDelayDays > 0 && (
          <div className="gantt-tooltip-delay-warning">
            ⚠ Mở trễ {task.startDelayDays} ngày
          </div>
        )}

        {(actualStart || actualEnd) && (
          <div className="gantt-tooltip-section">
            <span className="gantt-tooltip-section-title">Thực tế:</span>
            {actualStart && <div>Bắt đầu: {actualStart}</div>}
            {actualEnd && <div>Kết thúc: {actualEnd}</div>}
          </div>
        )}

        <div className="gantt-tooltip-section gantt-tooltip-cpm">
          <div>ES: {task.es ?? '--'} · EF: {task.ef ?? '--'}</div>
          <div>LS: {task.ls ?? '--'} · LF: {task.lf ?? '--'}</div>
          <div>Slack: {task.slack ?? '--'} ngày</div>
        </div>
      </div>
    );
  };
>>>>>>> f022a01a95bb5a0b18549ae648489720629402b9

  const columns = [
    {
      title: t('Mã WBS'),
      dataIndex: 'wbsCode',
      key: 'wbsCode',
      width: 110
    },
    {
      title: t('Công việc găng'),
      dataIndex: 'name',
      key: 'name',
      width: 260
    },
    {
      title: t('Thời lượng'),
      dataIndex: 'duration',
      key: 'duration',
      align: 'right',
      render: duration => duration == null ? '--' : `${duration} ${t('ngày')}`
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
      title: t('Tiến độ'),
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
          <h1 className="page-title">{t('Tiến độ dự án')}</h1>
          <p className="page-subtitle">{t('Phân tích tiến độ và các công việc trên đường găng')}</p>
        </div>
        <div className="progress-page-controls">
          <Select
            aria-label={t('Chọn dự án')}
            value={selectedProjectId}
            options={projectOptions}
            onChange={setSelectedProjectId}
            placeholder={t('Chọn dự án')}
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
            {schedule ? t('Tính lại công việc găng') : t('Tính công việc găng')}
          </Button>
        </div>
      </div>

      {pageError && (
        <Alert
          type="error"
          showIcon
          closable
          message={t('Không thể tải tiến độ')}
          description={pageError}
          style={{ marginBottom: 16 }}
          onClose={() => setPageError('')}
        />
      )}

      <Spin spinning={loadingProjects}>
        {!loadingProjects && projects.length === 0 ? (
          <div className="wbs-main-card progress-empty-state">
            <Empty description={t('Chưa có dự án để tính tiến độ')} />
          </div>
        ) : schedule ? (
          <>
            {schedule.summary?.complete === false && (
              <Alert
                type="warning"
                showIcon
                style={{ marginBottom: 16 }}
                message={t('Kết quả chưa đầy đủ')}
                description={`${schedule.summary.unscheduledTaskCount} ${t('công việc thiếu thời lượng hợp lệ nên chưa được tính vào đường găng.')}`}
              />
            )}

            <div className="critical-path-summary progress-summary">
              <div className="critical-path-metrics">
                <div>
                  <span>{t('Tổng công việc')}</span>
                  <strong>{schedule.summary?.totalTasks ?? 0}</strong>
                </div>
                <div>
                  <span>{t('Công việc găng')}</span>
                  <strong>{schedule.summary?.criticalTasksCount ?? criticalTasks.length}</strong>
                </div>
                <div>
                  <span>{t('Thời lượng đường găng')}</span>
                  <strong>{schedule.summary?.projectDuration ?? 0} {t('ngày')}</strong>
                </div>
                <div>
                  <span>{t('Số đường găng')}</span>
                  <strong>{schedule.summary?.criticalPathCount ?? '0'}</strong>
                </div>
              </div>
            </div>

            <section className="wbs-main-card gantt-section">
<<<<<<< HEAD
              <div className="wbs-card-header">
                <div className="wbs-card-title-box">
                  <span className="wbs-card-title">{t('Biểu đồ Gantt tiến độ')}</span>
                  <span className="wbs-card-subtitle">{t('Di chuột hoặc dùng bàn phím để xem thông số CPM')}</span>
                </div>
              </div>
              {scheduledTasks.length > 0 ? (
                <div className="gantt-scroll-area">
                  <div className="gantt-chart" role="table" aria-label={t('Biểu đồ Gantt và lịch CPM')}>
                    <div className="gantt-header" role="row">
                      <span className="gantt-task-heading" role="columnheader">{t('Công việc')}</span>
                      <div className="gantt-axis" role="columnheader" aria-label={t('Ngày dự án')}>
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
                          `Slack: ${task.slack ?? '--'} ${t('ngày')}`
                        ].join('. ');

                        return (
                          <div className="gantt-row" role="row" key={task.id}>
                            <div className="gantt-task-label" role="rowheader">
                              <span className="gantt-task-code">{task.wbsCode}</span>
                              <span className="gantt-task-name">{task.name}</span>
                              {task.isCritical && <span className="gantt-critical-tag">{t('GĂNG')}</span>}
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
                                    ⚠ {t('GĂNG')}
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
                    <span><i className="gantt-legend-bar" aria-hidden="true" /> {t('Công việc thường')}</span>
                    <span><i className="gantt-legend-bar is-critical" aria-hidden="true" /> {t('Công việc găng (viền đậm, sọc chéo, nhãn)')}</span>
                  </div>
                </div>
              ) : (
                <div className="gantt-empty">
                  <Empty description={t('Chưa có công việc đủ thời lượng để hiển thị trên biểu đồ')} />
                </div>
              )}
=======
              <GanttChart
                tasks={scheduledTasks}
                schedule={schedule}
                scheduleError={pageError}
                loading={loadingSchedule}
                renderTooltip={renderCpmTooltip}
              />
>>>>>>> f022a01a95bb5a0b18549ae648489720629402b9
            </section>

            <section className="wbs-main-card progress-table-section">
              <div className="wbs-card-header">
                <div className="wbs-card-title-box">
                  <span className="wbs-card-title">{t('Bảng tiến độ công việc găng')}</span>
                  <span className="wbs-card-subtitle">{t('Critical Path Progress')}</span>
                </div>
              </div>
              <Table
                rowKey="id"
                columns={columns}
                dataSource={criticalTasks}
                pagination={false}
                size="middle"
                scroll={{ x: 1000 }}
                locale={{ emptyText: <Empty description={t('Chưa có công việc găng trong dự án này')} /> }}
              />
            </section>

            <MilestoneAlertSection projectId={selectedProjectId} />
          </>
        ) : (
          <div className="wbs-main-card progress-empty-state">
            <Empty description={selectedProjectId ? t('Đang tải kết quả tiến độ...') : t('Chọn dự án để xem tiến độ')} />
          </div>
        )}
      </Spin>
    </div>
  );
};

export default ProgressPage;
