import React from 'react';
import { RiseOutlined } from '@ant-design/icons';

const OverallProgress = ({ summary }) => (
  <section className="investor-panel investor-overall-panel">
    <div className="investor-panel-heading">
      <div>
        <h2>Tiến độ tổng thể</h2>
        <p>Tình hình hoàn thành toàn bộ công việc của dự án</p>
      </div>
      <span className="investor-trend"><RiseOutlined /> Theo kế hoạch</span>
    </div>
    <div className="investor-overall-content">
      <div
        className="investor-progress-track"
        role="progressbar"
        aria-label="Tiến độ tổng thể"
        aria-valuemin="0"
        aria-valuemax="100"
        aria-valuenow={summary.overallProgress}
      >
        <div className="investor-progress-fill" style={{ width: `${summary.overallProgress}%` }} />
      </div>
      <div className="investor-overall-stats">
        <strong>{summary.overallProgress}%</strong>
        <span>{summary.completedTasks} / {summary.totalTasks} công việc hoàn thành</span>
      </div>
    </div>
  </section>
);

export default OverallProgress;
