/**
 * Short relative wording for list rows. Easier to scan than a timestamp, and
 * the exact time is kept in a title attribute wherever this is shown.
 */
export function relativeTime(iso: string, now: number = Date.now()): string {
  const then = new Date(iso).getTime();
  if (Number.isNaN(then)) {
    return '';
  }

  // Elapsed time floors rather than rounds. Rounding would report 31 seconds
  // as a minute ago, which reads as wrong to anyone watching the clock.
  const minutes = Math.floor((now - then) / 60000);
  if (minutes < 1) {
    return 'just now';
  }
  if (minutes < 60) {
    return `${minutes} min ago`;
  }

  const hours = Math.floor(minutes / 60);
  if (hours < 24) {
    return `${hours} h ago`;
  }
  return new Date(iso).toLocaleDateString();
}

export function exactTime(iso: string): string {
  const date = new Date(iso);
  return Number.isNaN(date.getTime()) ? '' : date.toLocaleString();
}
