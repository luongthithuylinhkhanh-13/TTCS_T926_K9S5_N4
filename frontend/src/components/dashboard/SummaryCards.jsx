import React from 'react';
import {
  CheckCircleOutlined,
  ClockCircleOutlined,
  ExclamationCircleOutlined,
  UnorderedListOutlined,
} from '@ant-design/icons';

const cards = [
  {
    key: 'totalTasks',
    label: 'Tổng số công việc',
    icon: <UnorderedListOutlined />,
    color: 'blue',
    percentage: 100,
  },
  {
    key: 'completedTasks',
    label: 'Công việc đã hoàn thành',
    icon: <CheckCircleOutlined />,
    color: 'green',
    percentage: 'completed',
  },
  {
    key: 'inProgressTasks',
    label: 'Công việc đang thực hiện',
    icon: <ClockCircleOutlined />,
    color: 'orange',
    percentage: 'inProgress',
  },
  {
    key: 'delayedTasks',
    label: 'Công việc bị trễ',
    icon: <ExclamationCircleOutlined />,
    color: 'red',
    percentage: 'delayed',
  },
];

const SummaryCards = ({ summary }) => (
  <section className="investor-summary-grid" aria-label="Thống kê nhanh">
    {cards.map(card => {
      const percentage = card.percentage === 100
        ? 100
        : card.percentage === 'completed'
          ? Math.round((summary.completedTasks / summary.totalTasks) * 100)
          : card.percentage === 'inProgress'
            ? Math.round((summary.inProgressTasks / summary.totalTasks) * 100)
            : Math.round((summary.delayedTasks / summary.totalTasks) * 100);

      return (
        <article className={`investor-summary-card ${card.color}`} key={card.key}>
          <div className="investor-summary-top">
            <span className="investor-summary-icon" aria-hidden="true">{card.icon}</span>
            <span className="investor-summary-percent">{percentage}%</span>
          </div>
          <strong className="investor-summary-value">{summary[card.key]}</strong>
          <span className="investor-summary-label">{card.label}</span>
        </article>
      );
    })}
  </section>
);

export default SummaryCards;
