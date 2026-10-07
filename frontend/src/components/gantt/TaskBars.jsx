import React, { useMemo } from 'react';
import { Tooltip } from 'antd';
import {
  TIMELINE_CONFIG,
  computeTaskBarGeometry,
  generateTimelineTicks,
} from '../../utils/timelineCoordinate';

/**
 * TaskBars component renders all Gantt task bars inside a SINGLE SVG root.
 * Optimized for ~500 tasks with zero JavaScript scroll-event overhead.
 *
 * @param {Object} props
 * @param {Array<Object>} props.tasks Array of task objects
 * @param {Object} props.bounds Timeline bounds from calculateTimelineBounds()
 * @param {'day'|'week'} [props.unit='day'] Unit scale mode
 * @param {typeof TIMELINE_CONFIG} [props.config=TIMELINE_CONFIG]
 * @param {(task: Object) => React.ReactNode} [props.renderTooltip] Optional tooltip renderer
 * @param {string} [props.className='']
 * @param {Object} [props.style={}]
 */
const TaskBars = ({
  tasks = [],
  bounds,
  unit = 'day',
  config = TIMELINE_CONFIG,
  renderTooltip,
  className = '',
  style = {},
}) => {
  const rowHeight = config?.ROW_HEIGHT ?? TIMELINE_CONFIG.ROW_HEIGHT;
  const totalTasks = tasks.length;
  const totalHeight = Math.max(rowHeight, totalTasks * rowHeight);

  // Generate grid ticks matching the timeline axis
  const ticks = useMemo(() => {
    if (!bounds?.timelineStart || !bounds?.timelineEnd) return [];
    try {
      return generateTimelineTicks(
        bounds.timelineStart,
        bounds.timelineEnd,
        unit,
        config
      );
    } catch {
      return [];
    }
  }, [bounds?.timelineStart, bounds?.timelineEnd, unit, config]);

  // Compute bar geometries
  const taskGeometries = useMemo(() => {
    if (!bounds) return [];
    return tasks.map((task, index) => ({
      task,
      index,
      geom: computeTaskBarGeometry(task, bounds, index, config),
    }));
  }, [tasks, bounds, config]);

  if (!bounds) return null;

  const { totalWidth } = bounds;

  return (
    <div
      className={`gantt-taskbars-wrapper ${className}`}
      style={{
        width: totalWidth,
        height: totalHeight,
        position: 'relative',
        ...style,
      }}
    >
      <svg
        className="gantt-taskbars-svg"
        width={totalWidth}
        height={totalHeight}
        style={{
          display: 'block',
          overflow: 'visible',
          userSelect: 'none',
        }}
        aria-label="Thân biểu đồ Gantt"
      >
        {/* 1. Background Grid Lines */}
        <g className="gantt-grid-layer" aria-hidden="true">
          {/* Vertical date gridlines */}
          {ticks.map((tick) => (
            <line
              key={tick.id}
              x1={tick.x}
              y1={0}
              x2={tick.x}
              y2={totalHeight}
              stroke={tick.isMajor ? '#e2e8f0' : '#f8fafc'}
              strokeDasharray={tick.isMajor ? 'none' : '2,2'}
              strokeWidth={1}
            />
          ))}

          {/* Right boundary vertical line */}
          <line
            x1={totalWidth}
            y1={0}
            x2={totalWidth}
            y2={totalHeight}
            stroke="#cbd5e1"
            strokeWidth={1}
          />

          {/* Horizontal row separator lines */}
          {tasks.map((_, index) => (
            <line
              key={`row-line-${index}`}
              x1={0}
              y1={(index + 1) * rowHeight}
              x2={totalWidth}
              y2={(index + 1) * rowHeight}
              stroke="#f1f5f9"
              strokeWidth={1}
            />
          ))}
        </g>

        {/* 2. Task Bars Layer */}
        <g className="gantt-bars-layer">
          {taskGeometries.map(({ task, index, geom }) => {
            if (!geom) return null; // Unscheduled task: no bar

            const isCritical = geom.isCritical;
            const barKey = task.id || `task-bar-${index}`;

            const defaultTooltipContent = (
              <div className="gantt-tooltip">
                <strong>
                  {task.wbsCode ? `${task.wbsCode} · ` : ''}
                  {task.name || task.label || `Task ${index + 1}`}
                </strong>
                {task.startDate && <div>Bắt đầu: {task.startDate}</div>}
                {task.endDate && <div>Kết thúc: {task.endDate}</div>}
                {task.duration != null && <div>Thời lượng: {task.duration} ngày</div>}
                {task.es != null && <div>ES: {task.es}</div>}
                {task.ef != null && <div>EF: {task.ef}</div>}
                {task.slack != null && <div>Slack: {task.slack} ngày</div>}
                {task.progress != null && <div>Tiến độ: {task.progress}%</div>}
              </div>
            );

            const tooltipTitle = renderTooltip ? renderTooltip(task) : defaultTooltipContent;

            const tooltipAccessibleText = [
              task.name || task.label || `Công việc ${index + 1}`,
              task.startDate ? `Bắt đầu: ${task.startDate}` : '',
              task.endDate ? `Kết thúc: ${task.endDate}` : '',
              task.duration ? `Thời lượng: ${task.duration} ngày` : '',
              isCritical ? 'ĐƯỜNG GĂNG' : '',
            ]
              .filter(Boolean)
              .join('. ');

            const barNode = (
              <g
                key={barKey}
                className={`gantt-bar-group${isCritical ? ' is-critical' : ''}`}
                tabIndex={0}
                role="img"
                aria-label={tooltipAccessibleText}
                style={{ cursor: 'pointer' }}
              >
                {/* Native SVG title for fast non-blocking tooltip */}
                <title>{tooltipAccessibleText}</title>

                {/* Base task bar rectangle */}
                <rect
                  x={geom.x}
                  y={geom.y}
                  width={geom.width}
                  height={geom.height}
                  rx={4}
                  ry={4}
                  fill={isCritical ? '#fee2e2' : '#dbeafe'}
                  stroke={isCritical ? '#dc2626' : '#2563eb'}
                  strokeWidth={isCritical ? 1.5 : 1}
                  className="gantt-bar-base"
                />

                {/* Progress fill rectangle */}
                {geom.progressWidth > 0 && (
                  <rect
                    x={geom.x}
                    y={geom.y}
                    width={geom.progressWidth}
                    height={geom.height}
                    rx={4}
                    ry={4}
                    fill={isCritical ? '#dc2626' : '#2563eb'}
                    opacity={0.85}
                    className="gantt-bar-progress"
                  />
                )}

                {/* Critical indicator badge */}
                {isCritical && (
                  <text
                    x={geom.x + geom.width + 6}
                    y={geom.y + geom.height / 2 + 4}
                    fill="#dc2626"
                    fontSize={10}
                    fontWeight={700}
                    fontFamily="system-ui, -apple-system, sans-serif"
                    className="gantt-bar-critical-label"
                  >
                    ⚠ GĂNG
                  </text>
                )}
              </g>
            );

            return (
              <Tooltip
                key={barKey}
                title={tooltipTitle}
                placement="top"
                mouseEnterDelay={0.1}
              >
                {barNode}
              </Tooltip>
            );
          })}
        </g>
      </svg>
    </div>
  );
};

export default TaskBars;
