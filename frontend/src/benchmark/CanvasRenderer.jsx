import React, { useRef, useEffect } from 'react';

/**
 * Canvas 2D Renderer for 500 Gantt bars.
 * Renders 500 bars on a single HTML5 <canvas> element.
 */
const CanvasRenderer = ({ bars, chartWidth, chartHeight, highlightIndex = -1 }) => {
  const canvasRef = useRef(null);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;

    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    // Clear buffer
    ctx.clearRect(0, 0, chartWidth, chartHeight);
    ctx.fillStyle = '#ffffff';
    ctx.fillRect(0, 0, chartWidth, chartHeight);

    // Draw 500 bars
    for (let i = 0; i < bars.length; i++) {
      const bar = bars[i];
      ctx.fillStyle = i === highlightIndex ? '#ef4444' : '#3b82f6';

      // Rounded rectangle for matching visual appearance
      if (ctx.roundRect) {
        ctx.beginPath();
        ctx.roundRect(bar.x, bar.y, bar.width, bar.height, 3);
        ctx.fill();
      } else {
        ctx.fillRect(bar.x, bar.y, bar.width, bar.height);
      }
    }
  }, [bars, chartWidth, chartHeight, highlightIndex]);

  return (
    <canvas
      ref={canvasRef}
      width={chartWidth}
      height={chartHeight}
      style={{ display: 'block', backgroundColor: '#ffffff' }}
      aria-label="Canvas Gantt Benchmark"
    />
  );
};

export default CanvasRenderer;
