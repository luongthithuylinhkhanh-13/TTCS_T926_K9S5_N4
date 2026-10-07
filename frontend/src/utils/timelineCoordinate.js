/**
 * Pure JavaScript module for timeline coordinate conversions and date arithmetic.
 * Independent of React, DOM, or browser rendering APIs.
 */

export const TIMELINE_CONFIG = {
  DAY_WIDTH: 40,
  WEEK_WIDTH: 140,
  HEADER_HEIGHT: 56,
  SECONDARY_TIER_HEIGHT: 26,
  PRIMARY_TIER_HEIGHT: 30,
};

const MS_PER_DAY = 86400000;

/**
 * Validates and parses a date into a normalized UTC date representation.
 * Supports ISO date string ('YYYY-MM-DD' or full ISO string), Date object, or timestamp.
 *
 * @param {string|Date|number} input
 * @returns {{ year: number, month: number, day: number, timestamp: number }}
 */
export function parseDate(input) {
  if (input == null || input === '') {
    throw new Error('Invalid date input: input is null or empty');
  }

  let year;
  let month;
  let day;
  let timestamp;

  if (typeof input === 'string') {
    // Check standard YYYY-MM-DD pattern without time
    const ymdMatch = /^(\d{4})-(\d{2})-(\d{2})$/.exec(input.trim());
    if (ymdMatch) {
      year = parseInt(ymdMatch[1], 10);
      month = parseInt(ymdMatch[2], 10);
      day = parseInt(ymdMatch[3], 10);

      if (month < 1 || month > 12 || day < 1 || day > 31) {
        throw new Error(`Invalid date input: out-of-range values ${year}-${month}-${day}`);
      }

      timestamp = Date.UTC(year, month - 1, day);
      const checkDate = new Date(timestamp);
      if (
        checkDate.getUTCFullYear() !== year ||
        checkDate.getUTCMonth() !== month - 1 ||
        checkDate.getUTCDate() !== day
      ) {
        throw new Error(`Invalid date input: calendar date ${year}-${month}-${day} does not exist`);
      }
    } else {
      const parsed = new Date(input);
      if (isNaN(parsed.getTime())) {
        throw new Error(`Invalid date input: cannot parse "${input}"`);
      }
      year = parsed.getUTCFullYear();
      month = parsed.getUTCMonth() + 1;
      day = parsed.getUTCDate();
      timestamp = parsed.getTime();
    }
  } else if (input instanceof Date) {
    if (isNaN(input.getTime())) {
      throw new Error('Invalid date input: Date instance is NaN');
    }
    year = input.getUTCFullYear();
    month = input.getUTCMonth() + 1;
    day = input.getUTCDate();
    timestamp = input.getTime();
  } else if (typeof input === 'number') {
    if (!Number.isFinite(input)) {
      throw new Error('Invalid date input: timestamp is not finite');
    }
    const d = new Date(input);
    year = d.getUTCFullYear();
    month = d.getUTCMonth() + 1;
    day = d.getUTCDate();
    timestamp = d.getTime();
  } else {
    throw new Error('Invalid date input: unsupported type');
  }

  return { year, month, day, timestamp };
}

/**
 * Returns integer day index since Unix epoch in UTC.
 * Immune to client-side timezones and Daylight Saving Time (DST).
 *
 * @param {string|Date|number} date
 * @returns {number}
 */
export function dayNumber(date) {
  const { timestamp } = parseDate(date);
  return Math.floor(timestamp / MS_PER_DAY);
}

/**
 * Calculates calendar day difference between start and end date (end - start).
 * Returns integer for standard calendar dates, or exact float for sub-day timestamps.
 *
 * @param {string|Date|number} start
 * @param {string|Date|number} end
 * @returns {number}
 */
export function diffInDays(start, end) {
  const startMs = parseDate(start).timestamp;
  const endMs = parseDate(end).timestamp;
  return (endMs - startMs) / MS_PER_DAY;
}

