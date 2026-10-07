import React from 'react';

/**
 * SVG Renderer for 500 Gantt bars.
 * Renders 500 <rect> nodes in the SVG DOM tree.
 */
const SvgRenderer = ({ bars, chartWidth, chartHeight, highlightIndex = -1 }) => {
  return (
    <svg
      width={chartWidth}
      height={chartHeight}
      style={{ display: 'block', backgroundColor: '#ffffff' }}
      aria-label="SVG Gantt Benchmark"
    >
      {bars.map((bar, index) => (
        <rect
          key={bar.id}
          x={bar.x}
          y={bar.y}
          width={bar.width}
          height={bar.height}
          rx={3}
          ry={3}
          fill={index === highlightIndex ? '#ef4444' : '#3b82f6'}
        />
      ))}
    </svg>
  );
};

export default SvgRenderer;
