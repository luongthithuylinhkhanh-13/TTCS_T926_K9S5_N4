import assert from 'node:assert/strict';
import test from 'node:test';
import {
  TIMELINE_CONFIG,
  parseDate,
  dayNumber,
  diffInDays,
  addDays,
  getMonday,
  formatDate,
  dateToX,
  xToDate,
  calculateTimelineBounds,
  generateTimelineTicks,
  generateSecondaryGroups,
} from '../src/utils/timelineCoordinate.js';

test('1. Day mode coordinate conversion', () => {
  const baseDate = '2026-09-01';

  // Same day -> x = 0
  assert.equal(dateToX('2026-09-01', baseDate, 'day'), 0);

  // +1 day -> x = 40 (DAY_WIDTH = 40)
  assert.equal(dateToX('2026-09-02', baseDate, 'day'), 40);

  // +10 days -> x = 400
  assert.equal(dateToX('2026-09-11', baseDate, 'day'), 400);

  // Custom config
  const customConfig = { DAY_WIDTH: 50, WEEK_WIDTH: 200 };
  assert.equal(dateToX('2026-09-03', baseDate, 'day', customConfig), 100);
});

test('2. Week mode coordinate conversion & fractional positions', () => {
  // 2026-09-28 is a Monday
  const mondayOrigin = '2026-09-28';

  // Monday origin -> x = 0
  assert.equal(dateToX('2026-09-28', mondayOrigin, 'week'), 0);

  // Tuesday (+1 day) -> x = 20 (140 / 7 = 20 px/day)
  assert.equal(dateToX('2026-09-29', mondayOrigin, 'week'), 20);

  // Wednesday (+2 days) -> x = 40
  assert.equal(dateToX('2026-09-30', mondayOrigin, 'week'), 40);

  // Thursday (+3 days) -> x = 60
  assert.equal(dateToX('2026-10-01', mondayOrigin, 'week'), 60);

  // Friday (+4 days) -> x = 80
  assert.equal(dateToX('2026-10-02', mondayOrigin, 'week'), 80);

  // Saturday (+5 days) -> x = 100
  assert.equal(dateToX('2026-10-03', mondayOrigin, 'week'), 100);

  // Sunday (+6 days) -> x = 120
  assert.equal(dateToX('2026-10-04', mondayOrigin, 'week'), 120);

  // 3.5 days (midday Thursday) -> x = 70
  const middayThu = addDays(mondayOrigin, 3.5);
  assert.equal(dateToX(middayThu, mondayOrigin, 'week'), 70);

  // Next Monday (+7 days) -> x = 140
  assert.equal(dateToX('2026-10-05', mondayOrigin, 'week'), 140);

  // +14 days (2 weeks) -> x = 280
  assert.equal(dateToX('2026-10-12', mondayOrigin, 'week'), 280);

  // Does NOT snap all days of week to same X
  assert.notEqual(
    dateToX('2026-09-29', mondayOrigin, 'week'),
    dateToX('2026-09-30', mondayOrigin, 'week')
  );
});

test('3. Roundtrip conversion (dateToX <-> xToDate)', () => {
  const baseDate = '2026-09-01';

  // Day mode roundtrip
  const dayTestCoordinates = [0, 40, 80, 200, 480, 1200];
  for (const x of dayTestCoordinates) {
    const calculatedDate = xToDate(x, baseDate, 'day');
    const backToX = dateToX(calculatedDate, baseDate, 'day');
    assert(
      Math.abs(backToX - x) <= 0.001,
      `Day mode roundtrip failed for x=${x}: got backToX=${backToX}`
    );
  }

  // Week mode roundtrip
  const mondayOrigin = '2026-09-28';
  const weekTestCoordinates = [0, 20, 40, 70, 140, 280, 700];
  for (const x of weekTestCoordinates) {
    const calculatedDate = xToDate(x, mondayOrigin, 'week');
    const backToX = dateToX(calculatedDate, mondayOrigin, 'week');
    assert(
      Math.abs(backToX - x) <= 0.001,
      `Week mode roundtrip failed for x=${x}: got backToX=${backToX}`
    );
  }
});

test('4. Calendar boundary immunity & Date arithmetic', () => {
  // Month boundary: Sep 30 -> Oct 01 = exactly 1 day
  assert.equal(diffInDays('2026-09-30', '2026-10-01'), 1);
  assert.equal(addDays('2026-09-30', 1), '2026-10-01');

  // Year boundary: Dec 31 -> Jan 01 = exactly 1 day
  assert.equal(diffInDays('2026-12-31', '2027-01-01'), 1);
  assert.equal(addDays('2026-12-31', 1), '2027-01-01');

  // Leap year 2028: Feb 28 -> Mar 01 = exactly 2 days (due to Feb 29)
  assert.equal(diffInDays('2028-02-28', '2028-03-01'), 2);
  assert.equal(addDays('2028-02-28', 1), '2028-02-29');
  assert.equal(addDays('2028-02-28', 2), '2028-03-01');

  // Non-leap year 2026: Feb 28 -> Mar 01 = exactly 1 day
  assert.equal(diffInDays('2026-02-28', '2026-03-01'), 1);
});

