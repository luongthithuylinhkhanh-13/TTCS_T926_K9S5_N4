import React from 'react';
import { CheckOutlined } from '@ant-design/icons';

const dateFormatter = new Intl.DateTimeFormat('vi-VN', {
  day: '2-digit',
  month: '2-digit',
  year: 'numeric',
});

const MilestoneTimeline = ({ milestones }) => (
  <section className="investor-panel">
    <div className="investor-panel-heading">
      <div>
        <h2>Các mốc quan trọng</h2>
        <p>Theo dõi các cột mốc chính của dự án</p>
      </div>
    </div>
    <ol className="investor-timeline">
      {milestones.map(milestone => (
        <li className={milestone.completed ? 'completed' : ''} key={milestone.id}>
          <span className="investor-timeline-marker" aria-hidden="true">
            {milestone.completed && <CheckOutlined />}
          </span>
          <div className="investor-timeline-content">
            <strong>{milestone.name}</strong>
            <span>{dateFormatter.format(new Date(`${milestone.date}T00:00:00`))}</span>
          </div>
          <span className={`investor-milestone-state ${milestone.completed ? 'is-completed' : ''}`}>
            {milestone.completed ? 'Đã hoàn thành' : 'Chưa hoàn thành'}
          </span>
        </li>
      ))}
    </ol>
  </section>
);

export default MilestoneTimeline;
