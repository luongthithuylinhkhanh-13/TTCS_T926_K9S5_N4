import test from 'node:test';
import assert from 'node:assert/strict';
import { buildScheduleTimeline } from './scheduleTimeline.js';

test('aligns baseline and current bars on the same inclusive date axis', () => {
  const timeline = buildScheduleTimeline(
    [{
      id: 'task-1',
      wbsCode: '1.1',
      name: 'Foundation',
      startDate: '2026-10-05',
      endDate: '2026-10-10'
    }],
    [{
      itemId: 'task-1',
      wbsCode: '1.1',
      name: 'Foundation',
      startDate: '2026-10-01',
      endDate: '2026-10-03'
    }]
  );

  assert.equal(timeline.rows.length, 1);
  assert.equal(timeline.rows[0].baselineBar.left, 0);
  assert.equal(timeline.rows[0].baselineBar.width, (3 / 31) * 100);
  assert.equal(timeline.rows[0].currentBar.left, (4 / 31) * 100);
  assert.equal(timeline.rows[0].currentBar.width, (6 / 31) * 100);
});

test('keeps deleted tasks visible from their baseline snapshot', () => {
  const timeline = buildScheduleTimeline([], [{
    itemId: 'removed-task',
    wbsCode: '2.1',
    name: 'Removed task',
    startDate: '2026-10-01',
    endDate: '2026-10-02'
  }]);

  assert.equal(timeline.rows.length, 1);
  assert.equal(timeline.rows[0].name, 'Removed task');
  assert.equal(timeline.rows[0].currentBar, null);
  assert.ok(timeline.rows[0].baselineBar);
});

test('ignores tasks with missing or invalid date ranges', () => {
  const timeline = buildScheduleTimeline([
    { id: 'undated', wbsCode: '1.1', name: 'Undated' },
    {
      id: 'invalid',
      wbsCode: '1.2',
      name: 'Invalid range',
      startDate: '2026-10-10',
      endDate: '2026-10-01'
    }
  ], []);

  assert.deepEqual(timeline.rows, []);
  assert.equal(timeline.totalDays, 0);
});
