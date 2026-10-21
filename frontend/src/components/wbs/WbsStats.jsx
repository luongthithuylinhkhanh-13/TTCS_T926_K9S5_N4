import React from 'react';
import { 
  CheckCircleOutlined,
  ClockCircleOutlined,
  NodeIndexOutlined,
  SyncOutlined
} from '@ant-design/icons';
import { useLocale } from '../../utils/LocaleContext';

const WbsStats = ({ stats }) => {
  const { t } = useLocale();
  const {
    totalTasks = 0,
    inProgress = 0,
    completed = 0,
    notStarted = 0,
    paused = 0
  } = stats || {};
  const completionPercent = totalTasks > 0
    ? Math.round((completed / totalTasks) * 100)
    : 0;
  const progressPercent = totalTasks > 0
    ? Math.round((inProgress / totalTasks) * 100)
    : 0;

  const statusRows = [
    { key: 'inprogress', label: t('Đang thực hiện'), value: inProgress, icon: <SyncOutlined /> },
    { key: 'completed', label: t('Hoàn thành'), value: completed, icon: <CheckCircleOutlined /> },
    { key: 'notstarted', label: t('Chưa bắt đầu'), value: notStarted, icon: <ClockCircleOutlined /> },
    { key: 'paused', label: t('Tạm dừng'), value: paused, icon: <ClockCircleOutlined /> }
  ].map((row) => {
    const percent = totalTasks > 0 ? Math.round((row.value / totalTasks) * 100) : 0;

    return {
      ...row,
      percent,
      tooltip: `${row.label}: ${percent}% (${row.value}/${totalTasks})`
    };
  });

  return (
    <section className="wbs-overview-panel" aria-label={t('Tổng quan cơ cấu công việc')}>
      <div className="wbs-overview-heading">
        <div className="wbs-overview-title">
          <span className="wbs-overview-icon"><NodeIndexOutlined /></span>
          <div>
            <span className="wbs-overview-eyebrow">{t('TỔNG QUAN WBS')}</span>
            <h2>{t('Cơ cấu công việc')}</h2>
          </div>
        </div>
        <div className="wbs-overview-total">
          <strong>{totalTasks}</strong>
          <span>{t('đầu việc')}</span>
        </div>
      </div>

      <div className="wbs-overview-progress">
        <div className="wbs-progress-copy">
          <span>{t('Mức hoàn thành')}</span>
          <strong>{completionPercent}%</strong>
        </div>
        <div className="wbs-progress-track" aria-label={`${completionPercent}% ${t('hoàn thành')}`}>
          <span className="wbs-progress-completed" style={{ width: `${completionPercent}%` }} />
          <span className="wbs-progress-active" style={{ width: `${progressPercent}%` }} />
        </div>
        <div className="wbs-progress-meta">
          <span><i className="wbs-dot completed" />{completed} {t('hoàn thành')}</span>
          <span><i className="wbs-dot active" />{inProgress} {t('đang thực hiện')}</span>
          <span><i className="wbs-dot pending" />{notStarted} {t('chưa bắt đầu')}</span>
        </div>
      </div>

      <div className="wbs-status-list">
        {statusRows.map((row) => (
          <div
            className={`wbs-status-row ${row.key}`}
            key={row.key}
            title={row.tooltip}
            aria-label={`${row.label}: ${row.percent}%`}
          >
            <span className="wbs-status-icon">{row.icon}</span>
            <span className="wbs-status-label">{row.label}</span>
            <strong>{row.value}</strong>
          </div>
        ))}
      </div>
    </section>
  );
};

export default WbsStats;
