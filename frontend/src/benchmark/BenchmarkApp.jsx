import React, { useState, useRef, useMemo, useEffect, useCallback } from 'react';
import {
  LAYOUT,
  generateBenchmarkTasks,
  computeLayout,
  computeChartSize,
} from './benchmarkData';
import SvgRenderer from './SvgRenderer';
import CanvasRenderer from './CanvasRenderer';

const WARM_UP_COUNT = 3;
const MEASURE_RUNS = 10;

// Helper to wait 2 consecutive animation frames to ensure layout & paint
const nextFrame = () =>
  new Promise((resolve) =>
    requestAnimationFrame(() => requestAnimationFrame(resolve))
  );

function computeStats(times) {
  if (!times || times.length === 0) {
    return { min: 0, max: 0, median: 0, average: 0, raw: [] };
  }
  const sorted = [...times].sort((a, b) => a - b);
  const sum = times.reduce((acc, t) => acc + t, 0);
  const mid = Math.floor(sorted.length / 2);
  const median =
    sorted.length % 2 !== 0
      ? sorted[mid]
      : (sorted[mid - 1] + sorted[mid]) / 2;

  return {
    min: Number(sorted[0].toFixed(2)),
    max: Number(sorted[sorted.length - 1].toFixed(2)),
    median: Number(median.toFixed(2)),
    average: Number((sum / times.length).toFixed(2)),
    raw: times.map((t) => Number(t.toFixed(2))),
  };
}

