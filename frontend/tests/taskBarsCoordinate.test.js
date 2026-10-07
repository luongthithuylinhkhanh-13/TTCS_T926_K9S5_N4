import assert from 'node:assert/strict';
import test from 'node:test';
import {
  TIMELINE_CONFIG,
  calculateTimelineBounds,
  computeTaskBarGeometry,
  isCycleError,
} from '../src/utils/timelineCoordinate.js';
import { generateBenchmarkTasks } from '../src/benchmark/benchmarkData.js';

test('1. Day mode task bar geometry calculation', () => {
  // Timeline from 2026-09-01 to 2026-09-30 (Day mode: DAY_WIDTH = 40px)
  const bounds = calculateTimelineBounds('2026-09-01', '2026-09-30', 'day');

  // Task 1: 1 day duration (2026-09-01 to 2026-09-01) at row 0
  const task1 = {
    id: 't-1',
    startDate: '2026-09-01',
    endDate: '2026-09-01',
    progress: 50,
    isCritical: false,
  };
  const geom1 = computeTaskBarGeometry(task1, bounds, 0);
  assert(geom1 != null);
  assert.equal(geom1.x, 0);
  assert.equal(geom1.width, 40); // 1 day * 40px
  assert.equal(geom1.y, 7); // row 0: 0 * 36 + 7
  assert.equal(geom1.height, 22);
  assert.equal(geom1.progressWidth, 20); // 50% of 40px
  assert.equal(geom1.isCritical, false);

  // Task 2: 3 days duration (2026-09-03 to 2026-09-05) at row 1
  const task2 = {
    id: 't-2',
    startDate: '2026-09-03',
    endDate: '2026-09-05',
    progress: 100,
    isCritical: true,
  };
  const geom2 = computeTaskBarGeometry(task2, bounds, 1);
  assert(geom2 != null);
  assert.equal(geom2.x, 2 * 40); // +2 days from Sep 1 = 80px
  assert.equal(geom2.width, 3 * 40); // 3 days * 40 = 120px
  assert.equal(geom2.y, 36 + 7); // row 1: 36 + 7 = 43px
  assert.equal(geom2.progressWidth, 120); // 100% of 120px
  assert.equal(geom2.isCritical, true);

  // Task 3: 7 days duration (2026-09-01 to 2026-09-07)
  const task3 = {
    id: 't-3',
    startDate: '2026-09-01',
    endDate: '2026-09-07',
  };
  const geom3 = computeTaskBarGeometry(task3, bounds, 2);
  assert(geom3 != null);
  assert.equal(geom3.width, 7 * 40); // 280px
});

test('2. Week mode task bar geometry & Monday alignment', () => {
  // Timeline: startDate = Wednesday 2026-09-30 to 2026-10-15
  // Monday of 2026-09-30 is Monday 2026-09-28
  const bounds = calculateTimelineBounds('2026-09-30', '2026-10-15', 'week');
  assert.equal(bounds.timelineStart, '2026-09-28'); // Aligned to Monday
  assert.equal(bounds.dayWidth, 20); // 140 / 7 = 20px/day

  // Task on Monday 2026-09-28 (1 day)
  const taskMon = {
    id: 't-mon',
    startDate: '2026-09-28',
    endDate: '2026-09-28',
  };
  const geomMon = computeTaskBarGeometry(taskMon, bounds, 0);
  assert(geomMon != null);
  assert.equal(geomMon.x, 0);
  assert.equal(geomMon.width, 20); // 1 day = 20px

  // Task starting on Wednesday 2026-09-30 (3 days: Wed, Thu, Fri -> ends 2026-10-02)
  // Wednesday is +2 days from Monday 2026-09-28 -> x = 2 * 20 = 40px
  const taskWed = {
    id: 't-wed',
    startDate: '2026-09-30',
    endDate: '2026-10-02',
  };
  const geomWed = computeTaskBarGeometry(taskWed, bounds, 1);
  assert(geomWed != null);
  assert.equal(geomWed.x, 40); // Does NOT snap to Monday 0px!
  assert.equal(geomWed.width, 3 * 20); // 60px

  // Task 7 days (1 full week: 2026-09-28 to 2026-10-04)
  const taskWeek = {
    id: 't-week',
    startDate: '2026-09-28',
    endDate: '2026-10-04',
  };
  const geomWeek = computeTaskBarGeometry(taskWeek, bounds, 2);
  assert(geomWeek != null);
  assert.equal(geomWeek.width, 140); // Exactly 140px in week mode
});