/**
 * Adds (or subtracts) a number of days to/from a date.
 * If days is integer, returns 'YYYY-MM-DD'.
 * If days has a fractional part, returns ISO string with time.
 *
 * @param {string|Date|number} date
 * @param {number} days
 * @returns {string} ISO 'YYYY-MM-DD' or ISO string
 */
export function addDays(date, days) {
  const { timestamp } = parseDate(date);
  const nextTimestamp = timestamp + days * MS_PER_DAY;
  const d = new Date(nextTimestamp);

  // If the result aligns to UTC midnight, return clean YYYY-MM-DD
  if (nextTimestamp % MS_PER_DAY === 0) {
    const yyyy = d.getUTCFullYear();
    const mm = String(d.getUTCMonth() + 1).padStart(2, '0');
    const dd = String(d.getUTCDate()).padStart(2, '0');
    return `${yyyy}-${mm}-${dd}`;
  }

  return d.toISOString();
}

/**
 * Returns Monday of the week containing the given date.
 * ISO-8601 week starts on Monday.
 *
 * @param {string|Date|number} date
 * @returns {string} ISO 'YYYY-MM-DD'
 */
export function getMonday(date) {
  const { timestamp } = parseDate(date);
  const d = new Date(timestamp);
  // getUTCDay: 0 is Sunday, 1 is Monday, ..., 6 is Saturday
  const dayOfWeek = d.getUTCDay();
  const daysSinceMonday = (dayOfWeek + 6) % 7;
  // Floor to UTC midnight
  const midnightMs = Math.floor(timestamp / MS_PER_DAY) * MS_PER_DAY;
  const mondayMs = midnightMs - daysSinceMonday * MS_PER_DAY;
  const mondayDate = new Date(mondayMs);
  const yyyy = mondayDate.getUTCFullYear();
  const mm = String(mondayDate.getUTCMonth() + 1).padStart(2, '0');
  const dd = String(mondayDate.getUTCDate()).padStart(2, '0');
  return `${yyyy}-${mm}-${dd}`;
}

/**
 * Formats a date into a deterministic display string.
 *
 * @param {string|Date|number} date
 * @param {'DD/MM'|'YYYY-MM-DD'|'DD/MM/YYYY'|'MM/YYYY'|'MONTH_NAME'} [format='DD/MM']
 * @returns {string}
 */
export function formatDate(date, format = 'DD/MM') {
  const { year, month, day } = parseDate(date);
  const dd = String(day).padStart(2, '0');
  const mm = String(month).padStart(2, '0');
  const yyyy = String(year);

  switch (format) {
    case 'DD/MM':
      return `${dd}/${mm}`;
    case 'DD/MM/YYYY':
      return `${dd}/${mm}/${yyyy}`;
    case 'MM/YYYY':
      return `${mm}/${yyyy}`;
    case 'MONTH_NAME':
      return `Tháng ${mm}/${yyyy}`;
    case 'YYYY-MM-DD':
    default:
      return `${yyyy}-${mm}-${dd}`;
  }
}

/**
 * Calculates the ISO-8601 week number for a given date.
 *
 * @param {string|Date|number} date
 * @returns {number}
 */
export function getISOWeekNumber(date) {
  const { timestamp } = parseDate(date);
  const d = new Date(timestamp);
  // Set to nearest Thursday: current date + 4 - current day number (with Sunday as 7)
  const dayNr = (d.getUTCDay() + 6) % 7;
  d.setUTCDate(d.getUTCDate() - dayNr + 3);
  const firstThursday = d.getTime();
  d.setUTCMonth(0, 1);
  if (d.getUTCDay() !== 4) {
    d.setUTCMonth(0, 1 + ((4 - d.getUTCDay() + 7) % 7));
  }
  return 1 + Math.ceil((firstThursday - d.getTime()) / (7 * MS_PER_DAY));
}