test('5. getMonday() and week origin alignment', () => {
  // 2026-09-28 is Monday -> getMonday is 2026-09-28
  assert.equal(getMonday('2026-09-28'), '2026-09-28');

  // 2026-09-30 is Wednesday -> getMonday is 2026-09-28
  assert.equal(getMonday('2026-09-30'), '2026-09-28');

  // 2026-10-04 is Sunday -> getMonday is 2026-09-28
  assert.equal(getMonday('2026-10-04'), '2026-09-28');

  // 2026-10-05 is next Monday -> getMonday is 2026-10-05
  assert.equal(getMonday('2026-10-05'), '2026-10-05');
});

test('6. calculateTimelineBounds() consistency for Day and Week modes', () => {
  // Day mode for 2026-09-01 to 2026-09-10 (10 days)
  const dayBounds = calculateTimelineBounds('2026-09-01', '2026-09-10', 'day');
  assert.equal(dayBounds.timelineStart, '2026-09-01');
  assert.equal(dayBounds.timelineEnd, '2026-09-10');
  assert.equal(dayBounds.totalDays, 10);
  assert.equal(dayBounds.totalWidth, 10 * 40); // 400px

  // Week mode: startDate = Wednesday 2026-09-30, endDate = Friday 2026-10-09
  // Week containing startDate starts Monday 2026-09-28
  // Week containing endDate ends Sunday 2026-10-11
  // Total 14 days (2 weeks)
  const weekBounds = calculateTimelineBounds('2026-09-30', '2026-10-09', 'week');
  assert.equal(weekBounds.timelineStart, '2026-09-28'); // Monday
  assert.equal(weekBounds.timelineEnd, '2026-10-11'); // Sunday
  assert.equal(weekBounds.totalDays, 14);
  assert.equal(weekBounds.totalWidth, 2 * 140); // 280px

  // Guarantee that dateToX with weekBounds.timelineStart matches
  const targetX = dateToX('2026-09-30', weekBounds.timelineStart, 'week');
  // 2026-09-30 is Wednesday (+2 days from Monday 2026-09-28) -> 2 * 20 = 40px
  assert.equal(targetX, 40);
});

test('7. Timeline Ticks generation (generateTimelineTicks)', () => {
  // 30 days in Day mode (2026-09-01 to 2026-09-30)
  const dayTicks = generateTimelineTicks('2026-09-01', '2026-09-30', 'day');
  assert.equal(dayTicks.length, 30);

  // Check monotonic increasing x
  for (let i = 0; i < dayTicks.length; i++) {
    assert.equal(dayTicks[i].x, i * 40);
    assert.equal(dayTicks[i].width, 40);
    assert.equal(dayTicks[i].primaryLabel, String(i + 1).padStart(2, '0'));
  }
  // First day is major
  assert.equal(dayTicks[0].isMajor, true);

  // Week mode (30 days span from 2026-09-01 Tuesday to 2026-09-30 Wednesday)
  // Monday of 2026-09-01 is 2026-08-31
  // Sunday of 2026-09-30 is 2026-10-04
  // 2026-08-31 to 2026-10-04 is 35 days = 5 weeks
  const weekTicks = generateTimelineTicks('2026-09-01', '2026-09-30', 'week');
  assert.equal(weekTicks.length, 5);
  for (let i = 0; i < weekTicks.length; i++) {
    assert.equal(weekTicks[i].x, i * 140);
    assert.equal(weekTicks[i].width, 140);
    assert(weekTicks[i].primaryLabel.startsWith('T.'));
  }

  // Secondary grouping (Months)
  const secondaryGroups = generateSecondaryGroups(dayTicks);
  assert.equal(secondaryGroups.length, 1);
  assert.equal(secondaryGroups[0].label, 'Tháng 09/2026');
  assert.equal(secondaryGroups[0].width, 30 * 40);
});

test('8. Edge cases: same date and invalid input rejection', () => {
  // Same date (startDate === endDate)
  const sameDateBounds = calculateTimelineBounds('2026-09-01', '2026-09-01', 'day');
  assert.equal(sameDateBounds.totalDays, 1);
  assert.equal(sameDateBounds.totalWidth, 40);
  assert(!Number.isNaN(sameDateBounds.totalWidth));
  assert(Number.isFinite(sameDateBounds.totalWidth));

  // Invalid date inputs throw descriptive error
  assert.throws(() => parseDate(null), /Invalid date input/);
  assert.throws(() => parseDate(undefined), /Invalid date input/);
  assert.throws(() => parseDate(''), /Invalid date input/);
  assert.throws(() => parseDate('not-a-date'), /Invalid date input/);
  assert.throws(() => parseDate('2026-02-31'), /Invalid date input/); // non-existent date

  // End date before start date
  assert.throws(
    () => calculateTimelineBounds('2026-09-10', '2026-09-01', 'day'),
    /endDate .* is before startDate/
  );
});
