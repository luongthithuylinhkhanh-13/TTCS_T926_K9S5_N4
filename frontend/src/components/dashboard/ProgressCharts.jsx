import React from 'react';

const chartColors = ['#16a36a', '#3478f6', '#d9e1ec'];

const createPieSlices = values => {
  const total = values.reduce((sum, item) => sum + item.value, 0);
  let currentAngle = -90;

  return values.map((item, index) => {
    const sliceAngle = (item.value / total) * 360;
    const startAngle = currentAngle;
    const endAngle = currentAngle + sliceAngle;
    currentAngle = endAngle;
    const toPoint = angle => {
      const radians = (angle * Math.PI) / 180;
      return [50 + 45 * Math.cos(radians), 50 + 45 * Math.sin(radians)];
    };
    const start = toPoint(startAngle);
    const end = toPoint(endAngle);
    const largeArc = sliceAngle > 180 ? 1 : 0;
    const path = `M 50 50 L ${start[0]} ${start[1]} A 45 45 0 ${largeArc} 1 ${end[0]} ${end[1]} Z`;

    return { ...item, color: chartColors[index], path };
  });
};

const ProgressCharts = ({ summary, categories }) => {
  const distribution = [
    { label: 'Hoàn thành', value: summary.completedTasks },
    { label: 'Đang làm', value: summary.inProgressTasks },
    { label: 'Chưa bắt đầu', value: summary.notStartedTasks },
  ];
  const slices = createPieSlices(distribution);

  return (
    <section className="investor-charts-grid" aria-label="Biểu đồ tiến độ">
      <article className="investor-panel chart-panel">
        <div className="investor-panel-heading">
          <div>
            <h2>Cơ cấu công việc</h2>
            <p>Phân bổ theo trạng thái</p>
          </div>
        </div>
        <div className="investor-pie-layout">
          <svg className="investor-pie-chart" viewBox="0 0 100 100" role="img" aria-label="Biểu đồ tròn cơ cấu công việc">
            {slices.map(slice => (
              <path key={slice.label} d={slice.path} fill={slice.color}>
                <title>{slice.label}: {slice.value} công việc</title>
              </path>
            ))}
          </svg>
          <ul className="investor-chart-legend">
            {slices.map(slice => (
              <li key={slice.label}>
                <span style={{ backgroundColor: slice.color }} />
                <span>{slice.label}</span>
                <strong>{slice.value}</strong>
              </li>
            ))}
          </ul>
        </div>
      </article>

      <article className="investor-panel chart-panel">
        <div className="investor-panel-heading">
          <div>
            <h2>Tiến độ hạng mục</h2>
            <p>Tỷ lệ hoàn thành từng hạng mục (%)</p>
          </div>
        </div>
        <div className="investor-bar-chart" role="img" aria-label="Biểu đồ cột tiến độ theo hạng mục">
          {[0, 25, 50, 75, 100].map(tick => (
            <span className="investor-chart-tick" key={tick} style={{ bottom: `${tick}%` }}>
              <span>{tick}</span>
            </span>
          ))}
          <div className="investor-bars">
            {categories.map(category => (
              <div
                className="investor-bar-column"
                key={category.id}
                style={{ '--bar-progress': category.progress / 100 }}
              >
                <strong>{category.progress}%</strong>
                <span className="investor-bar" style={{ height: `${category.progress}%` }} />
                <span className="investor-bar-label">{category.name}</span>
              </div>
            ))}
          </div>
        </div>
      </article>
    </section>
  );
};

export default ProgressCharts;