/**
 * Converts a target date to horizontal pixel coordinate X.
 *
 * @param {string|Date|number} date Target date to place on timeline
 * @param {string|Date|number} baseDate Timeline coordinate origin (startDate or Monday-aligned timelineStart)
 * @param {'day'|'week'} [unit='day']
 * @param {typeof TIMELINE_CONFIG} [config=TIMELINE_CONFIG]
 * @returns {number} Horizontal coordinate in pixels
 */
export function dateToX(date, baseDate, unit = 'day', config = TIMELINE_CONFIG) {
  const deltaDays = diffInDays(baseDate, date);

  if (unit === 'week') {
    const weekWidth = config?.WEEK_WIDTH ?? TIMELINE_CONFIG.WEEK_WIDTH;
    return (deltaDays / 7) * weekWidth;
  }

  const dayWidth = config?.DAY_WIDTH ?? TIMELINE_CONFIG.DAY_WIDTH;
  return deltaDays * dayWidth;
}

/**
 * Converts a horizontal pixel coordinate X back to a calendar date.
 * Inverse of dateToX().
 *
 * @param {number} x Pixel coordinate
 * @param {string|Date|number} baseDate Timeline coordinate origin
 * @param {'day'|'week'} [unit='day']
 * @param {typeof TIMELINE_CONFIG} [config=TIMELINE_CONFIG]
 * @returns {string} ISO 'YYYY-MM-DD' or ISO timestamp string
 */
export function xToDate(x, baseDate, unit = 'day', config = TIMELINE_CONFIG) {
  if (typeof x !== 'number' || !Number.isFinite(x)) {
    throw new Error('Invalid x coordinate: must be a finite number');
  }

  let deltaDays;
  if (unit === 'week') {
    const weekWidth = config?.WEEK_WIDTH ?? TIMELINE_CONFIG.WEEK_WIDTH;
    deltaDays = (x / weekWidth) * 7;
  } else {
    const dayWidth = config?.DAY_WIDTH ?? TIMELINE_CONFIG.DAY_WIDTH;
    deltaDays = x / dayWidth;
  }

  return addDays(baseDate, deltaDays);
}

/**
 * Calculates timeline boundaries, total days, and total pixel width.
 * Used synchronously by TimelineAxis and T-31 TaskBars to guarantee matching origins and widths.
 *
 * @param {string|Date|number} startDate
 * @param {string|Date|number} endDate
 * @param {'day'|'week'} [unit='day']
 * @param {typeof TIMELINE_CONFIG} [config=TIMELINE_CONFIG]
 * @returns {{
 *   timelineStart: string,
 *   timelineEnd: string,
 *   totalDays: number,
 *   totalWidth: number,
 *   unit: 'day'|'week',
 *   unitWidth: number,
 *   dayWidth: number
 * }}
 */
export function calculateTimelineBounds(
  startDate,
  endDate,
  unit = 'day',
  config = TIMELINE_CONFIG
) {
  const normStart = formatDate(startDate, 'YYYY-MM-DD');
  const normEnd = formatDate(endDate, 'YYYY-MM-DD');

  if (diffInDays(normStart, normEnd) < 0) {
    throw new Error(`Invalid timeline range: endDate (${normEnd}) is before startDate (${normStart})`);
  }

  const dayWidth = config?.DAY_WIDTH ?? TIMELINE_CONFIG.DAY_WIDTH;
  const weekWidth = config?.WEEK_WIDTH ?? TIMELINE_CONFIG.WEEK_WIDTH;

  if (unit === 'week') {
    // In week mode, origin aligns to Monday of the week containing startDate
    const timelineStart = getMonday(normStart);

    // Timeline end extends to the Sunday of the week containing endDate
    const mondayOfEnd = getMonday(normEnd);
    const timelineEnd = addDays(mondayOfEnd, 6);

    const totalDays = Math.round(diffInDays(timelineStart, timelineEnd)) + 1;
    const totalWeeks = Math.ceil(totalDays / 7);
    const totalWidth = totalWeeks * weekWidth;

    return {
      timelineStart,
      timelineEnd,
      totalDays,
      totalWidth,
      unit: 'week',
      unitWidth: weekWidth,
      dayWidth: weekWidth / 7,
    };
  }

  // Day mode
  const timelineStart = normStart;
  const timelineEnd = normEnd;
  const totalDays = Math.round(diffInDays(timelineStart, timelineEnd)) + 1;
  const totalWidth = totalDays * dayWidth;

  return {
    timelineStart,
    timelineEnd,
    totalDays,
    totalWidth,
    unit: 'day',
    unitWidth: dayWidth,
    dayWidth,
  };
}