const BenchmarkApp = () => {
  const tasks = useMemo(() => generateBenchmarkTasks(LAYOUT.TOTAL_TASKS), []);
  const bars = useMemo(() => computeLayout(tasks), [tasks]);
  const chartSize = useMemo(() => computeChartSize(bars), [bars]);

  const [activeRenderer, setActiveRenderer] = useState('svg'); // 'svg' | 'canvas' | null
  const [highlightIndex, setHighlightIndex] = useState(-1);
  const [isRunning, setIsRunning] = useState(false);
  const [statusMessage, setStatusMessage] = useState('Sẵn sàng thực hiện benchmark.');

  // Results state
  const [results, setResults] = useState({
    svg: {
      render: null,
      scroll: null,
      interaction: null,
      domNodes: null,
      memoryMb: null,
    },
    canvas: {
      render: null,
      scroll: null,
      interaction: null,
      domNodes: null,
      memoryMb: null,
    },
  });

  const scrollContainerRef = useRef(null);

  // Measure DOM nodes & Memory
  const getEnvironmentMetrics = useCallback((type) => {
    let domNodes = 0;
    if (type === 'svg') {
      domNodes = document.querySelectorAll('svg rect').length;
    } else if (type === 'canvas') {
      domNodes = document.querySelectorAll('canvas').length;
    }

    let memoryMb = null;
    if (window.performance && window.performance.memory) {
      memoryMb = Number(
        (window.performance.memory.usedJSHeapSize / (1024 * 1024)).toFixed(2)
      );
    }
    return { domNodes, memoryMb };
  }, []);

  // 1. Initial Render Benchmark
  const runRenderBenchmark = async (type) => {
    setStatusMessage(`Đang benchmark Render [${type.toUpperCase()}] (${WARM_UP_COUNT} warm-up + ${MEASURE_RUNS} runs)...`);
    const measuredTimes = [];

    for (let i = 0; i < WARM_UP_COUNT + MEASURE_RUNS; i++) {
      // Force unmount
      setActiveRenderer(null);
      await nextFrame();

      const t0 = performance.now();
      setActiveRenderer(type);
      await nextFrame();
      const t1 = performance.now();

      const duration = t1 - t0;
      if (i >= WARM_UP_COUNT) {
        measuredTimes.push(duration);
      }
    }

    const stats = computeStats(measuredTimes);
    const { domNodes, memoryMb } = getEnvironmentMetrics(type);

    setResults((prev) => ({
      ...prev,
      [type]: {
        ...prev[type],
        render: stats,
        domNodes,
        memoryMb,
      },
    }));

    setStatusMessage(`Hoàn tất Render [${type.toUpperCase()}]: Median = ${stats.median} ms`);
    return stats;
  };

  // 2. Scroll Smoothness Test
  const runScrollTest = async (type) => {
    setStatusMessage(`Đang đo Scroll Smoothness [${type.toUpperCase()}] trong 3 giây...`);
    setActiveRenderer(type);
    await nextFrame();

    const container = scrollContainerRef.current;
    if (!container) return;

    container.scrollTop = 0;
    const maxScroll = container.scrollHeight - container.clientHeight;
    const durationMs = 3000;
    const frameDeltas = [];

    return new Promise((resolve) => {
      const startTime = performance.now();
      let lastTime = startTime;

      function tick(now) {
        const delta = now - lastTime;
        frameDeltas.push(delta);
        lastTime = now;

        const progress = Math.min((now - startTime) / durationMs, 1);
        // Easing-free linear scroll
        container.scrollTop = maxScroll * progress;

        if (progress < 1) {
          requestAnimationFrame(tick);
        } else {
          // Discard first frame (warm-up anomaly)
          const validDeltas = frameDeltas.slice(1);
          const avgDelta =
            validDeltas.reduce((a, b) => a + b, 0) / validDeltas.length;
          const avgFps = Number((1000 / avgDelta).toFixed(1));
          const jankFrames = validDeltas.filter((d) => d > 33.33).length; // >33.33ms = drops below 30 FPS

          const scrollResult = {
            avgFps,
            jankFrames,
            totalFrames: validDeltas.length,
          };

          setResults((prev) => ({
            ...prev,
            [type]: {
              ...prev[type],
              scroll: scrollResult,
            },
          }));

          setStatusMessage(`Hoàn tất Scroll [${type.toUpperCase()}]: FPS trung bình = ${avgFps}, Jank = ${jankFrames} frames`);
          resolve(scrollResult);
        }
      }

      requestAnimationFrame(tick);
    });
  };

  // 3. Interaction Re-render Test
  const runInteractionTest = async (type) => {
    setStatusMessage(`Đang benchmark Interaction (highlight bar) [${type.toUpperCase()}]...`);
    setActiveRenderer(type);
    setHighlightIndex(-1);
    await nextFrame();

    const interactionRuns = [];
    for (let i = 0; i < 5; i++) {
      // Toggle highlight on bar 250
      const targetIndex = i % 2 === 0 ? 250 : -1;
      const t0 = performance.now();
      setHighlightIndex(targetIndex);
      await nextFrame();
      const t1 = performance.now();
      interactionRuns.push(t1 - t0);
    }

    const stats = computeStats(interactionRuns);
    setHighlightIndex(-1);

    setResults((prev) => ({
      ...prev,
      [type]: {
        ...prev[type],
        interaction: stats,
      },
    }));

    setStatusMessage(`Hoàn tất Interaction [${type.toUpperCase()}]: Median = ${stats.median} ms`);
    return stats;
  };

  // Run full benchmark suite for a specific renderer
  const runFullBenchmarkFor = async (type) => {
    setIsRunning(true);
    try {
      await runRenderBenchmark(type);
      await new Promise((r) => setTimeout(r, 200));
      await runScrollTest(type);
      await new Promise((r) => setTimeout(r, 200));
      await runInteractionTest(type);
    } finally {
      setIsRunning(false);
    }
  };

  // Run complete benchmark suite for BOTH SVG & Canvas sequentially
  const runFullSuite = async () => {
    setIsRunning(true);
    try {
      setStatusMessage('Bắt đầu chạy benchmark trọn bộ cho SVG và Canvas...');
      // 1. SVG
      await runRenderBenchmark('svg');
      await new Promise((r) => setTimeout(r, 300));
      await runScrollTest('svg');
      await new Promise((r) => setTimeout(r, 300));
      await runInteractionTest('svg');
      await new Promise((r) => setTimeout(r, 500));

      // 2. Canvas
      await runRenderBenchmark('canvas');
      await new Promise((r) => setTimeout(r, 300));
      await runScrollTest('canvas');
      await new Promise((r) => setTimeout(r, 300));
      await runInteractionTest('canvas');

      setStatusMessage('Hoàn tất trọn bộ benchmark cho cả SVG và Canvas!');
    } finally {
      setIsRunning(false);
    }
  };

  // Expose global runner for automated / remote execution if needed
  useEffect(() => {
    window.__runGanttBenchmarkSuite = runFullSuite;
    window.__getBenchmarkResults = () => results;
  }, [results]);

  return (
    <div style={styles.container}>
      {/* Header */}
      <header style={styles.header}>
        <div style={styles.titleRow}>
          <h1 style={styles.title}>T-29: Spike Benchmark SVG vs HTML Canvas (500 Bars)</h1>
          <span style={styles.badge}>Engineering Spike</span>
        </div>
        <p style={styles.subtitle}>
          Thử nghiệm hiệu năng render 500 task bars giữa SVG (&lt;rect&gt;) và HTML5 Canvas 2D.
          Dataset và Layout được đồng bộ hoàn toàn. Độc lập với production Gantt.
        </p>
      </header>

      {/* Control Toolbar */}
      <section style={styles.toolbar}>
        <div style={styles.buttonGroup}>
          <button
            style={{ ...styles.btn, ...styles.btnPrimary }}
            onClick={runFullSuite}
            disabled={isRunning}
          >
            {isRunning ? 'Đang chạy benchmark...' : 'Chạy toàn bộ Suite (SVG + Canvas)'}
          </button>
          <button
            style={styles.btn}
            onClick={() => runFullBenchmarkFor('svg')}
            disabled={isRunning}
          >
            Chạy Benchmark SVG
          </button>
          <button
            style={styles.btn}
            onClick={() => runFullBenchmarkFor('canvas')}
            disabled={isRunning}
          >
            Chạy Benchmark Canvas
          </button>
        </div>

        <div style={styles.viewToggleGroup}>
          <span style={styles.toggleLabel}>Xem trực tiếp:</span>
          <button
            style={{
              ...styles.toggleBtn,
              ...(activeRenderer === 'svg' ? styles.toggleBtnActive : {}),
            }}
            onClick={() => setActiveRenderer('svg')}
            disabled={isRunning}
          >
            SVG
          </button>
          <button
            style={{
              ...styles.toggleBtn,
              ...(activeRenderer === 'canvas' ? styles.toggleBtnActive : {}),
            }}
            onClick={() => setActiveRenderer('canvas')}
            disabled={isRunning}
          >
            Canvas
          </button>
          <button
            style={styles.toggleBtn}
            onClick={() => setHighlightIndex((prev) => (prev === 250 ? -1 : 250))}
          >
            {highlightIndex === 250 ? 'Bỏ Highlight' : 'Highlight Bar #250'}
          </button>
        </div>
      </section>

      {/* Status Banner */}
      <div style={styles.statusBar}>
        <strong>Trạng thái:</strong> {statusMessage}
      </div>

      {/* Results Comparison Grid */}
      <section style={styles.resultsSection}>
        <h2 style={styles.sectionTitle}>Bảng so sánh kết quả (500 Bars)</h2>
        <div style={styles.tableWrapper}>
          <table style={styles.table}>
            <thead>
              <tr style={styles.tableHeaderRow}>
                <th style={styles.th}>Chỉ số (Metric)</th>
                <th style={styles.th}>SVG (&lt;rect&gt;)</th>
                <th style={styles.th}>Canvas 2D</th>
                <th style={styles.th}>So sánh & Nhận xét</th>
              </tr>
            </thead>
            <tbody>
              {/* Initial Render */}
              <tr style={styles.tr}>
                <td style={styles.tdBold}>Initial Render - Median</td>
                <td style={styles.td}>
                  {results.svg.render ? `${results.svg.render.median} ms` : '--'}
                </td>
                <td style={styles.td}>
                  {results.canvas.render ? `${results.canvas.render.median} ms` : '--'}
                </td>
                <td style={styles.tdMuted}>
                  {results.svg.render && results.canvas.render
                    ? results.canvas.render.median < results.svg.render.median
                      ? `Canvas nhanh hơn ${(results.svg.render.median / results.canvas.render.median).toFixed(1)}x`
                      : `SVG tương đương hoặc nhanh hơn`
                    : 'Chưa có đủ số liệu'}
                </td>
              </tr>
              <tr style={styles.tr}>
                <td style={styles.tdSub}>Min / Average / Max (ms)</td>
                <td style={styles.td}>
                  {results.svg.render
                    ? `${results.svg.render.min} / ${results.svg.render.average} / ${results.svg.render.max}`
                    : '--'}
                </td>
                <td style={styles.td}>
                  {results.canvas.render
                    ? `${results.canvas.render.min} / ${results.canvas.render.average} / ${results.canvas.render.max}`
                    : '--'}
                </td>
                <td style={styles.tdMuted}>10 measurement runs (sau 3 warm-ups)</td>
              </tr>

              {/* Scroll Smoothness */}
              <tr style={styles.tr}>
                <td style={styles.tdBold}>Scroll Smoothness (FPS)</td>
                <td style={styles.td}>
                  {results.svg.scroll ? `${results.svg.scroll.avgFps} FPS` : '--'}
                </td>
                <td style={styles.td}>
                  {results.canvas.scroll ? `${results.canvas.scroll.avgFps} FPS` : '--'}
                </td>
                <td style={styles.tdMuted}>Đo trong 3s cuộn liên tục</td>
              </tr>
              <tr style={styles.tr}>
                <td style={styles.tdSub}>Jank Frames (&gt;33.3ms)</td>
                <td style={styles.td}>
                  {results.svg.scroll ? `${results.svg.scroll.jankFrames} / ${results.svg.scroll.totalFrames} frames` : '--'}
                </td>
                <td style={styles.td}>
                  {results.canvas.scroll ? `${results.canvas.scroll.jankFrames} / ${results.canvas.scroll.totalFrames} frames` : '--'}
                </td>
                <td style={styles.tdMuted}>Số khung hình rớt dưới 30 FPS</td>
              </tr>

              {/* Interaction Re-render */}
              <tr style={styles.tr}>
                <td style={styles.tdBold}>Interaction Re-render (Median)</td>
                <td style={styles.td}>
                  {results.svg.interaction ? `${results.svg.interaction.median} ms` : '--'}
                </td>
                <td style={styles.td}>
                  {results.canvas.interaction ? `${results.canvas.interaction.median} ms` : '--'}
                </td>
                <td style={styles.tdMuted}>Highlight 1 bar (cập nhật trạng thái)</td>
              </tr>

              {/* DOM Nodes */}
              <tr style={styles.tr}>
                <td style={styles.tdBold}>DOM Node Overhead</td>
                <td style={styles.td}>
                  {results.svg.domNodes != null ? `${results.svg.domNodes} nodes (<rect>)` : '--'}
                </td>
                <td style={styles.td}>
                  {results.canvas.domNodes != null ? `${results.canvas.domNodes} node (<canvas>)` : '--'}
                </td>
                <td style={styles.tdMuted}>SVG giữ 500 element DOM, Canvas giữ 1 element</td>
              </tr>

              {/* JS Heap Memory */}
              <tr style={styles.tr}>
                <td style={styles.tdBold}>JS Heap Memory (ước lượng)</td>
                <td style={styles.td}>
                  {results.svg.memoryMb != null ? `${results.svg.memoryMb} MB` : 'N/A'}
                </td>
                <td style={styles.td}>
                  {results.canvas.memoryMb != null ? `${results.canvas.memoryMb} MB` : 'N/A'}
                </td>
                <td style={styles.tdMuted}>Chrome performance.memory (best-effort)</td>
              </tr>
            </tbody>
          </table>
        </div>

        {/* Raw Times Section */}
        {(results.svg.render || results.canvas.render) && (
          <div style={styles.rawBox}>
            <h3 style={styles.rawTitle}>Dữ liệu chi tiết từng lần đo (Raw 10 runs):</h3>
            {results.svg.render && (
              <p style={styles.rawLine}>
                <strong>SVG runs (ms):</strong> [{results.svg.render.raw.join(', ')}]
              </p>
            )}
            {results.canvas.render && (
              <p style={styles.rawLine}>
                <strong>Canvas runs (ms):</strong> [{results.canvas.render.raw.join(', ')}]
              </p>
            )}
          </div>
        )}
      </section>

      {/* Live Chart Viewport */}
      <section style={styles.viewportSection}>
        <div style={styles.viewportHeader}>
          <h2 style={styles.sectionTitle}>
            Giao diện vẽ trực tiếp: {activeRenderer ? activeRenderer.toUpperCase() : 'None (Unmounted)'}
          </h2>
          <span style={styles.dimLabel}>
            Kích thước đồ thị: {chartSize.width}px x {chartSize.height}px (500 bars)
          </span>
        </div>

        <div
          ref={scrollContainerRef}
          style={styles.scrollContainer}
          id="benchmark-scroll-container"
        >
          {activeRenderer === 'svg' && (
            <SvgRenderer
              bars={bars}
              chartWidth={chartSize.width}
              chartHeight={chartSize.height}
              highlightIndex={highlightIndex}
            />
          )}
          {activeRenderer === 'canvas' && (
            <CanvasRenderer
              bars={bars}
              chartWidth={chartSize.width}
              chartHeight={chartSize.height}
              highlightIndex={highlightIndex}
            />
          )}
          {activeRenderer === null && (
            <div style={styles.unmountedState}>
              Đang unmounted để đo render thời gian thực...
            </div>
          )}
        </div>
      </section>
    </div>
  );
};

