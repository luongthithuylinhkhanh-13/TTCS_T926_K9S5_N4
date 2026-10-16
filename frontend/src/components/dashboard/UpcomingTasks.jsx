import React from 'react';

const dateFormatter = new Intl.DateTimeFormat('vi-VN', {
  day: '2-digit',
  month: '2-digit',
  year: 'numeric',
});

const taskStatusLabels = {
  'in-progress': 'Đang thực hiện',
  'not-started': 'Chưa bắt đầu',
};

const getDeadlineState = endDate => {
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  const deadline = new Date(`${endDate}T00:00:00`);
  const daysRemaining = Math.ceil((deadline - today) / 86400000);

  if (daysRemaining < 0) return { className: 'overdue', label: 'Quá hạn' };
  if (daysRemaining < 3) return { className: 'due-soon', label: daysRemaining === 0 ? 'Đến hạn hôm nay' : `Còn ${daysRemaining} ngày` };
  return { className: 'normal', label: 'Sắp đến hạn' };
};

const UpcomingTasks = ({ tasks }) => (
  <section className="investor-panel">
    <div className="investor-panel-heading">
      <div>
        <h2>Công việc sắp đến hạn</h2>
        <p>Các công việc cần theo dõi trong thời gian tới</p>
      </div>
      <span className="investor-task-count">{tasks.length} công việc</span>
    </div>
    <div className="investor-table-wrap">
      <table className="investor-table upcoming-table">
        <thead>
          <tr>
            <th>Tên công việc</th>
            <th>Người phụ trách</th>
            <th>Ngày kết thúc</th>
            <th>Trạng thái</th>
          </tr>
        </thead>
        <tbody>
          {tasks.map(task => {
            const deadline = getDeadlineState(task.endDate);
            return (
              <tr className={`task-row-${deadline.className}`} key={task.id}>
                <td><strong>{task.name}</strong></td>
                <td>{task.assignee}</td>
                <td>{dateFormatter.format(new Date(`${task.endDate}T00:00:00`))}</td>
                <td>
                  <div className="investor-task-statuses">
                    <span className={`investor-status status-${task.status === 'in-progress' ? 'on-track' : 'normal'}`}>
                      <span className="investor-status-dot" />
                      {taskStatusLabels[task.status]}
                    </span>
                    <span className={`investor-deadline-label ${deadline.className}`}>{deadline.label}</span>
                  </div>
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  </section>
);

export default UpcomingTasks;
