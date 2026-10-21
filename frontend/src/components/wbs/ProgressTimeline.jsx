import React, { useMemo } from 'react';
import { Empty } from 'antd';
import {
  buildScheduleTimeline,
  formatScheduleTimestamp
} from '../../utils/scheduleTimeline';

const ProgressTimeline = ({ tasks, baselineTasks, baselineCapturedAt }) => {
  const timeline = useMemo(
    () => buildScheduleTimeline(tasks, baselineTasks),
    [tasks, baselineTasks]
  );

  const capturedDate = baselineCapturedAt
    ? new Intl.DateTimeFormat('vi-VN', { dateStyle: 'short' })
      .format(new Date(baselineCapturedAt))
    : null;

  return (
    <section className="wbs-main-card schedule-timeline-card">
      <div className="schedule-timeline-heading">
        <div className="wbs-card-title-box">
          <span className="wbs-card-title">So sánh kế hoạch tiến độ</span>
          <span className="wbs-card-subtitle">
            Thanh mờ là kế hoạch gốc; thanh màu là lịch hiện tại
          </span>
        </div>
        <div className="schedule-timeline-legend" aria-label="Chú giải biểu đồ">
          <span><i className="schedule-legend-baseline" />Kế hoạch gốc</span>
          <span><i className="schedule-legend-current" />Hiện tại</span>
        </div>
      </div>

      {capturedDate && (
        <p className="schedule-baseline-caption">
          Kế hoạch gốc được chốt lần đầu ngày {capturedDate} và không thay đổi khi tính lại.
        </p>
      )}

      {timeline.rows.length === 0 ? (
        <Empty
          image={Empty.PRESENTED_IMAGE_SIMPLE}
          description="Chưa có ngày bắt đầu và kết thúc để hiển thị trên biểu đồ"
        />
      ) : (
        <div className="schedule-timeline-scroll">
          <div className="schedule-timeline-content">
            <div className="schedule-timeline-header">
              <span className="schedule-timeline-label-heading">Công việc</span>
              <div className="schedule-timeline-axis">
                {timeline.ticks.map(tick => (
                  <span
                    key={tick.key}
                    className="schedule-timeline-tick"
                    style={{ left: `${tick.offset}%` }}
                  >
                    {tick.label}
                  </span>
                ))}
              </div>
            </div>

            {timeline.rows.map(row => (
              <div className="schedule-timeline-row" key={row.itemId}>
                <div className="schedule-timeline-task" title={row.name}>
                  <strong>{row.wbsCode}</strong>
                  <span>{row.name}</span>
                </div>
                <div className="schedule-timeline-track">
                  {timeline.ticks.map(tick => (
                    <i
                      key={tick.key}
                      className="schedule-timeline-gridline"
                      style={{ left: `${tick.offset}%` }}
                      aria-hidden="true"
                    />
                  ))}
                  {row.baselineBar && (
                    <div
                      className="schedule-timeline-bar schedule-timeline-bar-baseline"
                      style={{
                        left: `${row.baselineBar.left}%`,
                        width: `${row.baselineBar.width}%`
                      }}
                      title={`Kế hoạch gốc: ${formatScheduleTimestamp(row.baselineRange.start)} - ${formatScheduleTimestamp(row.baselineRange.end)}`}
                    />
                  )}
                  {row.currentBar && (
                    <div
                      className={`schedule-timeline-bar schedule-timeline-bar-current${row.isCritical ? ' is-critical' : ''}`}
                      style={{
                        left: `${row.currentBar.left}%`,
                        width: `${row.currentBar.width}%`
                      }}
                      title={`Hiện tại: ${formatScheduleTimestamp(row.currentRange.start)} - ${formatScheduleTimestamp(row.currentRange.end)}`}
                    />
                  )}
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </section>
  );
};

export default ProgressTimeline;
