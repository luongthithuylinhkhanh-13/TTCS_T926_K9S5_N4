const parseDate = value => {
  if (typeof value !== 'string' || !/^\d{4}-\d{2}-\d{2}$/.test(value)) {
    return null;
  }

  const timestamp = Date.parse(`${value}T00:00:00Z`);
  if (!Number.isFinite(timestamp) || new Date(timestamp).toISOString().slice(0, 10) !== value) {
    return null;
  }

  return timestamp;
};

const getDateRange = item => {
  const start = parseDate(item?.startDate);
  const end = parseDate(item?.endDate);

  if (start === null || end === null || end < start) {
    return null;
  }

  return { start, end };
};

const dayTimestamp = 24 * 60 * 60 * 1000;

export const buildScheduleTimeline = (tasks = [], baselineTasks = []) => {
  const baselineById = new Map(
    baselineTasks.map(item => [item.itemId, item])
  );
  const tasksById = new Map(tasks.map(item => [item.id, item]));
  const itemIds = new Set([...baselineById.keys(), ...tasksById.keys()]);
  const rows = Array.from(itemIds)
    .map(itemId => {
      const current = tasksById.get(itemId);
      const baseline = baselineById.get(itemId);
      const currentRange = getDateRange(current);
      const baselineRange = getDateRange(baseline);

      return {
        itemId,
        wbsCode: current?.wbsCode || baseline?.wbsCode || '',
        name: current?.name || baseline?.name || '',
        isCritical: Boolean(current?.isCritical),
        currentRange,
        baselineRange
      };
    })
    .filter(row => row.currentRange || row.baselineRange)
    .sort((a, b) => a.wbsCode.localeCompare(b.wbsCode, undefined, { numeric: true }));

  if (rows.length === 0) {
    return { rows, ticks: [], totalDays: 0 };
  }

  const ranges = rows.flatMap(row =>
    [row.currentRange, row.baselineRange].filter(Boolean)
  );
  const earliest = Math.min(...ranges.map(range => range.start));
  const latest = Math.max(...ranges.map(range => range.end));
  const firstMonth = new Date(earliest);
  const startTimestamp = Date.UTC(firstMonth.getUTCFullYear(), firstMonth.getUTCMonth(), 1);
  const lastDate = new Date(latest);
  const endTimestamp = Date.UTC(lastDate.getUTCFullYear(), lastDate.getUTCMonth() + 1, 1);
  const totalDays = (endTimestamp - startTimestamp) / dayTimestamp;
  const ticks = [];

  for (
    let timestamp = startTimestamp;
    timestamp < endTimestamp;
    timestamp = Date.UTC(
      new Date(timestamp).getUTCFullYear(),
      new Date(timestamp).getUTCMonth() + 1,
      1
    )
  ) {
    const date = new Date(timestamp);
    ticks.push({
      key: `${date.getUTCFullYear()}-${date.getUTCMonth()}`,
      label: new Intl.DateTimeFormat('vi-VN', {
        month: 'short',
        year: '2-digit',
        timeZone: 'UTC'
      }).format(date),
      offset: ((timestamp - startTimestamp) / dayTimestamp / totalDays) * 100
    });
  }

  const getBar = range => {
    if (!range) return null;

    const leftDays = (range.start - startTimestamp) / dayTimestamp;
    const durationDays = (range.end - range.start) / dayTimestamp + 1;

    return {
      left: (leftDays / totalDays) * 100,
      width: (durationDays / totalDays) * 100
    };
  };

  return {
    rows: rows.map(row => ({
      ...row,
      currentBar: getBar(row.currentRange),
      baselineBar: getBar(row.baselineRange)
    })),
    ticks,
    totalDays
  };
};

export const formatScheduleDate = value => {
  const timestamp = parseDate(value);
  if (timestamp === null) return '--';

  return new Intl.DateTimeFormat('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    timeZone: 'UTC'
  }).format(timestamp);
};

export const formatScheduleTimestamp = timestamp => {
  if (!Number.isFinite(timestamp)) return '--';

  return new Intl.DateTimeFormat('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    timeZone: 'UTC'
  }).format(timestamp);
};
