import React from 'react';
import { 
  UnorderedListOutlined, 
  SyncOutlined, 
  CheckCircleOutlined, 
  ClockCircleOutlined 
} from '@ant-design/icons';
import { useLocale } from '../../utils/LocaleContext';

const WbsStats = ({ stats }) => {
  const { t } = useLocale();
  const { totalTasks = 0, inProgress = 0, completed = 0, notStarted = 0 } = stats || {};

  return (
    <div className="stats-grid">
      {/* CARD 1 */}
      <div className="stat-card total">
        <div className="stat-info">
          <span className="stat-label">{t('Tổng công việc')}</span>
          <span className="stat-value" style={{ fontSize: '1.5rem', fontWeight: 600 }}>{totalTasks}</span>
          <span style={{ fontSize: 11, color: '#94A3B8', marginTop: 2 }}>{t('Toàn bộ WBS')}</span>
        </div>
        <div className="stat-icon-wrapper blue">
          <UnorderedListOutlined />
        </div>
      </div>

      {/* CARD 2 */}
      <div className="stat-card inprogress">
        <div className="stat-info">
          <span className="stat-label">{t('Đang thực hiện')}</span>
          <span className="stat-value" style={{ color: '#2563EB' }}>{inProgress}</span>
          <span style={{ fontSize: 11, color: '#94A3B8', marginTop: 2 }}>{t('Cần theo dõi sát')}</span>
        </div>
        <div className="stat-icon-wrapper blue">
          <SyncOutlined spin={inProgress > 0} />
        </div>
      </div>

      {/* CARD 3 */}
      <div className="stat-card completed">
        <div className="stat-info">
          <span className="stat-label">{t('Hoàn thành')}</span>
          <span className="stat-value" style={{ color: '#16A34A' }}>{completed}</span>
          <span style={{ fontSize: 11, color: '#94A3B8', marginTop: 2 }}>{t('Đã nghiệm thu')}</span>
        </div>
        <div className="stat-icon-wrapper green">
          <CheckCircleOutlined />
        </div>
      </div>

      {/* CARD 4 */}
      <div className="stat-card notstarted">
        <div className="stat-info">
          <span className="stat-label">{t('Chưa bắt đầu')}</span>
          <span className="stat-value" style={{ color: '#64748B' }}>{notStarted}</span>
          <span style={{ fontSize: 11, color: '#94A3B8', marginTop: 2 }}>{t('Theo kế hoạch')}</span>
        </div>
        <div className="stat-icon-wrapper gray">
          <ClockCircleOutlined />
        </div>
      </div>
    </div>
  );
};

export default WbsStats;