// Clean, scoped inline styling (zero external CSS dependencies)
const styles = {
  container: {
    fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif',
    backgroundColor: '#f8fafc',
    minHeight: '100vh',
    padding: '24px 32px',
    color: '#0f172a',
    boxSizing: 'border-box',
  },
  header: {
    marginBottom: '20px',
    borderBottom: '1px solid #e2e8f0',
    paddingBottom: '16px',
  },
  titleRow: {
    display: 'flex',
    alignItems: 'center',
    gap: '12px',
    marginBottom: '8px',
  },
  title: {
    fontSize: '22px',
    fontWeight: '700',
    margin: 0,
    color: '#1e293b',
  },
  badge: {
    fontSize: '12px',
    fontWeight: '600',
    backgroundColor: '#e0e7ff',
    color: '#4338ca',
    padding: '3px 10px',
    borderRadius: '12px',
  },
  subtitle: {
    margin: 0,
    fontSize: '14px',
    color: '#64748b',
    lineHeight: '1.5',
  },
  toolbar: {
    display: 'flex',
    flexWrap: 'wrap',
    gap: '16px',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: '16px',
    backgroundColor: '#ffffff',
    padding: '14px 20px',
    borderRadius: '8px',
    boxShadow: '0 1px 3px rgba(0,0,0,0.06)',
  },
  buttonGroup: {
    display: 'flex',
    gap: '10px',
    flexWrap: 'wrap',
  },
  btn: {
    padding: '8px 16px',
    fontSize: '13px',
    fontWeight: '600',
    backgroundColor: '#f1f5f9',
    color: '#334155',
    border: '1px solid #cbd5e1',
    borderRadius: '6px',
    cursor: 'pointer',
    transition: 'background-color 0.15s ease',
  },
  btnPrimary: {
    backgroundColor: '#2563eb',
    color: '#ffffff',
    borderColor: '#1d4ed8',
  },
  viewToggleGroup: {
    display: 'flex',
    alignItems: 'center',
    gap: '8px',
  },
  toggleLabel: {
    fontSize: '13px',
    fontWeight: '600',
    color: '#64748b',
  },
  toggleBtn: {
    padding: '6px 12px',
    fontSize: '12px',
    fontWeight: '600',
    backgroundColor: '#f8fafc',
    color: '#475569',
    border: '1px solid #cbd5e1',
    borderRadius: '4px',
    cursor: 'pointer',
  },
  toggleBtnActive: {
    backgroundColor: '#0f172a',
    color: '#ffffff',
    borderColor: '#0f172a',
  },
  statusBar: {
    backgroundColor: '#eff6ff',
    border: '1px solid #bfdbfe',
    color: '#1e40af',
    padding: '10px 16px',
    borderRadius: '6px',
    fontSize: '13px',
    marginBottom: '20px',
  },
  resultsSection: {
    backgroundColor: '#ffffff',
    borderRadius: '8px',
    padding: '20px',
    marginBottom: '24px',
    boxShadow: '0 1px 3px rgba(0,0,0,0.06)',
  },
  sectionTitle: {
    fontSize: '16px',
    fontWeight: '700',
    margin: '0 0 12px 0',
    color: '#1e293b',
  },
  tableWrapper: {
    overflowX: 'auto',
  },
  table: {
    width: '100%',
    borderCollapse: 'collapse',
    fontSize: '13px',
    textAlign: 'left',
  },
  tableHeaderRow: {
    backgroundColor: '#f8fafc',
    borderBottom: '2px solid #e2e8f0',
  },
  th: {
    padding: '10px 14px',
    fontWeight: '600',
    color: '#475569',
  },
  tr: {
    borderBottom: '1px solid #f1f5f9',
  },
  td: {
    padding: '10px 14px',
    color: '#1e293b',
    fontWeight: '500',
  },
  tdBold: {
    padding: '10px 14px',
    fontWeight: '600',
    color: '#0f172a',
  },
  tdSub: {
    padding: '6px 14px',
    color: '#64748b',
    fontSize: '12px',
  },
  tdMuted: {
    padding: '10px 14px',
    color: '#64748b',
    fontSize: '12px',
  },
  rawBox: {
    marginTop: '16px',
    backgroundColor: '#f8fafc',
    padding: '12px 16px',
    borderRadius: '6px',
    border: '1px solid #e2e8f0',
  },
  rawTitle: {
    fontSize: '12px',
    fontWeight: '700',
    margin: '0 0 6px 0',
    color: '#475569',
  },
  rawLine: {
    fontSize: '12px',
    margin: '4px 0',
    fontFamily: 'monospace',
    color: '#334155',
  },
  viewportSection: {
    backgroundColor: '#ffffff',
    borderRadius: '8px',
    padding: '20px',
    boxShadow: '0 1px 3px rgba(0,0,0,0.06)',
  },
  viewportHeader: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: '12px',
  },
  dimLabel: {
    fontSize: '12px',
    color: '#94a3b8',
  },
  scrollContainer: {
    height: '420px',
    overflowY: 'auto',
    overflowX: 'auto',
    border: '1px solid #cbd5e1',
    borderRadius: '6px',
    backgroundColor: '#f8fafc',
    position: 'relative',
  },
  unmountedState: {
    padding: '40px',
    textAlign: 'center',
    color: '#94a3b8',
    fontSize: '14px',
  },
};

export default BenchmarkApp;
