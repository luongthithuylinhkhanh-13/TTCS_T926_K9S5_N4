import React, { useState, useMemo } from 'react';
import { Alert, Spin, Empty } from 'antd';
import {
  TIMELINE_CONFIG,
  calculateTimelineBounds,
  isCycleError,
} from '../../utils/timelineCoordinate';
import TimelineAxis from './TimelineAxis';
import TimelineUnitControl from './TimelineUnitControl';
import TaskBars from './TaskBars';

/**
 * Composite Gantt Chart Component supporting:
 * - Two-pane fixed left column (task names) + horizontal scrollable SVG timeline
 * - Smooth native browser horizontal scrolling for ~500 tasks
 * - Unit switching between 'day' and 'week'
 * - T-25 cycle detection error handling
 *
 * @param {Object} props
 * @param {Array<Object>} props.tasks Tasks list
 * @param {Object} [props.schedule] Schedule object from API
 * @param {string} [props.scheduleError] Error message from schedule fetch
 * @param {boolean} [props.loading=false] Loading state
 * @param {'day'|'week'} [props.defaultUnit='day'] Default timeline unit
 * @param {typeof TIMELINE_CONFIG} [props.config=TIMELINE_CONFIG]
 * @param {(task: Object) => React.ReactNode} [props.renderTooltip] Custom tooltip renderer
 * @param {string} [props.className='']
 * @param {Object} [props.style={}]
 */
const GanttChart = ({
  tasks = [],
  schedule = null,
  scheduleError = '',
  loading = false,
  defaultUnit = 'day',
  config = TIMELINE_CONFIG,
  renderTooltip,
  className = '',
  style = {},
}) => {
  const [unit, setUnit] = useState(defaultUnit);

  const rowHeight = config?.ROW_HEIGHT ?? TIMELINE_CONFIG.ROW_HEIGHT;
  const headerHeight = config?.HEADER_HEIGHT ?? TIMELINE_CONFIG.HEADER_HEIGHT;

  // Check if T-25 cycle error occurred
  const hasCycle = isCycleError(scheduleError);

  // Compute timeline date range from tasks (T-27 contract: startDate & endDate)
  const { minDate, maxDate } = useMemo(() => {
    let minD = null;
    let maxD = null;

    for (const task of tasks) {
      if (task?.startDate) {
        if (!minD || task.startDate < minD) minD = task.startDate;
      }
      if (task?.endDate) {
        if (!maxD || task.endDate > maxD) maxD = task.endDate;
      }
    }

    if (!minD || !maxD || minD > maxD) {
      return { minDate: null, maxDate: null };
    }

    return { minDate: minD, maxDate: maxD };
  }, [tasks]);

  // Compute timeline boundaries using T-30
  const bounds = useMemo(() => {
    if (!minDate || !maxDate) return null;
    try {
      return calculateTimelineBounds(minDate, maxDate, unit, config);
    } catch (err) {
      console.error('Failed to calculate timeline bounds:', err);
      return null;
    }
  }, [minDate, maxDate, unit, config]);

  // Render Cycle Alert if T-25 cycle error detected
  if (hasCycle) {
    return (
      <div className={`gantt-chart-wrapper ${className}`} style={style}>
        <Alert
          type="error"
          showIcon
          message="Phát hiện quan hệ phụ thuộc tạo thành vòng lặp (Cycle Detected)"
          description={
            scheduleError ||
            'Quan hệ tiền nhiệm giữa các công việc tạo thành chu trình khép kín. Không thể tính toán và hiển thị tiến độ hợp lệ.'
          }
          style={{ marginBottom: 16 }}
        />
      </div>
    );
  }

  // Render generic error if present
  if (scheduleError && !hasCycle) {
    return (
      <div className={`gantt-chart-wrapper ${className}`} style={style}>
        <Alert
          type="error"
          showIcon
          message="Không thể tải tiến độ dự án"
          description={scheduleError}
          style={{ marginBottom: 16 }}
        />
      </div>
    );
  }

  // Empty state if no tasks or no valid dates to form bounds
  if (!loading && (!tasks || tasks.length === 0 || !bounds)) {
    return (
      <div className={`gantt-chart-wrapper ${className}`} style={style}>
        <div className="gantt-empty">
          <Empty description="Chưa có công việc đủ thời lượng để hiển thị trên biểu đồ" />
        </div>
      </div>
    );
  }

  return (
    <div className={`gantt-chart-wrapper ${className}`} style={style}>
      {/* 1. Header Toolbar */}
      <div
        className="gantt-toolbar-container"
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          marginBottom: 12,
          flexWrap: 'wrap',
          gap: 12,
        }}
      >
        <div className="wbs-card-title-box">
          <span className="wbs-card-title">Biểu đồ Gantt tiến độ</span>
          <span className="wbs-card-subtitle" style={{ marginLeft: 8 }}>
            Di chuột hoặc dùng bàn phím để xem thông số CPM
          </span>
        </div>
        <TimelineUnitControl value={unit} onChange={setUnit} />
      </div>

      {/* 2. Loading state */}
      <Spin spinning={loading}>
        {bounds ? (
          <div className="gantt-two-pane-container">
            {/* LEFT PANE: Fixed Task-Name Column (260px) */}
            <div className="gantt-left-pane">
              {/* Left Header */}
              <div
                className="gantt-left-header"
                style={{ height: headerHeight }}
                role="columnheader"
              >
                <span>Công việc</span>
              </div>

              {/* Left Task Rows List */}
              <div className="gantt-left-body" role="rowgroup">
                {tasks.map((task, index) => (
                  <div
                    key={task.id || `task-row-${index}`}
                    className={`gantt-left-row${task.isCritical ? ' is-critical' : ''}`}
                    style={{ height: rowHeight }}
                    role="row"
                  >
                    <div className="gantt-task-label" role="rowheader">
                      {task.wbsCode && (
                        <span className="gantt-task-code">{task.wbsCode}</span>
                      )}
                      <span
                        className="gantt-task-name"
                        title={task.name || task.label}
                      >
                        {task.name || task.label || `Task ${index + 1}`}
                      </span>
                      {task.isCritical && (
                        <span className="gantt-critical-tag">GĂNG</span>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            </div>

            {/* RIGHT VIEWPORT: Horizontal Scroll Container */}
            <div className="gantt-right-viewport">
              <div
                className="gantt-timeline-canvas-container"
                style={{ width: bounds.totalWidth }}
              >
                {/* Timeline Axis Header (Synchronous width & scroll) */}
                <TimelineAxis
                  startDate={bounds.timelineStart}
                  endDate={bounds.timelineEnd}
                  unit={unit}
                  height={headerHeight}
                  config={config}
                />

                {/* Task Bars Body (SVG) */}
                <TaskBars
                  tasks={tasks}
                  bounds={bounds}
                  unit={unit}
                  config={config}
                  renderTooltip={renderTooltip}
                />
              </div>
            </div>
          </div>
        ) : null}

        {/* 3. Legend */}
        <div className="gantt-legend" style={{ marginTop: 12 }}>
          <span>
            <i className="gantt-legend-bar" aria-hidden="true" /> Công việc thường
          </span>
          <span>
            <i className="gantt-legend-bar is-critical" aria-hidden="true" /> Công việc găng (viền đậm, nhãn ⚠ GĂNG)
          </span>
        </div>
      </Spin>
    </div>
  );
};

export default GanttChart;
