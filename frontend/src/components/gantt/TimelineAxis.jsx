import React, { useMemo } from 'react';
import {
  TIMELINE_CONFIG,
  calculateTimelineBounds,
  generateTimelineTicks,
  generateSecondaryGroups,
} from '../../utils/timelineCoordinate';

/**
 * SVG-based Timeline Axis Header for Gantt charts.
 * Supports dual-tier header (Month/Year secondary tier + Day/Week primary tier)
 * and seamless switching between 'day' and 'week' units.
 *
 * @param {Object} props
 * @param {string|Date} props.startDate Start date of the timeline range
 * @param {string|Date} props.endDate End date of the timeline range
 * @param {'day'|'week'} [props.unit='day'] Unit scale mode
 * @param {number} [props.height=56] Axis header height in pixels
 * @param {number} [props.gridHeight=0] Optional height for extending vertical grid lines downwards (for T-31)
 * @param {Object} [props.config=TIMELINE_CONFIG]
 * @param {string} [props.className]
 * @param {Object} [props.style]
 */
const TimelineAxis = ({
  startDate,
  endDate,
  unit = 'day',
  height = TIMELINE_CONFIG.HEADER_HEIGHT,
  gridHeight = 0,
  config = TIMELINE_CONFIG,
  className = '',
  style = {},
}) => {
  const secondaryHeight = config.SECONDARY_TIER_HEIGHT ?? 26;
  const primaryHeight = config.PRIMARY_TIER_HEIGHT ?? 30;
  const totalSvgHeight = height + (gridHeight > 0 ? gridHeight : 0);

  // Compute boundaries and ticks
  const { bounds, ticks, secondaryGroups } = useMemo(() => {
    if (!startDate || !endDate) {
      return { bounds: null, ticks: [], secondaryGroups: [] };
    }
    try {
      const b = calculateTimelineBounds(startDate, endDate, unit, config);
      const t = generateTimelineTicks(startDate, endDate, unit, config);
      const s = generateSecondaryGroups(t);
      return { bounds: b, ticks: t, secondaryGroups: s };
    } catch (err) {
      console.error('TimelineAxis bounds calculation error:', err);
      return { bounds: null, ticks: [], secondaryGroups: [] };
    }
  }, [startDate, endDate, unit, config]);

  if (!bounds || ticks.length === 0) {
    return null;
  }

  const { totalWidth } = bounds;

  return (
    <div
      className={`gantt-timeline-axis-wrapper ${className}`}
      style={{
        overflow: 'visible',
        width: totalWidth,
        ...style,
      }}
      role="region"
      aria-label={`Trục thời gian Gantt theo đơn vị ${unit === 'week' ? 'Tuần' : 'Ngày'}`}
    >
      <svg
        className="gantt-timeline-svg"
        width={totalWidth}
        height={totalSvgHeight}
        style={{
          display: 'block',
          overflow: 'visible',
          userSelect: 'none',
        }}
      >
        {/* 1. Background Rect for Header */}
        <rect
          x={0}
          y={0}
          width={totalWidth}
          height={height}
          fill="#f8fafc"
          className="gantt-timeline-bg"
        />

        {/* 2. Top Tier: Secondary Grouping (Month / Year) */}
        <g className="gantt-timeline-secondary-tier">
          {secondaryGroups.map((group, idx) => (
            <g key={group.id} className="gantt-timeline-month-group">
              {/* Month cell background */}
              <rect
                x={group.x}
                y={0}
                width={group.width}
                height={secondaryHeight}
                fill={idx % 2 === 0 ? '#f1f5f9' : '#f8fafc'}
                stroke="#cbd5e1"
                strokeWidth={1}
              />
              {/* Month / Year Label */}
              <text
                x={group.x + group.width / 2}
                y={secondaryHeight / 2 + 4}
                textAnchor="middle"
                fontSize={11}
                fontWeight={600}
                fill="#334155"
                fontFamily="system-ui, -apple-system, sans-serif"
              >
                {group.label}
              </text>
            </g>
          ))}
          {/* Divider between secondary and primary tiers */}
          <line
            x1={0}
            y1={secondaryHeight}
            x2={totalWidth}
            y2={secondaryHeight}
            stroke="#cbd5e1"
            strokeWidth={1}
          />
        </g>

        {/* 3. Bottom Tier: Primary Ticks (Days or Weeks) */}
        <g className="gantt-timeline-primary-tier">
          {ticks.map((tick) => (
            <g key={tick.id} className="gantt-timeline-tick">
              {/* Cell separator border */}
              <line
                x1={tick.x}
                y1={secondaryHeight}
                x2={tick.x}
                y2={height}
                stroke={tick.isMajor ? '#94a3b8' : '#e2e8f0'}
                strokeWidth={tick.isMajor ? 1.5 : 1}
              />

              {/* Tick Primary Label (Day number '01' or Week number 'T.36') */}
              <text
                x={tick.x + tick.width / 2}
                y={secondaryHeight + primaryHeight / 2 + 4}
                textAnchor="middle"
                fontSize={unit === 'week' ? 11 : 11}
                fontWeight={tick.isMajor ? 600 : 500}
                fill={tick.isMajor ? '#1e293b' : '#64748b'}
                fontFamily="system-ui, -apple-system, sans-serif"
              >
                {tick.primaryLabel}
              </text>

              {/* Optional: Extended vertical grid line downwards into chart body (for T-31) */}
              {gridHeight > 0 && (
                <line
                  x1={tick.x}
                  y1={height}
                  x2={tick.x}
                  y2={totalSvgHeight}
                  stroke={tick.isMajor ? '#cbd5e1' : '#f1f5f9'}
                  strokeDasharray={tick.isMajor ? 'none' : '3,3'}
                  strokeWidth={1}
                  className="gantt-timeline-body-gridline"
                />
              )}
            </g>
          ))}

          {/* Right boundary closing tick line */}
          <line
            x1={totalWidth}
            y1={secondaryHeight}
            x2={totalWidth}
            y2={height}
            stroke="#cbd5e1"
            strokeWidth={1}
          />
        </g>

        {/* Bottom border of the timeline header */}
        <line
          x1={0}
          y1={height}
          x2={totalWidth}
          y2={height}
          stroke="#cbd5e1"
          strokeWidth={1}
          className="gantt-timeline-bottom-border"
        />
      </svg>
    </div>
  );
};

export default TimelineAxis;