test('3. Month and Year boundary crossing', () => {
  const bounds = calculateTimelineBounds('2026-09-20', '2027-01-15', 'day');

  // Month boundary: Sep 28 to Oct 05 (8 calendar days: Sep 28, 29, 30, Oct 1, 2, 3, 4, 5)
  const taskMonthCross = {
    id: 't-cross-month',
    startDate: '2026-09-28',
    endDate: '2026-10-05',
  };
  const geomMonthCross = computeTaskBarGeometry(taskMonthCross, bounds, 0);
  assert(geomMonthCross != null);
  assert.equal(geomMonthCross.width, 8 * 40); // 320px
  assert(Number.isFinite(geomMonthCross.x));
  assert(Number.isFinite(geomMonthCross.width));

  // Year boundary: Dec 28 to Jan 05 (9 calendar days: Dec 28, 29, 30, 31, Jan 1, 2, 3, 4, 5)
  const taskYearCross = {
    id: 't-cross-year',
    startDate: '2026-12-28',
    endDate: '2027-01-05',
  };
  const geomYearCross = computeTaskBarGeometry(taskYearCross, bounds, 1);
  assert(geomYearCross != null);
  assert.equal(geomYearCross.width, 9 * 40); // 360px
  assert(Number.isFinite(geomYearCross.x));
  assert(Number.isFinite(geomYearCross.width));
});

test('4. Unscheduled and invalid tasks handling', () => {
  const bounds = calculateTimelineBounds('2026-09-01', '2026-09-30', 'day');

  // Null or missing task
  assert.equal(computeTaskBarGeometry(null, bounds, 0), null);
  assert.equal(computeTaskBarGeometry(undefined, bounds, 0), null);

  // Missing dates
  assert.equal(computeTaskBarGeometry({ id: 't-1' }, bounds, 0), null);
  assert.equal(computeTaskBarGeometry({ id: 't-2', startDate: '2026-09-01' }, bounds, 0), null);
  assert.equal(computeTaskBarGeometry({ id: 't-3', endDate: '2026-09-10' }, bounds, 0), null);

  // End date before start date
  assert.equal(
    computeTaskBarGeometry(
      { id: 't-4', startDate: '2026-09-10', endDate: '2026-09-01' },
      bounds,
      0
    ),
    null
  );

  // Invalid date strings
  assert.equal(
    computeTaskBarGeometry(
      { id: 't-5', startDate: 'invalid', endDate: 'invalid' },
      bounds,
      0
    ),
    null
  );
});

test('5. 500 tasks benchmark dataset performance and finiteness', () => {
  // Generate 500 deterministic tasks from T-29 benchmark data
  const rawTasks = generateBenchmarkTasks(500);
  assert.equal(rawTasks.length, 500);

  // Bounds covering the tasks range
  const bounds = calculateTimelineBounds('2026-09-01', '2026-12-31', 'day');

  // Calculate geometry for all 500 tasks
  const startTime = Date.now();
  for (let i = 0; i < rawTasks.length; i++) {
    const geom = computeTaskBarGeometry(rawTasks[i], bounds, i);
    assert(geom != null, `Task ${i} should produce valid geometry`);
    assert(Number.isFinite(geom.x), `Task ${i} x must be finite`);
    assert(Number.isFinite(geom.y), `Task ${i} y must be finite`);
    assert(Number.isFinite(geom.width), `Task ${i} width must be finite`);
    assert(Number.isFinite(geom.height), `Task ${i} height must be finite`);
    assert(geom.width > 0, `Task ${i} width must be positive`);
    assert.equal(geom.y, i * 36 + 7);
  }
  const duration = Date.now() - startTime;
  // Verify calculation completes quickly without memory leaks
  assert(duration < 200, `Calculating 500 task geometries should take < 200ms (took ${duration}ms)`);
});

test('6. T-25 Cycle dependency error detection contract', () => {
  // Real backend message from CpmEngine.java line 119
  assert.equal(isCycleError('Quan hệ tiền nhiệm tạo thành vòng lặp'), true);

  // Backend message from TaskDependencyService.java line 34
  assert.equal(
    isCycleError('Không thể thiết lập quan hệ vì sẽ tạo thành vòng lặp tuần hoàn (Cyclic Dependency).'),
    true
  );

  // Error object with message
  assert.equal(
    isCycleError(new Error('Quan hệ tiền nhiệm tạo thành vòng lặp')),
    true
  );

  // Unrelated errors
  assert.equal(isCycleError('Dự án không tồn tại'), false);
  assert.equal(isCycleError('Network error (500)'), false);
  assert.equal(isCycleError(null), false);
  assert.equal(isCycleError(undefined), false);
});
