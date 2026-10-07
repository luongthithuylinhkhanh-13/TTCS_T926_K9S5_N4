/**
 * Helper formatting utilities for Gantt schedule presentation (T-37).
 */

/**
 * Formats ISO date 'YYYY-MM-DD' to Vietnamese display format 'DD/MM/YYYY'.
 * Returns null if input is null/undefined or empty.
 *
 * @param {string} dateStr
 * @returns {string|null}
 */
export function formatDateVN(dateStr) {
  if (!dateStr || typeof dateStr !== 'string') return null;
  const parts = dateStr.split('-');
  if (parts.length === 3) {
    return `${parts[2]}/${parts[1]}/${parts[0]}`;
  }
  return dateStr;
}

/**
 * Formats schedule variance days according to T-37 contract:
 * > 0: '+X ngày'
 * = 0: '0 ngày'
 * < 0: '-X ngày'
 *
 * @param {number|null} days
 * @returns {string|null}
 */
export function formatVariance(days) {
  if (days == null || !Number.isFinite(days)) return null;
  if (days > 0) return `+${days} ngày`;
  if (days === 0) return '0 ngày';
  return `${days} ngày`;
}
