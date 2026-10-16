import React from 'react';

const statusLabels = {
  'on-track': 'Đúng tiến độ',
  delayed: 'Chậm',
  completed: 'Hoàn thành',
};

const ProgressByCategory = ({ categories }) => (
  <section className="investor-panel">
    <div className="investor-panel-heading">
      <div>
        <h2>Tiến độ theo hạng mục</h2>
        <p>Tỷ lệ hoàn thành theo từng phần việc</p>
      </div>
    </div>
    <div className="investor-table-wrap">
      <table className="investor-table category-table">
        <thead>
          <tr>
            <th>Hạng mục</th>
            <th>Tiến độ</th>
            <th>Trạng thái</th>
          </tr>
        </thead>
        <tbody>
          {categories.map(category => (
            <tr key={category.id}>
              <td>
                <strong>{category.name}</strong>
                <span className="investor-table-detail">{category.totalTasks} công việc</span>
              </td>
              <td>
                <div className="category-progress">
                  <div className="category-progress-track">
                    <span style={{ width: `${category.progress}%` }} />
                  </div>
                  <strong>{category.progress}%</strong>
                </div>
              </td>
              <td>
                <span className={`investor-status status-${category.status}`}>
                  <span className="investor-status-dot" />
                  {statusLabels[category.status]}
                </span>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  </section>
);

export default ProgressByCategory;
