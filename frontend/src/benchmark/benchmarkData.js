/**
 * Benchmark constants and configuration for T-29 spike.
 */
export const LAYOUT = {
  TOTAL_TASKS: 500,
  ROW_HEIGHT: 24,
  BAR_HEIGHT: 16,
  BAR_Y_OFFSET: 4, // (ROW_HEIGHT - BAR_HEIGHT) / 2
  SCALE: 4, // px per time unit
};

/**
 * Generates a deterministic dataset of 500 tasks.
 * No randomness — reproducible on all runs and devices.
 * Each task has: id, label, start, duration, row
 *
 * @param {number} count
 * @returns {Array<{id: string, label: string, start: number, duration: number, row: number}>}
 */
export function generateBenchmarkTasks(count = LAYOUT.TOTAL_TASKS) {
  const tasks = [];
  for (let i = 0; i < count; i++) {
    tasks.push({
      id: `bench-${i}`,
      label: `Task ${i + 1}`,
      start: (i * 7 + (i % 13) * 3) % 200,
      duration: 5 + (i % 20),
      row: i,
    });
  }
  return tasks;
}

/**
 * Computes pixel rectangles from task data.
 * Shared between SVG and Canvas to guarantee identical geometry.
 *
 * @param {Array<{id: string, start: number, duration: number, row: number}>} tasks
 * @param {typeof LAYOUT} layout
 * @returns {Array<{id: string, x: number, y: number, width: number, height: number, row: number}>}
 */
export function computeLayout(tasks, layout = LAYOUT) {
  return tasks.map(task => ({
    id: task.id,
    x: task.start * layout.SCALE,
    y: task.row * layout.ROW_HEIGHT + layout.BAR_Y_OFFSET,
    width: task.duration * layout.SCALE,
    height: layout.BAR_HEIGHT,
    row: task.row,
  }));
}

/**
 * Computes overall chart bounding dimensions.
 *
 * @param {Array<{x: number, width: number}>} bars
 * @param {typeof LAYOUT} layout
 * @returns {{width: number, height: number}}
 */
export function computeChartSize(bars, layout = LAYOUT) {
  const maxWidth = bars.length > 0
    ? Math.max(...bars.map(b => b.x + b.width)) + 30
    : 1000;
  const height = layout.TOTAL_TASKS * layout.ROW_HEIGHT;
  return { width: Math.max(maxWidth, 900), height };
}