/**
 * Generates ticks and labels for rendering the SVG timeline axis and gridlines.
 *
 * @param {string|Date|number} startDate
 * @param {string|Date|number} endDate
 * @param {'day'|'week'} [unit='day']
 * @param {typeof TIMELINE_CONFIG} [config=TIMELINE_CONFIG]
 * @returns {Array<{
 *   id: string,
 *   x: number,
 *   width: number,
 *   date: string,
 *   primaryLabel: string,
 *   secondaryLabel: string,
 *   isMajor: boolean,
 *   weekNumber?: number
 * }>}
 */
export function generateTimelineTicks(
  startDate,
  endDate,
  unit = 'day',
  config = TIMELINE_CONFIG
) {
  const bounds = calculateTimelineBounds(startDate, endDate, unit, config);
  const { timelineStart, unitWidth } = bounds;
  const ticks = [];

  if (unit === 'week') {
    const totalWeeks = Math.round(bounds.totalDays / 7);
    for (let w = 0; w < totalWeeks; w++) {
      const mondayDate = addDays(timelineStart, w * 7);
      const weekNum = getISOWeekNumber(mondayDate);
      const { day, month } = parseDate(mondayDate);
      const x = w * unitWidth;

      // Major tick if the week crosses into a new month or is the first week
      const isMajor = day <= 7 || w === 0;

      ticks.push({
        id: `week-${mondayDate}`,
        x,
        width: unitWidth,
        date: mondayDate,
        primaryLabel: `T.${weekNum}`,
        secondaryLabel: `Tháng ${String(month).padStart(2, '0')}`,
        isMajor,
        weekNumber: weekNum,
      });
    }
    return ticks;
  }

  // Day mode
  for (let d = 0; d < bounds.totalDays; d++) {
    const currDate = addDays(timelineStart, d);
    const { day, month } = parseDate(currDate);
    const x = d * unitWidth;

    // Major tick on the 1st of every month or first tick
    const isMajor = day === 1 || d === 0;

    ticks.push({
      id: `day-${currDate}`,
      x,
      width: unitWidth,
      date: currDate,
      primaryLabel: String(day).padStart(2, '0'),
      secondaryLabel: `Tháng ${String(month).padStart(2, '0')}`,
      isMajor,
    });
  }

  return ticks;
}

/**
 * Groups consecutive ticks by their secondary label (e.g. month/year)
 * to render clean, grouped header blocks in the secondary tier.
 *
 * @param {Array<{ x: number, width: number, date: string }>} ticks
 * @returns {Array<{ id: string, x: number, width: number, label: string }>}
 */
export function generateSecondaryGroups(ticks) {
  if (!ticks || ticks.length === 0) return [];

  const groups = [];
  let currentGroup = null;

  for (const tick of ticks) {
    const { month, year } = parseDate(tick.date);
    const label = `Tháng ${String(month).padStart(2, '0')}/${year}`;

    if (!currentGroup || currentGroup.label !== label) {
      if (currentGroup) {
        groups.push(currentGroup);
      }
      currentGroup = {
        id: `group-${year}-${month}`,
        x: tick.x,
        width: tick.width,
        label,
      };
    } else {
      currentGroup.width += tick.width;
    }
  }

  if (currentGroup) {
    groups.push(currentGroup);
  }

  return groups;
}
